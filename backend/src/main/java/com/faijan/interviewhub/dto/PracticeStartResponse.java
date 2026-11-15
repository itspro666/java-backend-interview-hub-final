package com.faijan.interviewhub.dto;
import java.time.Instant;import java.util.List;import java.util.Map;
public record PracticeStartResponse(Long attemptId,String technology,String difficulty,int durationMinutes,Instant startedAt,Instant deadlineAt,List<PracticeQuestion> questions,Map<Long,String> savedAnswers) {
 public record PracticeQuestion(Long id,String title,String technology,String category,String difficulty,List<OptionView> options) {}
 public record OptionView(String key,String text) {}
}
