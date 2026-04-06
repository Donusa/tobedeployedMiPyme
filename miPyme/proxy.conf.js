const PROXY_BACKEND_URL = process.env['PROXY_BACKEND_URL'] || 'https://px47l7q6-8080.brs.devtunnels.ms';

module.exports = {
  "/api": {
    "target": PROXY_BACKEND_URL,
    "secure": false,
    "changeOrigin": true,
    "logLevel": "debug"
  },
  "/ws": {
    "target": PROXY_BACKEND_URL.replace(/^http/, 'ws'),
    "ws": true,
    "secure": false,
    "changeOrigin": true,
    "logLevel": "debug"
  }
};
