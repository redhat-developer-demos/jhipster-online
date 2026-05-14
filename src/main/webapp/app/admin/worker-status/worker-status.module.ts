import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { JhonlineSharedModule } from 'app/shared/shared.module';

import { WorkerStatusComponent } from './worker-status.component';
import { workerStatusRoute } from './worker-status.route';

@NgModule({
  imports: [JhonlineSharedModule, RouterModule.forChild([workerStatusRoute])],
  declarations: [WorkerStatusComponent]
})
export class WorkerStatusModule {}
