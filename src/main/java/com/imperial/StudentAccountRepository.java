package com.imperial;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface StudentAccountRepository extends JpaRepository<StudentAccount,Long>{
 Optional<StudentAccount> findByEmail(String email);
 boolean existsByEmail(String email);
}
