# config run at local

CREATE DATABASE IF NOT EXISTS cinema_user_db;
CREATE DATABASE IF NOT EXISTS cinema_movie_db;
CREATE DATABASE IF NOT EXISTS cinema_inventory_db;
CREATE DATABASE IF NOT EXISTS cinema_booking_db;
CREATE DATABASE IF NOT EXISTS cinema_payment_db;

## config-service

.\mvnw.cmd -pl infrastructure/config-service spring-boot:run

## discovery-service

```text
$env:CONFIG_SERVER_URL = "http://localhost:8888"
.\mvnw.cmd -pl infrastructure/discovery-service spring-boot:run
```

## User Service

New-Item -ItemType Directory -Force .\.local\keys | Out-Null

openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out .\.local\keys\user-service-private.pem

openssl pkey -in .\.local\keys\user-service-private.pem -pubout -out .\.local\keys\user-service-public.pem

```text
$privateKey = (Resolve-Path .\.local\keys\user-service-private.pem).Path.Replace('\','/')
$publicKey = (Resolve-Path .\.local\keys\user-service-public.pem).Path.Replace('\','/')

$env:CONFIG_SERVER_URL = "http://localhost:8888"
$env:EUREKA_URL = "http://localhost:8761/eureka/"
$env:USER_DB_URL = "jdbc:mysql://localhost:3306/cinema_user_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:USER_DB_USERNAME = "root"
$env:USER_DB_PASSWORD = "huykun17"
$env:USER_AUTHORIZATION_SERVER_ISSUER = "http://localhost:8082"
$env:USER_JWT_AUDIENCES = "cinema-api"
$env:USER_JWT_SIGNING_ENABLED = "true"
$env:USER_JWT_SIGNING_KEY_ID = "cinema-local-key-1"
$env:USER_JWT_PRIVATE_KEY_LOCATION = "file:$privateKey"
$env:USER_JWT_PUBLIC_KEY_LOCATION = "file:$publicKey"
$env:CINEMA_LOCAL_BOOTSTRAP_ENABLED = "true"
.\mvnw.cmd -pl services/user-service spring-boot:run
```

## movie-service

$env:CONFIG_SERVER_URL = "http://localhost:8888"
$env:EUREKA_SERVER_URL = "http://localhost:8761/eureka/"
$env:MOVIE_DB_URL = "jdbc:mysql://localhost:3306/cinema_movie_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:MOVIE_DB_USERNAME = "root"
$env:MOVIE_DB_PASSWORD = "huykun17"
$env:CINEMA_AUTH_ISSUER = "http://localhost:8082"
$env:CINEMA_AUTH_JWK_SET_URI = "http://localhost:8082/oauth2/jwks"
$env:CINEMA_AUTH_AUDIENCE = "cinema-api"
.\mvnw.cmd -pl services/movie-service spring-boot:run

## Inventory Service

$env:CONFIG_SERVER_URL = "http://localhost:8888"
$env:EUREKA_URL = "http://localhost:8761/eureka/"
$env:INVENTORY_DB_URL = "jdbc:mysql://localhost:3306/cinema_inventory_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:INVENTORY_DB_USERNAME = "root"
$env:INVENTORY_DB_PASSWORD = "huykun17"
$env:CINEMA_AUTH_ISSUER = "http://localhost:8082"
$env:CINEMA_AUTH_JWK_SET_URI = "http://localhost:8082/oauth2/jwks"
$env:CINEMA_AUTH_AUDIENCE = "cinema-api"
$env:KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"
$env:INVENTORY_KAFKA_ENABLED = "true"
.\mvnw.cmd -pl services/inventory-service spring-boot:run

## Booking Service

$env:CONFIG_SERVER_URL = "http://localhost:8888"
$env:EUREKA_SERVER_URL = "http://localhost:8761/eureka/"
$env:BOOKING_DB_URL = "jdbc:mysql://localhost:3306/cinema_booking_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:BOOKING_DB_USERNAME = "root"
$env:BOOKING_DB_PASSWORD = "huykun17"
$env:CINEMA_AUTH_ISSUER = "http://localhost:8082"
$env:CINEMA_AUTH_JWK_SET_URI = "http://localhost:8082/oauth2/jwks"
$env:CINEMA_AUTH_AUDIENCE = "cinema-api"
$env:KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"
$env:BOOKING_KAFKA_ENABLED = "true"
.\mvnw.cmd -pl services/booking-service spring-boot:run

## Payment Service

$env:CONFIG_SERVER_URL = "http://localhost:8888"
$env:EUREKA_SERVER_URL = "http://localhost:8761/eureka/"
$env:PAYMENT_DB_URL = "jdbc:mysql://localhost:3306/cinema_payment_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:PAYMENT_DB_USERNAME = "root"
$env:PAYMENT_DB_PASSWORD = "huykun17"
$env:CINEMA_AUTH_ISSUER = "http://localhost:8082"
$env:CINEMA_AUTH_JWK_SET_URI = "http://localhost:8082/oauth2/jwks"
$env:CINEMA_AUTH_AUDIENCE = "cinema-api"
$env:KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"
$env:PAYMENT_KAFKA_ENABLED = "true"
$env:PAYMENT_PROVIDER = "MOCK"
$env:PAYMENT_PROVIDER_OPERATION_SCHEDULING_ENABLED = "true"
.\mvnw.cmd -pl services/payment-service spring-boot:run

## Gateway Service

```text
$env:CONFIG_SERVER_URL = "http://localhost:8888"
$env:EUREKA_SERVER_URL = "http://localhost:8761/eureka/"
$env:CINEMA_AUTH_ISSUER = "http://localhost:8082"
$env:CINEMA_AUTH_JWK_SET_URI = "http://localhost:8082/oauth2/jwks"
$env:CINEMA_AUTH_AUDIENCE = "cinema-api"
.\mvnw.cmd -pl infrastructure/gateway-service spring-boot:run
```

### create RAS key

mkdir -p .local/keys
New-Item -ItemType Directory -Force .\local-keys
openssl genpkey -algorithm RSA `  -pkeyopt rsa_keygen_bits:2048`
-out .\local-keys\jwt-private.pem

openssl rsa `  -pubout`
-in .\local-keys\jwt-private.pem `
-out .\local-keys\jwt-public.pem

| Service   |   Port | Swagger                                 |
| --------- | -----: | --------------------------------------- |
| Movie     | `8081` | `http://localhost:8081/swagger-ui.html` |
| User      | `8082` | `http://localhost:8082/swagger-ui.html` |
| Inventory | `8083` | `http://localhost:8083/swagger-ui.html` |
| Booking   | `8084` | `http://localhost:8084/swagger-ui.html` |
| Payment   | `8085` | `http://localhost:8085/swagger-ui.html` |

Bạn là technical pair-programming assistant cho dự án Cinema System. Hãy hướng dẫn tôi triển khai các đề xuất Backend để hỗ trợ Frontend. Tôi đang dùng ChatGPT Work trên browser, không dùng Codex CLI.

## Repository

- Backend: https://github.com/HuyKunNe/cinema-system
- Frontend: https://github.com/HuyKunNe/cinematic-web
- Tài liệu đề xuất: `docs/cinema_backend_home_proposals.md` trong repo Backend.
- HTML/CSS tham chiếu giao diện: `docs/design/reference/html-convert` trong repo Frontend.

## Quyền truy cập và cách làm việc

- Cả hai repository chỉ được phép đọc. Không sửa file, tạo commit, push, mở PR hoặc thay đổi GitHub.
- Dùng GitHub connector để đọc `main` và các file cần thiết. Không coi dữ liệu từ chat cũ là trạng thái repo hiện tại.
- Tôi sẽ tự áp dụng code và tự commit. Sau mỗi round, cung cấp hướng dẫn triển khai bằng Markdown/code block để tôi thực hiện.
- Không chạy hoặc yêu cầu chạy test/build trong các round, trừ khi tôi yêu cầu sau.
- Sau khi đưa đầy đủ hướng dẫn cho một round, dừng lại để tôi tự implement và báo kết quả. Chỉ tiếp tục round sau khi tôi xác nhận.
- Trả lời bằng tiếng Việt, giải thích rõ file nào cần sửa, vị trí nào, thay đổi ra sao và lý do.

## Đọc và xác minh trước khi hướng dẫn

1. Đọc toàn bộ `docs/cinema_backend_home_proposals.md`.
2. Đọc source code liên quan ở cả BE và FE để xác minh trạng thái hiện tại, gồm controller, service, repository, entity, DTO/response, security, OpenAPI/generated client và query composable.
3. Ghi rõ repo đang ở commit nào và phân biệt:
   - Đã có trong source.
   - Chỉ mới được đề xuất trong tài liệu.
   - Cần quyết định nghiệp vụ trước khi làm.
4. Nếu nội dung trong repo khác với danh sách đề xuất bên dưới, hãy nêu khác biệt và dùng source code hiện tại để xác minh API nào đang tồn tại. Không được trình bày endpoint/field đề xuất như thể đã implement.
5. Không đoán tên class, field, enum, response wrapper hoặc cấu trúc thư mục. Nếu chưa xác minh được, hãy nói rõ và chỉ ra file cần kiểm tra tiếp.

## Danh sách đề xuất cần thực hiện

Dùng tài liệu trong Backend làm nguồn đề xuất chính và xem danh sách này như phạm vi cần đối chiếu:

- BE-HOME-01: Tối ưu truy vấn suất chiếu mở bán cho QuickBooking.
- BE-HOME-02: Thống nhất điều kiện tạo reservation/hold, gồm suất quá giờ và trạng thái phòng/rạp.
- BE-HOME-03: `backdropUrl` nullable nếu có nguồn ảnh ngang chính thức.
- BE-HOME-04: API Movie catalog có paging/filter nếu quy mô cần; giữ nguyên API cũ để không phá client.
- BE-HOME-05: Availability ghế chỉ khi có nguồn dữ liệu đáng tin cậy.
- BE-HOME-06: Age rating chỉ khi có dữ liệu và quy ước nghiệp vụ xác nhận.
- BE-HOME-07: Rà soát validation `trailerUrl` hiện có.
- BE-HOME-08: Hero editorial metadata chỉ khi cần quy trình quản trị; carousel autoplay thuộc FE.
- BE-HOME-09: Promotion API chỉ khi có contract khuyến mãi thật.
- BE-HOME-10: Membership API chỉ khi có contract thành viên thật.
- BE-HOME-11: Thống nhất timezone cho ngày/giờ chiếu.
- BE-HOME-12: Kiểm tra CORS preflight tại Gateway và môi trường deploy.

Không mở lại R28 Notification đang được hoãn.

## Thứ tự làm việc

- Đọc tài liệu và source trước, sau đó bắt đầu round đầu tiên theo ưu tiên trong tài liệu.
- Nếu BE-HOME-01 trong tài liệu hiện tại đề xuất endpoint `GET /api/v1/showtimes/bookable`, hãy xác minh endpoint đó chưa tồn tại trước khi hướng dẫn thêm. Contract đề xuất cần được kiểm tra với code thực tế; không tự đổi tên route hoặc response.
- Với BE-HOME-02, nếu còn thiếu quyết định nghiệp vụ, trình bày những điểm cần chốt và tiếp tục các phần độc lập, không tự chọn chính sách có ảnh hưởng đến đặt vé.
- Không cho Movie Service truy cập trực tiếp database của Inventory Service.
- Không thay đổi contract đang dùng theo cách phá vỡ client hiện tại. Ưu tiên API additive và cập nhật OpenAPI/generated client theo quy trình repo.
- Không tự thêm promotion, membership, age rating, availability hay metadata không có nguồn dữ liệu chính thức.
- Phần thiết kế FE phải bám theo HTML/CSS trong `docs/design/reference/html-convert`. Không có Figma.

## Định dạng kết quả cho mỗi round

1. **Mục tiêu và trạng thái hiện tại:** ghi rõ điều gì đã có và điều gì còn thiếu, kèm đường dẫn file/source làm căn cứ.
2. **Các file tôi cần sửa:** liệt kê chính xác file Backend, và file Frontend nếu cần cập nhật contract hoặc client.
3. **Hướng dẫn code:** cung cấp từng thay đổi trong code block riêng, nói rõ thay thế hoặc chèn vào vị trí nào. Không đưa code giả định là code của repo.
4. **API contract:** nếu round có API, nêu method/path/query/body/response/error và ví dụ request/response. Phân biệt rõ contract hiện tại với contract mới đề xuất.
5. **Tác động và tương thích:** migration, security, caller hiện tại, generated client và các tình huống cần lưu ý.
6. **Kiểm tra thủ công:** chỉ nêu những bước cần thiết để tôi kiểm tra sau khi tự implement; không chạy test/build.
7. **Commit message:** đưa một commit message phù hợp cho round đó.

Bắt đầu bằng việc đọc tài liệu và source mới nhất, sau đó hướng dẫn round đầu tiên. Không yêu cầu tôi xác nhận trước nếu tài liệu và code đã đủ để bắt đầu.```
