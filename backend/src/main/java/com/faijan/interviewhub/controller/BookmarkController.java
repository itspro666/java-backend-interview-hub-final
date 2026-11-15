package com.faijan.interviewhub.controller;
import com.faijan.interviewhub.dto.BookmarkView;import com.faijan.interviewhub.service.BookmarkService;import org.springframework.http.HttpStatus;import org.springframework.security.core.Authentication;import org.springframework.web.bind.annotation.*;import java.util.List;
@RestController @RequestMapping("/api/bookmarks") public class BookmarkController {
 private final BookmarkService service;public BookmarkController(BookmarkService s){service=s;}
 @GetMapping public List<BookmarkView> list(Authentication auth){return service.list(auth.getName());}
 @PostMapping("/{questionId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void add(Authentication auth,@PathVariable Long questionId){service.add(auth.getName(),questionId);}
 @DeleteMapping("/{questionId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void remove(Authentication auth,@PathVariable Long questionId){service.remove(auth.getName(),questionId);}
}
