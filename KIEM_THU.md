# Kiểm thử bản đăng xe trực tiếp

Chạy `mvnw.cmd test` trên Windows hoặc `./mvnw test` trên Linux bằng JDK 21.
Báo cáo hiện tại: `verification/direct-publication.json`. Các kết quả phiên bản trước
nằm trong `verification/history/`; đó là lịch sử, không phải các ca được chạy lại lần này.

Bản cập nhật này đã chạy thành công **21 ca CarManage** qua HTTP thật trên cổng ngẫu nhiên
với H2. Các kiểm tra chính:

| Ca kiểm tra | Kết quả |
|---|---|
| Owner thêm xe đầy đủ |201 và PUBLISHED; đọc công khai được ngay |
| Owner sửa giá/thông tin |200 và vẫn PUBLISHED; giữ ID/chủ sở hữu |
| Xe có ảnh dòng xe, chưa upload |Đăng được; photoIds vẫn rỗng, hiển thị ảnh đúng dòng |
| Xe ngoài danh mục chưa có ảnh |400; tải ảnh hợp lệ rồi đăng được |
| Thiếu địa điểm/địa chỉ/mô tả/năm |400; không lưu xe mới thiếu thông tin |
| Lấy ảnh, sửa, đăng hoặc lưu trữ xe người khác |403 |
| Customer tạo/cập nhật xe |403 |
| Xe cũ chưa đăng |Chủ xe có thể dùng /publish hoặc alias /submit; đăng ngay |
| Xe đã chờ duyệt cũ và đủ thông tin |Migration chuyển PUBLISHED, chạy lặp không sinh xe |
| Xe nháp chưa gửi, thiếu thông tin, lưu trữ, dữ liệu mẫu |Không tự công khai |
| API duyệt cũ /review do admin gọi |410; không còn thay đổi trạng thái duyệt |
| Catalog và địa điểm |21 dòng xe có ảnh/giá dương, 34 tỉnh/thành, chuẩn hóa tên cũ |

Migration chỉ sửa trạng thái/ghi chú của xe đã gửi chờ duyệt, không ghi đè ID, giá,
chủ sở hữu, địa chỉ. Kiểm tra đã xác nhận các giá trị này và số bản ghi giữ nguyên.
Không đổi schema hoặc yêu cầu xóa volume khi nâng cấp.

**17 nhóm kiểm tra giao diện DOM** cũng đạt, chạy với Auth và CarManage thật trên H2,
Rental availability là stub có điều khiển. Đã kiểm tra owner chọn dòng xe, thêm xe không
qua admin, customer thấy ngay giá thực tế, owner sửa giá/tải ảnh vẫn giữ đăng, quyền
sở hữu ảnh/xe, admin quản lý xe thay cho duyệt, xe bận bị loại và lỗi lịch có thông báo.
Danh mục, ảnh, bộ lọc, dữ liệu tỉnh/thành và bảo vệ địa chỉ nhận xe tiếp tục hoạt động.

Đã build JAR đủ bốn module Auth, CarManage, Rental, Eureka bằng JDK 21. Lần này không
chạy lại Docker, PostgreSQL, kiểm thử đồng thời đặt thuê hoặc kiểm tra bố cục bằng
trình duyệt. Quy trình owner xác nhận yêu cầu thuê, thanh toán/hoàn tiền và snapshot
chuyến xe không thay đổi; kiểm chứng nền của chúng xem `verification/history/v8/`.

Sau cập nhật trên máy: `docker compose ps`, kiểm tra health/readiness và log, dùng owner
đăng một xe rồi customer tìm đúng địa điểm/lịch. Không dùng `down -v` nếu cần giữ dữ liệu.
Hướng dẫn từng bước: `DANG_XE_TRUC_TIEP.txt` và `HUONG_DAN_CHAY.txt`.
