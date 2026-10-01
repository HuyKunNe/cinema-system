# Đề xuất cập nhật Backend phục vụ Customer Home Page

Ngày rà soát: 2026-10-01
Trạng thái: PROPOSED — chưa xác nhận triển khai

Backend: HuyKunNe/cinema-system

Frontend: HuyKunNe/cinematic-web

## 1. Mục đích và phạm vi

Tổng hợp các thiếu hụt dữ liệu và nghiệp vụ backend được phát hiện khi
phát triển F3.1 — Customer Home Page.

Các khu vực liên quan:

- HeroBanner.
- QuickBooking.
- NowShowing và MovieCard.
- UpcomingMovies.
- Promotion.
- Membership: ghi nhận thiếu hụt dữ liệu, chưa mở rộng phạm vi triển khai.

Nguồn giao diện chính thức:

- docs/design/reference/html-convert/cinematic-home-desktop.html
- docs/design/reference/html-convert/cinematic-home-desktop.css
- docs/design/reference/html-convert/cinematic-home-mobile.html
- docs/design/reference/html-convert/cinematic-home-mobile.css

Đây là tài liệu đề xuất, không phải đặc tả API đã triển khai.
Endpoint, field và enum được đề xuất phải được review trước khi FE sử dụng.

R28 Notification Service tiếp tục được hoãn.
Không làm lại luồng đăng nhập OAuth2/OIDC hiện có.

### Yêu cầu FE hiện tại

- HeroBanner tự chuyển sau 5 giây, animation trái/phải 700ms.
- NowShowing là carousel chuyển thủ công, không autoplay.
- Promotion được yêu cầu chuyển thành carousel thủ công, không autoplay.
- Tiêu đề dài phải giữ bố cục ổn định.
- Hero có nút “Xem trailer”; MovieCard có nút Play trên poster.
- Nhãn phân loại độ tuổi phải lấy từ dữ liệu thật.

Các yêu cầu animation, timer, swipe, controls và chiều cao nội dung thuộc FE.
Không cần endpoint riêng để thực hiện carousel.

Tại commit FE đã đọc:

- Hero đã có autoplay và animation trái/phải.
- NowShowing đã có carousel thủ công.
- Promotion vẫn là danh sách tĩnh; hướng dẫn carousel chưa được xác nhận
  trong source remote.
- Promotion và Membership vẫn là nội dung minh họa có nhãn rõ ràng.
- Chưa có dữ liệu phân loại độ tuổi trong HomeMovie.

Tài liệu này thay thế các mô tả cũ về NowShowing autoplay và việc đưa
Membership carousel vào cùng phạm vi.

## 2. Hiện trạng backend đã xác nhận

| Khu vực                  | Source hiện tại                                                                      | Giới hạn                                                              |
| ------------------------ | ------------------------------------------------------------------------------------ | --------------------------------------------------------------------- |
| Movie catalog            | GET /api/v1/movies trả List<MovieResponse>                                           | Controller chưa có query filter hoặc pagination                       |
| Movie metadata           | Có title, description, durationMinutes, releaseDate, genres, status                  | Chưa có phân loại độ tuổi hoặc tagline riêng                          |
| Media                    | Có posterUrl và trailerUrl nullable                                                  | Chưa có backdropUrl; media URL trong request mới có validation độ dài |
| Catalog ordering         | MovieServiceImpl.findAll() gọi repository.findAll()                                  | Chưa có sort rõ ràng                                                  |
| Cinema catalog           | GET /api/v1/cinemas, city tùy chọn                                                   | Trả rạp active, sort theo name                                        |
| Showtimes theo phim      | GET /api/v1/showtimes/by-movie/{movieId}                                             | Trả tất cả trạng thái theo phim, sort startsAt tăng dần               |
| Showtimes theo thời gian | GET /api/v1/showtimes?from=…&to=…                                                    | Chưa có cinemaId/movieId filter; repository dùng StartsAtBetween      |
| ShowtimeResponse         | Có movieId, roomId, roomName, cinemaId, cinemaName, startsAt, endsAt, status         | Chưa có timezone ID hoặc availability summary                         |
| Reservation consumer     | Kiểm tra showtime tồn tại và OPEN_FOR_BOOKING, thời hạn request/hold, trạng thái ghế | Chưa có guard startsAt > now hoặc room/cinema active tại bước này     |
| Direct hold              | ShowSeatServiceImpl.hold() kiểm tra expiration và AVAILABLE                          | Chưa có guard điều kiện showtime tại entry point này                  |
| Promotion/Membership     | Chưa tìm thấy contract phục vụ nội dung Home trong source đã rà soát                 | FE đang dùng dữ liệu minh họa                                         |
| Security                 | GET movie/inventory catalog public ở Gateway và service                              | Không cần đổi login để đọc Home                                       |
| CORS                     | Gateway config có localhost pattern và origin Vercel đã yêu cầu                      | Cấu hình source chưa chứng minh môi trường runtime đã nạp đúng        |

Các trạng thái Showtime hiện có:

- SCHEDULED
- OPEN_FOR_BOOKING
- CLOSED
- CANCELLED
- COMPLETED

OPEN_FOR_BOOKING không đồng nghĩa với còn ghế.
Kết quả read API không bảo đảm một reservation sau đó sẽ thành công.

README BE ghi R28 Deferred và maintenance Inventory lock hardening
In progress. Một số tài liệu khác vẫn còn dòng Current target R28.
Không dùng các dòng cũ đó để tự mở lại R28 hoặc đánh dấu maintenance DONE.

## 3. Danh sách đề xuất và ưu tiên

Giữ nguyên ID BE-HOME-01 đến BE-HOME-05 từ tài liệu trước.

| ID         | Ưu tiên                             | Đề xuất                                          | Phạm vi            |
| ---------- | ----------------------------------- | ------------------------------------------------ | ------------------ |
| BE-HOME-01 | P1                                  | Read API lịch mở bán theo rạp/phim/thời gian     | Inventory          |
| BE-HOME-02 | P1, cần chốt nghiệp vụ              | Điều kiện nhận reservation/hold mới thống nhất   | Inventory          |
| BE-HOME-03 | P1                                  | backdropUrl nullable                             | Movie              |
| BE-HOME-04 | P2                                  | Catalog filter và thứ tự ổn định                 | Movie              |
| BE-HOME-05 | P2, tùy chọn                        | Availability summary                             | Inventory          |
| BE-HOME-06 | P1                                  | Phân loại độ tuổi thật                           | Movie              |
| BE-HOME-07 | P1                                  | Chuẩn hóa, validation và bổ sung dữ liệu trailer | Movie              |
| BE-HOME-08 | P2, tùy chọn                        | Tagline và metadata biên tập Hero                | Movie/Home content |
| BE-HOME-09 | P2, cần chốt domain                 | Nội dung Promotion được quản lý                  | Domain chưa chốt   |
| BE-HOME-10 | Backlog                             | Nội dung Membership và chương trình thành viên   | Domain chưa chốt   |
| BE-HOME-11 | P1: chốt quy ước; field tùy nhu cầu | Timezone và ngày chiếu                           | Inventory/FE       |
| BE-HOME-12 | Kiểm tra vận hành                   | CORS và cấu hình môi trường FE                   | Gateway/config     |

Để giải quyết trực tiếp giao diện hiện tại, ưu tiên:
BE-HOME-06, BE-HOME-07, BE-HOME-03.

QuickBooking cần BE-HOME-01 và quyết định nghiệp vụ BE-HOME-02.
Các đề xuất còn lại không phải điều kiện bắt buộc để làm carousel.

## 4. BE-HOME-06 — Phân loại độ tuổi

### Hiện trạng

Movie entity, CreateMovieRequest, UpdateMovieRequest và MovieResponse
chưa có trường phân loại độ tuổi.

HTML tham chiếu có nhãn 13+, 16+ và P.
Không được lấy nhãn của phim mẫu để gán cho phim thật.

### Contract đề xuất — chưa triển khai

Thêm ageRating nullable vào Movie và các request/response tương ứng.

Đề xuất dùng enum code ổn định, FE tự map thành nhãn hiển thị.
Bộ mã ứng viên cần được chốt theo dữ liệu phân loại thực tế của dự án:

- P
- T13
- T16
- T18

Ví dụ mapping giao diện được đề xuất:

| Code | Nhãn FE           |
| ---- | ----------------- |
| P    | P                 |
| T13  | 13+               |
| T16  | 16+               |
| T18  | 18+               |
| null | Chưa có thông tin |

Đây chưa phải enum backend hiện có hoặc danh mục phân loại đã được phê duyệt.
Chỉ bổ sung mã khác khi có yêu cầu và nguồn dữ liệu xác nhận.

### Quy tắc

- Phim cũ giữ null; không tự backfill tất cả thành P hoặc T13.
- Phân loại do người quản trị nhập từ nguồn xác nhận.
- Không suy luận phân loại từ title, genre hoặc description.
- Phân loại hiển thị chưa tự động trở thành cơ chế xác minh tuổi khi đặt vé.
- Nếu cần hạn chế booking theo tuổi, phải chốt nghiệp vụ và contract riêng.
- Chốt omission/null semantics của UpdateMovieRequest trước khi mở rộng PUT.

### File liên quan

Các path sau tương đối với
services/movie-service/src/main/java/com/cinema/movie/:

- entity/Movie.java
- entity/ — enum mới sau khi chốt tên và bộ mã
- dto/request/CreateMovieRequest.java
- dto/request/UpdateMovieRequest.java
- dto/response/MovieResponse.java
- mapper/MovieMapper.java
- service/impl/MovieServiceImpl.java

Thêm migration mới tại:

services/movie-service/src/main/resources/db/migration/

### Tiêu chí review

- Phim cũ có ageRating null vẫn đọc được.
- Create/update/read giữ đúng code.
- Giá trị ngoài enum được xử lý theo cơ chế lỗi hiện có.
- FE hiển thị nhãn đúng và có fallback khi null.
- Không sửa generated FE DTO bằng tay.

## 5. BE-HOME-07 — Trailer và validation media URL

### Hiện trạng

trailerUrl đã có trong entity, create/update request và MovieResponse.
Không cần tạo thêm endpoint chỉ để mở trailer.

Request hiện giới hạn tối đa 500 ký tự.
Chưa có validation protocol được thể hiện trong các DTO đã đọc.

FE hiện chỉ sử dụng trailer URL HTTP(S) hợp lệ.
Khi không có URL, nút trailer có thể hiển thị disabled theo hướng dẫn FE.

### Đề xuất

- Trim khoảng trắng của trailerUrl.
- Chuẩn hóa blank thành null.
- Chốt URL tuyệt đối HTTP(S) cho trailer.
- Từ chối URL không hợp lệ hoặc protocol ngoài contract.
- Giữ nullable để phim chưa có trailer vẫn hợp lệ.
- Bổ sung trailer thật qua API quản trị hiện có.
- Không dùng một trailer mẫu chung cho mọi phim.
- Không yêu cầu backend tải URL để kiểm tra nội dung khi lưu.

Tách chính sách trailer URL khỏi image URL nếu poster/backdrop cần hỗ trợ
relative path của storage/CDN.

Nếu sau này cần phát trailer trong modal:

- Chốt provider và cách chuyển URL xem thành URL embed.
- Không mặc định mọi URL HTTP(S) đều nhúng được.
- Không thêm trailerEmbedUrl trong round này nếu chưa có nhu cầu xác nhận.

### File liên quan

Dưới services/movie-service/src/main/java/com/cinema/movie/:

- dto/request/CreateMovieRequest.java
- dto/request/UpdateMovieRequest.java
- service/impl/MovieServiceImpl.java
- mapper/MovieMapper.java
- error/MovieErrorCode.java — chỉ mở rộng nếu cần mã lỗi phù hợp
- Vị trí validator/helper theo convention thực tế khi triển khai

Đọc MovieMapperConfig trước khi quyết định chuẩn hóa ở mapper hoặc service.
Không để create và update có hai chính sách URL khác nhau.

### Tiêu chí review

- Null/blank được xử lý nhất quán.
- URL có khoảng trắng ngoài được chuẩn hóa.
- URL sai hoặc protocol không được phép bị từ chối.
- URL quá dài vẫn bị validation.
- URL đúng format nhưng không tải được vẫn có fallback FE.
- Không thay đổi luồng đăng nhập để xem trailer công khai.

## 6. BE-HOME-03 — Artwork ngang cho Hero

### Contract đề xuất — chưa triển khai

Thêm backdropUrl nullable vào:

- Movie entity.
- CreateMovieRequest.
- UpdateMovieRequest.
- MovieResponse.

Schema ứng viên:

movies.backdrop_url VARCHAR(500) NULL

posterUrl giữ ý nghĩa hiện tại.
backdropUrl phục vụ ảnh nền ngang, tránh crop poster dọc làm Hero.

### Quy tắc

- Dùng migration mới, không sửa migration đã áp dụng.
- Giới hạn độ dài và chuẩn hóa blank.
- Chốt URL/path được phép theo storage thực tế.
- Không thêm upload endpoint nếu chỉ cần lưu metadata.
- Không gán ảnh generic thành artwork thật của phim.
- Chốt semantics PUT: omission hoặc null không được gây mất artwork ngoài ý muốn.

Movie Service tại commit đã đọc có V1 và V2.
Chọn version migration tiếp theo tại thời điểm triển khai;
không cố định V3 nếu một round trước đã sử dụng version đó.

### File liên quan

Dưới services/movie-service/src/main/java/com/cinema/movie/:

- entity/Movie.java
- dto/request/CreateMovieRequest.java
- dto/request/UpdateMovieRequest.java
- dto/response/MovieResponse.java
- mapper/MovieMapper.java
- service/impl/MovieServiceImpl.java

Migration:

services/movie-service/src/main/resources/db/migration/

### FE sau khi có contract

- Regenerate client từ OpenAPI đúng backend version.
- Map backdropUrl vào HomeMovie.
- Cô lập fallback: backdrop → poster → placeholder dự án → gradient.
- Fallback cần xử lý cả thiếu URL và lỗi tải ảnh.
- Giữ nội dung, artwork và indicator của slide đồng bộ.

## 7. BE-HOME-01 — Read API lịch mở bán cho QuickBooking

### Vấn đề

FE đang lấy tất cả phim NOW_SHOWING và lịch theo từng phim,
sau đó lọc theo rạp/trạng thái/thời gian.

Khi đã chọn rạp, danh sách phim chưa được xác định trực tiếp từ lịch
mở bán của chính rạp đó.

### Contract đề xuất — chưa tồn tại

GET /api/v1/showtimes/bookable

| Query    | Kiểu                     | Yêu cầu  |
| -------- | ------------------------ | -------- |
| cinemaId | UUID                     | Bắt buộc |
| movieId  | UUID                     | Tùy chọn |
| from     | OffsetDateTime có offset | Bắt buộc |
| to       | OffsetDateTime có offset | Bắt buộc |

Response đề xuất: List<ShowtimeResponse> trực tiếp.
Không có kết quả: HTTP 200 với [].

Giữ response convention của controller hiện có.
Không đổi các endpoint cũ sang wrapper hoặc pagination một cách âm thầm.

### Điều kiện lọc đề xuất

- Khớp cinemaId.
- Khớp movieId nếu được cung cấp.
- OPEN_FOR_BOOKING.
- Room và Cinema active.
- startsAt > serverNow.
- startsAt thuộc [from, to).
- Sort startsAt ASC, id ASC.

Điều kiện startsAt > now cần thống nhất với BE-HOME-02.
Nếu nghiệp vụ có cutoff trước giờ chiếu, chốt cutoff trước khi triển khai.

Endpoint thời gian hiện tại dùng StartsAtBetween.
Không sửa semantics endpoint cũ sang [from, to) trong cùng round.

### Validation và giới hạn

- from/to bắt buộc và to > from.
- Sai UUID hoặc thời gian trả lỗi theo cơ chế chung.
- Cinema không tồn tại dùng lỗi CINEMA_NOT_FOUND hiện có.
- Chốt behavior rạp inactive: empty list hay lỗi nghiệp vụ.
- Khoảng tra cứu tối đa phải cấu hình và tài liệu hóa.
- 30 ngày chỉ là giá trị ứng viên, chưa phải giới hạn hiện có.
- Không silently truncate danh sách.

Nếu cần pagination, chốt contract trước.
FE phải lấy đủ dữ liệu cần thiết trước khi suy ra phim/ngày.

### Ownership và cách FE dùng

Inventory trả lịch và movieId; Movie Service sở hữu metadata phim.
Không join database của Movie Service.

FE có thể:

1. Load catalog phim và rạp active.
2. Chọn rạp, lấy lịch bookable trong khoảng hiển thị.
3. Lấy tập movieId từ lịch.
4. Ghép catalog để hiển thị tên/artwork.
5. Lọc tiếp theo phim, ngày và suất.
6. Refetch/revalidate khi tiếp tục.

Không cần tạo Home Service hoặc BFF chỉ cho bước này.

Cần chốt có cho phép đặt trước phim UPCOMING hay không.
Nếu có, QuickBooking không chỉ nhận mảng NOW_SHOWING.
NowShowing vẫn chỉ hiển thị NOW_SHOWING.

Inventory không sở hữu MovieStatus.
Read API Inventory riêng không xác nhận phim active bằng cách đọc database Movie.
Nếu cần enforcement trạng thái Movie ở BE, thiết kế integration riêng.

### File liên quan

Dưới services/inventory-service/src/main/java/com/cinema/inventory/:

- controller/ShowtimeController.java
- service/ShowtimeService.java
- service/impl/ShowtimeServiceImpl.java
- repository/ShowtimeRepository.java
- dto/response/ShowtimeResponse.java
- config/InventorySecurityConfig.java

Gateway:

infrastructure/gateway-service/src/main/java/com/cinema/gateway/config/
GatewaySecurityConfiguration.java

GET endpoint mới dưới /api/v1/showtimes/\*\* đã nằm trong public patterns
hiện tại; kiểm tra lại trước khi quyết định sửa security.

Các index đã có:

- idx_rooms_cinema_active
- idx_showtimes_movie_start
- idx_showtimes_room_status_start

Dùng EXPLAIN với dữ liệu thực tế trước khi thêm index mới.

## 8. BE-HOME-02 — Điều kiện nhận reservation/hold mới

### Quyết định nghiệp vụ cần chốt

Nếu chỉ nhận đặt mới trước giờ chiếu và tại rạp/phòng hoạt động,
BE phải kiểm tra tại bước tạo hold, không dựa vào FE/read API.

Điều kiện đề xuất:

- Showtime tồn tại.
- OPEN_FOR_BOOKING.
- Chưa đến cutoff đã chốt.
- Room và Cinema active.
- Request/hold chưa hết hạn.
- Ghế hợp lệ và có thể giữ theo quy tắc hiện tại.

### Entry point cần rà soát

Dưới services/inventory-service/src/main/java/com/cinema/inventory/:

- service/impl/SeatReservationRequestedConsumerServiceImpl.java
- service/impl/ShowSeatServiceImpl.java
- entity/Showtime.java
- event/SeatReservationRejectionReason.java

Consumer hiện có ProcessedEvent registration và Clock.
Direct hold cũng sử dụng Clock.

### Yêu cầu bảo toàn

- Tái sử dụng Clock; lấy thời điểm nhất quán trong mỗi operation.
- Giữ idempotency và Transactional Outbox.
- Event duplicate không tạo kết quả terminal trái với lần xử lý trước.
- Không tự hủy hold/booking đã có khi thời gian trôi qua hoặc rạp bị deactivate.
- Phân biệt tạo hold mới với retry/replay hợp lệ của hold đã có.
- Giữ lock ordering và các guard trạng thái ghế hiện tại.
- Review cả reservation consumer và direct hold.

Nếu thêm rejection reason, kiểm tra contract phía Booking consumer.
Không đổi event enum một phía.

Chỉ thêm if chưa bảo đảm ngăn race với admin đóng suất/deactivate.
Nếu cần bảo đảm đó, review transaction/locking của các operation liên quan
và xác định thứ tự áp dụng quy tắc khi chúng chạy đồng thời.

## 9. BE-HOME-11 — Timezone và ngày chiếu

### Hiện trạng

ShowtimeResponse trả OffsetDateTime.
Chưa có timezone ID của rạp trong CinemaResponse/ShowtimeResponse đã đọc.

FE đang suy ra ngày theo timezone trình duyệt.
Người dùng ở timezone khác có thể thấy suất thuộc ngày khác với ngày của rạp.

### Đề xuất

Chốt một trong hai chính sách:

1. Toàn hệ thống dùng timezone kinh doanh cấu hình chung.
2. Mỗi rạp có timezone IANA riêng nếu hỗ trợ nhiều timezone.

Asia/Ho_Chi_Minh là giá trị ứng viên cho hệ thống chỉ vận hành tại Việt Nam.
Không coi đây là field hoặc cấu hình BE đã có.

Nếu cần hỗ trợ theo rạp, đề xuất timeZone trong Cinema và response liên quan.
Chỉ thêm sau khi chốt phạm vi vận hành.

### Quy tắc

- Giữ timestamp có offset/instant trong API.
- Không thay bằng chuỗi giờ địa phương không có timezone.
- FE nhóm ngày/hiển thị giờ theo timezone rạp hoặc timezone kinh doanh.
- Query from/to gửi offset rõ ràng.
- Server dùng Clock để kiểm tra cutoff.
- Không yêu cầu FE dùng đồng hồ máy khách làm thẩm quyền nghiệp vụ.

## 10. BE-HOME-04 — Catalog filter và thứ tự ổn định

### Hiện trạng

MovieController.findAll() chưa có query filter.
MovieServiceImpl gọi repository.findAll() không có sort rõ ràng.

### Đề xuất P2

- Thứ tự mặc định ổn định, có id làm tie-breaker.
- Chốt cách xử lý releaseDate null.
- Có thể bổ sung status filter nếu catalog tăng lớn.
- Chỉ bổ sung pagination khi cần và có contract tương thích rõ ràng.

Sort releaseDate DESC, null cuối, id ASC là phương án ứng viên.
Thứ tự này không đồng nghĩa “phim nổi bật”.

Nếu chỉ cần thứ tự cho Home nhỏ, FE có thể sort trước.
Không bắt buộc sửa BE để hoàn thành carousel.

Không suy ra rating, độ phổ biến hoặc featured từ thứ tự repository.

## 11. BE-HOME-08 — Tagline và biên tập Hero

### Hiện trạng

Movie có description nhưng chưa có tagline riêng hoặc featured ordering.
FE đang dùng tagline chung.

### Đề xuất tùy chọn

Nếu cần nội dung theo từng phim:

- tagline nullable.
- Giới hạn độ dài được chốt theo nhu cầu nội dung.
- Null dùng copy chung hoặc ẩn theo quyết định FE.
- Không tự rút description thành một thông điệp marketing chưa được duyệt.

Nếu cần quản trị phim xuất hiện trên Hero:

- Chốt featured và thứ tự hiển thị.
- Chốt thời gian bắt đầu/kết thúc nếu có campaign.
- Dùng tie-breaker ổn định.
- Không cho FE tự coi mọi NOW_SHOWING là danh sách được biên tập.

Chọn ownership trước:

- Metadata riêng của phim có thể thuộc Movie Service.
- Campaign/Home content cần thiết kế ownership riêng nếu vượt khỏi metadata phim.

Không bắt buộc tạo hero endpoint mới.
Không thêm timer hoặc animation config vào BE chỉ để phục vụ carousel hiện tại.

## 12. BE-HOME-09 — Nội dung Promotion

### Hiện trạng

Promotion FE sử dụng promotionPreviews trong presentation/marketing.ts.
Các giá và combo từ thiết kế có nhãn “Nội dung minh họa · chưa áp dụng”.

Chưa có contract backend đã xác nhận cho promotion của Home.
Carousel thủ công không yêu cầu backend mới.

### Hai phạm vi cần phân biệt

#### A. Nội dung quảng bá

Nếu cần quản trị nội dung Home, chốt model có thể bao gồm:

- ID.
- Tiêu đề, nhãn và mô tả.
- Artwork/alt text nếu dùng ảnh.
- Thứ tự.
- Trạng thái xuất bản.
- Thời gian hiệu lực.
- Destination đã được kiểm soát nếu có CTA.

Đây là danh sách field đề xuất, không phải response hiện có.
Chưa chốt endpoint, DTO hoặc service owner.

Public read chỉ trả nội dung được xuất bản và đang hiệu lực.
Mutation phải có quyền quản trị được thiết kế và cấp thực tế.

#### B. Ưu đãi áp dụng vào giao dịch

“Đồng giá 45K”, giảm giá hoặc combo có hiệu lực cần nghiệp vụ riêng:

- Điều kiện áp dụng theo ngày/rạp/phim/suất/loại ghế.
- Timezone áp dụng.
- Mức giá/discount có kiểu dữ liệu tiền và currency rõ ràng.
- Chính sách cộng dồn, giới hạn và hết hạn.
- Giá cuối cùng được BE tính và lưu theo ownership đã chốt.
- Quy tắc hoàn tiền và snapshot của booking.
- Combo cần catalog/order nếu hệ thống bán bắp nước thật.

Không để FE đổi giá dựa trên text của banner.
Nội dung quảng bá không tự trở thành pricing rule.

Không mặc định đưa domain này vào Movie Service hoặc R28.
Chốt ownership với Booking/Payment và domain giá trước khi triển khai.

## 13. BE-HOME-10 — Membership

### Hiện trạng

Membership FE là nội dung minh họa.
Chưa có contract đã xác nhận cho tier, point balance, earn/redeem hoặc benefits.

Không sao chép tên/mã thành viên mẫu trong HTML thành dữ liệu người dùng thật.

### Đề xuất backlog

Nếu chỉ cần giới thiệu chương trình:

- Quản lý nội dung quyền lợi đã được duyệt.
- Có thứ tự, trạng thái xuất bản và destination nếu có.

Nếu cần chương trình thành viên thật:

- Chốt ownership.
- Membership profile/tier có contract riêng.
- Point ledger, earn/redeem, reversal có nghiệp vụ riêng.
- Chốt tích điểm ở thời điểm thanh toán/booking nào.
- Chốt hoàn điểm khi hủy/refund.
- Bảo đảm idempotency khi xử lý event.
- Dữ liệu cá nhân lấy từ endpoint authenticated theo identity hiện có.

Không tính điểm từ FE.
Không tự thêm domain thành viên vào User Service chỉ vì đã có user profile.
Không triển khai notification/R28 như điều kiện để làm nội dung Membership.

Phần này không nằm trong round carousel Promotion hiện tại.

## 14. BE-HOME-05 — Availability summary

Đề xuất tùy chọn nếu UX cần hiển thị số ghế còn khả dụng.

Inventory sở hữu dữ liệu show_seats.

Cần chốt:

- AVAILABLE có nghĩa gì.
- HELD hết hạn nhưng chưa release có được tính hay không.
- UNAVAILABLE/BOOKED bị loại như thế nào.
- Summary được tính tại thời điểm nào.
- Chính sách freshness/cache nếu có.

Không đưa held_by_booking_id hoặc dữ liệu người dùng vào public summary.
Không thực hiện một request đếm ghế cho từng suất.

Ưu tiên aggregate theo batch và đo chi phí query.
Summary không bảo đảm giữ được ghế khi người dùng tiếp tục.

Chưa thay ShowtimeResponse cho đến khi semantics được chốt.

## 15. BE-HOME-12 — CORS và môi trường

Source gateway config đã có:

- Localhost pattern mặc định http://localhost:\*.
- https://cinematic-2awjfcb0p-huykunnes-projects.vercel.app

File:

infrastructure/config-service/src/main/resources/config-repo/
gateway-service.yml

Không ghi nhận “chưa hỗ trợ origin Vercel” như một thiếu hụt source hiện tại.

Cần kiểm tra vận hành:

- Gateway đã nạp đúng config/profile.
- Origin thực tế khớp allowlist.
- Preflight và GET có header CORS đúng.
- Deployment URL mới hoặc domain chính thức được cấu hình khi cần.
- FE production trỏ tới Gateway có thể truy cập từ môi trường người dùng.

Không mở toàn bộ origin chỉ để xử lý một deployment.
Không thay auth/security rules vì lỗi CORS.
CORS ở Gateway không tự cấu hình CORS cho trang authorization User Service.

Chưa gọi runtime API trong lần rà soát này;
không tuyên bố lỗi CORS đã được khắc phục end-to-end.

## 16. Quy tắc triển khai chung

### Database và compatibility

- Migration mới cho schema thay đổi.
- Không sửa V1/V2 đã áp dụng.
- Phim cũ có field mới null vẫn đọc được.
- Chốt semantics PUT cho omission/null trước khi deploy.
- Không âm thầm đổi List response thành page/wrapper.
- Không join database giữa các service.
- Không thay event contract một phía.

### Security

- Giữ public read catalog hiện tại.
- Movie mutation giữ movie:manage.
- Showtime mutation giữ showtime:manage.
- Direct hold giữ authorization hiện có.
- API mới ngoài catalog patterns cần review Gateway và service.
- Không invent permission chưa được cấp.
- Không làm lại login OIDC.

### FE integration

Sau khi BE contract được triển khai và review:

1. Cập nhật OpenAPI snapshot đúng môi trường/backend commit.
2. Regenerate Orval.
3. Cập nhật feature mapper và HomeMovie.
4. Component sử dụng query/service/composable hiện có.
5. Không gọi Axios trực tiếp từ component.
6. Không sửa generated DTO bằng tay.
7. Giữ fallback artwork/metadata được cô lập.
8. Chỉ bỏ nhãn minh họa khi có dữ liệu và nghiệp vụ thật.

### Documentation

Cập nhật các tài liệu thực tế bị ảnh hưởng sau mỗi round:

- docs/cinema_backend_home_proposals.md
- docs/10_ROADMAP.md
- docs/11_CHANGELOG.md
- docs/06_DATABASE_DESIGN.md nếu đổi schema
- docs/08_SECURITY.md nếu đổi security
- API/architecture/event docs khi contract hoặc ownership thay đổi

Không đánh dấu DONE chỉ vì đã có proposal hoặc code block.

## 17. Các round triển khai đề xuất

| Round | Phạm vi                                                 | Commit message ứng viên                                        |
| ----- | ------------------------------------------------------- | -------------------------------------------------------------- |
| BE-H1 | Age rating                                              | feat(movie): add optional movie age classification             |
| BE-H2 | Trailer normalization/validation và dữ liệu thật        | fix(movie): normalize and validate trailer URLs                |
| BE-H3 | Backdrop artwork                                        | feat(movie): add optional backdrop artwork                     |
| BE-H4 | Chốt cutoff/timezone/active policy, triển khai read API | feat(inventory): expose bookable showtimes by cinema and movie |
| BE-H5 | Áp dụng policy tại các entry point reservation/hold     | fix(inventory): enforce eligibility for new seat holds         |
| BE-H6 | Catalog filter/order nếu cần                            | feat(movie): add stable catalog filtering and ordering         |

Age rating và backdrop có thể dùng migration riêng theo thứ tự triển khai.
Không cố định version migration trong proposal.

BE-HOME-05, 08, 09, 10 giữ backlog cho đến khi xác nhận nhu cầu/domain.
BE-HOME-12 là kiểm tra cấu hình runtime; chỉ sửa nếu phát hiện sai lệch.

Mỗi round chỉ thực hiện phạm vi đã chọn.
Review kết quả trước khi chuyển round tiếp theo.

## 18. Kiểm tra thủ công đề xuất

### Movie

- Đọc phim cũ khi field mới null.
- Tạo/sửa/đọc age rating.
- Trailer hợp lệ, blank, sai protocol và quá dài.
- Poster/backdrop khác nhau được trả đúng vai trò.
- Update không xóa field mới ngoài semantics đã chốt.
- Public GET hoạt động; mutation giữ authorization.

### QuickBooking

- Rạp khác nhau trả lịch khác nhau.
- Chỉ trả trạng thái hợp lệ theo policy.
- Kiểm tra from, to và cutoff.
- Kiểm tra rạp/phòng inactive.
- Phim UPCOMING theo quyết định đặt trước.
- Cùng giờ ở nhiều phòng có thứ tự ổn định.
- Ngày chiếu nhất quán khi timezone trình duyệt khác timezone rạp.

### Reservation

- Suất không đủ điều kiện không tạo hold mới.
- Direct hold và event consumer dùng cùng policy.
- Duplicate/replay không tạo kết quả trái ngược.
- Không làm hỏng release/confirmation/compensation hiện có.
- Không tuyên bố đã bảo đảm concurrency chỉ từ kiểm tra Swagger tuần tự.

### Home

- Hero/MovieCard dùng cùng phân loại.
- Trailer đúng phim, thiếu URL có trạng thái phù hợp.
- Backdrop lỗi kích hoạt fallback.
- Hero autoplay; NowShowing/Promotion không autoplay.
- Tiêu đề dài không làm bố cục nhảy.
- Promotion/Membership giữ nhãn minh họa khi chưa có contract thật.

Không chạy automated tests, lint, type-check hoặc build trong round tài liệu này.
Các kiểm tra trên là checklist đề xuất, chưa được xác nhận đã thực hiện.

## 19. Nguồn đối chiếu

Các path BE tương đối với repository cinema-system tại commit ghi ở đầu tài liệu:

### Movie

- services/movie-service/src/main/java/com/cinema/movie/controller/MovieController.java
- services/movie-service/src/main/java/com/cinema/movie/entity/Movie.java
- services/movie-service/src/main/java/com/cinema/movie/dto/request/CreateMovieRequest.java
- services/movie-service/src/main/java/com/cinema/movie/dto/request/UpdateMovieRequest.java
- services/movie-service/src/main/java/com/cinema/movie/dto/response/MovieResponse.java
- services/movie-service/src/main/java/com/cinema/movie/mapper/MovieMapper.java
- services/movie-service/src/main/java/com/cinema/movie/service/impl/MovieServiceImpl.java
- services/movie-service/src/main/java/com/cinema/movie/config/MovieSecurityConfig.java
- services/movie-service/src/main/resources/db/migration/

### Inventory

- services/inventory-service/src/main/java/com/cinema/inventory/controller/ShowtimeController.java
- services/inventory-service/src/main/java/com/cinema/inventory/service/impl/ShowtimeServiceImpl.java
- services/inventory-service/src/main/java/com/cinema/inventory/repository/ShowtimeRepository.java
- services/inventory-service/src/main/java/com/cinema/inventory/dto/response/ShowtimeResponse.java
- services/inventory-service/src/main/java/com/cinema/inventory/enums/ShowtimeStatus.java
- services/inventory-service/src/main/java/com/cinema/inventory/service/impl/CinemaServiceImpl.java
- services/inventory-service/src/main/java/com/cinema/inventory/service/impl/SeatReservationRequestedConsumerServiceImpl.java
- services/inventory-service/src/main/java/com/cinema/inventory/service/impl/ShowSeatServiceImpl.java
- services/inventory-service/src/main/java/com/cinema/inventory/event/SeatReservationRejectionReason.java
- services/inventory-service/src/main/java/com/cinema/inventory/config/InventorySecurityConfig.java
- services/inventory-service/src/main/resources/db/migration/

### Gateway và tài liệu

- infrastructure/gateway-service/src/main/java/com/cinema/gateway/config/GatewaySecurityConfiguration.java
- infrastructure/config-service/src/main/resources/config-repo/gateway-service.yml
- README.md
- docs/01_AI_CONTEXT.md
- docs/02_ARCHITECTURE.md
- docs/08_SECURITY.md
- docs/10_ROADMAP.md

### Frontend

- src/features/home/components/HeroBanner.vue
- src/features/home/components/MovieCard.vue
- src/features/home/components/NowShowingSection.vue
- src/features/home/components/PromotionSection.vue
- src/features/home/components/MembershipSection.vue
- src/features/home/presentation/marketing.ts
- src/features/home/models/home-movie.ts
- src/features/home/mappers/home-movie.mapper.ts
- docs/design/reference/html-convert/

Các cập nhật chưa có trong commit remote không được coi là đã triển khai.
Không gọi API runtime, chạy test/build, áp dụng migration hoặc ghi vào repository
trong lần tổng hợp này.
