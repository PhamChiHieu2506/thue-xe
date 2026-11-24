# VeloDrive — hệ thống thuê xe theo SOA

Ứng dụng phục vụ khách thuê, chủ xe và quản trị viên. Dữ liệu tài khoản, xe, đơn thuê, giao dịch và lịch sử xử lý được lưu trong PostgreSQL. Không tạo sẵn khách hàng, xe cho thuê, đơn thuê, doanh thu hoặc đánh giá giả.

## Chạy bằng Docker trên Windows

Cài JDK 21 và mở Docker Desktop đến khi Engine running. Trong thư mục có `docker-compose.yml`, mở PowerShell và chạy:

```powershell
.\scripts\00a_setup_local.cmd
docker compose up -d --build
docker compose ps
```

Mở **http://localhost:8080**. Hướng dẫn từng thao tác: [HUONG_DAN_CHAY.txt](HUONG_DAN_CHAY.txt).

- Máy mới: tài khoản quản trị ban đầu ở `.local/TAI_KHOAN_QUAN_TRI.txt` (tệp riêng, được tạo trên máy bạn).
- Nâng cấp: tiếp tục dùng mật khẩu của tài khoản quản trị hiện có. Chạy thiết lập nhiều lần không đặt lại mật khẩu.
- Khách thuê và chủ xe tự đăng ký trên web, chọn đúng vai trò. Không có tài khoản dùng chung hay chức năng chọn vai trò sau đăng nhập.
- Trang tìm xe có sẵn 21 dòng xe thuộc 6 hãng để chọn, với ảnh cố định theo dòng xe. Xe có thể đặt thuê hiển thị riêng theo địa điểm và lịch, ngay sau khi chủ xe lưu đủ thông tin. Danh mục có giá/ngày và địa điểm hiển thị được cấu hình sẵn; không tạo biển số, chủ sở hữu hoặc xác nhận lịch trống của chiếc xe cụ thể.

## Nâng cấp từ bản trước

Giữ nguyên thư mục dự án đang sử dụng, `.env`, `.local`, các volume Docker và tên project Compose `ptpmhdv-kiemtra2`. Sao lưu dữ liệu trước khi thay đổi phiên bản. Dừng dịch vụ bằng `docker compose down` (không thêm `-v`), chép đè mã nguồn mới, chạy thiết lập rồi build lại như trên. Nếu chép vào thư mục mới mà thiếu `.env` cũ, mật khẩu ngẫu nhiên mới sẽ không khớp dữ liệu cũ.

Bản này giữ tài khoản và thông tin xe thực tế đã nhập. Xe cũ đã gửi chờ duyệt và đủ thông tin được tự đăng khi dịch vụ khởi động. Xe cũ còn thiếu thông tin hoặc chưa từng đăng giữ riêng; chủ xe bổ sung rồi bấm Lưu thay đổi hoặc Đăng xe. Không tạo lại xe hoặc đổi ID/chủ sở hữu/giá. Các dòng xe mẫu từ bản trước được loại khỏi mọi danh sách và API chi tiết bằng cờ tương thích cũ; không xóa vật lý dữ liệu. Bộ tạo xe mẫu, đặt lại mật khẩu và giao diện minh họa đã được gỡ. Nếu chép đè lên bản cũ, bước thiết lập chuyển đúng các file source đã ngừng dùng sang `.local/retired-source` để không bị Maven biên dịch lại; có thể xem lại bản lưu này khi cần. Sau nâng cấp, xóa các thư mục `target` cũ trước khi build trong Eclipse để tránh class đã xóa còn sót; Docker build dùng source mới.

## Luồng nghiệp vụ

1. Chủ xe đăng ký → khai báo xe, giá, địa điểm, năm sản xuất và mô tả → **Thêm và đăng xe**. Xe được công bố ngay, không qua quản trị duyệt.
2. Có thể chọn dòng xe có sẵn ảnh hoặc tải ảnh của xe. Sửa thông tin và lưu vẫn giữ xe được đăng; quản trị viên có trang **Quản lý xe** để cập nhật/lưu trữ.
3. Khách chọn địa điểm và lịch thuê → xem ảnh/chi phí → gửi yêu cầu.
4. Chủ xe duyệt yêu cầu nếu không trùng lịch → hệ thống giữ chỗ đến hạn thanh toán.
5. Khách chuyển khoản và khai báo mã giao dịch, hoặc thanh toán tiền mặt. Chủ xe kiểm tra **tiền thực nhận** rồi xác nhận. Đủ khoản giữ chỗ thì chuyến được xác nhận.
6. Chủ xe xác nhận bàn giao → thu phần còn lại → xác nhận trả xe, hoàn tất.
7. Hủy trước bàn giao hoặc hết hạn: lưu nghĩa vụ hoàn số tiền đã được xác nhận. Chủ xe thực hiện hoàn tiền ở ngoài hệ thống rồi ghi mã giao dịch hoàn.

Khoản giữ chỗ mặc định 40%, hạn tối đa 24 giờ sau khi duyệt và không muộn hơn giờ nhận xe; cấu hình bằng `BOOKING_DEPOSIT_PERCENT`, `BOOKING_HOLD_MINUTES` trong `.env`. Chi phí tính theo từng 24 giờ, làm tròn lên; không tự cộng phí, bảo hiểm hay đánh giá chưa có dữ liệu.

**Thanh toán hiện tại:** ghi nhận chuyển khoản/tiền mặt và đối soát thủ công. Hệ thống chưa kết nối cổng thanh toán, không tự chuyển tiền và không tự xác nhận thông báo chuyển khoản của khách.

## Ranh giới SOA

| Dịch vụ | Cổng | Trách nhiệm và dữ liệu sở hữu |
|---|---:|---|
| Auth | 8082 | Đăng ký, đăng nhập, JWT RSA, hồ sơ được che, mật khẩu, khóa tài khoản; `auth_db` |
| CarManage | 8080 | Hồ sơ xe, ảnh, đăng trực tiếp, tìm xe; `carmanage_db`, volume `car_media` |
| Rental | 8081 | Lịch xe, báo giá, yêu cầu/duyệt, bàn giao/trả, giao dịch/hoàn tiền, lịch sử; `rental_db` |
| Eureka | 8761 | Danh bạ dịch vụ; Rental tìm CarManage theo tên bằng OpenFeign + LoadBalancer |

Rental gọi hợp đồng HTTP của CarManage; không truy vấn bảng xe hoặc bảng người dùng trực tiếp. Mỗi dịch vụ có tài khoản DB, network dữ liệu và volume riêng. Frontend phối hợp API tìm xe và lịch trống; backend vẫn khóa lịch khi duyệt để chống đặt trùng.

Công nghệ: Java 21, Spring Boot 4.1.2, Spring Security/JWT, Spring Data JPA, PostgreSQL 17, Eureka Server, OpenFeign, Spring Cloud LoadBalancer, Actuator, Maven JAR, Dockerfile và Docker Compose. Nhật ký qua SLF4J/Logback và `docker compose logs`. Xem [THIET_KE_SOA.md](THIET_KE_SOA.md), [api/openapi.json](api/openapi.json), [KIEM_THU.md](KIEM_THU.md).

## Vận hành

```powershell
docker compose logs --tail=100 rental-service
docker compose stop carmanage-service
docker compose start carmanage-service
docker compose restart rental-service
docker compose --profile balance up -d --build
```

Bản sao CarManage ở cổng 8090 dùng chung dữ liệu và ảnh. Khởi tạo schema qua instance chính, bản sao dùng `ddl-auto=validate`. Khi cập nhật bản có đổi schema, dùng `docker compose --profile balance down` trước rồi build lại để tắt cả bản sao cũ.

Health/readiness: `/actuator/health`, `/actuator/health/readiness`. Các cổng DB không công bố ra máy chủ. Dừng/khởi động lại không xóa dữ liệu; `docker compose down -v` xóa volume nên không dùng khi nâng cấp.

## Quyền riêng tư và phạm vi triển khai

Họ tên/email được che trong DTO hồ sơ; số điện thoại chỉ hiện 3 số cuối. Mật khẩu lưu BCrypt, JWT ký RSA có issuer/audience/expiry. Không lưu JWT vào localStorage. Đổi mật khẩu yêu cầu mật khẩu hiện tại; tài khoản đã khóa bị từ chối đăng nhập. JWT đã cấp có hạn 15 phút và hiện chưa có thu hồi tập trung tức thời giữa các dịch vụ.

Ảnh xe công khai, dùng JPEG/PNG và loại metadata EXIF/GPS khi tải lên. Địa chỉ chi tiết không có trong danh sách công khai, được cung cấp theo quy trình xác nhận chuyến. API thông tin cá nhân và giao dịch dùng `Cache-Control: no-store`.

Compose phục vụ trên localhost. Khi đưa lên Internet cần cấu hình HTTPS/domain/CORS, quản lý bí mật, sao lưu, migration được kiểm soát, kiểm duyệt giấy tờ xe, hợp đồng và tích hợp thanh toán theo đơn vị vận hành. Source chưa triển khai các tích hợp bên ngoài này.

Tham khảo luồng sử dụng công khai của Mioto: https://www.mioto.vn/howitwork và https://www.mioto.vn/bookinghowto . Giao diện giữ thương hiệu VeloDrive; kiến trúc SOA ở đây là thiết kế của dự án, không khẳng định là kiến trúc nội bộ Mioto.

## Cập nhật giao diện tìm xe

- Đã bỏ cụm ba bước giới thiệu, đoạn mô tả và hai nhãn dưới banner, hai liên kết ở footer theo yêu cầu.
- Customer luôn có 21 dòng xe có ảnh: VinFast, Toyota, Hyundai, Kia, Mazda, Mercedes-Benz. Có lọc hãng/số chỗ, chọn dòng xe và tìm xe đã đăng theo địa điểm/lịch.
- Địa điểm nhận xe là danh sách đầy đủ 34 tỉnh/thành hiện hành. Cùng danh sách được dùng trong biểu mẫu owner. Tên tỉnh cũ được đối chiếu khi tìm kiếm và chuẩn hóa lúc lưu; không tự sửa hàng loạt dữ liệu cũ.
- Xe owner chưa upload ảnh được hiển thị ảnh đúng dòng xe (ví dụ Toyota Vios, G63), thay cho khung trống. Ảnh tải lên luôn được ưu tiên; ảnh dòng xe không được tính là ảnh owner đã upload. Khi lưu đủ thông tin, xe được đăng trực tiếp dù dùng ảnh dòng xe.
- Không có nhãn demo, ảnh ngẫu nhiên hoặc nhãn ảnh trên các thẻ xe. Metadata nguồn ảnh giữ trong `verification/car-image-sources.json` và tài liệu tài sản.

Nguồn danh mục tỉnh/thành và mã số: https://xaydungchinhsach.chinhphu.vn/bang-danh-muc-va-ma-so-cua-34-tinh-thanh-moi-cac-don-vi-hanh-chinh-cap-xa-moi-11925070418263625.htm . Nguồn đối chiếu tên sau sắp xếp: https://xaydungchinhsach.chinhphu.vn/chi-tiet-34-don-vi-hanh-chinh-cap-tinh-tu-12-6-2025-119250612141845533.htm . Dữ liệu được đóng gói cùng ứng dụng, không gọi website ngoài khi mở trang.

## Giá và địa điểm trên thẻ dòng xe

21 dòng xe có `displayPricePerDay` và `displayCity` trong hai cột cuối của
`carmanage-service/src/main/resources/car-catalog.tsv`. Các địa điểm được phân bổ
một lần rồi lưu vào file, không đổi mỗi khi mở trang. Giá trong danh mục là giá
hiển thị do dự án cấu hình, không phải giá thị trường đã khảo sát hoặc xác nhận
cho một chiếc xe cụ thể. Thông tin nguồn hãng ở các cột trước chỉ áp dụng dữ liệu
mẫu xe/ảnh, không xác thực hai cột cấu hình của ứng dụng.

Thẻ hiển thị địa điểm, giá VND/ngày và ảnh. Bộ lọc tỉnh/thành, hãng, số chỗ và mức
giá áp dụng đồng nhất với dữ liệu đang hiển thị. Khi có xe owner đã công bố và
còn lịch trống, thẻ ưu tiên giá/địa điểm từ chiếc xe phù hợp có giá thấp nhất.
Mở dòng xe sẽ chọn sẵn địa điểm trên thẻ; giao diện vẫn tìm lại xe đã đăng trước
khi đi tới báo giá hoặc đặt thuê. Không tạo thêm bản ghi Car và không thay đổi
giá, địa chỉ, chủ sở hữu của xe đã có.

Ví dụ cấu hình: VinFast VF 6 hiển thị 850.000 VND/ngày tại Đà Nẵng. Có thể sửa
hai cột cuối file TSV, giữ nguyên khóa dòng xe và cấu trúc chín cột, rồi build lại.
Hướng dẫn cập nhật: `CAP_NHAT_GIA_DIA_DIEM.txt`.

## Đăng xe trực tiếp

Owner thêm xe hoặc lưu chỉnh sửa hợp lệ thì `listingStatus=PUBLISHED`; customer tìm thấy xe theo địa điểm và lịch ngay, không cần admin thao tác. Biển số, tên/hãng, năm, số chỗ, giá dương, tỉnh/thành, khu vực, địa chỉ và mô tả phải đầy đủ. Với dòng xe ngoài danh mục ảnh, cần tải ít nhất một ảnh. Quyền sở hữu xe/ảnh và che địa chỉ vẫn được kiểm tra ở server.

Xe đã gửi chờ duyệt từ bản cũ được chuyển sang đã đăng lúc khởi động nếu đủ thông tin và ảnh hợp lệ. Bản nháp chưa gửi, xe lưu trữ, dữ liệu mẫu và bản ghi thiếu thông tin không tự công khai. Chủ xe mở **Xe của tôi**, bổ sung rồi lưu để đăng ngay. Việc chủ xe xác nhận **yêu cầu thuê/chuyến xe** vẫn giữ nguyên. Xem `DANG_XE_TRUC_TIEP.txt`.
