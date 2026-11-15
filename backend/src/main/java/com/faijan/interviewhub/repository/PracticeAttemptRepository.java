package com.faijan.interviewhub.repository;
import com.faijan.interviewhub.entity.PracticeAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface PracticeAttemptRepository extends JpaRepository<PracticeAttempt,Long> {
    List<PracticeAttempt> findByUserEmailOrderByStartedAtDesc(String email);
    Optional<PracticeAttempt> findByIdAndUserEmail(Long id,String email);
    long countByUserEmail(String email);
}
