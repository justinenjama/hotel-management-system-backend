package com.justine.utils;

import com.justine.model.Invoice;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
public class InvoiceServiceHelper {

    private final CloudinaryService cloudinaryService;

    public InvoiceServiceHelper(CloudinaryService cloudinaryService) {
        this.cloudinaryService = cloudinaryService;
    }

    /**
     * Generate PDF in memory, upload to Cloudinary, and return the public URL for the large size.
     * The invoice object will store all generated sizes.
     */
    public String generateAndUploadInvoice(Invoice invoice) {
        try {
            // 1. Generate the PDF as MultipartFile (in memory)
            MultipartFile pdfFile = InvoicePdfGenerator.generateReceipt(invoice);

            // 2. Upload to Cloudinary under folder "invoices" with eager sizes
            Map<String, String> urls = cloudinaryService.uploadFileWithEagerSizes(pdfFile, "invoices");

            // 3. Save URLs to the invoice object
            invoice.setInvoiceUrl(urls.get("large"));          // main invoice URL
            invoice.setInvoiceUrlMedium(urls.get("medium"));   // optional medium size
            invoice.setInvoiceUrlThumbnail(urls.get("thumbnail")); // optional thumbnail

            return urls.get("large");
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate and upload invoice: " + e.getMessage(), e);
        }
    }
}
