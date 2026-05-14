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
import { Component, Input } from '@angular/core';

@Component({
  standalone: false,
  selector: 'jhi-item-count',
  template: `
    <div>
      Showing {{ (page - 1) * itemsPerPage == 0 ? 1 : (page - 1) * itemsPerPage + 1 }}
      -
      {{ page * itemsPerPage < total ? page * itemsPerPage : total }} of {{ total }} items.
    </div>
  `
})
export class ItemCountComponent {
  /** Current page number (1-based). */
  @Input() page!: number;

  /** Total number of items. */
  @Input() total!: number;

  /** Items per page. */
  @Input() itemsPerPage!: number;

  i18nEnabled = false;

  i18nValues(): Record<string, number> {
    const first = (this.page - 1) * this.itemsPerPage === 0 ? 1 : (this.page - 1) * this.itemsPerPage + 1;
    const second = this.page * this.itemsPerPage < this.total ? this.page * this.itemsPerPage : this.total;

    return {
      first,
      second,
      total: this.total
    };
  }
}

/** @deprecated Use ItemCountComponent */
export { ItemCountComponent as JhiItemCountComponent };
