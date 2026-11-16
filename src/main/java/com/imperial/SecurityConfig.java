package com.imperial;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
@Configuration
public class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
 @Bean UserDetailsService users(@Value("${ADMIN_PASSWORD:}") String password, PasswordEncoder encoder){
  var manager = new InMemoryUserDetailsManager();
  if(password == null || password.length() < 12){
   System.err.println("ADMIN_PASSWORD missing or too short (minimum 12 characters); admin login is disabled.");
  } else {
   manager.createUser(User.withUsername("admin").password(encoder.encode(password)).roles("ADMIN").build());
  }
  return manager;
 }
 @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
  return http.authorizeHttpRequests(a -> a
    .requestMatchers("/", "/index.html", "/style.css", "/app.js", "/actuator/health").permitAll()
    .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/courses", "/api/courses/**").permitAll()
    .requestMatchers("/admin.html", "/admin.js", "/api/admin/**").hasRole("ADMIN")
    .anyRequest().denyAll())
    .formLogin(f -> f.defaultSuccessUrl("/admin.html",true).permitAll())
    .logout(l -> l.logoutSuccessUrl("/"))
    .build();
 }
}