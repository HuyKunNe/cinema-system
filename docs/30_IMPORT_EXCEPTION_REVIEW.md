# Rà soát import và exception Backend

Ngày: 2026-11-17. Phạm vi: main/test Java trong common, services và infrastructure; bỏ build/generated.

## Kết quả import

- Quét 1.265 file Java; chuẩn hóa import trong 75 file.
- Thay 173 tham chiếu package đầy đủ bằng import cụ thể; loại 10 wildcard import, gồm static import trong test.
- Rà lexical bỏ qua comment/string/text block: không còn FQN trong code hoặc wildcard import. Giữ tên lớp trong cấu hình, chuỗi runtime, Javadoc và assertion string.
- Không đổi logic, dependency, API, migration, SQL hoặc annotation semantics. Giữ các thay đổi chưa commit có sẵn; test source chỉ sửa import/tên tham chiếu.

| Module | File thay import |
| --- | ---: |
| common/common-jackson | 1 |
| common/common-outbox | 2 |
| common/common-security | 1 |
| common/common-storage | 1 |
| services/booking-service | 27 |
| services/inventory-service | 18 |
| services/movie-service | 1 |
| services/payment-service | 10 |
| services/user-service | 14 |

## Exception: giữ nguyên HTTP và response contract

Đã đọc common-exception (ErrorCode, ErrorCategory, BusinessException và các subtype), common-api/GlobalExceptionHandler, error code của các service và wrapper batch Inventory. Các subtype Validation/Conflict/NotFound/Unauthorized/Forbidden/Locked/InternalServer/ServiceUnavailable có mapping riêng. Exception Java chung đi qua fallback HTTP 500, code INTERNAL_SERVER_ERROR, message Unexpected error, category null.

Không đổi exception chỉ vì tên IllegalArgumentException hoặc IllegalStateException. Các guard vé dưới đây chưa có ErrorCode giữ nguyên toàn bộ contract. Không tự tạo code hoặc đổi lỗi 500 thành 400/409 trong lượt này.

| Vị trí trong BookingTicket.java | Ý nghĩa / quyết định |
| --- | --- |
| checkIn: Ticket is not issued | Tiền điều kiện trạng thái. Service hiện trả ALREADY_USED trước khi gọi domain method. Nếu muốn lỗi nghiệp vụ riêng, cần duyệt code/message/status rồi dùng ConflictException có sẵn. |
| constructor: Ticket requires completed seat snapshot | Snapshot lưu trữ không nhất quán; không mặc định là lỗi input khách. RESERVATION_RESULT_MISMATCH có sẵn nhưng dùng nó sẽ đổi code/message/status hiện tại. Cần duyệt mapping invariant nội bộ. |
| capacityFor: Unsupported ticket seat type | Loại ghế đọc từ snapshot DB. Đề xuất code SYSTEM trong BookingErrorCode cùng InternalServerException sau khi duyệt contract; không tự đổi sang lỗi request 400. |

Common-exception có subtype phù hợp cho các mapping mới, nhưng CommonErrorCode hiện chỉ có RESOURCE_LOCKED, không có code tương ứng chính xác fallback ở trên. Common mapping dùng category của ErrorCode nên cũng không giữ nguyên category null nếu tự thêm code SYSTEM. Do yêu cầu bảo toàn contract, chưa sửa ba guard này; không thêm hệ thống exception song song.

## Nhóm generic exception giữ vì thuộc kỹ thuật

- common-search/common-storage: hợp đồng thư viện, object name/traversal, stream, sort/pagination; không gán thành lỗi nghiệp vụ điện ảnh.
- common-jackson/common-kafka và ShowtimeBatchPayloadCodec: serialization, fingerprint, thuật toán. Không đổi lỗi kỹ thuật thành business error.
- User JWT/security audit: invariant principal/context/UUID. Bootstrap, issuer/JWK, Google, OutboxProperties, cấu hình timeout và StorageAutoConfiguration: fail-fast cấu hình.
- Utility/constant constructors: ngăn khởi tạo. Test fixture: ký JWT, JWK server, latch và mô phỏng lỗi; giữ ngữ nghĩa fixture.
- ShowtimeBatchItemException bọc BusinessException gốc và itemIndex phục vụ handler batch hiện tại; giữ wrapper để bảo toàn response theo từng item.

Không thay logic exception trong lượt này khi chưa có mapping bảo toàn contract. Không thêm dependency common-exception vào utility chỉ để đổi tên exception kỹ thuật.

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

