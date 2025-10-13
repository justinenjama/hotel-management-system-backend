package com.justine.utils;

import com.justine.model.Invoice;
import com.justine.model.Guest;
import com.justine.model.Booking;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class InvoicePdfGenerator {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(ZoneId.systemDefault());

    /**
     * Generate PDF receipt for a paid invoice
     *
     * @param invoice Invoice entity
     * @param outputDir directory to store the PDF
     * @return File pointing to generated PDF
     */
    public static File generateReceipt(Invoice invoice, String outputDir) {
        try {
            File dir = new File(outputDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String fileName = "receipt_" + invoice.getInvoiceNumber() + ".pdf";
            File file = new File(dir, fileName);

            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            // Title
            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            Paragraph title = new Paragraph("Payment Receipt", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            document.add(new Paragraph("\n"));

            // Company Info
            Font boldFont = new Font(Font.HELVETICA, 12, Font.BOLD);
            document.add(new Paragraph("HotelFlow Ltd.", boldFont));
            document.add(new Paragraph("123 Beach Road, Nairobi, Kenya"));
            document.add(new Paragraph("Email: support@hotelflow.com"));
            document.add(new Paragraph("Phone: +254 700 000 000"));

            document.add(new Paragraph("\n"));

            // Client Info
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

            // Invoice Info Table
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);

            table.addCell("Invoice Number");
            table.addCell(invoice.getInvoiceNumber());

            table.addCell("Issued Date");
            table.addCell(invoice.getIssuedDate() != null ? DATE_FORMAT.format(invoice.getIssuedDate()) : "-");

            table.addCell("Total Amount");
            table.addCell(String.format("%.2f", invoice.getTotalAmount()));

            table.addCell("Status");
            table.addCell(invoice.isPaid() ? "PAID" : "UNPAID");

            document.add(table);

            document.add(new Paragraph("\n"));

            // Footer
            Paragraph footer = new Paragraph("Thank you for choosing our hotel!", boldFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();

            return file;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate receipt PDF: " + e.getMessage(), e);
        }
    }
}
