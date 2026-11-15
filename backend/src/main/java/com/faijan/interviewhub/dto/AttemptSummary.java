package com.faijan.interviewhub.dto;
import java.time.Instant;
public record AttemptSummary(Long id,String technology,String difficulty,int score,int totalQuestions,double percentage,boolean timedOut,String status,Instant startedAt,Instant completedAt) {}
