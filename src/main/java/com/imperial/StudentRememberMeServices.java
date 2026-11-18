package com.imperial;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

/** Student logins get persistent sessions by default, without persistent admin cookies. */
public class StudentRememberMeServices extends PersistentTokenBasedRememberMeServices {
 public StudentRememberMeServices(String key,UserDetailsService users,PersistentTokenRepository tokens){
  super(key,users,tokens);
  setCookieName("IE_REMEMBER");
  setUseSecureCookie(true);
  setTokenValiditySeconds(60*60*24*365);
 }
 @Override
 protected boolean rememberMeRequested(HttpServletRequest request,String parameter){
  // Only the application's student login should be sticky. Admin sessions
  // remain ordinary browser sessions unless the admin explicitly opts in.
  String username=request.getParameter("username");
  return username!=null&&!username.trim().equalsIgnoreCase("admin");
 }
}
