/* Chỉ API xe đã có trong dự án. Ba API đọc khác bật sau khi triển khai.
   Các đường dẫn được truy cập qua cùng origin / API Gateway.
   Định dạng JSON xem HUONG_DAN.md. Không đặt thông tin đăng nhập ở đây. */
window.DASHBOARD_CONFIG = Object.freeze({
  authLoginUrl: 'http://localhost:8082/api/auth/login',
  endpoints: { cars: '/api/cars', rentals: null, customers: null, payments: null },
  timeoutMs: 8000,
  defaultMode: 'live'
});
