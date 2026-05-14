import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { JdlAiConfig } from 'app/home/jdl-metadata/jdl-ai.service';

export interface McpExtraFile {
  path: string;
  content: string;
}

export interface McpPreviewResponse {
  files: Record<string, string>;
}

@Injectable({ providedIn: 'root' })
export class McpGeneratorService {
  private readonly jsonHeaders = new HttpHeaders({ 'Content-Type': 'application/json' });

  constructor(private http: HttpClient) {}

  getMcpAiConfig(): Observable<JdlAiConfig> {
    return this.http.get<JdlAiConfig>('api/mcp-ai/config');
  }

  expand(body: {
    framework: string;
    baseMcpConfigJson?: string;
    jdlContext?: string;
    userPrompt: string;
    modelId?: string;
  }): Observable<{ text: string }> {
    return this.http.post<{ text: string }>('api/mcp-ai/expand', body);
  }

  jdlToTools(body: { jdl: string; framework: string; modelId?: string }): Observable<{ text: string }> {
    return this.http.post<{ text: string }>('api/mcp-ai/jdl-to-tools', body);
  }

  preview(mcpConfig: Record<string, unknown>): Observable<McpPreviewResponse> {
    return this.http.post<McpPreviewResponse>('api/mcp-preview', mcpConfig, { headers: this.jsonHeaders });
  }

  downloadZip(mcpConfig: Record<string, unknown>): Observable<HttpResponse<Blob>> {
    return this.http.post('api/generate-mcp', JSON.stringify(mcpConfig), {
      headers: this.jsonHeaders,
      observe: 'response',
      responseType: 'blob'
    });
  }
}
