package com.imperial;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class EmailVerificationService {
 private final EmailVerificationTokenRepository tokens;
 private final JavaMailSender mail;
 private final SecureRandom random=new SecureRandom();
 private final boolean enabled;
 private final String from;
 private final String base;
 public EmailVerificationService(EmailVerificationTokenRepository tokens,JavaMailSender mail,
  @Value("${MAIL_ENABLED:false}") boolean enabled,@Value("${MAIL_FROM:}") String from,
  @Value("${APP_BASE_URL:https://imperial-education.onrender.com}") String base){
  this.tokens=tokens;this.mail=mail;this.enabled=enabled;this.from=from.trim();this.base=base.replaceAll("/+$","");
 }
 public boolean isConfigured(){return enabled&&!from.isBlank();}
 private String hash(String value){
  try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}
  catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
 }
 @Transactional
 public void send(StudentAccount student){
  if(!isConfigured())throw new IllegalStateException("Email service not configured");
  byte[] bytes=new byte[32];random.nextBytes(bytes);
  String secret=HexFormat.of().formatHex(bytes);
  EmailVerificationToken token=tokens.save(new EmailVerificationToken(student,hash(secret),Instant.now().plusSeconds(86400)));
  SimpleMailMessage msg=new SimpleMailMessage();msg.setFrom(from);msg.setTo(student.getEmail());
  msg.setSubject("IMPERIAL EDUCATION - Verify your email");
  msg.setText("Verify your email using this link (valid for 24 hours):\n"+base+"/verify-email.html?token="+secret+"\n\nIf you did not register, ignore this message.");
  try{mail.send(msg);}catch(Exception e){token.markUsed();tokens.save(token);throw new IllegalStateException("Could not send verification email");}
 }
 @Transactional
 public void verify(String secret){
  if(secret==null||!secret.matches("[a-fA-F0-9]{64}"))throw new IllegalArgumentException("Invalid verification link");
  EmailVerificationToken token=tokens.findByTokenHash(hash(secret.toLowerCase(Locale.ROOT)))
   .orElseThrow(()->new IllegalArgumentException("Invalid verification link"));
  if(token.isUsed()||!token.getExpiresAt().isAfter(Instant.now()))
   throw new IllegalArgumentException("Verification link expired or already used");
  token.getStudent().markEmailVerified();token.markUsed();
 }
}
