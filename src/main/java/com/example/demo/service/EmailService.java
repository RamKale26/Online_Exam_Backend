package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public CompletableFuture<Boolean> sendOtp(String to, String otp) {
        try {
            System.out.println("=================================");
            System.out.println("[EMAIL] Sending OTP to: " + to);
            System.out.println("[EMAIL] From: " + fromEmail);
            System.out.println("=================================");

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Your OTP - Online Exam System");

            String htmlContent = "<html><body style='font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 20px;'>"
                    + "<div style='max-width: 480px; margin: 0 auto; background: #ffffff; border-radius: 10px; padding: 30px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);'>"
                    + "<h2 style='color: #4f46e5; text-align: center; margin-top: 0;'>Online Exam System</h2>"
                    + "<p style='color: #555; font-size: 16px;'>Hello,</p>"
                    + "<p style='color: #555; font-size: 16px;'>Your One-Time Password (OTP) for login is:</p>"
                    + "<div style='text-align: center; margin: 30px 0;'>"
                    + "<span style='font-size: 40px; font-weight: bold; letter-spacing: 10px; color: #4f46e5; background: #eef2ff; padding: 15px 30px; border-radius: 8px;'>"
                    + otp
                    + "</span></div>"
                    + "<p style='color: #888; font-size: 14px;'>This OTP is valid for <strong>5 minutes</strong>. Do not share it with anyone.</p>"
                    + "<p style='color: #aaa; font-size: 12px; margin-top: 30px; text-align: center;'>If you did not request this OTP, please ignore this email.</p>"
                    + "</div></body></html>";

            helper.setText(htmlContent, true);
            mailSender.send(message);

            System.out.println("[EMAIL] OTP SENT SUCCESSFULLY to: " + to);
            return CompletableFuture.completedFuture(true);

        } catch (Exception e) {
            System.err.println("[EMAIL ERROR] Failed to send OTP to: " + to);
            System.err.println("[EMAIL ERROR] Type: " + e.getClass().getName());
            System.err.println("[EMAIL ERROR] Cause: " + e.getMessage());
            Throwable root = e;
            while (root.getCause() != null)
                root = root.getCause();
            System.err.println("[EMAIL ERROR] Root cause: " + root.getMessage());
            e.printStackTrace();
            return CompletableFuture.completedFuture(false);
        }
    }

    @Async
    public CompletableFuture<Boolean> sendResultEmail(String toEmail, String studentName, String categoryName,
            int score, int totalQuestions, List<com.example.demo.model.Answer> answers) {
        try {
            System.out.println("=================================");
            System.out.println("[EMAIL] Sending result to: " + toEmail);
            System.out.println("=================================");

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Your Exam Performance Report - " + categoryName);

            double percentage = totalQuestions > 0 ? ((double) score / totalQuestions) * 100 : 0;
            String status = percentage >= 50 ? "PASSED" : "FAILED";
            String statusColor = percentage >= 50 ? "#22c55e" : "#ef4444";

            StringBuilder htmlContent = new StringBuilder();
            htmlContent.append("<html><body style='font-family: Arial, sans-serif; color: #333;'>");
            htmlContent.append(
                    "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e2e8f0; border-radius: 8px;'>");
            htmlContent.append("<h2 style='color: #4f46e5; text-align: center;'>Online Exam Performance Report</h2>");
            htmlContent.append("<p>Dear <strong>").append(studentName).append("</strong>,</p>");
            htmlContent.append("<p>Thank you for taking the exam. Here is a summary of your performance:</p>");

            htmlContent.append("<table style='width: 100%; border-collapse: collapse; margin: 20px 0;'>");
            htmlContent.append(
                    "<tr style='background-color: #f8fafc;'><td style='padding: 10px; border: 1px solid #e2e8f0;'><strong>Category:</strong></td><td style='padding: 10px; border: 1px solid #e2e8f0;'>")
                    .append(categoryName).append("</td></tr>");
            htmlContent.append(
                    "<tr><td style='padding: 10px; border: 1px solid #e2e8f0;'><strong>Score:</strong></td><td style='padding: 10px; border: 1px solid #e2e8f0;'>")
                    .append(score).append(" / ").append(totalQuestions).append("</td></tr>");
            htmlContent.append(
                    "<tr style='background-color: #f8fafc;'><td style='padding: 10px; border: 1px solid #e2e8f0;'><strong>Percentage:</strong></td><td style='padding: 10px; border: 1px solid #e2e8f0;'>")
                    .append(String.format("%.1f", percentage)).append("%</td></tr>");
            htmlContent.append(
                    "<tr><td style='padding: 10px; border: 1px solid #e2e8f0;'><strong>Status:</strong></td><td style='padding: 10px; border: 1px solid #e2e8f0; color: ")
                    .append(statusColor).append("; font-weight: bold;'>").append(status).append("</td></tr>");
            htmlContent.append("</table>");

            if (answers != null && !answers.isEmpty()) {
                htmlContent.append("<h3>Detailed Review</h3>");
                htmlContent.append("<table style='width: 100%; border-collapse: collapse; margin: 15px 0;'>");
                htmlContent.append("<thead style='background-color: #4f46e5; color: white;'>");
                htmlContent.append(
                        "<tr><th style='padding: 8px; border: 1px solid #e2e8f0; text-align: left;'>Question</th><th style='padding: 8px; border: 1px solid #e2e8f0; text-align: left;'>Your Answer</th><th style='padding: 8px; border: 1px solid #e2e8f0; text-align: left;'>Correct Answer</th><th style='padding: 8px; border: 1px solid #e2e8f0;'>Result</th></tr>");
                htmlContent.append("</thead><tbody>");

                for (com.example.demo.model.Answer ans : answers) {
                    boolean isCorrect = ans.getQuestion().getCorrectAnswer().equalsIgnoreCase(ans.getSelectedAnswer());
                    String rowBg = isCorrect ? "#f0fdf4" : "#fef2f2";
                    String resText = isCorrect ? "✓ Correct" : "✗ Incorrect";
                    String resColor = isCorrect ? "#16a34a" : "#dc2626";

                    htmlContent.append("<tr style='background-color: ").append(rowBg).append(";'>");
                    htmlContent.append("<td style='padding: 8px; border: 1px solid #e2e8f0;'>")
                            .append(ans.getQuestion().getQuestion()).append("</td>");
                    htmlContent.append("<td style='padding: 8px; border: 1px solid #e2e8f0;'>")
                            .append(ans.getSelectedAnswer()).append("</td>");
                    htmlContent.append("<td style='padding: 8px; border: 1px solid #e2e8f0;'>")
                            .append(ans.getQuestion().getCorrectAnswer()).append("</td>");
                    htmlContent.append("<td style='padding: 8px; border: 1px solid #e2e8f0; color: ").append(resColor)
                            .append("; font-weight: bold; text-align: center;'>").append(resText).append("</td>");
                    htmlContent.append("</tr>");
                }
                htmlContent.append("</tbody></table>");
            }

            htmlContent.append("<p style='margin-top: 20px;'>Best regards,<br/>Online Exam System Team</p>");
            htmlContent.append("</div></body></html>");

            helper.setText(htmlContent.toString(), true);
            mailSender.send(message);

            System.out.println("[EMAIL] Result email SENT SUCCESSFULLY to: " + toEmail);
            return CompletableFuture.completedFuture(true);

        } catch (Exception e) {
            System.err.println("[EMAIL ERROR] Failed to send result email to: " + toEmail);
            System.err.println("[EMAIL ERROR] Cause: " + e.getMessage());
            e.printStackTrace();
            return CompletableFuture.completedFuture(false);
        }
    }
}