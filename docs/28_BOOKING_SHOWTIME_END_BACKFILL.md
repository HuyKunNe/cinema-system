# Backfill giờ kết thúc cho booking cũ

Ngày triển khai source: 2026-11-16. Chưa chạy migration hoặc backfill runtime.

Chủ dự án đã cho phép dùng `endsAt` hiện tại của Inventory để phân loại booking
cũ. Giá trị này không chứng minh lịch tại thời điểm mua. Chỉ điền trường null;
không đổi trạng thái, tiền, ghế hoặc dữ liệu lịch đã có.

## API và quyền

`POST /api/v1/bookings/management/showtime-end-backfill`

Gateway và Booking Service cùng yêu cầu ADMIN theo rule management hiện có.
Actor lấy từ principal; không nhận actor/userId từ request. Không gửi token
trong URL. Response dùng ApiResponse, Cache-Control no-store.

| Tham số | Mặc định | Ý nghĩa                                              |
| ------- | -------- | ---------------------------------------------------- |
| dryRun  | true     | Chỉ đọc lịch và báo WOULD_UPDATE; false cho phép ghi |
| size    | 10       | 1–20 booking mỗi lô                                  |
| afterId | bỏ trống | Cursor UUID v7 từ nextCursor của lô trước            |

Preview: `POST /api/v1/bookings/management/showtime-end-backfill?dryRun=true&size=10`.
Apply: cùng URL với `dryRun=false`. Bắt đầu apply không truyền cursor từ lần
preview cuối; cursor chỉ dùng để đi tiếp trong cùng lượt quét preview hoặc apply.

Data response chứa dryRun, nextCursor (null khi hết lô) và rows. Mỗi row chứa
bookingId, showtimeId, endsAt nullable và result: WOULD_UPDATE, UPDATED,
ALREADY_SET, NOT_FOUND, SHOWTIME_CHANGED, INVALID_SCHEDULE, SHOWTIME_NOT_FOUND
hoặc INVENTORY_UNAVAILABLE. nextCursor là ID cuối được xét nếu còn dữ liệu;
các booking lỗi có thể được thử lại bằng lượt quét mới bắt đầu không cursor.
Đây không phải snapshot bất biến giữa nhiều request; nếu phát sinh row mới có
ID trước cursor, lượt quét mới sẽ lấy lại những row còn null đó.

## Transaction và nguồn dữ liệu

Chỉ đọc `GET /api/v1/showtimes/{id}` công khai của Inventory, kiểm tra đúng ID,
startsAt/endsAt hợp lệ và end > start. Không đọc DB service khác. Cache theo
showtime chỉ tồn tại trong một lô; timeout connect 2 giây/read 3 giây; không tự
theo redirect. `BOOKING_INVENTORY_BASE_URL` cấu hình địa chỉ service (mặc định
local port 8083); không dùng input request làm địa chỉ đích.

HTTP diễn ra ngoài transaction ghi. Mỗi booking ghi trong transaction riêng,
khóa lại row và kiểm tra showtimeId/giá trị còn null. Event v2 hoặc lô khác đã
điền trước thì không ghi đè. Nếu lỗi DB làm request thất bại, các row đã commit
vẫn giữ nguyên; gọi lại từ đầu an toàn vì truy vấn chỉ lấy các row còn null.

Backfill ghi nguồn CURRENT_SCHEDULE_BACKFILL, thời điểm từ systemClock và UUID
admin. Reservation event ghi nguồn RESERVATION_EVENT. Migration không giả thời
điểm ghi cho snapshot cũ. Cột provenance là audit nội bộ, chưa thêm vào public
BookingResponse. Bộ lọc period dùng giá trị đã lưu; backfill không tự theo dõi
mọi thay đổi lịch tiếp theo.

## MySQL và rollout

Áp dụng Booking V6 rồi V7 trước khi chạy service mới (ddl-auto validate). V7
thêm ba cột nullable nguồn/thời điểm/actor và đánh dấu RESERVATION_EVENT cho
row đã có endsAt; không điền endsAt còn thiếu. Không đổi FK hoặc giá tiền.
Rollback code trước; có thể giữ cột thêm. Nếu cần drop cột, bảo toàn audit trước
và chỉ drop sau khi không còn instance phụ thuộc. Không tự đặt lại endsAt null
để rollback vì có thể xóa snapshot hợp lệ đến từ event sau đó.

## Kiểm tra thủ công khi triển khai

Kiểm tra quyền ADMIN, preview không ghi, apply chỉ điền null, source/actor/thời
điểm đúng, lặp request không ghi đè, 404/timeout không chặn row khác, cursor và
period phân trang sau apply. Kiểm tra chạy đồng thời với event giữ ghế trong môi
trường phù hợp. Các bước này chưa được chạy; không chạy test tự động trong lượt
triển khai source. Không có thay đổi FE hay giao dịch thanh toán.
