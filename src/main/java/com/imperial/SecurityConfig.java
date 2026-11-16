package com.imperial;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
@Configuration
public class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
 @Bean UserDetailsService users(StudentAccountRepository accounts,@Value("${ADMIN_PASSWORD:}") String password,PasswordEncoder encoder){
  return username -> {
   if("admin".equals(username) && password!=null && password.length()>=12){
    return User.withUsername("admin").password(encoder.encode(password)).roles("ADMIN").build();
   }
   StudentAccount a=accounts.findByEmail(username.toLowerCase(java.util.Locale.ROOT))
     .orElseThrow(()->new UsernameNotFoundException("Account not found"));
   return User.withUsername(a.getEmail()).password(a.getPasswordHash()).roles("STUDENT").build();
  };
 }
 @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
  return http.authorizeHttpRequests(a->a
    .requestMatchers("/","/index.html","/style.css","/app.js","/register.html","/register.js","/actuator/health","/api/account/register","/api/account/csrf","/api/account/password/**","/forgot-password.html","/reset-password.html","/password.js","/policies.html","/faq.html","/api/session","/api/announcements","/api/visit").permitAll()
    .requestMatchers(org.springframework.http.HttpMethod.GET,"/api/courses","/api/courses/**").permitAll()
    .requestMatchers("/admin.html","/admin.js","/api/admin/**").hasRole("ADMIN")
    .requestMatchers("/api/payments/instructions").permitAll()
    .requestMatchers("/api/student/**","/checkout.html","/checkout.js").hasRole("STUDENT")
    .requestMatchers("/account.html","/account.js","/api/account/me").hasRole("STUDENT")
    .anyRequest().denyAll())
    .formLogin(f->f.successHandler((request,response,authentication)->{
     boolean admin=authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
     response.sendRedirect(admin?"/admin.html":"/account.html");
    }).permitAll())
    .exceptionHandling(e->e
     .defaultAuthenticationEntryPointFor((request,response,exception)->response.sendError(401),
       new org.springframework.security.web.util.matcher.AntPathRequestMatcher("/api/admin/**"))
     .defaultAccessDeniedHandlerFor((request,response,exception)->response.sendError(403),
       new org.springframework.security.web.util.matcher.AntPathRequestMatcher("/api/admin/**")))
    .logout(l->l.logoutSuccessUrl("/"))
    .build();
 }
}
