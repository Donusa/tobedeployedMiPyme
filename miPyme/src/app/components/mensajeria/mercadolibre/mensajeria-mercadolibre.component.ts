import { Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { MercadoLibreService, MercadoLibreConversation, MercadoLibreMessage } from '../../../services/mercadolibre.service';
import { MeliRealtimeService } from '../../../services/meli-realtime.service';
import { finalize } from 'rxjs/operators';
import { LayoutService } from '../../../services/layout.service';

@Component({
    selector: 'app-mensajeria-mercadolibre',
    templateUrl: './mensajeria-mercadolibre.component.html',
    styleUrls: ['./mensajeria-mercadolibre.component.css']
})
export class MensajeriaMercadolibreComponent implements OnInit, OnDestroy, AfterViewChecked {
    conversations: MercadoLibreConversation[] = [];
    selectedConversation: MercadoLibreConversation | null = null;
    messages: MercadoLibreMessage[] = [];
    newMessage: string = '';
    isLoadingConversations = false;
    isLoadingMessages = false;
    isSending = false;

    mobilePanelMode: 'list' | 'chat' = 'list';
    @ViewChild('messagesContainer') messagesContainer?: ElementRef<HTMLDivElement>;
    private shouldScrollToBottom = false;

    constructor(private mercadoLibreService: MercadoLibreService,
        private realtime: MeliRealtimeService,
        public layoutService: LayoutService) { }

    backToList(): void {
      this.mobilePanelMode = 'list';
    }

    ngOnInit(): void {
        this.loadConversations();
        this.realtime.connect().catch(() => { });
    }

    ngOnDestroy(): void {
        this.realtime.disconnect();
    }

    loadConversations() {
        this.isLoadingConversations = true;
        this.mercadoLibreService.getConversations()
            .pipe(finalize(() => this.isLoadingConversations = false))
            .subscribe({
                next: (data: MercadoLibreConversation[]) => {
                    this.conversations = data;
                },
                error: () => {
                    this.conversations = [];
                }
            });
    }

    selectConversation(conversation: MercadoLibreConversation) {
        this.selectedConversation = conversation;

        if (this.layoutService.isMobile()) {
          this.mobilePanelMode = 'chat';
        }
        this.isLoadingMessages = true;
        this.mercadoLibreService.getMessages(conversation.id)
            .pipe(finalize(() => this.isLoadingMessages = false))
            .subscribe({
                next: (data: MercadoLibreMessage[]) => {
                    this.messages = data;
                    this.shouldScrollToBottom = true;
                },
                error: () => {
                    this.messages = [];
                }
            });
        this.realtime.subscribeToConversation(conversation.id, (payload: any) => {
            if (Array.isArray(payload)) {
                const existing = new Set(this.messages.map(m => m.id));
                for (const p of payload) {
                    if (!existing.has(p.id)) {
                        this.messages.push({
                            id: p.id,
                            fromUserId: p.fromUserId,
                            toUserId: p.toUserId,
                            text: p.text,
                            status: p.status,
                            dateCreated: p.dateCreated,
                            dateRead: '',
                            readByMe: false
                        });
                    }
                }
                this.shouldScrollToBottom = true;
            } else if (payload && payload.type === 'sent') {
                this.mercadoLibreService.getMessages(conversation.id).subscribe((data: MercadoLibreMessage[]) => {
                    this.messages = data;
                    this.shouldScrollToBottom = true;
                });
            }
        });
    }

    sendMessage() {
        if (!this.selectedConversation || !this.newMessage.trim() || this.isSending) return;
        this.isSending = true;
        this.mercadoLibreService.sendMessage(this.selectedConversation.id, this.newMessage)
            .pipe(finalize(() => this.isSending = false))
            .subscribe({
                next: () => {
                    this.newMessage = '';
                },
                error: () => { }
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
