package com.imperial;
import jakarta.persistence.*;
@Entity
@Table(name="course_telegram_groups")
public class CourseTelegramGroup {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="course_id",nullable=false,unique=true) private Course course;
 @Column(nullable=false,length=512) private String inviteUrl;
 protected CourseTelegramGroup(){}
 public CourseTelegramGroup(Course course,String inviteUrl){this.course=course;this.inviteUrl=inviteUrl;}
 public Long getId(){return id;}
 public Course getCourse(){return course;}
 public String getInviteUrl(){return inviteUrl;}
 public void setInviteUrl(String value){inviteUrl=value;}
}
