package com.faijan.interviewhub.dto;
import jakarta.validation.constraints.*;
public record QuestionRequest(
    @NotBlank @Size(max=500) String title,
    @NotBlank @Size(max=80) String technology,
    @NotBlank @Size(max=100) String category,
    @NotBlank @Pattern(regexp="(?i)Beginner|Intermediate|Advanced") String difficulty,
    @NotBlank @Size(max=10000) String explanation,
    @Size(max=20000) String codeExample,
    @Size(max=500) String optionA,@Size(max=500) String optionB,@Size(max=500) String optionC,@Size(max=500) String optionD,
    @Pattern(regexp="[ABCDabcd]", message="correctOption must be A, B, C or D") String correctOption
) {
    public boolean hasAnyOption(){return optionA!=null||optionB!=null||optionC!=null||optionD!=null||correctOption!=null;}
    public boolean hasCompleteMcq(){return optionA!=null&&!optionA.isBlank()&&optionB!=null&&!optionB.isBlank()&&optionC!=null&&!optionC.isBlank()&&optionD!=null&&!optionD.isBlank()&&correctOption!=null&&!correctOption.isBlank();}
}
