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
import { AfterContentInit, ContentChild, Directive, Host, HostListener, Input } from '@angular/core';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { IconDefinition, faSort, faSortDown, faSortUp } from '@fortawesome/free-solid-svg-icons';

import { SortDirective } from './sort.directive';

@Directive({
  selector: '[jhiSortBy]'
})
export class SortByDirective implements AfterContentInit {
  @Input() jhiSortBy!: string;
  @ContentChild(FaIconComponent, { static: true }) iconComponent!: FaIconComponent;

  private readonly sortIcon = faSort;
  private readonly sortAscIcon = faSortUp;
  private readonly sortDescIcon = faSortDown;

  constructor(@Host() private jhiSort: SortDirective) {}

  ngAfterContentInit(): void {
    if (
      !this.iconComponent ||
      !this.jhiSort.predicate ||
      this.jhiSort.predicate === '_score' ||
      this.jhiSort.predicate !== this.jhiSortBy
    ) {
      return;
    }
    this.updateIconDefinition(this.iconComponent, this.jhiSort.ascending ? this.sortAscIcon : this.sortDescIcon);
    this.jhiSort.activeIconComponent = this.iconComponent;
  }

  @HostListener('click')
  onClick(): void {
    if (!this.iconComponent || !this.jhiSort.predicate || this.jhiSort.predicate === '_score') {
      return;
    }
    this.jhiSort.sort(this.jhiSortBy);
    this.updateIconDefinition(this.jhiSort.activeIconComponent, this.sortIcon);
    this.updateIconDefinition(this.iconComponent, this.jhiSort.ascending ? this.sortAscIcon : this.sortDescIcon);
    this.jhiSort.activeIconComponent = this.iconComponent;
  }

  private updateIconDefinition(iconComponent: FaIconComponent | undefined, icon: IconDefinition): void {
    if (iconComponent) {
      iconComponent.icon = icon.iconName as any;
      (iconComponent as any).render?.();
    }
  }
}

/** @deprecated Use SortByDirective */
export { SortByDirective as JhiSortByDirective };
