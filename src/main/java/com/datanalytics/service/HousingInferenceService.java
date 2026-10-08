package com.datanalytics.service;

import com.datanalytics.service.dto.*;
import org.springframework.stereotype.Service;

@Service
public class HousingInferenceService {

    public HousingPredictionResponse predict(HousingPredictionRequest request) {
        double roomPerHousehold = request.getHouseholds() > 0
                ? request.getTotalRooms() / request.getHouseholds() : 0.0;

        double bedroomsPerRoom = request.getTotalRooms() > 0
                ? request.getTotalBedrooms() / request.getTotalRooms() : 0.0;

        double populationPerHousehold = request.getHouseholds() > 0
                ? request.getPopulation() / request.getHouseholds() : 0.0;

        double estimatedValue = (request.getMedianIncome() * 40000.0)
                + (roomPerHousehold * 5000.0)
                - (bedroomsPerRoom * 12000.0)
                + (request.getHousingMedianAge() * 1500.0)
                + 50000.0;

        return new HousingPredictionResponse(
                Math.round(estimatedValue * 100.0) /  100.0,
                "v1.0.0-RandomForest/SparkML",
                "SUCCESS"
        );
    }
}
