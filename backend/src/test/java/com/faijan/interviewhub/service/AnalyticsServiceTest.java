package com.faijan.interviewhub.service;

import com.faijan.interviewhub.dto.LearningAnalytics;
import com.faijan.interviewhub.entity.AppUser;
import com.faijan.interviewhub.entity.PracticeAttempt;
import com.faijan.interviewhub.entity.QuestionBookmark;
import com.faijan.interviewhub.entity.UserRole;
import com.faijan.interviewhub.repository.PracticeAttemptRepository;
import com.faijan.interviewhub.repository.QuestionBookmarkRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AnalyticsServiceTest {
    @Test
    void calculatesCompletedAttemptAveragesAndBookmarkCount() {
        PracticeAttemptRepository attempts = mock(PracticeAttemptRepository.class);
        QuestionBookmarkRepository bookmarks = mock(QuestionBookmarkRepository.class);
        AppUser user = new AppUser("Learner", "learner@example.com", "hash", UserRole.USER);
        PracticeAttempt java = new PracticeAttempt(user, "Java", "Beginner", 10, 20, List.of(1L), "[]");
        java.complete(8, false, "[]");
        PracticeAttempt spring = new PracticeAttempt(user, "Spring Boot", "Intermediate", 10, 20, List.of(2L), "[]");
        spring.complete(4, false, "[]");
        PracticeAttempt inProgress = new PracticeAttempt(user, "Kafka", "Beginner", 10, 20, List.of(3L), "[]");
        when(attempts.findByUserEmailOrderByStartedAtDesc("learner@example.com")).thenReturn(List.of(java, spring, inProgress));
        when(bookmarks.findByUserEmailOrderByCreatedAtDesc("learner@example.com")).thenReturn(List.of(mock(QuestionBookmark.class), mock(QuestionBookmark.class)));

        LearningAnalytics result = new AnalyticsService(attempts, bookmarks).forUser("learner@example.com");

        assertEquals(2, result.totalAttempts());
        assertEquals(20, result.totalExamQuestions());
        assertEquals(60.0, result.averagePercentage());
        assertEquals(80.0, result.bestPercentage());
        assertEquals(2, result.bookmarks());
        assertEquals(2, result.technologyProgress().size());
    }
}
