package com.imperial;

import java.util.Date;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Persist remember-me logins in the app's existing PostgreSQL database, surviving Render restarts. */
@Component
public class DatabaseRememberMeTokenRepository implements PersistentTokenRepository {
 private final PersistentLoginTokenRepository tokens;
 public DatabaseRememberMeTokenRepository(PersistentLoginTokenRepository tokens){this.tokens=tokens;}
 @Override @Transactional
 public void createNewToken(PersistentRememberMeToken token){
  tokens.save(new PersistentLoginToken(token.getUsername(),token.getSeries(),
   token.getTokenValue(),token.getDate().toInstant()));
 }
 @Override @Transactional
 public void updateToken(String series,String tokenValue,Date lastUsed){
  tokens.findById(series).ifPresent(token->{token.update(tokenValue,lastUsed.toInstant());tokens.save(token);});
 }
 @Override @Transactional(readOnly=true)
 public PersistentRememberMeToken getTokenForSeries(String series){
  return tokens.findById(series).map(t->new PersistentRememberMeToken(t.getUsername(),
   t.getSeries(),t.getTokenValue(),Date.from(t.getLastUsed()))).orElse(null);
 }
 @Override @Transactional
 public void removeUserTokens(String username){tokens.deleteAllByUsername(username);}
}
