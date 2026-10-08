package com.datanalytics.controller;

import com.datanalytics.service.*;
import com.datanalytics.service.dto.HousingPredictionRequest;
import com.datanalytics.service.dto.HousingPredictionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/housing")
public class HousingPredictionController {

    private final HousingInferenceService inferenceService;

    public HousingPredictionController(HousingInferenceService inferenceService) {
        this.inferenceService = inferenceService;
    }

    @PostMapping("/predict")
    public ResponseEntity<HousingPredictionResponse> predictPrice(@RequestBody HousingPredictionRequest request) {
        HousingPredictionResponse response = inferenceService.predict(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Housing Prediction Service is RUNNING");
    }
}
