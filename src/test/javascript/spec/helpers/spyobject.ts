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
export interface GuinessCompatibleSpy extends jasmine.Spy {
  /** By chaining the spy with and.returnValue, all calls to the function will return a specific
   * value. */
  andReturn(val: any): GuinessCompatibleSpy;
  /** By chaining the spy with and.callFake, all calls to the spy will delegate to the supplied
   * function. */
  andCallFake(fn: Function): GuinessCompatibleSpy;
  /** removes all recorded calls */
  reset(): void;
}

export class SpyObject {
  constructor(type?: any) {
    if (type) {
      const props = Object.getOwnPropertyNames(type.prototype);
      props.forEach(prop => {
        if (prop === 'constructor') {
          return;
        }
        let m = null;
        try {
          m = type.prototype[prop];
        } catch (e) {
          // ignore getters that throw when accessed during spy setup (e.g. abstract bases)
        }
        if (typeof m === 'function') {
          this.spy(prop);
        }
      });
    }
  }

  spy(name: string): GuinessCompatibleSpy {
    if (!this[name]) {
      this[name] = this.createGuinnessCompatibleSpy(name);
    }
    return this[name];
  }

  private createGuinnessCompatibleSpy(name: string): GuinessCompatibleSpy {
    const newSpy: GuinessCompatibleSpy = jasmine.createSpy(name) as any;
    newSpy.andCallFake = newSpy.and.callFake as any;
    newSpy.andReturn = newSpy.and.returnValue as any;
    newSpy.reset = newSpy.calls.reset as any;
    // revisit return null here (previously needed for rtts_assert).
    newSpy.and.returnValue(null);
    return newSpy;
  }
}
