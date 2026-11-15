package com.faijan.interviewhub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.faijan.interviewhub.entity.Question;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QuestionSecuritySerializationTest {
    @Test void publicQuestionJsonDoesNotExposeCorrectOption() throws Exception {
        Question q=new Question("What is DI?","Spring Boot","Spring Core","Beginner","The container injects dependencies.",null,"A","B","C","D","B");
        ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();
        JsonNode json=mapper.readTree(mapper.writeValueAsString(q));
        assertFalse(json.has("correctOption"));
        assertEquals("A",json.path("options").path("A").asText());
        assertEquals(4,json.path("options").size());
    }
}
