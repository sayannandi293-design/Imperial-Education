package com.imperial;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="password_reset_tokens")
public class PasswordResetToken {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) private StudentAccount student;
 @Column(nullable=false,length=64,unique=true) private String tokenHash;
 @Column(nullable=false) private Instant expiresAt;
 @Column(nullable=false) private boolean used=false;
 protected PasswordResetToken(){}
 public PasswordResetToken(StudentAccount student,String hash,Instant expiry){this.student=student;this.tokenHash=hash;this.expiresAt=expiry;}
 public StudentAccount getStudent(){return student;}
 public Instant getExpiresAt(){return expiresAt;}
 public boolean isUsed(){return used;}
 public void markUsed(){used=true;}
}
