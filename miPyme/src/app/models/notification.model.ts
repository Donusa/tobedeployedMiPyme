export type NotificationType = 'MESSAGE' | 'STOCK_ALERT' | 'UNINVOICED_SALE' | 'ML_WEBHOOK' | 'TN_WEBHOOK';

export interface Notification {
  id: number;
  type: NotificationType;
  message: string;
  url: string;
  referenceKey: string;
  read: boolean;
  viewedAt: string | null;
  createdAt: string;
}
