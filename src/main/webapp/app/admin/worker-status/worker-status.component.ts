import { Component, OnInit, OnDestroy } from '@angular/core';
import { WorkerStatusService, WorkerStatusResponse, WorkerEntry, AiStatus } from './worker-status.service';

@Component({
  standalone: false,
  selector: 'jhi-worker-status',
  templateUrl: './worker-status.component.html'
})
export class WorkerStatusComponent implements OnInit, OnDestroy {
  workers: WorkerEntry[] = [];
  ai?: AiStatus;
  timestamp = '';
  loading = false;
  error = '';

  private refreshTimer?: ReturnType<typeof setInterval>;

  constructor(private workerStatusService: WorkerStatusService) {}

  ngOnInit(): void {
    this.refresh();
  }

  ngOnDestroy(): void {
    this.stopAutoRefresh();
  }

  refresh(): void {
    this.loading = true;
    this.error = '';
    this.workerStatusService.getStatus().subscribe({
      next: (res: WorkerStatusResponse) => {
        this.workers = res.workers;
        this.ai = res.ai;
        this.timestamp = res.timestamp;
        this.loading = false;
      },
      error: err => {
        this.loading = false;
        this.error = err.error?.detail || err.message || 'Failed to load worker status';
      }
    });
  }

  startAutoRefresh(): void {
    this.stopAutoRefresh();
    this.refreshTimer = setInterval(() => this.refresh(), 10000);
  }

  stopAutoRefresh(): void {
    if (this.refreshTimer) {
      clearInterval(this.refreshTimer);
      this.refreshTimer = undefined;
    }
  }

  get isAutoRefreshing(): boolean {
    return !!this.refreshTimer;
  }

  toggleAutoRefresh(): void {
    if (this.isAutoRefreshing) {
      this.stopAutoRefresh();
    } else {
      this.startAutoRefresh();
    }
  }

  badgeClass(status: string): string {
    switch (status) {
      case 'UP':
        return 'bg-success';
      case 'DOWN':
        return 'bg-danger';
      case 'DISABLED':
        return 'bg-secondary';
      default:
        return 'bg-warning';
    }
  }

  latencyClass(ms: number): string {
    if (ms < 0) return 'text-muted';
    if (ms < 200) return 'text-success';
    if (ms < 1000) return 'text-warning';
    return 'text-danger';
  }
}
