# PHẦN 2 – BÁO CÁO CÁ NHÂN (Nguyễn Đức Thái)

## 2.1. Các chức năng đảm nhiệm và luồng hoạt động

- Đọc EPUB: Tích hợp `WebView` với cầu nối JavaScript để nhận vùng văn bản được chọn, áp dụng highlight, hiển thị thanh công cụ nổi (Highlight/Translate/Note), lưu highlight và ghi chú vào bộ nhớ cục bộ.
- Đọc PDF: Sử dụng `PdfRenderer` hiển thị theo trang; lớp phủ `Canvas` cho vẽ (bút) và ghi chú; lưu chú thích per-page; hỗ trợ đổi màu bút, xoá nét, thêm marker ghi chú.
- Dịch ngôn ngữ (EPUB): Tích hợp ML Kit (Language Identification + Translation) cho dịch offline; tự động nhận dạng nguồn; kiểm tra/trì hoãn mở dialog cho tới khi dịch xong; bộ nhớ đệm translator để giảm thời gian chờ về sau.
- Quản lý dữ liệu: Lưu highlight EPUB và annotation PDF theo dạng JSON, đồng bộ theo id sách/trang; tải lại khi mở.

Luồng điển hình EPUB:
1) Người dùng chọn đoạn văn → 2) WebView gửi range về app → 3) Thanh công cụ nổi hiển thị → 4) Chọn Highlight/Note/Translate → 5) Với Translate: xác định ngôn ngữ, đảm bảo model sẵn sàng, dịch và hiển thị `TranslationDialog` → 6) Người dùng sao chép/khoá lưu nếu cần.

Luồng điển hình PDF:
1) Mở trang → 2) Canvas hiển thị annotation đã lưu → 3) Người dùng vẽ/đổi màu/xoá → 4) Nhấn vào vị trí để thêm ghi chú → 5) Lưu theo trang.

## 2.2. Thiết kế giao diện (UI/UX)

- Compose Material 3, thanh công cụ nổi tối giản với nút rõ ràng (Highlight/Translate/Note) để giảm số thao tác.
- `TranslationDialog`: hiển thị kết quả, lựa chọn ngôn ngữ đích, trạng thái tải/khả dụng model, nút sao chép/lưu; không mở sớm trước khi có kết quả.
- `ModelDownloadDialog`: chỉ hiện khi thiếu model; thông báo nhẹ nhàng, tránh gây gián đoạn.
- PDF overlay: thanh màu bút, nút xoá nét, marker ghi chú, đảm bảo tương phản tốt trên nền trang.

## 2.3. Giải pháp kỹ thuật chính

- EPUB Highlight Bridge: JavaScript trong WebView đánh dấu bằng thẻ/span + class; app nhận vùng, tạo `Highlight` và lưu JSON; khi tải lại, tiêm script để khôi phục highlight.
- PDF Annotation: `PdfRenderer` + `Canvas` Compose; lưu các path (toạ độ, màu, độ dày) và danh sách notes theo trang; khởi tạo lại overlay mỗi lần đổi trang.
- ML Kit Translation:
  - Nhận dạng ngôn ngữ nguồn với `LanguageIdentification`.
  - Kiểm tra/đảm bảo model dịch đã tải; xoá ràng buộc Wi‑Fi; cache translator theo cặp (src→dst).
  - Dời thời điểm mở dialog cho tới khi dịch xong; dùng `LaunchedEffect` để đồng bộ trạng thái khi người dùng chọn lại ngôn ngữ.
- Lưu trữ JSON: Quản lý đọc/ghi an toàn, đặt theo bookId/pageIndex để tránh xung đột; khởi động app sẽ nạp lại dữ liệu đã lưu.

## 2.4. Điểm mới và giá trị mang lại

- Trải nghiệm dịch tức thời trong EPUB, hoạt động offline sau lần tải đầu.
- Overlay PDF trực quan, phù hợp ghi chú học tập/đọc tài liệu chuyên môn.
- Kiến trúc tách lớp quản lý dịch (TranslationManager), highlight/annotation manager → dễ bảo trì, mở rộng (ví dụ đồng bộ đám mây).

## 2.5. Khó khăn và cách khắc phục

- Độ trễ dịch ban đầu do tải model: Khắc phục bằng phát hiện thiếu model, hiển thị dialog tải riêng, cache translator, và chỉ hiển thị kết quả sau khi dịch xong.
- Đồng bộ UI Compose với kết quả dịch: Sử dụng `remember` + `LaunchedEffect` để cập nhật khi người dùng đổi ngôn ngữ đích; tránh dialog mở trước khi sẵn sàng.
- Phục hồi highlight EPUB qua WebView: Tách script khôi phục, chuẩn hoá selector, xử lý các trường hợp DOM thay đổi.
- Hiệu năng PDF overlay: Giới hạn số path vẽ mỗi lần render, batch lưu, và tối ưu hit‑test cho ghi chú.

## 2.6. Đánh giá và hướng phát triển

- Đánh giá: Các tính năng hoạt động ổn định, trải nghiệm mượt sau lần tải model; ghi chú PDF đủ dùng cho nhu cầu đọc cơ bản.
- Hướng phát triển:
  - Dịch cho PDF (chọn vùng chữ OCR‑able) và EPUB toàn đoạn.
  - Undo/Redo cho annotation, bộ chọn bút nâng cao (độ dày, opacity).
  - Đồng bộ hoá qua đám mây, xuất/nhập highlight/annotation.
  - Thống kê thời gian đọc, heatmap highlight để hỗ trợ học tập.
