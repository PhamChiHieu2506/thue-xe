/* Public API origins. Credentials and private keys never belong here. */
window.VELODRIVE_CONFIG = Object.freeze({
  authOrigin: `${location.protocol}//${location.hostname}:8082`,
  rentalOrigin: `${location.protocol}//${location.hostname}:8081`,
  timeoutMs: 12000,
});
