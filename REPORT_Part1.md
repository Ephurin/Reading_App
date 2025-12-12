# BÁO CÁO NHÓM – PHẦN 1: CÔNG VIỆC CHUNG

> Ứng dụng: Reading App (Android – EPUB/PDF Reader + Ghi chú + Dịch nhanh)

## 1.a Lý do lựa chọn ứng dụng
- Nhu cầu đọc sách điện tử (PDF/EPUB) trên di động tăng mạnh; người dùng cần một ứng dụng gọn nhẹ, đọc mượt, có ghi chú/đánh dấu và hỗ trợ dịch nhanh để hiểu sâu nội dung ngoại ngữ.
- Ứng dụng tương tự trên thị trường:
  - Google Play Books, Kindle, Moon+ Reader…: mạnh về hệ sinh thái/sync nhưng một số thao tác nâng cao (vẽ tay trên PDF, highlight EPUB có ghi chú, dịch offline) còn hạn chế hoặc phụ thuộc tài khoản/dịch vụ.
- Điểm khác biệt của nhóm:
  - Kết hợp 3 mảng trong một luồng đọc: (1) EPUB có highlight + ghi chú; (2) PDF có vẽ tay + note; (3) Dịch nhanh offline bằng ML Kit, đổi ngôn ngữ đích ngay trong hộp thoại.
  - Lưu trữ ngoại tuyến, không bắt buộc đăng nhập để dùng các tính năng cốt lõi; tối ưu thao tác một chạm và giao diện gọn, dễ dùng.

## 1.b Mô tả yêu cầu chung của ứng dụng
### Yêu cầu chức năng (Functional)
- Quản lý thư viện cục bộ: import file PDF/EPUB, hiển thị danh sách, xóa.
- Đọc EPUB:
  - Hiển thị chương, chuyển chương; thanh công cụ nổi khi chọn văn bản.
  - Tô đậm (highlight) đoạn chọn với 5 màu; thêm/sửa/xóa ghi chú; xem danh sách highlight.
- Đọc PDF:
  - Phóng to/thu nhỏ; Canvas overlay để vẽ tay (nhiều nét, bảng màu); đặt note marker theo tọa độ; xóa nét vẽ từng trang.
- Dịch nội dung (EPUB):
  - Chọn đoạn văn bản → tự nhận diện ngôn ngữ nguồn → dịch sang ngôn ngữ đích (mặc định EN), đổi ngôn ngữ, sao chép kết quả, lưu bản dịch thành ghi chú.
- Dấu trang/tiến độ đọc: lưu vị trí (chương/trang), đánh dấu và chuyển nhanh.
- Tìm kiếm sách online (Google Books): truy vấn, xem kết quả cơ bản và import khi khả dụng.
- Đăng nhập/Thông tin tài khoản: cấu hình cơ bản (mở rộng cho kịch bản đồng bộ sau này).

### Yêu cầu phi chức năng (Non-functional)
- Hiệu năng: render trang mượt; thao tác highlight/vẽ tay tức thời; dịch nhanh sau lần tải model đầu.
- Khả dụng ngoại tuyến: đọc, highlight/ghi chú, vẽ tay, lưu trữ đều offline; dịch offline sau khi tải model ML Kit một lần (~30MB).
- Tương thích: Android 7.0+ (minSdk 24), Material 3, đa kích thước màn hình.
- Trải nghiệm: UI tối giản, thao tác một chạm; hỗ trợ chế độ tối/sáng; thông báo rõ khi tải model dịch.
- An toàn dữ liệu: lưu nội bộ (app sandbox); không rò rỉ file người dùng ra ngoài.

### UC tổng quát (mô tả ngắn)
- Actor chính: Người đọc.
- Use cases: Import Sách; Mở Sách; Đọc EPUB (Chuyển Chương, Highlight + Note, Dịch); Đọc PDF (Zoom, Vẽ tay, Note); Quản lý Dấu trang/Tiến độ; Tìm kiếm Sách Online; Quản lý Thư viện; Cài đặt ngôn ngữ dịch/ tải model.
- Quan hệ: “Người đọc” kích hoạt từng UC; “Đọc EPUB/PDF” bao gồm UC con (highlight, note, zoom, vẽ, dịch…).

## 1.c Phân tích lựa chọn kiến trúc, công nghệ & mô hình thiết kế
- Kiến trúc: MVVM – ViewModel quản lý trạng thái đọc/tiến độ; Jetpack Compose dựng UI; Coroutine cho xử lý nền.
- Công nghệ chính:
  - UI: Jetpack Compose + Material 3 (TopAppBar, Dialog, Canvas…).
  - EPUB: WebView + JavaScript Bridge để bắt selection, chèn `span` highlight, gọi callback Android.
  - PDF: `PdfRenderer` render bitmap; Canvas overlay + gesture (drag/tap/transform) để vẽ nét và đặt note.
  - Dịch: Google ML Kit (Language Identification + Translation) – hỗ trợ offline sau khi tải model.
  - Lưu trữ: JSON nội bộ cho highlight/annotation; cơ sở dữ liệu cục bộ cho thư viện sách; SharedPreferences/Datastore cho cấu hình nhẹ.
  - Mạng: Google Books API cho tìm kiếm sách online.
- Lý do chọn: Compose giúp rút ngắn thời gian UI và quản trị state; WebView phù hợp EPUB; PdfRenderer ổn định, không kéo thêm lib nặng; ML Kit cho dịch offline không cần API key đám mây.

## 1.d Cơ sở dữ liệu
- Hình thức lưu trữ:
  - Sách & metadata: cơ sở dữ liệu cục bộ (offline).
  - Highlight EPUB: JSON nội bộ qua `HighlightManager` (id, bookFilePath, chapterIndex, selectedText, color, note, timestamp…).
  - Annotation PDF: JSON nội bộ qua `PdfAnnotationManager` (type=NOTE/DRAWING…, drawingPaths[], position, color, timestamp…).
  - Dấu trang/tiến độ: lưu nội bộ theo sách/chương-trang.
- Sơ đồ/Quan hệ (logic):
  - Entity Book(id, title, author, filePath, type[PDF/EPUB], cover, addedAt…).
  - Entity Progress(bookId, chapterOrPage, updatedAt).
  - JSON Highlights (bookId, chapterIndex, text, color, note,…).
  - JSON PdfAnnotations (bookId, pageNumber, drawingPaths[], note-position,…).
  - Quan hệ: Book 1–N Progress/Highlights/Annotations (liên kết qua bookId hoặc filePath).
- Lý do: DB cho tra cứu thư viện nhanh; JSON linh hoạt cho cấu trúc annotation đa dạng, không cần lược đồ cố định.

## 1.e Tổng quan các nội dung mới và khó
- Điểm mới:
  - Dịch nhanh ngay trong ngữ cảnh đọc EPUB, hoạt động offline sau khi tải model, đổi ngôn ngữ đích tức thời.
  - Hai cơ chế ghi chú theo định dạng: highlight + note (EPUB) và vẽ tay + note (PDF), cùng triết lý lưu trữ nhẹ (JSON).
  - Thanh công cụ nổi khi chọn văn bản; luồng "chọn → highlight/ghi chú → dịch → lưu thành ghi chú" tự nhiên.
- Điểm khó:
  - Bridge JavaScript–Android cho highlight chính xác theo selection DOM (xử lý `Range.surroundContents`, fallback thao tác node).
  - Canvas overlay PDF: quản lý nhiều nét vẽ mượt, đồng bộ với zoom, lưu/khôi phục đường nét theo trang.
  - Quản trị state Compose với WebView/PdfRenderer; cache Translator để dịch tức thời sau lần đầu; trải nghiệm tải model (~30MB) thân thiện.
- Hướng phát triển:
  - Đồng bộ đám mây (ghi chú/annotation/tiến độ) đa thiết bị.
  - Export/Import ghi chú (JSON/Markdown), chèn annotation vào PDF.
  - Từ điển/IPA, TTS, tìm kiếm toàn văn; undo/redo nét vẽ; highlight hình học (underline/rect) cho PDF; bảo mật/mã hóa dữ liệu.
