package com.costoptimizer.forecasting_service.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.costoptimizer.forecasting_service.service.ForecastingService;

@RestController
@RequestMapping("/api/forecast")
public class ForecastController {
    @Autowired
    private ForecastingService forecastingService;

    @GetMapping
    public ResponseEntity<?> getForecast(@RequestParam(defaultValue = "7") int days) {
        try {
            List<Double> forecast = forecastingService.createForecast(days);
            return ResponseEntity.ok(forecast);
        } catch (IllegalStateException e) {
            // If there's not enough data, return a user-friendly error message.
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            // For any other unexpected errors.
            return ResponseEntity.internalServerError().body("An unexpected error occurred while generating the forecast.");
        }
    }
}
