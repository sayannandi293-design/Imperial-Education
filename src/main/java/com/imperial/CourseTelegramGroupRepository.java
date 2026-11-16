package com.imperial;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CourseTelegramGroupRepository extends JpaRepository<CourseTelegramGroup,Long> {
 Optional<CourseTelegramGroup> findByCourse_Id(Long courseId);
}
