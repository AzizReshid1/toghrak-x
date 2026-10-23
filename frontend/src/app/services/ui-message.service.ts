import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export type UiMessageKind = 'info' | 'success' | 'error';

export interface UiMessage {
  kind: UiMessageKind;
  text: string;
}

/** A message on screen as a toast; `id` lets one toast be closed without touching the others. */
export interface Toast extends UiMessage {
  id: number;
}

/**
 * App-wide toast notifications. Every toast closes itself after a few seconds (errors stay a
 * little longer so they can be read) and can be closed earlier with its × button.
 */
@Injectable({ providedIn: 'root' })
export class UiMessageService {
  private static readonly DURATION_MS: Record<UiMessageKind, number> = { info: 4000, success: 4000, error: 7000 };
  private static readonly MAX_VISIBLE = 4;

  private toastsSubject = new BehaviorSubject<Toast[]>([]);
  readonly toasts$ = this.toastsSubject.asObservable();

  private nextId = 1;

  show(message: UiMessage): void {
    // The same message twice in a row (e.g. several requests failing for one reason) is shown once
    if (this.toastsSubject.value.some((t) => t.kind === message.kind && t.text === message.text)) return;

    const toast: Toast = { ...message, id: this.nextId++ };
    this.toastsSubject.next([...this.toastsSubject.value, toast].slice(-UiMessageService.MAX_VISIBLE));
    setTimeout(() => this.dismiss(toast.id), UiMessageService.DURATION_MS[message.kind]);
  }

  success(text: string): void {
    this.show({ kind: 'success', text });
  }

  error(text: string): void {
    this.show({ kind: 'error', text });
  }

  info(text: string): void {
    this.show({ kind: 'info', text });
  }

  dismiss(id: number): void {
    this.toastsSubject.next(this.toastsSubject.value.filter((t) => t.id !== id));
  }
}
