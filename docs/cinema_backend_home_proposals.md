# Đề xuất thay đổi Backend hỗ trợ F3.1 — QuickBooking, NowShowing và Membership

Ngày: 01/10/2026  
Trạng thái: PROPOSED — chưa triển khai  
Backend đã đọc: HuyKunNe/cinema-system @ efdac4a16cb9ec286e579716883d05165ebb87b9  
Frontend đã đọc: HuyKunNe/cinematic-web @ e1ae38c63ad9497ab1c6bc9b308545d70746c371

Tài liệu này là ghi chú đề xuất để người dùng tự triển khai theo từng round. Không có thay đổi nào được ghi vào hai repository. Chưa xác nhận người dùng đã áp dụng hướng dẫn QuickBooking ở cuộc hội thoại.

Nguồn giao diện: docs/design/reference/html-convert/cinematic-home-desktop.html/.css và cinematic-home-mobile.html/.css. QuickBooking dùng thứ tự Rạp → Phim → Ngày → Suất. Carousel tự chạy là yêu cầu mở rộng của người dùng, không phải hành vi có sẵn trong HTML tĩnh.

## 1. Các giới hạn đã xác nhận từ source

| Khu vực | Hiện tại | Hệ quả với frontend |
| --- | --- | --- |
| Movie catalog | GET /api/v1/movies trả List<MovieResponse>; không có query filter trong controller | FE tải toàn bộ catalog rồi lọc NOW_SHOWING |
| Artwork | Movie có posterUrl, trailerUrl; không có backdropUrl | Poster bị crop khi dùng cho nền ngang; fallback hiện là ảnh generic theo hash ID |
| Rạp | GET /api/v1/cinemas có city tùy chọn; chỉ trả rạp active | Có danh sách rạp hợp lệ để bắt đầu QuickBooking |
| Lịch theo phim | GET /api/v1/showtimes/by-movie/{movieId} trả toàn bộ lịch theo phim, sắp startsAt tăng dần | FE phải lọc rạp, trạng thái và thời gian; chưa chọn được phim theo rạp ngay sau bước 1 |
| Lịch theo khoảng | GET /api/v1/showtimes?from=…&to=… dùng query thời gian; chưa có cinemaId/movieId filter | Có thể lọc ở FE nhưng tải dư dữ liệu |
| Response lịch | ShowtimeResponse có movieId, roomId, roomName, cinemaId, cinemaName, startsAt, endsAt, status | Có đủ thông tin cho chọn ngày/giờ; không có số ghế còn trống |
| Security | GET catalog/lịch công khai tại Gateway và Movie/Inventory Service; ghi Movie cần movie:manage, ghi lịch cần showtime:manage | Không cần thêm luồng login cho đọc dữ liệu Home |
| Reservation consumer | Kiểm tra showtime tồn tại, OPEN_FOR_BOOKING, yêu cầu/hold chưa hết hạn và trạng thái ghế | Tại consumer đã đọc, chưa kiểm tra startsAt > now hoặc room/cinema active |

Điều kiện suất chưa bắt đầu là chính sách FE đang có; chưa thể coi là quy tắc BE đã được áp dụng trong reservation consumer. “Mở bán” không đồng nghĩa “còn ghế”.

## 2. Danh sách ưu tiên

| ID | Ưu tiên | Đề xuất | Lợi ích |
| --- | --- | --- | --- |
| BE-HOME-01 | P1 | Read API cho lịch mở bán theo rạp, phim, khoảng thời gian | Phim/ngày/suất ở QuickBooking phản ánh lịch của rạp đã chọn |
| BE-HOME-02 | P1, cần chốt nghiệp vụ | Kiểm tra điều kiện nhận reservation mới tại Inventory | Tránh khác biệt giữa danh sách gợi ý và điều kiện giữ ghế |
| BE-HOME-03 | P1 | backdropUrl tùy chọn trong Movie | Carousel có ảnh nền ngang đúng phim |
| BE-HOME-04 | P2 | Quy tắc sắp thứ tự catalog ổn định | Carousel không đổi thứ tự do repository.findAll() không có sort rõ ràng |
| BE-HOME-05 | P2, tùy chọn | Metadata availability thuộc Inventory | Có thể hiển thị số ghế hiện khả dụng sau khi chốt semantics |

Khuyến nghị làm 01, chốt 02, rồi 03. Không cần làm 04/05 để hoàn thành carousel cơ bản.

## 3. BE-HOME-01 — Bookable showtimes API

### Contract đề xuất — chưa tồn tại

GET /api/v1/showtimes/bookable

| Query | Đề xuất |
| --- | --- |
| cinemaId | UUID, bắt buộc |
| movieId | UUID, tùy chọn |
| from | OffsetDateTime có offset, bắt buộc |
| to | OffsetDateTime có offset, bắt buộc |

Response đề xuất: HTTP 200 với List<ShowtimeResponse>, giữ contract list trực tiếp như controller hiện tại. Khi không có suất phù hợp: [].

Không tự bọc ApiResponse cho riêng endpoint này. Không đổi semantics các endpoint cũ. cinemaId sai định dạng hoặc khoảng thời gian sai trả lỗi validation theo cơ chế chung hiện tại; cinemaId không tồn tại dùng lỗi CINEMA_NOT_FOUND đã có.

### Điều kiện lọc được đề xuất

- Thuộc cinemaId.
- Nếu có movieId, khớp movieId.
- status = OPEN_FOR_BOOKING.
- Room và Cinema đang active.
- startsAt > serverNow.
- startsAt nằm trong [from, to); đầu bao gồm, cuối loại trừ.
- Sort startsAt ASC, id ASC để ổn định khi nhiều phòng có cùng giờ.

Các điều kiện này là contract mới được đề xuất. Endpoint getByTimeRange hiện dùng StartsAtBetween; không sửa endpoint cũ sang khoảng nửa mở một cách âm thầm.

Giới hạn khoảng tra cứu cần cấu hình để tránh yêu cầu quá rộng. Gợi ý ban đầu: tối đa 30 ngày; đây là lựa chọn cần chốt, không phải giới hạn backend hiện có. FE cũng cần có khoảng tra cứu được thông báo rõ. Không hiển thị “không có lịch” nếu thực tế chỉ đang xét một khoảng mà UI không nói cho người dùng.

Không silently truncate response. Nếu volume thực tế cần pagination, chốt contract pagination trước và bảo đảm FE lấy đủ các trang trước khi suy ra danh sách phim/ngày.

### Cách FE tận dụng

1. Load movie catalog và các rạp active.
2. Sau khi chọn rạp, gọi endpoint mới với cinemaId và khoảng tra cứu.
3. Lấy tập movieId duy nhất từ lịch trả về.
4. Ghép với MovieResponse ở FE để lấy title, posterUrl, backdropUrl.
5. Từ tập lịch đã lấy, lọc movieId để tạo ngày và suất; không cần request riêng cho từng phim.
6. Khi tiếp tục, kiểm tra lại lựa chọn bằng dữ liệu mới.

Inventory chỉ sở hữu lịch và movieId tham chiếu. Không join database Movie, không trả title/poster bằng cách truy cập bảng Movie. Chưa cần tạo Home Service hoặc BFF.

Phim UPCOMING có OPEN_FOR_BOOKING cần được chốt có cho phép đặt trước hay không. Nếu có, QuickBooking lấy ứng viên từ lịch mở bán và catalog, không giới hạn vào mảng nowShowing; NowShowing vẫn chỉ hiển thị NOW_SHOWING. Movie INACTIVE/ENDED được loại ở FE theo chính sách catalog; endpoint Inventory riêng không xác nhận được status trong database Movie.

### File liên quan đã tồn tại

- services/inventory-service/src/main/java/com/cinema/inventory/controller/ShowtimeController.java
- services/inventory-service/src/main/java/com/cinema/inventory/service/ShowtimeService.java
- services/inventory-service/src/main/java/com/cinema/inventory/service/impl/ShowtimeServiceImpl.java
- services/inventory-service/src/main/java/com/cinema/inventory/repository/ShowtimeRepository.java
- services/inventory-service/src/main/java/com/cinema/inventory/dto/response/ShowtimeResponse.java
- services/inventory-service/src/main/java/com/cinema/inventory/config/InventorySecurityConfig.java
- infrastructure/gateway-service/src/main/java/com/cinema/gateway/config/GatewaySecurityConfiguration.java

DTO query mới chỉ cần tạo nếu phù hợp convention và validation thực tế của service. Endpoint mới nằm dưới các GET patterns công khai hiện tại; rà soát để xác nhận, không mặc định cần sửa security hoặc thêm permission.

Schema hiện đã có idx_rooms_cinema_active, idx_showtimes_movie_start và idx_showtimes_room_status_start. Kiểm tra EXPLAIN với dữ liệu thực tế trước khi thêm index. Nếu cần migration, dùng version tiếp theo tại thời điểm triển khai; không sửa V1/V2 đã áp dụng.

### Kiểm tra thủ công

- Hai rạp có lịch khác nhau: chỉ trả lịch của rạp được chọn.
- movieId tùy chọn lọc đúng phim.
- Không trả SCHEDULED/CLOSED/CANCELLED/COMPLETED.
- Không trả suất đã bắt đầu, phòng inactive hoặc rạp inactive.
- Kiểm tra suất đúng from, đúng to và nhiều phòng cùng giờ.
- Đọc không đăng nhập được; các API ghi vẫn giữ quyền hiện tại.
- Các endpoint lịch cũ giữ response và behavior.
- QuickBooking không có ứng viên trong khoảng hiển thị empty state đúng.

Commit gợi ý:
feat(inventory): expose bookable showtimes filtered by cinema and movie

## 4. BE-HOME-02 — Chốt và áp dụng quy tắc nhận reservation mới

### Quy tắc cần chốt

Nếu nghiệp vụ yêu cầu chỉ nhận đặt mới trước giờ chiếu và tại rạp/phòng hoạt động, Inventory phải kiểm tra các điều kiện đó tại bước giữ ghế, không dựa vào FE hoặc read API.

Đề xuất cho reservation mới:
- Showtime tồn tại và OPEN_FOR_BOOKING.
- startsAt > now theo Clock của server.
- Room và Cinema active.
- Giữ nguyên validation thời hạn request/hold, trạng thái ghế và idempotency.

Không tự hủy booking/hold đã tồn tại chỉ vì rạp bị deactivate hoặc thời gian trôi qua. Cần xác định rõ việc xử lý duplicate event của booking đã giữ ghế; không đặt guard mới ở vị trí khiến replay hợp lệ phát sinh kết quả terminal trái với lần xử lý trước.

Consumer hiện dùng Clock cho now; tái sử dụng cùng thời điểm đó để tránh nhiều cách lấy thời gian trong một transaction. Rà soát mọi entry point có thể tạo hold trước khi tuyên bố quy tắc nhất quán.

Đây không phải bảo đảm tuyệt đối chống mọi race: admin có thể đóng suất đồng thời với reservation. Nếu yêu cầu ngăn race này, phải rà soát và thiết kế transaction/lock của cả hai phía; chỉ thêm if không đủ để khẳng định tính nguyên tử.

### File trọng tâm

- services/inventory-service/src/main/java/com/cinema/inventory/service/impl/SeatReservationRequestedConsumerServiceImpl.java
- services/inventory-service/src/main/java/com/cinema/inventory/entity/Showtime.java
- services/inventory-service/src/main/java/com/cinema/inventory/service/impl/ShowSeatServiceImpl.java — rà soát entry point liên quan
- services/inventory-service/src/main/java/com/cinema/inventory/event/SeatReservationRejectionReason.java — kiểm tra trước khi chọn reason code

Tái sử dụng rejection reason phù hợp nếu ý nghĩa khớp; không tự thêm enum/event field rồi bỏ qua compatibility của Booking consumer. Không đổi format sự kiện trong round read API.

### Kiểm tra thủ công

- Yêu cầu giữ ghế mới với suất đã bắt đầu hoặc CLOSED bị từ chối đúng flow.
- Trường hợp active/inactive có behavior theo quyết định đã chốt.
- Event duplicate của một reservation đã xử lý không tạo kết quả trái ngược.
- Booking và release flow hiện có vẫn giữ nguyên semantics.

Commit gợi ý:
fix(inventory): enforce showtime eligibility for new seat reservations

## 5. BE-HOME-03 — Movie backdropUrl

### Contract đề xuất — chưa tồn tại

Thêm String backdropUrl nullable vào:
- Movie entity.
- CreateMovieRequest.
- UpdateMovieRequest.
- MovieResponse.

Schema đề xuất: movies.backdrop_url VARCHAR(500) NULL, tương tự poster_url. Dùng migration mới; hiện Movie Service có V1/V2, nên V3__add_movie_backdrop_url.sql là tên ứng viên nếu V3 vẫn chưa được dùng khi triển khai.

backdropUrl dùng cho ảnh nền ngang của hero/carousel; posterUrl giữ nguyên ý nghĩa. Không gán một URL ngẫu nhiên để giả làm artwork của phim.

Validation đề xuất:
- Tối đa 500 ký tự như media URL hiện tại.
- Blank chuẩn hóa về null.
- Chốt dạng URL được phép theo storage/CDN thật: URL tuyệt đối http/https hoặc relative path được duyệt.
- Không thêm upload endpoint trong round này.
- Không yêu cầu backend tải URL để “kiểm tra ảnh” khi lưu metadata.

Create/Update hiện chỉ dùng Size cho posterUrl/trailerUrl; không mô tả việc kiểm tra protocol/domain là tính năng có sẵn. Chốt omission/null behavior của Update (PUT): nếu mapper ghi null, client cũ không gửi field mới có thể xóa backdrop đã có. Cần ghi rõ contract và cập nhật caller trước khi deploy; không tự biến PUT thành PATCH.

### File liên quan

- services/movie-service/src/main/java/com/cinema/movie/entity/Movie.java
- services/movie-service/src/main/java/com/cinema/movie/dto/request/CreateMovieRequest.java
- services/movie-service/src/main/java/com/cinema/movie/dto/request/UpdateMovieRequest.java
- services/movie-service/src/main/java/com/cinema/movie/dto/response/MovieResponse.java
- services/movie-service/src/main/java/com/cinema/movie/mapper/MovieMapper.java
- services/movie-service/src/main/java/com/cinema/movie/service/impl/MovieServiceImpl.java
- services/movie-service/src/main/resources/db/migration/ — thêm migration mới

MapStruct có thể map cùng tên sau khi entity/DTO được mở rộng; phải kiểm tra output create/update/response, không mặc định chỉ sửa DTO là đủ. Giữ quyền movie:manage cho mutation và public GET cho đọc.

### FE sau khi BE hoàn thành

- Export OpenAPI đúng môi trường; cập nhật metadata backend commit của snapshot.
- Regenerate Orval; không sửa generated model bằng tay.
- Thêm backdropUrl vào HomeMovie và home-movie.mapper.ts.
- Cô lập fallback trong src/features/home/presentation/artwork.ts:
  backdropUrl → posterUrl → placeholder của dự án → gradient.
- Nếu ảnh ưu tiên tải lỗi, thử nguồn tiếp theo, không chỉ fallback khi URL null.
- Chỉ commit slide mới khi nội dung, ảnh và indicator có thể cập nhật đồng bộ.

Auto-slide, nút trước/sau, dots, pause khi tương tác, bàn phím, reduced motion và ổn định chiều cao tiêu đề vẫn là trách nhiệm FE; không cần BE timer hay carousel endpoint.

### Kiểm tra thủ công

- Movie cũ không có backdrop vẫn đọc được.
- Tạo/sửa/đọc phim có backdrop trả đúng URL.
- Blank và null theo semantics đã chốt.
- URL quá dài bị validation; URL lỗi tải ở browser kích hoạt fallback.
- Phim có poster dọc + backdrop ngang hiển thị đúng vai trò.
- Không làm mất ảnh của phim khác khi chuyển slide.

Commit gợi ý:
feat(movie): add optional backdrop artwork to movie contracts

## 6. Các đề xuất P2

### BE-HOME-04 — Thứ tự catalog

MovieServiceImpl.findAll() hiện gọi movieRepository.findAll() không có sort rõ ràng. Nếu cần ổn định carousel, đề xuất sort catalog theo releaseDate DESC với null cuối và id ASC, hoặc sort ở FE nếu chỉ phục vụ Home.

Đó là thứ tự mặc định, không phải “phim nổi bật”. Chỉ thêm featured/sortOrder nếu có yêu cầu quản trị biên tập cụ thể. Không tạo tagline, age rating hay rating giả để bám mẫu HTML.

### BE-HOME-05 — Availability summary

Chỉ bổ sung nếu UX cần. Inventory sở hữu summary từ show_seats. Chốt “available” dựa trên trạng thái AVAILABLE hay có tính HELD đã hết hạn nhưng chưa release. Không trả held_by_booking_id hoặc dữ liệu người dùng ra public response.

Summary có thể cũ ngay sau khi đọc, không bảo đảm giữ được ghế. Không đặt mỗi suất một request đếm ghế gây N+1; cần aggregate theo batch và đo chi phí. Chưa đề xuất thay response ShowtimeResponse hiện tại cho đến khi semantics được chốt.

## 7. Trình tự review và cập nhật tài liệu

- Round BE1: chỉ BE-HOME-01; review contract và kiểm tra thủ công trước.
- Round BE2: BE-HOME-02 sau khi chốt quy tắc, duplicate handling và yêu cầu concurrency.
- Round BE3: BE-HOME-03; sau đó regenerate FE clients.
- Quay lại round FE QuickBooking để dùng lịch theo rạp.
- Sau kết quả QuickBooking, thực hiện NowShowing carousel, rồi Membership carousel ở một round riêng theo bổ sung của người dùng.
- P2 giữ trong backlog; R28 và authentication không nằm trong phạm vi đề xuất.

Khi người dùng đã triển khai, cập nhật các tài liệu BE thực tế bị ảnh hưởng: docs/10_ROADMAP.md, docs/11_CHANGELOG.md, docs/08_SECURITY.md nếu có đổi security, và API/architecture docs theo convention repo. Không ghi DONE trước khi review.

Commit nếu người dùng tự đưa riêng ghi chú này vào repo:
docs: record backend proposals for quick booking and movie artwork

## 8. Bổ sung FE — NowShowingSection và MembershipSection chưa có carousel

Đã đọc lại hai component tại commit e1ae38c và xác nhận:

- NowShowingSection.vue render MovieCard bằng v-for trong home-movie-scroller. Desktop là grid; mobile có horizontal scroll/snap qua CSS. Chưa có active slide, timer, prev/next, dots hoặc pause.
- MembershipSection.vue render một thẻ MEMBER và membershipBenefits dạng grid. Đây là nội dung minh họa từ presentation/marketing.ts; chưa có carousel. CSS mobile hiện ẩn quyền lợi thứ tư.
- HeroBanner.vue là component riêng, có chuyển banner thủ công; không coi hành vi của HeroBanner là carousel đã có cho NowShowingSection.

### NowShowing carousel — phần FE cần làm

- Dùng dữ liệu phim backend đã map qua HomeMovie; không tạo dữ liệu phim mẫu.
- Bổ sung auto-slide, prev/next, dots, bàn phím, pause khi hover/focus và dừng sau thao tác thủ công; cho phép bật lại bằng nút rõ ràng.
- Không tự chạy khi prefers-reduced-motion; tạm dừng khi tab bị ẩn; cleanup timer khi unmount.
- Chuyển phim, nội dung và artwork đồng bộ; giữ chiều cao vùng tiêu đề/nội dung ổn định.
- Nếu chưa có BE-HOME-03, dùng fallback artwork được cô lập và ghi rõ giới hạn crop poster.
- Tham chiếu card layout của khu vực NowShowing và controls của Hero trong HTML/CSS; hành vi và bố cục carousel mở rộng là suy luận cần review, không tuyên bố pixel match với grid tĩnh.

### Membership carousel — đề xuất FE trước

- Gợi ý mỗi slide là một quyền lợi đã có trong membershipBenefits; không tự tạo các hạng thành viên, điểm hoặc quyền lợi mới.
- Giữ nhãn “Nội dung minh họa · quyền lợi đang chờ xác nhận”. Chuyển slide không biến các quyền lợi này thành dữ liệu đã được backend hỗ trợ.
- Giữ thẻ MEMBER và phần giới thiệu ổn định; carousel nằm tại vùng quyền lợi. Đây là lựa chọn thiết kế đề xuất, chưa phải implementation.
- Hiển thị được cả bốn quyền lợi trên mobile, thay cho rule ẩn item thứ tư; thêm controls/indicator và hỗ trợ bàn phím/reduced motion tương tự NowShowing.
- Chưa cần thay đổi BE để tạo carousel trình bày nội dung minh họa. Nếu muốn quyền lợi live, cần chốt domain/API membership riêng; không bịa endpoint hoặc gộp việc này vào R28.
- Có thể chia sẻ composable điều khiển carousel nếu semantics tương thích; mỗi carousel giữ state/timer riêng. Không buộc Membership phụ thuộc nội bộ feature khác.

File FE cần review ở các round sau: src/features/home/components/NowShowingSection.vue, MembershipSection.vue, MovieCard.vue, presentation/marketing.ts, presentation/artwork.ts, src/styles/home.css và src/styles/tokens.css. Chỉ đề xuất composable mới khi bắt đầu round triển khai.

Commit gợi ý cho từng round sau, chỉ dùng sau khi đã áp dụng:
- feat(home): add accessible auto carousel to now showing
- feat(home): add membership benefits carousel

## 9. Nguồn đã đọc

Các link dưới đây pin vào commit để truy vết bằng chứng:

- [MovieController](https://github.com/HuyKunNe/cinema-system/blob/efdac4a16cb9ec286e579716883d05165ebb87b9/services/movie-service/src/main/java/com/cinema/movie/controller/MovieController.java)
- [MovieResponse](https://github.com/HuyKunNe/cinema-system/blob/efdac4a16cb9ec286e579716883d05165ebb87b9/services/movie-service/src/main/java/com/cinema/movie/dto/response/MovieResponse.java)
- [MovieServiceImpl](https://github.com/HuyKunNe/cinema-system/blob/efdac4a16cb9ec286e579716883d05165ebb87b9/services/movie-service/src/main/java/com/cinema/movie/service/impl/MovieServiceImpl.java)
- [ShowtimeController](https://github.com/HuyKunNe/cinema-system/blob/efdac4a16cb9ec286e579716883d05165ebb87b9/services/inventory-service/src/main/java/com/cinema/inventory/controller/ShowtimeController.java)
- [ShowtimeServiceImpl](https://github.com/HuyKunNe/cinema-system/blob/efdac4a16cb9ec286e579716883d05165ebb87b9/services/inventory-service/src/main/java/com/cinema/inventory/service/impl/ShowtimeServiceImpl.java)
- [ShowtimeRepository](https://github.com/HuyKunNe/cinema-system/blob/efdac4a16cb9ec286e579716883d05165ebb87b9/services/inventory-service/src/main/java/com/cinema/inventory/repository/ShowtimeRepository.java)
- [Reservation consumer](https://github.com/HuyKunNe/cinema-system/blob/efdac4a16cb9ec286e579716883d05165ebb87b9/services/inventory-service/src/main/java/com/cinema/inventory/service/impl/SeatReservationRequestedConsumerServiceImpl.java)
- [Inventory security](https://github.com/HuyKunNe/cinema-system/blob/efdac4a16cb9ec286e579716883d05165ebb87b9/services/inventory-service/src/main/java/com/cinema/inventory/config/InventorySecurityConfig.java)
- [Gateway security](https://github.com/HuyKunNe/cinema-system/blob/efdac4a16cb9ec286e579716883d05165ebb87b9/infrastructure/gateway-service/src/main/java/com/cinema/gateway/config/GatewaySecurityConfiguration.java)
- [Frontend Home code](https://github.com/HuyKunNe/cinematic-web/tree/e1ae38c63ad9497ab1c6bc9b308545d70746c371/src/features/home)
- [HTML/CSS reference](https://github.com/HuyKunNe/cinematic-web/tree/e1ae38c63ad9497ab1c6bc9b308545d70746c371/docs/design/reference/html-convert)

Đã rà soát source, DTO, enum, security, mapper và migrations liên quan. Không gọi API runtime, không chạy test/build và không áp dụng migration. Các endpoint/field trong phần đề xuất chưa tồn tại trong source đã đọc.
