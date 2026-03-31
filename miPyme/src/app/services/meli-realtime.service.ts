import { Injectable, OnDestroy } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class MeliRealtimeService implements OnDestroy {
  private client: Client | null = null;
  private subscription: StompSubscription | null = null;

  private toWsUrl(httpUrl: string): string {
    if (httpUrl.startsWith('https://')) {
      return httpUrl.replace('https://', 'wss://') + '/ws';
    }
    if (httpUrl.startsWith('http://')) {
      return httpUrl.replace('http://', 'ws://') + '/ws';
    }
    return httpUrl + '/ws';
  }

  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.client && this.client.connected) {
        resolve();
        return;
      }
      const url = this.toWsUrl(environment.apiUrl);
      this.client = new Client({
        brokerURL: url,
        reconnectDelay: 3000,
        debug: () => {}
      });
      this.client.onConnect = () => resolve();
      this.client.onStompError = () => reject('STOMP error');
      this.client.activate();
    });
  }

  subscribeToConversation(conversationId: number, handler: (payload: any) => void): void {
    if (!this.client) return;
    if (this.subscription) {
      this.subscription.unsubscribe();
      this.subscription = null;
    }
    const destination = `/topic/meli/messages/${conversationId}`;
    this.subscription = this.client.subscribe(destination, (message: IMessage) => {
      try {
        const body = JSON.parse(message.body);
        handler(body);
      } catch {
        handler(message.body);
      }
    });
  }

  disconnect(): void {
    if (this.subscription) {
      this.subscription.unsubscribe();
      this.subscription = null;
    }
    if (this.client) {
      this.client.deactivate();
      this.client = null;
    }
  }

  ngOnDestroy(): void {
    this.disconnect();
  }
}
