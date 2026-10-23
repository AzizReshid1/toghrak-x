import { QuillConfig } from 'ngx-quill';
import Quill from 'quill';

/**
 * Rich-text editor (Quill) settings shared by the post editor and the admin page editor.
 *
 * Only formats the backend keeps are offered: its Jsoup sanitizer removes classes and inline
 * styles, so colours, fonts or alignment would silently disappear after saving. `formats` also
 * limits what pasted text can bring in.
 */
export const EDITOR_CONFIG: QuillConfig = {
  // getSemanticHTML() output: real <ul>/<ol> lists and <pre> code blocks, plain HTML the backend accepts
  format: 'html',
  formats: ['header', 'bold', 'italic', 'underline', 'blockquote', 'code-block', 'list', 'link'],
  modules: {
    toolbar: [
      [{ header: [2, 3, false] }],
      ['bold', 'italic', 'underline'],
      ['blockquote', 'code-block'],
      [{ list: 'ordered' }, { list: 'bullet' }],
      ['link'],
      ['clean'],
    ],
  },
};

/**
 * What an editor writes into its form field: Quill's semantic HTML, with one fix. Quill 2.0.3's
 * getSemanticHTML() turns every space into &nbsp;, which stops normal line wrapping and leaks into
 * the plain-text search copy. Inside <pre> code blocks the plain spaces are still kept exactly.
 * Used as [valueGetter] on every <quill-editor>.
 */
export function editorHtml(quill: Quill): string | null {
  const html = quill.getSemanticHTML().replaceAll('&nbsp;', ' ');
  return html === '<p></p>' || html === '<p><br></p>' ? null : html;
}
