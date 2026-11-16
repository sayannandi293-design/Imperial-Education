package com.imperial;
import java.util.List;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/courses")
public class CourseController {
 private final CourseRepository courses;
 public CourseController(CourseRepository courses){this.courses=courses;}
 @GetMapping public List<Course> all(){return courses.findAll();}
 @GetMapping("/{id}") public Course one(@PathVariable Long id){return courses.findById(id).orElseThrow(()->new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));}
}
