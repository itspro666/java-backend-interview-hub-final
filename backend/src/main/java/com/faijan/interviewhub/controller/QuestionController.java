package com.faijan.interviewhub.controller;
import com.faijan.interviewhub.dto.QuestionRequest;import com.faijan.interviewhub.entity.Question;import com.faijan.interviewhub.service.QuestionService;import jakarta.validation.Valid;import jakarta.validation.constraints.*;import org.springframework.validation.annotation.Validated;import org.springframework.data.domain.Page;import org.springframework.http.HttpStatus;import org.springframework.web.bind.annotation.*;
@RestController @Validated @RequestMapping("/api/questions")
public class QuestionController {
 private final QuestionService service;public QuestionController(QuestionService service){this.service=service;}
 @GetMapping public Page<Question> search(@RequestParam(required=false)@Size(max=200)String q,@RequestParam(required=false)@Size(max=80)String technology,@RequestParam(required=false)@Size(max=100)String category,@RequestParam(required=false)@Size(max=20)String difficulty,@RequestParam(defaultValue="0")@Min(0)int page,@RequestParam(defaultValue="12")@Min(1)@Max(50)int size){return service.search(q,technology,category,difficulty,page,size);}
 @GetMapping("/{id}") public Question get(@PathVariable Long id){Question question=service.get(id);service.publishViewedEvent(question);return question;}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public Question create(@Valid @RequestBody QuestionRequest request){return service.create(request);}
 @PutMapping("/{id}") public Question update(@PathVariable Long id,@Valid @RequestBody QuestionRequest request){return service.update(id,request);}
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id){service.delete(id);}
}
