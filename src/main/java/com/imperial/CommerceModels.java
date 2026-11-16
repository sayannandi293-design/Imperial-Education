package com.imperial;
import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="course_coupons")
class CourseCoupon {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
 @Column(nullable=false,unique=true,length=40) String code;
 @Column(nullable=false) int discountPercent;
 @Column(nullable=false) boolean active=true;
 @Column(nullable=false) int maxUses=0;
 @Column(nullable=false) int usedCount=0;
 protected CourseCoupon(){}
 CourseCoupon(String code,int percent,int max){this.code=code;this.discountPercent=percent;this.maxUses=max;}
}

@Entity @Table(name="site_announcements")
class SiteAnnouncement {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
 @Column(nullable=false,length=120) String title;
 @Column(nullable=false,length=500) String message;
 @Column(nullable=false) boolean active=true;
 protected SiteAnnouncement(){}
 SiteAnnouncement(String title,String message){this.title=title;this.message=message;}
}

@Entity @Table(name="course_wishlist",uniqueConstraints=@UniqueConstraint(columnNames={"student_id","course_id"}))
class CourseWishlist {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) StudentAccount student;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) Course course;
 protected CourseWishlist(){}
 CourseWishlist(StudentAccount student,Course course){this.student=student;this.course=course;}
}

@Entity @Table(name="course_reviews",uniqueConstraints=@UniqueConstraint(columnNames={"student_id","course_id"}))
class CourseReview {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) StudentAccount student;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) Course course;
 @Column(nullable=false) int rating;
 @Column(nullable=false,length=1000) String comment;
 @Column(nullable=false) boolean published=false;
 @Column(nullable=false) Instant createdAt=Instant.now();
 protected CourseReview(){}
 CourseReview(StudentAccount student,Course course,int rating,String comment){
  this.student=student;this.course=course;this.rating=rating;this.comment=comment;
 }
}

@Entity @Table(name="support_tickets")
class SupportTicket {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) StudentAccount student;
 @Column(nullable=false,length=150) String subject;
 @Column(nullable=false,length=3000) String message;
 @Column(nullable=false,length=3000) String adminReply="";
 @Column(nullable=false,length=16) String status="OPEN";
 @Column(nullable=false) Instant createdAt=Instant.now();
 protected SupportTicket(){}
 SupportTicket(StudentAccount student,String subject,String message){this.student=student;this.subject=subject;this.message=message;}
}

@Entity @Table(name="visitor_daily_counts",uniqueConstraints=@UniqueConstraint(columnNames="day"))
class VisitorDailyCount {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
 @Column(nullable=false,unique=true,length=10) String day;
 @Column(nullable=false) long views=0;
 protected VisitorDailyCount(){}
 VisitorDailyCount(String day){this.day=day;this.views=1;}
}
