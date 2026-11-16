package com.imperial;
import jakarta.persistence.*;
@Entity
@Table(name="student_accounts",uniqueConstraints=@UniqueConstraint(columnNames="email"))
public class StudentAccount {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,length=120) private String fullName;
 @Column(nullable=false,unique=true,length=254) private String email;
 @Column(nullable=false,length=100) private String passwordHash;
 protected StudentAccount(){}
 public StudentAccount(String fullName,String email,String passwordHash){this.fullName=fullName;this.email=email;this.passwordHash=passwordHash;}
 public Long getId(){return id;}
 public String getFullName(){return fullName;}
 public String getEmail(){return email;}
 public String getPasswordHash(){return passwordHash;}
 public void setPasswordHash(String hash){this.passwordHash=hash;}
}
