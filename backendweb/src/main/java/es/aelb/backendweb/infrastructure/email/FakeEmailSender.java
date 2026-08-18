package es.aelb.backendweb.infrastructure.email;
import es.aelb.backendweb.application.email.*; import org.springframework.context.annotation.Profile; import org.springframework.stereotype.Component; import java.util.*; import java.util.concurrent.CopyOnWriteArrayList;
@Component("fakeEmailSender") @Profile("test") public class FakeEmailSender implements EmailSender { private final List<EmailMessage> sent=new CopyOnWriteArrayList<>(); public void send(EmailMessage m){sent.add(m);} public List<EmailMessage> sentEmails(){return List.copyOf(sent);} }
