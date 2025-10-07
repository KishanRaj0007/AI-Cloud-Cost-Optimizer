package com.costoptimizer.recommendation_service.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.costoptimizer.recommendation_service.model.Recommendation;
import com.costoptimizer.recommendation_service.service.RecommendationService;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {
    @Autowired
    private RecommendationService recommendationService;

    @GetMapping
    public List<Recommendation> getRecommendations() {
        return recommendationService.generateRecommendations();
    }
}
