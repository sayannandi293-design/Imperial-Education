package com.imperial;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface NotificationReadRepository extends JpaRepository<NotificationRead,Long>{
 List<NotificationRead> findByRecipientKey(String recipientKey);
 boolean existsByRecipientKeyAndEventKey(String recipientKey,String eventKey);
}
