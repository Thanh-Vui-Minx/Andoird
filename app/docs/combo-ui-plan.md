# Kế hoạch cập nhật UI tổng thể

Ngày: 2025-11-13
Phiên bản: 1.1
Người soạn: (auto-generated)

## Mục tiêu tóm tắt
- Cập nhật lại toàn bộ UI liên quan đến Menu/Combo/Cart/Profile/History/Voucher/Payment và các dialog liên quan để có giao diện đồng nhất, trực quan và thân thiện với người dùng.
- Phân tách rõ ràng 2 mục "Thức ăn" và "Nước uống" trên trang Menu và hiện thực Combo (thường xuyên vs giới hạn thời gian) với CRUD cho admin.
- Chuẩn hóa các layout XML (card style, padding, headers, bottom taskbar) và trích xuất chuỗi văn bản vào `strings.xml` khi hợp lý.

## Mở rộng phạm vi (Scope cập nhật)
Các file layout sẽ được rà soát và cập nhật để thống nhất giao diện và UX:

- activity layouts:
  - `activity_main.xml`
  - `activity_menu.xml`
  - `activity_cart.xml`
  - `activity_profile.xml`
  - `activity_order_history.xml`
  - `activity_payment.xml`
  - `activity_voucher_list.xml`
  - `activity_signup.xml`

- item / card layouts:
  - `item_food.xml`, `item_menu.xml`, `item_cart.xml`, `item_order.xml`, `item_voucher.xml`, `item_combo.xml`, `item_combo_food.xml`

- dialogs:
  - `dialog_food_details.xml`, `dialog_combo_detail.xml`, `dialog_checkout.xml`, `dialog_add_food.xml`, `dialog_add_combo.xml`, `dialog_add_voucher.xml`, `dialog_order_details.xml`

- other UI artifacts
  - styles/colors: review `colors.xml` and `styles.xml` usages
  - strings: extract hard-coded Vietnamese strings to `strings.xml`

> Ghi chú: tôi sẽ không thay đổi logic backend (Firestore rules) — nếu cần thay đổi quyền truy cập tôi sẽ đề xuất snippet rule trong phần follow-up.

## Deliverables (Sản phẩm giao)
1. Tài liệu kế hoạch (file này) đã cập nhật.
2. Tập hợp layout XML đã được chuẩn hóa (các file nêu ở trên) với cấu trúc card/headers/taskbar đồng nhất.
3. Cập nhật code liên quan: `MenuActivity.java`, `MenuAdapter.java`, `ComboAdapter.java`, `CartActivity.java` (nếu cần), `VoucherActivity.java` (tinh chỉnh hiển thị), `ProfileActivity.java` (layout tinh chỉnh).
4. Trích xuất chuỗi vào `strings.xml` (theo lựa chọn của bạn).
5. Kiểm thử nhanh: checklist QA + ghi chú các vấn đề phát hiện (nếu có).

## Thiết kế & UX đề xuất (tóm tắt)
- Hệ thống: sử dụng card-based design cho danh sách (món, combo, đơn hàng, voucher).
- Header / Section: rõ ràng, kiểu chữ lớn hơn cho header sections và màu chủ đạo cho accent.
- Taskbar dưới cùng: duy trì card taskbar hiện tại nhưng thống nhất icon và spacing cho mọi activity.
- Dialog: tất cả dialog chính (chi tiết món, checkout, add combo) sẽ có width ~95% màn hình và scrollable.
- Internationalization: tách text vào `strings.xml` để sau này dễ dịch.

## Kế hoạch công việc (chi tiết, theo giai đoạn)

- Giai đoạn 1 — Khảo sát & Chuẩn bị (0.5 ngày)
  1. Đánh giá nhanh tất cả layout hiện có (tôi đã liệt kê file ở trên).
  2. Chốt 3 quyết định thiết kế đầu vào (bạn xác nhận):
     - Palette màu (giữ hiện tại / cung cấp màu mới)
     - Có trích xuất strings sang `strings.xml` không (Yes/No)
     - Mức thay đổi: Full layout refactor (A) hoặc Chỉ visual polish (B)

- Giai đoạn 2 — Thay đổi layout cơ bản (1 - 2 ngày)
  1. Áp dụng card/header/taskbar chung vào: `activity_menu.xml`, `activity_main.xml`, `activity_cart.xml`, `activity_profile.xml`, `activity_order_history.xml`.
  2. Chuẩn hóa `item_*.xml` (spacing, hình, font-size, colors).
  3. Trích xuất strings (nếu được chọn).

- Giai đoạn 3 — Combo & Admin flows (1 ngày)
  1. Hoàn thiện combo UI (nếu chưa xong): `item_combo.xml`, `dialog_add_combo.xml`, `dialog_combo_detail.xml`.
  2. Đảm bảo admin có thể mở dialog thêm combo từ taskbar hoặc menu Add.

- Giai đoạn 4 — Checkout / Voucher / Order polish (0.5 ngày)
  1. Đồng bộ dialog thanh toán `dialog_checkout.xml` với style chung.
  2. Làm mới `activity_voucher_list.xml` và `dialog_add_voucher.xml` để khớp style.

- Giai đoạn 5 — Test & Fix (0.5 - 1 ngày)
  1. Chạy kiểm tra layout trên nhiều kích thước màn hình (emulator) — sửa overflow/ellipsize.
  2. Kiểm thử thao tác CRUD cơ bản (thêm món, thêm combo, thêm voucher, checkout flow).

## Các file ưu tiên sửa (lần lượt theo deploy)
1. `activity_menu.xml`, `item_menu.xml`, `item_food.xml`, `item_combo.xml` (Menu UX)
2. `dialog_add_combo.xml`, `dialog_combo_detail.xml` (Admin combo)
3. `dialog_checkout.xml`, `CartActivity` (checkout UX)
4. `activity_profile.xml`, `activity_order_history.xml`, `activity_voucher_list.xml`, `activity_payment.xml` (profile/history/payment ux)
5. `activity_main.xml`, `activity_signup.xml` (entry flows)

## Acceptance Criteria (mở rộng)
- Giao diện nhất quán trên tất cả activity và dialog.
- Không có text hard-coded (nếu chọn extract strings).
- Các phần quan trọng hoạt động: add combo (admin), add to cart combo, checkout display, voucher apply.

## Các câu hỏi tôi cần bạn xác nhận (rất quan trọng)
1. Palette / colors: bạn muốn tôi giữ `@color/primary` và `@color/accent` hiện tại hay thay bằng palette mới? (Ghi rõ hex nếu đổi). Nếu bạn không chọn, tôi sẽ giữ màu hiện tại.
2. Strings: có trích xuất tất cả text sang `strings.xml` không? (Khuyến nghị: Yes)
3. Phạm vi thay đổi: bạn chọn A (Full refactor: cấu trúc & visual) hoặc B (Visual polish only)? (Tôi đề xuất A để đồng nhất lâu dài.)
4. Assets: bạn có logo / hình minh họa muốn dùng thay placeholder không? Nếu có, upload vào `app/src/main/res/drawable/` hoặc gửi cho tôi link.
5. Edit combo: có cần tôi triển khai tính năng SỬA combo ngay trong lần này không? (Yes/No)

## Timeline ước lượng (sơ bộ)
- Khảo sát & chốt thiết kế: 0.5 ngày (chờ xác nhận bạn)
- Thay đổi layout cơ bản: 1 - 2 ngày
- Combo & Admin flows: 1 ngày
- Checkout/Voucher polish: 0.5 ngày
- Test & fix: 0.5 - 1 ngày

## Cách tôi sẽ thực hiện thay đổi (workflow)
1. Tôi sẽ sửa các file theo từng nhóm (3–6 file mỗi đợt) và commit trực tiếp vào branch `main` trong workspace. Mỗi đợt sửa tôi sẽ báo: "What/why/outcome" trước khi chạy các thay đổi.
2. Sau mỗi 3–5 sửa tôi sẽ báo tiến độ và danh sách file đã thay đổi.
3. Tôi sẽ chạy kiểm tra lỗi Java/compile-time (nếu môi trường hỗ trợ JDK 11) và sửa lỗi compile nếu tìm thấy. Nếu build gặp rào cản JDK (như hiện tại), tôi sẽ tạm thời chạy kiểm tra static (lint-like) và đọc lỗi biên dịch từ output nếu có.

---

Nếu bạn đồng ý, hãy trả lời tóm tắt: ví dụ "OK, bắt đầu; Palette: giữ hiện tại; Strings: Yes; Scope: A; Edit combo: Yes" hoặc chỉ "OK, bắt đầu" để tôi áp dụng mặc định (giữ màu hiện tại, trích xuất strings, full refactor, bật sửa combo).

Cảm ơn — tôi sẽ bắt đầu ngay sau khi nhận được xác nhận của bạn.