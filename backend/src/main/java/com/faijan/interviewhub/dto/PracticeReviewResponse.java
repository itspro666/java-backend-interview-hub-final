package com.faijan.interviewhub.dto;
import java.time.Instant;import java.util.List;
public record PracticeReviewResponse(Long attemptId,String technology,String difficulty,int score,int totalQuestions,double percentage,boolean timedOut,Instant completedAt,List<AnswerReview> review) {
 public record AnswerReview(Long questionId,String title,String selectedOption,String correctOption,boolean correct,String explanation) {}
}
