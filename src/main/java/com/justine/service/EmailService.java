package com.justine.service;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface EmailService {
    void sendEmail(String to, String subject, String text);
    void sendEmail(String to, String subject, String text, HttpServletRequest request);


    void sendEmailToMultiple(String[] to, String subject, String text);

    List<String> getAdminEmails();

    List<String> getManagerEmails();

    void sendEmailWithBcc(String from, String[] to, String[] bcc, String subject, String text);

}
