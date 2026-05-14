import { Component, OnInit, ViewChild } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { AiAssistPanelComponent } from 'app/shared/editor-ai/ai-assist-panel.component';
import { YamlEditorComponent } from './yaml-editor.component';

@Component({
  standalone: false,
  selector: 'jhi-helm-template-editor',
  templateUrl: './helm-template-editor.component.html'
})
export class HelmTemplateEditorComponent implements OnInit {
  files: string[] = [];
  selectedPath = '';
  content = '';
  loadError = '';
  saveMessage = '';
  saveError = '';

  @ViewChild('yamlRef') yamlEditor?: YamlEditorComponent;
  @ViewChild('aiRef') aiPanel?: AiAssistPanelComponent;

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.reloadFileList();
  }

  reloadFileList(): void {
    this.loadError = '';
    this.http.get<string[]>('api/admin/helm-template/files').subscribe(
      list => {
        this.files = list ?? [];
        if (this.files.length > 0 && !this.selectedPath) {
          this.select(this.files[0]);
        }
      },
      () => {
        this.files = [];
        this.loadError = 'Could not load Helm template file list.';
      }
    );
  }

  select(path: string): void {
    this.selectedPath = path;
    this.saveMessage = '';
    this.saveError = '';
    this.http.get('api/admin/helm-template/file', { params: { path }, responseType: 'text' }).subscribe(
      body => {
        this.content = body ?? '';
      },
      () => {
        this.content = '';
        this.saveError = 'Could not read file.';
      }
    );
  }

  save(): void {
    if (!this.selectedPath) {
      return;
    }
    this.saveMessage = '';
    this.saveError = '';
    this.http.put('api/admin/helm-template/file', this.content, { params: { path: this.selectedPath }, observe: 'response' }).subscribe(
      res => {
        if (res.status === 204) {
          this.saveMessage = 'Saved.';
        }
      },
      err => {
        if (err.status === 412) {
          this.saveError = 'Override directory is not configured (application.helm-template.override-directory). Set it to save edits.';
        } else {
          this.saveError = 'Save failed.';
        }
      }
    );
  }

  resetFromClasspath(): void {
    if (!window.confirm('Overwrite all files in the Helm override directory with the bundled classpath chart?')) {
      return;
    }
    this.saveMessage = '';
    this.saveError = '';
    this.http.post('api/admin/helm-template/reset-from-classpath', {}, { observe: 'response' }).subscribe(
      res => {
        if (res.status === 204) {
          this.saveMessage = 'Reset complete.';
          this.reloadFileList();
          if (this.selectedPath) {
            this.select(this.selectedPath);
          }
        }
      },
      err => {
        if (err.status === 412) {
          this.saveError = 'Override directory is not configured.';
        } else {
          this.saveError = 'Reset failed.';
        }
      }
    );
  }

  getYamlSelection(): string {
    return this.yamlEditor?.getSelectedText() ?? '';
  }

  getYamlCursorLine(): number {
    return this.yamlEditor?.getCursorLine() ?? 0;
  }

  onAiShortcut(ev: { content: string; line: number; ch: number }): void {
    this.aiPanel?.runCompleteFromShortcut(ev.content, ev.line);
  }

  onInsertAiText(text: string): void {
    this.yamlEditor?.insertTextAtCursor(text);
  }

  onReplaceAllAi(text: string): void {
    this.content = text;
  }
}
