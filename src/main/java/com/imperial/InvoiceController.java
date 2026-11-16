package com.imperial;
import java.security.Principal;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController
public class InvoiceController {
 private final ManualPaymentRepository payments;
 private final StudentAccountRepository students;
 private final String sellerName;
 private final String sellerAddress;
 private final String sellerEmail;
 public InvoiceController(ManualPaymentRepository payments,StudentAccountRepository students,
 @Value("${INVOICE_SELLER_NAME:IMPERIAL EDUCATION}") String name,
 @Value("${INVOICE_SELLER_ADDRESS:}") String address,
 @Value("${INVOICE_SELLER_EMAIL:}") String email){
  this.payments=payments;this.students=students;
  this.sellerName=name;this.sellerAddress=address;this.sellerEmail=email;
 }
 public record InvoiceView(String invoiceNumber,String sellerName,String sellerAddress,
 String sellerEmail,String studentName,String studentEmail,String courseTitle,
 long amountInPaise,String utr,Instant paymentSubmittedAt,Instant approvedAt,String note){}
 @GetMapping("/api/student/payments/{id}/invoice") @Transactional(readOnly=true)
 public InvoiceView invoice(@PathVariable Long id,Principal principal){
  StudentAccount current=students.findByEmail(principal.getName())
   .orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));
  ManualPayment p=payments.findById(id)
   .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  if(!p.getStudent().getId().equals(current.getId()))throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  if(p.getStatus()!=ManualPayment.Status.APPROVED)
   throw new ResponseStatusException(HttpStatus.CONFLICT,"Invoice available after payment approval");
  return new InvoiceView("IE-"+p.getId(),sellerName,sellerAddress,sellerEmail,
   current.getFullName(),current.getEmail(),p.getCourse().getTitle(),
   p.getAmountInPaise(),p.getUtr(),p.getCreatedAt(),p.getReviewedAt(),
   "Payment record only. Not a GST tax invoice. Check seller tax and statutory invoice requirements.");
 }
}
