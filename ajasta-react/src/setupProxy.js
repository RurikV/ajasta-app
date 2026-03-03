const { createProxyMiddleware } = require('http-proxy-middleware');

/*
  Dev-time proxy configuration for Create React App.
  - Routes /v1/* API calls to the Kotlin backend on port 8090.
  - Routes /actuator/* health checks to the Kotlin backend.

  Notes:
  - When setupProxy.js is present, CRA ignores the "proxy" field in package.json.
  - The Kotlin backend uses /v1/* endpoints for all API calls.
*/
module.exports = function (app) {
  // Kotlin backend v1 API
  app.use(
    '/v1',
    createProxyMiddleware({
      target: 'http://localhost:8090',
      changeOrigin: true,
      logLevel: 'silent',
    })
  );

  // Actuator health endpoint
  app.use(
    '/actuator',
    createProxyMiddleware({
      target: 'http://localhost:8090',
      changeOrigin: true,
      logLevel: 'silent',
    })
  );

  // Legacy /api routes (for backwards compatibility if needed)
  app.use(
    '/api',
    createProxyMiddleware({
      target: 'http://localhost:8090',
      changeOrigin: true,
      logLevel: 'silent',
    })
  );
};
