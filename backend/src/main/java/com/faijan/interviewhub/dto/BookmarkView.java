package com.faijan.interviewhub.dto;
import com.faijan.interviewhub.entity.Question;
import java.time.Instant;
public record BookmarkView(Long bookmarkId,Instant savedAt,Question question) {}
