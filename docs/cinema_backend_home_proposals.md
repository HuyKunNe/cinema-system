# Backend cho Customer Home, Movie Catalog và QuickBooking

> **Trạng thái:** PARTIALLY IMPLEMENTED.
> **Ngày đối chiếu source:** 2026-10-05.
> **Nguồn giao diện:** cinematic-web/docs/design/reference/html-convert.
> **Cách đọc:** IMPLEMENTED nghĩa là đã có trong source tại baseline;
> không đồng nghĩa đã xác minh runtime, test hoặc deployment.
> **Ranh giới:** Không mở lại R28 Notification.

## 1. Mục tiêu

Theo dõi các API, quy tắc Backend và phần còn thiếu để hỗ trợ
Customer Home, Movie Catalog và QuickBooking.

Phân biệt rõ:

- Đã implement trong source.
- Chưa implement, còn ở mức đề xuất.
- Cần dữ liệu, contract hoặc quyết định nghiệp vụ.
- Đã có cấu hình nhưng chưa xác minh runtime.

## 2. Baseline đã đối chiếu

| Repository             | Commit                                   |
| ---------------------- | ---------------------------------------- |
| HuyKunNe/cinema-system | 00c759d574e44edc855cff866c9cc1a66af2c72b |
| HuyKunNe/cinematic-web | 4856878d420efa562a357d673e93eab04d463b32 |

Baseline là commit được dùng để kiểm tra source, không phải commit
chứa lần cập nhật tài liệu này.

Khi main thay đổi, kiểm tra lại source trước khi cập nhật trạng thái.

### 2.1 Movie Service

- GET /api/v1/movies trả List<MovieResponse>, giữ nguyên API cũ.
- GET /api/v1/movies/catalog đã có paging và filter status/genre.
- GET /api/v1/genres đã tồn tại.
- MovieStatus gồm UPCOMING, NOW_SHOWING, ENDED và INACTIVE.
- MovieResponse có posterUrl và trailerUrl.
- Movie entity và MovieResponse đã có backdropUrl và ageRating nullable.
- PUT /api/v1/movies/{id}/metadata cập nhật cả hai metadata, yêu cầu movie:manage.
- Request metadata yêu cầu cả hai key; null dùng để xóa giá trị.
- API sửa phim cũ bảo toàn metadata.
- Movie Service không sở hữu dữ liệu suất chiếu, phòng hoặc ghế.

### 2.2 Inventory

- GET /api/v1/showtimes/bookable đã tồn tại.
- ShowtimeResponse cung cấp movieId, roomId, roomName, cinemaId,
  cinemaName, startsAt, endsAt và status.
- ShowtimeResponse chưa có roomType hoặc availability tổng hợp.
- OPEN_FOR_BOOKING không bảo đảm còn ghế.

### 2.3 Caller Frontend

- home-queries.ts đã có useHomeBookableShowtimes.
- QuickBooking lấy lịch theo phim để xác định ngày có suất.
- Với ngày đã chọn, QuickBooking gọi API bookable theo rạp/phim/ngày.
- QuickBooking đọc loại phòng từ Room API rồi ghép theo roomId.
- cinema-programme.queries.ts vẫn lấy phòng và truy vấn theo từng phòng.
- movie-queries.ts và Home vẫn gọi findAll cho danh sách phim.
- Generated client đã có getMovieCatalog; caller Movies chưa chuyển
  sang paging server-side.
- cinema-time.ts dùng Asia/Ho_Chi_Minh.

### 2.4 Reservation và hold

- REST hold và reservation consumer cùng dùng
  ShowtimeRepository.existsHoldEligibleShowtime.
- Điều kiện gồm OPEN_FOR_BOOKING, startsAt > now,
  Room active và Cinema active.
- Consumer kiểm tra lại thời hạn hold sau khi khóa ghế.
- Replay và điều kiện tái sử dụng hold cùng booking được giữ nguyên.
- Confirm, release và compensation giữ logic hiện có.

## 3. Phạm vi và nguyên tắc

- Inventory sở hữu cinema, room, showtime, seat và hold.
- Movie Service sở hữu movie metadata và genre.
- Không truy cập trực tiếp database của service khác.
- Giữ API đang dùng; ưu tiên thay đổi additive.
- Không thêm metadata hoặc dữ liệu giao dịch bằng suy đoán.
- Carousel và autoplay thuộc Frontend.
- Không mở lại R28 Notification.
- Assistant chỉ đọc repository; người triển khai tự áp dụng và commit.

## 4. Trạng thái các đề xuất

| ID         | Nội dung                     | Trạng thái tại baseline                                         |
| ---------- | ---------------------------- | --------------------------------------------------------------- |
| BE-HOME-01 | Truy vấn suất mở bán         | IMPLEMENTED; có generated client và caller FE                   |
| BE-HOME-02 | Eligibility reservation/hold | IMPLEMENTED; chưa xác minh runtime trong lần đối chiếu này      |
| BE-HOME-03 | backdropUrl nullable         | Chờ nguồn ảnh ngang và quy ước media                            |
| BE-HOME-04 | Movie catalog paging/filter  | IMPLEMENTED ở BE và generated client; caller FE vẫn dùng API cũ |
| BE-HOME-05 | Availability tổng hợp        | Chờ contract và xác nhận cách tính                              |
| BE-HOME-06 | Age rating                   | Chờ dữ liệu và quy ước nghiệp vụ                                |
| BE-HOME-07 | Validation trailerUrl        | IMPLEMENTED                                                     |
| BE-HOME-08 | Hero editorial metadata      | Chờ nhu cầu và workflow quản trị                                |
| BE-HOME-09 | Promotion API                | Chờ contract khuyến mãi thật                                    |
| BE-HOME-10 | Membership API               | Chờ contract thành viên thật                                    |
| BE-HOME-11 | Timezone lịch chiếu          | IMPLEMENTED trong FE và quy ước API bookable                    |
| BE-HOME-12 | CORS deployment              | Có cấu hình source; chờ xác minh runtime                        |

## 5. BE-HOME-01 — Truy vấn suất chiếu mở bán

**Trạng thái: IMPLEMENTED.**

Method/path: GET /api/v1/showtimes/bookable.

| Query    | Bắt buộc | Ý nghĩa            |
| -------- | -------- | ------------------ |
| cinemaId | Có       | UUID rạp           |
| from     | Có       | ISO-8601 có offset |
| to       | Có       | ISO-8601 có offset |
| movieId  | Không    | UUID phim          |

Không có request body.

Response 200 là List<ShowtimeResponse> trực tiếp.
Không có kết quả trả [].

Điều kiện truy vấn:

- Showtime OPEN_FOR_BOOKING.
- startsAt thuộc [from, to).
- startsAt > thời điểm server kiểm tra.
- Room và Cinema active.
- Lọc movieId nếu được truyền.
- Sắp xếp startsAt ASC, id ASC.

from phải trước to. Query sai trả 400.
Không tìm thấy suất phù hợp, kể cả cinemaId không khớp dữ liệu,
trả danh sách rỗng.

Giới hạn khoảng truy vấn dùng cinema.inventory.bookable.maximum-range.
Service chỉ áp dụng giới hạn khi giá trị khác null.
BookableShowtimeProperties khởi tạo 7 ngày; application.yml còn có
INVENTORY_BOOKABLE_MAXIMUM_RANGE. Cần kiểm tra cấu hình hiệu lực
trước khi kết luận giới hạn tại deployment.

GET public ở Inventory và Gateway.
Response không có số ghế trống.

Source chính: ShowtimeController, ShowtimeServiceImpl,
ShowtimeRepository và BookableShowtimeProperties.

## 6. BE-HOME-02 — Chính sách reservation và hold

**Trạng thái: IMPLEMENTED theo chính sách đã duyệt.**

REST hold và reservation consumer cùng yêu cầu:

- Showtime tồn tại và OPEN_FOR_BOOKING.
- startsAt > thời điểm kiểm tra của server.
- Room và Cinema active.
- Thời hạn hold còn ở tương lai.
- Ghế đáp ứng điều kiện giữ ghế của luồng hiện tại.

Dùng Clock hiện có. Consumer lấy lại thời gian sau khi khóa ghế
để kiểm tra expiration và eligibility.

### Retry và replay

- Event mới phải kiểm tra lại eligibility.
- Hold cùng booking chỉ được tái sử dụng khi còn hạn và
  holdExpiresAt trùng với payload.
- Event đã xử lý trả alreadyProcessed trước kiểm tra eligibility.
- REST hold chỉ nhận ghế AVAILABLE; không tái sử dụng ghế HELD.

### Contract

REST: PUT /api/v1/show-seats/{showSeatId}/hold.

Body giữ nguyên bookingId và expiresAt.
Response 200 là ShowSeatResponse trực tiếp.

| HTTP    | Trường hợp                                         |
| ------- | -------------------------------------------------- |
| 400     | Request hoặc expiration không hợp lệ               |
| 404     | Không tìm thấy show seat                           |
| 409     | Ghế không AVAILABLE hoặc suất không đủ eligibility |
| 401/403 | Không xác thực hoặc thiếu quyền                    |

Suất không đủ eligibility trả
INVENTORY_SHOWTIME_NOT_BOOKABLE trong error envelope hiện có.

Consumer tạo rejection outbox với reason INVALID_REQUEST.
Expiration hết hạn dùng RESERVATION_EXPIRED.
Không thêm event reason hoặc event version mới.

REST hold tiếp tục yêu cầu inventory:write.

### Confirm và giải phóng ghế

- Giữ logic confirm hiện tại.
- Không thêm eligibility gate vào release, expiration hoặc compensation.
- Giữ processed-event registration, transaction và khóa ghế.

Eligibility dùng dữ liệu nhìn thấy trong transaction.
Việc tuần tự hóa tuyệt đối với thay đổi trạng thái suất/phòng/rạp
đồng thời thuộc phạm vi Inventory lock hardening.

Source chính: ShowSeatServiceImpl,
SeatReservationRequestedConsumerServiceImpl và ShowtimeRepository.

## 7. BE-HOME-06 — Age rating

**Trạng thái: IMPLEMENTED trong source.**

- AgeRating gồm P, K, T13, T16, T18.
- Entity và response cho phép null khi chưa có dữ liệu.
- API metadata yêu cầu movie:manage.
- FE hiển thị P, K, 13+, 16+, 18+; null hiển thị trạng thái thiếu dữ liệu.
- Chưa áp dụng kiểm tra tuổi vào reservation/hold.

Script scripts/cinestar_movie_age_ratings.sql bổ sung rating
cho dữ liệu Cinestar đã seed, theo snapshot ngày 2026-10-05.

Script chỉ cập nhật rating đang null, giữ giá trị đã quản trị.
Không suy đoán rating từ tên phim.
Việc script đã được chạy trên một database cần được xác nhận riêng.

## 8. BE-HOME-07 — trailerUrl

**Trạng thái: IMPLEMENTED.**

Create và update trong MovieServiceImpl:

- Nhận null.
- Trim khoảng trắng đầu/cuối.
- Chuyển chuỗi rỗng hoặc toàn khoảng trắng thành null.
- Giới hạn đầu vào 500 ký tự trước khi trim, cùng giới hạn DTO.
- Yêu cầu URI tuyệt đối với scheme HTTP hoặc HTTPS và host hợp lệ.
- Không chấp nhận port lớn hơn 65535.

Lỗi service dùng INVALID_TRAILER_URL hoặc TRAILER_URL_TOO_LONG.
Validation DTO có thể trả lỗi validation chung trước khi vào service.

Chưa có host allowlist được xác nhận trong phạm vi này.
Không tự thêm chính sách host mới.

Chuẩn hóa áp dụng khi gọi create/update qua service.
SQL seed không đi qua validation này; dữ liệu cũ không được tự backfill.

## 9. BE-HOME-03 — backdropUrl

**Trạng thái: IMPLEMENTED trong source; chờ bổ sung ảnh ngang xác minh được.**

- Migration V3 thêm backdrop_url nullable.
- Backend trả metadata gốc, không thay backdropUrl bằng posterUrl.
- API metadata cập nhật URL qua quyền movie:manage.
- Hero ưu tiên backdropUrl, sau đó posterUrl, rồi fallback hiện có.
- Movie cards tiếp tục dùng posterUrl.
- Chưa thêm upload API hoặc tự tạo dữ liệu ảnh ngang.

## 10. BE-HOME-04 — Movie catalog

**Trạng thái: IMPLEMENTED ở Backend và generated client.**

Method/path: GET /api/v1/movies/catalog.

| Query  | Mặc định      | Ý nghĩa                   |
| ------ | ------------- | ------------------------- |
| status | Không lọc     | MovieStatus               |
| genre  | Không lọc     | UUID genre                |
| page   | 0             | Chỉ số trang bắt đầu từ 0 |
| size   | Theo cấu hình | Số phim mỗi trang         |

Không có request body.

Cấu hình source: default-size = 8, max-size = 100.
Có thể override bằng MOVIE_CATALOG_DEFAULT_SIZE và
MOVIE_CATALOG_MAX_SIZE.

Response 200 là PageResponse<MovieResponse> trực tiếp:

- content: danh sách phim.
- page.page: chỉ số trang.
- page.size: kích thước trang.
- page.totalElements: tổng số phim.
- page.totalPages: tổng số trang.
- page.first và page.last: cờ trang đầu/cuối.

Không bọc thêm success envelope.

Sắp xếp releaseDate DESC, ngày null ở cuối, id ASC.
Không truyền status nghĩa là không lọc trạng thái.
genre là UUID, không phải tên thể loại.

Paging/filter sai trả 400.
Genre UUID hợp lệ nhưng không có phim khớp trả trang rỗng.
Nếu phim bị thiếu giữa bước đọc ID và bước tải entity,
service có thể trả MOVIE_NOT_FOUND / 404.

GET /api/v1/movies vẫn trả List<MovieResponse>.
GET catalog public theo security hiện tại.

Generated client đã có getMovieCatalog.
movie-queries.ts và Home vẫn dùng findAll.
Việc chuyển caller sang paging server-side là phần FE riêng.

Không thêm cinemaId vào MovieController.

## 11. BE-HOME-11 — Timezone

**Trạng thái: đã có quy ước và implementation FE.**

- Timezone nghiệp vụ FE: Asia/Ho_Chi_Minh.
- cinema-time.ts nhóm ngày và định dạng giờ theo timezone này.
- getCinemaDayRange tạo khoảng ngày từ 00:00 +07:00
  rồi gửi ISO timestamp UTC tương đương.
- API bookable dùng [from, to).
- Clock kiểm tra thời điểm server.
- Không đổi semantics của API khoảng thời gian cũ.
- Không thêm timezone riêng từng Cinema khi chưa có nhu cầu.

Chưa xác minh runtime trên các timezone trình duyệt trong lần đối chiếu này.

## 12. BE-HOME-08 — API cung cấp phim nổi bật cho HeroBanner

### 12.1 Trạng thái và baseline

Trạng thái: PROPOSED — chưa triển khai.

Đã đối chiếu:

- Backend: `00c759d574e44edc855cff866c9cc1a66af2c72b`.
- Frontend: `d1517b639e9859a12e7e4dca3dc3081123c39e43`.

Backend hiện có:

- `GET /api/v1/movies`.
- `GET /api/v1/movies/catalog`.
- Movie metadata gồm poster, trailer, trạng thái và thể loại.
- Public GET cho Movie API.
- Quyền `movie:manage` cho thao tác quản lý phim.

Backend chưa có:

- API trả danh sách phim dành riêng cho Hero.
- Metadata quản lý việc xuất hiện và thứ tự phim trên Hero.
- Thống kê popularity theo phim.
- `backdropUrl` trong MovieResponse tại baseline này.

Không coi endpoint hoặc field đề xuất bên dưới là contract đã tồn tại.

### 12.2 Yêu cầu sản phẩm

- Hero hiển thị tối đa 4 phim đang chiếu.
- Danh sách và thứ tự hiển thị do Backend cung cấp.
- Không dùng danh sách movie ID cố định trong FE.
- Không dùng phim mới nhất, dữ liệu mock hoặc nội dung mẫu để thay kết quả Hero.
- Phim phải phù hợp với rạp đã chọn khi người dùng có location.
- Khi không có phim phù hợp, trả danh sách rỗng.
- Animation và cơ chế chuyển slide tiếp tục thuộc FE.

Trong phạm vi này, phim nổi bật là lựa chọn biên tập được quản trị
trong DB qua API. Không diễn giải thứ tự biên tập thành thống kê
doanh số, lượt xem hoặc số vé bán.

### 12.3 Metadata đề xuất trong Movie Service

Bổ sung các field quản lý Hero:

| Field          | Ý nghĩa                                |
| -------------- | -------------------------------------- |
| `heroEnabled`  | Cho phép phim xuất hiện trên Hero      |
| `heroPriority` | Thứ tự ưu tiên; số nhỏ xuất hiện trước |
| `heroStartsAt` | Thời điểm bắt đầu hiển thị; nullable   |
| `heroEndsAt`   | Thời điểm kết thúc hiển thị; nullable  |

Quy tắc:

- Migration đặt `heroEnabled = false` cho dữ liệu hiện có.
- Không seed danh sách phim nổi bật bằng ID cố định.
- Phim chỉ được đưa lên Hero sau khi quản trị cập nhật qua API.
- `heroPriority` là số nguyên không âm.
- Khi có cả hai thời điểm, `heroEndsAt` phải sau `heroStartsAt`.
- Thời gian API có offset; so sánh theo instant.
- Khoảng hiển thị là `[heroStartsAt, heroEndsAt)`.
- Giá trị null có nghĩa là không giới hạn ở phía tương ứng.

### 12.4 Public API đề xuất

Endpoint mới:

`GET /api/v1/movies/hero`

Operation ID đề xuất: `getHeroMovies`.

Query parameters:

| Parameter  | Quy tắc                                             |
| ---------- | --------------------------------------------------- |
| `limit`    | Mặc định 4; tối thiểu 1; tối đa 10                  |
| `movieIds` | Bộ lọc ID phim tùy chọn, lấy từ dữ liệu API hiện có |

Default limit và max limit phải nằm trong configuration properties.

`movieIds` nhận các UUID thực tế, không phải danh sách cấu hình
cố định trong FE. Cần xác định giới hạn số ID và validation
trong OpenAPI trước khi triển khai.

Quy tắc truy vấn:

1. Chỉ lấy Movie có trạng thái `NOW_SHOWING`.
2. Chỉ lấy Movie có `heroEnabled = true`.
3. Kiểm tra thời hạn hiển thị theo clock phía Backend.
4. Áp dụng `movieIds` nếu request có bộ lọc.
5. Sắp xếp `heroPriority ASC`, sau đó `id ASC`.
6. Áp dụng `limit` sau khi lọc và sắp xếp.
7. Không đủ phim thì trả ít hơn limit.
8. Không có phim phù hợp thì trả danh sách rỗng.

Response đề xuất: danh sách `MovieResponse`, theo đúng thứ tự Hero.

Giữ convention response/envelope hiện có của service.
Giữ nguyên contract của `/movies` và `/movies/catalog`.

Endpoint GET này public như Movie catalog hiện tại.
Không yêu cầu đăng nhập chỉ để xem Hero.

### 12.5 Quản trị nội dung qua API

Endpoint ghi đề xuất:

`PATCH /api/v1/movies/{movieId}/hero`

Payload quản lý:

- `heroEnabled`.
- `heroPriority`.
- `heroStartsAt`.
- `heroEndsAt`.

Endpoint này chỉ cập nhật cấu hình Hero, không thay toàn bộ Movie.

Yêu cầu:

- Bảo vệ bằng quyền `movie:manage` tại service.
- Kiểm tra Gateway cho phương thức PATCH và route mới.
- Phim không tồn tại trả lỗi theo convention hiện có.
- Validation priority và khoảng thời gian phải thực hiện ở Backend.
- Response và OpenAPI phải mô tả cấu hình sau cập nhật.
- Không mở public write hoặc thay đổi luồng đăng nhập hiện có.

Có thể quản lý qua Swagger/API trước khi FE admin có màn hình tương ứng.

### 12.6 Áp dụng city/rạp đã chọn

Movie Service không sở hữu cinema, room hoặc showtime.
Không truy cập database của Inventory để lọc rạp.

Luồng FE đề xuất:

- Không có rạp đã chọn: gọi Hero API không có `movieIds`.
- Có rạp đã chọn:
  - Lấy danh sách phim hợp lệ từ chương trình của rạp bằng API hiện có.
  - Truyền các movie ID thực tế vào Hero API.
  - Backend lọc các ID đó trước khi áp dụng limit.
- Chương trình rạp đang tải: hiển thị loading.
- Chương trình rạp tải lỗi: hiển thị lỗi và cho thử lại.
- Danh sách phim của rạp rỗng: hiển thị Hero rỗng,
  không bỏ bộ lọc rồi gọi Hero toàn hệ thống.

Query key FE phải chứa cinema ID, tập ID ứng viên và limit.
Chuẩn hóa thứ tự ID trong query key để cache ổn định.

Danh sách ứng viên chỉ là bộ lọc.
Backend vẫn kiểm tra trạng thái Movie và cấu hình Hero.

### 12.7 Ảnh và dữ liệu thiếu

- Dùng `backdropUrl` khi đề xuất tại mục 9 đã được triển khai.
- Nếu backdrop thiếu, dùng `posterUrl` từ API.
- Nếu ảnh thiếu hoặc tải lỗi, dùng nền/placeholder trung tính.
- Không gán ảnh phim mẫu hoặc nội dung phim giả.
- Trailer thiếu thì xử lý trạng thái chưa có trailer.
- Không suy đoán age rating.

Không dùng field chưa tồn tại trong generated client.

### 12.8 Tích hợp FE sau khi Backend hoàn thành

- Cập nhật OpenAPI snapshot và sinh lại Orval client.
- Tạo query Hero trong feature Home.
- Mapper chuyển response thành model Hero phù hợp.
- HeroBanner nhận dữ liệu từ query mới.
- NowShowing và QuickBooking tiếp tục dùng danh sách riêng.
- Xóa selector ID tĩnh và fallback chọn phim theo releaseDate.
- Không gọi Axios trực tiếp trong HeroBanner.
- Không đổi animation hoặc tạo API timer.
- Kết quả rỗng và lỗi không được thay bằng dữ liệu tĩnh.

Chưa nối FE vào endpoint đề xuất trước khi Backend implement
và contract runtime được xác nhận.

### 12.9 Xếp hạng hot tự động — phạm vi riêng

Nếu sản phẩm yêu cầu hot theo số vé bán, cần thiết kế nguồn
thống kê và tiêu chí xếp hạng riêng.

Booking hiện có `showtimeId`, `confirmedAt` và thông tin ghế;
BookingConfirmedPayload chưa có `movieId`.

Vì vậy không được giả định đã có thống kê theo phim.

Thiết kế sau cần xác định:

- Cửa sổ thống kê và đơn vị đếm: vé hay booking.
- Cách liên kết showtime với movie bằng API/event hoặc read model.
- Xử lý event trùng lặp, cancellation và refund.
- Độ trễ cập nhật và khả năng đối soát.
- Ranh giới sở hữu dữ liệu giữa Booking, Inventory và Movie.

Không join trực tiếp database giữa các service.
Không đưa số liệu giả vào Hero để mô phỏng ranking.

### 12.10 Kiểm tra thủ công sau triển khai

- Chưa cấu hình phim nổi bật: API trả danh sách rỗng.
- Bật phim qua API quản trị: public Hero API trả phim tương ứng.
- Đổi priority: thứ tự response thay đổi và ổn định.
- Phim UPCOMING, ENDED hoặc INACTIVE không xuất hiện.
- Kiểm tra thời điểm bắt đầu và kết thúc hiển thị.
- Kiểm tra default limit, giới hạn limit và UUID không hợp lệ.
- Kiểm tra bộ lọc movieIds được áp dụng trước limit.
- Đổi rạp trên FE: Hero chỉ hiển thị phim phù hợp với rạp mới.
- API lỗi hoặc trả rỗng: không xuất hiện dữ liệu mock hoặc ID tĩnh.
- Public GET hoạt động không cần đăng nhập.
- PATCH không có quyền bị từ chối theo security convention.
- Client cũ của `/movies` và `/movies/catalog` tiếp tục hoạt động.

### 12.11 Chi tiết contract triển khai Hero

Chỉ chuyển trạng thái sang IMPLEMENTED sau khi áp dụng source.

Public:

- GET /api/v1/movies/hero.
- operationId: getHeroMovies.
- Response: List<MovieResponse> trực tiếp.
- limit mặc định 4, tối đa 10; cấu hình có thể giảm các giới hạn.
- movieIds tùy chọn, dạng repeated query parameter.
- Tối đa 100 ID; giới hạn hiệu lực theo cinema.movie.hero.max-movie-ids.
- Thiếu movieIds: không lọc ứng viên.
- Có bộ lọc rỗng: trả [].
- UUID không tồn tại trong bộ lọc không gây 404.
- Lọc ứng viên trước limit.
- Chỉ trả NOW_SHOWING, heroEnabled và còn trong thời hạn.
- Thứ tự heroPriority ASC, id ASC.
- Không có phim phù hợp trả [].

Quản trị:

- PATCH /api/v1/movies/{movieId}/hero.
- operationId: updateMovieHero.
- Yêu cầu movie:manage.
- Không gửi field: giữ giá trị hiện tại.
- heroEnabled và heroPriority không nhận null.
- heroStartsAt/heroEndsAt nhận null để xóa giới hạn.
- Body không có field Hero hợp lệ trả 400.
- Kiểm tra khoảng thời gian trên cấu hình sau khi merge.
- Response: MovieHeroConfigurationResponse trực tiếp.

Dữ liệu:

- Migration V4 mặc định tắt Hero cho mọi phim.
- Không seed danh sách phim nổi bật.
- Thời gian chuẩn hóa UTC, độ chính xác microsecond.
- Không thay request CRUD hoặc public MovieResponse hiện tại.

FE:

- Chỉ nối query Hero sau khi xác nhận spec runtime và generated client.
- FE mặc định yêu cầu 4 phim.
- Có rạp: truyền ID ứng viên từ chương trình rạp.
- Chương trình rạp rỗng hoặc lỗi không được chuyển thành Hero toàn hệ thống.

## 13. BE-HOME-09 — Promotion

**Trạng thái: chờ contract khuyến mãi thật.**

Cần xác định thời hạn, đối tượng áp dụng, giá trị giảm,
giới hạn sử dụng, validation checkout và quyền quản trị.

Nội dung minh họa không thay thế contract khuyến mãi giao dịch.
Không thêm dữ liệu khuyến mãi giả vào Movie hoặc Inventory response.

## 14. BE-HOME-10 — Membership

**Trạng thái: chờ contract thành viên thật.**

Cần xác nhận nguồn thành viên, quyền lợi, điều kiện áp dụng
và service sở hữu nghiệp vụ.

Không thêm quyền lợi hoặc dữ liệu thành viên suy đoán
vào Movie, Cinema hoặc Showtime response.

## 15. BE-HOME-05 — Availability

**Trạng thái: chưa có contract availability tổng hợp được duyệt.**

Inventory đã có dữ liệu show_seats và trạng thái ghế.
Điều đó chưa xác định contract tổng hợp cho Home.

Trước khi thêm field cần chốt:

- Cách tính AVAILABLE, HELD và hold hết hạn.
- Thời điểm đọc, độ trễ cho phép và cache.
- Dữ liệu show_seats đã đầy đủ cho các suất cần hiển thị hay chưa.
- Cách truy vấn batch, tránh một request cho từng suất.

Availability là thông tin tại thời điểm đọc;
hold vẫn phải kiểm tra và khóa ghế lại.

Không coi snapshot seed từ Cinestar là kết nối availability trực tiếp.
Không thêm availability vào Movie Service.

## 16. BE-HOME-12 — CORS và deployment

**Trạng thái: có cấu hình source; chưa xác minh deployment.**

Cấu hình Gateway nằm tại:
infrastructure/config-service/src/main/resources/config-repo/gateway-service.yml.

Các giá trị mặc định trong source:

- CORS_ALLOWED_ORIGIN_PATTERN: http://localhost:\*.
- CORS_FE_ORIGIN: https://cinematic-web-beta.vercel.app.
- CORS_FE_PREVIEW_ORIGIN:
  https://cinematic-2awjfcb0p-huykunnes-projects.vercel.app.

Gateway cho phép OPTIONS qua security.
Global CORS đã có add-to-simple-url-handler-mapping,
danh sách methods/headers và allow-credentials.

Cần kiểm tra cấu hình hiệu lực, origin FE thực tế,
preflight và response qua Gateway.
Không kết luận CORS đã hoạt động chỉ từ YAML.

Nếu FE gọi trực tiếp service thay vì Gateway,
cần kiểm tra CORS của service đó riêng.

## 17. OpenAPI và generated client

Backend source là nguồn xác minh contract.
Runtime spec được phục vụ tại /v3/api-docs của từng service.

orval.config.ts hiện đọc trực tiếp spec local của:
Movie, User, Inventory, Booking và Payment.

Script hiện tại: npm run api.

Output hiện tại: src/services/api/generated/<service>/.
Không sửa generated DTO/client bằng tay.

openapi/ dành cho snapshot đã review và metadata nguồn.
Tại baseline FE, thư mục này mới có README;
Orval hiện không đọc snapshot làm input.

Không generate R28 Notification khi đang hoãn.
Không đổi route, operationId hoặc response để khớp tài liệu cũ.

## 18. Kiểm tra thủ công đề xuất

Đây là checklist để người triển khai kiểm tra runtime khi phù hợp.
Không ghi nhận các bước dưới đây đã pass chỉ vì đã đọc source.

### Bookable

- Request hợp lệ trả danh sách hoặc [].
- from được bao gồm, to không được bao gồm.
- Không trả suất đã bắt đầu, chưa mở bán hoặc phòng/rạp inactive.
- Đối chiếu giới hạn khoảng truy vấn theo cấu hình hiệu lực.

### Reservation và hold

- Suất đủ điều kiện tạo được hold.
- Suất quá giờ, không mở bán hoặc phòng/rạp inactive bị từ chối.
- Expiration hết hạn trong lúc chờ khóa bị từ chối.
- Retry cùng booking giữ điều kiện expiration trùng khớp.
- Replay không tạo thêm hold/outbox.
- Confirm, release và compensation giữ hành vi hiện tại.
- Route ghi dữ liệu giữ đúng quyền.

### Movie catalog và trailer

- API danh sách cũ giữ kiểu response.
- Catalog có filter, paging, trang rỗng và thứ tự ổn định.
- Response dùng content/page đúng như generated DTO.
- Create/update chuẩn hóa trailer rỗng thành null.
- URL không hợp lệ bị từ chối theo validation hiện có.

### FE và deployment

- Ngày/giờ vẫn đúng khi timezone trình duyệt khác Việt Nam.
- QuickBooking xử lý loading, empty, error và suất không còn hợp lệ.
- Loại phòng được lấy từ Room API.
- Preflight đúng origin hoạt động qua Gateway.

## 19. Phần còn lại và thứ tự xử lý

1. Đồng bộ tài liệu với implementation và baseline mới.
2. Xác minh runtime BE-HOME-12 tại môi trường deploy thực tế.
3. Chỉ triển khai backdrop/age rating khi có nguồn dữ liệu và quy ước.
4. Chỉ thêm availability tổng hợp khi contract được duyệt.
5. Editorial, Promotion và Membership theo contract riêng.
6. Chuyển FE Movies sang catalog server-side khi có nhu cầu;
   không cần thêm lại endpoint Backend.

Không coi seed database là migration triển khai các metadata chưa có.
Không mở rộng scope sang R28 Notification.

## 20. Source tham chiếu

Backend:

- services/inventory-service/src/main/java/com/cinema/inventory/controller/ShowtimeController.java
- services/inventory-service/src/main/java/com/cinema/inventory/repository/ShowtimeRepository.java
- services/inventory-service/src/main/java/com/cinema/inventory/service/impl/ShowSeatServiceImpl.java
- services/inventory-service/src/main/java/com/cinema/inventory/service/impl/SeatReservationRequestedConsumerServiceImpl.java
- services/movie-service/src/main/java/com/cinema/movie/controller/MovieController.java
- services/movie-service/src/main/java/com/cinema/movie/service/impl/MovieServiceImpl.java
- services/movie-service/src/main/java/com/cinema/movie/repository/MovieRepository.java
- common/common-response/src/main/java/com/cinema/common/response/model/PageResponse.java
- common/common-response/src/main/java/com/cinema/common/response/model/PageInfo.java

Frontend:

- src/features/home/api/home-queries.ts
- src/features/home/components/QuickBooking.vue
- src/features/movies/api/movie-queries.ts
- src/features/cinemas/api/cinema-programme.queries.ts
- src/utils/cinema-time.ts
- src/services/api/generated/inventory-service/showtime-controller/showtime-controller.ts
- src/services/api/generated/movie-service/movie-controller/movie-controller.ts
- orval.config.ts
- package.json
- docs/design/reference/html-convert
