import { Toast, UiMessageService } from './ui-message.service';

describe('UiMessageService', () => {
  let service: UiMessageService;
  let toasts: Toast[];

  beforeEach(() => {
    vi.useFakeTimers(); // toasts close themselves on a timer
    service = new UiMessageService();
    service.toasts$.subscribe((t) => (toasts = t));
  });

  afterEach(() => vi.useRealTimers());

  it('closes a toast by itself after a few seconds', () => {
    service.success('Saved.');
    expect(toasts.map((t) => t.text)).toEqual(['Saved.']);

    vi.advanceTimersByTime(4000);
    expect(toasts).toEqual([]);
  });

  it('can be closed early, one toast at a time', () => {
    service.error('First');
    service.error('Second');

    service.dismiss(toasts[0].id);
    expect(toasts.map((t) => t.text)).toEqual(['Second']);
  });

  it('does not show the same message twice at once', () => {
    service.error('Server error.');
    service.error('Server error.');

    expect(toasts.length).toBe(1);
  });
});
