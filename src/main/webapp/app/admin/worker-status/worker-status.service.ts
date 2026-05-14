import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface WorkerEntry {
  name: string;
  enabled: boolean;
  baseUrl: string;
  timeoutSeconds: number;
  status: 'UP' | 'DOWN' | 'DISABLED';
  latencyMs: number;
  httpStatus?: number;
  healthBody?: string;
  error?: string;
}

export interface AiModelEntry {
  id: string;
  label: string;
  model: string;
  hasApiUrl: string;
}

export interface AiStatus {
  enabled: boolean;
  assistantAvailable: boolean;
  ragEnabled: boolean;
  ragSemanticEnabled: boolean;
  defaultModelId: string;
  insecureTls: boolean;
  connectTimeoutMs: number;
  readTimeoutMs: number;
  models: AiModelEntry[];
}

export interface WorkerStatusResponse {
  timestamp: string;
  workers: WorkerEntry[];
  ai: AiStatus;
}

@Injectable({ providedIn: 'root' })
export class WorkerStatusService {
  constructor(private http: HttpClient) {}

  getStatus(): Observable<WorkerStatusResponse> {
    return this.http.get<WorkerStatusResponse>('api/admin/worker-status');
  }
}
