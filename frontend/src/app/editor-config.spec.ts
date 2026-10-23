import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { QuillModule } from 'ngx-quill';
import Quill from 'quill';
import { EDITOR_CONFIG, editorHtml } from './editor-config';

@Component({
  standalone: false,
  template: `<quill-editor [formControl]="content" [valueGetter]="editorHtml" (onEditorCreated)="created($event)"></quill-editor>`,
})
class EditorHostComponent {
  readonly editorHtml = editorHtml;
  content = new FormControl<string | null>('<p>Hello <strong>world</strong></p><ul><li>one</li><li>two</li></ul>');
  editor!: Quill;
  ready!: Promise<void>;
  private resolveReady!: () => void;

  constructor() {
    this.ready = new Promise((resolve) => (this.resolveReady = resolve));
  }

  created(editor: Quill) {
    this.editor = editor;
    this.resolveReady();
  }
}

// Runs the real Quill editor in the browser: saved HTML must appear as formatting, and what the
// editor writes back must be plain HTML that the backend sanitizer keeps unchanged.
describe('Rich-text editor (Quill with EDITOR_CONFIG)', () => {
  let host: EditorHostComponent;
  let element: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReactiveFormsModule, QuillModule.forRoot(EDITOR_CONFIG)],
      declarations: [EditorHostComponent],
    }).compileComponents();
    const fixture = TestBed.createComponent(EditorHostComponent);
    host = fixture.componentInstance;
    element = fixture.nativeElement;
    fixture.detectChanges();
    await host.ready;
    await fixture.whenStable();
    fixture.detectChanges();
  });

  it('shows saved HTML as formatting, not as visible tags', () => {
    const editable = element.querySelector('.ql-editor') as HTMLElement;
    expect(editable.textContent).not.toContain('<p>');
    expect(editable.querySelector('strong')?.textContent).toBe('world');
  });

  it('writes back plain HTML: real <ul> lists, normal spaces', () => {
    host.editor.insertText(host.editor.getLength() - 1, ' three', 'user');
    const html = host.content.value ?? '';

    expect(html).toContain('<strong>world</strong>');
    expect(html).toContain('<ul>');
    expect(html).not.toContain('data-list'); // Quill's internal list markup, which the sanitizer would break
    expect(html).not.toContain('&nbsp;');
  });
});
