package com.imperial;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CourseMaterialRepository extends JpaRepository<CourseMaterial,Long> {
 List<CourseMaterial> findByCourse_IdOrderByIdAsc(Long courseId);
}
