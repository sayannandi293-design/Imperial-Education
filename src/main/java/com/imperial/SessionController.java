package com.imperial;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;
@RestController
public class SessionController {
 @GetMapping("/api/session")
 public Map<String,Object> session(Authentication auth,@RequestAttribute("_csrf") CsrfToken csrf){
  boolean signedIn=auth!=null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
  boolean admin=signedIn && auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ADMIN"));
  return Map.of("signedIn",signedIn,"role",signedIn?(admin?"ADMIN":"STUDENT"):"GUEST",
                "csrfHeader",csrf.getHeaderName(),"csrfToken",csrf.getToken());
 }
}
