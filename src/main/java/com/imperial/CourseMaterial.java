package com.imperial;
import jakarta.persistence.*;
@Entity
@Table(name="course_materials")
public class CourseMaterial {
 public enum MaterialType { VIDEO, PDF }
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) private Course course;
 @Column(nullable=false,length=150) private String title;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=10) private MaterialType type;
 @Column(nullable=false,length=2048) private String contentUrl;
 protected CourseMaterial(){}
 public CourseMaterial(Course course,String title,MaterialType type,String url){this.course=course;this.title=title;this.type=type;this.contentUrl=url;}
 public Long getId(){return id;}
 public Course getCourse(){return course;}
 public String getTitle(){return title;}
 public MaterialType getType(){return type;}
 public String getContentUrl(){return contentUrl;}
}
