package com.imperial;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
@Entity
public class Course {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @NotBlank private String title;
 @Column(length=3000) private String description;
 @Min(0) private long priceInPaise;
 private String imageUrl;
 public Long getId(){return id;}
 public void setId(Long value){id=value;}
 public String getTitle(){return title;}
 public void setTitle(String x){title=x;}
 public String getDescription(){return description;}
 public void setDescription(String x){description=x;}
 public long getPriceInPaise(){return priceInPaise;}
 public void setPriceInPaise(long x){priceInPaise=x;}
 public String getImageUrl(){return imageUrl;}
 public void setImageUrl(String x){imageUrl=x;}
}
