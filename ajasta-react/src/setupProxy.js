const { createProxyMiddleware } = require('http-proxy-middleware');

/*
  Dev-time proxy configuration for Create React App.
  - Routes CMS API calls to the CMS microservice on port 8091.
  - Routes all other /api calls to the main backend on port 8090.

  Notes:
  - When setupProxy.js is present, CRA ignores the "proxy" field in package.json.
  - Order matters: put the more specific '/api/cms' before the generic '/api'.
*/
module.exports = function (app) {
  // CMS service
  app.use(
    '/api/cms',
    createProxyMiddleware({
      target: 'http://localhost:8091',
      changeOrigin: true,
      logLevel: 'silent',
    })
  );

  // Main backend
  app.use(
    '/api',
    createProxyMiddleware({
      target: 'http://localhost:8090',
      changeOrigin: true,
      logLevel: 'silent',
    })
  );
};
