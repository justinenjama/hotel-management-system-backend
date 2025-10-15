package com.justine.serviceImpl;

import com.justine.enums.StaffRole;
import com.justine.model.Staff;
import com.justine.repository.StaffRepository;
import com.justine.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final StaffRepository staffRepository;

    public EmailServiceImpl(JavaMailSender mailSender, StaffRepository staffRepository) {
        this.mailSender = mailSender;
        this.staffRepository = staffRepository;
    }

    @Override
    public void sendEmail(String to, String subject, String text) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, true); // enable HTML
            mailSender.send(mimeMessage);
            log.info("Email sent successfully to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    @Override
    public void sendEmailToMultiple(String[] to, String subject, String text) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, true);
            mailSender.send(mimeMessage);
            log.info("Bulk email sent successfully to {}", (Object) to);
        } catch (Exception e) {
            log.error("Failed to send bulk email: {}", e.getMessage());
        }
    }

    @Override
    public List<String> getAdminEmails() {
        return staffRepository.findByRole(StaffRole.ADMIN)
                .stream()
                .map(Staff::getEmail)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getManagerEmails() {
        return staffRepository.findByRole(StaffRole.MANAGER)
                .stream()
                .map(Staff::getEmail)
                .collect(Collectors.toList());
    }
}
