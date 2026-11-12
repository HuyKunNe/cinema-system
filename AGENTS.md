# AGENTS.md — Backend

## Dự án và phạm vi

- Repository BE: https://github.com/HuyKunNe/cinema-system
- Repository FE tham chiếu API: https://github.com/HuyKunNe/cinematic-web
- Đây là hệ thống đặt vé xem phim dùng Java, Spring Boot, Spring Cloud và Maven multi-module.
- Đọc code và tài liệu trong checkout hiện tại trước khi xác định trạng thái dự án.
- Không coi roadmap hoặc tên module là bằng chứng rằng chức năng đã triển khai hoặc xác minh thành công.

## Làm việc trong repository

- Trước khi sửa, kiểm tra `git status` và giữ nguyên mọi thay đổi có sẵn.
- Codex được sửa file cần thiết trong phạm vi task hiện tại để tôi review.
- Không tự pull, reset, đổi branch, xóa thay đổi, commit hoặc push.
- Không sửa repository FE. Chỉ đọc FE khi cần đối chiếu API.
- Không chạy test mặc định. Chỉ chạy khi tôi yêu cầu rõ.
- Không tuyên bố build, test, migration hoặc tích hợp đã thành công nếu chưa thực sự chạy và quan sát kết quả.
- Không chạy migration hoặc lệnh có thể ghi vào database production.
- Không in, lưu vào Git hoặc đưa vào tài liệu secret, password, token hay connection string.
- Nếu tôi chỉ yêu cầu giải thích hoặc hướng dẫn, không sửa file.

## Nguồn thông tin

Đọc các tài liệu liên quan trước khi làm:

- `README.md`
- `docs/BE_ROADMAP.md`
- `docs/10_ROADMAP.md`
- `docs/11_CHANGELOG.md`
- `CURRENT_STATUS.md` nếu có
- `AGENTS.md` trong thư mục con nếu có

Ưu tiên nguồn theo thứ tự:

1. Yêu cầu mới nhất của tôi.
2. Code, cấu hình và Git status trong checkout hiện tại.
3. Contract runtime/OpenAPI hiện hành và controller/DTO/validation/security tương ứng.
4. Roadmap, changelog và tài liệu kiến trúc.

Nếu code và tài liệu mâu thuẫn, chỉ ra khác biệt, dùng code hiện tại để mô tả hành vi và không tự sửa nghiệp vụ dựa trên giả định.

Roadmap hiện ghi R28 Notification đang hoãn. Kiểm tra tài liệu mới nhất trước khi lập kế hoạch; không tự mở R28.

## Công nghệ và database

- Giữ nguyên công nghệ, phiên bản dependency và cấu trúc Maven hiện có; kiểm tra POM trước khi thêm dependency.
- Database tiếp tục dùng MySQL. Giữ cấu trúc database-per-service và các migration Flyway theo repo.
- Không chuyển sang PostgreSQL hoặc Supabase.
- Không thay JDBC driver, SQL dialect, kiểu dữ liệu, connection config hoặc lịch sử migration MySQL nếu task không yêu cầu.
- Không dùng Hibernate `ddl-auto=update` để thay thế migration.
- Không sửa migration đã áp dụng như thể đó là migration mới. Tạo migration tiếp theo theo quy ước của service.
- Rà soát transaction, khóa, unique constraint, foreign key và tính tương thích dữ liệu khi thay đổi persistence.

## Kiến trúc và nghiệp vụ

- Giữ ranh giới sở hữu dữ liệu theo service. Không truy cập trực tiếp database của service khác.
- Giao tiếp giữa service qua API hoặc event contract hiện có.
- Giữ các quy tắc hiện có về transaction, idempotency, transactional outbox, retry và xử lý event trùng.
- Với Inventory, đặc biệt bảo toàn invariant giữ ghế, xác nhận, hủy/nhả ghế, hết hạn hold, rollback và xử lý cạnh tranh.
- Không thêm endpoint, DTO, enum hoặc event trùng chức năng. Trước khi tạo mới, tìm API/model tương đương.
- Khi đổi API, kiểm tra controller, service, validation, authorization, OpenAPI, lỗi trả về và client FE liên quan.
- Không khẳng định một API còn dùng nếu source hiện tại đã xóa hoặc thay thế nó.

### Phim và phiên bản chiếu

- Đối chiếu với contract mới nhất của BE.
- Khi contract hiện tại yêu cầu `screeningVersions` nằm trong request tạo/sửa phim, giữ phiên bản chiếu trong cùng workflow đó.
- Không tự tách thành thao tác tạo phiên bản sau khi lưu phim nếu chưa có yêu cầu mới.
- Phân biệt định dạng hình ảnh và công nghệ trình chiếu theo model hiện tại; không tự gộp IMAX/4DX vào 2D/3D.

### Suất chiếu theo đợt

Khi task liên quan đến tạo nhiều suất chiếu, áp dụng các quy tắc nghiệp vụ đã được thống nhất:

- Một đợt có thể tạo suất cho một phim tại một rạp, qua nhiều phòng.
- Chọn một phim cho cả đợt; chọn phiên bản riêng theo từng phòng.
- Nhập khung giờ cho từng phòng rồi áp dụng vào những ngày cụ thể đã chọn.
- Nếu có suất bị trùng, chặn lưu toàn bộ đợt và chỉ rõ các suất cần sửa; không lưu một phần.

Đối chiếu các quy tắc này với API hiện có. Nếu API không đáp ứng, nêu contract còn thiếu; không tự bịa endpoint hoặc âm thầm thay nghiệp vụ.

## Xác thực và Google Login

- User Service tiếp tục là hệ thống định danh/OIDC issuer của ứng dụng.
- Nếu làm Google Login, Google là nhà cung cấp danh tính bên ngoài; FE tiếp tục luồng OIDC hiện có với User Service.
- Ánh xạ Google identity sang tài khoản nội bộ và quyền lấy từ dữ liệu nội bộ. Không lấy role/permission từ Google profile.
- Không đưa Google Client Secret vào FE hoặc repo.
- Không coi Google Login đã hoàn chỉnh chỉ vì đã thêm nút hoặc client ID. Đối chiếu account linking/provisioning, principal, JWT claims, session và audit.
- Nếu quy tắc tạo tài khoản/liên kết tài khoản chưa được chốt, dừng ở phần cần quyết định và hỏi tôi.
- Các biến SMTP hoặc Google OAuth chỉ có tác dụng nếu được property binding và code runtime thực sự sử dụng. Không tuyên bố đã tích hợp chỉ dựa trên việc thêm biến môi trường.

## Thay đổi và tài liệu

- Làm một phạm vi BE cụ thể mỗi lượt, giữ diff tập trung.
- Cập nhật tài liệu liên quan khi thay đổi contract, migration, cấu hình hoặc trạng thái roadmap.
- Không đánh dấu một phase hoàn tất nếu chỉ có code mà các tiêu chí xác minh bắt buộc chưa được chạy.
- Nếu task bị chặn bởi API, quyết định nghiệp vụ, secret hoặc quyền truy cập, báo đúng điểm chặn và đề xuất bước tiếp theo.

## Báo cáo sau mỗi lượt

Trả lời bằng tiếng Việt, gồm:

1. Mục tiêu và phần đã làm.
2. File đã sửa và lý do.
3. Ảnh hưởng đến API, service khác, MySQL hoặc FE.
4. Kiểm tra đã chạy và chưa chạy; ghi rõ test đã bỏ qua nếu không được yêu cầu.
5. Việc còn lại hoặc quyết định cần tôi chốt.
6. Commit message gợi ý.

Không tự commit hoặc push.
