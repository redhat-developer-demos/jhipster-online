/**
 * Copyright 2017-2024 the original author or authors from the JHipster project.
 *
 * This file is part of the JHipster Online project, see https://github.com/jhipster/jhipster-online
 * for more information.
 *
 * Vendored building blocks from ng-jhipster 0.16.0 (Apache-2.0); see sibling files in this folder.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';

import { JhSortModule } from '../sort/jh-sort.module';

import { JhiKeysPipe } from './pipe/keys.pipe';
import { JhiFilterPipe } from './pipe/filter.pipe';
import { JhiPureFilterPipe } from './pipe/pure-filter.pipe';
import { JhiOrderByPipe } from './pipe/order-by.pipe';
import { JhiTranslateDirective } from './language/jhi-translate.directive';
import { JhiBooleanComponent } from './component/jhi-boolean.component';
import { JhiJvmMemoryComponent } from './component/metrics/jhi-jvm-memory.component';
import { JhiJvmThreadsComponent } from './component/metrics/jhi-jvm-threads.component';
import { JhiMetricsSystemComponent } from './component/metrics/jhi-metrics-system.component';
import { JhiMetricsHttpRequestComponent } from './component/metrics/jhi-metrics-request.component';
import { JhiMetricsEndpointsRequestsComponent } from './component/metrics/jhi-metrics-endpoints-requests';
import { JhiMetricsCacheComponent } from './component/metrics/jhi-metrics-cache.component';
import { JhiMetricsDatasourceComponent } from './component/metrics/jhi-metrics-datasource.component';
import { JhiMetricsGarbageCollectorComponent } from './component/metrics/jhi-metrics-garbagecollector.component';
import { JhiThreadModalComponent } from './component/metrics/jhi-metrics-modal-threads.component';

const PIPES = [JhiKeysPipe, JhiFilterPipe, JhiPureFilterPipe, JhiOrderByPipe];

const METRICS_AND_BOOLEAN = [
  JhiBooleanComponent,
  JhiJvmMemoryComponent,
  JhiJvmThreadsComponent,
  JhiMetricsSystemComponent,
  JhiMetricsHttpRequestComponent,
  JhiMetricsEndpointsRequestsComponent,
  JhiMetricsCacheComponent,
  JhiMetricsDatasourceComponent,
  JhiMetricsGarbageCollectorComponent,
  JhiThreadModalComponent
];

@NgModule({
  imports: [CommonModule, FormsModule, NgbModule, JhSortModule],
  declarations: [...PIPES, ...METRICS_AND_BOOLEAN, JhiTranslateDirective],
  exports: [
    CommonModule,
    FormsModule,
    NgbModule,
    JhSortModule,
    ...PIPES,
    ...METRICS_AND_BOOLEAN,
    JhiTranslateDirective
  ]
})
export class NgJhipsterCompatModule {}
