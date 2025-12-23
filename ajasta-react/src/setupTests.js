// Global Jest setup
// - Extend jest-dom matchers
// - Ensure Axios resolves to CJS build under Jest to avoid ESM parse issues

import '@testing-library/jest-dom';

// eslint-disable-next-line no-undef
jest.mock('axios', () => {
  try {
    // Prefer CommonJS build for Node/Jest
    // eslint-disable-next-line global-require, import/no-extraneous-dependencies
    return require('axios/dist/node/axios.cjs');
  } catch (e) {
    // Fallback to the actual module if path not available
    // eslint-disable-next-line global-require
    return jest.requireActual('axios');
  }
});
