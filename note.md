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

## run codex

```text
codex.cmd -C "E:\CinemaWorkspace\Cinema-web" --sandbox workspace-write --ask-for-approval on-request
```

Đọc các file sau trước khi làm việc:

- AGENTS.md
- docs/CURRENT_STATUS.md
- docs/architecture.md
- README.md
- ../cinema-web-ai-context-be-contracts.md
- mã nguồn backend tại ../cinema-system

Quyền làm việc:

- Được phép chỉnh sửa file bên trong repository Cinema-web.
- Repository ../cinema-system chỉ được phép đọc.
- Không được sửa, commit hoặc push repository cinema-system.
- Không được commit hoặc push Cinema-web nếu tôi chưa yêu cầu rõ ràng.
- Không được cài dependency mới nếu tôi chưa đồng ý.

Quy trình bắt buộc:

1. Kiểm tra trạng thái source và tài liệu hiện tại.
2. Trước khi sửa, trình bày:
   - mục tiêu;
   - danh sách file dự kiến sửa;
   - nội dung thay đổi dự kiến.
3. Dừng lại và chờ tôi trả lời "approve".
4. Chỉ sau khi tôi approve mới được chỉnh sửa file.
5. Không chạy test, lint, type-check hoặc build.
6. Sau khi sửa, hiển thị:
   - danh sách file đã thay đổi;
   - tóm tắt thay đổi;
   - git diff liên quan;
   - các bước kiểm tra thủ công đề xuất.
7. Không tự động commit.
8. Chỉ commit khi tôi yêu cầu rõ ràng.

codex.cmd resume --last --sandbox workspace-write --ask-for-approval on-request

Tiếp tục công việc từ phiên trước.

Chỉ kiểm tra:

- git status;
- commit hiện tại;
- docs/CURRENT_STATUS.md;
- các file liên quan trực tiếp đến task đang làm.

Không cần đọc lại toàn bộ repository.
Không chạy test, lint, type-check hoặc build.

Trước khi chỉnh sửa:

1. Tóm tắt trạng thái hiện tại.
2. Đề xuất danh sách file cần sửa.
3. Dừng lại và chờ tôi trả lời "approve".

Backend ../cinema-system chỉ được phép đọc.
Không commit hoặc push nếu tôi chưa yêu cầu.
