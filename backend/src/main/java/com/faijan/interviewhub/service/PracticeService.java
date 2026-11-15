package com.faijan.interviewhub.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.faijan.interviewhub.dto.*;
import com.faijan.interviewhub.entity.*;
import com.faijan.interviewhub.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PracticeService {
    private final QuestionRepository questions;private final PracticeAttemptRepository attempts;private final UserRepository users;private final ObjectMapper mapper;
    public PracticeService(QuestionRepository q,PracticeAttemptRepository a,UserRepository u,ObjectMapper m){questions=q;attempts=a;users=u;mapper=m;}
    @Transactional
    public PracticeStartResponse start(String email,String technology,String difficulty,int requestedCount,int durationMinutes){
        int count=Math.max(1,Math.min(requestedCount,30));int duration=Math.max(5,Math.min(durationMinutes,60));
        List<Question> pool=questions.findByActiveTrueAndCorrectOptionIsNotNull().stream()
            .filter(Question::hasMcq)
            .filter(q->technology==null||technology.isBlank()||q.getTechnology().equalsIgnoreCase(technology))
            .filter(q->difficulty==null||difficulty.isBlank()||q.getDifficulty().equalsIgnoreCase(difficulty)).collect(Collectors.toCollection(ArrayList::new));
        if(pool.size()<count)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Only "+pool.size()+" MCQ questions match these filters. Reduce the question count or broaden the filters.");
        Collections.shuffle(pool);List<Question> chosen=pool.subList(0,count);
        AppUser user=users.findByEmailIgnoreCase(email).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Account not found"));
        List<PracticeQuestionSnapshot> snapshots=chosen.stream().map(q->new PracticeQuestionSnapshot(q.getId(),q.getTitle(),q.getTechnology(),q.getCategory(),q.getDifficulty(),q.getOptions(),q.revealCorrectOption(),q.getExplanation())).toList();
        String snapshotJson;try{snapshotJson=mapper.writeValueAsString(snapshots);}catch(Exception ex){throw new IllegalStateException("Could not create exam snapshot",ex);}
        PracticeAttempt attempt=attempts.save(new PracticeAttempt(user,technology==null||technology.isBlank()?"Mixed":technology,difficulty==null||difficulty.isBlank()?"Mixed":difficulty,count,duration,chosen.stream().map(Question::getId).toList(),snapshotJson));
        return toStartResponse(attempt,snapshots,Map.of());
    }
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public PracticeStartResponse resume(String email,Long attemptId){
        PracticeAttempt attempt=attempts.findByIdAndUserEmail(attemptId,email).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Practice session not found"));
        if(!"IN_PROGRESS".equals(attempt.getStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"This practice session is already complete");
        if(Instant.now().isAfter(attempt.getDeadlineAt())){finalizeAttempt(attempt,true);throw new ResponseStatusException(HttpStatus.CONFLICT,"The exam deadline has passed. The saved answers have been submitted.");}
        try{
            List<PracticeQuestionSnapshot> snapshots=mapper.readValue(attempt.getQuestionSnapshotJson(),new TypeReference<List<PracticeQuestionSnapshot>>(){});
            Map<Long,String> saved=mapper.readValue(attempt.getAnswersJson(),new TypeReference<Map<Long,String>>(){});
            return toStartResponse(attempt,snapshots,saved);
        }catch(Exception ex){if(ex instanceof ResponseStatusException response)throw response;throw new IllegalStateException("Could not restore practice session",ex);}
    }
    @Transactional
    public void saveAnswers(String email,Long attemptId,Map<Long,String> answers){
        PracticeAttempt attempt=attempts.findByIdAndUserEmail(attemptId,email).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Practice session not found"));
        if(!"IN_PROGRESS".equals(attempt.getStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"This practice session has already been submitted");
        if(Instant.now().isAfter(attempt.getDeadlineAt()))throw new ResponseStatusException(HttpStatus.CONFLICT,"The exam deadline has passed");
        Set<Long> allowed=new HashSet<>(attempt.questionIds());
        if(answers!=null&&!allowed.containsAll(answers.keySet()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Answer contains a question outside this session");
        if(answers!=null)for(String selected:answers.values())if(selected!=null&&!Set.of("A","B","C","D").contains(selected.trim().toUpperCase()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Answer options must be A, B, C or D");
        try{attempt.recordAnswers(mapper.writeValueAsString(answers==null?Map.of():answers));}catch(Exception ex){throw new IllegalStateException("Could not save exam answers",ex);}
    }
    @Transactional
    public PracticeReviewResponse submit(String email,Long attemptId){
        PracticeAttempt attempt=attempts.findByIdAndUserEmail(attemptId,email).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Practice session not found"));
        if(!"IN_PROGRESS".equals(attempt.getStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"This practice session has already been submitted");
        return finalizeAttempt(attempt, Instant.now().isAfter(attempt.getDeadlineAt()));
    }
    @Transactional
    public List<AttemptSummary> history(String email){
        Instant now=Instant.now();
        List<PracticeAttempt> rows=attempts.findByUserEmailOrderByStartedAtDesc(email);
        for(PracticeAttempt attempt:rows){
            if("IN_PROGRESS".equals(attempt.getStatus()) && now.isAfter(attempt.getDeadlineAt())) finalizeAttempt(attempt,true);
        }
        return rows.stream().map(this::summary).toList();
    }
    private PracticeReviewResponse finalizeAttempt(PracticeAttempt attempt,boolean timedOut){
        Map<Long,String> answers;
        try{answers=mapper.readValue(attempt.getAnswersJson(),new TypeReference<Map<Long,String>>(){});}catch(Exception ex){throw new IllegalStateException("Could not read saved exam answers",ex);}
        List<PracticeQuestionSnapshot> snapshots;
        try{snapshots=mapper.readValue(attempt.getQuestionSnapshotJson(),new TypeReference<List<PracticeQuestionSnapshot>>(){});}catch(Exception ex){throw new IllegalStateException("Could not read exam snapshot",ex);}
        Map<Long,PracticeQuestionSnapshot> byId=snapshots.stream().collect(Collectors.toMap(PracticeQuestionSnapshot::id,q->q));
        List<PracticeReviewResponse.AnswerReview> review=new ArrayList<>();int score=0;
        for(Long id:attempt.questionIds()){
            PracticeQuestionSnapshot q=byId.get(id);
            if(q==null)throw new IllegalStateException("Exam snapshot is missing question "+id);
            String selected=answers==null?null:answers.get(id);
            if(selected!=null&&!Set.of("A","B","C","D").contains(selected.trim().toUpperCase()))throw new IllegalStateException("Saved answer contains an invalid option");
            String normalized=selected==null?null:selected.trim().toUpperCase(Locale.ROOT);
            boolean correct=q.correctOption()!=null&&q.correctOption().equalsIgnoreCase(normalized==null?"":normalized);if(correct)score++;
            review.add(new PracticeReviewResponse.AnswerReview(id,q.title(),normalized,q.correctOption(),correct,q.explanation()));
        }
        try{attempt.complete(score,timedOut,mapper.writeValueAsString(review));}catch(Exception ex){throw new IllegalStateException("Could not persist exam review",ex);}
        return toResponse(attempt,review);
    }
    @Transactional(readOnly=true)
    public PracticeReviewResponse review(String email,Long id){
        PracticeAttempt a=attempts.findByIdAndUserEmail(id,email).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Attempt not found"));
        if(!"COMPLETED".equals(a.getStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"This attempt has not been submitted yet");
        try{return toResponse(a,mapper.readValue(a.getReviewJson(),new TypeReference<List<PracticeReviewResponse.AnswerReview>>(){}));}catch(Exception ex){throw new IllegalStateException("Could not read attempt review",ex);}
    }
    private PracticeStartResponse toStartResponse(PracticeAttempt attempt,List<PracticeQuestionSnapshot> snapshots,Map<Long,String> savedAnswers){return new PracticeStartResponse(attempt.getId(),attempt.getTechnology(),attempt.getDifficulty(),attempt.getDurationMinutes(),attempt.getStartedAt(),attempt.getDeadlineAt(),snapshots.stream().map(this::toPracticeQuestion).toList(),savedAnswers==null?Map.of():savedAnswers);}
    private PracticeStartResponse.PracticeQuestion toPracticeQuestion(PracticeQuestionSnapshot q){List<PracticeStartResponse.OptionView> options=q.options().entrySet().stream().map(e->new PracticeStartResponse.OptionView(e.getKey(),e.getValue())).toList();return new PracticeStartResponse.PracticeQuestion(q.id(),q.title(),q.technology(),q.category(),q.difficulty(),options);}
    private PracticeReviewResponse toResponse(PracticeAttempt a,List<PracticeReviewResponse.AnswerReview> review){double pct=a.getTotalQuestions()==0?0:Math.round(a.getScore()*10000.0/a.getTotalQuestions())/100.0;return new PracticeReviewResponse(a.getId(),a.getTechnology(),a.getDifficulty(),a.getScore(),a.getTotalQuestions(),pct,a.isTimedOut(),a.getCompletedAt(),review);}
    private AttemptSummary summary(PracticeAttempt a){double pct=a.getTotalQuestions()==0?0:Math.round(a.getScore()*10000.0/a.getTotalQuestions())/100.0;return new AttemptSummary(a.getId(),a.getTechnology(),a.getDifficulty(),a.getScore(),a.getTotalQuestions(),pct,a.isTimedOut(),a.getStatus(),a.getStartedAt(),a.getCompletedAt());}
}
