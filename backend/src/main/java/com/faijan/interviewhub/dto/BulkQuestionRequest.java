package com.faijan.interviewhub.dto;
import jakarta.validation.Valid;import jakarta.validation.constraints.NotEmpty;import jakarta.validation.constraints.Size;import java.util.List;
public record BulkQuestionRequest(@NotEmpty @Size(max=100) List<@Valid QuestionRequest> questions) {}
