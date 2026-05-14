import { Route } from '@angular/router';
import { WorkerStatusComponent } from './worker-status.component';

export const workerStatusRoute: Route = {
  path: '',
  component: WorkerStatusComponent,
  data: {
    pageTitle: 'Worker Status'
  }
};
