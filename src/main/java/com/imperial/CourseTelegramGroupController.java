package com.imperial;
import java.net.URI;
import java.security.Principal;
import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController
public class CourseTelegramGroupController {
 private final CourseTelegramGroupRepository groups;
 private final CourseRepository courses;
 private final StudentAccountRepository students;
 private final ManualPaymentRepository payments;
 public CourseTelegramGroupController(CourseTelegramGroupRepository groups,CourseRepository courses,StudentAccountRepository students,ManualPaymentRepository payments){
  this.groups=groups;this.courses=courses;this.students=students;this.payments=payments;
 }
 public record TelegramInput(@NotBlank @Size(max=512) String inviteUrl){}
 public record TelegramView(String inviteUrl){}
 private String validated(String raw){
  String input=raw.trim();
  URI uri;
  try{uri=URI.create(input);}catch(IllegalArgumentException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid Telegram link");}
  String host=uri.getHost();
  if(!"https".equalsIgnoreCase(uri.getScheme())||host==null||uri.getUserInfo()!=null||uri.getPort()!=-1||
    !("t.me".equalsIgnoreCase(host)||"telegram.me".equalsIgnoreCase(host))||
    uri.getFragment()!=null||uri.getRawQuery()!=null||uri.getPath()==null||uri.getPath().length()<2)
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use a valid https://t.me/... Telegram link");
  return input;
 }
 @GetMapping("/api/admin/courses/{courseId}/telegram")
 @Transactional(readOnly=true)
 public TelegramView adminGet(@PathVariable Long courseId){
  if(!courses.existsById(courseId))throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  return new TelegramView(groups.findByCourse_Id(courseId).map(CourseTelegramGroup::getInviteUrl).orElse(""));
 }
 @PutMapping("/api/admin/courses/{courseId}/telegram")
 @Transactional
 public TelegramView adminSave(@PathVariable Long courseId,@Valid @RequestBody TelegramInput body){
  Course course=courses.findById(courseId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  String url=validated(body.inviteUrl());
  CourseTelegramGroup group=groups.findByCourse_Id(courseId).orElseGet(()->new CourseTelegramGroup(course,url));
  group.setInviteUrl(url);groups.save(group);return new TelegramView(url);
 }
 @DeleteMapping("/api/admin/courses/{courseId}/telegram")
 @ResponseStatus(HttpStatus.NO_CONTENT)
 @Transactional
 public void adminDelete(@PathVariable Long courseId){
  groups.findByCourse_Id(courseId).ifPresent(groups::delete);
 }
 @GetMapping("/api/student/courses/{courseId}/telegram")
 @Transactional(readOnly=true)
 public TelegramView studentGet(@PathVariable Long courseId,Principal principal){
  StudentAccount student=students.findByEmail(principal.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));
  if(!payments.existsByStudent_IdAndCourse_IdAndStatus(student.getId(),courseId,ManualPayment.Status.APPROVED))
    throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Payment approval required");
  return new TelegramView(groups.findByCourse_Id(courseId).map(CourseTelegramGroup::getInviteUrl).orElse(""));
 }
}
