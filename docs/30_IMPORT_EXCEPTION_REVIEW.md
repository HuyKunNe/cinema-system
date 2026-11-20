# Rà soát import và exception Backend

Ngày: 2026-11-17. Phạm vi: main/test Java trong common, services và infrastructure; bỏ build/generated.

## Tiếp tục ngày 2026-11-20

- Lượt tiếp theo, Git status sạch: BookingTicketService giữ cause của lỗi giải mã Base64 bằng ValidationException(ErrorCode, Throwable) có sẵn. Giữ HTTP 400, BOOKING_TICKET_QR_INVALID, message và category hiện tại; không trả cause trong ErrorBody.
- Rà các catch trực tiếp ném shared exception với một ErrorCode trong services: còn hai catch URISyntaxException ở MovieServiceImpl. Giữ nguyên vì thông điệp nguyên nhân có thể chứa URL đầu vào; quy tắc giữ cause chỉ áp dụng khi an toàn và hữu ích.
- Kiểm tra lượt QR: Maven offline `-pl services/booking-service -am -DskipTests compile` BUILD SUCCESS cho 17 module. Không chạy test hoặc ghi DB; git diff --check không có lỗi whitespace.

- Git status sạch khi bắt đầu. Đọc lại hướng dẫn exception, constructor của common-exception và GlobalExceptionHandler trước khi sửa.
- TicketCheckInService: khi RestClientException xảy ra trong bước đọc lịch chiếu, truyền nguyên nhân gốc vào constructor InternalServerException(ErrorCode, Throwable) sẵn có. Tuân theo quy tắc giữ cause tại boundary; không thêm exception hoặc error code.
- Giữ HTTP 500, code BOOKING_TICKET_SCHEDULE_UNAVAILABLE, message và category SYSTEM. Handler tạo response từ ErrorCode, không đưa cause vào ErrorBody.
- Ba guard BookingTicket và các ứng viên generic bên dưới vẫn chưa đổi: chưa có mapping được duyệt đáp ứng yêu cầu giữ nguyên contract. Không coi phần exception đã hoàn tất.
- Kiểm tra: `mvn -o "-Dmaven.repo.local=C:/Users/Administrator/.m2/repository" -pl services/booking-service -am -DskipTests compile`. Lần đầu lỗi compiler khi truy cập JAR eureka-client trong cache; chạy lại ngoài sandbox BUILD SUCCESS cho 17 module. Chưa xác định chắc nguyên nhân lỗi lần đầu. Không chạy test, API hoặc ghi DB. `git diff --check` không có lỗi whitespace.

## Căn cứ và lượt rà soát tiếp theo

- AGENTS.md: giữ thay đổi hiện có, không đổi nghiệp vụ/contract, không chạy test mặc định.
- docs/05_CODING_CONVENTIONS.md, phần exception và review checklist: lỗi dự kiến của ứng dụng, domain, cấu hình, serialization, search, storage và security phải theo common-exception. Generic exception trong production là ứng viên cần chuyển đổi, không phải mẫu được phép tiếp tục áp dụng.
- README.md, docs/02_ARCHITECTURE.md và POM: giữ ranh giới module/service và dependency hiện tại.
- .editorconfig yêu cầu UTF-8, LF, Java thụt 4 spaces. .vscode/settings.json chọn Google Java Format kiểu AOSP; không tự organize imports khi save. POM có property phiên bản Checkstyle/Spotless nhưng chưa có plugin kiểm tra tương ứng được kích hoạt. Không áp thêm cấu hình formatter bên ngoài.
- Lượt rà soát tiếp theo bắt đầu khi kết quả chuẩn hóa import bên dưới đã có trong checkout. Quét lại 1.265 file không thấy FQN trong token code, wildcard hoặc import không dùng theo kiểm tra tham chiếu tên. Không sửa thêm Java trong lượt này; giữ nguyên toàn bộ thay đổi đã có.

## Kết quả import

- Quét 1.265 file Java; chuẩn hóa import trong 75 file.
- Thay 173 tham chiếu package đầy đủ bằng import cụ thể; loại 10 wildcard import, gồm static import trong test.
- Rà lexical bỏ qua comment/string/text block: không còn FQN trong code hoặc wildcard import. Giữ tên lớp trong cấu hình, chuỗi runtime, Javadoc và assertion string.
- Không đổi logic, dependency, API, migration, SQL hoặc annotation semantics. Giữ các thay đổi chưa commit có sẵn; test source chỉ sửa import/tên tham chiếu.

| Module                     | File thay import |
| -------------------------- | ---------------: |
| common/common-jackson      |                1 |
| common/common-outbox       |                2 |
| common/common-security     |                1 |
| common/common-storage      |                1 |
| services/booking-service   |               27 |
| services/inventory-service |               18 |
| services/movie-service     |                1 |
| services/payment-service   |               10 |
| services/user-service      |               14 |

## Exception: giữ nguyên HTTP và response contract

Đã đọc common-exception (ErrorCode, ErrorCategory, BusinessException và các subtype), common-api/GlobalExceptionHandler, error code của các service và wrapper batch Inventory. Các subtype Validation/Conflict/NotFound/Unauthorized/Forbidden/Locked/InternalServer/ServiceUnavailable có mapping riêng. Exception Java chung đi qua fallback HTTP 500, code INTERNAL_SERVER_ERROR, message Unexpected error, category null.

Không đổi exception chỉ vì tên IllegalArgumentException hoặc IllegalStateException. Các guard vé dưới đây chưa có ErrorCode giữ nguyên toàn bộ contract. Không tự tạo code hoặc đổi lỗi 500 thành 400/409 trong lượt này.

| Vị trí trong BookingTicket.java                      | Ý nghĩa / quyết định                                                                                                                                                                           |
| ---------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| checkIn: Ticket is not issued                        | Tiền điều kiện trạng thái. Service hiện trả ALREADY_USED trước khi gọi domain method. Nếu muốn lỗi nghiệp vụ riêng, cần duyệt code/message/status rồi dùng ConflictException có sẵn.           |
| constructor: Ticket requires completed seat snapshot | Snapshot lưu trữ không nhất quán; không mặc định là lỗi input khách. RESERVATION_RESULT_MISMATCH có sẵn nhưng dùng nó sẽ đổi code/message/status hiện tại. Cần duyệt mapping invariant nội bộ. |
| capacityFor: Unsupported ticket seat type            | Loại ghế đọc từ snapshot DB. Đề xuất code SYSTEM trong BookingErrorCode cùng InternalServerException sau khi duyệt contract; không tự đổi sang lỗi request 400.                                |

Common-exception có subtype phù hợp cho các mapping mới, nhưng CommonErrorCode hiện chỉ có RESOURCE_LOCKED, không có code tương ứng chính xác fallback ở trên. Common mapping dùng category của ErrorCode nên cũng không giữ nguyên category null nếu tự thêm code SYSTEM. Do yêu cầu bảo toàn contract, chưa sửa ba guard này; không thêm hệ thống exception song song.

## Generic exception còn cần thống nhất mapping

- common-search/common-storage: validation hợp đồng thư viện, object name/traversal, stream, sort/pagination là ứng viên chuyển đổi theo coding conventions. StorageException và SearchException hiện là RuntimeException riêng; không mặc định chúng đã tuân theo common-exception.
- common-jackson/common-kafka và ShowtimeBatchPayloadCodec: serialization/fingerprint cũng cần mapping tại boundary sở hữu lỗi. Cần thống nhất mã lỗi, thông điệp và cách giữ cause; không tự gán lỗi input khách.
- User JWT/security audit: invariant principal/context/UUID; bootstrap, issuer/JWK, Google, OutboxProperties, cấu hình timeout, StorageAutoConfiguration và tracing: cần phân biệt lỗi dự kiến với lỗi lập trình/cấu hình khởi động. Không coi chúng hợp lệ chỉ vì thuộc kỹ thuật.
- Utility/constant constructor ngăn khởi tạo và test fixture ký JWT/JWK/latch/mô phỏng lỗi được giữ nguyên; chúng không phải response lỗi nghiệp vụ.
- ShowtimeBatchItemException bọc BusinessException gốc và itemIndex phục vụ handler batch hiện tại; giữ wrapper để bảo toàn response theo từng item. Guard itemIndex của wrapper vẫn nằm trong danh sách rà soát.

Không thay logic exception trong lượt này khi chưa có mapping bảo toàn contract. CommonErrorCode chỉ có RESOURCE_LOCKED; chưa có bộ mã chung biểu diễn chính xác các trường hợp trên. Việc dùng subtype có sẵn với một mã gần nghĩa có thể đổi status/code/message/category. Cần duyệt mapping theo boundary trước khi sửa; không tự thêm dependency hoặc mã lỗi. Danh sách vị trí cụ thể nằm cuối tài liệu. Một throw có thể đã được catch/translate trước khi ra API; không suy ra mọi throw đều trả HTTP 500.

## Kiểm tra

Lần biên dịch lại sau sắp xếp import cuối cùng cũng BUILD SUCCESS cho 29 module.
Đối chiếu 70 file tracked vốn sạch trước lượt này với HEAD: không phát hiện
khác biệt phần thân ngoài rút gọn tên type/import và khoảng trắng. Các file đã
có thay đổi trước đó không dùng HEAD để suy ra baseline, tránh ghi đè công việc
đang làm. Bộ quét cuối không còn wildcard hoặc FQN trong token code.

Đã chạy Maven offline tại root với -DskipTests test-compile: BUILD SUCCESS cho 29 module, gồm main/test source. Không thực thi test case, Surefire hoặc Failsafe. Có warning unchecked trong Kafka configuration tests và deprecated AntPathRequestMatcher; không sửa ngoài phạm vi.

Đã quét lại FQN/wildcard và kiểm tra git diff --check. Không khởi động service, gọi API, ghi DB/migrate, sửa FE hoặc commit/push.

## File Java được chuẩn hóa

- common/common-jackson/src/test/java/com/cinema/common/jackson/serialization/UnknownPropertyTest.java
- common/common-outbox/src/test/java/com/cinema/common/outbox/claim/DefaultOutboxClaimServiceTest.java
- common/common-outbox/src/test/java/com/cinema/common/outbox/publisher/KafkaOutboxPublisherTest.java
- common/common-security/src/test/java/com/cinema/common/security/context/SecurityContextUtilsTest.java
- common/common-storage/src/test/java/com/cinema/common/storage/minio/MinioStorageServiceTest.java
- services/booking-service/src/main/java/com/cinema/booking/controller/BookingTicketCheckInController.java
- services/booking-service/src/main/java/com/cinema/booking/controller/BookingTicketController.java
- services/booking-service/src/main/java/com/cinema/booking/controller/BookingTicketLookupController.java
- services/booking-service/src/main/java/com/cinema/booking/controller/BookingTicketRecoveryController.java
- services/booking-service/src/main/java/com/cinema/booking/controller/ShowtimeEndBackfillController.java
- services/booking-service/src/main/java/com/cinema/booking/entity/Booking.java
- services/booking-service/src/main/java/com/cinema/booking/entity/BookingTicket.java
- services/booking-service/src/main/java/com/cinema/booking/repository/BookingTicketRepository.java
- services/booking-service/src/main/java/com/cinema/booking/service/BookingTicketService.java
- services/booking-service/src/main/java/com/cinema/booking/service/ShowtimeEndBackfillService.java
- services/booking-service/src/main/java/com/cinema/booking/service/TicketCheckInService.java
- services/booking-service/src/main/java/com/cinema/booking/event/listener/BookingTicketKafkaListener.java
- services/booking-service/src/main/java/com/cinema/booking/event/validation/BookingTicketSnapshotValidator.java
- services/booking-service/src/main/java/com/cinema/booking/service/impl/BookingServiceImpl.java
- services/booking-service/src/main/java/com/cinema/booking/service/impl/SeatReservedConsumerServiceImpl.java
- services/booking-service/src/test/java/com/cinema/booking/event/BookingLifecycleOutboxFactoryTest.java
- services/booking-service/src/test/java/com/cinema/booking/event/DefaultBookingConfirmedOutboxFactoryTest.java
- services/booking-service/src/test/java/com/cinema/booking/event/DefaultSeatReservationRequestedOutboxFactoryTest.java
- services/booking-service/src/test/java/com/cinema/booking/integration/ReservationResultRaceIntegrationTest.java
- services/booking-service/src/test/java/com/cinema/booking/outbox/BookingOutboxTransactionIntegrationTest.java
- services/booking-service/src/test/java/com/cinema/booking/outbox/SeatReservationRequestedPublicationIntegrationTest.java
- services/booking-service/src/test/java/com/cinema/booking/event/serialization/JacksonSeatReservedPayloadReaderTest.java
- services/booking-service/src/test/java/com/cinema/booking/service/impl/BookingCancellationServiceImplTest.java
- services/booking-service/src/test/java/com/cinema/booking/service/impl/BookingCreationServiceImplTest.java
- services/booking-service/src/test/java/com/cinema/booking/service/impl/BookingServiceImplTest.java
- services/booking-service/src/test/java/com/cinema/booking/service/impl/PaymentFailedConsumerServiceImplTest.java
- services/booking-service/src/test/java/com/cinema/booking/service/impl/SeatReservedConsumerServiceImplTest.java
- services/inventory-service/src/main/java/com/cinema/inventory/event/DefaultSeatReservedOutboxFactory.java
- services/inventory-service/src/main/java/com/cinema/inventory/event/validation/DefaultSeatReservationRequestedMessageValidator.java
- services/inventory-service/src/main/java/com/cinema/inventory/service/impl/SeatReservationRequestedConsumerServiceImpl.java
- services/inventory-service/src/test/java/com/cinema/inventory/controller/InventoryServiceTokenSecurityTest.java
- services/inventory-service/src/test/java/com/cinema/inventory/controller/SeatControllerTest.java
- services/inventory-service/src/test/java/com/cinema/inventory/controller/ShowSeatControllerTest.java
- services/inventory-service/src/test/java/com/cinema/inventory/controller/ShowSeatSecurityTest.java
- services/inventory-service/src/test/java/com/cinema/inventory/controller/ShowtimeControllerTest.java
- services/inventory-service/src/test/java/com/cinema/inventory/event/DefaultSeatReservationRejectedOutboxFactoryTest.java
- services/inventory-service/src/test/java/com/cinema/inventory/event/DefaultSeatReservedOutboxFactoryTest.java
- services/inventory-service/src/test/java/com/cinema/inventory/integration/BookingLifecycleReleaseKafkaIntegrationTest.java
- services/inventory-service/src/test/java/com/cinema/inventory/integration/SeatReleaseRequestedKafkaIntegrationTest.java
- services/inventory-service/src/test/java/com/cinema/inventory/integration/ShowSeatConcurrencyIntegrationTest.java
- services/inventory-service/src/test/java/com/cinema/inventory/service/InventoryDraftDeletionServiceTest.java
- services/inventory-service/src/test/java/com/cinema/inventory/service/PublishedVersionLifecycleTest.java
- services/inventory-service/src/test/java/com/cinema/inventory/event/serialization/JacksonBookingConfirmedPayloadReaderTest.java
- services/inventory-service/src/test/java/com/cinema/inventory/service/impl/RoomServiceImplTest.java
- services/inventory-service/src/test/java/com/cinema/inventory/service/impl/ShowtimeServiceImplTest.java
- services/movie-service/src/main/java/com/cinema/movie/repository/MovieRepository.java
- services/payment-service/src/test/java/com/cinema/payment/controller/PaymentProviderWebhookControllerTest.java
- services/payment-service/src/test/java/com/cinema/payment/provider/PaymentProviderOperationMySqlIntegrationTest.java
- services/payment-service/src/test/java/com/cinema/payment/service/RefundPaymentProviderOperationWorkerMySqlIntegrationTest.java
- services/payment-service/src/test/java/com/cinema/payment/service/RefundProviderResultApplicationMySqlIntegrationTest.java
- services/payment-service/src/test/java/com/cinema/payment/service/RefundProviderResultApplicationRollbackIntegrationTest.java
- services/payment-service/src/test/java/com/cinema/payment/provider/webhook/PaymentProviderWebhookHttpIntegrationTest.java
- services/payment-service/src/test/java/com/cinema/payment/service/impl/FinancialAuditReadServiceImplTest.java
- services/payment-service/src/test/java/com/cinema/payment/service/impl/PaymentResultOutboxServiceImplTest.java
- services/payment-service/src/test/java/com/cinema/payment/service/impl/PaymentTransactionClaimServiceImplTest.java
- services/payment-service/src/test/java/com/cinema/payment/service/impl/RefundPaymentTransactionClaimServiceImplTest.java
- services/user-service/src/main/java/com/cinema/user/config/AuthorizationServerConfiguration.java
- services/user-service/src/main/java/com/cinema/user/controller/CurrentUserController.java
- services/user-service/src/main/java/com/cinema/user/security/GoogleLoginSuccessHandler.java
- services/user-service/src/main/java/com/cinema/user/oauth2/token/RefreshTokenTrackingServiceImpl.java
- services/user-service/src/test/java/com/cinema/user/config/AuthorizationServerConfigurationTest.java
- services/user-service/src/test/java/com/cinema/user/config/AuthorizationServerSecurityConfigurationTest.java
- services/user-service/src/test/java/com/cinema/user/oauth2/AuthorizationCodePkceTokenIntegrationTest.java
- services/user-service/src/test/java/com/cinema/user/oauth2/ClientCredentialsTokenIntegrationTest.java
- services/user-service/src/test/java/com/cinema/user/oauth2/JwkSetEndpointIntegrationTest.java
- services/user-service/src/test/java/com/cinema/user/oauth2/OAuth2ProtocolPolicyIntegrationTest.java
- services/user-service/src/test/java/com/cinema/user/oauth2/RegisteredClientFactoryImplTest.java
- services/user-service/src/test/java/com/cinema/user/oauth2/impl/AuthorizationSessionRevocationServiceImplTest.java
- services/user-service/src/test/java/com/cinema/user/oauth2/impl/OAuth2ClientRegistrationServiceImplTest.java
- services/user-service/src/test/java/com/cinema/user/oauth2/token/RefreshTokenReuseServiceImplTest.java

## Inventory generic throw trong production

Danh sách dưới đây là điểm cần phân loại/mapping, chưa phải kết luận tất cả là lỗi nghiệp vụ. Loại constructor utility và common-test khỏi bảng. Giữ nguyên các throw cho đến khi xác định mapping tương thích tại boundary.

| File                                                                                                      | Dòng | Exception                |
| --------------------------------------------------------------------------------------------------------- | ---: | ------------------------ |
| common/common-jackson/src/main/java/com/cinema/common/jackson/util/JsonUtils.java                         |   19 | IllegalArgumentException |
| common/common-jackson/src/main/java/com/cinema/common/jackson/util/JsonUtils.java                         |   27 | IllegalArgumentException |
| common/common-kafka/src/main/java/com/cinema/common/kafka/serializer/KafkaEventSerializer.java            |   22 | IllegalStateException    |
| common/common-outbox/src/main/java/com/cinema/common/outbox/config/OutboxProperties.java                  |   21 | IllegalStateException    |
| common/common-search/src/main/java/com/cinema/common/search/model/SearchPage.java                         |   27 | IllegalArgumentException |
| common/common-search/src/main/java/com/cinema/common/search/model/SearchPage.java                         |   34 | IllegalArgumentException |
| common/common-search/src/main/java/com/cinema/common/search/model/SearchPage.java                         |   41 | IllegalArgumentException |
| common/common-search/src/main/java/com/cinema/common/search/model/SearchPage.java                         |   48 | IllegalArgumentException |
| common/common-search/src/main/java/com/cinema/common/search/model/SearchQuery.java                        |   42 | IllegalArgumentException |
| common/common-search/src/main/java/com/cinema/common/search/model/SearchQuery.java                        |   49 | IllegalArgumentException |
| common/common-search/src/main/java/com/cinema/common/search/model/SearchQuery.java                        |   56 | IllegalArgumentException |
| common/common-search/src/main/java/com/cinema/common/search/model/SearchSort.java                         |   17 | IllegalArgumentException |
| common/common-search/src/main/java/com/cinema/common/search/service/DefaultSearchService.java             |   53 | IllegalArgumentException |
| common/common-search/src/main/java/com/cinema/common/search/service/DefaultSearchService.java             |  131 | IllegalArgumentException |
| common/common-storage/src/main/java/com/cinema/common/storage/config/StorageAutoConfiguration.java        |  112 | IllegalStateException    |
| common/common-storage/src/main/java/com/cinema/common/storage/minio/MinioStorageService.java              |  274 | IllegalArgumentException |
| common/common-storage/src/main/java/com/cinema/common/storage/minio/MinioStorageService.java              |  339 | IllegalArgumentException |
| common/common-storage/src/main/java/com/cinema/common/storage/minio/MinioStorageService.java              |  355 | IllegalArgumentException |
| common/common-storage/src/main/java/com/cinema/common/storage/minio/MinioStorageService.java              |  363 | IllegalArgumentException |
| common/common-storage/src/main/java/com/cinema/common/storage/minio/MinioStorageService.java              |  379 | IllegalArgumentException |
| common/common-storage/src/main/java/com/cinema/common/storage/model/StorageDownload.java                  |   16 | IllegalArgumentException |
| common/common-storage/src/main/java/com/cinema/common/storage/model/StorageDownload.java                  |   23 | IllegalArgumentException |
| common/common-storage/src/main/java/com/cinema/common/storage/model/StorageUploadRequest.java             |   18 | IllegalArgumentException |
| common/common-storage/src/main/java/com/cinema/common/storage/model/StorageUploadRequest.java             |   25 | IllegalArgumentException |
| common/common-storage/src/main/java/com/cinema/common/storage/model/StorageUploadRequest.java             |   32 | IllegalArgumentException |
| common/common-tracing/src/main/java/com/cinema/common/tracing/service/DefaultTracingService.java          |  167 | IllegalArgumentException |
| services/booking-service/src/main/java/com/cinema/booking/entity/BookingTicket.java                       |   76 | IllegalStateException    |
| services/booking-service/src/main/java/com/cinema/booking/entity/BookingTicket.java                       |  100 | IllegalArgumentException |
| services/booking-service/src/main/java/com/cinema/booking/entity/BookingTicket.java                       |  114 | IllegalArgumentException |
| services/inventory-service/src/main/java/com/cinema/inventory/config/MovieCatalogClientConfiguration.java |   35 | IllegalArgumentException |
| services/inventory-service/src/main/java/com/cinema/inventory/config/MovieCatalogClientConfiguration.java |   63 | IllegalArgumentException |
| services/inventory-service/src/main/java/com/cinema/inventory/exception/ShowtimeBatchItemException.java   |   16 | IllegalArgumentException |
| services/inventory-service/src/main/java/com/cinema/inventory/service/ShowtimeBatchPayloadCodec.java      |   61 | IllegalStateException    |
| services/user-service/src/main/java/com/cinema/user/bootstrap/InventoryMovieClientBootstrap.java          |   64 | IllegalStateException    |
| services/user-service/src/main/java/com/cinema/user/bootstrap/LocalBootstrapRunner.java                   |  250 | IllegalStateException    |
| services/user-service/src/main/java/com/cinema/user/bootstrap/LocalBootstrapRunner.java                   |  262 | IllegalStateException    |
| services/user-service/src/main/java/com/cinema/user/config/AuthorizationServerSecurityConfiguration.java  |  244 | IllegalStateException    |
| services/user-service/src/main/java/com/cinema/user/config/AuthorizationServerSecurityConfiguration.java  |  247 | IllegalStateException    |
| services/user-service/src/main/java/com/cinema/user/config/GoogleLoginConfiguration.java                  |   21 | IllegalStateException    |
| services/user-service/src/main/java/com/cinema/user/security/jwt/CinemaJwtTokenCustomizer.java            |   61 | IllegalStateException    |
| services/user-service/src/main/java/com/cinema/user/security/jwt/CinemaJwtTokenCustomizer.java            |   68 | IllegalStateException    |
| services/user-service/src/main/java/com/cinema/user/security/audit/impl/SecurityAuditRecorderImpl.java    |   45 | IllegalArgumentException |
| services/user-service/src/main/java/com/cinema/user/security/audit/impl/SecurityAuditRecorderImpl.java    |   52 | IllegalStateException    |

Lượt kiểm tra tiếp theo: Maven offline với `-DskipTests test-compile` kết thúc mã 0; BUILD SUCCESS cho 29 module. Không chạy test. `git diff --check` không báo lỗi. Không đổi Java, API, MySQL, migration, dependency hoặc FE trong lượt kiểm tra tiếp theo.
