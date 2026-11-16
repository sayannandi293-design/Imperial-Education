package com.imperial;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController
@RequestMapping("/api/account/password")
public class PasswordResetController {
 private final StudentAccountRepository accounts;
 private final PasswordResetTokenRepository tokens;
 private final PasswordEncoder encoder;
 private final JavaMailSender mailer;
 private final SecureRandom random=new SecureRandom();
 private final String baseUrl;
 private final String fromAddress;
 private final boolean mailEnabled;
 public PasswordResetController(StudentAccountRepository accounts,PasswordResetTokenRepository tokens,PasswordEncoder encoder,JavaMailSender mailer,
   @Value("${APP_BASE_URL:https://imperial-education.onrender.com}") String baseUrl,
   @Value("${MAIL_FROM:}") String fromAddress,
   @Value("${MAIL_ENABLED:false}") boolean mailEnabled){
  this.accounts=accounts;this.tokens=tokens;this.encoder=encoder;this.mailer=mailer;
  this.baseUrl=baseUrl.replaceAll("/+$","");this.fromAddress=fromAddress;this.mailEnabled=mailEnabled;
 }
 public record Forgot(@NotBlank @Email String email){}
 public record Reset(@NotBlank String token,@NotBlank @Size(min=12,max=72) String password){}
 private String hash(String token){
  try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}
  catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
 }
 @PostMapping("/forgot")
 public Map<String,String> forgot(@Valid @RequestBody Forgot body){
  String generic="If this account exists and email delivery is configured, a reset link will be sent.";
  if(!mailEnabled||fromAddress.isBlank())return Map.of("message",generic);
  accounts.findByEmail(body.email().trim().toLowerCase(Locale.ROOT)).ifPresent(student->{
   byte[] bytes=new byte[32];random.nextBytes(bytes);
   String raw=HexFormat.of().formatHex(bytes);
   PasswordResetToken saved=tokens.saveAndFlush(new PasswordResetToken(student,hash(raw),Instant.now().plusSeconds(900)));
   SimpleMailMessage message=new SimpleMailMessage();
   message.setFrom(fromAddress);message.setTo(student.getEmail());message.setSubject("IMPERIAL EDUCATION - Password reset");
   message.setText("Use this one-time link to reset your password (expires in 15 minutes):\n"+baseUrl+"/reset-password.html?token="+raw+"\n\nIf you didn't request this, ignore this email.");
   try{mailer.send(message);}catch(Exception ex){saved.markUsed();tokens.save(saved);}
  });
  return Map.of("message",generic);
 }
 @PostMapping("/reset")
 @Transactional
 public Map<String,String> reset(@Valid @RequestBody Reset body){
  if(!body.token().matches("[a-fA-F0-9]{64}"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid reset token");
  PasswordResetToken record=tokens.findByTokenHash(hash(body.token().toLowerCase(Locale.ROOT))).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid or expired reset link"));
  if(record.isUsed()||!record.getExpiresAt().isAfter(Instant.now()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid or expired reset link");
  record.getStudent().setPasswordHash(encoder.encode(body.password()));
  record.markUsed();
  return Map.of("message","Password updated. Please sign in.");
 }
}
