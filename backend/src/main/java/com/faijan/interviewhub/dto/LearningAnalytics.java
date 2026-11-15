package com.faijan.interviewhub.dto;
import java.util.List;
public record LearningAnalytics(long totalAttempts,long totalExamQuestions,double averagePercentage,double bestPercentage,long bookmarks,List<TechnologyProgress> technologyProgress) {
 public record TechnologyProgress(String technology,long attempts,long questionsAnswered,double averagePercentage,double bestPercentage) {}
}
