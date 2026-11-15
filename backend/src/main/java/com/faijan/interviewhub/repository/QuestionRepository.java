package com.faijan.interviewhub.repository;
import com.faijan.interviewhub.entity.Question;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
public interface QuestionRepository extends JpaRepository<Question,Long>, JpaSpecificationExecutor<Question> {
    Page<Question> findByActiveTrue(Pageable pageable);
    List<Question> findByActiveTrueAndCorrectOptionIsNotNull();
    long countByActiveTrue();
    boolean existsByTitleIgnoreCase(String title);
    boolean existsByTitleIgnoreCaseAndIdNot(String title, Long id);
}
