package com.faijan.interviewhub.dto;
import com.faijan.interviewhub.entity.Question;
import java.time.Instant;
public record AdminQuestionView(Long id,String title,String technology,String category,String difficulty,String explanation,String codeExample,java.util.Map<String,String> options,String correctOption,boolean active,Instant createdAt) {
 public static AdminQuestionView from(Question q){return new AdminQuestionView(q.getId(),q.getTitle(),q.getTechnology(),q.getCategory(),q.getDifficulty(),q.getExplanation(),q.getCodeExample(),q.getOptions(),q.revealCorrectOption(),q.isActive(),q.getCreatedAt());}
}
