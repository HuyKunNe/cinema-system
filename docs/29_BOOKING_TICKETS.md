# Vé theo đơn vị ghế — BE-U04

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

## Trạng thái kiểm chứng

Maven offline compile Booking cùng các module phụ thuộc với `-DskipTests` đã
BUILD SUCCESS. Lần chạy sandbox gặp AccessDeniedException với JAR cache;
chạy ngoài sandbox sau phê duyệt đã thành công. Không đổi dependency/cache.

Chưa chạy migration, test, phát hành trên DB, kiểm tra owner/cạnh tranh/Kafka
hoặc sửa FE. Các bước thủ công cần môi trường phù hợp: một ghế một vé, COUPLE
capacity 2, replay không đổi ID/QR, GET owner/khác owner, transaction rollback,
đơn chưa CONFIRMED không được phát hành và retry/DLT khi event sai.
