package com.imperial;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface TicketChatMessageRepository extends JpaRepository<TicketChatMessage,Long>{
 List<TicketChatMessage> findByTicket_IdOrderByCreatedAtAscIdAsc(Long id);
}
