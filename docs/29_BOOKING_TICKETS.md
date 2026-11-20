# Vé theo đơn vị ghế — BE-U04

## Máy in giả lập — source ngày 2026-11-20

Đã thêm luồng giả lập in theo UUID booking, dành cho ADMIN hoặc STAFF đúng rạp. Đây là endpoint riêng để thử nghiệp vụ trước khi nối máy in thật; chưa triển khai pickup-pass, lookup QR booking hay print-jobs vật lý. Không đổi QR từng vé/check-in hoặc tạo vé giả. Các ghi chú “chưa có quyền Staff theo rạp trong Booking” bên dưới mô tả bước nền tảng; chỉ endpoint giả lập mới được mở cho STAFF sau kiểm tra scope, các route management cũ vẫn ADMIN.

### Bật và gọi API

Booking Service mặc định tắt controller. Bật bằng `BOOKING_PRINT_SIMULATOR_ENABLED=true` khi cần giả lập. `BOOKING_USER_BASE_URL` mặc định http://localhost:8082; `BOOKING_INVENTORY_BASE_URL` là cấu hình Inventory hiện có. Cả application.yml và config-repo/booking-service.yml có binding flag; không đổi cấu hình MySQL hoặc thêm credential. Cần User migration V4 và Booking migration V12 trước khi chạy code mới; lượt triển khai này chưa tự chạy migration.

`POST /api/v1/bookings/{bookingId}/print-simulations`

- Access token của ADMIN/STAFF, UUID booking đầy đủ và header Idempotency-Key bắt buộc, 1–100 ký tự ASCII chữ/số/chấm/gạch ngang/gạch dưới.
- Body gồm deviceId (cùng tập ký tự, 1–100) và outcome. deviceId là nhãn thiết bị do client khai báo phục vụ audit, chưa phải danh tính POS đã đăng ký/chứng thực.
- outcome chỉ nhận SIMULATED_SUCCEEDED hoặc SIMULATED_FAILED. Client chọn kết quả để thử luồng; chỉ endpoint giả lập cho phép điều này, không được dùng contract này làm callback máy in thật.

Ví dụ body, không chứa secret:

```json
{
  "deviceId": "POS-LOCAL-01",
  "outcome": "SIMULATED_SUCCEEDED"
}
```

HTTP 200 theo ApiResponse, Cache-Control no-store. data gồm printJobId, bookingId, displayCode (6 ký tự cuối UUID viết hoa), mode luôn SIMULATION, status, actorId, deviceId, cinemaId, showtimeId, completedAt, tickets và totalGuests. Mỗi ticket gồm ticketId, bookingSeatId, seatNumber, capacity; một ghế COUPLE là một vé/capacity 2. Trả toàn bộ bộ vé và tổng số khách, không trả/lưu QR trong snapshot mô phỏng. completedAt là lúc hoàn thành mô phỏng phía server, không phải xác nhận máy in vật lý.

### Kiểm tra quyền và nghiệp vụ

Booking gửi bearer hiện hành tới `GET /api/v1/users/me/staff-context`; endpoint này không nhận userId từ FE. User Service đọc account ACTIVE, role và assignment trực tiếp trong User DB: ADMIN trả administrator=true; STAFF phải có đúng một assignment và trả cinemaId hiện tại; USER, account bị khóa/tắt, STAFF thiếu assignment bị 403. Quyền không chỉ dựa vào claim JWT cũ. Booking kiểm tra userId trong response trùng actor đang gọi; User lỗi/response sai không cho in. Không ghi bearer vào log/cause; tắt redirect khi gọi User/Inventory, timeout connect 1s/read 2s.

Đọc showtime/cinema qua API Inventory: rạp phải active, lịch có startsAt/endsAt hợp lệ và trạng thái SCHEDULED/OPEN_FOR_BOOKING/CLOSED. CANCELLED/COMPLETED/trạng thái khác bị chặn. STAFF chỉ được đúng rạp, ADMIN tất cả rạp. Sau đó khóa booking trong transaction, kiểm tra CONFIRMED, showtimeId vẫn đúng và giờ server không sau startsAt + 15 phút (mốc +15 phút vẫn được xét). Không đặt cửa sổ mở in sớm hơn vì chưa có yêu cầu; booking CONFIRMED trước giờ chiếu được mô phỏng.

Bộ vé phải khớp toàn bộ booking_seats theo ID, không chỉ đếm số lượng: không thiếu/trùng đơn vị ghế, snapshot đã hoàn tất, seat number/bookingId và capacity khớp rule BookingTicket. Tất cả vé phải ISSUED; bất kỳ USED hoặc trạng thái khác chặn toàn bộ. Không sửa status/checkedInAt của vé, trạng thái booking, payment, reservation hoặc Kafka. Dùng cùng khóa booking mà issuance/recovery/check-in đang dùng để tuần tự hóa những thao tác này trong Booking DB.

### Idempotency, audit và giới hạn mô phỏng

Mỗi booking tối đa một row booking_print_simulations. Cùng key/actor/device/outcome trả snapshot kết quả cũ; không thực hiện mô phỏng lần nữa, kể cả vé đã USED sau lần đầu. Vẫn kiểm tra lại quyền/rạp, trạng thái CONFIRMED, lịch/rạp hợp lệ trước khi trả snapshot. Kết quả replay là lịch sử, không phải quyền in hiện tại. Replay không kiểm tra lại thời hạn tạo job nên có thể đọc kết quả cũ sau +15 phút nếu các điều kiện quyền/lịch còn hợp lệ.

Key khác cho cùng booking bị 409 BOOKING_PRINT_SIMULATION_ALREADY_EXISTS. Cùng key nhưng khác actor/device/outcome, hoặc cùng actor tái sử dụng key cho booking khác, bị 409 BOOKING_PRINT_IDEMPOTENCY_CONFLICT. Unique booking_id và unique actor_id/idempotency_key bảo vệ cả concurrency; khóa booking chặn hai key đồng thời tạo hai row. Lỗi mô phỏng cũng lưu kết quả terminal và không tự tạo lần thử mới bằng key khác; retry cùng key chỉ trả lỗi mô phỏng đã ghi.

Actor, thiết bị, rạp, thời điểm và kết quả được lưu trong row/snapshot của job. SIMULATED_SUCCEEDED/SIMULATED_FAILED đều không chứng minh in giấy; không tạo bản ghi đã in thật và không tiêu thụ quyền in vật lý trong workflow tương lai. Chưa có printer transport, spooler, tài nguyên PDF, callback thiết bị, retry vật lý hoặc in lại. Không thêm nút in vào User FE; không sửa Staff/POS FE hoặc generate client.

V12\_\_booking_print_simulations.sql thêm bảng MySQL, FK booking trong cùng DB, index rạp/thời điểm, unique constraints, CHECK status và JSON_VALID cho snapshot. Actor/rạp không có FK liên DB. Rollback: tắt simulator, dừng writer, xuất/giữ audit rồi rollback code; chỉ bỏ bảng mới sau khi lưu dữ liệu, không xóa booking/vé hay sửa V1–V11. Bảng mô phỏng tách khỏi dữ liệu in vật lý tương lai.

Các mã mới: 403 BOOKING_PRINT_ACCESS_DENIED; 503 BOOKING_PRINT_DEPENDENCY_UNAVAILABLE; 409 BOOKING_PRINT_NOT_ALLOWED, BOOKING_PRINT_WINDOW_CLOSED, BOOKING_PRINT_TICKETS_NOT_READY, BOOKING_PRINT_TICKET_NOT_ISSUED và các lỗi idempotency trên; 500 BOOKING_PRINT_SIMULATION_STORAGE_FAILED. Thiếu/sai header hoặc body trả validation 400 theo common-api. User endpoint có 403 USER_STAFF_ACCESS_DENIED. Không đổi contract endpoint cũ.

Giới hạn: context User và lịch Inventory là snapshot trước transaction Booking, không có khóa phân tán ngăn đổi rạp/role/lịch trong khoảng đọc đến commit. Kiểm tra lại mỗi request giảm rủi ro token cũ nhưng không thay thế cơ chế nhất quán liên service. Nhãn deviceId không phải thiết bị được xác thực. Runtime scope, race và OpenAPI chưa được xác minh chỉ bằng biên dịch; cần rà tiếp trước khi dùng làm nền tảng in thật.

Kiểm tra thủ công sau triển khai (chưa thực hiện ở lượt source này): flag tắt không có endpoint; USER/STAFF sai rạp bị chặn; ADMIN đúng role DB được mọi rạp; tắt account/đổi rạp/revoke role rồi dùng token cũ phải bị chặn theo context mới; vé thiếu/USED/đơn không CONFIRMED không tạo row; mô phỏng hai outcome trên hai booking khác nhau; retry cùng key giữ jobId/snapshot và không thêm row; key khác hoặc cùng key payload khác bị 409; check-in cạnh tranh không tạo job mới sau USED; kiểm tra +15 phút, Inventory/User lỗi, body/header sai. Không bật service hoặc ghi DB tự động chỉ để xác minh.

Kiểm tra biên dịch cuối: Maven offline `-pl services/booking-service,services/user-service -am -DskipTests test-compile` BUILD SUCCESS cho 18 module, gồm main/test source và không thực thi test case. `git diff --check` không có lỗi whitespace. Chưa gọi API, chạy migration, khởi động service hoặc xác minh OpenAPI runtime/thiết bị. Khi bật simulator và triển khai schema, cần đọc /v3/api-docs thật trước khi generate client.

## QR booking và Staff/POS — quyết định ngày 2026-11-20

Đây là workflow mới, tách khỏi QR từng vé và check-in bên dưới. Quyết định đã chốt:

- Nhân viên thuộc đúng một rạp, tài khoản do ADMIN cấp; ADMIN được thao tác mọi rạp. Chỉ làm BE và contract trong đợt này, chưa sửa trang Admin hoặc User FE.
- Display code là 6 ký tự cuối UUID booking, viết hoa. Vẫn dùng UUID đầy đủ cho DB/API/route; suffix có thể trùng, không phải credential hoặc khóa tìm duy nhất.
- QR booking hết hạn tại startsAt + 15 phút; lookup không đánh dấu USED. Chỉ chủ booking CONFIRMED và đã đủ toàn bộ vé được nhận QR. Ghế COUPLE là một đơn vị vé, capacity 2.
- Print job bị chặn nếu đã có lần in hoặc bất kỳ vé nào USED. Không in một phần booking; không tự coi tạo job là in vật lý thành công. Quét/bấm lặp phải dùng idempotency và kiểm tra lại phạm vi rạp phía server.

**Đã có source ở bước nền tảng:** API quản lý tài khoản nhân viên và gán rạp trong User Service, mô tả bên dưới. **Chưa có:** pickup-pass, lookup QR booking, print job, API xác nhận máy in, quyền Staff theo rạp trong Booking, suffix search, trang FE và generated client mới. Không gọi các endpoint đề xuất trong hội thoại như thể đã tồn tại. Không mở quyền STAFF cho các route quản lý Booking hiện tại.

### API Admin quản lý nhân viên

Yêu cầu đồng thời role ADMIN và permission user:manage từ access token do User Service cấp. STAFF/USER hoặc client credential chỉ có user:manage không được gọi. Các route nằm dưới /api/v1/users nên Gateway route hiện có chuyển tiếp tới User Service. User Service kiểm tra cả filter và method security; giữ phân quyền quản trị hiện tại của các endpoint khác.

| Method | Path                                               | Kết quả                                                                                   |
| ------ | -------------------------------------------------- | ----------------------------------------------------------------------------------------- |
| POST   | /api/v1/users/staff                                | 201, tạo user/credential/profile/role STAFF và một assignment trong cùng transaction      |
| GET    | /api/v1/users/staff?cinemaId={uuid}&page=0&size=20 | 200, PageResponse với content và page theo common-response; cinemaId tùy chọn, size 1–100 |
| GET    | /api/v1/users/staff/{userId}                       | 200, tài khoản và rạp được gán; thiếu assignment trả 404                                  |
| PUT    | /api/v1/users/staff/{userId}/cinema                | 200, đổi rạp dưới khóa ghi; gửi lại cùng rạp giữ nguyên assignment/audit                  |

POST nhận email, username, password, firstName, lastName, phoneNumber, cinemaId. email đúng định dạng, tối đa 320 ký tự; username 3–100 ký tự ASCII gồm chữ/số/dấu chấm/gạch dưới/gạch ngang. Password do ADMIN đặt, tối thiểu 12 ký tự và tối đa 72 byte UTF-8 theo credential service hiện có. Tên tối đa 100, điện thoại tối đa 32 ký tự; các trường profile tùy chọn. cinemaId bắt buộc là UUIDv7. PUT chỉ nhận cinemaId.

Response từng tài khoản: userId, username, email, status, cinemaId, assignedBy, createdAt, updatedAt. createdAt/updatedAt là thời điểm của assignment. Không trả password/hash. Response thành công dùng DTO trực tiếp như các API User hiện có; lỗi dùng common-api. List dùng PageResponse, không serialize Spring Page làm contract. Các response đều Cache-Control: no-store.

Tài khoản cấp bởi ADMIN được ACTIVE để đăng nhập bằng credential đã cấp; admin_provisioned ghi nguồn cấp, không giả lập email_verified_at. Tắt/mở lại tài khoản loại này giữ ACTIVE mà không giả nhận đã xác minh email. Tài khoản đăng ký công khai/Google và quy tắc xác minh cũ không đổi. Đổi mật khẩu qua endpoint owner hiện có; chưa thêm cơ chế bắt buộc đổi mật khẩu lần đầu hoặc gửi credential bằng email. ADMIN chuyển credential qua kênh nội bộ; không đưa credential vào URL/log. Request DTO che toàn bộ toString; audit chỉ chứa actor/target/rạp.

V2 seed STAFF có các permission quản lý catalog toàn cục. Vì chưa có guard theo rạp ở các API đó, JpaUserDetailsService giới hạn account admin_provisioned không có role ADMIN: giữ role và chỉ giữ permission booking:read (đọc booking owner theo contract hiện có). Không tự cấp movie:manage, inventory:manage, showtime:manage hoặc quyền quản trị toàn cục cho nhân viên mới. Áp dụng chung principal cho password và Google login; không đổi quyền STAFF legacy hoặc ADMIN. Các API nghiệp vụ Staff/POS tiếp theo phải bổ sung kiểm tra scope dữ liệu phía BE trước khi cấp thêm permission. Có assignment không đồng nghĩa đã có quyền gọi Booking management.

Trước khi tạo/đổi rạp, User Service đọc GET /api/v1/cinemas/{id} của Inventory, timeout connect 1s/read 2s, không theo redirect. Rạp không tồn tại: 404 USER_STAFF_CINEMA_NOT_FOUND; không active: 409 USER_STAFF_CINEMA_INACTIVE; không xác minh được: 503 USER_STAFF_CINEMA_UNAVAILABLE. Không truy cập DB Inventory và không thêm secret/service credential cho API catalog công khai này. Property cinema.user.inventory-base-url mặc định http://localhost:8083; có thể cấu hình bằng CINEMA_USER_INVENTORY_BASE_URL khi port/deployment khác.

Email/username đã có: 409 USER_STAFF_ACCOUNT_ALREADY_EXISTS, không tự chuyển khách thành STAFF. Chỉ tạo STAFF, không nhận role từ body. Audit STAFF_ACCOUNT_CREATED, USER_ROLE_ASSIGNED và STAFF_CINEMA_CHANGED theo cơ chế security audit hiện có. Nhân viên đã tồn tại từ trước nhưng chưa có assignment không được tự gán rạp hoặc quyền toàn cục; API mới chưa tự di chuyển dữ liệu này. List hiển thị các account có assignment, không suy ra assignment từ role STAFF.

### MySQL, OpenAPI và kiểm chứng bước nền tảng

V4\_\_staff_cinema_assignments.sql là migration mới của User DB: thêm users.admin_provisioned mặc định false cho dữ liệu cũ; bảng staff_cinema_assignments có unique user_id (một user chỉ có một rạp), FK tới users cho user và actor, index theo rạp/thời điểm và theo thời điểm. cinema_id không có FK liên DB. Đổi rạp cập nhật một row, audit giữ rạp cũ/mới; optimistic version và pessimistic lock bảo vệ cập nhật đồng thời.

Không tự tạo assignment cho STAFF cũ. Rạp có thể bị đổi trạng thái sau lần đọc API: đây là snapshot kiểm tra lúc gán, không phải khóa phân tán; API pickup/print tương lai phải xác minh lại rạp, trạng thái account/role và assignment hiện hành phía BE, không chỉ dùng claim cũ. Chưa thêm cinemaId vào JWT hoặc mở quyền bán/in của STAFF trong bước này.

Rollback cần dừng code mới, xuất/giữ assignment và audit trước khi bỏ bảng/cột mới; không xóa user/credential/profile/role đã tạo, không sửa V1–V3 hoặc reset các tài khoản nhân viên. Code cũ khi enable user chưa xác minh email có thể đưa về PENDING_VERIFICATION nên cần kế hoạch tương thích trước rollback. Chưa thực thi migration/rollback trên DB.

Controller/DTO mới được springdoc đọc qua cơ chế common-openapi sẵn có, không thêm Operation/ApiResponse/Schema annotation. Chưa khởi động service hoặc xuất /v3/api-docs; cần kiểm tra schema runtime sau triển khai V4 rồi mới generate client từ endpoint thật. Chưa generate client hay sửa FE.

Kiểm tra thủ công cần làm sau khi triển khai: ADMIN tạo STAFF ở rạp active và đăng nhập; USER/STAFF bị 403; trùng email/username bị 409; rạp sai/inactive/Inventory mất kết nối không tạo account; đổi rạp đồng thời vẫn chỉ một assignment và audit đúng; disable/enable rồi đăng nhập lại; list phân trang đúng scope lọc và không có credential. Không chạy test hoặc ghi DB trong lượt triển khai source này.

Kiểm tra source ngày 2026-11-20: `mvn -o "-Dmaven.repo.local=C:/Users/Administrator/.m2/repository" -pl services/user-service -am -DskipTests test-compile` BUILD SUCCESS cho 15 module (main và test source, không thực thi test). `git diff --check` không có lỗi whitespace. Chưa xác minh integration, migration, scope/permission hoặc schema OpenAPI ở runtime; các tiêu chí thủ công trên vẫn mở.

**Đọc kết quả sử dụng:** API owner đọc tickets và ADMIN lookup cùng trả thêm
checkedInAt, luôn hiện null nếu chưa dùng. Với USED, field chứa thời điểm nhận
vé gốc đã lưu, không cập nhật khi lookup/retry. Không trả actor nội bộ cho chủ
vé. TicketResponse.toString che QR; JSON owner vẫn trả payload theo contract
để render. Thay đổi là bổ sung field, không đổi endpoint hoặc migration V11.
FE cần đồng bộ client để hiển thị thời điểm sử dụng; chưa sửa FE.

## Check-in ADMIN — quy tắc mới nhất ngày 2026-11-17

Quyết định đã chốt: quét một lần cho cả capacity của vé, COUPLE nhận cả hai
người. Cửa sổ inclusive: startsAt - 30 phút <= serverNow <= startsAt + 15 phút.
Các ghi chú “chưa có check-in/chờ quyết định” bên dưới mô tả giai đoạn trước,
được thay thế bởi contract này; không có UI Staff hoặc quyền STAFF mới.

`POST /api/v1/bookings/management/tickets/check-in` nhận body qrPayload như
lookup. Cả Gateway và Booking dùng rule ADMIN hiện có. Response ApiResponse,
no-store gồm ticketId, bookingId, seatNumber, capacity, result và checkedInAt.
CHECKED_IN là lượt nhận đầu tiên; ALREADY_USED là vé đã nhận, không cho vào
lần nữa. Không trả token. Vé USED trả thời điểm gốc, không gọi Inventory lại.

Vé chưa dùng: đọc startsAt/endsAt/status qua API Inventory, ngoài transaction
ghi, timeout connect 1s/read 2s, không theo redirect. Không suy startsAt từ giá
trị khác hoặc dùng giờ FE. Thiếu/sai lịch, Inventory lỗi thì không check-in;
suất CANCELLED/COMPLETED/status không nhận diện bị chặn. CLOSED vẫn được xét
trong cửa sổ vì đóng bán không đồng nghĩa đóng check-in.

Sau đọc lịch: khóa booking rồi ticket trong transaction, kiểm tra CONFIRMED,
showtimeId không đổi và giờ server tại lúc giữ khóa. Chuyển ISSUED → USED,
lưu checkedInAt, checkedInBy và checkInShowtimeStartsAt. Quét đồng thời chỉ
một lượt CHECKED_IN. Retry sau commit trả ALREADY_USED. Ngoài cửa sổ trả 409
BOOKING_TICKET_CHECK_IN_WINDOW_CLOSED; suất không nhận vé trả 409
BOOKING_TICKET_CHECK_IN_NOT_ALLOWED; lỗi xác minh lịch trả system error
BOOKING_TICKET_SCHEDULE_UNAVAILABLE. Không có nhánh bỏ qua xác minh lịch.

V11 thêm ba cột audit/căn cứ cửa sổ, mở constraint ISSUED/USED và kiểm tra audit
đầy đủ cùng thời điểm trong cửa sổ cho USED. Triển khai V9 → V10 → V11 trước
service mới. Không thay migration cũ; rollback code trước, giữ dữ liệu vé USED,
không reset ISSUED hoặc thu hẹp constraint khi chưa bảo toàn vé đã sử dụng.
UUID actor không có FK liên DB User. Không đổi Kafka/Payment hoặc sửa FE.

Lịch Inventory đọc qua API là snapshot; chưa có khóa phân tán ngăn lịch bị đổi
hoặc hủy giữa đọc và commit. Kiểm chứng race này cùng ranh giới -30/+15 phút,
QR sai, quyền, replay/scan đồng thời và Inventory lỗi vẫn còn mở. Không chạy
test, migration hoặc check-in ghi DB trong lượt triển khai source.

## Check-in ADMIN — quy tắc mới nhất ngày 2026-11-17

Quyết định đã chốt: quét một lần cho cả capacity của vé, COUPLE nhận cả hai
người. Cửa sổ inclusive: startsAt - 30 phút <= serverNow <= startsAt + 15 phút.
Các ghi chú “chưa có check-in/chờ quyết định” bên dưới mô tả giai đoạn trước,
được thay thế bởi contract này; không có UI Staff hoặc quyền STAFF mới.

`POST /api/v1/bookings/management/tickets/check-in` nhận body qrPayload như
lookup. Cả Gateway và Booking dùng rule ADMIN hiện có. Response ApiResponse,
no-store gồm ticketId, bookingId, seatNumber, capacity, result và checkedInAt.
CHECKED_IN là lượt nhận đầu tiên; ALREADY_USED là vé đã nhận, không cho vào
lần nữa. Không trả token. Vé USED trả thời điểm gốc, không gọi Inventory lại.

Vé chưa dùng: đọc startsAt/endsAt/status qua API Inventory, ngoài transaction
ghi, timeout connect 1s/read 2s, không theo redirect. Không suy startsAt từ giá
trị khác hoặc dùng giờ FE. Thiếu/sai lịch, Inventory lỗi thì không check-in;
suất CANCELLED/COMPLETED/status không nhận diện bị chặn. CLOSED vẫn được xét
trong cửa sổ vì đóng bán không đồng nghĩa đóng check-in.

Sau đọc lịch: khóa booking rồi ticket trong transaction, kiểm tra CONFIRMED,
showtimeId không đổi và giờ server tại lúc giữ khóa. Chuyển ISSUED → USED,
lưu checkedInAt, checkedInBy và checkInShowtimeStartsAt. Quét đồng thời chỉ
một lượt CHECKED_IN. Retry sau commit trả ALREADY_USED. Ngoài cửa sổ trả 409
BOOKING_TICKET_CHECK_IN_WINDOW_CLOSED; suất không nhận vé trả 409
BOOKING_TICKET_CHECK_IN_NOT_ALLOWED; lỗi xác minh lịch trả system error
BOOKING_TICKET_SCHEDULE_UNAVAILABLE. Không có nhánh bỏ qua xác minh lịch.

V11 thêm ba cột audit/căn cứ cửa sổ, mở constraint ISSUED/USED và kiểm tra audit
đầy đủ cùng thời điểm trong cửa sổ cho USED. Triển khai V9 → V10 → V11 trước
service mới. Không thay migration cũ; rollback code trước, giữ dữ liệu vé USED,
không reset ISSUED hoặc thu hẹp constraint khi chưa bảo toàn vé đã sử dụng.
UUID actor không có FK liên DB User. Không đổi Kafka/Payment hoặc sửa FE.

Lịch Inventory đọc qua API là snapshot; chưa có khóa phân tán ngăn lịch bị đổi
hoặc hủy giữa đọc và commit. Kiểm chứng race này cùng ranh giới -30/+15 phút,
QR sai, quyền, replay/scan đồng thời và Inventory lỗi vẫn còn mở. Không chạy
test, migration hoặc check-in ghi DB trong lượt triển khai source.

Quyết định ngày 2026-11-17: một vé/QR theo mỗi booking-seat. COUPLE là một vé
capacity=2; STANDARD/VIP/ACCESSIBLE capacity=1. Không nhân đôi số vé ghế đôi.

## Luồng phát hành

Consumer đối chiếu snapshot event với dữ liệu đã lưu: totalAmount/currency,
confirmedAt cùng instant (bao gồm occurredAt envelope), paymentId UUID v7,
đủ tập ghế duy nhất, seatType/price/ticketGroupId từng ghế và tổng các dòng.
Sai dữ liệu bị từ chối trước khi tạo vé; processed marker rollback cùng
transaction nên không đánh dấu thành công cho event bị lỗi. So sánh tiền bằng
giá trị số và thời gian bằng instant, không lệ thuộc scale/offset hiển thị.
Không kiểm tra trực tiếp DB Payment; paymentId chỉ được kiểm tra định dạng,
chưa phải chứng minh tham chiếu giao dịch qua một nguồn Payment độc lập.

Booking đã CONFIRMED phát Outbox booking-confirmed version 1 như hiện tại.
Consumer group booking-ticket-issuance của Booking nhận event này độc lập với
Inventory. Trong một transaction: validate envelope/liên kết booking, đăng ký
processed-event, khóa booking, kiểm tra CONFIRMED, đọc snapshot ghế và lưu các
vé còn thiếu. Lỗi rollback cả processed marker và vé. UNIQUE booking_seat_id
và khóa booking chống phát hành trùng, kể cả replay với eventId khác. Ghế được
đọc từ dữ liệu Booking có thẩm quyền; không nhận giá/ghế từ FE để phát hành.

Phát hành là bất đồng bộ sau xác nhận; không khẳng định Inventory consumer đã
hoàn tất BOOKED chỉ vì vé đã phát hành. Luồng compensation/race của Inventory
vẫn cần kiểm chứng. Không có check-in hoặc gửi email trong bước này.

## Tra cứu QR chỉ đọc dành cho ADMIN

`POST /api/v1/bookings/management/tickets/lookup` nhận JSON object với trường
`qrPayload` lấy nguyên giá trị từ API vé của chủ booking. Không đặt QR trong URL.
Gateway/Booking yêu cầu ADMIN theo rule management hiện có. Kiểm tra prefix v1,
token base64url canonical 32 byte; payload sai trả validation error, token
đúng định dạng nhưng không tồn tại trả BOOKING_TICKET_NOT_FOUND.

Response ApiResponse/no-store có ticketId, bookingId, showtimeId, seatNumber,
capacity, ticketStatus, bookingStatus, showtimeEndsAt nullable. Không trả lại
token/QR hoặc dữ liệu cá nhân. LookupRequest.toString che QR. Không log payload.
Tra cứu không ghi DB, không consume vé, không tự đổi ISSUED hay trả cờ cho phép
vào rạp. Caller phải hiểu đây là đối chiếu dữ liệu; check-in/quyền Staff/thời
gian chấp nhận và hết hiệu lực chưa được triển khai. Index qr_token từ V9 được
dùng lại, không thêm migration trong bước lookup. Chưa sửa FE hoặc xác minh
runtime quyền/lookup; không chạy test.

## Khôi phục phát hành do ADMIN

`POST /api/v1/bookings/management/{bookingId}/tickets/issue?dryRun=true`.
Gateway và Booking đều áp dụng role ADMIN từ rule management hiện có.
Mặc định dryRun=true, chỉ kiểm tra snapshot/type và đếm thiếu, không sinh QR
hay ghi vé. dryRun=false tạo các vé còn thiếu trong một transaction. Chỉ chấp
nhận CONFIRMED; không hủy, thu tiền, đổi ghế hoặc đổi ID/QR của vé đã có.

Response ApiResponse có bookingId, dryRun, expectedTickets, existingTickets,
createdTickets, wouldCreateTickets. Không trả QR qua thao tác ADMIN này. Actor
lấy từ CurrentUser.id(), không nhận từ request. Cùng khóa booking với Kafka
consumer và unique booking-seat nên retry hoặc chạy đồng thời không tạo trùng.
Lỗi giữa chừng rollback cả lô vé trong booking; không cần sửa processed_events.

V10 thêm issuance_source/issued_by: Kafka tạo nguồn BOOKING_CONFIRMED_EVENT,
khôi phục tạo ADMIN_RECOVERY cùng UUID admin. createdAt sẵn có là thời điểm
phát hành; vé cũ giữ nguồn null thay vì giả actor. CHECK chặn source/actor không
khớp. Đây không phải phát hành lại/rotate QR hay tự phát hành hàng loạt.
Apply V9 rồi V10 trước service mới. Rollback code trước, giữ bảng/cột audit;
không xóa vé để làm lại. Không có FK liên DB User Service cho UUID actor.

## API đọc cho chủ sở hữu

`GET /api/v1/bookings/{bookingId}/tickets` trả ApiResponse chứa array, no-store.
Mỗi vé có id, bookingId, bookingSeatId, seatNumber, capacity, status, qrPayload.
JWT/principal của chủ booking được kiểm tra bằng findByIdAndUserId; không đúng
chủ hoặc không tồn tại trả BOOKING_NOT_FOUND. Gateway/Booking giữ rule xác thực
hiện tại; không mở public lookup. GET không phát hành hoặc sinh lại token.

Chưa phát hành trả danh sách rỗng; FE không được suy ra rỗng là đã gửi vé hoặc
thanh toán thất bại. Chỉ thông báo có vé sau khi response chứa vé đã lưu.
FE encode nguyên chuỗi qrPayload thành QR, không tự tạo từ bookingId.

qrPayload có prefix `cinema-ticket:v1:` và token random 256 bit, lưu một lần
với unique index phân biệt hoa/thường. Token không chứa email, giá, quyền hoặc
thông tin cá nhân. Không ghi QR/token vào log, URL query hay analytics; chỉ
trả qua endpoint owner. Đây là payload định danh opaque, chưa có API quét/
xác thực/check-in. Không coi QR đã render là bằng chứng được kiểm soát vào rạp.
ISSUED là trạng thái duy nhất ở bước này. Hết hiệu lực/hủy/sử dụng và các
trạng thái tương ứng cần quyết định nghiệp vụ và migration ở bước sau.

## MySQL và triển khai

V9 thêm booking_tickets trong DB Booking: FK đến bookings/booking_seats,
unique booking_seat_id và qr_token, index booking_id, CHECK capacity/status.
Không sửa migration trước, không đổi Payment/event contract. Áp dụng V9 trước
service mới. Bật BOOKING_KAFKA_ENABLED để consumer chạy; cần topic
booking-confirmed và retry/DLT theo cấu hình Booking hiện tại. Có thể cấu hình
cinema.booking.kafka.topics.booking-confirmed và consumer-groups.ticket-issuance.

Rollback service trước và giữ bảng để bảo toàn vé/QR. Không drop bảng hoặc
replay nhằm đổi token đã phát. Booking CONFIRMED lịch sử chỉ có vé khi event
booking-confirmed còn trong Kafka được consumer group mới nhận; chưa có backfill
API tự động theo lô hoặc bảo đảm mọi đơn cũ được phát hành. ADMIN có thể dùng
thao tác khôi phục từng booking mô tả trên. Không sửa processed_events bằng SQL
để giả lập replay. Kafka retention/DLT cần kiểm tra khi triển khai.

## Bước check-in đang chờ quyết định

Source hiện chỉ có ISSUED và lookup chỉ đọc. Trước khi thêm thao tác consume vé
cần chốt: ghế đôi dùng một lượt cho cả cặp hay hai lượt riêng, cùng cửa sổ thời
gian được vào rạp. Nếu cửa sổ phụ thuộc startsAt, snapshot hiện chỉ có endsAt
là chưa đủ; không suy giờ bắt đầu từ duration hoặc giờ FE gửi.

Thiết kế cần bảo toàn: nhận QR qua body, kiểm tra booking CONFIRMED và lịch có
thẩm quyền; khóa theo thứ tự booking rồi ticket thống nhất với issuance/recovery.
Trong một transaction cập nhật trạng thái/lượt dùng và lưu thời điểm/actor.
Quét đồng thời không được chấp nhận vượt capacity; retry không tạo lượt vào
thứ hai. Cần migration mới sau V10 cho trạng thái/audit check-in, không sửa V9.
Khởi đầu chỉ dùng quyền quản trị hiện có; không tự mở rộng quyền STAFF hoặc
UI Staff chỉ vì role STAFF đã có trong User Service. Các điểm này là thiết kế
chờ quyết định, chưa có API check-in hay thay đổi quyền ở runtime.

## Trạng thái kiểm chứng

Maven offline compile Booking cùng các module phụ thuộc với `-DskipTests` đã
BUILD SUCCESS. Lần chạy sandbox gặp AccessDeniedException với JAR cache;
chạy ngoài sandbox sau phê duyệt đã thành công. Không đổi dependency/cache.

Chưa chạy migration, test, phát hành trên DB, kiểm tra owner/cạnh tranh/Kafka
hoặc sửa FE. Các bước thủ công cần môi trường phù hợp: một ghế một vé, COUPLE
capacity 2, replay không đổi ID/QR, GET owner/khác owner, transaction rollback,
đơn chưa CONFIRMED không được phát hành và retry/DLT khi event sai.
