import { fakeAsync, tick } from '@angular/core/testing';
import { Toast, UiMessageService } from './ui-message.service';

describe('UiMessageService', () => {
  let service: UiMessageService;
  let toasts: Toast[];

  beforeEach(() => {
    service = new UiMessageService();
    service.toasts$.subscribe((t) => (toasts = t));
  });

  it('closes a toast by itself after a few seconds', fakeAsync(() => {
    service.success('Saved.');
    expect(toasts.map((t) => t.text)).toEqual(['Saved.']);

    tick(4000);
    expect(toasts).toEqual([]);
  }));

  it('can be closed early, one toast at a time', fakeAsync(() => {
    service.error('First');
    service.error('Second');

    service.dismiss(toasts[0].id);
    expect(toasts.map((t) => t.text)).toEqual(['Second']);
    tick(7000);
  }));

  it('does not show the same message twice at once', fakeAsync(() => {
    service.error('Server error.');
    service.error('Server error.');

    expect(toasts.length).toBe(1);
    tick(7000);
  }));
});
