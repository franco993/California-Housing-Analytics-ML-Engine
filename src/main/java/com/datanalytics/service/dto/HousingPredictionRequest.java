package com.datanalytics.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HousingPredictionRequest {

    private double longitude;
    private double latitude;
    private double housingMedianAge;
    private double totalRooms;
    private double totalBedrooms;
    private double population;
    private double households;
    private double medianIncome;
    private String oceanProximity;
}
