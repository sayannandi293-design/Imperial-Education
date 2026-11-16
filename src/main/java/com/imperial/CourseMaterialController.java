package com.imperial;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController
public class CourseMaterialController {
 private final CourseMaterialRepository materials;
 private final CourseRepository courses;
 private final StudentAccountRepository students;
 private final ManualPaymentRepository payments;
 public CourseMaterialController(CourseMaterialRepository materials,CourseRepository courses,StudentAccountRepository students,ManualPaymentRepository payments){
  this.materials=materials;this.courses=courses;this.students=students;this.payments=payments;
 }
 public record MaterialInput(@NotBlank @Size(max=150) String title,@NotNull CourseMaterial.MaterialType type,
     @NotBlank @Size(max=2048) String url){}
 public record MaterialView(Long id,Long courseId,String title,String type,String url){}
 private MaterialView view(CourseMaterial material){
  return new MaterialView(material.getId(),material.getCourse().getId(),material.getTitle(),material.getType().name(),material.getContentUrl());
 }
 @GetMapping("/api/admin/courses/{courseId}/materials")
 @Transactional(readOnly=true)
 public List<MaterialView> adminList(@PathVariable Long courseId){
  if(!courses.existsById(courseId))throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  return materials.findByCourse_IdOrderByIdAsc(courseId).stream().map(this::view).toList();
 }
 @PostMapping("/api/admin/courses/{courseId}/materials")
 @ResponseStatus(HttpStatus.CREATED)
 @Transactional
 public MaterialView create(@PathVariable Long courseId,@Valid @RequestBody MaterialInput request){
  Course course=courses.findById(courseId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  String url=request.url().trim();
  try{
   URI uri=URI.create(url);
   if(!"https".equalsIgnoreCase(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null)
    throw new IllegalArgumentException();
  }catch(IllegalArgumentException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"An HTTPS URL is required");}
  return view(materials.save(new CourseMaterial(course,request.title().trim(),request.type(),url)));
 }
 @DeleteMapping("/api/admin/materials/{id}")
 @ResponseStatus(HttpStatus.NO_CONTENT)
 @Transactional
 public void remove(@PathVariable Long id){
  if(!materials.existsById(id))throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  materials.deleteById(id);
 }
 @GetMapping("/api/student/courses/{courseId}/materials")
 @Transactional(readOnly=true)
 public List<MaterialView> studentList(@PathVariable Long courseId,Principal principal){
  StudentAccount user=students.findByEmail(principal.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));
  if(!payments.existsByStudent_IdAndCourse_IdAndStatus(user.getId(),courseId,ManualPayment.Status.APPROVED))
   throw new ResponseStatusException(HttpStatus.FORBIDDEN,"This course has not been approved");
  return materials.findByCourse_IdOrderByIdAsc(courseId).stream().map(this::view).toList();
 }
}
