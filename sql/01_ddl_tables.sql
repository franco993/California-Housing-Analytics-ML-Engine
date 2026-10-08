Create Database If NOT EXISTS housing_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

Use housing_db;

DROP Table If EXISTS stg_housing;
CREATE TABLE stg_housing (
    stg_id INT AUTO_INCREMENT PRIMARY KEY,
    longitude DECIMAL(10, 4),
    latitude DECIMAL(10, 4),
    housing_median_age DECIMAL(10, 2),
    total_rooms DECIMAL(12, 2),
    total_bedrooms DECIMAL(12, 2),
    population DECIMAL(12, 2),
    households DECIMAL(12, 2),
    median_income DECIMAL(10, 4),
    median_house_value DECIMAL(12, 2),
    ocean_proximity VARCHAR(50),
    loaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS fact_housing_districts;
DROP TABLE IF EXISTS dim_ocean_proximity;

CREATE TABLE dim_ocean_proximity (
    ocean_proximity_id INT AUTO_INCREMENT PRIMARY KEY,
    ocean_proximity_code VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255)
) ENGINE=InnoDB;

INSERT INTO dim_ocean_proximity (ocean_proximity_code, description) VALUES
('<1H OCEAN', 'Within 1 hour drive to the ocean'),
('INLAND', 'Inland territory'),
('NEAR OCEAN', 'Located near the ocean coast'),
('NEAR BAY', 'Located near San Francisco Bay area'),
('ISLAND', 'Located on an island');

CREATE TABLE fact_housing_districts (
    district_id INT AUTO_INCREMENT PRIMARY KEY,
    longitude Decimal(10, 4) NOT NULL,
    latitude DECIMAL(10, 4) NOT NULL ,
    housing_median_age DECIMAL(10, 2),
    total_rooms DECIMAL(12, 2),
    total_bedrooms DECIMAL(12, 2),
    population DECIMAL(12, 2),
    households DECIMAL(12, 2),
    median_income DECIMAL(10, 4),
    median_house_value DECIMAL(12, 2),
    ocean_proximity_id INT,

    rooms_per_household DECIMAL(10, 4),
    bedrooms_per_room DECIMAL(10, 4),
    population_per_household DECIMAL(10, 4),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_ocean_proximity FOREIGN KEY (ocean_proximity_id)
    REFERENCES dim_ocean_proximity(ocean_proximity_id)
);

CREATE INDEX idx_coords ON fact_housing_districts(longitude, latitude);
CREATE INDEX idx_ocean ON fact_housing_districts(ocean_proximity_id);

DROP TABLE IF EXISTS housing_audit_log;
CREATE TABLE housing_audit_log (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    district_id INT NULL,
    action_type VARCHAR(20),
    message TEXT,
    execution_user VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;