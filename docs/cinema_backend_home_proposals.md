# Đề xuất cập nhật Backend cho Customer Home, Movie Catalog và QuickBooking

> **Trạng thái:** PROPOSED — chưa triển khai
> **Phạm vi:** API và dữ liệu Backend cần xem xét để hỗ trợ Customer Home, Movie Catalog và QuickBooking của `cinematic-web`.
> **Quyền repo:** Backend và frontend chỉ đọc trong quá trình lập đề xuất này. Không có file, API, migration hay cấu hình nào được thay đổi.
> **Nguồn giao diện:** Các bản HTML/CSS tại `cinematic-web/docs/design/reference/html-convert`; dự án hiện không dùng Figma làm nguồn thiết kế.
> **Ranh giới tính năng:** Không mở lại R28 Notification đang được hoãn.

## 1. Mục tiêu

Tài liệu ghi lại:

- Những API và dữ liệu hiện có có thể dùng cho giao diện khách hàng.
- Những giới hạn hiện tại có ảnh hưởng đến Home, Movies và QuickBooking.
- Các thay đổi Backend nên cân nhắc, kèm thứ tự ưu tiên và hợp đồng API ở mức đề xuất.
- Các quyết định nghiệp vụ cần thống nhất trước khi triển khai.

Đây không phải danh sách endpoint đã tồn tại. Các route, query parameter, field, quy tắc lọc và commit message mang nhãn **đề xuất** cần được xác nhận trước khi implement.

## 2. Baseline đã đối chiếu

Các nhận định dưới đây dựa trên source code tại:

| Repo                     | Commit được đối chiếu                      | Ghi chú                                                                                            |
| ------------------------ | ------------------------------------------ | -------------------------------------------------------------------------------------------------- |
| `HuyKunNe/cinema-system` | `8b285bd32f842dbcabe57638ba5330c8ea20fcb6` | Có tài liệu đề xuất Customer Home; chưa có nghĩa là các đề xuất trong tài liệu đã được triển khai. |
| `HuyKunNe/cinematic-web` | `02f627c4de1b3d30dc2425d7d2ce5cc5e1a6b52f` | FE đang dùng API hiện có và xử lý một số lọc ở phía client.                                        |

Nếu nhánh `main` thay đổi sau các commit trên, hãy kiểm tra lại controller, DTO, generated client và query composable trước khi dùng đề xuất này.

### 2.1 Movie Service

- `GET /api/v1/movies` hiện trả danh sách `List<MovieResponse>`; chưa có phân trang hoặc query filter tại controller.
- `MovieResponse` hiện có thông tin phim như tên, mô tả, thời lượng, ngày phát hành, poster, trailer, trạng thái và thể loại.
- Trạng thái phim hiện có `UPCOMING`, `NOW_SHOWING`, `ENDED` và `INACTIVE`.
- `GET /api/v1/genres` đã tồn tại.
- FE hiện lọc trạng thái, thể loại và sắp xếp ở phía client. Movie catalog có cơ chế tải thêm ở FE.
- Movie Service không sở hữu dữ liệu rạp, phòng chiếu hoặc suất chiếu. Không thêm lọc rạp vào `MovieController`.

### 2.2 Cinema và Showtime

- Cinema API hiện hỗ trợ lấy danh sách rạp đang hoạt động và lọc theo thành phố.
- Showtime API hiện có các truy vấn theo khoảng thời gian, theo phòng và theo phim.
- `ShowtimeResponse` cung cấp các định danh phim, phòng và rạp cùng thời gian bắt đầu/kết thúc và trạng thái.
- Response hiện không cung cấp số ghế trống. `OPEN_FOR_BOOKING` có nghĩa là suất chiếu đang mở đặt vé, không đảm bảo còn ghế.
- FE QuickBooking hiện lấy suất theo phim rồi lọc theo rạp đã chọn, trạng thái và thời gian ở phía client.
- FE lấy chương trình theo rạp thông qua danh sách phòng và nhiều truy vấn suất chiếu theo phòng. Cách này có thể phát sinh nhiều request khi một rạp có nhiều phòng.

### 2.3 Customer Home

- Hero hiện có cơ chế tự chuyển slide ở FE. Không cần thêm API timer hoặc animation cho chức năng tự chuyển slide.
- NowShowing hiện là carousel thao tác cuộn thủ công. Yêu cầu tự động chạy carousel NowShowing là phần cần triển khai ở FE; Backend chỉ cần cung cấp dữ liệu phim phù hợp.
- Promotion hiện dùng dữ liệu preview phía FE và carousel cuộn thủ công. Chưa có promotion API hoặc hợp đồng khuyến mãi thật.
- Membership benefits hiện là nội dung minh họa ở FE, chưa được lấy từ Membership API.

### 2.4 Reservation và hold ghế

- Luồng xử lý reservation hiện kiểm tra suất chiếu tồn tại và trạng thái `OPEN_FOR_BOOKING`.
- Hold ghế hiện kiểm tra thời hạn hold và trạng thái ghế.
- Các kiểm tra hiện tại chưa chứng minh nhất quán rằng suất chiếu vẫn ở tương lai hoặc rạp/phòng vẫn hoạt động tại thời điểm tạo hold.
- Việc thay đổi quy tắc này cần được áp dụng nhất quán cho mọi điểm có thể tạo hold và phải giữ nguyên cơ chế idempotency, release và compensation hiện có.

## 3. Phạm vi và ngoài phạm vi

### Trong phạm vi

- Tối ưu truy vấn suất chiếu cho QuickBooking.
- Xác định chính sách hợp lệ khi tạo reservation/hold.
- Bổ sung metadata phim khi có nguồn dữ liệu chính thức.
- Cân nhắc phân trang và lọc Movie catalog khi quy mô dữ liệu cần đến.
- Xác định timezone dùng chung cho ngày và giờ chiếu.
- Kiểm tra CORS ở runtime cho các origin FE được phép.

### Ngoài phạm vi

- Không triển khai thay đổi trong tài liệu này.
- Không thiết kế lại animation, carousel hoặc lưu trạng thái địa điểm ở Backend.
- Không tạo promotion, giá vé, membership benefit hoặc quyền lợi giả khi chưa có nghiệp vụ và nguồn dữ liệu.
- Không mở lại R28 Notification.
- Không thay đổi response hiện tại theo cách làm hỏng client đang dùng.
- Không thêm Movie Service truy vấn trực tiếp database của Inventory Service.

## 4. Danh sách đề xuất và ưu tiên

| ID         | Ưu tiên                       | Service             | Nội dung                                                                                      | Trạng thái                |
| ---------- | ----------------------------- | ------------------- | --------------------------------------------------------------------------------------------- | ------------------------- |
| BE-HOME-01 | Cao                           | Inventory           | Thêm truy vấn suất chiếu mở bán cho QuickBooking, lọc theo rạp và khoảng thời gian ở Backend. | Đề xuất                   |
| BE-HOME-02 | Cao, cần quyết định nghiệp vụ | Inventory           | Chuẩn hóa điều kiện tạo reservation/hold với suất chiếu, phòng và rạp.                        | Cần thống nhất chính sách |
| BE-HOME-03 | Trung bình                    | Movie               | Thêm `backdropUrl` nullable nếu có nhu cầu ảnh ngang chính thức cho Hero.                     | Tùy chọn                  |
| BE-HOME-04 | Khi catalog tăng              | Movie               | Thêm API catalog phân trang và lọc, giữ nguyên `GET /movies`.                                 | Chưa cấp thiết            |
| BE-HOME-05 | Tùy chọn                      | Inventory           | Xây dựng thông tin availability nếu UI thực sự cần số ghế hoặc trạng thái còn chỗ.            | Chưa có nguồn dữ liệu     |
| BE-HOME-06 | Trung bình                    | Movie               | Thêm phân loại độ tuổi khi có dữ liệu được xác nhận.                                          | Cần nguồn dữ liệu         |
| BE-HOME-07 | Thấp                          | Movie               | Chuẩn hóa và xác thực `trailerUrl` hiện có.                                                   | Nên rà soát               |
| BE-HOME-08 | Tùy chọn                      | Movie/CMS           | Hỗ trợ thứ tự hoặc nội dung biên tập cho Hero nếu cần quản trị từ hệ thống.                   | Chưa cần cho autoplay     |
| BE-HOME-09 | Ngoài đợt hiện tại            | Promotion           | Thiết kế API khuyến mãi khi có nghiệp vụ giá, điều kiện và thời hạn.                          | Chưa có contract          |
| BE-HOME-10 | Ngoài đợt hiện tại            | Membership          | Tích hợp quyền lợi thành viên khi có Membership API và chính sách thật.                       | Chưa có contract          |
| BE-HOME-11 | Cao                           | FE/Backend contract | Thống nhất timezone cho truy vấn và hiển thị lịch chiếu.                                      | Cần thống nhất            |
| BE-HOME-12 | Cao khi deploy                | Gateway/config      | Xác nhận preflight CORS từ đúng domain FE đang chạy.                                          | Cần kiểm tra runtime      |

## 5. BE-HOME-01 — Truy vấn suất chiếu mở bán cho QuickBooking

### Vấn đề hiện tại

QuickBooking cần danh sách suất chiếu phù hợp với rạp, phim và ngày đã chọn. FE hiện phải truy vấn theo phim rồi lọc thêm theo rạp, trạng thái và thời gian. Khi dữ liệu tăng, lọc phía client khiến response lớn hơn cần thiết và làm logic lọc bị phân tán.

### Hợp đồng API đề xuất

Thêm endpoint mới trong Inventory Service, không đổi semantics của các endpoint showtime hiện có:

`GET /api/v1/showtimes/bookable`

Query parameters đề xuất:

| Tham số    | Bắt buộc | Ý nghĩa                                        |
| ---------- | -------: | ---------------------------------------------- |
| `cinemaId` |       Có | Rạp đã chọn trong QuickBooking.                |
| `from`     |       Có | Mốc đầu khoảng thời gian, ISO-8601 có offset.  |
| `to`       |       Có | Mốc cuối khoảng thời gian, ISO-8601 có offset. |
| `movieId`  |    Không | Chỉ lấy suất của phim đã chọn.                 |

Response đề xuất là danh sách `ShowtimeResponse` theo convention hiện tại của Inventory. Không có kết quả trả `[]`.

### Quy tắc lọc đề xuất

- Chỉ trả suất có trạng thái `OPEN_FOR_BOOKING`.
- Chỉ trả suất có `startsAt` trong khoảng nửa mở `[from, to)`.
- Chỉ trả suất có `startsAt` sau thời điểm hiện tại của server.
- Chỉ trả suất thuộc rạp/phòng đang hoạt động tại thời điểm truy vấn.
- Nếu có `movieId`, lọc theo phim đó.
- Sắp xếp `startsAt ASC`, sau đó `id ASC` để kết quả ổn định.
- `from` và `to` phải hợp lệ, có offset và `from < to`.
- Giới hạn độ dài khoảng truy vấn bằng cấu hình hoặc chính sách đã thống nhất; không tự đặt một giới hạn nghiệp vụ trong lúc implement.
- Không trả số ghế trống nếu service chưa tính được availability đáng tin cậy.

### Cách FE dự kiến sử dụng

QuickBooking giữ thứ tự tương tác theo thiết kế: **Rạp → Phim → Ngày → Suất**. FE gọi endpoint theo ngày/rạp đã chọn, truyền `movieId` khi người dùng đã chọn phim. FE vẫn cần có trạng thái loading, empty và error; không coi danh sách rỗng là lỗi API.

Endpoint này là **đề xuất**, chưa tồn tại chỉ vì được ghi trong tài liệu.

## 6. BE-HOME-02 — Chính sách tạo reservation và hold

Trước khi sửa logic cần thống nhất các quy tắc sau:

1. Có chặn tạo hold khi `startsAt <= now` không?
2. Nếu rạp hoặc phòng bị ngừng hoạt động sau khi suất đã mở bán, có chặn reservation mới không?
3. Nếu suất chuyển khỏi `OPEN_FOR_BOOKING` trong lúc checkout, trạng thái nào là nguồn quyết định cuối cùng?
4. Quy tắc áp dụng như thế nào cho retry, replay message, release và compensation?

### Hướng xử lý đề xuất

- Đặt điều kiện hợp lệ tại các điểm Backend thực sự tạo reservation/hold, không chỉ dựa vào việc FE đã ẩn nút đặt vé.
- Dùng `Clock` hoặc abstraction thời gian hiện có để kiểm tra thời điểm.
- Giữ nguyên locking, idempotency, release và compensation.
- Không biến endpoint đọc danh sách suất chiếu thành cơ chế bảo đảm ghế còn trống.
- Không mở rộng quyền truy cập public cho endpoint hold hiện có nếu endpoint đó đang yêu cầu quyền `inventory:write`.
- Xác nhận chính sách xử lý rạp/phòng bị deactivate trước khi thêm kiểm tra này; không tự suy diễn từ quy tắc lúc mở bán suất.

Đây là thay đổi có ảnh hưởng đến nghiệp vụ và cần review cùng phần Inventory lock hardening đang được thực hiện.

## 7. BE-HOME-06 — Phân loại độ tuổi của phim

Movie hiện chưa có field phân loại độ tuổi trong model/response. FE hiện xử lý trường hợp thiếu dữ liệu bằng cách hiển thị trạng thái chưa có thông tin.

### Đề xuất

- Chỉ thêm field `ageRating` khi có dữ liệu chính thức và quy ước enum được xác nhận.
- Các mã `P`, `K`, `T13`, `T16`, `T18` là danh sách ứng viên để trao đổi với nghiệp vụ; chưa phải enum đã được Backend xác nhận.
- Field cần nullable để tương thích dữ liệu phim cũ.
- Không tự điền rating cho dữ liệu cũ bằng suy đoán.
- Thống nhất nhãn hiển thị tiếng Việt và mapping giữa mã API với FE.
- Badge hiển thị không tự động thay thế chính sách kiểm tra tuổi hoặc quy trình xác minh độ tuổi khi đặt vé.

Migration, DTO, validation, mapper và OpenAPI cần được cập nhật đồng bộ nếu đề xuất được chấp thuận.

## 8. BE-HOME-07 — Chuẩn hóa `trailerUrl`

`trailerUrl` đã tồn tại trong Movie model/response; đây là việc rà soát field hiện có, không phải đề xuất thêm endpoint.

### Đề xuất

- Chuẩn hóa khoảng trắng đầu/cuối.
- Chuyển giá trị rỗng thành `null`.
- Chỉ nhận URL tuyệt đối với scheme được cho phép, tối thiểu là `https`; chỉ cho phép `http` nếu môi trường hoặc chính sách nội bộ cần.
- Xác nhận giới hạn chiều dài với giới hạn DTO hiện tại trước khi thay đổi.
- FE tiếp tục xử lý URL không hợp lệ an toàn và không nhúng nội dung từ nguồn tùy ý.
- Chốt danh sách host được phép nếu hệ thống có chính sách giới hạn nền tảng video.

Không đổi response hiện tại hoặc thêm trailer endpoint nếu chưa có yêu cầu phát sinh.

## 9. BE-HOME-03 — Ảnh ngang `backdropUrl`

Movie hiện có `posterUrl`, nhưng chưa có field ảnh ngang riêng. Nếu Hero cần ảnh ngang chính thức, không nên dùng poster dọc như một backdrop mà không có fallback UI.

### Đề xuất

- Bổ sung field nullable `backdropUrl` vào entity/DTO nếu có nguồn ảnh ngang được quản lý.
- Migration thêm cột nullable; không buộc cập nhật toàn bộ dữ liệu cũ.
- Chốt quy ước storage/CDN và kích thước ảnh với nơi quản lý media.
- FE dùng `backdropUrl` khi hợp lệ, fallback về ảnh local/project hoặc poster đã có.
- Không thêm upload API hoặc lưu file trong Movie Service nếu kiến trúc media hiện tại không hỗ trợ.

## 10. BE-HOME-04 — Movie catalog phân trang và lọc

`GET /api/v1/movies` hiện trả danh sách và FE đang lọc một phần ở client. Chức năng này chưa phải blocker khi dữ liệu còn nhỏ, nhưng nên có hướng mở rộng trước khi catalog tăng đáng kể.

### Nguyên tắc tương thích

- Giữ nguyên `GET /api/v1/movies` và kiểu response hiện tại để không phá client cũ.
- Nếu cần phân trang, thêm route mới, ví dụ `GET /api/v1/movies/catalog`.
- Dùng `PageResponse<MovieResponse>`/`PageInfo` theo shared response model hiện có.
- Chốt rõ API trả `PageResponse` trực tiếp hay nằm trong response envelope theo convention của service trước khi generate client.
- Thống nhất page index, default size và max size với convention dùng chung; cấu hình giới hạn thay vì hardcode số rải rác.

### Bộ lọc đề xuất

- `status`: lọc theo `MovieStatus` hiện có.
- `genre`: lọc theo định danh ổn định của genre sau khi xác nhận shape thực tế của `GenreResponse`.
- Có thể thêm từ khóa tìm kiếm khi Movie UI có ô tìm kiếm và nghiệp vụ thống nhất cách tìm.
- Không thêm `cinemaId` vào `MovieController`. Lọc phim theo suất chiếu hoặc địa điểm thuộc trách nhiệm truy vấn của Inventory hoặc một API tổng hợp được thiết kế rõ ràng.
- Nếu FE chưa cần lọc/phân trang server-side, tiếp tục dùng endpoint cũ cho đến khi có lý do đo được để chuyển.

### Sắp xếp đề xuất

Nếu cần thứ tự mặc định ổn định, có thể dùng ngày phát hành giảm dần, giá trị ngày rỗng ở cuối và `id ASC` làm tie-breaker. Cần xác nhận đây là thứ tự mong muốn của sản phẩm trước khi đưa vào contract.

## 11. BE-HOME-11 — Timezone cho lịch chiếu

Showtime dùng dữ liệu thời gian có offset. Cinema response hiện chưa cung cấp timezone riêng cho từng rạp. Dữ liệu dự án hiện phục vụ rạp tại Việt Nam.

### Đề xuất

- Thống nhất `Asia/Ho_Chi_Minh` làm timezone nghiệp vụ cho ngày/giờ chiếu trong FE.
- FE gửi `from`/`to` dưới dạng ISO-8601 có offset rõ ràng.
- FE nhóm suất theo ngày ở timezone nghiệp vụ, không dựa ngầm vào timezone máy người dùng.
- API mới sử dụng khoảng nửa mở `[from, to)` để tránh tính trùng thời điểm ở hai khoảng ngày liền kề.
- Không thêm timezone riêng trên từng Cinema nếu chưa có nhu cầu vận hành rạp ở nhiều múi giờ.
- Không âm thầm đổi semantics của endpoint khoảng thời gian hiện tại; endpoint mới có thể áp dụng quy ước mới sau khi contract được duyệt.

## 12. BE-HOME-08 — Nội dung biên tập cho Hero

Tự chuyển slide 5 giây, điều khiển carousel, dừng khi người dùng tương tác và hỗ trợ reduced motion là hành vi UI/FE. Backend không cần API timer.

Chỉ xem xét API hoặc field biên tập nếu đội vận hành cần tự chọn phim, thứ tự, tiêu đề hoặc tagline cho Hero. Khi đó cần xác định ai quản lý nội dung và có màn hình quản trị hay không. Không thêm field `featured` hoặc thứ tự hiển thị nếu chưa có workflow để cập nhật chúng.

## 13. BE-HOME-09 — Promotion

FE hiện đang hiển thị dữ liệu preview cục bộ, không phải khuyến mãi có thể giao dịch.

Trước khi tạo Promotion API cần có contract cho:

- Thời hạn áp dụng và trạng thái chiến dịch.
- Đối tượng hoặc suất/phim/rạp được áp dụng.
- Giá trị giảm, giới hạn và điều kiện sử dụng.
- Cách xác thực khuyến mãi khi checkout.
- Quyền quản lý và kiểm toán dữ liệu.

Không để FE hiển thị mức giảm như giá thật nếu Backend chưa xác nhận tính hợp lệ tại thời điểm đặt vé.

## 14. BE-HOME-10 — Membership

Membership benefits ở Home hiện chỉ mang tính minh họa. Chưa có contract API trong phạm vi đang đối chiếu để thay thế nội dung đó.

Chỉ tích hợp khi có Membership API, quy tắc quyền lợi, điều kiện áp dụng và nguồn dữ liệu chính thức. Không thêm dữ liệu thành viên giả vào Movie, Cinema hoặc Showtime response.

## 15. BE-HOME-05 — Availability của ghế

`OPEN_FOR_BOOKING` chỉ mô tả trạng thái mở bán, không phải số ghế còn trống.

Nếu sản phẩm cần hiển thị “còn chỗ”, “sắp hết” hoặc số ghế:

- Xác định nguồn sự thật và thời điểm tính availability.
- Tính theo batch cho nhiều showtime nếu cần; tránh gọi một API riêng cho từng suất.
- Không cache lâu khiến thông tin giữ chỗ sai lệch.
- Coi thông tin trên Home là gợi ý; bước giữ ghế/checkout vẫn phải kiểm tra lại trong Inventory.
- Không bổ sung field availability vào Movie Service vì dữ liệu ghế thuộc Inventory.

Hiện chưa có đủ contract để đề xuất một field response cụ thể.

## 16. BE-HOME-12 — CORS và triển khai

Cấu hình source hiện có pattern cho localhost và một origin Vercel cụ thể:

`https://cinematic-2awjfcb0p-huykunnes-projects.vercel.app`

Cấu hình trong source không chứng minh ứng dụng đã deploy nhận đúng origin hoặc OPTIONS preflight.

### Kiểm tra khi triển khai

- Xác nhận origin thực tế của FE production trùng với origin được cấu hình.
- Kiểm tra preflight `OPTIONS` tới API qua Gateway và các service cần thiết.
- Xác nhận `Access-Control-Allow-Origin`, methods và headers cần thiết trong response.
- Nếu dùng credentials, không dùng wildcard origin.
- Đưa domain thay đổi theo deployment vào cấu hình môi trường phù hợp thay vì thêm nhiều domain preview không kiểm soát.
- Không coi lỗi CORS là lỗi MovieController nếu preflight bị chặn ở Gateway hoặc cấu hình deployment.

## 17. Quy tắc kiến trúc khi triển khai

- Inventory sở hữu cinema, room, showtime, seat và hold.
- Movie Service sở hữu movie metadata và genre.
- Không truy cập trực tiếp database của service khác.
- Giữ nguyên các endpoint/response hiện tại; ưu tiên endpoint mới có tính additive.
- Cập nhật OpenAPI và generated FE client từ contract đã được duyệt.
- Không sửa DTO sinh tự động bằng tay.
- Giữ response public phù hợp với public read hiện có; không yêu cầu đăng nhập chỉ để xem Home nếu sản phẩm chưa đổi yêu cầu.
- Không lưu trạng thái chọn city/cinema của người dùng trong Backend chỉ để phục vụ điều hướng FE. Trạng thái này hiện do FE quản lý.
- Lưu ý Pinia store hiện giữ lựa chọn trong phiên SPA; persistence sau khi reload là việc của FE nếu sản phẩm cần.
- Không đưa thông tin secret, credential hoặc password mẫu vào source, config hay tài liệu commit.

## 18. Kiểm tra thủ công đề xuất

Các mục dưới đây là checklist **đề xuất để chạy sau khi implement**. Chưa có kiểm tra runtime, migration, test hoặc build nào được thực hiện trong lúc soạn tài liệu.

### Movie catalog

- Gọi `GET /api/v1/movies`; xác nhận client cũ vẫn nhận đúng kiểu danh sách.
- Nếu thêm catalog route, kiểm tra trang đầu, trang cuối, kết quả rỗng, status filter và genre filter.
- Kiểm tra thứ tự ổn định giữa các lần gọi khi nhiều phim có cùng ngày phát hành hoặc ngày bị thiếu.
- Kiểm tra FE vẫn xử lý được phim thiếu trailer, poster, backdrop hoặc age rating.

### Cinema và Showtime

- Truy vấn suất cho một rạp và một khoảng thời gian hợp lệ.
- Kiểm tra `from` bao gồm và `to` không bao gồm theo quy ước của endpoint mới.
- Kiểm tra suất quá khứ, suất ngoài rạp đã chọn, suất chưa mở bán và phòng/rạp không hoạt động.
- Kiểm tra ngày/giờ trên FE khi timezone trình duyệt khác `Asia/Ho_Chi_Minh`.
- Kiểm tra QuickBooking theo thứ tự **Rạp → Phim → Ngày → Suất**, cùng các trạng thái loading, empty và error.

### Reservation và hold

Chỉ thực hiện sau khi chính sách BE-HOME-02 được duyệt:

- Thử reservation với suất `OPEN_FOR_BOOKING` trong tương lai.
- Thử suất đã bắt đầu hoặc đã kết thúc.
- Thử suất chuyển trạng thái trong lúc người dùng checkout.
- Thử trạng thái phòng/rạp thay đổi sau khi suất mở bán, theo chính sách đã duyệt.
- Kiểm tra hold hết hạn, ghế không còn `AVAILABLE`, retry/replay và compensation.
- Xác nhận các route ghi dữ liệu vẫn được bảo vệ đúng quyền.

### Customer Home và metadata

- Kiểm tra Hero carousel tự chuyển ở FE, có thể dừng/tiếp tục phù hợp với accessibility và reduced motion.
- Kiểm tra yêu cầu autoplay của NowShowing được thực hiện ở FE mà không phụ thuộc API timer.
- Xác nhận Promotion và Membership vẫn được phân biệt rõ với dữ liệu khuyến mãi/quyền lợi thật.
- Kiểm tra CORS preflight từ đúng origin FE production qua Gateway.

## 19. Lộ trình và commit message gợi ý

Chỉ commit sau khi thay đổi tương ứng đã được implement, review và xác nhận. Các commit message sau là gợi ý, không có thay đổi nào đã được thực hiện bởi tài liệu này.

| Thứ tự | Phạm vi                                               | Commit message gợi ý                                             |
| ------ | ----------------------------------------------------- | ---------------------------------------------------------------- |
| 1      | Thêm truy vấn suất mở bán cho QuickBooking            | `feat(inventory): add bookable showtime query for customer flow` |
| 2      | Chuẩn hóa điều kiện tạo hold sau khi duyệt nghiệp vụ  | `fix(inventory): align customer hold eligibility checks`         |
| 3      | Thêm age rating khi có nguồn dữ liệu xác nhận         | `feat(movie): add movie age rating metadata`                     |
| 4      | Chuẩn hóa validation trailer hiện có                  | `fix(movie): validate trailer URLs consistently`                 |
| 5      | Thêm backdrop nullable khi có nguồn ảnh chính thức    | `feat(movie): add movie backdrop metadata`                       |
| 6      | Thêm catalog phân trang khi cần và giữ API cũ         | `feat(movie): add paginated customer catalog`                    |
| 7      | Chỉ thêm API Promotion/Membership theo contract riêng | Commit theo domain và API đã được duyệt                          |

## 20. Tài liệu và source tham chiếu

### Backend

- Repo: `https://github.com/HuyKunNe/cinema-system`
- Tài liệu hiện tại: `docs/cinema_backend_home_proposals.md`
- Các khu vực cần đối chiếu khi implement: Movie controller/service/entity/DTO/repository, Cinema controller/service, Showtime controller/service/repository, reservation consumer/service, shared response model, Gateway CORS và OpenAPI.

### Frontend

- Repo: `https://github.com/HuyKunNe/cinematic-web`
- Thiết kế tham chiếu: `docs/design/reference/html-convert`
- Các khu vực cần đối chiếu khi implement: Home sections, Movies page, QuickBooking, location store, API client/generated models và các query composable.

### Ghi chú trạng thái

Đây là tài liệu đề xuất dựa trên hai baseline commit nêu ở đầu file. Trước khi triển khai, hãy kiểm tra lại `main` của cả hai repo và cập nhật phần baseline nếu commit đã thay đổi.
