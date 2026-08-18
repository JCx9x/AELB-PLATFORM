package es.aelb.backendweb.infrastructure.email;
import es.aelb.backendweb.application.email.*; import es.aelb.backendweb.infrastructure.config.EmailProperties; import org.springframework.context.annotation.Profile; import org.springframework.mail.*; import org.springframework.mail.javamail.*; import org.springframework.stereotype.Component;
@Component("smtpEmailSender") @Profile({"local","dev"})
public class SmtpEmailSender implements EmailSender {
 private final JavaMailSender sender; private final EmailProperties props;
 public SmtpEmailSender(EmailProperties props){this.props=props; JavaMailSenderImpl s=new JavaMailSenderImpl();s.setHost(props.getSmtpHost());s.setPort(props.getSmtpPort());s.getJavaMailProperties().put("mail.smtp.auth",String.valueOf(props.isSmtpAuth()));s.getJavaMailProperties().put("mail.smtp.starttls.enable",String.valueOf(props.isSmtpTls()));this.sender=s;}
 public void send(EmailMessage m){try{MimeMessageHelper h=new MimeMessageHelper(sender.createMimeMessage(),true,"UTF-8");h.setFrom(props.getFrom(),props.getFromName());h.setTo(m.recipient());h.setSubject(m.subject());h.setText(m.plainText(),m.html());sender.send(h.getMimeMessage());}catch(MailAuthenticationException|MailSendException e){throw new TemporaryEmailException("SMTP no disponible",e);}catch(Exception e){throw new PermanentEmailException("Email inválido",e);}}
}
