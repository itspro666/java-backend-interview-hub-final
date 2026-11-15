package com.faijan.interviewhub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.faijan.interviewhub.dto.PracticeReviewResponse;import com.faijan.interviewhub.dto.PracticeQuestionSnapshot;
import com.faijan.interviewhub.entity.*;
import com.faijan.interviewhub.repository.*;
import com.faijan.interviewhub.service.PracticeService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PracticeServiceTest {
    @Test void refusesToStartWhenQuestionPoolIsTooSmall() {
        QuestionRepository questions=mock(QuestionRepository.class);
        PracticeAttemptRepository attempts=mock(PracticeAttemptRepository.class);
        UserRepository users=mock(UserRepository.class);
        when(questions.findByActiveTrueAndCorrectOptionIsNotNull()).thenReturn(List.of());
        PracticeService service=new PracticeService(questions,attempts,users,new ObjectMapper());
        assertThrows(ResponseStatusException.class,()->service.start("learner@example.com",null,null,5,20));
        verifyNoInteractions(users,attempts);
    }

    @Test void scoresAnswersOnTheServerAndReturnsTheCorrectAnswerAfterSubmission() throws Exception {
        QuestionRepository questions=mock(QuestionRepository.class);
        PracticeAttemptRepository attempts=mock(PracticeAttemptRepository.class);
        UserRepository users=mock(UserRepository.class);
        AppUser user=new AppUser("Learner","learner@example.com","hash",UserRole.USER);
        Question q=new Question("Which annotation creates a REST controller?","Spring Boot","Spring MVC","Beginner","It combines controller and response body semantics.",null,"@Service","@RestController","@Repository","@Bean","B");
        PracticeAttempt attempt=new PracticeAttempt(user,"Spring Boot","Beginner",1,20,List.of(8L),new ObjectMapper().writeValueAsString(List.of(new PracticeQuestionSnapshot(8L,q.getTitle(),q.getTechnology(),q.getCategory(),q.getDifficulty(),q.getOptions(),q.revealCorrectOption(),q.getExplanation()))));
        when(attempts.findByIdAndUserEmail(3L,"learner@example.com")).thenReturn(Optional.of(attempt));
        PracticeService service=new PracticeService(questions,attempts,users,new ObjectMapper());
        service.saveAnswers("learner@example.com",3L,Map.of(8L,"B"));
        PracticeReviewResponse result=service.submit("learner@example.com",3L);
        assertEquals(1,result.score());
        assertEquals(100.0,result.percentage());
        assertTrue(result.review().get(0).correct());
        assertEquals("B",result.review().get(0).correctOption());
        assertEquals("COMPLETED",attempt.getStatus());
        verifyNoInteractions(questions); // Submission is scored against the immutable exam-time snapshot.
    }

    @Test void refusesAnswersOutsideTheAttemptQuestionSet() {
        QuestionRepository questions=mock(QuestionRepository.class);
        PracticeAttemptRepository attempts=mock(PracticeAttemptRepository.class);
        UserRepository users=mock(UserRepository.class);
        AppUser user=new AppUser("Learner","learner@example.com","hash",UserRole.USER);
        PracticeAttempt attempt=new PracticeAttempt(user,"Mixed","Mixed",1,20,List.of(8L),"[]");
        when(attempts.findByIdAndUserEmail(3L,"learner@example.com")).thenReturn(Optional.of(attempt));
        PracticeService service=new PracticeService(questions,attempts,users,new ObjectMapper());
        assertThrows(ResponseStatusException.class,()->service.saveAnswers("learner@example.com",3L,Map.of(999L,"A")));
        verifyNoInteractions(questions);
    }
    @Test void rejectsAnAnswerOptionOutsideTheFourChoices() {
        QuestionRepository questions=mock(QuestionRepository.class);
        PracticeAttemptRepository attempts=mock(PracticeAttemptRepository.class);
        UserRepository users=mock(UserRepository.class);
        AppUser user=new AppUser("Learner","learner@example.com","hash",UserRole.USER);
        PracticeAttempt attempt=new PracticeAttempt(user,"Java","Beginner",1,20,List.of(8L),"[]");
        when(attempts.findByIdAndUserEmail(3L,"learner@example.com")).thenReturn(Optional.of(attempt));
        PracticeService service=new PracticeService(questions,attempts,users,new ObjectMapper());
        assertThrows(ResponseStatusException.class,()->service.saveAnswers("learner@example.com",3L,Map.of(8L,"E")));
    }

    @Test void resumesOwnedExamWithPreviouslySavedAnswers() throws Exception {
        QuestionRepository questions=mock(QuestionRepository.class);
        PracticeAttemptRepository attempts=mock(PracticeAttemptRepository.class);
        UserRepository users=mock(UserRepository.class);
        AppUser user=new AppUser("Learner","learner@example.com","hash",UserRole.USER);
        Question q=new Question("Question","Java","Core Java","Beginner","Explanation",null,"One","Two","Three","Four","B");
        ObjectMapper mapper=new ObjectMapper();
        String snapshot=mapper.writeValueAsString(List.of(new PracticeQuestionSnapshot(8L,q.getTitle(),q.getTechnology(),q.getCategory(),q.getDifficulty(),q.getOptions(),q.revealCorrectOption(),q.getExplanation())));
        PracticeAttempt attempt=new PracticeAttempt(user,"Java","Beginner",1,20,List.of(8L),snapshot);
        attempt.recordAnswers(mapper.writeValueAsString(Map.of(8L,"B")));
        when(attempts.findByIdAndUserEmail(3L,"learner@example.com")).thenReturn(Optional.of(attempt));

        var result=new PracticeService(questions,attempts,users,mapper).resume("learner@example.com",3L);

        assertEquals("B",result.savedAnswers().get(8L));
        assertEquals(1,result.questions().size());
        verifyNoInteractions(questions);
    }

    @Test void finalizesAnExpiredSessionBeforeReturningConflictOnResume() throws Exception {
        QuestionRepository questions=mock(QuestionRepository.class);
        PracticeAttemptRepository attempts=mock(PracticeAttemptRepository.class);
        UserRepository users=mock(UserRepository.class);
        AppUser user=new AppUser("Learner","learner@example.com","hash",UserRole.USER);
        Question q=new Question("Question","Java","Core Java","Beginner","Explanation",null,"One","Two","Three","Four","B");
        ObjectMapper mapper=new ObjectMapper();
        String snapshot=mapper.writeValueAsString(List.of(new PracticeQuestionSnapshot(8L,q.getTitle(),q.getTechnology(),q.getCategory(),q.getDifficulty(),q.getOptions(),q.revealCorrectOption(),q.getExplanation())));
        PracticeAttempt attempt=new PracticeAttempt(user,"Java","Beginner",1,20,List.of(8L),snapshot);
        attempt.recordAnswers(mapper.writeValueAsString(Map.of(8L,"B")));
        org.springframework.test.util.ReflectionTestUtils.setField(attempt,"deadlineAt",java.time.Instant.now().minusSeconds(1));
        when(attempts.findByIdAndUserEmail(4L,"learner@example.com")).thenReturn(Optional.of(attempt));

        assertThrows(ResponseStatusException.class,()->new PracticeService(questions,attempts,users,mapper).resume("learner@example.com",4L));
        assertEquals("COMPLETED",attempt.getStatus());
        assertTrue(attempt.isTimedOut());
        assertEquals(1,attempt.getScore());
    }

    @Test void doesNotExposeAnotherLearnersAttempt() {
        QuestionRepository questions=mock(QuestionRepository.class);
        PracticeAttemptRepository attempts=mock(PracticeAttemptRepository.class);
        UserRepository users=mock(UserRepository.class);
        when(attempts.findByIdAndUserEmail(3L,"other@example.com")).thenReturn(Optional.empty());
        PracticeService service=new PracticeService(questions,attempts,users,new ObjectMapper());
        assertThrows(ResponseStatusException.class,()->service.submit("other@example.com",3L));
    }

}
