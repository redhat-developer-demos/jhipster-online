/**
 * Copyright 2017-2024 the original author or authors from the JHipster project.
 *
 * This file is part of the JHipster Online project, see https://github.com/jhipster/jhipster-online
 * for more information.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { Injectable, NgZone, SecurityContext } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';

export type AlertType = 'success' | 'danger' | 'warning' | 'info';

export interface JhiAlert {
  id?: number;
  type: AlertType;
  msg: string;
  params?: any;
  timeout?: number;
  toast?: boolean;
  scoped?: boolean;
  position?: string;
  close?: (alerts: JhiAlert[]) => void;
}

export type Alert = JhiAlert;

@Injectable({ providedIn: 'root' })
export class AlertService {
  private alertId = 0;
  private readonly alerts: JhiAlert[] = [];
  private readonly defaultTimeout = 5000;
  private readonly toast = false;

  constructor(private sanitizer: DomSanitizer, private ngZone: NgZone) {}

  clear(): void {
    this.alerts.splice(0, this.alerts.length);
  }

  get(): JhiAlert[] {
    return this.alerts;
  }

  success(msg: string, params?: any, position?: string): JhiAlert {
    return this.addAlert(
      {
        type: 'success',
        msg,
        params,
        timeout: this.defaultTimeout,
        toast: this.isToast(),
        position
      },
      []
    );
  }

  error(msg: string, params?: any, position?: string): JhiAlert {
    return this.addAlert(
      {
        type: 'danger',
        msg,
        params,
        timeout: this.defaultTimeout,
        toast: this.isToast(),
        position
      },
      []
    );
  }

  warning(msg: string, params?: any, position?: string): JhiAlert {
    return this.addAlert(
      {
        type: 'warning',
        msg,
        params,
        timeout: this.defaultTimeout,
        toast: this.isToast(),
        position
      },
      []
    );
  }

  info(msg: string, params?: any, position?: string): JhiAlert {
    return this.addAlert(
      {
        type: 'info',
        msg,
        params,
        timeout: this.defaultTimeout,
        toast: this.isToast(),
        position
      },
      []
    );
  }

  addAlert(alertOptions: JhiAlert, extAlerts?: JhiAlert[]): JhiAlert {
    alertOptions.id = this.alertId++;
    const alert = this.factory(alertOptions);
    if (alertOptions.timeout && alertOptions.timeout > 0) {
      this.ngZone.runOutsideAngular(() => {
        setTimeout(() => {
          this.ngZone.run(() => {
            this.closeAlert(alertOptions.id!, extAlerts);
          });
        }, alertOptions.timeout);
      });
    }
    return alert;
  }

  isToast(): boolean {
    return this.toast;
  }

  closeAlert(id: number, extAlerts?: JhiAlert[]): JhiAlert[] {
    const thisAlerts: JhiAlert[] = extAlerts && extAlerts.length > 0 ? extAlerts : this.alerts;
    return this.closeAlertByIndex(thisAlerts.map(e => e.id).indexOf(id), thisAlerts);
  }

  private closeAlertByIndex(index: number, thisAlerts: JhiAlert[]): JhiAlert[] {
    return thisAlerts.splice(index, 1);
  }

  private factory(alertOptions: JhiAlert): JhiAlert {
    const sanitized = this.sanitizer.sanitize(SecurityContext.HTML, alertOptions.msg) ?? '';
    const alert: JhiAlert = {
      type: alertOptions.type,
      msg: sanitized,
      id: alertOptions.id,
      timeout: alertOptions.timeout,
      toast: alertOptions.toast,
      position: alertOptions.position ? alertOptions.position : 'top right',
      scoped: alertOptions.scoped,
      close: (alerts: JhiAlert[]) => {
        return this.closeAlert(alertOptions.id!, alerts);
      }
    };
    if (!alert.scoped) {
      this.alerts.push(alert);
    }
    return alert;
  }
}

/** @deprecated Use AlertService */
export { AlertService as JhiAlertService };
