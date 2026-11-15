package com.faijan.interviewhub.service;
import com.faijan.interviewhub.dto.QuestionRequest;import com.faijan.interviewhub.entity.Question;import com.faijan.interviewhub.event.QuestionViewedEvent;import com.faijan.interviewhub.repository.QuestionRepository;
import org.springframework.cache.annotation.*;import org.slf4j.Logger;import org.slf4j.LoggerFactory;import org.springframework.data.domain.*;import org.springframework.data.jpa.domain.Specification;import org.springframework.http.HttpStatus;import org.springframework.kafka.core.KafkaTemplate;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import org.springframework.web.server.ResponseStatusException;import java.util.Locale;
@Service @Transactional
public class QuestionService {
 private static final Logger log=LoggerFactory.getLogger(QuestionService.class);
 private final QuestionRepository repo;private final KafkaTemplate<String,Object> kafka;
 public QuestionService(QuestionRepository repo,KafkaTemplate<String,Object> kafka){this.repo=repo;this.kafka=kafka;}
 @Transactional(readOnly=true) public Page<Question> search(String q,String technology,String category,String difficulty,int page,int size){
  if(page<0||size<1||size>50)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"page must be >= 0 and size must be between 1 and 50");
  Specification<Question> spec=(root,query,cb)->cb.isTrue(root.get("active"));
  if(q!=null&&!q.isBlank()){String term="%"+q.toLowerCase(Locale.ROOT).replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";spec=spec.and((r,x,c)->c.or(c.like(c.lower(r.get("title")),term,'\\'),c.like(c.lower(r.get("explanation")),term,'\\'),c.like(c.lower(r.get("technology")),term,'\\'),c.like(c.lower(r.get("category")),term,'\\')));}
  if(technology!=null&&!technology.isBlank())spec=spec.and((r,x,c)->c.equal(c.lower(r.get("technology")),technology.toLowerCase(Locale.ROOT)));
  if(category!=null&&!category.isBlank())spec=spec.and((r,x,c)->c.equal(c.lower(r.get("category")),category.toLowerCase(Locale.ROOT)));
  if(difficulty!=null&&!difficulty.isBlank())spec=spec.and((r,x,c)->c.equal(c.lower(r.get("difficulty")),difficulty.toLowerCase(Locale.ROOT)));
  return repo.findAll(spec,PageRequest.of(page,size,Sort.by("technology").ascending().and(Sort.by("id").ascending())));
 }
 @Cacheable(value="question",key="#id") @Transactional(readOnly=true) public Question get(Long id){return repo.findById(id).filter(Question::isActive).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Question not found: "+id));}
 public void publishViewedEvent(Question question){try{kafka.send("question-viewed",question.getId().toString(),new QuestionViewedEvent(question.getId(),question.getTechnology())).whenComplete((result,error)->{if(error!=null)log.warn("Could not publish question-viewed event for question {}",question.getId(),error);});}catch(Exception ex){log.warn("Could not publish question-viewed event for question {}",question.getId(),ex);}}
 @CacheEvict(value="question",allEntries=true) public Question create(QuestionRequest r){validateMcq(r);ensureTitleAvailable(r.title(),null);return repo.save(toEntity(r));}
 @CacheEvict(value="question",allEntries=true) public java.util.List<Question> createAll(java.util.List<QuestionRequest> requests){if(requests.size()>100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"A bulk import may contain at most 100 questions");java.util.Set<String> titles=new java.util.HashSet<>();for(QuestionRequest request:requests){validateMcq(request);String normalized=request.title().trim().toLowerCase(Locale.ROOT);if(!titles.add(normalized))throw new ResponseStatusException(HttpStatus.CONFLICT,"The bulk import contains duplicate question titles");ensureTitleAvailable(request.title(),null);}return repo.saveAll(requests.stream().map(this::toEntity).toList());}
 private Question toEntity(QuestionRequest r){return new Question(r.title().trim(),r.technology().trim(),r.category().trim(),r.difficulty(),r.explanation().trim(),r.codeExample(),r.optionA(),r.optionB(),r.optionC(),r.optionD(),r.correctOption()==null?null:r.correctOption().toUpperCase());}
 @CacheEvict(value="question",allEntries=true) public Question update(Long id,QuestionRequest r){validateMcq(r);Question q=repo.findById(id).filter(Question::isActive).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Question not found: "+id));ensureTitleAvailable(r.title(),id);q.update(r.title().trim(),r.technology().trim(),r.category().trim(),r.difficulty(),r.explanation().trim(),r.codeExample(),r.optionA(),r.optionB(),r.optionC(),r.optionD(),r.correctOption()==null?null:r.correctOption().toUpperCase());return q;}
 @CacheEvict(value="question",allEntries=true) public void delete(Long id){Question q=repo.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Question not found: "+id));q.setActive(false);}
 private void validateMcq(QuestionRequest r){
  if(!r.hasAnyOption()) return;
  if(!r.hasCompleteMcq()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Provide all four MCQ options and the correct option, or leave all MCQ fields empty");
  java.util.Set<String> options=new java.util.HashSet<>(java.util.List.of(r.optionA().trim().toLowerCase(Locale.ROOT),r.optionB().trim().toLowerCase(Locale.ROOT),r.optionC().trim().toLowerCase(Locale.ROOT),r.optionD().trim().toLowerCase(Locale.ROOT)));
  if(options.size()!=4) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"MCQ options must be distinct");
 }
 private void ensureTitleAvailable(String title,Long excludedId){String normalized=title.trim();boolean duplicate=excludedId==null?repo.existsByTitleIgnoreCase(normalized):repo.existsByTitleIgnoreCaseAndIdNot(normalized,excludedId);if(duplicate)throw new ResponseStatusException(HttpStatus.CONFLICT,"A question with this title already exists");}
}
