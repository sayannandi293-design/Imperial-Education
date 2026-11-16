package com.imperial;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api/admin")
public class AdminController {
 private final CourseRepository repository;
 public AdminController(CourseRepository repository) { this.repository=repository; }
 @GetMapping("/csrf")
 public Map<String,String> csrf(@RequestAttribute("_csrf") CsrfToken token) {
  return Map.of("headerName",token.getHeaderName(),"token",token.getToken());
 }
 @GetMapping("/courses") public List<Course> all(){return repository.findAll();}
 @PostMapping("/courses") @ResponseStatus(HttpStatus.CREATED)
 public Course create(@Valid @RequestBody Course input){
  input.setId(null);
  return repository.save(input);
 }
 @PutMapping("/courses/{id}") public Course update(@PathVariable Long id, @Valid @RequestBody Course input){
  Course existing=repository.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  existing.setTitle(input.getTitle());
  existing.setDescription(input.getDescription());
  existing.setPriceInPaise(input.getPriceInPaise());
  existing.setImageUrl(input.getImageUrl());
  return repository.save(existing);
 }
 @DeleteMapping("/courses/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
 public void delete(@PathVariable Long id){
  if(!repository.existsById(id))throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  repository.deleteById(id);
 }
}