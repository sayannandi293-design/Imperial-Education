package com.imperial;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
@Table(name="notification_reads",uniqueConstraints=@UniqueConstraint(columnNames={"recipient_key","event_key"}))
public class NotificationRead {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="recipient_key",nullable=false,length=80) private String recipientKey;
 @Column(name="event_key",nullable=false,length=100) private String eventKey;
 @Column(nullable=false) private Instant readAt=Instant.now();
 protected NotificationRead(){}
 public NotificationRead(String recipientKey,String eventKey){
  this.recipientKey=recipientKey;this.eventKey=eventKey;
 }
 public String getEventKey(){return eventKey;}
}
