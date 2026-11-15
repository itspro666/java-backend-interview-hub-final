package com.faijan.interviewhub;

import com.faijan.interviewhub.dto.QuestionRequest;
import com.faijan.interviewhub.entity.Question;
import com.faijan.interviewhub.repository.QuestionRepository;
import com.faijan.interviewhub.service.QuestionService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QuestionServiceTest {
    @Test void rejectsIncompleteMcq() {
        QuestionRepository repository=mock(QuestionRepository.class);
        @SuppressWarnings("unchecked") KafkaTemplate<String,Object> kafka=mock(KafkaTemplate.class);
        QuestionService service=new QuestionService(repository,kafka);
        QuestionRequest request=new QuestionRequest("Question","Java","Core Java","Beginner","Explanation",null,"A",null,null,null,"A");
        ResponseStatusException error=assertThrows(ResponseStatusException.class,()->service.create(request));
        assertEquals(HttpStatus.BAD_REQUEST,error.getStatusCode());
        verifyNoInteractions(repository);
    }

    @Test void softDeleteMarksQuestionInactive() {
        QuestionRepository repository=mock(QuestionRepository.class);
        @SuppressWarnings("unchecked") KafkaTemplate<String,Object> kafka=mock(KafkaTemplate.class);
        Question q=new Question("Question","Java","Core Java","Beginner","Explanation",null);
        when(repository.findById(4L)).thenReturn(Optional.of(q));
        QuestionService service=new QuestionService(repository,kafka);
        service.delete(4L);
        assertFalse(q.isActive());
        verify(repository).findById(4L);
        verify(repository,never()).deleteById(anyLong());
    }

    @Test void rejectsMcqWithDuplicateOptions() {
        QuestionRepository repository=mock(QuestionRepository.class);
        @SuppressWarnings("unchecked") KafkaTemplate<String,Object> kafka=mock(KafkaTemplate.class);
        QuestionService service=new QuestionService(repository,kafka);
        QuestionRequest request=new QuestionRequest("Question","Java","Core Java","Beginner","Explanation",null,"Same","Same","Third","Fourth","A");
        ResponseStatusException error=assertThrows(ResponseStatusException.class,()->service.create(request));
        assertEquals(HttpStatus.BAD_REQUEST,error.getStatusCode());
        verifyNoInteractions(repository);
    }

    @Test void rejectsDuplicateQuestionTitle() {
        QuestionRepository repository=mock(QuestionRepository.class);
        @SuppressWarnings("unchecked") KafkaTemplate<String,Object> kafka=mock(KafkaTemplate.class);
        when(repository.existsByTitleIgnoreCase("Existing question")).thenReturn(true);
        QuestionService service=new QuestionService(repository,kafka);
        QuestionRequest request=new QuestionRequest("Existing question","Java","Core Java","Beginner","Explanation",null,null,null,null,null,null);
        ResponseStatusException error=assertThrows(ResponseStatusException.class,()->service.create(request));
        assertEquals(HttpStatus.CONFLICT,error.getStatusCode());
        verify(repository,never()).save(any(Question.class));
    }

    @Test void rejectsOversizedPageRequest() {
        QuestionRepository repository=mock(QuestionRepository.class);
        @SuppressWarnings("unchecked") KafkaTemplate<String,Object> kafka=mock(KafkaTemplate.class);
        QuestionService service=new QuestionService(repository,kafka);
        assertThrows(ResponseStatusException.class,()->service.search(null,null,null,null,0,100));
        verifyNoInteractions(repository);
    }
}
