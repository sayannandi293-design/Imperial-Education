package com.imperial;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
@Configuration
public class SecurityConfig {
 @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
  return http.authorizeHttpRequests(a -> a
    .requestMatchers("/","/index.html","/style.css","/app.js","/api/courses","/api/courses/**","/actuator/health").permitAll()
    .anyRequest().authenticated()).formLogin(f -> f.permitAll()).build();
 }
}
