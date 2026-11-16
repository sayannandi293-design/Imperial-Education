package com.imperial;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
@Table(name="support_ticket_chat_messages")
public class TicketChatMessage {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) private SupportTicket ticket;
 @Column(nullable=false,length=10) private String sender;
 @Column(nullable=false,length=3000) private String body;
 @Column(nullable=false) private Instant createdAt=Instant.now();
 protected TicketChatMessage(){}
 public TicketChatMessage(SupportTicket ticket,String sender,String body){
  this.ticket=ticket;this.sender=sender;this.body=body;
 }
 public Long getId(){return id;}
 public String getSender(){return sender;}
 public String getBody(){return body;}
 public Instant getCreatedAt(){return createdAt;}
}
