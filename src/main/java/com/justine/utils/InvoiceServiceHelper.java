package com.justine.utils;

import com.justine.model.Invoice;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class InvoiceServiceHelper {

    private final CloudinaryService cloudinaryService;

    public InvoiceServiceHelper(CloudinaryService cloudinaryService) {
        this.cloudinaryService = cloudinaryService;
    }

    /**
     * Generate PDF, upload to Cloudinary, and return URL
     */
    public String generateAndUploadInvoice(Invoice invoice) {
        try {
            // 1. Generate local PDF file
            File pdfFile = InvoicePdfGenerator.generateReceipt(invoice, "invoices");

            // 2. Upload to Cloudinary under folder "invoices"
            String url = cloudinaryService.uploadFile(pdfFile, "invoices");

            // 3. Save URL to invoice
            invoice.setInvoiceUrl(url);

            // 4. Delete local file after upload (optional cleanup)
            if (pdfFile.exists()) {
                pdfFile.delete();
            }

            return url;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate and upload invoice: " + e.getMessage(), e);
        }
    }
}
