package com.faijan.interviewhub.service;

import com.faijan.interviewhub.entity.AppUser;
import com.faijan.interviewhub.entity.Question;
import com.faijan.interviewhub.entity.UserRole;
import com.faijan.interviewhub.repository.QuestionBookmarkRepository;
import com.faijan.interviewhub.repository.QuestionRepository;
import com.faijan.interviewhub.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BookmarkServiceTest {
    @Test
    void savesBookmarkForTheAuthenticatedUser() {
        QuestionBookmarkRepository bookmarks = mock(QuestionBookmarkRepository.class);
        UserRepository users = mock(UserRepository.class);
        QuestionRepository questions = mock(QuestionRepository.class);
        AppUser user = new AppUser("Learner", "learner@example.com", "hash", UserRole.USER);
        Question question = new Question("Question", "Java", "Core Java", "Beginner", "Explanation", null);
        when(bookmarks.existsByUserEmailAndQuestionId("learner@example.com", 12L)).thenReturn(false);
        when(users.findByEmailIgnoreCase("learner@example.com")).thenReturn(Optional.of(user));
        when(questions.findById(12L)).thenReturn(Optional.of(question));

        new BookmarkService(bookmarks, users, questions).add("learner@example.com", 12L);

        verify(bookmarks).save(any());
    }

    @Test
    void addingAnExistingBookmarkIsIdempotent() {
        QuestionBookmarkRepository bookmarks = mock(QuestionBookmarkRepository.class);
        when(bookmarks.existsByUserEmailAndQuestionId("learner@example.com", 12L)).thenReturn(true);

        new BookmarkService(bookmarks, mock(UserRepository.class), mock(QuestionRepository.class))
            .add("learner@example.com", 12L);

        verify(bookmarks).existsByUserEmailAndQuestionId("learner@example.com", 12L);
        verifyNoMoreInteractions(bookmarks);
    }
}
