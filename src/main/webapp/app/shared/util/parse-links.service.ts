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

/**
 * Parses RFC5988 Link headers (subset used by Spring Data REST pagination links).
 */
@Injectable({
  providedIn: 'root'
})
export class ParseLinksService {
  parse(header: string): Record<string, number | undefined> {
    if (!header || header.length === 0) {
      throw new Error('input must not be of zero length');
    }

    const parts: string[] = header.split(',');
    const links: Record<string, number | undefined> = {};

    parts.forEach(p => {
      const section: string[] = p.split(';');

      if (section.length !== 2) {
        throw new Error('section could not be split on ";"');
      }

      const url: string = section[0].replace(/<(.*)>/, '$1').trim();
      const queryString: Record<string, string | undefined> = {};

      url.replace(new RegExp('([^?=&]+)(=([^&]*))?', 'g'), (_$0, $1, _$2, $3) => {
        queryString[$1] = $3;
        return '';
      });

      let page: number | string | undefined = queryString.page;

      if (typeof page === 'string') {
        page = parseInt(page, 10);
      }

      const name: string = section[1].replace(/rel="(.*)"/, '$1').trim();
      links[name] = page;
    });
    return links;
  }
}

/** @deprecated Use ParseLinksService */
export { ParseLinksService as JhiParseLinks };
