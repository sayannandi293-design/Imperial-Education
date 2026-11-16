package com.imperial;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
@Table(name="manual_payments")
public class ManualPayment {
 public enum Status { PENDING, APPROVED, REJECTED }
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) private StudentAccount student;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) private Course course;
 @Column(nullable=false) private long amountInPaise;
 @Column(nullable=false,length=100) private String utr;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) private Status status=Status.PENDING;
 @Column(nullable=false) private Instant createdAt=Instant.now();
 private Instant reviewedAt;
 protected ManualPayment(){}
 public ManualPayment(StudentAccount student,Course course,long amountInPaise,String utr){
  this.student=student;this.course=course;this.amountInPaise=amountInPaise;this.utr=utr;
 }
 public Long getId(){return id;}
 public StudentAccount getStudent(){return student;}
 public Course getCourse(){return course;}
 public long getAmountInPaise(){return amountInPaise;}
 public String getUtr(){return utr;}
 public Status getStatus(){return status;}
 public Instant getCreatedAt(){return createdAt;}
 public Instant getReviewedAt(){return reviewedAt;}
 public void review(Status state){this.status=state;this.reviewedAt=Instant.now();}
}
