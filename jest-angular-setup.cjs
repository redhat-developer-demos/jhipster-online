/**
 * Runs once before each test file; Angular 19+ allows only one initTestEnvironment()
 * per process, so swallow the harmless "already been called" case.
 */

const { expect: expectJ, jest: jestRt } = require('@jest/globals');

/* Jest Circus has no Jasmine globals; SpyObject helpers expect jasmine.createSpy */
if (typeof globalThis.jasmine === 'undefined') {
  globalThis.jasmine = {
    createSpy() {
      const fn = jestRt.fn().mockImplementation(() => {});
      const spy = fn;
      spy.and = {
        returnValue(v) {
          fn.mockReturnValue(v);
          return spy;
        },
        callFake(f) {
          fn.mockImplementation(f);
          return spy;
        },
      };
      spy.calls = {
        reset() {
          fn.mockReset();
        },
      };
      return spy;
    },
    objectContaining: sample => expectJ.objectContaining(sample),
  };
}

if (typeof globalThis.spyOn !== 'function') {
  globalThis.spyOn = (object, method) => {
    const j = jestRt.spyOn(object, method);
    const wrap = j;
    wrap.and = {
      returnValue(v) {
        j.mockReturnValue(v);
        return wrap;
      },
      callFake(f) {
        j.mockImplementation(f);
        return wrap;
      },
    };
    return wrap;
  };
}

require('zone.js');
require('zone.js/testing');
const { TextEncoder, TextDecoder } = require('util');

const { getTestBed } = require('@angular/core/testing');
const {
  BrowserDynamicTestingModule,
  platformBrowserDynamicTesting,
} = require('@angular/platform-browser-dynamic/testing');

if (typeof globalThis.TextEncoder === 'undefined') {
  globalThis.TextEncoder = TextEncoder;
  globalThis.TextDecoder = TextDecoder;
}

const testEnvironmentOptions = globalThis.ngJest?.testEnvironmentOptions ?? Object.create(null);

try {
  getTestBed().initTestEnvironment(
    [BrowserDynamicTestingModule],
    platformBrowserDynamicTesting(),
    testEnvironmentOptions
  );
} catch (err) {
  const msg = err && err.message ? err.message : String(err);
  if (!msg.includes('already been called')) {
    throw err;
  }
}
