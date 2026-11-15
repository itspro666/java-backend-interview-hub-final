package com.faijan.interviewhub.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="practice_attempts", indexes={@Index(name="idx_attempt_user_started",columnList="user_id,started_at"),@Index(name="idx_attempt_status",columnList="status")})
public class PracticeAttempt {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false) private AppUser user;
    @Column(nullable=false,length=80) private String technology;
    @Column(nullable=false,length=20) private String difficulty;
    @Column(nullable=false) private int totalQuestions;
    @Column(nullable=false) private int durationMinutes;
    @Column(nullable=false,length=20) private String status="IN_PROGRESS";
    @Column(nullable=false,updatable=false) private Instant startedAt=Instant.now();
    @Column(nullable=false) private Instant deadlineAt;
    private Instant completedAt;
    @Column(nullable=false) private int score;
    @Column(nullable=false) private boolean timedOut;
    @Column(columnDefinition="LONGTEXT") private String reviewJson;
    @Column(nullable=false,columnDefinition="TEXT") private String questionIdsCsv;
    @Column(nullable=false,columnDefinition="LONGTEXT") private String answersJson="{}";
    @Column(nullable=false,columnDefinition="LONGTEXT") private String questionSnapshotJson="[]";
    protected PracticeAttempt(){}
    public PracticeAttempt(AppUser user,String technology,String difficulty,int totalQuestions,int durationMinutes,java.util.List<Long> questionIds,String questionSnapshotJson){this.user=user;this.technology=technology;this.difficulty=difficulty;this.totalQuestions=totalQuestions;this.durationMinutes=durationMinutes;this.questionIdsCsv=questionIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));this.questionSnapshotJson=questionSnapshotJson;this.deadlineAt=startedAt.plusSeconds(durationMinutes*60L);}
    public Long getId(){return id;} public String getTechnology(){return technology;} public String getDifficulty(){return difficulty;} public int getTotalQuestions(){return totalQuestions;} public int getDurationMinutes(){return durationMinutes;} public String getStatus(){return status;} public Instant getStartedAt(){return startedAt;} public Instant getDeadlineAt(){return deadlineAt;} public Instant getCompletedAt(){return completedAt;} public int getScore(){return score;} public boolean isTimedOut(){return timedOut;} public String getReviewJson(){return reviewJson;} public String getAnswersJson(){return answersJson;} public String getQuestionSnapshotJson(){return questionSnapshotJson;} public java.util.List<Long> questionIds(){return java.util.Arrays.stream(questionIdsCsv.split(",")).filter(v->!v.isBlank()).map(Long::valueOf).toList();}
    public boolean belongsTo(String email){return user.getEmail().equalsIgnoreCase(email);}
    public void recordAnswers(String answersJson){this.answersJson=answersJson;}
    public void complete(int score,boolean timedOut,String reviewJson){this.score=score;this.timedOut=timedOut;this.reviewJson=reviewJson;this.completedAt=Instant.now();this.status="COMPLETED";}
}
