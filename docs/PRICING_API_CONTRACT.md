# Contract giá vé hiện hành

Đối chiếu source BE tại commit:
635edb3557fc7400c55d99d7992d6bf472696c5c.

Tài liệu này mô tả contract đang có trong source.
Các tài liệu đề xuất hoặc mockup cũ không thay thế contract này.

## 1. Mô hình giá

- Mỗi rạp có các phiên bản policy.
- FARE_MATRIX_V2 dùng giá trọn gói được cấu hình trong FareRule.
- Không dùng basePrice của suất chiếu hoặc hệ số SeatType để tính toàn bảng giá.
- Không cộng phụ thu ghế, format hoặc thuộc tính phim.
- Ticket category độc lập với seat type.
- Nhiều category có thể cùng sử dụng STANDARD, VIP hoặc COUPLE.
- Mỗi experienceType/contentFormat có các fare riêng.
- Không tự kế thừa số tiền 2D sang 3D, 4DX hoặc IMAX.

## 2. Nhóm ngày và khung giờ

Nhóm ngày được cấu hình, không phải enum bảng giá cố định.

Ví dụ cấu hình phù hợp với bảng giá thông thường:

| Nhóm             | daysOfWeek                           | includesHolidays |
| ---------------- | ------------------------------------ | ---------------- |
| Thứ Hai–Thứ Năm  | MONDAY, TUESDAY, WEDNESDAY, THURSDAY | false            |
| Thứ Sáu–Chủ Nhật | FRIDAY, SATURDAY, SUNDAY             | false            |
| Chỉ lễ/Tết       | rỗng                                 | true             |

Các mã nhóm do policy khai báo; client không được giả định mã cố định.

Khung giờ dùng phút trong ngày, theo timezone snapshot của policy:

| Khoảng      | fromMinute | toMinute |
| ----------- | ---------: | -------: |
| Trước 17:00 |          0 |     1020 |
| Từ 17:00    |       1020 |     1440 |
| Cả ngày     |          0 |     1440 |

Khoảng giờ là [fromMinute, toMinute).
Đúng 17:00 thuộc khoảng bắt đầu tại 1020.
Một time band không đi qua nửa đêm.

Ngày lễ ưu tiên hơn thứ trong tuần:
nếu ngày địa phương có trong holidayDates,
chỉ nhóm includesHolidays=true được xét.

Chọn nhóm giá lễ không tự tạo lịch ngày lễ.
Ngày lễ cụ thể được quản lý riêng trong holiday-dates của policy.
Không tự suy ra lễ âm lịch hoặc lặp lại ngày lễ giữa các năm.

## 3. Ghế đôi

- Pricing seat type có thể mang tên SWEETBOX và map sang physical COUPLE.
- Một ShowSeat COUPLE có capacity=2.
- Giá được cấu hình theo PER_PAIR và tính một lần cho cả cặp.
- Không nhân amountVnd thêm với capacity.
- Giá ghế đơn trong luồng bán dùng PER_TICKET.
- Điều kiện ưu đãi cho hai người trong cặp cần được xác nhận riêng.

## 4. API quản trị đang tồn tại

Các route dưới đây tương đối với:
/api/v1/cinemas/{cinemaId}/pricing-policies/{policyId}

Tất cả yêu cầu inventory:manage.

| Method | Route                 | Chức năng                                 |
| ------ | --------------------- | ----------------------------------------- |
| GET    | /fare-matrix          | Đọc nhóm ngày, khung giờ, fare và lịch lễ |
| PUT    | /fare-matrix          | Thay toàn bộ nhóm ngày, khung giờ và fare |
| GET    | /holiday-dates        | Đọc lịch ngày lễ của policy               |
| PUT    | /holiday-dates        | Thay lịch ngày lễ của draft               |
| GET    | /sale-scopes          | Đọc phạm vi được phép bán                 |
| PUT    | /sale-scopes          | Thay phạm vi được phép bán của draft      |
| POST   | /fare-matrix/validate | Phân tích cấu hình và coverage toàn draft |
| POST   | /publish              | Kiểm tra lại và publish policy            |

Các catalog ticket category và seat type được quản lý riêng.

## 5. Contract lưu ma trận

PUT /fare-matrix nhận:

- expectedVersion: policy.version hiện tại.
- dayGroups: toàn bộ nhóm ngày cần giữ.
- timeBands: toàn bộ khung giờ cần giữ.
- fareRules: toàn bộ fare cần giữ.

Đây là full replacement, không phải cập nhật riêng những ô thay đổi.

Khi sửa một category hoặc một format trên giao diện,
request vẫn phải giữ các fare thuộc category/format khác.

Giới hạn request:

- Tối đa 100 day groups.
- Tối đa 100 time bands.
- Tối đa 2.000 fare rules.

Mỗi fare tham chiếu dayGroupCode và timeBandCode có trong request.
ticketCategoryCode và seatTypeCode phải nhất quán với catalog của policy
để policy đủ điều kiện sử dụng.

amountVnd là chuỗi số nguyên không âm:

- Chấp nhận "55000".
- Không chấp nhận JSON number 55000.
- Không chấp nhận "55.000", "55,000" hoặc "55k".
- API cấu hình hiện lưu tối đa Long.MAX_VALUE.
- Quote/booking còn kiểm tra giới hạn tổng tiền của luồng thanh toán.

Fare bắt buộc khai báo REGULAR_SCREENING hoặc ADVANCE_SCREENING.
ANY không được dùng cho fare; sale scope có thể dùng ANY.

PUT không thay holiday dates, ticket categories, seat types hoặc sale scopes.
Các tham chiếu trong cấu hình được giữ lại phải tiếp tục nhất quán.

Response là CinemaPricingPolicyResponse với optimistic version mới.
Không phải CinemaFareMatrixResponse.

ID của fare và dimensions được tạo lại khi thay ma trận.
Sau khi lưu, client cập nhật version và tải lại dữ liệu.
Không dùng fare ID cũ cho thao tác tiếp theo.

HTTP 409: tải lại policy/ma trận và xử lý thay đổi đồng thời.
Không tự gửi lại payload cũ với version mới.

Lưu draft chưa xác nhận coverage hoặc quyền publish.
COMPLETE của validate chỉ nói phân tích đã hoàn tất;
phải đọc matrixValid, unitPricingValid và các findings.

Publish chạy lại kiểm tra dưới lock.
HTTP 422 có thể trả published=false và validation details.
Policy đã publish không được sửa qua các API cập nhật draft.

## 6. Version và hiệu lực

- policy.version: optimistic version dùng trong expectedVersion.
- policy.policyVersion: phiên bản nghiệp vụ của chính sách.
- Khoảng ngày hiệu lực là [effectiveFrom, effectiveTo).
- Ngày hiệu lực được diễn giải bằng timezone snapshot của policy.
- Không chọn policy chỉ vì policyVersion lớn nhất.
- Không tự thay policy/layout đã gắn vào suất chiếu.

## 7. Quy tắc category

- Policy hoàn chỉnh có đúng một category REGULAR, UNRESTRICTED.
- Premium Cinema chuyển CATEGORY_DISCOUNT về REGULAR.
- C_MONDAY, C_MEMBER, C_TEN chuyển về REGULAR khi là suất chiếu sớm.
- Suất chiếu sớm là ngày bắt đầu theo timezone policy
  trước ngày công chiếu chính thức đã snapshot từ Movie Service.
- Không xác định suất chiếu sớm bằng thứ tự suất trong ngày.
- Fare REGULAR không yêu cầu eligibility code.
- Điều kiện ưu đãi thực tế phải có verifier đáng tin cậy.
- Thiếu verifier không được coi là khách hàng đã đủ điều kiện.

## 8. Quote và booking đang tồn tại

| Method | Route                            | Chức năng                                  |
| ------ | -------------------------------- | ------------------------------------------ |
| POST   | /api/v1/bookings/quote           | Tạo báo giá theo ghế và loại vé            |
| GET    | /api/v1/bookings/quote/{quoteId} | Đọc quote còn hạn của người hiện tại       |
| POST   | /api/v1/bookings/from-quote      | Tạo booking từ quote                       |
| GET    | /api/v1/bookings/{bookingId}     | Đọc booking và snapshot giá của chủ sở hữu |

Quote request dùng showSeatId và ticketCategoryCode.
Không dùng seatId vật lý thay cho showSeatId.

Tạo booking từ quote chỉ nhận clientRequestId và quoteId.
Backend lấy ghế và giá từ quote đã lưu.
202 không có nghĩa đã giữ ghế thành công.

BookingPricingResponse chỉ được tạo từ snapshot đã chốt.
Không tính lại booking lịch sử bằng policy hiện hành.
Các evidence references nội bộ không được trả trong booking response.

## 9. Phạm vi chưa hoàn tất

- Chưa có các implementation xác minh ưu đãi thực tế.
- Điều kiện vé ưu đãi cho người đi khác người đặt chưa được chốt.
- Điều kiện ưu đãi theo cặp chưa được chốt.
- Chưa chạy kiểm chứng toàn luồng trong các round hướng dẫn này.
- Không triển khai chức năng hoàn vé trong phạm vi hiện tại.

## 10. Dữ liệu giá

Không seed giá tham khảo vào production bằng enum, service hoặc migration.
Giá từ ảnh CGV là dữ liệu tham khảo, cần xác nhận trước khi publish.
