package com.imperial;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController
public class CommerceController {
 private final StudentAccountRepository students;
 private final CourseRepository courses;
 private final ManualPaymentRepository payments;
 private final CouponRepository coupons;
 private final AnnouncementRepository announcements;
 private final WishlistRepository wishlists;
 private final ReviewRepository reviews;
 private final SupportTicketRepository tickets;
 private final VisitorDailyCountRepository visitors;
 public CommerceController(StudentAccountRepository students,CourseRepository courses,ManualPaymentRepository payments,CouponRepository coupons,
 AnnouncementRepository announcements,WishlistRepository wishlists,ReviewRepository reviews,SupportTicketRepository tickets,VisitorDailyCountRepository visitors){
  this.students=students;this.courses=courses;this.payments=payments;this.coupons=coupons;this.announcements=announcements;
  this.wishlists=wishlists;this.reviews=reviews;this.tickets=tickets;this.visitors=visitors;
 }
 private StudentAccount user(Principal p){return students.findByEmail(p.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));}
 public record CouponInput(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{3,40}") String code,@Min(1) @Max(90) int percent,@Min(0) int maxUses){}
 public record CouponView(Long id,String code,int percent,int maxUses,int usedCount,boolean active){}
 private CouponView cv(CourseCoupon c){return new CouponView(c.id,c.code,c.discountPercent,c.maxUses,c.usedCount,c.active);}
 @GetMapping("/api/admin/coupons") public List<CouponView> coupons(){return coupons.findAll().stream().map(this::cv).toList();}
 @PostMapping("/api/admin/coupons") @ResponseStatus(HttpStatus.CREATED)
 public CouponView couponCreate(@Valid @RequestBody CouponInput input){
  if(coupons.findByCodeIgnoreCase(input.code()).isPresent())throw new ResponseStatusException(HttpStatus.CONFLICT,"Coupon already exists");
  return cv(coupons.save(new CourseCoupon(input.code().toUpperCase(Locale.ROOT),input.percent(),input.maxUses())));
 }
 @PostMapping("/api/admin/coupons/{id}/toggle")
 public CouponView couponToggle(@PathVariable Long id){
  CourseCoupon c=coupons.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  c.active=!c.active;return cv(coupons.save(c));
 }
 public record CouponQuote(long amountInPaise,int percent,String code){}
 @GetMapping("/api/student/coupon-quote")
 public CouponQuote quote(@RequestParam Long courseId,@RequestParam String code){
  Course course=courses.findById(courseId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  CourseCoupon c=coupons.findByCodeIgnoreCase(code.trim()).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Coupon not available"));
  if(!c.active||(c.maxUses>0&&c.usedCount>=c.maxUses))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Coupon expired");
  return new CouponQuote(course.getPriceInPaise()*(100-c.discountPercent)/100,c.discountPercent,c.code);
 }
 public record AnnouncementInput(@NotBlank @Size(max=120) String title,@NotBlank @Size(max=500) String message){}
 public record AnnouncementView(Long id,String title,String message,boolean active){}
 private AnnouncementView av(SiteAnnouncement a){return new AnnouncementView(a.id,a.title,a.message,a.active);}
 @GetMapping("/api/announcements") public List<AnnouncementView> activeAnnouncements(){
  return announcements.findAll().stream().filter(a->a.active).map(this::av).toList();
 }
 @GetMapping("/api/admin/announcements") public List<AnnouncementView> allAnnouncements(){return announcements.findAll().stream().map(this::av).toList();}
 @PostMapping("/api/admin/announcements") @ResponseStatus(HttpStatus.CREATED)
 public AnnouncementView createAnnouncement(@Valid @RequestBody AnnouncementInput input){
  return av(announcements.save(new SiteAnnouncement(input.title().trim(),input.message().trim())));
 }
 @PostMapping("/api/admin/announcements/{id}/toggle")
 public AnnouncementView toggleAnnouncement(@PathVariable Long id){
  SiteAnnouncement a=announcements.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  a.active=!a.active;return av(announcements.save(a));
 }
 public record WishlistView(Long courseId,String title,long priceInPaise){}
 @GetMapping("/api/student/wishlist") @Transactional(readOnly=true)
 public List<WishlistView> wishlist(Principal p){
  return wishlists.findByStudent_Id(user(p).getId()).stream().map(w->new WishlistView(w.course.getId(),w.course.getTitle(),w.course.getPriceInPaise())).toList();
 }
 @PostMapping("/api/student/wishlist/{courseId}")
 @Transactional
 public Map<String,Boolean> addWishlist(Principal p,@PathVariable Long courseId){
  StudentAccount s=user(p);
  Course c=courses.findById(courseId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  if(wishlists.findByStudent_IdAndCourse_Id(s.getId(),c.getId()).isEmpty())wishlists.save(new CourseWishlist(s,c));
  return Map.of("saved",true);
 }
 @DeleteMapping("/api/student/wishlist/{courseId}")
 @Transactional
 public void removeWishlist(Principal p,@PathVariable Long courseId){
  wishlists.findByStudent_IdAndCourse_Id(user(p).getId(),courseId).ifPresent(wishlists::delete);
 }
 public record ReviewInput(@Min(1) @Max(5) int rating,@NotBlank @Size(max=1000) String comment){}
 public record ReviewView(Long id,String studentName,int rating,String comment,String createdAt){}
 private ReviewView rv(CourseReview r){return new ReviewView(r.id,r.student.getFullName(),r.rating,r.comment,r.createdAt.toString());}
 @GetMapping("/api/courses/{courseId}/reviews") @Transactional(readOnly=true)
 public List<ReviewView> publicReviews(@PathVariable Long courseId){
  return reviews.findByCourse_IdAndPublishedTrueOrderByCreatedAtDesc(courseId).stream().map(this::rv).toList();
 }
 @PostMapping("/api/student/courses/{courseId}/review") @Transactional
 public Map<String,String> submitReview(Principal p,@PathVariable Long courseId,@Valid @RequestBody ReviewInput input){
  StudentAccount s=user(p);
  if(!payments.existsByStudent_IdAndCourse_IdAndStatus(s.getId(),courseId,ManualPayment.Status.APPROVED))
   throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only enrolled students can leave reviews");
  if(reviews.findByStudent_IdAndCourse_Id(s.getId(),courseId).isPresent())
   throw new ResponseStatusException(HttpStatus.CONFLICT,"You already reviewed this course");
  Course course=courses.findById(courseId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  reviews.save(new CourseReview(s,course,input.rating(),input.comment().trim()));
  return Map.of("message","Review submitted for moderation");
 }
 public record ModeratedReview(Long id,String course,String student,int rating,String comment,boolean published){}
 @GetMapping("/api/admin/reviews") @Transactional(readOnly=true)
 public List<ModeratedReview> adminReviews(){
  return reviews.findAll().stream().map(r->new ModeratedReview(r.id,r.course.getTitle(),r.student.getFullName(),r.rating,r.comment,r.published)).toList();
 }
 @PostMapping("/api/admin/reviews/{id}/toggle") public Map<String,Boolean> moderate(@PathVariable Long id){
  CourseReview r=reviews.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  r.published=!r.published;reviews.save(r);return Map.of("published",r.published);
 }
 public record TicketInput(@NotBlank @Size(max=150) String subject,@NotBlank @Size(max=3000) String message){}
 public record TicketView(Long id,String subject,String message,String status,String adminReply,String studentEmail,String createdAt){}
 private TicketView tv(SupportTicket t){return new TicketView(t.id,t.subject,t.message,t.status,t.adminReply,t.student.getEmail(),t.createdAt.toString());}
 @PostMapping("/api/student/tickets") @ResponseStatus(HttpStatus.CREATED) @Transactional
 public TicketView submitTicket(Principal p,@Valid @RequestBody TicketInput input){
  return tv(tickets.save(new SupportTicket(user(p),input.subject().trim(),input.message().trim())));
 }
 @GetMapping("/api/student/tickets") @Transactional(readOnly=true)
 public List<TicketView> myTickets(Principal p){return tickets.findByStudent_IdOrderByCreatedAtDesc(user(p).getId()).stream().map(this::tv).toList();}
 @GetMapping("/api/admin/tickets") @Transactional(readOnly=true)
 public List<TicketView> adminTickets(){return tickets.findAllByOrderByCreatedAtDesc().stream().map(this::tv).toList();}
 public record TicketReply(@NotBlank @Size(max=3000) String reply){}
 @PostMapping("/api/admin/tickets/{id}/reply") @Transactional public TicketView reply(@PathVariable Long id,@Valid @RequestBody TicketReply input){
  SupportTicket t=tickets.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  t.adminReply=input.reply().trim();t.status="RESOLVED";return tv(tickets.save(t));
 }
 @GetMapping("/api/visit") @Transactional public Map<String,Boolean> countVisit(){
  visitors.incrementOrInsert(LocalDate.now(ZoneOffset.UTC).toString());
  return Map.of("recorded",true);
 }
 @GetMapping("/api/admin/visitors")
 public Map<String,Object> visitorStats(){
  return Map.of("pageViews",visitors.findAll().stream().mapToLong(v->v.views).sum(),
   "lastDays",visitors.findAll().stream().sorted(Comparator.comparing((VisitorDailyCount v)->v.day).reversed())
     .limit(14).map(v->Map.of("date",v.day,"views",v.views)).toList());
 }
}
