package com.cinex.cinema.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    /** Địa chỉ gửi mail, trùng với username trong cấu hình SMTP. */
    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /** Gửi mã OTP đặt lại mật khẩu tới email do người dùng nhập vào. */
    public void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Mã OTP khôi phục mật khẩu");
        message.setText("Mã OTP của bạn là: " + otp + "\nMã này có hiệu lực trong 5 phút. Vui lòng không chia sẻ cho người khác.");

        try {
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Gửi email OTP tới {} thất bại", toEmail, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không gửi được email OTP, vui lòng thử lại");
        }
    }
}
