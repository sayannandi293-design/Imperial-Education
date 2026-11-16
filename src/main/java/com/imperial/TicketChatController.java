package com.imperial;
import java.security.Principal;
import java.time.Instant;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController
public class TicketChatController {
 private final SupportTicketRepository tickets;
 private final TicketChatMessageRepository messages;
 private final StudentAccountRepository students;
 public TicketChatController(SupportTicketRepository tickets,TicketChatMessageRepository messages,StudentAccountRepository students){
  this.tickets=tickets;this.messages=messages;this.students=students;
 }
 public record ChatMessage(String id,String sender,String body,Instant createdAt){}
 public record MessageInput(@NotBlank @Size(max=3000) String body){}
 private SupportTicket find(Long id,Principal principal,boolean admin){
  SupportTicket t=tickets.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Ticket not found"));
  if(!admin){
   Long userId=students.findByEmail(principal.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED)).getId();
   if(!t.student.getId().equals(userId))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Ticket not found");
  }
  return t;
 }
 private List<ChatMessage> history(SupportTicket t){
  List<ChatMessage> items=new ArrayList<>();
  items.add(new ChatMessage("initial-"+t.id,"STUDENT",t.message,t.createdAt));
  if(t.adminReply!=null&&!t.adminReply.isBlank())
   items.add(new ChatMessage("legacy-reply-"+t.id,"ADMIN",t.adminReply,t.createdAt));
  for(TicketChatMessage m:messages.findByTicket_IdOrderByCreatedAtAscIdAsc(t.id))
   items.add(new ChatMessage(String.valueOf(m.getId()),m.getSender(),m.getBody(),m.getCreatedAt()));
  return items;
 }
 @GetMapping("/api/student/tickets/{id}/messages") @Transactional(readOnly=true)
 public List<ChatMessage> studentHistory(@PathVariable Long id,Principal principal){
  return history(find(id,principal,false));
 }
 @GetMapping("/api/admin/tickets/{id}/messages") @Transactional(readOnly=true)
 public List<ChatMessage> adminHistory(@PathVariable Long id,Principal principal){
  return history(find(id,principal,true));
 }
 private ChatMessage send(Long id,Principal principal,boolean admin,MessageInput input){
  SupportTicket ticket=find(id,principal,admin);
  String body=input.body().trim();
  if(body.isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Message is required");
  TicketChatMessage result=messages.save(new TicketChatMessage(ticket,admin?"ADMIN":"STUDENT",body));
  if(admin){ticket.status="RESOLVED";}
  else ticket.status="OPEN";
  tickets.save(ticket);
  return new ChatMessage(String.valueOf(result.getId()),result.getSender(),result.getBody(),result.getCreatedAt());
 }
 @PostMapping("/api/student/tickets/{id}/messages") @ResponseStatus(HttpStatus.CREATED) @Transactional
 public ChatMessage studentSend(@PathVariable Long id,Principal principal,@Valid @RequestBody MessageInput input){
  return send(id,principal,false,input);
 }
 @PostMapping("/api/admin/tickets/{id}/messages") @ResponseStatus(HttpStatus.CREATED) @Transactional
 public ChatMessage adminSend(@PathVariable Long id,Principal principal,@Valid @RequestBody MessageInput input){
  return send(id,principal,true,input);
 }
}
