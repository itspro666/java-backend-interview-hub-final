package com.faijan.interviewhub.repository;
import com.faijan.interviewhub.entity.QuestionBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;
import java.util.Optional;
public interface QuestionBookmarkRepository extends JpaRepository<QuestionBookmark,Long> {
    @EntityGraph(attributePaths="question")
    List<QuestionBookmark> findByUserEmailOrderByCreatedAtDesc(String email);
    Optional<QuestionBookmark> findByUserEmailAndQuestionId(String email,Long questionId);
    boolean existsByUserEmailAndQuestionId(String email,Long questionId);
}
