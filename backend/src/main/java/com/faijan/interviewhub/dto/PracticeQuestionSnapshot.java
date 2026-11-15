package com.faijan.interviewhub.dto;
import java.util.Map;
/** Immutable exam-time copy; stored privately with the attempt so later admin edits cannot change its scoring. */
public record PracticeQuestionSnapshot(Long id,String title,String technology,String category,String difficulty,Map<String,String> options,String correctOption,String explanation) {}
