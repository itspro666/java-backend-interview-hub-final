package com.faijan.interviewhub.controller;
import com.faijan.interviewhub.dto.LearningAnalytics;import com.faijan.interviewhub.service.AnalyticsService;import org.springframework.security.core.Authentication;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/analytics") public class AnalyticsController {private final AnalyticsService service;public AnalyticsController(AnalyticsService s){service=s;}@GetMapping("/me") public LearningAnalytics mine(Authentication auth){return service.forUser(auth.getName());}}
