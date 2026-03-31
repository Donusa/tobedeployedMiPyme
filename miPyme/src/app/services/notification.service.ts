import { Injectable, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject, BehaviorSubject, timer } from 'rxjs';
import { switchMap, takeUntil, tap, catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { Notification } from '../models/notification.model';

@Injectable({
  providedIn: 'root'
})
export class NotificationService implements OnDestroy {
  private apiUrl = `${environment.apiUrl}/api/notifications`;
  private destroy$ = new Subject<void>();


  notifications$ = new BehaviorSubject<Notification[]>([]);


  unreadCount$ = new BehaviorSubject<number>(0);

  private pollingStarted = false;

  constructor(private http: HttpClient) { }


  startPolling(): void {
    if (this.pollingStarted) return;
    this.pollingStarted = true;


    this.loadNotifications();

    timer(60000, 60000)
      .pipe(
        takeUntil(this.destroy$),
        switchMap(() => this.http.get<Notification[]>(this.apiUrl))
      )
      .subscribe({
        next: (data) => {
          this.notifications$.next(data);
          this.unreadCount$.next(data.filter(n => !n.read).length);
        },
        error: () => { }
      });
  }

  loadNotifications(): void {
    this.http.get<Notification[]>(this.apiUrl).subscribe({
      next: (data) => {
        this.notifications$.next(data);
        this.unreadCount$.next(data.filter(n => !n.read).length);
      },
      error: () => {
        this.notifications$.next([]);
        this.unreadCount$.next(0);
      }
    });
  }

  getAll(): Observable<Notification[]> {
    return this.http.get<Notification[]>(this.apiUrl);
  }

  markAsRead(id: number): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${id}/read`, {}).pipe(
      tap(() => {
        const current = this.notifications$.value;
        const updated = current.map(n =>
          n.id === id ? { ...n, read: true, viewedAt: new Date().toISOString() } : n
        );
        this.notifications$.next(updated);
        this.unreadCount$.next(updated.filter(n => !n.read).length);
      })
    );
  }

  markAllAsRead(): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/read-all`, {}).pipe(
      tap(() => {
        const current = this.notifications$.value;
        const now = new Date().toISOString();
        const updated = current.map(n => ({ ...n, read: true, viewedAt: now }));
        this.notifications$.next(updated);
        this.unreadCount$.next(0);
      })
    );
  }

  getUnreadCount(): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${this.apiUrl}/unread-count`);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`).pipe(
      tap(() => {
        const current = this.notifications$.value.filter(n => n.id !== id);
        this.notifications$.next(current);
        this.unreadCount$.next(current.filter(n => !n.read).length);
      })
    );
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
