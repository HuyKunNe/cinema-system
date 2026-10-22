# Đề xuất BE: chính sách giá vé theo ma trận rạp, định dạng và nhóm vé

> Bản thay thế cho đề xuất giá suất chiếu cũ. Repository BE chỉ đọc; hãy tự chép nội dung đã duyệt vào tài liệu tương ứng trong repo `cinema-system`.
>
> Ảnh tham khảo nhận diện thương hiệu CGV. Giá dưới đây chỉ mô tả dữ liệu đọc từ ảnh, không mặc định là giá áp dụng cho mọi rạp của dự án. Cần nghiệp vụ xác nhận rạp, chi nhánh, ngày hiệu lực và điều kiện trước khi nhập dữ liệu.

## 1. Mục tiêu và quyết định mô hình

Tách giá vé thành các chiều độc lập để biểu diễn được nhiều bảng giá cùng lúc:

| Chiều                   | Ví dụ                                                                         | Nguồn dữ liệu / ý nghĩa                                                                             |
| ----------------------- | ----------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------- |
| Rạp và chính sách       | `cinemaId`, policy version                                                    | Chủ sở hữu bảng giá và lịch sử thay đổi                                                             |
| Loại trải nghiệm        | `STANDARD`, `PREMIUM_CINEMA`, `4DX`, `IMAX`                                   | Năng lực phòng/loại suất chiếu; không phải loại ghế                                                 |
| Phiên bản nội dung      | `2D`, `3D`                                                                    | Ghép với loại trải nghiệm thành nhãn như `STANDARD · 3D`; cần riêng cho 4DX, IMAX và Premium Cinema |
| Nhóm ngày               | `MON_TUE_THU`, `WEDNESDAY`, `FRI_SAT_SUN_HOLIDAY`                             | Ngày địa phương tại rạp; ngày lễ dùng lịch được cấu hình                                            |
| Khung giờ / cờ suất đầu | `BEFORE_12`, `12_17`, `17_21_30`, `AFTER_21_30`, `FIRST_SHOWING`, `BEFORE_17` | Khung giờ tính theo timezone của rạp; `FIRST_SHOWING` là thuộc tính của suất chiếu                  |
| Nhóm vé                 | `ADULT`, `CHILD`, `CGV_YOUNG`, `STUDENT`, `SENIOR`                            | Nhóm người mua/điều kiện ưu đãi; không phải loại ghế                                                |
| Loại ghế                | `STANDARD`, `VIP`, `SWEETBOX`                                                 | Lấy từ sơ đồ ghế đã xuất bản của phòng                                                              |
| Điều kiện ưu đãi        | thành viên, tuổi, giấy tờ, giờ áp dụng                                        | Phải được xác minh bằng nghiệp vụ; không suy ra chỉ từ lựa chọn FE                                  |

**Đề xuất:** lưu giá theo các dòng `FareRule` có thể khớp nhiều chiều, và lưu phần cộng thêm riêng bằng `SurchargeRule` chỉ khi bảng thực sự mô tả phụ thu. Không dùng một `basePrice` duy nhất trên showtime, không dùng hệ số toàn cục theo `SeatType`, và không lưu giá chốt trong `ShowSeat`.

Một số dòng trong bảng đã là giá trọn gói theo định dạng/nhóm vé/loại ghế; không cộng lại phụ thu VIP hoặc Sweetbox vào các dòng đó. Phụ thu được cộng ở bước quote chỉ khi đúng điều kiện và `stackable` đã được xác nhận.

## 2. Bảng giá tham khảo cần cấu hình

Đơn vị là VND. Giá và điều kiện chưa có ngày hiệu lực trong ảnh; phải xác nhận trước khi publish.

### 2.1. Vé 2D

| Nhóm ngày                       | Điều kiện                                                                       |     Giá |
| ------------------------------- | ------------------------------------------------------------------------------- | ------: |
| Thứ Hai, Ba, Năm                | Trẻ em và thành viên CGV Young                                                  |  60.000 |
| Thứ Hai, Ba, Năm                | Matinee trước 12:00: học sinh, sinh viên, người cao tuổi và suất chiếu đầu tiên |  70.000 |
| Thứ Hai, Ba, Năm                | Người lớn từ 12:00                                                              |  85.000 |
| Thứ Tư                          | Thứ Tư Vui Vẻ                                                                   |  75.000 |
| Thứ Sáu, Bảy, Chủ Nhật, ngày lễ | CGV Young và trẻ em trước 17:00                                                 |  70.000 |
| Thứ Sáu, Bảy, Chủ Nhật, ngày lễ | Học sinh, sinh viên, người cao tuổi trước 17:00                                 |  80.000 |
| Thứ Sáu, Bảy, Chủ Nhật, ngày lễ | Người lớn                                                                       | 105.000 |

| Phân hạng Premium Cinema | Nhóm ngày                       |     Giá |
| ------------------------ | ------------------------------- | ------: |
| Premium Cinema 2D        | Thứ Hai, Ba, Năm                | 180.000 |
| Premium Cinema 2D        | Thứ Sáu, Bảy, Chủ Nhật, ngày lễ | 230.000 |

Phụ thu 2D đọc từ ảnh: ghế VIP `+5.000`, phim bom tấn `+5.000`, Sweetbox `+25.000`. Cụm Matinee/“Suất Chiếu Đầu Tiên” chưa chỉ rõ điều kiện là AND hay OR; cần nghiệp vụ xác nhận. Xác nhận phụ thu có áp dụng cho Premium Cinema, có cộng dồn không, và Sweetbox tính cho mỗi vé hay cả cặp.

### 2.2. Vé 3D — người lớn

| Nhóm ngày                       | Khung giờ   | Ghế thường | Ghế VIP | Sweetbox |
| ------------------------------- | ----------- | ---------: | ------: | -------: |
| Thứ Hai, Ba, Năm                | Trước 12:00 |     90.000 | 100.000 |  120.000 |
| Thứ Hai, Ba, Năm                | 12:00–17:00 |    110.000 | 120.000 |  150.000 |
| Thứ Hai, Ba, Năm                | 17:00–21:30 |    130.000 | 140.000 |  170.000 |
| Thứ Hai, Ba, Năm                | Sau 21:30   |    110.000 | 120.000 |  150.000 |
| Thứ Tư                          | Cả ngày     |    100.000 | 100.000 |  120.000 |
| Thứ Sáu, Bảy, Chủ Nhật, ngày lễ | Trước 12:00 |    110.000 | 120.000 |  140.000 |
| Thứ Sáu, Bảy, Chủ Nhật, ngày lễ | 12:00–17:00 |    130.000 | 140.000 |  170.000 |
| Thứ Sáu, Bảy, Chủ Nhật, ngày lễ | 17:00–21:30 |    150.000 | 200.000 |  200.000 |
| Thứ Sáu, Bảy, Chủ Nhật, ngày lễ | Sau 21:30   |    130.000 | 140.000 |  170.000 |

| Phân hạng Premium Cinema | Nhóm ngày                       |     Giá |
| ------------------------ | ------------------------------- | ------: |
| Premium Cinema 3D        | Thứ Hai, Ba, Năm                | 230.000 |
| Premium Cinema 3D        | Thứ Sáu, Bảy, Chủ Nhật, ngày lễ | 270.000 |

**Cần xác minh hai ô 17:00–21:30 của bảng 3D cuối tuần:** ảnh hiển thị `200.000` cho VIP và `200.000` cho Sweetbox. Giữ đúng giá trị đọc được trong dữ liệu nháp, nhưng không publish trước khi rạp xác nhận.

### 2.3. Vé 3D ưu đãi

| Nhóm vé                             | Nhóm ngày                       | Điều kiện giờ     | Giá ghế thường / VIP |
| ----------------------------------- | ------------------------------- | ----------------- | -------------------: |
| Học sinh, sinh viên, người cao tuổi | Thứ Hai, Ba, Năm                | Trước 17:00       |               90.000 |
| Học sinh, sinh viên, người cao tuổi | Thứ Tư Vui Vẻ                   | Cả ngày theo bảng |              100.000 |
| Học sinh, sinh viên, người cao tuổi | Thứ Sáu, Bảy, Chủ Nhật, ngày lễ | Trước 17:00       |              110.000 |
| Trẻ em và CGV Young                 | Thứ Hai, Ba, Năm                | Trước 17:00       |               80.000 |
| Trẻ em và CGV Young                 | Thứ Tư Vui Vẻ                   | Cả ngày theo bảng |              100.000 |
| Trẻ em và CGV Young                 | Thứ Sáu, Bảy, Chủ Nhật, ngày lễ | Trước 17:00       |              100.000 |

Ghi chú ảnh: Sweetbox của vé ưu đãi 3D `+20.000`. Xác nhận giá VIP có bằng ghế thường cho các nhóm ưu đãi, và phụ thu Sweetbox tính mỗi vé hay mỗi cặp.

### 2.4. Vé 4DX

| Phiên bản phim | Nhóm ngày                       |     Trước 12:00 |     12:00–17:00 |     17:00–21:30 |       Sau 21:30 |
| -------------- | ------------------------------- | --------------: | --------------: | --------------: | --------------: |
| 3D             | Thứ Hai, Ba, Năm                |         160.000 |         190.000 |         210.000 |         190.000 |
| 3D             | Thứ Tư Vui Vẻ                   | 170.000 cả ngày | 170.000 cả ngày | 170.000 cả ngày | 170.000 cả ngày |
| 3D             | Thứ Sáu, Bảy, Chủ Nhật, ngày lễ |         180.000 |         210.000 |         270.000 |         210.000 |
| 2D             | Thứ Hai, Ba, Năm                |         150.000 |         160.000 |         180.000 |         160.000 |
| 2D             | Thứ Tư Vui Vẻ                   | 150.000 cả ngày | 150.000 cả ngày | 150.000 cả ngày | 150.000 cả ngày |
| 2D             | Thứ Sáu, Bảy, Chủ Nhật, ngày lễ |         170.000 |         180.000 |         200.000 |         180.000 |

### 2.5. Vé IMAX

| Phiên bản phim | Nhóm ngày                       | Suất chiếu đầu tiên | Trước 17:00 | Sau 17:00 |
| -------------- | ------------------------------- | ------------------: | ----------: | --------: |
| 3D             | Thứ Hai, Ba, Tư, Năm            |             120.000 |     150.000 |   180.000 |
| 3D             | Thứ Sáu, Bảy, Chủ Nhật, ngày lễ |             140.000 |     190.000 |   250.000 |
| 2D             | Thứ Hai, Ba, Tư, Năm            |             100.000 |     120.000 |   140.000 |
| 2D             | Thứ Sáu, Bảy, Chủ Nhật, ngày lễ |             120.000 |     150.000 |   180.000 |

## 3. Mô hình dữ liệu đề xuất

Tên class/entity cần theo quy ước hiện có của BE; bảng dưới là trách nhiệm dữ liệu, không bắt buộc đúng tên.

| Entity                   | Trường chính / trách nhiệm                                                                                                                                                                                                                                              |
| ------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `CinemaPricingPolicy`    | `id`, `cinemaId`, `version`, `status` (`DRAFT/PUBLISHED/ARCHIVED`), `currency`, `timezone`, `effectiveFrom`, `effectiveTo`, `publishedAt`, audit fields.                                                                                                                |
| `FareRule`               | `policyId`, `experienceType`, nullable `contentFormat`, `dayGroup`, nullable `timeBand`, nullable `ticketCategory`, nullable `seatType`, điều kiện `isFirstShowing`, `amount`, `priority`, `ruleCode`, `effective dates`. Null chỉ được dùng như wildcard có kiểm soát. |
| `SurchargeRule`          | điều kiện khớp (seat type, movie attribute, experience format, ticket category), `amount`, `unit` (`PER_TICKET/PER_PAIR/PER_BOOKING`), `stackable`, thời hạn.                                                                                                           |
| `CinemaHoliday`          | `cinemaId` hoặc vùng áp dụng, ngày, tên, `dayGroupOverride`; ngày lễ phải có ưu tiên xác định so với Wednesday/weekday.                                                                                                                                                 |
| `Showtime`               | `roomId`, `startsAt`, `endsAt`, `experienceType`, `contentFormat`, `pricingPolicyId/version`, `roomLayoutId/version`. Loại trải nghiệm và phiên bản nội dung phải tương thích với phòng và bản phát hành phim.                                                          |
| `ShowSeat`               | Seat identity/type/location và trạng thái tồn kho; không phải nguồn giá chốt.                                                                                                                                                                                           |
| `BookingLine` / `Ticket` | `showtimeId`, `seatId`, `ticketCategory`, policy version, matched fare rule IDs, surcharge breakdown, final amount, currency; snapshot để giữ lịch sử.                                                                                                                  |

Ràng buộc:

- Mỗi suất chiếu tham chiếu một bản policy đã publish để tránh giá thay đổi giữa lúc mở bán. Quy định rõ cách xử lý suất chiếu đã tạo nếu policy mới được publish.
- Policy đã publish bất biến; sửa bằng revision mới và publish theo ngày hiệu lực.
- Dùng giờ địa phương của rạp để xác định nhóm ngày và khung giờ; API nên truyền/đáp lại ISO-8601 có offset.
- Lịch lễ có độ ưu tiên cao hơn nhóm thứ trong tuần. Không suy ra ngày lễ từ client.
- Không cho hai rule cùng độ ưu tiên khớp cùng một tổ hợp dimensions. Validate missing cells, overlap, khoảng giờ rỗng/chồng lấn trước publish.
- Lưu giá tiền dạng số nguyên VND; không dùng floating point.
- Cần chốt các câu hỏi ghi ở trên (Premium, cờ suất đầu, cách tính Sweetbox, VIP trong ưu đãi, hai ô 3D cuối tuần) trước khi nhập tariff production.

## 4. API đề xuất

Giữ route/convention hiện hữu của dự án nếu đã có endpoint tương đương. Các API dưới đây là contract đề xuất, không khẳng định đang tồn tại.

```http
GET    /api/v1/cinemas/{cinemaId}/pricing-policies/active
GET    /api/v1/cinemas/{cinemaId}/pricing-policies/{policyId}
POST   /api/v1/cinemas/{cinemaId}/pricing-policies
PUT    /api/v1/cinemas/{cinemaId}/pricing-policies/{policyId}/fare-rules
POST   /api/v1/cinemas/{cinemaId}/pricing-policies/{policyId}/validate
POST   /api/v1/cinemas/{cinemaId}/pricing-policies/{policyId}/publish
POST   /api/v1/showtimes/pricing-preview
POST   /api/v1/bookings/quote
```

### 4.1. Tạo suất chiếu

Loại bỏ `basePrice`. Cấu hình giá thuộc policy của rạp. Nếu `experienceType` xác định từ phòng thì BE lấy từ phòng; nếu chọn theo phiên bản phát hành phim thì FE gửi riêng `experienceType` và `contentFormat`, BE kiểm tra tương thích với phòng/phim.

```json
{
  "movieId": "movie-uuid",
  "roomId": "room-uuid",
  "startsAt": "2026-10-23T19:00:00+07:00",
  "endsAt": "2026-10-23T21:12:00+07:00",
  "experienceType": "STANDARD",
  "contentFormat": "3D"
}
```

`experienceType` nhận `STANDARD`, `PREMIUM_CINEMA`, `4DX`, `IMAX`; `contentFormat` nhận `2D` hoặc `3D`. Cần xác nhận `PREMIUM_CINEMA` là loại phòng hay loại trải nghiệm trong domain BE. Không ghép công nghệ, phiên bản nội dung và phân hạng phòng vào một enum.

### 4.2. Pricing preview

Input cần đủ rạp/phòng/phim/thời gian/format; preview trả policy version, nhóm ngày/khung giờ đã khớp, mức giá theo nhóm vé/loại ghế và thông báo nếu ô giá thiếu.

```json
{
  "cinemaId": "cinema-uuid",
  "roomId": "room-uuid",
  "movieId": "movie-uuid",
  "startsAt": "2026-10-23T19:00:00+07:00",
  "experienceType": "STANDARD",
  "contentFormat": "3D"
}
```

```json
{
  "policy": { "id": "policy-uuid", "version": 4, "currency": "VND" },
  "resolved": {
    "dayGroup": "FRI_SAT_SUN_HOLIDAY",
    "timeBand": "17_21_30",
    "isFirstShowing": false
  },
  "fares": [
    {
      "ticketCategory": "ADULT",
      "seatType": "STANDARD",
      "amount": 150000,
      "ruleCode": "3D_WEEKEND_EVENING_ADULT_STD"
    },
    {
      "ticketCategory": "ADULT",
      "seatType": "VIP",
      "amount": 200000,
      "ruleCode": "3D_WEEKEND_EVENING_ADULT_VIP",
      "needsBusinessConfirmation": true
    }
  ],
  "surcharges": [],
  "warnings": ["Giá VIP 3D cuối tuần 17:00–21:30 cần rạp xác nhận trước khi publish."]
}
```

### 4.3. Booking quote — contract đang tồn tại

FE gọi POST /api/v1/bookings/quote với quyền booking:create.

```json
{
  "showtimeId": "UUID-v5-hoặc-v7",
  "items": [
    {
      "showSeatId": "UUID-v5-hoặc-v7",
      "ticketCategoryCode": "ADULT"
    }
  ]
}
```

- showSeatId là ID ghế của suất chiếu, không phải seatId vật lý.
- ticketCategoryCode phải thuộc catalog của policy gắn với suất chiếu.
- Booking Service trả ApiResponse<BookingQuoteResponse>.
- Inventory cung cấp POST /api/v1/booking-quotes cho cùng luồng;
  endpoint này trả BookingQuoteResponse trực tiếp.
- Backend tự tính giá trọn gói; số tiền VND trong response là chuỗi số nguyên.
- Response có quoteId, showtimeVersion, pricingPolicyId,
  pricingPolicyVersion, items, totalAmountVnd, issuedAt và expiresAt.
- Mỗi item có requested/effective category, fare rule, fare unit,
  amountVnd và evidence điều kiện thực sự sử dụng.
- COUPLE tính một lần theo PER_PAIR.
- Quote lưu snapshot nhưng không giữ ghế và không tạo booking.
- Suất SCHEDULED trả saleOpen=false; seatsHeld=false.
- Category bắt buộc được xác minh trước.
- Fare được xét theo priority giảm dần; chỉ xác minh các nhóm
  priority cần thiết để xác định kết quả duy nhất.
- Thiếu verifier không được coi là khách hàng không đủ điều kiện.
- Chưa có implementation xác minh ưu đãi thực tế trong repo.
- CreateBookingRequest chưa nhận quoteId; booking chưa tiêu thụ quote.
- Suất dùng policy vẫn bị chặn mở bán bằng
  SHOWTIME_POLICY_BOOKING_NOT_READY cho đến khi hoàn tất tích hợp booking.
- Các ví dụ phụ thu và first-show flag ở phần thiết kế ban đầu
  là nội dung lịch sử, không phải contract triển khai hiện tại.
- FARE_MATRIX_V2 dùng giá trọn gói, không cộng phụ thu.
- Suất chiếu sớm được xác định bằng ngày địa phương của policy
  trước ngày công chiếu chính thức đã snapshot.

## 5. Luồng xử lý BE

1. Tạo suất chiếu: kiểm tra movie, room, format/content format, xung đột lịch, quyền tạo và timezone.
2. Chọn layout PUBLISHED mới nhất của phòng; gắn `roomLayoutId/version` vào showtime.
3. Chọn policy PUBLISHED của cinema tại `startsAt`; gắn policy/version vào showtime.
4. Xác thực policy có thể tính giá cho các seat types, nhóm vé và format của suất chiếu. Nếu chưa đủ, trả lỗi trước khi phát hành/mở bán.
5. Sinh `ShowSeat` từ layout với dữ liệu inventory, không sao chép giá cơ bản cố định.
6. Preview/quote phân giải day group, time band, cờ first showing, ticket category, seat type và surcharge trên server.
7. Lưu quote snapshot và fare breakdown vào booking/ticket khi xác nhận; không tính lại lịch sử theo policy hiện hành.

## 6. Lỗi nghiệp vụ

| Trường hợp                                       | HTTP gợi ý | Mã lỗi                                     |
| ------------------------------------------------ | ---------: | ------------------------------------------ |
| Chưa có layout đã publish                        |        409 | `PUBLISHED_ROOM_LAYOUT_REQUIRED`           |
| Chưa có policy giá đã publish                    |        409 | `PUBLISHED_CINEMA_PRICING_POLICY_REQUIRED` |
| Không có giá khớp chiều của suất chiếu           |        409 | `TICKET_PRICE_RULE_NOT_FOUND`              |
| Có nhiều rule cùng độ ưu tiên khớp               |        409 | `AMBIGUOUS_TICKET_PRICE_RULE`              |
| Format không tương thích phòng/phim              |        422 | `SHOWTIME_FORMAT_NOT_SUPPORTED`            |
| Không đủ điều kiện nhóm vé                       |        422 | `TICKET_CATEGORY_NOT_ELIGIBLE`             |
| Policy chưa thể publish do matrix thiếu/xung đột |        422 | `PRICING_POLICY_INVALID`                   |

## 7. Khu vực BE cần cập nhật

- `CreateShowtimeRequest`: bỏ `basePrice`; chuẩn hóa format công nghệ và format nội dung.
- Pricing entities, repositories, DTOs, migrations, validation và authorization.
- Pricing resolver dùng timezone cinema, holiday calendar, time band, first-show flag và rule priority.
- Showtime creation: gắn policy/layout version và kiểm tra matrix trước khi mở bán.
- Booking quote/confirm: giá tính trên server, snapshot từng vé; xem lại mọi chỗ đọc `ShowSeat.price`.
- Cập nhật OpenAPI và tái sinh FE API client sau khi BE contract được chốt.
- Bảo toàn tương thích với showtime/booking hiện có bằng migration có kiểm soát; không đổi lại giá lịch sử.

## 8. Thứ tự triển khai

1. Nghiệp vụ xác nhận tariff trong mục 2, ngày hiệu lực, ngày lễ, timezone, định nghĩa suất đầu, Premium Cinema và phụ thu.
2. Thêm policy version + fare matrix + holiday/surcharge model và luồng validate/publish.
3. Thêm pricing preview; nối vào tạo suất chiếu.
4. Thêm quote/confirm và lưu giá snapshot cho từng booking line/ticket.
5. FE chuyển sang các API mới; bỏ `basePrice` sau giai đoạn tương thích.
6. Cập nhật OpenAPI, migration và dữ liệu cấu hình theo từng cinema.

## 9. Commit message gợi ý

```text
feat(pricing): support cinema fare matrices by format and ticket category
```

## Quyết định nghiệp vụ đã xác nhận

- Suất chiếu sớm là suất có ngày bắt đầu theo timezone policy
  trước ngày công chiếu chính thức.
- Dùng advanceScreeningCondition:
  ANY, ADVANCE_SCREENING, REGULAR_SCREENING.
- Khung giờ sáng/Matinee được cấu hình riêng bằng time band.
- Draft simulation nhận simulatedReleaseDate; Backend tự phân loại.
- Luồng showtime/quote thực tế phải lấy ngày công chiếu từ Movie Service.
  Thiếu ngày công chiếu phải được xử lý rõ ràng trước khi tính giá thực tế.
- SWEETBOX trong pricing catalog được cấu hình ánh xạ tới physical COUPLE.
- Fare và phụ thu Sweetbox/COUPLE tính theo cặp.
  Một ShowSeat COUPLE được tính giá một lần theo PER_PAIR.
- Đơn vị tính theo cặp không đồng nghĩa với giá/phụ thu tham khảo đã được duyệt.
- Inventory migrations đã được gom thành V1–V3 cho database tạo mới.

## Phạm vi bán của policy

- Sale scope là danh sách cho phép bán, khai báo độc lập với fare rules.
- Mỗi scope xác định experience/content, nhóm ngày, khung giờ,
  category, mã ghế và điều kiện suất chiếu sớm.
- Các scope khớp được hợp lại; scope không có priority.
- Ngoài scope là NOT_FOR_SALE.
- Trong scope nhưng không có fare khớp là GAP.
- Scope áp dụng trong thời gian hiệu lực của policy.
- Review kiểm tra tham chiếu; coverage hiện vẫn phân tích một ngày/scenario.
- Chưa có chứng nhận coverage toàn policy hoặc quyền publish.

## Kiểm tra toàn phạm vi ma trận

- POST /api/v1/cinemas/{cinemaId}/pricing-policies/{policyId}/fare-matrix/validate
  kiểm tra draft theo expectedVersion.
- Chạy review cấu hình trước, sau đó kiểm tra coverage và ambiguity
  trên các sale scopes trong toàn thời gian hiệu lực.
- Ngày và giờ được chia tại các ranh giới làm thay đổi tập rule khớp.
- Eligibility được kiểm tra theo mô hình các mã dương độc lập:
  tối đa một requiredEligibilityCode trên mỗi fare và điều kiện category.
- COMPLETE thể hiện phân tích hoàn tất; cần đọc matrixValid để biết kết quả.
- ANALYSIS_LIMIT_REACHED không được xem là hợp lệ.
- publicationValidated luôn false.
- Kết quả chưa xác minh tương thích giữa eligibility codes, dữ liệu khách hàng,
  phụ thu, capability phòng/phim, lịch lễ đầy đủ hoặc scheduling instants.
- Publish phải kiểm tra lại version và nội dung dưới lock ghi.

## Giá trọn gói và vai trò category

- FARE_MATRIX_V2 sử dụng giá trọn gói được cấu hình trong FareRule.
- Không cộng phụ thu ghế, định dạng hoặc thuộc tính phim.
- Mỗi policy hoàn chỉnh có đúng một category REGULAR, eligibility UNRESTRICTED.
- CATEGORY_DISCOUNT biểu diễn giá giảm theo đối tượng.
- Premium Cinema chuyển CATEGORY_DISCOUNT về category REGULAR.
- C_MONDAY, C_MEMBER và C_TEN chuyển về REGULAR khi là suất chiếu sớm.
- Suất chiếu sớm được xác định theo ngày bắt đầu ở timezone policy,
  trước ngày công chiếu chính thức.
- Fare phải khai báo REGULAR_SCREENING hoặc ADVANCE_SCREENING.
- ANY chỉ được dùng cho sale scope, không được dùng cho fare.
- Fare REGULAR không có requiredEligibilityCode.
- Preview/coverage trả requestedTicketCategoryCode,
  ticketCategoryCode thực dùng và categoryAdjustmentReason.
- Sweetbox ánh xạ physical COUPLE; giá PER_PAIR tính một lần cho cả cặp.
- Không triển khai chức năng hoàn vé trong phạm vi thay đổi này.
- GET/PUT fare-matrix/surcharges chỉ còn phục vụ đối chiếu/xóa cấu hình cũ.
- Giá tham khảo CGV chưa phải dữ liệu production đã được phê duyệt.

## Publish và phân giải published policy

- POST .../pricing-policies/{policyId}/publish xuất bản FARE_MATRIX_V2 draft.
- expectedVersion là optimistic policy.version.
- Publish lấy lock rạp trước, sau đó lock ghi policy.
- Kiểm tra overlap và chạy lại toàn bộ matrix validator trong transaction.
- Chỉ publish khi analysis COMPLETE, matrixValid và unitPricingValid đều true.
- Ghi publishedAt, publishedBy và publicationConfirmationReference.
- Business confirmation không thay thế xác minh khách hàng hoặc capability thực tế.
- Published policy và nội dung của nó bất biến qua các API hiện hữu.
- Không tự cắt hiệu lực policy đã publish.
- GET .../pricing-policies/active?startsAt=... chọn policy theo showtime start.
- Mỗi policy sử dụng timezone snapshot để diễn giải ngày hiệu lực.
- Khoảng hiệu lực là [effectiveFrom, effectiveTo]; effectiveTo không được bao gồm.
- Không chọn policy theo policyVersion lớn nhất.
- Showtime mới đã gắn published policy ID/business version, published layout ID/business version và Movie release-date/version snapshot.
- Booking/ticket và giá lịch sử chưa thay đổi.

## Capability phòng và phiên bản chiếu của phim

- Room và Movie khai báo tập ScreeningFormat riêng.
- Không suy luận capability từ RoomType.
- RoomType.VIP không tự ánh xạ thành PREMIUM_CINEMA.
- Không mặc định phim hỗ trợ 3D, 4DX hoặc IMAX.
- GET/PUT /api/v1/rooms/{roomId}/screening-capabilities quản lý capability phòng.
- GET/PUT /api/v1/movies/{movieId}/screening-capabilities quản lý bản chiếu phim.
- PUT là full replacement và kiểm tra optimistic version của owner.
- configuredFormats là danh sách được khai báo cho owner cụ thể.
- supportedFormatOptions chỉ là danh mục tổ hợp hệ thống nhận biết.
- Movie response trả releaseDate thật, status và version cùng formats.
- Danh sách rỗng không cho phép suy ra bất kỳ tổ hợp nào.
- Dữ liệu cũ không được tự seed capability bằng migration.
- Luồng tạo suất chiếu đã kiểm tra capability phòng/phim, chọn published layout và published policy, kiểm tra coverage, rồi sinh ShowSeat không chứa giá chốt.

## Đọc lại quote theo chủ sở hữu

- GET /api/v1/bookings/quote/{quoteId} trả ApiResponse<BookingQuoteResponse>.
- Inventory cung cấp GET /api/v1/booking-quotes/{quoteId}.
- Cả hai API yêu cầu booking:create.
- Chủ sở hữu lấy từ JWT; request không nhận userId.
- Quote không tồn tại hoặc thuộc người khác cùng trả 404.
- Quote hết hạn trả 409 INVENTORY_QUOTE_EXPIRED.
- Trả snapshot đã lưu, không tính lại giá.
- saleOpen là trạng thái tại thời điểm báo giá, không phải trạng thái hiện tại.
- GET không giữ ghế, không tiêu thụ quote và không xác nhận khả năng đặt vé.
- Bước consume quote phải kiểm tra owner, expiry, showtime version,
  trạng thái mở bán và trạng thái ghế trong luồng reservation.
- Booking vẫn chưa nhận quoteId; suất dùng policy vẫn bị chặn mở bán.
