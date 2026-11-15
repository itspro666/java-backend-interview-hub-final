package com.faijan.interviewhub.service;
import com.faijan.interviewhub.dto.BookmarkView;import com.faijan.interviewhub.entity.*;import com.faijan.interviewhub.repository.*;import org.springframework.http.HttpStatus;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import org.springframework.web.server.ResponseStatusException;import java.util.List;
@Service public class BookmarkService {
 private final QuestionBookmarkRepository bookmarks;private final UserRepository users;private final QuestionRepository questions;
 public BookmarkService(QuestionBookmarkRepository b,UserRepository u,QuestionRepository q){bookmarks=b;users=u;questions=q;}
 @Transactional(readOnly=true) public List<BookmarkView> list(String email){return bookmarks.findByUserEmailOrderByCreatedAtDesc(email).stream().map(b->new BookmarkView(b.getId(),b.getCreatedAt(),b.getQuestion())).toList();}
 @Transactional public void add(String email,Long questionId){if(bookmarks.existsByUserEmailAndQuestionId(email,questionId))return;AppUser user=users.findByEmailIgnoreCase(email).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Account not found"));Question q=questions.findById(questionId).filter(Question::isActive).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Question not found"));bookmarks.save(new QuestionBookmark(user,q));}
 @Transactional public void remove(String email,Long questionId){bookmarks.findByUserEmailAndQuestionId(email,questionId).ifPresent(bookmarks::delete);}
}
