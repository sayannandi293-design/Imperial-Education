package com.imperial;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ManualPaymentRepository extends JpaRepository<ManualPayment,Long>{
 List<ManualPayment> findByStudent_IdOrderByCreatedAtDesc(Long studentId);
 List<ManualPayment> findAllByOrderByCreatedAtDesc();
 boolean existsByStudent_IdAndCourse_IdAndStatus(Long studentId,Long courseId,ManualPayment.Status status);
 boolean existsByUtrIgnoreCaseAndStatusNot(String utr,ManualPayment.Status status);
}
