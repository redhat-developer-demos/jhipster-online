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
import { DatePipe } from '@angular/common';
import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class DateUtilsService {
  private pattern = 'yyyy-MM-dd';
  private datePipe: DatePipe;

  constructor() {
    this.datePipe = new DatePipe('en');
  }

  convertDateTimeFromServer(date: any): Date | null {
    if (date) {
      return new Date(date);
    }
    return null;
  }

  convertLocalDateFromServer(date: any): Date | null {
    if (date) {
      const dateString = date.split('-');
      return new Date(dateString[0], dateString[1] - 1, dateString[2]);
    }
    return null;
  }

  convertLocalDateToServer(date: any, pattern = this.pattern): string | null {
    if (date) {
      const newDate = new Date(date.year, date.month - 1, date.day);
      return this.datePipe.transform(newDate, pattern) ?? null;
    }
    return null;
  }

  dateformat(): string {
    return this.pattern;
  }

  toDate(date: any): Date | null {
    if (date === undefined || date === null) {
      return null;
    }
    const dateParts = date.split(/\D+/);
    if (dateParts.length === 7) {
      return new Date(dateParts[0], dateParts[1] - 1, dateParts[2], dateParts[3], dateParts[4], dateParts[5], dateParts[6]);
    }
    if (dateParts.length === 6) {
      return new Date(dateParts[0], dateParts[1] - 1, dateParts[2], dateParts[3], dateParts[4], dateParts[5]);
    }
    return new Date(dateParts[0], dateParts[1] - 1, dateParts[2], dateParts[3], dateParts[4]);
  }
}

/** @deprecated Use DateUtilsService */
export { DateUtilsService as JhiDateUtils };
