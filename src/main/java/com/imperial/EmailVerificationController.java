package com.imperial;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController @RequestMapping("/api/account/verification")
public class EmailVerificationController {
 private final StudentAccountRepository students;
 private final EmailVerificationService verification;
 public EmailVerificationController(StudentAccountRepository students,EmailVerificationService verification){
  this.students=students;this.verification=verification;
 }
 public record VerifyInput(@NotBlank String token){}
 public record ResendInput(@NotBlank @Email String email){}
 @PostMapping("/verify")
 public Map<String,String> verify(@Valid @RequestBody VerifyInput input){
  try{verification.verify(input.token());return Map.of("message","Email verified. You can now log in.");}
  catch(IllegalArgumentException ex){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,ex.getMessage());}
 }
 @PostMapping("/resend")
 public Map<String,String> resend(@Valid @RequestBody ResendInput input){
  String generic="If an unverified account exists and email is configured, a verification link will be sent.";
  if(!verification.isConfigured())return Map.of("message",generic);
  students.findByEmail(input.email().trim().toLowerCase(Locale.ROOT)).ifPresent(student->{
   if(student.isVerificationRequired()&&!student.isEmailVerified()){
    try{verification.send(student);}catch(Exception ignored){}
   }
  });
  return Map.of("message",generic);
 }
}
