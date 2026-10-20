# Đề xuất thay đổi Backend: suất chiếu và chính sách giá vé

> Đề xuất cập nhật theo bảng giá Cinestar Parkcity Hanoi trong ảnh. Đây là tài liệu hướng dẫn; không sửa repository.

## 1. Mục tiêu

- Backend tự chốt sơ đồ PUBLISHED mới nhất của phòng khi tạo suất chiếu.
- Chính sách giá của rạp mô tả giá theo ngày, khung giờ, nhóm vé và điều kiện ưu đãi.
- Phụ thu ghế, phim và định dạng được cấu hình riêng, không trộn vào giá gốc theo nhóm vé.
- Suất chiếu lưu phiên bản chính sách đã áp dụng; giá cuối cùng được tính khi khách lấy báo giá/đặt vé.

## 2. Hiện trạng đã đối chiếu

- CreateShowtimeRequest hiện cần movieId, roomId, startsAt, endsAt, basePrice; roomLayoutId là tùy chọn.
- ShowtimeServiceImpl.create() gọi ShowtimeLayoutService.resolveForNewShowtime(...), sau đó ShowSeatGenerationService.generate(showtime, basePrice).
- Nếu không truyền roomLayoutId nhưng phòng có layout đã xuất bản, backend trả lỗi SHOWTIME_LAYOUT_REQUIRED. Nếu không có layout matrix PUBLISHED, luồng hiện tại có thể dùng ghế legacy.
- Đã có GET /api/v1/room-layouts/published-seat-map?roomId={roomId}. API tìm layout matrix PUBLISHED mới nhất và hiện yêu cầu inventory:manage.
- SeatPricingPolicy hiện có hệ số toàn cục theo SeatType; ShowSeat.price hiện được lưu lúc sinh ghế.
- Bảng ảnh có các nhóm khách và khung giờ; mức vé không thể biểu diễn đầy đủ bằng một giá cố định cho từng SeatType.

## 3. Tách loại vé khỏi loại ghế

| Khái niệm      | Ví dụ                                             | Dùng để                                        |
| -------------- | ------------------------------------------------- | ---------------------------------------------- |
| TicketCategory | ADULT, SENIOR_55_PLUS, STUDENT_U22_TEACHER, CHILD | Chọn giá cơ bản và xác minh điều kiện mua      |
| SeatType       | STANDARD, VIP, COUPLE, ACCESSIBLE                 | Xác định vị trí/đặc tính ghế và phụ thu nếu có |
| ShowtimeFormat | STANDARD_2D, IMAX, SPECIAL_FORMAT                 | Phụ thu theo định dạng                         |
| MovieAttribute | BLOCKBUSTER                                       | Phụ thu theo thuộc tính phim                   |

Không đặt các nhóm khách vào SeatType. Ghế đôi +10.000đ là phụ thu ghế; vé người cao tuổi 55.000đ là giá theo TicketCategory và khung giờ.

## 4. Quy tắc giá cần biểu diễn

Các con số dưới đây được đọc từ ảnh tham khảo và chỉ nên dùng làm dữ liệu cấu hình cho đúng rạp, sau khi nghiệp vụ xác nhận:

| Điều kiện                                         | Người lớn | Trên 55 tuổi | HSSV/U22/Giáo viên | Trẻ em |
| ------------------------------------------------- | --------: | -----------: | -----------------: | -----: |
| C’Monday thứ Hai                                  |    45.000 |       45.000 |             45.000 | 45.000 |
| C’Member thứ Tư                                   |    45.000 |       45.000 |             45.000 | 45.000 |
| C’Ten trước 10:00 hoặc từ 22:00, không gồm lễ/Tết |    49.000 |       49.000 |             49.000 | 49.000 |
| Trong tuần 10:00–18:00                            |    69.000 |       55.000 |             49.000 | 49.000 |
| Trong tuần 18:00–22:00                            |    75.000 |       55.000 |             49.000 | 49.000 |
| Cuối tuần 10:00–18:00                             |    75.000 |       55.000 |             49.000 | 49.000 |
| Cuối tuần 18:00–22:00                             |    79.000 |       55.000 |             49.000 | 49.000 |
| Lễ/Tết                                            |    89.000 |       89.000 |             89.000 | 65.000 |

Ảnh ghi thứ trong tuần, phạm vi ngày, điều kiện C’Member và cách tính phụ thu. Hãy xác nhận các điều kiện này với nghiệp vụ trước khi nhập dữ liệu. Happy Hour ghi chú không áp dụng lễ/Tết. Trẻ dưới 0,7 m được miễn phí khi ngồi cùng phụ huynh; trẻ 0,7–1,3 m thuộc nhóm vé trẻ em.

Phụ thu được cấu hình riêng trong ảnh: ghế đôi +10.000đ, phim bom tấn +5.000đ, định dạng đặc biệt +5.000đ. Cần chốt rõ phụ thu ghế đôi tính cho mỗi ghế hay cho cả cặp, và các phụ thu phim/định dạng có được cộng dồn hay không.

## 5. Mô hình dữ liệu đề xuất

| Thành phần          | Trách nhiệm                                                                                                                             |
| ------------------- | --------------------------------------------------------------------------------------------------------------------------------------- |
| CinemaPricingPolicy | Chính sách phiên bản hóa thuộc cinema: cinemaId, version, status, currency, effective dates, publishedAt và audit fields.               |
| TicketPriceRule     | Quy tắc giá cơ bản: ngày/loại ngày, khoảng giờ địa phương, TicketCategory, số tiền, điều kiện thành viên/kênh, ngày hiệu lực, priority. |
| CinemaHolidayDate   | Lịch ngày lễ áp dụng cho rạp/khu vực; dùng để ưu tiên giá lễ và loại Happy Hour.                                                        |
| TicketSurchargeRule | Phụ thu theo SeatType, ShowtimeFormat hoặc MovieAttribute; gồm amount, unit, stackable và điều kiện áp dụng.                            |
| Showtime            | Giữ roomLayoutId và pricingPolicyId/version đã chốt lúc tạo suất chiếu.                                                                 |
| ShowSeat            | Quản lý ghế, vị trí và trạng thái giữ/đặt; không giữ giá cuối cùng theo loại vé.                                                        |
| BookingLine/Ticket  | Lưu TicketCategory, seatId, giá cuối cùng, currency và snapshot rule/policy dùng để tính.                                               |

Chính sách PUBLISHED không sửa tại chỗ. Tạo phiên bản mới rồi publish. Suất chiếu đã tạo giữ policy version của nó; nếu nghiệp vụ muốn cập nhật giá cho suất chiếu đã mở bán thì cần một luồng cập nhật riêng.

## 6. Cấu trúc rule minh họa

Mỗi rule giá cơ bản trả về một mức giá theo TicketCategory. Quy tắc cùng nhóm giá cơ bản không cộng dồn; chọn một rule theo priority. Phụ thu được tính sau đó theo cấu hình stackable.

```json
{
  "currency": "VND",
  "rules": [
    {
      "code": "MONDAY_HAPPY_DAY",
      "priority": 400,
      "days": ["MONDAY"],
      "ticketPrices": {
        "ADULT": 45000,
        "SENIOR_55_PLUS": 45000,
        "STUDENT_U22_TEACHER": 45000,
        "CHILD": 45000
      }
    },
    {
      "code": "WEDNESDAY_C_MEMBER",
      "priority": 400,
      "days": ["WEDNESDAY"],
      "memberTier": "C_MEMBER",
      "ticketPrices": {
        "ADULT": 45000,
        "SENIOR_55_PLUS": 45000,
        "STUDENT_U22_TEACHER": 45000,
        "CHILD": 45000
      }
    },
    {
      "code": "HAPPY_HOUR",
      "priority": 300,
      "timeRanges": [
        { "from": "00:00", "to": "10:00" },
        { "from": "22:00", "to": "24:00" }
      ],
      "excludeDayType": "HOLIDAY",
      "ticketPrices": {
        "ADULT": 49000,
        "SENIOR_55_PLUS": 49000,
        "STUDENT_U22_TEACHER": 49000,
        "CHILD": 49000
      }
    }
  ],
  "surcharges": [
    {
      "code": "COUPLE_SEAT",
      "condition": { "seatType": "COUPLE" },
      "amount": 10000,
      "unit": "CONFIRM_WITH_BUSINESS",
      "stackable": true
    },
    {
      "code": "BLOCKBUSTER",
      "condition": { "movieAttribute": "BLOCKBUSTER" },
      "amount": 5000,
      "unit": "PER_TICKET",
      "stackable": true
    },
    {
      "code": "SPECIAL_FORMAT",
      "condition": { "showtimeFormat": "SPECIAL" },
      "amount": 5000,
      "unit": "PER_TICKET",
      "stackable": true
    }
  ]
}
```

Đây chỉ là ví dụ contract. Mức giá và điều kiện phải lưu theo rạp, không hardcode trong enum, service hay FE. Dùng khoảng thời gian nửa mở, ví dụ 10:00–18:00 gồm 10:00 và không gồm 18:00, để hai khung giờ không bị chồng lấn.

## 7. API đề xuất

```http
GET  /api/v1/cinemas/{cinemaId}/pricing-policies/active
POST /api/v1/cinemas/{cinemaId}/pricing-policies
POST /api/v1/cinemas/{cinemaId}/pricing-policies/{policyId}/publish
POST /api/v1/showtimes/pricing-preview
POST /api/v1/bookings/quote
```

- GET policy trả phiên bản PUBLISHED và các rule/phụ thu để trang cấu hình hoặc preview hiển thị.
- Pricing preview nhận cinemaId, roomId, movieId, startsAt và trả mức giá dự kiến theo nhóm vé cùng phụ thu tương ứng.
- Booking quote nhận showtimeId, seatId và TicketCategory cho từng vé; backend xác minh điều kiện thành viên/giấy tờ theo khả năng của hệ thống rồi trả tổng tiền.
- Quyền đọc policy/preview phải phù hợp cho người quản lý suất chiếu; API sơ đồ PUBLISHED hiện cần xem lại quyền inventory:manage.

Tạo suất chiếu sau chuyển đổi:

```json
{
  "movieId": "movie-uuid",
  "roomId": "room-uuid",
  "startsAt": "2026-10-25T18:30:00+07:00",
  "endsAt": "2026-10-25T20:42:00+07:00"
}
```

Bỏ basePrice khỏi CreateShowtimeRequest. Backend tự chọn layout PUBLISHED mới nhất và policy version đang được áp dụng. Sau giai đoạn tương thích, bỏ roomLayoutId khỏi request.

## 8. Luồng tính tiền và tạo suất chiếu

1. Khi tạo suất chiếu, xác thực movie, room, thời gian và quyền.
2. Chọn layout PUBLISHED mới nhất của room theo layoutVersion.
3. Chọn pricing policy PUBLISHED của cinema và lưu policy ID/version lên Showtime.
4. Sinh ShowSeat từ layout, chỉ tạo dữ liệu ghế/trạng thái.
5. Khi khách xem giá, đánh giá ngày/giờ bắt đầu theo timezone của rạp, loại ngày, nhóm vé và ưu đãi.
6. Chọn một rule giá cơ bản theo priority; áp dụng các phụ thu đủ điều kiện; trả báo giá.
7. Khi đặt vé, lưu tổng giá và rule/policy snapshot lên BookingLine/Ticket để bảo toàn lịch sử.

Nếu thiếu layout hoặc policy, trả lỗi nghiệp vụ rõ ràng; không âm thầm fallback sang ghế legacy cho form mới.

## 9. Lỗi nghiệp vụ gợi ý

| Trường hợp                               | HTTP gợi ý | Mã lỗi                                   |
| ---------------------------------------- | ---------- | ---------------------------------------- |
| Phòng chưa có layout matrix PUBLISHED    | 409        | PUBLISHED_ROOM_LAYOUT_REQUIRED           |
| Rạp chưa có pricing policy PUBLISHED     | 409        | PUBLISHED_CINEMA_PRICING_POLICY_REQUIRED |
| Không có rule cho khung giờ/nhóm vé      | 409        | TICKET_PRICE_RULE_NOT_FOUND              |
| TicketCategory không đủ điều kiện ưu đãi | 422        | TICKET_CATEGORY_NOT_ELIGIBLE             |
| Không có quyền xem cấu hình              | 403        | Mã quyền hiện có                         |

## 10. Khu vực BE cần cập nhật

- DTO CreateShowtimeRequest, pricing policy/rule, preview và booking quote.
- ShowtimeServiceImpl, ShowtimeLayoutService, ShowSeatGenerationService.
- Thêm entity/repository/migration cho policy, price rules, holiday calendar và surcharge rules.
- Tách TicketCategory khỏi SeatType.
- Tạo pricing resolver dùng timezone cinema, điều kiện ngày/giờ, ưu đãi và priority.
- Không dùng ShowSeat.price làm giá cuối cùng; lưu số tiền đã chốt trên BookingLine/Ticket.
- Điều chỉnh API/authorization đọc layout và policy cho showtime manager.
- Cập nhật OpenAPI; sau đó tái sinh FE API client.

## 11. Thứ tự triển khai

1. Chốt bảng giá, ngày áp dụng, ưu tiên Happy Day/C’Ten/lễ, điều kiện thành viên và quy tắc cộng phụ thu.
2. Thêm model và API quản lý policy version, rule, holiday và surcharge.
3. Thêm pricing preview và booking quote resolver.
4. Gắn policy version vào Showtime; refactor lưu giá cuối cùng trên booking/ticket.
5. Chuyển FE rồi loại basePrice khỏi request; giữ compatibility có thời hạn nếu cần.
6. Cập nhật OpenAPI và migration cho suất chiếu/ghế hiện có.

## 12. Commit message gợi ý

```text
feat(pricing): add versioned cinema tariff rules and ticket surcharges
```
