# Backend Roadmap — Cinema Booking System

**Cập nhật theo trạng thái repo đến:** 2026-11-06 (đối chiếu source `main` tại `ac09155`)  
**Repository:** https://github.com/HuyKunNe/cinema-system  
**Phạm vi:** Backend phục vụ Admin FE và User FE; không bao gồm triển khai Notification Service R28 đang được hoãn.

## Cách đọc trạng thái

- **DONE IN SOURCE:** code hoặc thay đổi đã có trong repository; chưa tự khẳng định đã qua kiểm chứng runtime.
- **VERIFY:** cần đối chiếu source, migration, OpenAPI và các kiểm tra được duyệt trước khi đóng.
- **IN PROGRESS:** đang có trong trạng thái dự án.
- **BLOCKED:** phụ thuộc contract hoặc quyết định chưa hoàn thành.
- **PLANNED:** chưa bắt đầu.

Roadmap này không thay thế `docs/10_ROADMAP.md`. P0.1 đã đồng bộ trạng thái tài liệu; checkpoint đang hoạt động là Inventory lifecycle-release lock hardening, tiếp tục theo P0.2. Chưa đóng các cổng xác minh runtime.

## Trạng thái đầu vào

- README ghi R1–R27 hoàn thành, Inventory lifecycle-release lock hardening đang thực hiện và R28 Notification được hoãn.
- P0.1 đã sửa các mô tả cũ ghi R28 là bước tiếp theo và R27/R27.10/R27.13 còn đang chạy. README, roadmap và changelog cùng ghi Inventory hardening đang thực hiện, R28 Deferred.
- R27 là mốc hoàn tất lịch sử. Commit `ae85063` đã bỏ API refund, API quản trị reconciliation và xử lý refund theo lịch; không coi các mô tả R27 cũ là contract FE hiện tại.
- Commit `c2489a68` ngày 2026-11-04 tách công nghệ chiếu khỏi định dạng 2D/3D. Changelog ghi kiểm tra tự động chưa chạy.
- Các API tạo/sửa phim đã được thiết kế để mang `screeningVersions` lồng trong request phim. Không chuyển việc này thành một bước tạo phiên bản riêng sau khi tạo phim.

## P0 — Khóa trạng thái và thứ tự công việc

### P0.1 — Đồng bộ trạng thái tài liệu

**Trạng thái:** DONE — rà soát source/tài liệu ngày 2026-11-06; không xác nhận runtime. Tiêu chí Inventory còn mở và bằng chứng source được ghi tại `docs/10_ROADMAP.md`, mục “Post-R27 Inventory lifecycle-release hardening”.

- Đối chiếu README, `docs/10_ROADMAP.md`, `docs/11_CHANGELOG.md` và trạng thái thực của working tree/main.
- Ghi rõ Inventory lifecycle-release lock hardening còn mở những tiêu chí nào.
- Ghi R28 Notification là **Deferred**, không phải checkpoint đang chạy.
- Không đổi trạng thái checkpoint chỉ dựa trên tên module hoặc mô tả dự kiến.

**Hoàn tất khi:** README, roadmap và changelog cùng chỉ một checkpoint BE đang hoạt động.

### P0.2 — Đóng hardening Inventory hiện hành

**Trạng thái:** IN PROGRESS / VERIFY — đã bổ sung test MySQL cho hai thứ tự cạnh tranh confirmation–lifecycle và hold đổi chủ sau bước lấy ID, test repository cho truy vấn ID/khóa, và retry cùng event sau rollback. Chưa biên dịch/chạy test, build hoặc kiểm tra tự động theo yêu cầu chủ dự án; chưa đóng P0.2. Code nghiệp vụ, API và migration giữ nguyên.

- Đọc phạm vi cụ thể của lifecycle-release lock hardening từ code và tài liệu hiện tại.
- Hoàn tất các invariant cạnh tranh và rollback còn thiếu.
- Chạy các kiểm tra tích hợp được BE roadmap yêu cầu sau khi chủ dự án cho phép.
- Cập nhật kết quả xác minh, không ghi “đã xác minh” nếu chưa chạy.

**Phụ thuộc:** xác nhận phạm vi hiện tại trong README và code; không tự mở rộng sang R28.

### P0.3 — Chốt contract dùng chung cho FE

**Điều chỉnh ngày 2026-11-10 theo yêu cầu chủ dự án:** bỏ annotation Swagger/OpenAPI
trong controller; không bổ sung lại `@Operation`, `@ApiResponse` hoặc metadata tương
đương tại controller. Springdoc tự suy ra operation; các tên explicit trong tài liệu
review cũ là bằng chứng lịch sử. Cần đối chiếu spec export và tên client FE khi
generate lại. Không thay đổi HTTP mapping, validation, security hoặc MySQL.

**Tiếp tục từ `f2d950e` ngày 2026-11-10:** đổi tên 28 handler trong 13 controller
Movie/Inventory sang đúng tên nghiệp vụ từng dùng làm explicit operationId; cả 28
tên khớp các hàm trong generated FE client hiện có (chỉ đọc). Không thêm annotation.
Rà soát tĩnh không thấy public method trùng tên giữa các REST controller hiện tại;
giữ nguyên phần thân handler, mapping và validation. Đây là bằng chứng source,
không thay thế kiểm chứng toàn runtime spec; P0.2/P0.3 vẫn mở.

**Trạng thái:** IN PROGRESS / VERIFY — rà soát source tại `34fafae`, snapshot/client FE chỉ đọc; bổ sung [contract và ví dụ tích hợp](19_FE_API_CONTRACT.md) cho Admin movie/showtime, User quote/booking và Admin payment reads. Bổ sung OpenAPI response 409 đã có trong source của API tạo screening version; không đổi operationId, endpoint, payload hoặc quyền. Runtime OpenAPI, tính duy nhất operationId trên toàn spec và các luồng còn lại chưa xác minh. P0.2 tiếp tục chờ các cổng kiểm tra được chủ dự án cho phép; lượt P0.3 này không đóng P0.2.

**Tiếp tục tại `4e58548`:** đã rà soát source và bổ sung [account/layout/pricing](20_FE_ACCOUNT_LAYOUT_PRICING_CONTRACT.md), đồng bộ tài liệu pricing vốn còn ghi implementation pending. Ghi nhận User API đang dùng session/CSRF, chưa có Resource Server chain cho Bearer trên các route account; đây là gap tích hợp cần chốt theo P5.1/P5.2. Không đổi security hoặc tự tạo API user search. Phần nguồn và ví dụ đã bổ sung; vẫn chờ runtime OpenAPI, operationId toàn spec và xác minh HTTP khi được phép. P0.3 chưa đóng.

- Đối chiếu controller, DTO, entity, migration và runtime OpenAPI.
- Giữ `operationId` duy nhất, có tên nghiệp vụ rõ ràng.
- Ghi breaking changes, migration và thứ tự triển khai FE/API client.
- Cung cấp ví dụ request/response cho từng luồng Admin và User cần dùng.

## P1 — Phiên bản chiếu và công nghệ trình chiếu

### P1.1 — Xác minh phần tách 2D/3D khỏi công nghệ chiếu

- Đối chiếu commit `c2489a68` và Changelog với code thực tế.
- Xác nhận `ContentFormat` biểu diễn 2D/3D; `ProjectionTechnology` biểu diễn STANDARD, IMAX, FOUR_DX, SCREEN_X và các giá trị được sản phẩm chấp thuận.
- Xác nhận các tổ hợp hợp lệ giữa công nghệ và định dạng, không tự tạo tích Descartes các tổ hợp.
- Rà soát response phiên bản phim và snapshot phiên bản trong Inventory showtime.

**Trạng thái:** DONE IN SOURCE / VERIFY.

### P1.2 — Bắt buộc phiên bản trong API tạo/sửa phim

- Yêu cầu ít nhất một phiên bản trong request tạo và request thay thế/sửa phim.
- Không chấp nhận danh sách null, bị lược hoặc rỗng.
- Tạo phim và các phiên bản chiếu trong cùng transaction.
- Giữ các endpoint đọc/kích hoạt phiên bản đang được FE dùng; không thêm endpoint trùng để tạo phiên bản sau khi lưu phim.

### P1.3 — Chặn bản ghi phiên bản trùng

- Kiểm tra trùng trên định danh đầy đủ: movie, format, projection technology, audio language, presentation type và subtitle language.
- Chuẩn hóa trường chuỗi trước khi kiểm tra.
- Đồng bộ validation service, repository và unique constraint trong database.
- Khi sửa, yêu cầu danh sách đầy đủ; giữ ID và `expectedVersion` của bản ghi đã lưu.
- Định nghĩa rõ phiên bản bỏ chọn được chuyển inactive như thế nào; không xóa lịch sử phiên bản đã dùng trong suất chiếu.

### P1.4 — Đồng bộ Inventory và suất chiếu

- Snapshot công nghệ chiếu vào showtime để hợp đồng không đổi khi metadata phim được cập nhật.
- Kiểm tra công nghệ/định dạng của phim có tương thích loại phòng đã chọn không.
- Trả các trường đủ để Admin và khách hàng phân biệt, ví dụ “IMAX · 2D”.
- Cập nhật tài liệu API và changelog.

**Hoàn tất P1 khi:** migration, API, dữ liệu cũ, validation và Inventory đều dùng cùng một định nghĩa phiên bản; FE có thể generate client từ OpenAPI.

## P2 — Contract sơ đồ phòng dạng ma trận

Tài liệu FE ghi editor ma trận đang lưu state trong bộ nhớ, trong khi contract được kiểm tra trước đó vẫn dùng canvas/geometry. Xác nhận lại code BE mới nhất trước khi triển khai.

### P2.1 — Chốt document và vòng đời sơ đồ

- Định nghĩa document chứa số hàng/cột, nhãn hàng suy ra theo vị trí, và cell rỗng hoặc ghế có số/type.
- Mặc định ma trận mới là 6×6 với các cell rỗng.
- Chốt semantics draft, publish, archived, revision và optimistic concurrency.
- Xác định ghế đôi chiếm một cell theo UI hiện tại; không suy ra `colspan` hoặc geometry.

### P2.2 — Thiết kế persistence và migration

- Đảm bảo giữ được hàng/cột rỗng, cell rỗng ở giữa và cột trống cuối.
- Không thay đổi seat catalog như một cách lưu ma trận.
- Dùng migration mới; không viết lại migration đã áp dụng.
- Xác định cách chuyển hoặc giữ dữ liệu geometry cũ mà không làm mất dữ liệu.

### P2.3 — Thiết kế API đọc/ghi

- API tải layout draft/published và toàn bộ matrix document.
- API tạo draft, lưu toàn bộ nội dung kèm revision, publish và truy vấn trạng thái.
- Lỗi conflict trả về version hiện tại để FE tải lại; không tự ghi đè.
- API giữ rõ ràng `seatType`, số ghế, trạng thái hoạt động và ID ổn định.

### P2.4 — Validation và xác minh contract

- Chặn số ghế không hợp lệ, số trùng trong cùng hàng và loại ghế không được hỗ trợ.
- Server chuẩn hóa số ghế 1…n theo thứ tự cell có ghế trong mỗi hàng nếu đó là quy tắc được chốt.
- Kiểm tra save/load giữ nguyên cell trống, kích thước và row labels.
- Cập nhật OpenAPI, ví dụ payload và docs Inventory ownership.

**Hoàn tất P2 khi:** Inventory sở hữu dữ liệu ma trận; API không yêu cầu FE chuyển matrix thành geometry.

## P3 — Giá vé và suất chiếu

### P3.1 — Đóng contract cấu hình bảng giá

- Xác minh version, draft/published, time bands, ticket groups và fare matrix hiện tại.
- Giữ ID định nghĩa để đổi tên/thứ tự không làm mất giá.
- Bảo đảm lỗi version conflict không cho phép ghi đè im lặng.
- Giữ đúng đơn vị ghế đôi là giá cả cặp; giá ghế đôi được phép khác theo nhóm vé, khung giờ và định dạng phim.
- Giá 2D và 3D nhập/resolve riêng; không tự đồng bộ giá.

### P3.2 — Hoàn thiện API suất chiếu

- Xác minh paging, filter theo cụm rạp/phim/phòng/ngày và status.
- Tạo/sửa suất chiếu phải tham chiếu phim, phiên bản chiếu, phòng đã cấu hình và bảng giá hợp lệ.
- Kiểm tra công nghệ phim với capability của phòng và không cho lịch bị chồng lấn.
- Lưu snapshot cần thiết cho suất chiếu đã xuất bản.

### P3.3 — Báo giá cho User FE

- Cung cấp giá vé cuối cùng cho suất chiếu/ghế/nhóm vé theo đúng bảng giá đã publish.
- Tính giá ghế đôi cho cả cặp.
- Trả rõ giá chưa cấu hình/không áp dụng; không coi bằng 0 là thiếu giá.
- Đảm bảo quote và giá lưu trong booking dùng cùng resolver.

## P4 — Rà soát contract Booking và Payment cho User FE

### P4.1 — Chốt luồng giữ ghế

- Xác minh lấy availability, hold, release, hết hạn và xử lý ghế bị chiếm.
- Inventory là nguồn sự thật của trạng thái ghế theo suất chiếu.
- Không để FE tự giữ trạng thái ghế có thẩm quyền trong Pinia/local storage.

### P4.2 — Chốt luồng tạo booking

- Xác minh request, idempotency, giá/ghế được snapshot và lỗi conflict.
- Không nhận giá do FE tự tính làm nguồn sự thật.
- Ghi rõ trạng thái khi booking chờ thanh toán hoặc bị hết hạn.

### P4.3 — Chốt luồng thanh toán

- Xác minh khởi tạo thanh toán, return URL, terminal result, retry và correlation ID.
- Giữ saga/event flow đã có; chỉ bổ sung API nếu phát hiện gap thực tế.
- Không đưa refund vào roadmap FE hiện tại vì refund API đã bị loại khỏi BE gần đây; cần thiết kế và duyệt lại riêng nếu muốn khôi phục.

## P5 — Account và API vận hành còn thiếu

### P5.1 — Đối chiếu Customer account

- Xác minh profile, booking history/detail và quyền sở hữu.
- Chỉ bổ sung API nếu User FE không thể hoàn thành luồng bằng contract hiện có.

### P5.2 — Đối chiếu Admin operations

- Xác minh booking lookup/detail, user management, payment lookup và reconciliation.
- Giữ quyền theo permission hiện có và audit actor từ principal đã xác thực.
- Promotions nằm ngoài scope cho đến khi nghiệp vụ/API được xác nhận.

## P6 — Đóng vòng kỹ thuật và tài liệu

### P6.1 — Kiểm tra liên service và migration

- Xác minh tương thích Movie → Inventory → Booking → Payment.
- Ghi rõ migration cần triển khai trước/sau service nào.

### P6.2 — Cập nhật tài liệu

- Đồng bộ README, Roadmap, Changelog, OpenAPI, database design, event catalog và sequence diagrams.
- Ghi các kiểm tra đã chạy/chưa chạy riêng biệt.

### P6.3 — Để R28 hoãn

- Không scaffolding Notification Service, API client hoặc UI Notification khi chưa có quyết định mở lại scope.

## Thứ tự phụ thuộc

`P0 → P1 → P2 → P3 → P4 → P5 → P6`

P4/P5 có thể audit song song với P2/P3, nhưng chỉ triển khai phần API gap sau khi contract hiện tại được đối chiếu.
