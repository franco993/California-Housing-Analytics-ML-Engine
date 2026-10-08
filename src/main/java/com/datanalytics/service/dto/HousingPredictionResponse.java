package com.datanalytics.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HousingPredictionResponse {
    private double predictedMedianHouseValue;
    private String modelVersion;
    private String status;
}
