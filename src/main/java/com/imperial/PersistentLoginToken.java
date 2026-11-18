package com.imperial;

import jakarta.persistence.*;
import java.time.Instant;

/** A hashed-free random bearer token is stored only server-side in PostgreSQL. */
@Entity
@Table(name="persistent_login_tokens",indexes=@Index(name="idx_persistent_login_user",columnList="username"))
public class PersistentLoginToken {
 @Id @Column(nullable=false,length=64) private String series;
 @Column(nullable=false,length=254) private String username;
 @Column(nullable=false,length=128) private String tokenValue;
 @Column(nullable=false) private Instant lastUsed;
 protected PersistentLoginToken(){}
 PersistentLoginToken(String username,String series,String tokenValue,Instant lastUsed){
  this.username=username;this.series=series;this.tokenValue=tokenValue;this.lastUsed=lastUsed;
 }
 public String getSeries(){return series;}
 public String getUsername(){return username;}
 public String getTokenValue(){return tokenValue;}
 public Instant getLastUsed(){return lastUsed;}
 public void update(String tokenValue,Instant lastUsed){this.tokenValue=tokenValue;this.lastUsed=lastUsed;}
}
