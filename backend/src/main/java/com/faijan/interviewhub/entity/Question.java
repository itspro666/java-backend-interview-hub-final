package com.faijan.interviewhub.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(name="questions", indexes={@Index(name="idx_question_category_level", columnList="category,difficulty"), @Index(name="idx_question_technology", columnList="technology"), @Index(name="idx_question_active", columnList="active")})
public class Question implements java.io.Serializable {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=500) private String title;
    @Column(nullable=false,length=80) private String technology;
    @Column(nullable=false,length=100) private String category;
    @Column(nullable=false,length=20) private String difficulty;
    @Column(nullable=false,columnDefinition="TEXT") private String explanation;
    @Column(columnDefinition="TEXT") private String codeExample;
    @Column(length=500) private String optionA;
    @Column(length=500) private String optionB;
    @Column(length=500) private String optionC;
    @Column(length=500) private String optionD;
    @JsonIgnore @Column(length=1) private String correctOption;
    @Column(nullable=false) private boolean active=true;
    @Column(nullable=false,updatable=false) private Instant createdAt=Instant.now();
    public Question() {}
    public Question(String title,String technology,String category,String difficulty,String explanation,String codeExample){this(title,technology,category,difficulty,explanation,codeExample,null,null,null,null,null);}
    public Question(String title,String technology,String category,String difficulty,String explanation,String codeExample,String a,String b,String c,String d,String answer){this.title=title;this.technology=technology;this.category=category;this.difficulty=difficulty;this.explanation=explanation;this.codeExample=codeExample;this.optionA=a;this.optionB=b;this.optionC=c;this.optionD=d;this.correctOption=answer;}
    public Long getId(){return id;} public String getTitle(){return title;} public String getTechnology(){return technology;} public String getCategory(){return category;} public String getDifficulty(){return difficulty;} public String getExplanation(){return explanation;} public String getCodeExample(){return codeExample;} public boolean isActive(){return active;} public Instant getCreatedAt(){return createdAt;}
    public Map<String,String> getOptions(){Map<String,String> out=new LinkedHashMap<>();if(optionA!=null)out.put("A",optionA);if(optionB!=null)out.put("B",optionB);if(optionC!=null)out.put("C",optionC);if(optionD!=null)out.put("D",optionD);return out;}
    public boolean hasMcq(){return correctOption!=null && optionA!=null && optionB!=null && optionC!=null && optionD!=null;}
    public boolean isCorrectOption(String selected){return correctOption!=null && correctOption.equalsIgnoreCase(selected==null?"":selected.trim());}
    public String revealCorrectOption(){return correctOption;}
    public void update(String title,String technology,String category,String difficulty,String explanation,String codeExample,String a,String b,String c,String d,String answer){this.title=title;this.technology=technology;this.category=category;this.difficulty=difficulty;this.explanation=explanation;this.codeExample=codeExample;this.optionA=a;this.optionB=b;this.optionC=c;this.optionD=d;this.correctOption=answer;}
    public void setActive(boolean v){active=v;}
}
