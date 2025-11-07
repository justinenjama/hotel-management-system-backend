package com.justine.serviceImpl;

import com.justine.enums.StaffRole;
import com.justine.model.Staff;
import com.justine.repository.StaffRepository;
import com.justine.security.events.RateLimitEvent;
import com.justine.service.EmailService;
import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
@EnableAsync
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final StaffRepository staffRepository;
    private ExecutorService emailExecutor;
    public EmailServiceImpl(JavaMailSender mailSender,
                            StaffRepository staffRepository) {
        this.mailSender = mailSender;
        this.staffRepository = staffRepository;
    }

    @PostConstruct
    public void init() {
        this.emailExecutor = Executors.newFixedThreadPool(3);
    }

    // ---------------- Utility to get client IP ----------------
    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        String forwarded = request.getHeader("X-Forwarded-For");
        return (forwarded != null) ? forwarded.split(",")[0] : request.getRemoteAddr();
    }

    // ---------------- Email Sending ----------------
    @Async
    @Override
    public void sendEmail(String to, String subject, String text) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, true);
            mailSender.send(mimeMessage);
            log.info("📩 Email sent successfully to {}", to);
        } catch (Exception e) {
            log.error("❌ Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    @Override
    public void sendEmail(String to, String subject, String text, HttpServletRequest request) {
        // You can reuse your existing logic, optionally log or handle the IP
        String ip = getClientIp(request);
        log.info("📨 Sending email to {} (IP={})", to, ip);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, true);
            mailSender.send(mimeMessage);
            log.info("📩 Email sent successfully to {} (IP={})", to, ip);
        } catch (Exception e) {
            log.error("❌ Failed to send email to {} (IP={}): {}", to, ip, e.getMessage());
        }
    }


    @Async
    @Override
    public void sendEmailToMultiple(String[] to, String subject, String text) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, true);
            mailSender.send(mimeMessage);
            log.info("📩 Bulk email sent successfully to {}", (Object) to);
        } catch (Exception e) {
            log.error("❌ Failed to send bulk email: {}", e.getMessage());
        }
    }

    @Async
    @Override
    public void sendEmailWithBcc(String from, String[] to, String[] bcc, String subject, String text) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setFrom(from);
            if (to != null && to.length > 0) helper.setTo(to);
            if (bcc != null && bcc.length > 0) helper.setBcc(bcc);
            helper.setSubject(subject);
            helper.setText(text, true);
            mailSender.send(mimeMessage);
            log.info("📩 Email with BCC sent ({} visible, {} bcc)",
                    (to != null ? to.length : 0), (bcc != null ? bcc.length : 0));
        } catch (Exception e) {
            log.error("❌ Failed to send email with BCC: {}", e.getMessage());
        }
    }

    // ---------------- Admin Retrievals ----------------
    @Override
    public List<String> getAdminEmails() {
        return staffRepository.findByRole(StaffRole.ADMIN)
                .stream()
                .map(Staff::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getManagerEmails() {
        return staffRepository.findByRole(StaffRole.MANAGER)
                .stream()
                .map(Staff::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    // ---------------- Event Listener for Rate-Limit Alerts ----------------
    @Async
    @EventListener
    public void handleRateLimitAlert(RateLimitEvent event) {
        try {
            List<String> admins = getAdminEmails();
            if (admins.isEmpty()) return;

            String subject = "⚠️ Rate Limit Alert: Repeated Abuse Detected";
            String message = """
                    <p>Dear Admin,</p>
                    <p>The system detected repeated rate-limit breaches:</p>
                    <ul>
                      <li><b>IP Address:</b> %s</li>
                      <li><b>Consecutive Days:</b> %d</li>
                    </ul>
                    <p>Regards,<br/>Security Bot</p>
                    """.formatted(event.getIp(), event.getConsecutiveDays());

            sendEmailToMultiple(admins.toArray(new String[0]), subject, message);
            log.info("🚨 Admin alert sent for repeated abuse by IP {}", event.getIp());
        } catch (Exception e) {
            log.error("❌ Failed to handle rate-limit alert event: {}", e.getMessage());
        }
    }
}
