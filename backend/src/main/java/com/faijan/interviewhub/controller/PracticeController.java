package com.faijan.interviewhub.controller;
import com.faijan.interviewhub.dto.*;import com.faijan.interviewhub.service.PracticeService;
import jakarta.validation.Valid;import jakarta.validation.constraints.*;import org.springframework.security.core.Authentication;import org.springframework.validation.annotation.Validated;import org.springframework.web.bind.annotation.*;import java.util.List;
@RestController @Validated @RequestMapping("/api/practice")
public class PracticeController {
 private final PracticeService service;public PracticeController(PracticeService s){service=s;}
 @PostMapping("/start") public PracticeStartResponse start(Authentication auth,@RequestParam(required=false)String technology,@RequestParam(required=false)String difficulty,@RequestParam(defaultValue="10")@Min(1)@Max(30)int count,@RequestParam(defaultValue="20")@Min(5)@Max(60)int durationMinutes){return service.start(auth.getName(),technology,difficulty,count,durationMinutes);}
 @GetMapping("/{attemptId}") public PracticeStartResponse resume(Authentication auth,@PathVariable Long attemptId){return service.resume(auth.getName(),attemptId);}
 @PutMapping("/{attemptId}/answers") public void saveAnswers(Authentication auth,@PathVariable Long attemptId,@RequestBody java.util.Map<Long,String> answers){service.saveAnswers(auth.getName(),attemptId,answers);}
 @PostMapping("/{attemptId}/submit") public PracticeReviewResponse submit(Authentication auth,@PathVariable Long attemptId){return service.submit(auth.getName(),attemptId);}
 @GetMapping("/attempts") public List<AttemptSummary> history(Authentication auth){return service.history(auth.getName());}
 @GetMapping("/attempts/{id}") public PracticeReviewResponse review(Authentication auth,@PathVariable Long id){return service.review(auth.getName(),id);}
}
