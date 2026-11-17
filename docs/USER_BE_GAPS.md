# Các phần BE còn thiếu để hoàn thiện giao diện User

Ngày đối chiếu: **2026-11-16**.

Baseline BE: commit `0d35d8c`, đối chiếu source trong checkout; tài liệu này
đang là file mới chưa được Git theo dõi. Kết quả dưới đây không phải chứng nhận
runtime, build, migration hay tích hợp nhà cung cấp thanh toán.

## Phạm vi và cách đọc

Danh sách nhu cầu ban đầu dựa trên thiết kế `design/reference/lumiere-cinema`.
Lượt rà soát này đối chiếu với generated API client và source
trong checkout FE và controller/DTO tại `E:/java/cinema-system/services`.
Danh sách này phục vụ trao đổi và lập kế hoạch BE, không khẳng định các API đề
xuất đã tồn tại. BE quyết định endpoint, service sở hữu, DTO và enum chính thức,
cập nhật OpenAPI rồi FE mới regenerate client và tích hợp.

Chỉ ghi nhận thiếu ở contract đang công khai; không kết luận rằng toàn bộ xử lý
nội bộ của BE chưa tồn tại. Không thay BE/MySQL, không tạo endpoint hay dữ liệu
giả ở FE. Các mức ưu tiên dưới đây là đề xuất, cần thống nhất trước khi triển khai.

## 1. Các contract còn thiếu

### BE-U01 — Danh mục và đặt bắp nước/combo

- **Màn hình:** `#/booking/food`, tóm tắt checkout, đơn và vé.
- **Hiện tại:** chưa thấy public controller/client cho sản phẩm bắp nước;
  CreateBookingRequest chưa nhận các dòng sản phẩm.
- **Cần BE cung cấp:** danh sách theo rạp, thông tin sản phẩm/combo, ảnh, thành
  phần, đơn vị bán, giá/tiền tệ, trạng thái khả dụng và giới hạn số lượng nếu có.
  Có contract báo giá và lưu số lượng, đơn giá, thành tiền vào đơn.
- **Cần chốt:** quản lý tồn kho hay chỉ trạng thái khả dụng; thời điểm giữ/trừ
  hàng; xử lý khi sản phẩm đổi giá/ngừng bán; giải phóng khi đơn thất bại/hết hạn.
- **Hoàn tất khi:** tổng thanh toán do BE xác nhận bao gồm cả vé và sản phẩm;
  đọc lại đơn trả được các dòng đã mua, không dựa vào giá danh mục hiện tại.

### BE-U02 — Ưu đãi và mã giảm giá

- **Màn hình:** danh sách/chi tiết ưu đãi, áp dụng mã ở checkout.
- **Hiện tại:** FE chỉ có trang empty state; chưa có public contract ưu đãi,
  kiểm tra mã hoặc kết quả giảm giá trong booking/payment response.
- **Cần BE cung cấp:** nội dung ưu đãi công khai, thời gian hiệu lực, điều kiện;
  kiểm tra mã theo suất/rạp, ghế/nhóm vé, giỏ sản phẩm và người dùng; trả số tiền
  giảm, giá cuối cùng và thông báo không áp dụng được.
- **Cần chốt:** cộng dồn, số lượt dùng, phạm vi giảm, điều kiện thành viên và
  cách hoàn lượt khi đơn thất bại/hủy/hết hạn. Không lấy mã/quy tắc mẫu của
  reference làm chính sách thật.
- **Hoàn tất khi:** BE kiểm tra lại lúc tạo đơn, lưu ưu đãi đã dùng và bảo đảm
  payment dùng đúng số tiền cuối cùng; retry cùng yêu cầu không tiêu lượt hai lần.

### BE-U03 — Thông tin người nhận và gửi vé

- **Màn hình:** form người nhận tại checkout, chi tiết đơn/vé.
- **Hiện tại:** hồ sơ hiện có email/họ tên/điện thoại nhưng CreateBookingRequest
  và BookingResponse chưa có snapshot người nhận của đơn. Hồ sơ không đồng nghĩa
  với dữ liệu đã được xác nhận ở thời điểm mua.
- **Cần BE cung cấp:** contract nhận, kiểm tra và lưu thông tin người nhận; trả
  lại phần dữ liệu được phép hiển thị cho chủ sở hữu. Chốt trường bắt buộc/tùy chọn.
- **Cần chốt:** có cho mua hộ hay không; gửi vé qua kênh nào, thời điểm gửi,
  trạng thái gửi và cách gửi lại nếu sản phẩm hỗ trợ.
- **Hoàn tất khi:** sửa hồ sơ sau đó không làm thay đổi người nhận của đơn cũ;
  FE chỉ thông báo đã gửi vé nếu có xác nhận tương ứng từ hệ thống.

### BE-U04 — Phát hành vé điện tử, QR và trạng thái sử dụng

**Triển khai source ngày 2026-11-17:** một vé mỗi booking-seat, COUPLE một vé
capacity=2. V9 thêm bảng vé; consumer booking-confirmed phát hành bất đồng bộ
và chống trùng theo seat. API owner đọc QR opaque đã lưu. Chi tiết tại
[contract vé](29_BOOKING_TICKETS.md). Chưa có check-in, gửi vé, expiry/void
workflow hoặc backfill bảo đảm mọi booking lịch sử; chưa đóng U04.

- **Màn hình:** result thành công, vé điện tử, in vé và Vé của tôi.
- **Hiện tại:** booking CONFIRMED đã có ghế/giá, nhưng public response chưa có
  mã vé/QR hay trạng thái sử dụng. FE đang hiển thị mã đơn, không giả QR.
- **Cần BE cung cấp:** định danh vé, nội dung QR hợp lệ hoặc tài nguyên QR do
  hệ thống cung cấp, liên hệ với booking/ghế và trạng thái phát hành/sử dụng.
- **Cần chốt:** một vé theo đơn hay theo đơn vị ghế; ghế đôi; điều kiện phát
  hành; trạng thái hủy/hết hiệu lực; dữ liệu nào được đưa vào QR.
- **Hoàn tất khi:** chỉ vé hợp lệ được phát hành sau xác nhận nghiệp vụ; đọc lại
  không phát hành trùng; QR và trạng thái hiển thị khớp cơ chế kiểm soát vé thật.
  Phần quét/check-in cần phối hợp riêng với BE/Staff, không tự mở rộng UI Staff.

### BE-U05 — Đặt và tra cứu vé không đăng nhập

- **Màn hình:** booking khách vãng lai và `#/lookup` trong reference.
- **Hiện tại:** BookingController dùng CurrentUser.id() khi tạo/đọc đơn;
  route FE tương ứng yêu cầu xác thực. Chưa có contract guest lookup.
- **Cần quyết định trước:** sản phẩm có hỗ trợ khách vãng lai hay tiếp tục yêu
  cầu tài khoản. Nếu không hỗ trợ, đây là khác biệt được chấp nhận với reference.
- **Nếu hỗ trợ:** BE cần contract tạo đơn khách vãng lai, xác minh quyền xem
  đơn/vé, phiên truy cập và quy tắc gắn đơn vào tài khoản sau này nếu có.
- **Hoàn tất khi:** không thể xem vé chỉ nhờ biết bookingId; quy định cách xác
  minh, thời hạn truy cập và giới hạn thử. Không mặc định mã đơn + email/điện
  thoại của bản demo là đủ cho hệ thống thật.

### BE-U06 — Bộ lọc cho các tab Vé của tôi

**Quan sát local chỉ đọc:** DB hiện áp dụng V1–V7, chưa có constraint V8;
OpenAPI port 8084 đã có period và API backfill. Còn 13 booking thiếu endsAt,
trong đó 5 CONFIRMED nên không thuộc hai tab thời gian cho đến khi backfill.
Không coi đây là lỗi phân loại bằng ngày hiện tại của FE. Chưa gọi preview/
apply có xác thực hoặc xác minh count/content theo tài khoản. Số liệu có thể
thay đổi sau thời điểm rà soát; không chạy test hay ghi DB trong bước này.

**Bảo toàn audit:** migration V8 ràng buộc nguồn/thời điểm/actor khớp với giờ
kết thúc bằng MySQL CHECK; không sửa V6/V7. Có SQL chỉ đọc kiểm tra row không
nhất quán và hướng dẫn rollback constraint trong tài liệu backfill. Chưa chạy
migration hoặc kiểm chứng constraint trên DB; không đánh dấu U06 hoàn tất.

**Backfill đã được duyệt và triển khai source:** dùng endsAt hiện tại từ API
Inventory cho booking còn null, lưu nguồn/thời điểm/admin bằng V7; API ADMIN
có dry-run mặc định, cursor, giới hạn lô và khóa kiểm tra trước khi ghi.
Chi tiết contract/rollout tại [hướng dẫn backfill](28_BOOKING_SHOWTIME_END_BACKFILL.md).
Chưa chạy migration, apply backfill hay kiểm chứng runtime; kế hoạch chờ duyệt
bên dưới là lịch sử, phương án 1 đã được chủ dự án chọn.

**Kế hoạch backfill cần duyệt:** dữ liệu lịch sử thiếu showtimeEndsAt không thể
khôi phục giờ kết thúc lúc mua chỉ từ `expiresAt` hoặc thời điểm thanh toán.
Inventory GET showtime chỉ cung cấp lịch hiện tại. Vì vậy chưa tự backfill bằng
giá trị này như thể đó là snapshot lịch sử. Hai phương án:

1. Cho phép dùng endsAt hiện tại từ API Inventory để phân loại booking cũ,
   đồng thời ghi nhận nguồn là lịch hiện tại, không khẳng định lịch lúc mua.
2. Giữ null cho tới khi có nguồn lịch sử được xác minh; booking vẫn đọc được
   trong danh sách không period nhưng không xuất hiện ở UPCOMING/PAST.

Nếu chọn phương án 1, triển khai công cụ/service theo lô có dry-run, phân trang
bằng cursor, đọc Inventory ngoài transaction Booking, kiểm tra showtimeId và
thời gian hợp lệ. Khi ghi, khóa lại Booking, chỉ cập nhật nếu giá trị vẫn null;
không đổi status, tiền, ghế hoặc ghi đè snapshot nhận từ event v2. Retry không
được ghi đè; lỗi/404 từng suất được báo cáo và không chặn toàn bộ lô. Không
query trực tiếp DB Inventory. Cần lưu dấu nguồn/thời điểm backfill nếu nghiệp
vụ duyệt sử dụng lịch hiện tại; schema nguồn dữ liệu chưa được triển khai.

**Rà soát lịch/đồng hồ:** API sửa suất hiện chỉ cho đổi lịch ở SCHEDULED
(`ShowtimeServiceImpl.update`). Bộ lọc period dùng bean `systemClock` của
Booking, chụp một cutoff cho content/count. Không tự thêm luồng đổi lịch suất
đã mở bán; backfill lịch sử và thay đổi lịch ngoài luồng này vẫn chưa giải quyết.

**Triển khai truy vấn period ngày 2026-11-16:** GET danh sách nhận tùy chọn
`period=UPCOMING|PAST`. Hai nhóm chỉ lấy CONFIRMED có showtimeEndsAt; UPCOMING
là end > thời gian UTC server, PAST là end <= thời gian đó. Chụp cutoff một lần
cho content/count trong request. Bộ lọc owner, status và period thực thi trong
DB trước pagination; status bổ sung giao với CONFIRMED nên status mâu thuẫn cho
trang rỗng. Bỏ period giữ truy vấn cũ. Null lịch sử không thuộc hai nhóm nhưng
vẫn hiện trong danh sách không lọc period. Đây là phân loại theo snapshot lúc
giữ ghế, chưa xử lý đồng bộ đổi lịch/backfill hoặc check-in. Chưa đóng U06.

**Tiếp tục đồng bộ snapshot:** Inventory đọc `Showtime.endsAt` khi tạo Outbox
`seat-reserved` version 2; Booking chấp nhận version 1/2 và lưu `showtimeEndsAt`
trong cùng transaction hoàn tất reservation. Version 2 thiếu thời gian hoặc
kết thúc không sau heldAt bị từ chối. Event version 1 giữ null cho booking cũ.
Snapshot này ghi nhận lịch tại thời điểm giữ ghế; chưa đồng bộ thay đổi lịch
sau đó, chưa backfill lịch sử. Truy vấn period đã được bổ sung ở bước tiếp theo.
Rollout: áp dụng V6, triển khai Booking consumer trước Inventory producer.
Chưa xác minh Kafka/runtime; không coi bước này là đóng U06.

**Nền tảng persistence ngày 2026-11-16:** migration Booking V6 thêm nullable
`showtime_ends_at` và index `(user_id, status, showtime_ends_at, id)`. Entity và
BookingResponse expose `showtimeEndsAt`; giữ constructor response cũ tương thích.
Ở bước persistence ban đầu chưa có cơ chế ghi giá trị. Source hiện đã bổ sung
event version 2 và truy vấn period; booking cũ/v1 vẫn có thể giữ null. Chưa đóng U06.
Không dùng `expiresAt` giữ ghế để suy ra giờ kết thúc phim.

V6 phải áp dụng trước khi chạy Booking Service có entity mới (ddl-auto validate).
Không backfill giả và không sửa baseline. Khi rollback, triển khai lại service
cũ trước; cột/index thêm có thể giữ để tránh mất dữ liệu. Chỉ drop index/cột
sau khi xác nhận không còn code phụ thuộc và đã bảo toàn dữ liệu cần thiết.

**Kiểm tra OpenAPI local ngày 2026-11-16:** đọc `localhost:8084/v3/api-docs`
xác nhận `status` là array enum tùy chọn trên GET danh sách Booking, page/size
có default và giới hạn đúng với controller. Chưa gọi truy vấn có xác thực để
xác nhận nội dung/count; chưa kiểm chứng tab theo thời gian. Hướng dẫn serialize
tham số, cache key và reset page nằm trong `19_FE_API_CONTRACT.md`.

**Bổ sung source ngày 2026-11-16:** `GET /api/v1/bookings` nhận `status` tùy
chọn (một hoặc nhiều BookingStatus qua tham số lặp lại). Lọc theo principal và trạng thái trước pagination,
bao gồm tổng bản ghi. Không truyền status giữ hành vi cũ. FE client chưa
regenerate; chưa xác minh runtime. Đây chưa phải các tab theo thời gian suất.

- **Màn hình:** `#/tickets` với Sắp tới / Đã qua / Chưa hoàn tất.
- **Hiện tại:** BE đã có bộ lọc status; generated client FE cần đồng bộ theo
  OpenAPI mới. BookingResponse chỉ có
  showtimeId, không có thời gian suất chiếu hoặc trạng thái sử dụng vé.
- **Cần BE cung cấp:** danh sách phân trang có lọc theo quy tắc tab đã thống
  nhất; tổng số bản ghi/số trang phản ánh đúng bộ lọc. Nếu hiển thị số lượng
  từng tab thì cần cách lấy số lượng chính xác tương ứng.
- **Đã chốt:** mốc chuyển sang Đã qua là lúc suất chiếu kết thúc (`endsAt`).
  Với đơn CONFIRMED: `endsAt > now` thuộc Sắp tới (bao gồm suất đang chiếu),
  `endsAt <= now` thuộc Đã qua. So sánh theo instant, không theo ngày lịch hoặc
  giờ thiết bị FE; check-in không quyết định mốc chuyển tab.
- **Còn cần xác định khi triển khai:** phân loại các trạng thái chưa CONFIRMED,
  cách đồng bộ khi lịch suất thay đổi và xử lý booking cũ thiếu thời gian suất.
- **Hoàn tất khi:** lọc đúng trước khi phân trang, không bỏ sót vé ở trang khác.
  FE không lọc riêng 20 đơn đang xem để giả tổng của cả tab.

**Rà soát phụ thuộc dữ liệu:** Inventory đã trả `startsAt`/`endsAt` qua
`GET /api/v1/showtimes/{showtimeId}` (`ShowtimeResponse`). Booking hiện chưa có
client đọc lịch Inventory; thay vào đó event seat-reserved version 2 hiện mang
showtimeEndsAt và Booking lưu snapshot phục vụ query period. Không join DB
Inventory hoặc lọc một trang trong bộ nhớ. Chưa có snapshot startsAt hay luồng
đồng bộ khi lịch thay đổi sau reservation.
Mốc kết thúc đã được chủ dự án xác nhận ngày 2026-11-16. Bộ lọc theo thời gian
đã có trong source, chưa xác minh runtime; không coi U06 đã hoàn tất.

### BE-U07 — Điểm và hạng thành viên

- **Màn hình:** membership, thẻ thành viên và thông tin tài khoản.
- **Hiện tại:** CurrentUserProfileResponse chưa có contract điểm/hạng; chưa thấy
  public API chương trình thành viên/lịch sử điểm.
- **Cần BE cung cấp:** chính sách thật, điểm khả dụng, hạng hiện tại, điều kiện
  lên/giữ hạng, quyền lợi và lịch sử tích/điều chỉnh điểm.
- **Cần chốt:** thời điểm cộng, xử lý giao dịch thất bại/hủy, hết hạn điểm và
  quyền lợi ảnh hưởng giá nếu có. Không sao chép tỷ lệ 1 điểm/10.000đ của demo.
- **Hoàn tất khi:** số liệu do BE tính và ghi nhận nhất quán; retry thanh toán
  không cộng điểm hai lần.

### BE-U08 — Khôi phục mật khẩu và gửi lại email xác minh

- **Màn hình:** quên mật khẩu và khôi phục khi email xác minh hết hạn.
- **Hiện tại:** đã có đổi mật khẩu khi đăng nhập và xác minh đăng ký; chưa thấy
  public controller/client cho quên/reset mật khẩu hoặc resend email xác minh.
- **Cần BE cung cấp:** luồng phục hồi qua User Service, URL trả về FE nếu cần,
  contract token/thời hạn và thông báo chung; resend nếu được sản phẩm cho phép.
- **Cần chốt:** UI đặt tại trang đăng nhập của BE hay FE; giới hạn gửi/thử,
  token dùng một lần và hành vi khi tài khoản không tồn tại.
- **Hoàn tất khi:** không làm lộ tình trạng đăng ký email qua thông báo; FE không
  tự gửi mật khẩu đăng nhập, lưu token phục hồi hoặc tạo endpoint không có thật.

### BE-U09 — Gửi yêu cầu hỗ trợ (chỉ khi có nhận yêu cầu thật)

- **Màn hình:** form liên hệ trong nhóm support.
- **Hiện tại:** chưa thấy public API tiếp nhận yêu cầu hỗ trợ trong các service
  đang generate. Form reference chỉ mô phỏng, không gửi yêu cầu thật.
- **Cần quyết định:** chỉ hiển thị kênh liên hệ đã được phê duyệt hay có form gửi.
  Chỉ phương án form gửi mới cần contract BE tiếp nhận, kiểm tra dữ liệu và xác
  nhận kết quả; mã theo dõi nếu nghiệp vụ yêu cầu.
- **Hoàn tất khi:** FE không báo gửi thành công trước khi hệ thống tiếp nhận.

### BE-U10 — Thanh toán thật và kết quả khi đơn đã đóng

- **Màn hình:** checkout, quay về từ cổng thanh toán, chi tiết đơn.
- **Hiện tại:** `CreateBookingRequest.paymentProvider` nhận `VNPAY`/`MOMO`;
  lựa chọn được lưu ở Booking và truyền bằng `payment-requested` version 2.
  Payment vẫn nhận version 1. Tuy nhiên adapter hiện chỉ có `MOCK`; nhận tên
  provider không chứng minh có checkout VNPay/MoMo hoạt động.
- **Đã có:** API đọc checkout cho chủ sở hữu tại
  `GET /api/v1/payments/bookings/{bookingId}/checkout`, lưu checkout URI,
  provider worker, cơ chế webhook chung và publisher Payment Outbox. Cần mở
  rộng các port này thay vì tạo API khởi tạo thanh toán trùng chức năng.
- **Còn thiếu:** adapter và xác minh callback riêng từng provider, cấu hình
  sandbox/production, URL quay về, kiểm tra số tiền/tiền tệ và tham chiếu giao
  dịch. Return URL không được tự xác nhận thanh toán hay phát hành vé.
- **Cần chốt:** kết quả thành công đến khi Booking đã hủy/hết hạn được đối soát
  thủ công hay tự hoàn tiền; chưa có quyết định trong phiên hiện tại. Refund
  API đã bị loại khỏi phạm vi công khai, không tự khôi phục theo bản thiết kế cũ.
- **Hoàn tất khi:** toàn bộ chuỗi Payment kết quả → Booking xác nhận → Inventory
  đặt ghế được kiểm chứng; callback/retry trùng không tạo giao dịch/vé thứ hai;
  đơn đã đóng không được xác nhận lại hoặc chiếm ghế đã nhả. FE phân biệt trạng
  thái tiền với trạng thái đơn và không hứa “đang chờ xác nhận” vô hạn.

## 2. Bổ sung dữ liệu được khuyến nghị, không phải blocker cho toàn bộ UI

- **Tóm tắt đơn/vé:** cân nhắc projection hoặc snapshot tên phim, rạp, phòng,
  ngày/giờ và phiên bản chiếu. Hiện FE tra thêm theo showtimeId; có thể dựng UI
  với API hiện tại nhưng phát sinh nhiều request và thông tin danh mục có thể đổi.
  Chốt rõ dữ liệu lịch sử nào phải giữ nguyên và dữ liệu nào được cập nhật.
- **Ảnh/nội dung chi tiết rạp, trải nghiệm:** CinemaResponse đã có tên, địa chỉ,
  liên hệ, giờ mở cửa và chỉ đường; đã có tiện ích/lịch chiếu. Chỉ cần bổ sung
  contract nếu nội dung/ảnh cụ thể trong thiết kế phải được quản lý động mà API
  hiện tại chưa cung cấp. Không coi việc thiếu route chi tiết rạp ở FE là thiếu BE.

## 3. Thứ tự trao đổi và bàn giao đề xuất

1. Hoàn thiện thanh toán và quyết định kết quả đơn đã đóng: BE-U10; sau đó
   chốt phạm vi vé điện tử/QR và tab Vé của tôi: BE-U04, BE-U06.
2. Chốt checkout có bắp nước, ưu đãi và người nhận: BE-U01, BE-U02, BE-U03.
3. Chốt có hỗ trợ guest và chương trình thành viên: BE-U05, BE-U07.
4. Chốt nơi xử lý khôi phục tài khoản và phương án liên hệ: BE-U08, BE-U09.

Mỗi mục được triển khai cần bàn giao OpenAPI, phân quyền, request/response,
trạng thái/lỗi, quy tắc phân trang nếu có, thời hạn và khả năng retry/idempotency
cho thao tác ghi. Với checkout, phải thống nhất tổng tiền và snapshot của đơn
với payment; không để FE tự tính chính sách hay giả kết quả cuối cùng.

## 4. Bằng chứng source và ranh giới triển khai

Các đường dẫn dưới đây tính từ root BE, trừ những dòng ghi rõ FE.

| Mục        | Bằng chứng đã đối chiếu                                                                                                                                                                           | Kết luận và phụ thuộc                                                                                                                                                |
| ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| U01–U03    | `services/booking-service/src/main/java/com/cinema/booking/dto/request/CreateBookingRequest.java`, `dto/response/BookingResponse.java` cùng package gốc; danh sách controller trong các service   | Request có ghế, giá so sánh và provider; chưa có dòng sản phẩm, giảm giá hay snapshot người nhận. Service sở hữu sản phẩm/ưu đãi cần được quyết định trước.          |
| U04        | `services/booking-service/src/main/java/com/cinema/booking/event/DefaultBookingConfirmedOutboxFactory.java`, `dto/response/BookingResponse.java`                                                  | Có event xác nhận đơn; chưa có public contract phát hành/đọc/check-in vé. Không đồng nhất `CONFIRMED` với vé QR đã phát hành.                                        |
| U05–U06    | `services/booking-service/src/main/java/com/cinema/booking/controller/BookingController.java`                                                                                                     | Tạo/đọc/hủy dùng `CurrentUser.id()`; list hiện chỉ nhận page/size. Bộ lọc phải áp dụng trước pagination.                                                             |
| U07–U08    | `services/user-service/src/main/java/com/cinema/user/controller/CurrentUserController.java`, `AuthenticationRegistrationController.java`                                                          | Đã có profile, đổi mật khẩu, đăng ký và xác minh email; chưa có controller phục hồi/resend hoặc membership. Google Login không thay thế phục hồi tài khoản mật khẩu. |
| U09        | Danh sách controller trong `services`; `docs/BE_ROADMAP.md` P6.3                                                                                                                                  | Chưa có public support intake; Notification R28 đang hoãn, không tự mở service gửi vé/thông báo.                                                                     |
| U10        | `services/payment-service/src/main/java/com/cinema/payment/provider/mock/MockPaymentProvider.java`, `config/PaymentOutboxConfiguration.java`, `controller/CustomerPaymentCheckoutController.java` | MOCK và nền tảng Outbox/checkout đã có; nhà cung cấp thật và chính sách đơn đã đóng còn mở.                                                                          |
| FE booking | `E:/java/cinematic-web/src/features/booking/components/BookingPayment.vue`, `api/customer-payment.ts`, generated Booking client                                                                   | Dùng contract checkout đang có; generated client là bằng chứng source, không chứng minh API export đang chạy đã đồng bộ.                                             |

Các file DTO trong bảng thuộc `com/cinema/booking` khi không lặp lại đầy đủ
prefix. Đây là inventory các gap công khai tìm được trong checkout; không dùng
kết quả tìm kiếm tên file để kết luận mọi xử lý nội bộ đều chưa tồn tại.

## 5. Yêu cầu chung khi triển khai từng gap

- Mỗi lượt chọn một mục có nghiệp vụ đã chốt. Các mục “cần chốt” chưa được coi
  là đã phê duyệt chỉ vì xuất hiện trong reference hoặc tài liệu này.
- Giữ MySQL và database-per-service. Thêm migration mới khi cần; mô tả backfill,
  dữ liệu cũ, index/constraint, thứ tự rollout và cách rollback trước khi chạy.
- Tiền vé, combo và giảm giá phải có nguồn có thẩm quyền và snapshot rõ ràng;
  không mở rộng tổng tiền bằng dữ liệu FE tự tính. Không sửa payload version 1
  đã phát hành như thể mọi consumer đều đã nâng cấp.
- API chủ sở hữu phải kiểm tra principal tại BE, không dựa vào userId FE gửi.
  QR/token tra cứu không được chứa dữ liệu cá nhân hoặc secret không cần thiết.
- Bàn giao contract chính thức và hướng dẫn FE theo từng mục; endpoint nêu ở
  U10 là API hiện hữu, các nhu cầu khác không phải tên endpoint đã cam kết.
- Kiểm tra thủ công sau khi triển khai: luồng thành công, lỗi validation,
  quyền sở hữu, request/event trùng, hết hạn và cạnh tranh; ghi kết quả thực tế.
  Không yêu cầu chạy các kiểm tra ghi dữ liệu trước khi có môi trường phù hợp.

## 6. Kiểm tra của lượt rà soát này

- Đã đọc tài liệu hiện có và đối chiếu controller, DTO, provider, cấu hình
  Outbox, source và generated client FE được dẫn ở trên.
- Chỉ cập nhật tài liệu này; không thay API/runtime, FE hay migration MySQL.
- Không chạy test, build, migration, gọi API hay truy cập DB trong lượt này.
- U01–U10 vẫn là gap hoặc quyết định đang mở, không đánh dấu phase hoàn tất.
