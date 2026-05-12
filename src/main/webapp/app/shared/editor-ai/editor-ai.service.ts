import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface EditorAiModelOption {
  id: string;
  label: string;
}

export interface EditorAiConfig {
  enabled: boolean;
  helpText?: string;
  ragEnabled?: boolean;
  defaultModelId?: string;
  models?: EditorAiModelOption[];
}

export interface EditorAiTextResponse {
  text: string;
}

@Injectable({ providedIn: 'root' })
export class EditorAiService {
  constructor(private http: HttpClient) {}

  getConfig(): Observable<EditorAiConfig> {
    return this.http.get<EditorAiConfig>('api/editor-ai/config');
  }

  complete(context: string, language: string, cursorLine?: number, modelId?: string): Observable<EditorAiTextResponse> {
    const body: { context: string; language: string; cursorLine?: number; modelId?: string } = {
      context: context ?? '',
      language
    };
    if (cursorLine !== undefined && cursorLine !== null) {
      body.cursorLine = cursorLine;
    }
    if (modelId && modelId.trim().length > 0) {
      body.modelId = modelId.trim();
    }
    return this.http.post<EditorAiTextResponse>('api/editor-ai/complete', body);
  }

  explain(selection: string, language: string, modelId?: string): Observable<EditorAiTextResponse> {
    const body: { selection: string; language: string; modelId?: string } = { selection, language };
    if (modelId && modelId.trim().length > 0) {
      body.modelId = modelId.trim();
    }
    return this.http.post<EditorAiTextResponse>('api/editor-ai/explain', body);
  }

  fix(content: string, language: string, errors?: string, modelId?: string): Observable<EditorAiTextResponse> {
    const body: { content: string; language: string; errors?: string; modelId?: string } = { content, language };
    if (errors && errors.trim().length > 0) {
      body.errors = errors.trim();
    }
    if (modelId && modelId.trim().length > 0) {
      body.modelId = modelId.trim();
    }
    return this.http.post<EditorAiTextResponse>('api/editor-ai/fix', body);
  }

  generateFromPrompt(prompt: string, language: string, modelId?: string): Observable<EditorAiTextResponse> {
    const body: { prompt: string; language: string; modelId?: string } = { prompt, language };
    if (modelId && modelId.trim().length > 0) {
      body.modelId = modelId.trim();
    }
    return this.http.post<EditorAiTextResponse>('api/editor-ai/generate-from-prompt', body);
  }
}
