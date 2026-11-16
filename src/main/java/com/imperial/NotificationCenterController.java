package com.imperial;
import java.security.Principal;
import java.time.Instant;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController
public class NotificationCenterController {
 private final StudentAccountRepository students;
 private final ManualPaymentRepository payments;
 private final SupportTicketRepository tickets;
 private final TicketChatMessageRepository chats;
 private final NotificationReadRepository reads;
 private final CouponRepository coupons;
 public NotificationCenterController(StudentAccountRepository students,ManualPaymentRepository payments,
  SupportTicketRepository tickets,TicketChatMessageRepository chats,NotificationReadRepository reads,CouponRepository coupons){
  this.students=students;this.payments=payments;this.tickets=tickets;this.chats=chats;this.reads=reads;this.coupons=coupons;
 }
 public record Notice(String id,String title,String message,String category,String target,Instant occurredAt,boolean read){}
 public record NoticeList(long unreadCount,List<Notice> items){}
 public record MarkInput(String id){}
 private StudentAccount current(Principal p){
  return students.findByEmail(p.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));
 }
 private String studentKey(Long studentId){return "STUDENT:"+studentId;}
 private List<Notice> available(String recipient){
  List<Notice> events=new ArrayList<>();
  if("ADMIN".equals(recipient)){
   for(ManualPayment pay:payments.findAllByOrderByCreatedAtDesc()){
    if(pay.getStatus()==ManualPayment.Status.PENDING)
     events.add(new Notice("payment-"+pay.getId(),"New payment request",
       pay.getStudent().getFullName()+" submitted a UPI reference for "+pay.getCourse().getTitle(),
       "PAYMENT","/admin.html#admin-manual-upi-payment-requests-11",pay.getCreatedAt(),false));
   }
   for(SupportTicket t:tickets.findAllByOrderByCreatedAtDesc()){
    events.add(new Notice("ticket-"+t.id,"New support ticket",
      t.student.getFullName()+": "+t.subject,"SUPPORT","/admin.html#admin-support-tickets-8",t.createdAt,false));
    for(TicketChatMessage m:chats.findByTicket_IdOrderByCreatedAtAscIdAsc(t.id)){
     if("STUDENT".equals(m.getSender()))
      events.add(new Notice("chat-"+m.getId(),"Student support message",
        t.student.getFullName()+": "+shortText(m.getBody()),"SUPPORT","/admin.html#admin-support-tickets-8",m.getCreatedAt(),false));
    }
   }
  }else{
   Long id=Long.valueOf(recipient.substring("STUDENT:".length()));
   for(CourseCoupon c:coupons.findAll()){
    if(c.active && (c.maxUses==0||c.usedCount<c.maxUses) && c.publishedAt!=null){
     events.add(new Notice("offer-"+c.id,"New course offer: "+c.discountPercent+"% OFF",
      "Use coupon "+c.code+" at checkout to get "+c.discountPercent+"% discount on eligible courses.",
      "OFFER","/account.html#notifications",c.publishedAt,false));
    }
   }
   for(ManualPayment pay:payments.findByStudent_IdOrderByCreatedAtDesc(id)){
    String state=pay.getStatus().name();
    String title=switch(state){case "APPROVED"->"Payment approved — course unlocked";case "REJECTED"->"Payment request rejected";default->"Payment awaiting review";};
    String body=pay.getCourse().getTitle()+" · Order IE-"+pay.getId();
    events.add(new Notice("payment-"+pay.getId()+"-"+state,title,body,"PAYMENT",
      "APPROVED".equals(state)?"/account.html#courses":"/account.html#payments",
      pay.getReviewedAt()!=null?pay.getReviewedAt():pay.getCreatedAt(),false));
   }
   for(SupportTicket t:tickets.findByStudent_IdOrderByCreatedAtDesc(id)){
    if(t.adminReply!=null&&!t.adminReply.isBlank())
     events.add(new Notice("legacy-reply-"+t.id,"Reply from support",
       t.subject+": "+shortText(t.adminReply),"SUPPORT","/account.html#support",t.createdAt,false));
    for(TicketChatMessage m:chats.findByTicket_IdOrderByCreatedAtAscIdAsc(t.id)){
     if("ADMIN".equals(m.getSender()))
      events.add(new Notice("chat-"+m.getId(),"New reply from support",
        t.subject+": "+shortText(m.getBody()),"SUPPORT","/account.html#support",m.getCreatedAt(),false));
    }
   }
  }
  return events;
 }
 private String shortText(String input){
  String value=input==null?"":input.replaceAll("\\s+"," ").trim();
  return value.length()>110?value.substring(0,110)+"…":value;
 }
 private NoticeList list(String recipient){
  Set<String> seen=new HashSet<>();
  for(NotificationRead r:reads.findByRecipientKey(recipient))seen.add(r.getEventKey());
  List<Notice> items=available(recipient).stream()
   .sorted(Comparator.comparing(Notice::occurredAt).reversed())
   .limit(100)
   .map(n->new Notice(n.id(),n.title(),n.message(),n.category(),n.target(),n.occurredAt(),seen.contains(n.id())))
   .toList();
  return new NoticeList(items.stream().filter(n->!n.read()).count(),items);
 }
 @GetMapping("/api/student/notifications") @Transactional(readOnly=true)
 public NoticeList mine(Principal principal){return list(studentKey(current(principal).getId()));}
 @GetMapping("/api/admin/notifications") @Transactional(readOnly=true)
 public NoticeList admin(){return list("ADMIN");}
 private void mark(String recipient,MarkInput input){
  String id=input.id()==null?"":input.id().trim();
  if(id.length()>100||!id.matches("[a-zA-Z0-9_-]+"))
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid notification");
  if(available(recipient).stream().noneMatch(n->n.id().equals(id)))
   throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Notification not found");
  if(!reads.existsByRecipientKeyAndEventKey(recipient,id)){
   try{reads.saveAndFlush(new NotificationRead(recipient,id));}
   catch(DataIntegrityViolationException ignored){/* A second tab may have marked it already. */}
  }
 }
 @PostMapping("/api/student/notifications/read") @Transactional
 public Map<String,Boolean> studentRead(Principal principal,@RequestBody MarkInput input){
  mark(studentKey(current(principal).getId()),input);return Map.of("read",true);
 }
 @PostMapping("/api/admin/notifications/read") @Transactional
 public Map<String,Boolean> adminRead(@RequestBody MarkInput input){
  mark("ADMIN",input);return Map.of("read",true);
 }
}
