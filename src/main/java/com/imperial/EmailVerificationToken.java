package com.imperial;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="email_verification_tokens")
public class EmailVerificationToken {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) private StudentAccount student;
 @Column(nullable=false,unique=true,length=64) private String tokenHash;
 @Column(nullable=false) private Instant expiresAt;
 @Column(nullable=false) private boolean used=false;
 protected EmailVerificationToken(){}
 public EmailVerificationToken(StudentAccount s,String h,Instant expiry){student=s;tokenHash=h;expiresAt=expiry;}
 public StudentAccount getStudent(){return student;}
 public Instant getExpiresAt(){return expiresAt;}
 public boolean isUsed(){return used;}
 public void markUsed(){used=true;}
}
