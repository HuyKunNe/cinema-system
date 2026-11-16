# Backfill giờ kết thúc cho booking cũ

Ngày triển khai source: 2026-11-16. Chưa chạy migration hoặc backfill runtime.

## Quan sát local ngày 2026-11-16

Sau lô apply đầu tiên do chủ dự án chạy qua Swagger, kiểm tra DB chỉ đọc xác
nhận 10 booking có nguồn CURRENT_SCHEDULE_BACKFILL (2 CONFIRMED, 1 CANCELLED,
3 EXPIRED, 4 PENDING), không row backfill nào thiếu giờ/thời điểm/actor. V8
constraint hiện đã có trong DB. Còn 3 CONFIRMED thiếu endsAt, ngoài một CONFIRMED
đã có snapshot RESERVATION_EVENT. Chưa hoàn tất toàn bộ backfill. Response lô
đầu trả nextCursor `01a1d300-9007-7ed8-ae24-b6c3c4884070`; tiếp tục apply từ
cursor này, rồi kiểm tra lại số row null và kết quả query period theo owner.
Đây là quan sát tiếp theo, thay thế số liệu trước apply ở đoạn bên dưới.

Rà soát chỉ đọc: OpenAPI Booking tại port 8084 có tham số period và endpoint
backfill. Flyway DB local ghi nhận V1–V7 thành công; đủ bốn cột giờ kết thúc/
provenance, chưa có constraint V8. Có 13 booking thiếu endsAt, gồm 5 CONFIRMED,
1 CANCELLED, 3 EXPIRED và 4 PENDING; một CONFIRMED khác có nguồn RESERVATION_EVENT.
Đây là số liệu tại thời điểm rà soát, không phải kết quả backfill hoặc xác nhận
API phân trang/ownership. Không truy vấn dữ liệu cá nhân hoặc in secret.

Bước triển khai còn lại: áp dụng V8 qua quy trình migration được duyệt; admin
gọi preview, kiểm tra kết quả rồi apply từng lô và đi tiếp bằng nextCursor.
Không lấy việc OpenAPI có endpoint làm bằng chứng endpoint ghi đã hoạt động.
Lượt rà soát không chạy migration, preview/apply có xác thực hoặc ghi DB.

## Quan sát local ngày 2026-11-16

Rà soát chỉ đọc: OpenAPI Booking tại port 8084 có tham số period và endpoint
backfill. Flyway DB local ghi nhận V1–V7 thành công; đủ bốn cột giờ kết thúc/
provenance, chưa có constraint V8. Có 13 booking thiếu endsAt, gồm 5 CONFIRMED,
1 CANCELLED, 3 EXPIRED và 4 PENDING; một CONFIRMED khác có nguồn RESERVATION_EVENT.
Đây là số liệu tại thời điểm rà soát, không phải kết quả backfill hoặc xác nhận
API phân trang/ownership. Không truy vấn dữ liệu cá nhân hoặc in secret.

Bước triển khai còn lại: áp dụng V8 qua quy trình migration được duyệt; admin
gọi preview, kiểm tra kết quả rồi apply từng lô và đi tiếp bằng nextCursor.
Không lấy việc OpenAPI có endpoint làm bằng chứng endpoint ghi đã hoạt động.
Lượt rà soát không chạy migration, preview/apply có xác thực hoặc ghi DB.

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

Data response chứa dryRun, nextCursor (null khi hết lô), rows và budgetLimited.
Mỗi request có ngân sách xử lý mềm 3 giây; sau khi xử lý ít nhất một row,
service dừng trước row tiếp theo nếu đã hết ngân sách. Lô có thể ít hơn size;
budgetLimited=true chỉ rõ lý do. Cursor luôn là row đã xét cuối cùng, không
nhảy qua những row chưa xử lý. Cứ tiếp tục theo nextCursor cho đến khi null.
Ngân sách này không ngắt HTTP/transaction đang chạy và không bảo đảm deadline
cứng nếu DB chậm; client vẫn cần hỗ trợ retry an toàn sau Gateway timeout.
Mỗi row chứa
bookingId, showtimeId, endsAt nullable và result: WOULD_UPDATE, UPDATED,
ALREADY_SET, NOT_FOUND, SHOWTIME_CHANGED, INVALID_SCHEDULE, SHOWTIME_NOT_FOUND
hoặc INVENTORY_UNAVAILABLE. nextCursor là ID cuối được xét nếu còn dữ liệu;
các booking lỗi có thể được thử lại bằng lượt quét mới bắt đầu không cursor.
Đây không phải snapshot bất biến giữa nhiều request; nếu phát sinh row mới có
ID trước cursor, lượt quét mới sẽ lấy lại những row còn null đó.

## Transaction và nguồn dữ liệu

Chỉ đọc `GET /api/v1/showtimes/{id}` công khai của Inventory, kiểm tra đúng ID,
startsAt/endsAt hợp lệ và end > start. Không đọc DB service khác. Cache theo
showtime chỉ tồn tại trong một lô; timeout connect 1 giây/read 2 giây; không tự
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

V8 bổ sung CHECK cho ba trạng thái dữ liệu hợp lệ: chưa biết giờ (tất cả trường
liên quan null), snapshot event (có giờ/nguồn RESERVATION_EVENT, không ghi audit
backfill), hoặc backfill (có giờ/nguồn CURRENT_SCHEDULE_BACKFILL/thời điểm/UUID
actor). Các nhánh có kiểm tra IS NOT NULL để tránh SQL UNKNOWN cho qua ràng
buộc. Không thay index/FK hoặc tự sửa row không nhất quán. Dùng MySQL hỗ trợ
CHECK được enforce như các constraint đã có trong baseline dự án.

Trước khi áp dụng V8, có thể xem trước row vi phạm bằng truy vấn chỉ đọc sau.
Nếu có kết quả, kiểm tra nguồn thực tế; không tự điền nguồn hoặc actor giả.

```sql
SELECT HEX(id) AS booking_id, showtime_ends_at, showtime_end_source,
       showtime_end_recorded_at, showtime_end_recorded_by
FROM bookings
WHERE NOT (
    (showtime_ends_at IS NULL AND showtime_end_source IS NULL
        AND showtime_end_recorded_at IS NULL AND showtime_end_recorded_by IS NULL)
    OR
    (showtime_ends_at IS NOT NULL AND showtime_end_source IS NOT NULL
        AND showtime_end_source = 'RESERVATION_EVENT'
        AND showtime_end_recorded_at IS NULL AND showtime_end_recorded_by IS NULL)
    OR
    (showtime_ends_at IS NOT NULL AND showtime_end_source IS NOT NULL
        AND showtime_end_source = 'CURRENT_SCHEDULE_BACKFILL'
        AND showtime_end_recorded_at IS NOT NULL AND showtime_end_recorded_by IS NOT NULL
        AND REGEXP_LIKE(showtime_end_recorded_by,
            '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$'))
);
```

Apply V8 sau V7. Nếu dữ liệu vi phạm, migration dừng thay vì sửa dữ liệu tự động.
DDL MySQL có implicit commit; lên kế hoạch triển khai riêng, không dựa vào
rollback transaction Flyway. Nếu cần rollback riêng constraint V8, dùng
`ALTER TABLE bookings DROP CHECK chk_bookings_showtime_end_provenance;` sau
khi kiểm tra phụ thuộc. Không xóa các cột provenance hoặc dữ liệu đã backfill.

V8 bổ sung CHECK cho ba trạng thái dữ liệu hợp lệ: chưa biết giờ (tất cả trường
liên quan null), snapshot event (có giờ/nguồn RESERVATION_EVENT, không ghi audit
backfill), hoặc backfill (có giờ/nguồn CURRENT_SCHEDULE_BACKFILL/thời điểm/UUID
actor). Các nhánh có kiểm tra IS NOT NULL để tránh SQL UNKNOWN cho qua ràng
buộc. Không thay index/FK hoặc tự sửa row không nhất quán. Dùng MySQL hỗ trợ
CHECK được enforce như các constraint đã có trong baseline dự án.

Trước khi áp dụng V8, có thể xem trước row vi phạm bằng truy vấn chỉ đọc sau.
Nếu có kết quả, kiểm tra nguồn thực tế; không tự điền nguồn hoặc actor giả.

```sql
SELECT HEX(id) AS booking_id, showtime_ends_at, showtime_end_source,
       showtime_end_recorded_at, showtime_end_recorded_by
FROM bookings
WHERE NOT (
    (showtime_ends_at IS NULL AND showtime_end_source IS NULL
        AND showtime_end_recorded_at IS NULL AND showtime_end_recorded_by IS NULL)
    OR
    (showtime_ends_at IS NOT NULL AND showtime_end_source IS NOT NULL
        AND showtime_end_source = 'RESERVATION_EVENT'
        AND showtime_end_recorded_at IS NULL AND showtime_end_recorded_by IS NULL)
    OR
    (showtime_ends_at IS NOT NULL AND showtime_end_source IS NOT NULL
        AND showtime_end_source = 'CURRENT_SCHEDULE_BACKFILL'
        AND showtime_end_recorded_at IS NOT NULL AND showtime_end_recorded_by IS NOT NULL
        AND REGEXP_LIKE(showtime_end_recorded_by,
            '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$'))
);
```

Apply V8 sau V7. Nếu dữ liệu vi phạm, migration dừng thay vì sửa dữ liệu tự động.
DDL MySQL có implicit commit; lên kế hoạch triển khai riêng, không dựa vào
rollback transaction Flyway. Nếu cần rollback riêng constraint V8, dùng
`ALTER TABLE bookings DROP CHECK chk_bookings_showtime_end_provenance;` sau
khi kiểm tra phụ thuộc. Không xóa các cột provenance hoặc dữ liệu đã backfill.

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
