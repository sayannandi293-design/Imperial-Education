package com.imperial;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PersistentLoginTokenRepository extends JpaRepository<PersistentLoginToken,String> {
 @Modifying
 @Query("delete from PersistentLoginToken t where t.username = :username")
 void deleteAllByUsername(@Param("username") String username);
}
