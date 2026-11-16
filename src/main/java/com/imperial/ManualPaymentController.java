package com.imperial;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
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
 public ManualPaymentController(ManualPaymentRepository payments,StudentAccountRepository students,CourseRepository courses,
  @Value("${UPI_ID:}") String upiId,@Value("${UPI_PAYEE_NAME:IMPERIAL EDUCATION}") String payee,
  @Value("${UPI_QR_IMAGE_URL:}") String qrImageUrl){
  this.payments=payments;this.students=students;this.courses=courses;
  this.upiId=upiId.trim();this.payee=payee.trim();this.qrImageUrl=qrImageUrl.trim();
 }
 public record PaymentView(Long id,Long courseId,String courseTitle,String studentName,String studentEmail,long amountInPaise,String utr,String status,Instant createdAt,Instant reviewedAt){}
 public record PaymentInstructions(String upiId,String payeeName,String qrImageUrl){}
 public record Submission(Long courseId,@NotBlank @Size(min=6,max=100) String utr){}
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
  return view(payments.saveAndFlush(new ManualPayment(user,course,course.getPriceInPaise(),utr)));
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
  p.review(ManualPayment.Status.APPROVED);return view(payments.save(p));
 }
 @PostMapping("/api/admin/payments/{id}/reject")
 @Transactional
 public PaymentView reject(@PathVariable Long id){
  ManualPayment p=payments.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  if(p.getStatus()!=ManualPayment.Status.PENDING)throw new ResponseStatusException(HttpStatus.CONFLICT,"This request was already reviewed");
  p.review(ManualPayment.Status.REJECTED);return view(payments.save(p));
 }
}
