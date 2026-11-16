package com.imperial;
import java.util.Map;
import java.util.Locale;
import java.security.Principal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController
@RequestMapping("/api/account")
public class AccountController {
 private final StudentAccountRepository accounts;
 private final PasswordEncoder passwords;
 public AccountController(StudentAccountRepository accounts,PasswordEncoder passwords){this.accounts=accounts;this.passwords=passwords;}
 public record Registration(@NotBlank @Size(max=120) String fullName, @NotBlank @Email @Size(max=254) String email, @NotBlank @Size(min=12,max=72) String password){}
 public record Profile(String fullName,String email){}
 @GetMapping("/csrf") public Map<String,String> csrf(@RequestAttribute("_csrf") CsrfToken token){
  return Map.of("headerName",token.getHeaderName(),"token",token.getToken());
 }
 @PostMapping("/register")
 @ResponseStatus(HttpStatus.CREATED)
 public Profile register(@Valid @RequestBody Registration registration){
  String email=registration.email().trim().toLowerCase(Locale.ROOT);
  String fullName=registration.fullName().trim();
  if(fullName.isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Name is required");
  if(accounts.existsByEmail(email))throw new ResponseStatusException(HttpStatus.CONFLICT,"An account with this email already exists");
  try{
   StudentAccount saved=accounts.saveAndFlush(new StudentAccount(fullName,email,passwords.encode(registration.password())));
   return new Profile(saved.getFullName(),saved.getEmail());
  }catch(DataIntegrityViolationException ex){throw new ResponseStatusException(HttpStatus.CONFLICT,"An account with this email already exists");}
 }
 public record UpdateProfile(@NotBlank @Size(max=120) String fullName){}
 @PutMapping("/me")
 public Profile updateProfile(Principal principal,@Valid @RequestBody UpdateProfile input){
  String name=input.fullName().trim();
  if(name.isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Name required");
  StudentAccount account=accounts.findByEmail(principal.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));
  account.setFullName(name);accounts.save(account);
  return new Profile(account.getFullName(),account.getEmail());
 }
 @GetMapping("/me") public Profile me(Principal principal){
  return accounts.findByEmail(principal.getName()).map(a->new Profile(a.getFullName(),a.getEmail()))
    .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
 }
}
