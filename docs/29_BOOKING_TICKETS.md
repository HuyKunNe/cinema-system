# Vé theo đơn vị ghế — BE-U04

Quyết định ngày 2026-11-17: một vé/QR theo mỗi booking-seat. COUPLE là một vé
capacity=2; STANDARD/VIP/ACCESSIBLE capacity=1. Không nhân đôi số vé ghế đôi.

## Luồng phát hành

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
API hoặc bảo đảm mọi đơn cũ được phát hành. Không sửa processed_events bằng SQL
để giả lập replay. Kafka retention/DLT cần kiểm tra khi triển khai.

## Trạng thái kiểm chứng

Maven offline compile Booking cùng các module phụ thuộc với `-DskipTests` đã
BUILD SUCCESS. Lần chạy sandbox gặp AccessDeniedException với JAR cache;
chạy ngoài sandbox sau phê duyệt đã thành công. Không đổi dependency/cache.

Chưa chạy migration, test, phát hành trên DB, kiểm tra owner/cạnh tranh/Kafka
hoặc sửa FE. Các bước thủ công cần môi trường phù hợp: một ghế một vé, COUPLE
capacity 2, replay không đổi ID/QR, GET owner/khác owner, transaction rollback,
đơn chưa CONFIRMED không được phát hành và retry/DLT khi event sai.
