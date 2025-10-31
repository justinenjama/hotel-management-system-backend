package com.justine.utils;

import com.justine.model.Invoice;
import com.justine.model.Guest;
import com.justine.model.Booking;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Utility to generate a PDF receipt in memory and return it as a MultipartFile.
 */
public class InvoicePdfGenerator {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(ZoneId.systemDefault());

    /**
     * Generate a PDF receipt for a paid invoice entirely in memory.
     *
     * @param invoice Invoice entity
     * @return MultipartFile representing the generated PDF (ready for upload)
     */
    public static MultipartFile generateReceipt(Invoice invoice) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // === Create PDF document ===
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, outputStream);
            document.open();

            // ===== Title =====
            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            Paragraph title = new Paragraph("Payment Receipt", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("\n"));

            // ===== Company Info =====
            Font boldFont = new Font(Font.HELVETICA, 12, Font.BOLD);
            document.add(new Paragraph("FiveStarHotel Ltd.", boldFont));
            document.add(new Paragraph("123 Beach Road, Nairobi, Kenya"));
            document.add(new Paragraph("Email: support@fivestarhotel.com"));
            document.add(new Paragraph("Phone: +254 711 000 999"));
            document.add(new Paragraph("\n"));

            // ===== Client Info =====
            Booking booking = invoice.getBooking();
            Guest client = booking != null ? booking.getGuest() : null;

            document.add(new Paragraph("Billed To:", boldFont));
            if (client != null) {
                document.add(new Paragraph(client.getFullName()));
                document.add(new Paragraph(client.getEmail()));
                document.add(new Paragraph(client.getPhoneNumber()));
                if (client.getIdNumber() != null) {
                    document.add(new Paragraph("ID/Passport: " + client.getIdNumber()));
                }
            } else {
                document.add(new Paragraph("Guest details not available"));
            }

            document.add(new Paragraph("\n"));

            // ===== Invoice Info Table =====
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);

            table.addCell("Invoice Number");
            table.addCell(invoice.getInvoiceNumber());

            table.addCell("Issued Date");
            table.addCell(invoice.getIssuedDate() != null
                    ? DATE_FORMAT.format(invoice.getIssuedDate())
                    : "-");

            table.addCell("Total Amount");
            table.addCell(String.format("%.2f", invoice.getTotalAmount()));

            table.addCell("Status");
            table.addCell(invoice.isPaid() ? "PAID" : "UNPAID");

            document.add(table);
            document.add(new Paragraph("\n"));

            // ===== Footer =====
            Paragraph footer = new Paragraph("Thank you for choosing our hotel!", boldFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();

            // === Convert PDF to MultipartFile ===
            byte[] pdfBytes = outputStream.toByteArray();
            String fileName = "receipt_" + invoice.getInvoiceNumber() + ".pdf";

            return new InMemoryMultipartFile(fileName, "application/pdf", pdfBytes);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate receipt PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Custom lightweight MultipartFile for in-memory data.
     */
    private static class InMemoryMultipartFile implements MultipartFile {

        private final String fileName;
        private final String contentType;
        private final byte[] content;

        public InMemoryMultipartFile(String fileName, String contentType, byte[] content) {
            this.fileName = fileName;
            this.contentType = contentType;
            this.content = content != null ? content : new byte[0];
        }

        @Override
        public String getName() {
            return fileName;
        }

        @Override
        public String getOriginalFilename() {
            return fileName;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() {
            return content;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(File dest) throws IOException {
            try (FileOutputStream fos = new FileOutputStream(dest)) {
                fos.write(content);
            }
        }
    }
}
