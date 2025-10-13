package com.justine.service;

import java.util.List;

public interface EmailService {
    void sendEmail(String to, String subject, String text);

    void sendEmailToMultiple(String[] to, String subject, String text);

    List<String> getAdminEmails();

    List<String> getManagerEmails();
}
