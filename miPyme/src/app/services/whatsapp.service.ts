import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface WppConversation {
    id: number;
    contactWaId: string;
    contactName: string;
    lastMessageAt: string;
    unreadCount: number;
}

export interface WppMessage {
    id: number;
    wamid: string;
    direction: 'IN' | 'OUT';
    type: string;
    textBody: string;
    mediaId: string;
    mediaUrlLocal: string;
    mediaMimeType: string;
    timestamp: string;
}

export interface WppConnectResult {
    sid: string;
    configId: string;
    appId: string;
}

@Injectable({
    providedIn: 'root'
})
export class WhatsappService {

    private baseUrl = `${environment.apiUrl}/api/wpp`;
    private webhookUrl = `${environment.apiUrl}/api/whatsapp`;

    constructor(private http: HttpClient) { }



    getStatus(): Observable<any> {
        return this.http.get(`${this.webhookUrl}/status`);
    }

    startConnect(): Observable<WppConnectResult> {
        return this.http.post<WppConnectResult>(`${this.baseUrl}/connect/start`, {});
    }

    finishConnect(sid: string, code: string, wabaId: string, phoneNumberId: string): Observable<any> {
        return this.http.post(`${this.baseUrl}/connect/finish`, {
            sid, code, wabaId, phoneNumberId
        });
    }

    disconnect(): Observable<any> {
        return this.http.post(`${this.webhookUrl}/disconnect`, {});
    }



    getConversations(): Observable<WppConversation[]> {
        return this.http.get<WppConversation[]>(`${this.baseUrl}/conversations`);
    }

    getMessages(conversationId: number): Observable<WppMessage[]> {
        return this.http.get<WppMessage[]>(`${this.baseUrl}/conversations/${conversationId}/messages`);
    }

    sendMessage(to: string, text: string): Observable<any> {
        return this.http.post(`${this.baseUrl}/messages/send`, { to, text });
    }

    markConversationRead(conversationId: number): Observable<any> {
        return this.http.post(`${this.baseUrl}/conversations/${conversationId}/read`, {});
    }


    syncConversations(preview = 10): Observable<any[]> {
        return this.http.post<any[]>(`${this.baseUrl}/conversations/sync?preview=${preview}`, {});
    }
}
