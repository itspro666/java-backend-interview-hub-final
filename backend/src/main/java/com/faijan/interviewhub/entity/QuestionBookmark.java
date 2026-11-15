package com.faijan.interviewhub.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="question_bookmarks", uniqueConstraints=@UniqueConstraint(name="uk_bookmark_user_question",columnNames={"user_id","question_id"}), indexes=@Index(name="idx_bookmark_user",columnList="user_id"))
public class QuestionBookmark {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false) private AppUser user;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="question_id",nullable=false) private Question question;
    @Column(nullable=false,updatable=false) private Instant createdAt=Instant.now();
    protected QuestionBookmark(){} public QuestionBookmark(AppUser user,Question question){this.user=user;this.question=question;}
    public Long getId(){return id;} public Question getQuestion(){return question;} public Instant getCreatedAt(){return createdAt;}
}
