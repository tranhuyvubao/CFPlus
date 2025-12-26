package com.example.do_an_hk1_androidstudio;

import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

public class GmailSender extends Authenticator {

    private final String userEmail;
    private final String userPassword;
    private final Session session;

    public GmailSender(String email, String password) {
        this.userEmail = email;
        this.userPassword = password;

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(userEmail, userPassword);
            }
        });
    }

    public void sendEmail(String toEmail, String subject, String messageBody) throws MessagingException {
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(userEmail));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject(subject);
        message.setText(messageBody);
        Transport.send(message);
    }
}
