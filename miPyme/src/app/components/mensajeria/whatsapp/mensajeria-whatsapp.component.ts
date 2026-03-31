import { Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { WhatsappService, WppConversation, WppMessage } from '../../../services/whatsapp.service';
import { finalize } from 'rxjs/operators';
import { interval, Subscription } from 'rxjs';
import { LayoutService } from '../../../services/layout.service';

@Component({
    selector: 'app-mensajeria-whatsapp',
    templateUrl: './mensajeria-whatsapp.component.html',
    styleUrls: ['./mensajeria-whatsapp.component.css']
})
export class MensajeriaWhatsappComponent implements OnInit, OnDestroy, AfterViewChecked {

    isConnected = false;
    isCheckingStatus = true;
    isConnecting = false;
    connectionError = '';


    mobilePanelMode: 'list' | 'chat' = 'list';

    conversations: WppConversation[] = [];
    selectedConversation: WppConversation | null = null;
    messages: WppMessage[] = [];
    newMessage = '';
    isLoadingConversations = false;
    isLoadingMessages = false;
    isSending = false;
    isSyncing = false;
    syncError = '';


    private syncedMessages = new Map<number, WppMessage[]>();

    @ViewChild('messagesContainer') messagesContainer?: ElementRef<HTMLDivElement>;
    private shouldScrollToBottom = false;
    private pollSub?: Subscription;


    private connectSid = '';

    constructor(private whatsappService: WhatsappService, public layoutService: LayoutService) { }


    backToList(): void {
      this.mobilePanelMode = 'list';
    }

    ngOnInit(): void {
        console.log('[WPP] Component initialized, checking status...');
        this.checkStatus();
    }

    ngOnDestroy(): void {
        this.pollSub?.unsubscribe();
    }

    checkStatus() {
        this.isCheckingStatus = true;
        console.log('[WPP] Calling GET /api/whatsapp/status...');

        this.whatsappService.getStatus().subscribe({
            next: (res: any) => {
                console.log('[WPP] Status response:', res);
                this.isConnected = res.connected;
                if (this.isConnected) {
                    console.log('[WPP] Already connected, syncing conversation history...');
                    this.syncHistory();
                    this.startPolling();
                } else {
                    console.log('[WPP] Not connected — showing connect button');
                }
                this.isCheckingStatus = false;
            },
            error: (err: any) => {
                console.error('[WPP] Status check failed:', err);
                this.isConnected = false;
                this.isCheckingStatus = false;
            }
        });
    }


    private startPolling(): void {
        this.pollSub?.unsubscribe();
        this.pollSub = interval(15000).subscribe(() => this.refreshData());
    }

    private refreshData(): void {

        this.whatsappService.getConversations().subscribe({
            next: (data) => {
                this.conversations = data;

                if (this.selectedConversation) {
                    const updated = data.find(c => c.id === this.selectedConversation!.id);
                    if (updated) this.selectedConversation = updated;
                }
            },
            error: (err: any) => console.warn('[WPP] Poll refresh failed:', err)
        });


        if (this.selectedConversation) {
            this.whatsappService.getMessages(this.selectedConversation.id).subscribe({
                next: (fresh: WppMessage[]) => {
                    if (fresh.length !== this.messages.length) {
                        this.messages = fresh;
                        this.syncedMessages.set(this.selectedConversation!.id, fresh);
                        this.shouldScrollToBottom = true;
                    }
                },
                error: () => {  }
            });
        }
    }



    startConnect() {
        console.log('[WPP] Connect button clicked');
        this.isConnecting = true;
        this.connectionError = '';


        const fbSdk = (window as any).FB;
        if (!fbSdk) {
            console.error('[WPP] ❌ Facebook SDK not loaded! Make sure the SDK script is in index.html');
            this.connectionError = 'Facebook SDK no cargado. Recargá la página e intentá de nuevo.';
            this.isConnecting = false;
            return;
        }
        console.log('[WPP] ✓ Facebook SDK found');


        console.log('[WPP] Calling POST /api/wpp/connect/start...');
        this.whatsappService.startConnect().subscribe({
            next: (res) => {
                console.log('[WPP] Session created:', res);
                this.connectSid = res.sid;
                this.launchEmbeddedSignup(res.appId, res.configId);
            },
            error: (err: any) => {
                console.error('[WPP] ❌ Failed to create session:', err);
                this.connectionError = 'Error al iniciar la conexión: ' + (err.error?.error || err.message || 'desconocido');
                this.isConnecting = false;
            }
        });
    }

    private launchEmbeddedSignup(appId: string, configId: string) {
        console.log('[WPP] Launching Embedded Signup with appId=' + appId + ', configId=' + configId);


        const messageHandler = (event: MessageEvent) => {
            console.log('[WPP] postMessage received from origin:', event.origin, 'data:', event.data);

            if (event.origin !== 'https://www.facebook.com' && event.origin !== 'https://web.facebook.com') {
                console.log('[WPP] Ignoring message from non-Facebook origin');
                return;
            }

            try {
                const data = typeof event.data === 'string' ? JSON.parse(event.data) : event.data;
                console.log('[WPP] Parsed Facebook message:', data);

                if (data.type === 'WA_EMBEDDED_SIGNUP') {
                    const eventData = data.data || {};
                    console.log('[WPP] WA_EMBEDDED_SIGNUP event:', eventData);

                    const { code, phone_number_id, waba_id } = eventData;

                    if (code && phone_number_id && waba_id) {
                        console.log('[WPP] Got all required data: code=' + code.substring(0, 10) + '..., phoneNumberId=' + phone_number_id + ', wabaId=' + waba_id);
                        window.removeEventListener('message', messageHandler);
                        this.finishConnect(code, waba_id, phone_number_id);
                    } else {
                        console.warn('[WPP] ⚠️ WA_EMBEDDED_SIGNUP event missing fields:', { code: !!code, phone_number_id: !!phone_number_id, waba_id: !!waba_id });
                    }
                }
            } catch (e) {

            }
        };

        window.addEventListener('message', messageHandler);


        const fbSdk = (window as any).FB;
        console.log('[WPP] Calling FB.login...');

        fbSdk.login((response: any) => {
            console.log('[WPP] FB.login callback received:', response);

            if (response.authResponse) {
                console.log('[WPP] ✓ FB.login success, authResponse:', {
                    code: response.authResponse.code ? response.authResponse.code.substring(0, 15) + '...' : null,
                    accessToken: response.authResponse.accessToken ? 'present' : null,
                    userID: response.authResponse.userID
                });


                if (response.authResponse.code && !this.isConnected) {
                    console.log('[WPP] Got code from authResponse directly — waiting for postMessage with phone/WABA IDs...');



                    setTimeout(() => {
                        if (this.isConnecting && !this.isConnected) {
                            console.warn('[WPP] ⚠️ Timeout: postMessage with WA data not received after 30s');
                            console.warn('[WPP] This may mean the user did not complete the WhatsApp setup in the popup');
                            this.connectionError = 'No se recibieron los datos de WhatsApp. ¿Completaste el setup en la ventana emergente?';
                            this.isConnecting = false;
                            window.removeEventListener('message', messageHandler);
                        }
                    }, 30000);
                }
            } else {
                console.warn('[WPP] ❌ FB.login cancelled or failed:', response);
                this.connectionError = 'Login cancelado o falló';
                this.isConnecting = false;
                window.removeEventListener('message', messageHandler);
            }
        }, {
            config_id: configId,
            response_type: 'code',
            override_default_response_type: true,
            extras: {
                setup: {},
                featureType: '',
                sessionInfoVersion: 2
            }
        });
    }

    private finishConnect(code: string, wabaId: string, phoneNumberId: string) {
        console.log('[WPP] Calling POST /api/wpp/connect/finish with sid=' + this.connectSid);

        this.whatsappService.finishConnect(this.connectSid, code, wabaId, phoneNumberId).subscribe({
            next: (res: any) => {
                console.log('[WPP] ✓ Connect finished successfully:', res);
                this.isConnected = true;
                this.isConnecting = false;
                this.syncHistory();
                this.startPolling();
            },
            error: (err: any) => {
                console.error('[WPP] ❌ Connect finish failed:', err);
                this.connectionError = 'Error al finalizar la conexión: ' + (err.error?.error || err.message || 'desconocido');
                this.isConnecting = false;
            }
        });
    }




    syncHistory() {
        this.isSyncing = true;
        this.syncError = '';
        console.log('[WPP] Syncing conversation history...');

        this.whatsappService.syncConversations()
            .pipe(finalize(() => this.isSyncing = false))
            .subscribe({
                next: (data) => {
                    console.log('[WPP] Sync complete:', data.length, 'conversations');

                    this.syncedMessages.clear();
                    this.conversations = data.map((d: any) => {

                        const msgs: WppMessage[] = (d.recentMessages || []) as WppMessage[];
                        this.syncedMessages.set(d.id, msgs);

                        return {
                            id:            d.id,
                            contactWaId:   d.contactWaId,
                            contactName:   d.contactName,
                            lastMessageAt: d.lastMessageAt,
                            unreadCount:   d.unreadCount
                        } as WppConversation;
                    });


                    if (this.selectedConversation) {
                        const fresh = this.syncedMessages.get(this.selectedConversation.id);
                        if (fresh) {
                            this.messages = fresh;
                            this.shouldScrollToBottom = true;
                        }
                    }
                },
                error: (err: any) => {
                    console.error('[WPP] Sync failed:', err);
                    this.syncError = 'Error al sincronizar historial';
                }
            });
    }

    loadConversations() {
        this.isLoadingConversations = true;
        console.log('[WPP] Loading conversations...');

        this.whatsappService.getConversations()
            .pipe(finalize(() => this.isLoadingConversations = false))
            .subscribe({
                next: (data) => {
                    console.log('[WPP] Loaded', data.length, 'conversations');
                    this.conversations = data;
                },
                error: (err: any) => {
                    console.error('[WPP] Failed to load conversations:', err);
                    this.conversations = [];
                }
            });
    }

    selectConversation(conversation: WppConversation) {
        console.log('[WPP] Selected conversation:', conversation.id, conversation.contactWaId);
        this.selectedConversation = conversation;

        if (this.layoutService.isMobile()) {
          this.mobilePanelMode = 'chat';
        }


        const cached = this.syncedMessages.get(conversation.id);
        if (cached && cached.length > 0) {
            this.messages = cached;
            this.shouldScrollToBottom = true;
            this.isLoadingMessages = false;
        } else {
            this.isLoadingMessages = true;
            this.messages = [];
        }


        this.whatsappService.getMessages(conversation.id)
            .pipe(finalize(() => this.isLoadingMessages = false))
            .subscribe({
                next: (fresh: WppMessage[]) => {
                    console.log('[WPP] Loaded', fresh.length, 'messages from API');
                    if (fresh.length > 0) {
                        this.messages = fresh;
                        this.syncedMessages.set(conversation.id, fresh);
                        this.shouldScrollToBottom = true;
                    }
                },
                error: (err: any) => {
                    console.error('[WPP] Failed to load messages:', err);
                    if (!cached) this.messages = [];
                }
            });

        if (conversation.unreadCount > 0) {
            this.whatsappService.markConversationRead(conversation.id).subscribe();
            conversation.unreadCount = 0;
        }
    }

    sendMessage() {
        if (!this.selectedConversation || !this.newMessage.trim() || this.isSending) return;

        this.isSending = true;
        const text = this.newMessage;
        this.newMessage = '';
        console.log('[WPP] Sending message to', this.selectedConversation.contactWaId);

        this.whatsappService.sendMessage(this.selectedConversation.contactWaId, text)
            .pipe(finalize(() => this.isSending = false))
            .subscribe({
                next: (res: any) => {
                    console.log('[WPP] ✓ Message sent, wamid:', res.wamid);
                    this.messages.push({
                        id: 0,
                        wamid: res.wamid,
                        direction: 'OUT',
                        type: 'text',
                        textBody: text,
                        mediaId: '',
                        mediaUrlLocal: '',
                        mediaMimeType: '',
                        timestamp: new Date().toISOString()
                    });
                    this.shouldScrollToBottom = true;
                },
                error: (err: any) => {
                    console.error('[WPP] ❌ Send failed:', err);
                    this.newMessage = text;
                }
            });
    }

    private scrollToBottom() {
        const el = this.messagesContainer?.nativeElement;
        if (el) {
            el.scrollTop = el.scrollHeight;
        }
    }

    ngAfterViewChecked(): void {
        if (this.shouldScrollToBottom) {
            this.scrollToBottom();
            this.shouldScrollToBottom = false;
        }
    }
}
