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
| HuyKunNe/cinema-system | 4079c22fa1aa1c17d04793778ee5ae2ca299b322 |
| HuyKunNe/cinematic-web | 50b74c1750088587571b862f696cc8830cfba156 |

Baseline này áp dụng cho phần Home, Movie Catalog và QuickBooking
tại mục 1–20. Các mục Booking phía sau có baseline riêng.

IMPLEMENTED nghĩa là đã có trong source tại baseline,
không đồng nghĩa đã xác minh runtime, test hoặc deployment.

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
- GET /api/v1/movies/hero đã tồn tại, trả List<MovieResponse> trực tiếp.
- PATCH /api/v1/movies/{movieId}/hero quản lý cấu hình Hero, yêu cầu movie:manage.
- Hero là lựa chọn biên tập, không phải thống kê phim bán chạy.
- MovieResponse không chứa các field cấu hình quản trị Hero.

### 2.2 Inventory

- GET /api/v1/showtimes/bookable đã tồn tại.
- ShowtimeResponse cung cấp movieId, roomId, roomName, cinemaId,
  cinemaName, startsAt, endsAt và status.
- ShowtimeResponse chưa có roomType hoặc availability tổng hợp.
- OPEN_FOR_BOOKING không bảo đảm còn ghế.

### 2.3 Caller Frontend

- home-queries.ts có useHomeBookableShowtimes và useHomeHeroMovies.
- QuickBooking lấy lịch theo phim để xác định ngày có suất.
- Với ngày đã chọn, QuickBooking gọi API bookable theo rạp/phim/ngày.
- QuickBooking đọc loại phòng từ Room API rồi ghép theo roomId.
- cinema-programme.queries.ts lấy phòng active và lịch theo từng phòng.
- HomePage dùng useHomeHero cho Hero; danh sách Home còn lại tiếp tục dùng useHomeProgramme.
- Khi có rạp, Hero truyền ID ứng viên từ chương trình rạp.
- Rạp không có ứng viên: Hero hiển thị rỗng và không gọi toàn hệ thống.
- Lỗi chương trình rạp: Hero hiển thị lỗi và cho thử lại.
- Axios đã serialize query array bằng tên parameter lặp.
- movie-queries.ts vẫn dùng findAll cho Movies.
- MoviesPage lọc trạng thái/thể loại, sắp xếp và tăng số phim hiển thị tại FE; chưa dùng paging server-side.
- Generated client có getMovieCatalog, getHeroMovies và updateMovieHero.
- cinema-time.ts dùng Asia/Ho_Chi_Minh.
- openapi/ hiện chỉ có README; chưa có snapshot JSON đã review.

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

| ID         | Nội dung                     | Trạng thái tại baseline                                                    |
| ---------- | ---------------------------- | -------------------------------------------------------------------------- |
| BE-HOME-01 | Truy vấn suất mở bán         | IMPLEMENTED; có generated client và caller FE                              |
| BE-HOME-02 | Eligibility reservation/hold | IMPLEMENTED; chưa xác minh đầy đủ runtime                                  |
| BE-HOME-03 | backdropUrl nullable         | IMPLEMENTED; còn bổ sung ảnh ngang từ nguồn xác nhận                       |
| BE-HOME-04 | Movie catalog paging/filter  | IMPLEMENTED ở BE và generated client; Movies vẫn dùng findAll              |
| BE-HOME-05 | Availability tổng hợp        | Chờ contract và cách tính được duyệt                                       |
| BE-HOME-06 | Age rating                   | IMPLEMENTED; dữ liệu nullable, có script bổ sung rating                    |
| BE-HOME-07 | Validation trailerUrl        | IMPLEMENTED                                                                |
| BE-HOME-08 | Hero editorial metadata      | IMPLEMENTED ở BE, generated client và caller FE; cấu hình nội dung qua API |
| BE-HOME-09 | Promotion API                | Chờ contract khuyến mãi thật                                               |
| BE-HOME-10 | Membership API               | Chờ contract thành viên thật                                               |
| BE-HOME-11 | Timezone lịch chiếu          | IMPLEMENTED trong FE và quy ước API bookable                               |
| BE-HOME-12 | CORS deployment              | Có cấu hình source; chờ xác minh runtime tại môi trường deploy             |

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

Trạng thái: IMPLEMENTED trong Backend, generated client và caller FE.

Baseline theo mục 2. Chưa ghi nhận kiểm tra runtime đầy đủ cho
cấu hình, phạm vi rạp và trạng thái hiển thị.

Source Backend:

- MovieHeroController.
- MovieHeroService.
- MovieRepository.
- MovieHeroProperties.
- MovieHeroConfigurationResponse.
- Migration V4\_\_add_movie_hero_configuration.sql.

Source Frontend:

- home-queries.ts.
- use-home-hero.ts.
- HomePage.vue.
- HeroBanner.vue.
- Generated movie-hero-controller.ts.
- axios-instance.ts.

### 12.2 Ý nghĩa phim nổi bật

Phim nổi bật là lựa chọn biên tập được quản lý qua API.

Không diễn giải heroPriority thành doanh số, lượt xem,
số vé bán hoặc ranking tự động.

FE yêu cầu tối đa 4 phim và giữ thứ tự Backend trả về.
Carousel, animation và autoplay thuộc FE.

### 12.3 Metadata đã implement

| Field        | Quy tắc                                         |
| ------------ | ----------------------------------------------- |
| heroEnabled  | Cho phép xuất hiện trên Hero                    |
| heroPriority | Số nguyên không âm; số nhỏ đứng trước           |
| heroStartsAt | Nullable; thời điểm bắt đầu được bao gồm        |
| heroEndsAt   | Nullable; thời điểm kết thúc không được bao gồm |

- Khi có cả hai mốc, heroEndsAt phải sau heroStartsAt.
- Null nghĩa là không giới hạn ở phía tương ứng.
- API chuẩn hóa thời gian về UTC với độ chính xác microsecond.
- Metadata quản trị được trả qua MovieHeroConfigurationResponse.
- Public MovieResponse giữ metadata phim hiện có.

### 12.4 Public API hiện có

GET /api/v1/movies/hero

operationId: getHeroMovies.
Public GET; không có request body.

| Query    | Quy tắc                                      |
| -------- | -------------------------------------------- |
| limit    | Không bắt buộc; mặc định cấu hình 4          |
| movieIds | Bộ lọc UUID tùy chọn; gửi bằng tên query lặp |

- maxLimit mặc định 10; cấu hình có thể giảm giới hạn.
- maxMovieIds mặc định 100; cấu hình có thể giảm giới hạn.
- Thiếu movieIds: truy vấn toàn hệ thống.
- Bộ lọc được áp dụng trước limit.
- UUID hợp lệ nhưng không khớp dữ liệu không gây 404.

Điều kiện và thứ tự:

1. Movie có trạng thái NOW_SHOWING.
2. heroEnabled = true.
3. Đang trong khoảng [heroStartsAt, heroEndsAt).
4. Thuộc movieIds nếu có bộ lọc.
5. Sắp xếp heroPriority ASC, id ASC.
6. Áp dụng limit.

Response 200: List<MovieResponse> trực tiếp.
Không có kết quả trả [].

Query sai trả 400.
Phim mất giữa bước chọn ID và tải entity có thể trả 404.

### 12.5 API quản trị hiện có

PATCH /api/v1/movies/{movieId}/hero

operationId: updateMovieHero.
Yêu cầu movie:manage.

Body có thể cập nhật từng phần:

- heroEnabled.
- heroPriority.
- heroStartsAt.
- heroEndsAt.

Quy tắc:

- Field không gửi: giữ giá trị hiện tại.
- heroEnabled và heroPriority không nhận null.
- heroStartsAt và heroEndsAt nhận null để xóa giới hạn.
- Request không có thay đổi Hero hợp lệ bị từ chối.
- Backend kiểm tra khoảng thời gian sau khi merge với cấu hình cũ.

Response 200: MovieHeroConfigurationResponse trực tiếp, gồm
movieId, heroEnabled, heroPriority, heroStartsAt và heroEndsAt.

Các lỗi: 400 validation, 404 phim không tồn tại,
401 chưa xác thực, 403 thiếu quyền.

### 12.6 Phạm vi rạp tại FE

Movie Service không sở hữu Cinema, Room hoặc Showtime.
Không truy cập trực tiếp database của Inventory.

use-home-hero.ts hiện thực:

- Chưa chọn rạp: gọi Hero không có movieIds.
- Có rạp: chờ xác minh location và chương trình rạp.
- ID ứng viên lấy từ suất OPEN_FOR_BOOKING trong tương lai,
  thuộc rạp đã chọn.
- Chương trình rạp đang tải: Hero loading.
- Chương trình rạp lỗi: Hero error và cho thử lại.
- Tập ứng viên rỗng: Hero rỗng, không gọi toàn hệ thống.

Query key chứa cinemaId, limit và ID ứng viên đã chuẩn hóa.
FE không cắt tập ứng viên trước khi Backend áp dụng limit.

Bộ lọc ứng viên không thay thế kiểm tra reservation/hold
tại Backend.

### 12.7 Ảnh và dữ liệu thiếu

- Hero ưu tiên backdropUrl, sau đó posterUrl.
- Nếu các ảnh đều thiếu hoặc tải thất bại, vẫn hiển thị nội dung
  phim với nền hiện có.
- Trailer thiếu được hiển thị theo trạng thái chưa có trailer.
- Age rating được đọc từ API; không suy đoán.

backdropUrl null không phải điều kiện loại phim khỏi Hero.

### 12.8 Tích hợp FE hiện có

- HomePage truyền heroMovies từ useHomeHero vào HeroBanner.
- Query gọi generated getHeroMovies và dùng toHomeMovie.
- NowShowing, Upcoming và QuickBooking dùng dữ liệu riêng.
- Query Hero refetch mỗi 30 giây khi tab hoạt động.
- Không có cam kết cập nhật tức thời.
- Kết quả rỗng/lỗi không được thay bằng danh sách phim tĩnh.
- Generated client đã có; openapi/ chưa có snapshot JSON đã review.

### 12.9 Xếp hạng hot tự động — phạm vi riêng

Chưa có contract ranking phim theo doanh số/lượt xem được xác nhận.

Nếu cần, phải xác định nguồn thống kê, cửa sổ thời gian,
cách liên kết booking với movie, xử lý replay/cancellation/refund
và khả năng đối soát.

Không join trực tiếp database giữa các service.
Không dùng số liệu giả để mô phỏng ranking.

### 12.10 Kiểm tra thủ công

- Chưa bật phim: API trả [].
- Bật phim NOW_SHOWING qua PATCH: GET trả phim khi đủ điều kiện.
- Đổi priority: thứ tự response thay đổi ổn định.
- UPCOMING, ENDED và INACTIVE không xuất hiện.
- Kiểm tra mốc bắt đầu được bao gồm và mốc kết thúc bị loại.
- Query sai, UUID sai và giới hạn vượt cấu hình bị từ chối.
- Bộ lọc ID được áp dụng trước limit.
- Rạp rỗng/lỗi không chuyển sang Hero toàn hệ thống.
- Đổi rạp không hiển thị kết quả của rạp cũ.
- Public GET và quyền PATCH hoạt động đúng.
- Client cũ của /movies và /movies/catalog tiếp tục hoạt động.

### 12.11 Khởi tạo nội dung và chẩn đoán danh sách rỗng

Migration V4 đặt hero_enabled = false.
Các script Cinestar hiện tại không bật cấu hình Hero.

Vì vậy, phim đã seed và có NOW_SHOWING vẫn chưa đủ điều kiện
xuất hiện trên Hero.

Quản trị chọn phim qua PATCH /api/v1/movies/{movieId}/hero.
Gửi heroEnabled = true để bật; field còn lại có thể bỏ qua
nếu muốn giữ cấu hình hiện tại.

Khi GET trả []:

1. Kiểm tra phim tồn tại và có NOW_SHOWING.
2. Kiểm tra hero_enabled.
3. Kiểm tra hero_starts_at và hero_ends_at.
4. Kiểm tra phim có thuộc tập movieIds được gửi hay không.
5. Kiểm tra request đang tới đúng service/database.

GET Movie thông thường không trả cấu hình quản trị Hero.
Không thể kết luận heroEnabled từ MovieResponse.

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

1. Hoàn tất xác minh runtime Hero sau khi cấu hình nội dung:
   response, thứ tự và phạm vi rạp.
2. Xác minh BE-HOME-12 tại môi trường deploy thực tế:
   origin, cấu hình hiệu lực và CORS preflight qua Gateway.
3. Bổ sung ảnh ngang và rating từ nguồn xác nhận khi cần.
   Schema và API metadata đã implement.
4. Availability, Promotion và Membership chỉ triển khai
   sau khi có contract nghiệp vụ được duyệt.
5. Chuyển Movies sang catalog server-side khi có nhu cầu.
   Cần xử lý phạm vi rạp và các lựa chọn sắp xếp hiện tại
   trước khi thay caller.
6. Lưu snapshot OpenAPI đã review và metadata nguồn tại openapi/.

Không phân trang danh mục toàn hệ thống rồi chỉ lọc theo rạp
trên từng trang: cách đó làm thiếu kết quả và sai tổng số phim.

API catalog hiện chưa nhận bộ lọc ID ứng viên hoặc sort parameter.
Nếu cần mở rộng contract, ưu tiên additive và giữ caller cũ.

Các đề xuất Booking tại mục 21–26 có baseline và trạng thái riêng.
Không coi chúng đã implement chỉ vì được ghi trong tài liệu.

Không mở lại R28 Notification.

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
- services/movie-service/src/main/java/com/cinema/movie/controller/MovieHeroController.java
- services/movie-service/src/main/java/com/cinema/movie/service/MovieHeroService.java
- services/movie-service/src/main/java/com/cinema/movie/config/MovieHeroProperties.java
- services/movie-service/src/main/java/com/cinema/movie/dto/response/MovieHeroConfigurationResponse.java
- services/movie-service/src/main/resources/db/migration/V4__add_movie_hero_configuration.sql

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
- src/features/home/composables/use-home-hero.ts
- src/features/home/pages/HomePage.vue
- src/features/home/components/HeroBanner.vue
- src/features/home/mappers/home-movie.mapper.ts
- src/features/home/presentation/artwork.ts
- src/features/movies/composables/use-movies-programme.ts
- src/features/movies/pages/MoviesPage.vue
- src/services/api/generated/movie-service/movie-hero-controller/movie-hero-controller.ts
- src/services/http/axios-instance.ts

## 21. Booking — baseline và luồng giao diện

Trạng thái của các đề xuất trong mục 22–25: PROPOSED.
Chưa được implement và chưa được xác minh runtime.

Ngày đối chiếu: 2026-10-06.

Baseline:

- Backend: 330ddac2138951376094aa4eb94732cf549f920c
- Frontend: d32dfd70bbd0ffaab5e5953d0409aff865cc0b6e

### 21.1 Luồng FE mục tiêu

- Header desktop/mobile mở /booking.
- /booking là điểm bắt đầu chọn rạp, phim, ngày và suất.
- Điểm bắt đầu sử dụng city/cinema đã chọn trong location.
- Movie Detail giữ nút dẫn xuống section lịch chiếu.
- Đề xuất nhãn nút: "Chọn suất chiếu".
- Chọn suất mở /booking/{showtimeId}.
- QuickBooking tiếp tục tới cùng trang này.
- Trang chọn ghế lấy cinemaId và roomId từ ShowtimeResponse.
- Không dùng location hiện tại để thay đổi phòng của suất đã chọn.
- Đổi rạp/phim/suất phải xóa lựa chọn ghế và giỏ dịch vụ cũ.
- Giữ luồng đăng nhập trực tiếp hiện có cho route/action cần xác thực.

### 21.2 Contract hiện có

Inventory:

- GET /api/v1/showtimes/{showtimeId}
- GET /api/v1/seats?roomId={roomId}
- GET /api/v1/show-seats?showtimeId={showtimeId}&availableOnly=false

Seat là ghế vật lý của phòng.
ShowSeat là giá và trạng thái ghế của một suất.

SeatResponse có rowLabel.
ShowSeatResponse không có rowLabel; liên kết với Seat bằng seatId.

GET /seats chỉ trả ghế vật lý đang active.
GET /show-seats phải lấy tất cả trạng thái để giữ vị trí ghế ổn định.

Booking:

- POST /api/v1/bookings yêu cầu xác thực.
- Request hiện có: clientRequestId, showtimeId, seatNumbers.
- Response HTTP 202, bọc trong ApiResponse<BookingResponse>.
- Booking mới có thể ở PENDING, chưa đồng nghĩa giữ ghế thành công.
- GET /api/v1/bookings/{bookingId} đọc booking của người dùng hiện tại.

Không gọi trực tiếp endpoint hold/book/release của ShowSeat
từ giao diện khách hàng: các thao tác này yêu cầu inventory:write.
Luồng giữ ghế của khách hàng đi qua Booking Service.

## 22. BE-BOOKING-01 — Layout phòng chiếu

Trạng thái: PROPOSED.
Owner đề xuất: Inventory Service.

### 22.1 Vấn đề hiện tại

Seat có seatNumber, rowLabel, seatType và active.
Room chưa có kích thước sơ đồ, vị trí màn hình hoặc layout version.

Không thể suy ra vị trí ghế từ tên ghế:

- A01 không nhất thiết nằm ở cột đầu tiên.
- Số ghế có thể bỏ qua khoảng trống hoặc lối đi.
- Hai ghế cùng rowLabel không chứng minh chúng liền kề.
- Số hàng không chứng minh vị trí màn hình.

scripts/cinestar_quoc_thanh_room06_booking.sql có source_column
trong bảng staging, nhưng dữ liệu này chưa được lưu vào model/API ghế.

Ví dụ trong seed:

- A01 có source_column = 5.
- N01 và N02 là COUPLE, có source_column lần lượt 1 và 2.

Không nhập trực tiếp source_column thành tọa độ rồi mặc định
mọi COUPLE có width bằng hai ô: cách này có thể gây chồng lấn.

Seed là dữ liệu phát triển từ một lần quan sát, không phải bằng chứng
về trạng thái ghế hoặc sơ đồ đang vận hành tại rạp.

### 22.2 Mô hình đề xuất

Layout thuộc từng Room, không dùng một sơ đồ chung cho cả Cinema.

RoomLayout:

- id
- roomId
- version
- status: DRAFT hoặc PUBLISHED — enum mới được đề xuất
- canvasWidth
- canvasHeight
- publishedAt

RoomLayoutSeat:

- layoutId
- seatId
- seatNumberSnapshot
- rowLabelSnapshot
- seatTypeSnapshot
- x
- y
- width
- height
- rotationDegrees

RoomLayoutElement:

- layoutId
- kind: SCREEN, AISLE hoặc EXIT — enum mới được đề xuất
- label
- x
- y
- width
- height
- rotationDegrees

Tọa độ dùng hệ đơn vị logic của layout, không phải pixel màn hình FE.
Gốc tọa độ nằm ở góc trên trái của canvas.

Khoảng trống không có ghế được giữ nguyên.
Chỉ đánh dấu AISLE/EXIT khi đã có dữ liệu xác nhận.

Nguồn import phải xác nhận vị trí màn hình, khoảng cách,
kích thước ghế đôi và lối đi của đúng phòng.

### 22.3 Version và validation

- PUBLISHED layout là bất biến.
- Thay đổi sơ đồ tạo version mới.
- Đề xuất Showtime lưu roomLayoutId khi tạo/generate ghế.
- Không áp dụng layout mới ngược vào suất đã tạo.
- Các tên và loại ghế dùng để đặt vé phải nhất quán với
  layout đã gắn vào suất.
- Một seatId xuất hiện tối đa một lần trong một layout.
- Seat phải thuộc đúng roomId.
- Kích thước phải dương, tọa độ phải nằm trong canvas.
- Kiểm tra ghế chồng lấn, kể cả khi có rotation.
- Không tự suy ra vị trí từ thứ tự phần tử API hoặc seatNumber.
- Không tự loại một vị trí khỏi layout đã publish khi Seat bị inactive;
  BE phải định nghĩa trạng thái sử dụng vị trí đó cho từng suất.

Quản lý layout sử dụng cơ chế phân quyền Inventory hiện có.
Read layout cho khách hàng không cấp quyền chỉnh sửa.

### 22.4 Ghế COUPLE

SeatType hiện có:

- STANDARD: capacity 1
- VIP: capacity 1
- COUPLE: capacity 2
- ACCESSIBLE: capacity 1

COUPLE là một đơn vị đặt vé có sức chứa hai người:

- Một seatId.
- Một seatNumber.
- Một thao tác chọn/bỏ chọn.
- Một lần giữ hoặc bán toàn bộ đơn vị.
- Một giá từ ShowSeatResponse.price.

Không tách COUPLE thành hai ghế độc lập.
Không nhân ShowSeatResponse.price thêm hai lần.
Không lấy capacity để suy ra width của ghế trong sơ đồ.

Giới hạn maxSeatsPerBooking hiện đếm đơn vị ghế trong request,
không phải tổng số người. Nếu cần giới hạn số người, phải bổ sung
quy tắc riêng và công bố cho FE.

## 23. BE-BOOKING-02 — Read model cho section chọn ghế

Trạng thái: PROPOSED.
Owner đề xuất: Inventory Service.

Endpoint đề xuất mới:
GET /api/v1/showtimes/{showtimeId}/seat-map

Endpoint này chưa tồn tại ở baseline.

### 23.1 Nội dung response đề xuất

- showtimeId
- cinemaId
- roomId
- layoutId
- layoutVersion
- serverTime
- canvasWidth
- canvasHeight
- elements: màn hình, lối đi và lối ra đã xác nhận
- seats: toàn bộ vị trí ghế của layout gắn với suất

Mỗi phần tử seats cung cấp:

- seatId
- showSeatId, nếu có
- seatNumber
- rowLabel
- seatType
- capacity
- x, y, width, height, rotationDegrees
- price
- currency
- status
- selectable

Giá và trạng thái lấy từ ShowSeat của suất.
Hình học lấy từ layout đã publish.
Không lấy giá từ cấu hình mặc định ở FE.

Không đưa heldByBookingId của khách hàng khác vào projection công khai.
Trạng thái giữ ghế của chính khách hàng được đối chiếu qua booking
thuộc tài khoản đó.

### 23.2 Quy tắc ghép dữ liệu

- Ghép bằng seatId.
- Không ghép theo index hoặc phân tích chuỗi seatNumber.
- BE xác minh ghế, layout và suất cùng roomId.
- Giữ vị trí của AVAILABLE, HELD, BOOKED và UNAVAILABLE.
- Vị trí không có ShowSeat không được coi là AVAILABLE.
- Thiếu giá không được đổi thành giá 0.
- Thiếu layout hoặc dữ liệu không nhất quán phải trả lỗi rõ ràng.
- Không trả sơ đồ tự sinh từ tên ghế để che giấu dữ liệu thiếu.
- selectable phải xét trạng thái ghế và điều kiện mở bán của suất.

API đọc không giữ ghế và không bảo đảm khách hàng sẽ giữ ghế thành công.
Booking Service và Inventory reservation consumer vẫn kiểm tra
điều kiện, khóa ghế và xử lý tranh chấp tại thời điểm đặt.

### 23.3 Hành vi section FE

- Hiển thị phim, rạp, phòng, ngày và giờ từ dữ liệu của suất.
- Chú giải phân biệt loại ghế, trạng thái và ghế đang chọn.
- Selected là trạng thái client; không đồng nghĩa HELD.
- HELD, BOOKED và UNAVAILABLE không cho chọn.
- Không tự đổi HELD thành AVAILABLE khi đồng hồ FE hết hạn.
- Tiền vé tạm tính bằng tổng price của các đơn vị ghế đã chọn.
- Số người được tính riêng bằng tổng capacity.
- Chưa tạo booking thì không hiển thị countdown giữ ghế.
- Countdown sau khi tạo booking sử dụng expiresAt từ BE.

Desktop:

- Sơ đồ ghế và phần tóm tắt nằm cạnh nhau nếu đủ chiều rộng.

Mobile/tablet:

- Giữ nguyên hình học của phòng.
- Sơ đồ cuộn ngang trong vùng riêng khi cần.
- Không dùng auto-fit để chuyển ghế sang hàng khác.
- Không làm toàn trang tràn ngang.
- Phần tổng tiền và thao tác tiếp tục không đè lên bottom navigation.
- Xoay thiết bị không thay đổi ghế đã chọn hoặc tọa độ ghế.

Giao diện được suy luận từ cinematic-movie-detail và
cinematic-cinema-detail vì chưa có reference riêng cho booking.

Trong thời gian chưa có layout:

- Có thể dùng danh sách ghế theo rowLabel làm fallback được ghi nhãn rõ.
- Không gọi danh sách đó là sơ đồ vị trí thực tế.
- Không thêm màn hình/lối đi giả.
- Ghế không ghép được dữ liệu phải không cho chọn.

## 24. BE-BOOKING-03 — Bắp nước, combo và dịch vụ theo rạp

Trạng thái: PROPOSED.
Owner MVP đề xuất: module Concession trong Inventory Service.

Đây là đề xuất tổ chức nghiệp vụ, chưa có module này ở baseline.
Chưa cần tách một microservice mới cho MVP.

### 24.1 Mô hình đề xuất

ConcessionProduct:

- Tên và mô tả sản phẩm.
- Artwork nullable.
- Trạng thái hoạt động.

ConcessionVariant:

- SKU/biến thể, kích cỡ hoặc hương vị.
- Quan hệ với sản phẩm.

CinemaConcessionOffer:

- Offer bán tại một cinema.
- Sản phẩm/biến thể hoặc định nghĩa combo.
- Giá, currency, phiên bản giá.
- Thời gian bán, trạng thái bán.
- Giới hạn số lượng và khả năng cung ứng.

ComboSlot:

- Nhóm lựa chọn trong combo.
- Các variant hợp lệ.
- Số lượng bắt buộc và giới hạn lựa chọn.

Không giả định mọi rạp có cùng menu, giá hoặc tồn kho.
Combo phải xác định được các thành phần sau khi khách chọn biến thể.
Không coi combo là ghế hoặc promotion.

### 24.2 API đề xuất mới

GET /api/v1/concessions?cinemaId={cinemaId}

Catalog cho khách hàng:

- Chỉ trả offer được phép bán tại rạp.
- Có giá, currency, giới hạn số lượng và trạng thái mua được.
- Trả cấu hình lựa chọn cho combo.
- Artwork thiếu thì FE dùng placeholder cô lập.
- Không thêm sản phẩm hoặc giá tĩnh ở FE.

POST /api/v1/concessions/quotes

Yêu cầu xác thực.
Request đề xuất gồm:

- showtimeId
- seatNumbers
- Các offerId, quantity và lựa chọn variant của combo.

Response đề xuất:

- quoteId
- expiresAt
- Các dòng vé và dịch vụ đã định giá
- seatSubtotal
- concessionSubtotal
- totalAmount
- currency

Tên endpoint và schema trên là PROPOSED, chưa phải contract hiện có.

Quote là báo giá, không giữ ghế hoặc giữ hàng.
BE xác minh rạp bán dịch vụ phải là rạp của showtime.

Không nhận giá hoặc totalAmount do FE tự gửi làm giá trị có thẩm quyền.
Thay đổi giá so với quote phải yêu cầu khách xác nhận lại,
không tự thanh toán một tổng tiền khác.

Quản trị catalog/giá/tồn kho cần phân quyền được khai báo rõ.
Không tự cấp inventory:write cho khách hàng.

## 25. BE-BOOKING-04 — Tích hợp dịch vụ vào booking và payment

Trạng thái: PROPOSED.
Owner: Booking Service, phối hợp Inventory và Payment.

### 25.1 Điểm phải thay đổi trong luồng hiện tại

SeatReservedConsumerServiceImpl hiện:

1. Hoàn thiện snapshot ghế.
2. Kiểm tra tổng giá ghế trong sự kiện.
3. Chuyển booking sang RESERVED.
4. Phát PaymentRequested ngay sau đó.

Vì vậy, không thêm combo sau RESERVED rồi chỉ cộng tổng tiền ở FE.

### 25.2 Luồng MVP đề xuất

FE:

1. Chọn ghế.
2. Chọn dịch vụ tùy chọn.
3. Xem báo giá và tổng tiền.
4. Gửi yêu cầu tạo booking sau khi khách xác nhận.

Đề xuất mở rộng CreateBookingRequest với:

- quoteId
- concessions: offerId, quantity và lựa chọn variant.

Các trường mới phải được thiết kế tương thích caller đặt vé thuần.
Giỏ concessions rỗng vẫn hỗ trợ đặt vé không mua dịch vụ.

Không gửi userId, giá hoặc tổng tiền do FE tự tính.

BE:

1. Kiểm tra người dùng, quote, payload và khả năng mở bán.
2. Tạo booking PENDING với giỏ dịch vụ đã xác nhận.
3. Giữ ghế và giữ các thành phần hàng hóa cần thiết.
4. Chỉ hoàn tất RESERVED khi tất cả phần bắt buộc thành công.
5. Lưu snapshot các dòng vé và dịch vụ.
6. Tính totalAmount có thẩm quyền.
7. Phát PaymentRequested đúng một lần.

Điều kiện hoàn tất phải được kiểm tra chung và có khóa/idempotency.
Không để consumer ghế phát payment trước khi biết kết quả giữ hàng.

### 25.3 Snapshot và tiền

BookingConcession lưu:

- Offer và phiên bản đã mua.
- Tên sản phẩm/combo tại thời điểm mua.
- Lựa chọn biến thể và thành phần.
- Quantity.
- Unit price và line total.
- Currency.

BookingResponse đề xuất bổ sung:

- concessionLines
- seatSubtotal
- concessionSubtotal

totalAmount là tổng cuối cùng do BE tính.
Không đọc lại giá catalog hiện tại để thay đổi booking đã chốt.

Vẫn kiểm tra SeatReserved.totalAmount bằng tổng snapshot ghế.
Không so tổng riêng của sự kiện ghế với tổng tiền gồm combo.

Nếu giá ghế tại lúc reservation không còn khớp quote,
phải xử lý từ chối và bù trừ trước khi phát payment.

PaymentRequested dùng tổng tiền cuối cùng đã lưu trong Booking.
Không mở rộng payment bằng cách nhận tổng tiền tùy ý từ FE.

### 25.4 Idempotency và bù trừ

Fingerprint của clientRequestId phải bao gồm:

- showtimeId
- Seat numbers đã chuẩn hóa.
- Quote và giỏ dịch vụ đã chuẩn hóa.
- Offer, quantity và lựa chọn variant.

Retry cùng request dùng lại clientRequestId.
Thay đổi lựa chọn tạo request mới.
Cùng request ID nhưng payload khác phải bị từ chối.

Nếu giữ ghế hoặc giữ hàng thất bại:

- Không thanh toán một phần.
- Giải phóng tài nguyên đã giữ.
- Booking trả lý do thất bại có thể hiển thị.

Cancellation, expiry và payment failure phải bù trừ cả ghế lẫn hàng.
Sự kiện replay không trừ hàng, giải phóng hàng hoặc tạo payment hai lần.

Không đọc trực tiếp database của service khác.
Không mở lại R28 Notification.

## 26. Tiêu chí nghiệm thu thủ công cho phần bổ sung

Layout:

- Đối chiếu đúng phòng, hướng màn hình, hàng ghế và lối đi.
- Kiểm tra hàng bắt đầu lệch cột, có khoảng trống và có ghế đôi.
- Ghế đôi là một đơn vị chọn; giá không bị nhân thêm.
- Layout mới không làm dịch chuyển ghế của suất đã có.
- Mobile/tablet xoay ngang giữ đúng hình học.

Seat map:

- Ghế bận vẫn giữ vị trí.
- Không hiển thị ghế thiếu ShowSeat/giá thành ghế mua được.
- Hai khách chọn cùng ghế không được cùng giữ thành công.
- Chọn trên FE chưa hiển thị thành ghế đã được giữ.

Concession và checkout:

- Menu/giá đúng rạp của suất.
- Không mua offer của rạp khác.
- Giỏ rỗng đặt được vé thuần.
- Hết hàng hoặc giá đổi không tự thanh toán tổng tiền mới.
- Retry không tạo booking/payment hoặc trừ hàng hai lần.
- Lỗi một nhánh reservation giải phóng nhánh đã thành công.
- Tổng payment khớp snapshot vé và dịch vụ.
