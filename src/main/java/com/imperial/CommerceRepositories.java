package com.imperial;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

interface CouponRepository extends JpaRepository<CourseCoupon,Long> {
 Optional<CourseCoupon> findByCodeIgnoreCase(String code);
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @org.springframework.data.jpa.repository.Query("select c from CourseCoupon c where upper(c.code)=upper(:code)")
 Optional<CourseCoupon> lockByCode(@org.springframework.data.repository.query.Param("code") String code);
}
interface AnnouncementRepository extends JpaRepository<SiteAnnouncement,Long>{}
interface WishlistRepository extends JpaRepository<CourseWishlist,Long> {
 List<CourseWishlist> findByStudent_Id(Long studentId);
 Optional<CourseWishlist> findByStudent_IdAndCourse_Id(Long studentId,Long courseId);
}
interface ReviewRepository extends JpaRepository<CourseReview,Long> {
 Optional<CourseReview> findByStudent_IdAndCourse_Id(Long studentId,Long courseId);
 List<CourseReview> findByCourse_IdAndPublishedTrueOrderByCreatedAtDesc(Long courseId);
}
interface SupportTicketRepository extends JpaRepository<SupportTicket,Long> {
 List<SupportTicket> findByStudent_IdOrderByCreatedAtDesc(Long studentId);
 List<SupportTicket> findAllByOrderByCreatedAtDesc();
}
interface VisitorDailyCountRepository extends JpaRepository<VisitorDailyCount,Long> {
 Optional<VisitorDailyCount> findByDay(String day);
 @org.springframework.data.jpa.repository.Modifying
 @org.springframework.data.jpa.repository.Query(value="INSERT INTO visitor_daily_counts (day, views) VALUES (:day, 1) ON CONFLICT (day) DO UPDATE SET views = visitor_daily_counts.views + 1",nativeQuery=true)
 void incrementOrInsert(@org.springframework.data.repository.query.Param("day") String day);
}
