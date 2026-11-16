package com.imperial;
import java.util.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/admin")
public class AdminInsightsController {
 private final StudentAccountRepository students;
 private final CourseRepository courses;
 private final ManualPaymentRepository payments;
 public AdminInsightsController(StudentAccountRepository students,CourseRepository courses,ManualPaymentRepository payments){
  this.students=students;this.courses=courses;this.payments=payments;
 }
 public record StudentView(Long id,String fullName,String email,long approvedCourses,long pendingRequests){}
 @GetMapping("/students")
 @Transactional(readOnly=true)
 public List<StudentView> students(){
  List<ManualPayment> all=payments.findAll();
  return students.findAll().stream().map(s->new StudentView(s.getId(),s.getFullName(),s.getEmail(),
   all.stream().filter(p->p.getStudent().getId().equals(s.getId())&&p.getStatus()==ManualPayment.Status.APPROVED).map(p->p.getCourse().getId()).distinct().count(),
   all.stream().filter(p->p.getStudent().getId().equals(s.getId())&&p.getStatus()==ManualPayment.Status.PENDING).count())).toList();
 }
 @GetMapping("/insights")
 @Transactional(readOnly=true)
 public Map<String,Object> insights(){
  List<ManualPayment> all=payments.findAll();
  long approved=all.stream().filter(p->p.getStatus()==ManualPayment.Status.APPROVED).count();
  long pending=all.stream().filter(p->p.getStatus()==ManualPayment.Status.PENDING).count();
  long rejected=all.stream().filter(p->p.getStatus()==ManualPayment.Status.REJECTED).count();
  long revenue=all.stream().filter(p->p.getStatus()==ManualPayment.Status.APPROVED).mapToLong(ManualPayment::getAmountInPaise).sum();
  return Map.of("students",students.count(),"courses",courses.count(),"approved",approved,"pending",pending,"rejected",rejected,"approvedRevenueInPaise",revenue);
 }
}
