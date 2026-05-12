import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { JhiAlert, JhiAlertService } from 'ng-jhipster';

import { EditorAiModelOption, EditorAiService } from './editor-ai.service';

@Component({
  selector: 'jhi-ai-assist-panel',
  templateUrl: './ai-assist-panel.component.html'
})
export class AiAssistPanelComponent implements OnInit {
  @Input() language = 'yaml';

  /** Current editor document (kept in sync by parent). */
  @Input() documentContent = '';

  /** Optional: return selected text from the editor (e.g. CodeMirror selection). */
  @Input() getSelection: () => string = () => '';

  /** Zero-based line index of the cursor (for completion). */
  @Input() getCursorLine: () => number = () => 0;

  @Output() insertText = new EventEmitter<string>();

  @Output() replaceDocument = new EventEmitter<string>();

  assistantEnabled = false;
  helpText = '';
  models: EditorAiModelOption[] = [];
  selectedModelId = '';
  generatePrompt = '';
  resultText = '';
  loading = false;
  explainPaste = '';
  fixErrors = '';

  constructor(private editorAiService: EditorAiService, private alertService: JhiAlertService) {}

  ngOnInit(): void {
    this.editorAiService.getConfig().subscribe(
      c => {
        this.assistantEnabled = c.enabled;
        this.helpText = c.helpText || '';
        this.models = c.models ?? [];
        const def = (c.defaultModelId || '').trim();
        if (def && this.models.some(m => m.id === def)) {
          this.selectedModelId = def;
        } else if (this.models.length > 0) {
          this.selectedModelId = this.models[0].id;
        } else {
          this.selectedModelId = '';
        }
      },
      () => {
        this.assistantEnabled = false;
        this.models = [];
        this.selectedModelId = '';
      }
    );
  }

  /** Called when user presses Ctrl+Shift+A in the YAML editor. */
  runCompleteFromShortcut(content: string, cursorLine: number): void {
    if (!this.assistantEnabled) {
      return;
    }
    this.resultText = '';
    this.loading = true;
    this.editorAiService.complete(content, this.language, cursorLine, this.selectedModelId).subscribe(
      res => {
        this.resultText = res.text ?? '';
        this.loading = false;
      },
      err => this.handleError(err)
    );
  }

  completeAtCursor(): void {
    const ctx = this.documentContent ?? '';
    const line = this.getCursorLine?.() ?? 0;
    this.resultText = '';
    this.loading = true;
    this.editorAiService.complete(ctx, this.language, line, this.selectedModelId).subscribe(
      res => {
        this.resultText = res.text ?? '';
        this.loading = false;
      },
      err => this.handleError(err)
    );
  }

  explainSelection(): void {
    const sel = (this.getSelection?.() ?? '').trim();
    if (!sel) {
      this.alertService.addAlert(
        { type: 'warning', msg: 'Select text in the editor first, or paste a fragment below.' } as JhiAlert,
        this.alertService.get()
      );
      return;
    }
    this.explainFragment(sel);
  }

  explainPasted(): void {
    const t = this.explainPaste.trim();
    if (!t) {
      this.alertService.addAlert({ type: 'warning', msg: 'Paste a JDL or YAML fragment to explain.' } as JhiAlert, this.alertService.get());
      return;
    }
    this.explainFragment(t);
  }

  fixDocument(): void {
    const content = (this.documentContent ?? '').trim();
    if (!content) {
      this.alertService.addAlert({ type: 'warning', msg: 'Nothing to fix — editor is empty.' } as JhiAlert, this.alertService.get());
      return;
    }
    this.resultText = '';
    this.loading = true;
    this.editorAiService.fix(content, this.language, this.fixErrors?.trim() || undefined, this.selectedModelId).subscribe(
      res => {
        this.resultText = res.text ?? '';
        this.loading = false;
      },
      err => this.handleError(err)
    );
  }

  generateFromPrompt(): void {
    const p = this.generatePrompt.trim();
    if (!p) {
      this.alertService.addAlert({ type: 'warning', msg: 'Enter a description first.' } as JhiAlert, this.alertService.get());
      return;
    }
    this.resultText = '';
    this.loading = true;
    this.editorAiService.generateFromPrompt(p, this.language, this.selectedModelId).subscribe(
      res => {
        this.resultText = res.text ?? '';
        this.loading = false;
      },
      err => this.handleError(err)
    );
  }

  insertResult(): void {
    if (!this.resultText) {
      return;
    }
    this.insertText.emit(this.resultText);
  }

  applyReplaceAll(): void {
    if (!this.resultText) {
      return;
    }
    this.replaceDocument.emit(this.resultText);
  }

  copyResult(): void {
    if (!this.resultText) {
      return;
    }
    navigator.clipboard.writeText(this.resultText).then(
      () => this.alertService.success('Copied', null),
      () => this.alertService.error('Copy failed', null)
    );
  }

  private explainFragment(text: string): void {
    this.resultText = '';
    this.loading = true;
    this.editorAiService.explain(text, this.language, this.selectedModelId).subscribe(
      res => {
        this.resultText = res.text ?? '';
        this.loading = false;
      },
      err => this.handleError(err)
    );
  }

  private handleError(err: { error?: { detail?: string; message?: string } }): void {
    const body = err.error;
    const fromServer = body?.detail || body?.message;
    this.alertService.error(fromServer || 'Editor AI request failed.', null);
    this.loading = false;
  }
}
