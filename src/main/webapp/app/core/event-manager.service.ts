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
import { Injectable } from '@angular/core';
import { Subscription, Subject } from 'rxjs';
import { filter, map } from 'rxjs/operators';

export class EventWithContent<T = any> {
  constructor(public name: string, public content: T) {}
}

/** @deprecated Use EventWithContent */
export type JhiEventWithContent<T = any> = EventWithContent<T>;

@Injectable({ providedIn: 'root' })
export class EventManager {
  private readonly subject = new Subject<EventWithContent<any> | string>();

  broadcast(event: EventWithContent<any> | string): void {
    this.subject.next(event);
  }

  subscribe(eventName: string, callback: (event: any) => void): Subscription {
    return this.subject
      .pipe(
        filter((event: EventWithContent<any> | string) => {
          if (typeof event === 'string') {
            return event === eventName;
          }
          return event.name === eventName;
        }),
        map((event: EventWithContent<any> | string) => {
          if (typeof event !== 'string') {
            return event;
          }
          return undefined;
        })
      )
      .subscribe(callback);
  }

  destroy(subscription: Subscription): void {
    subscription.unsubscribe();
  }
}

/** @deprecated Use EventManager */
export { EventManager as JhiEventManager };
