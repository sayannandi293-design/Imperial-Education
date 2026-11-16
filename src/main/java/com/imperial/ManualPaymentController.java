package com.imperial;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController
public class ManualPaymentController {
 private final ManualPaymentRepository payments;
 private final StudentAccountRepository students;
 private final CourseRepository courses;
 private final String upiId;
 private final String payee;
 private final String qrImageUrl;
 private final CouponRepository coupons;
 private final JavaMailSender mailer;
 private final boolean mailEnabled;
 private final String mailFrom;
 public ManualPaymentController(ManualPaymentRepository payments,StudentAccountRepository students,CourseRepository courses,CouponRepository coupons,JavaMailSender mailer,
  @Value("${UPI_ID:}") String upiId,@Value("${UPI_PAYEE_NAME:IMPERIAL EDUCATION}") String payee,
  @Value("${UPI_QR_IMAGE_URL:}") String qrImageUrl,@Value("${MAIL_ENABLED:false}") boolean mailEnabled,@Value("${MAIL_FROM:}") String mailFrom){
  this.payments=payments;this.students=students;this.courses=courses;
  this.upiId=upiId.trim();this.payee=payee.trim();this.qrImageUrl=qrImageUrl.trim();
  this.coupons=coupons;this.mailer=mailer;this.mailEnabled=mailEnabled;this.mailFrom=mailFrom;
 }
 public record PaymentView(Long id,Long courseId,String courseTitle,String studentName,String studentEmail,long amountInPaise,String utr,String status,Instant createdAt,Instant reviewedAt){}
 public record PaymentInstructions(String upiId,String payeeName,String qrImageUrl){}
 public record Submission(Long courseId,@NotBlank @Size(min=6,max=100) String utr,String couponCode){}
 private PaymentView view(ManualPayment p){
  return new PaymentView(p.getId(),p.getCourse().getId(),p.getCourse().getTitle(),p.getStudent().getFullName(),
   p.getStudent().getEmail(),p.getAmountInPaise(),p.getUtr(),p.getStatus().name(),p.getCreatedAt(),p.getReviewedAt());
 }
 private StudentAccount student(Principal principal){
  return students.findByEmail(principal.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));
 }
 @GetMapping("/api/payments/instructions")
 public PaymentInstructions instructions(){
  return new PaymentInstructions(upiId,payee,qrImageUrl);
 }
 @GetMapping("/api/student/payments")
 @Transactional(readOnly=true)
 public List<PaymentView> mine(Principal principal){
  return payments.findByStudent_IdOrderByCreatedAtDesc(student(principal).getId()).stream().map(this::view).toList();
 }
 @PostMapping("/api/student/payments")
 @ResponseStatus(HttpStatus.CREATED)
 @Transactional
 public PaymentView submit(Principal principal,@Valid @RequestBody Submission submission){
  if(upiId.isBlank())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"UPI payment has not been configured");
  StudentAccount user=student(principal);
  Course course=courses.findById(submission.courseId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Course not found"));
  if(payments.existsByStudent_IdAndCourse_IdAndStatus(user.getId(),course.getId(),ManualPayment.Status.APPROVED))
   throw new ResponseStatusException(HttpStatus.CONFLICT,"Already enrolled");
  if(payments.existsByStudent_IdAndCourse_IdAndStatus(user.getId(),course.getId(),ManualPayment.Status.PENDING))
   throw new ResponseStatusException(HttpStatus.CONFLICT,"A payment is already awaiting review for this course");
  String utr=submission.utr().trim().replaceAll("\\s+","");
  if(!utr.matches("[A-Za-z0-9]{6,35}"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Enter a valid UTR/reference number");
  if(payments.existsByUtrIgnoreCaseAndStatusNot(utr,ManualPayment.Status.REJECTED))
   throw new ResponseStatusException(HttpStatus.CONFLICT,"This reference number was already submitted");
  String code=submission.couponCode()==null?"":submission.couponCode().trim().toUpperCase(Locale.ROOT);
  long amount=course.getPriceInPaise();
  CourseCoupon used=null;
  if(!code.isEmpty()){
   used=coupons.lockByCode(code).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid coupon"));
   if(!used.active||(used.maxUses>0&&used.usedCount>=used.maxUses))
    throw new ResponseStatusException(HttpStatus.CONFLICT,"Coupon is not available");
   amount=amount*(100-used.discountPercent)/100;
   if(amount<=0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid discounted amount");
  }
  ManualPayment request=new ManualPayment(user,course,amount,utr);
  if(used!=null){request.setCouponCode(code);used.usedCount++;coupons.save(used);}
  return view(payments.saveAndFlush(request));
 }
 @GetMapping("/api/student/my-courses")
 @Transactional(readOnly=true)
 public List<Map<String,Object>> myCourses(Principal principal){
  return payments.findByStudent_IdOrderByCreatedAtDesc(student(principal).getId()).stream()
   .filter(p->p.getStatus()==ManualPayment.Status.APPROVED)
   .map(p->Map.<String,Object>of("courseId",p.getCourse().getId(),"title",p.getCourse().getTitle(),"amountInPaise",p.getAmountInPaise()))
   .distinct().toList();
 }
 @GetMapping("/api/admin/payments")
 @Transactional(readOnly=true)
 public List<PaymentView> all(){return payments.findAllByOrderByCreatedAtDesc().stream().map(this::view).toList();}
 @PostMapping("/api/admin/payments/{id}/approve")
 @Transactional
 public PaymentView approve(@PathVariable Long id){
  ManualPayment p=payments.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  if(p.getStatus()!=ManualPayment.Status.PENDING)throw new ResponseStatusException(HttpStatus.CONFLICT,"This request was already reviewed");
  if(payments.existsByStudent_IdAndCourse_IdAndStatus(p.getStudent().getId(),p.getCourse().getId(),ManualPayment.Status.APPROVED))
   throw new ResponseStatusException(HttpStatus.CONFLICT,"Student is already enrolled");
  p.review(ManualPayment.Status.APPROVED);PaymentView result=view(payments.save(p));notifyStudent(p,"APPROVED");return result;
 }
 @PostMapping("/api/admin/payments/{id}/reject")
 @Transactional
 public PaymentView reject(@PathVariable Long id){
  ManualPayment p=payments.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  if(p.getStatus()!=ManualPayment.Status.PENDING)throw new ResponseStatusException(HttpStatus.CONFLICT,"This request was already reviewed");
  p.review(ManualPayment.Status.REJECTED);PaymentView result=view(payments.save(p));notifyStudent(p,"REJECTED");return result;
 }
 private void notifyStudent(ManualPayment p,String state){
  if(!mailEnabled||mailFrom.isBlank())return;
  try{
   SimpleMailMessage message=new SimpleMailMessage();
   message.setFrom(mailFrom);message.setTo(p.getStudent().getEmail());
   message.setSubject("IMPERIAL EDUCATION — Order IE-"+p.getId()+" "+state);
   message.setText("Your order IE-"+p.getId()+" for "+p.getCourse().getTitle()+" is "+state+
    ".\nAmount: INR "+String.format(java.util.Locale.ROOT,"%.2f",p.getAmountInPaise()/100.0)+
    "\nSign in to My Account for your payment status and Telegram course access.\n"+
    "If you need help, contact https://t.me/sanakhan1i");
   mailer.send(message);
  }catch(Exception ignored){/* Email delivery does not change payment approval. */}
 }
 @GetMapping("/api/student/payments/{id}/receipt")
 @Transactional(readOnly=true)
 public Map<String,Object> receipt(Principal principal,@PathVariable Long id){
  StudentAccount student=student(principal);
  ManualPayment p=payments.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  if(!p.getStudent().getId().equals(student.getId()))throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  return Map.of("orderId","IE-"+p.getId(),"course",p.getCourse().getTitle(),
   "amountInPaise",p.getAmountInPaise(),"status",p.getStatus().name(),
   "utr",p.getUtr(),"submittedAt",p.getCreatedAt().toString(),
   "reviewedAt",p.getReviewedAt()==null?"":p.getReviewedAt().toString(),
   "student",p.getStudent().getFullName());
 }
}
