import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface JdlAiModelOption {
  id: string;
  label: string;
}

export interface JdlAiConfig {
  enabled: boolean;
  helpText?: string;
  ragEnabled?: boolean;
  defaultModelId?: string;
  models?: JdlAiModelOption[];
}

export interface JdlAiGenerateResponse {
  jdl: string;
}

@Injectable({ providedIn: 'root' })
export class JdlAiService {
  constructor(private http: HttpClient) {}

  getConfig(): Observable<JdlAiConfig> {
    return this.http.get<JdlAiConfig>('api/jdl-ai/config');
  }

  generate(prompt: string, modelId?: string): Observable<JdlAiGenerateResponse> {
    const body: { prompt: string; modelId?: string } = { prompt };
    if (modelId && modelId.trim().length > 0) {
      body.modelId = modelId.trim();
    }
    return this.http.post<JdlAiGenerateResponse>('api/jdl-ai/generate', body);
  }
}
