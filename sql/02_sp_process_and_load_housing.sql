DELIMITER //

-- DROP PROCEDURE IF EXISTS sp_process_and_load_housing//

CREATE PROCEDURE sp_process_and_load_housing()
BEGIN
    DECLARE v_rows_processed INT DEFAULT 0;

    -- 1. Tabla temporal con la mediana de total_bedrooms calculada para MySQL
    DROP TEMPORARY TABLE IF EXISTS tmp_bedrooms_median;

    CREATE TEMPORARY TABLE tmp_bedrooms_median AS
    WITH ranked_bedrooms AS (
        SELECT
            ocean_proximity,
            total_bedrooms,
            ROW_NUMBER() OVER (PARTITION BY ocean_proximity ORDER BY total_bedrooms) AS row_num,
            COUNT(*) OVER (PARTITION BY ocean_proximity) AS total_count
        FROM stg_housing
        WHERE total_bedrooms IS NOT NULL
    )
    SELECT
        ocean_proximity,
        ROUND(AVG(total_bedrooms), 2) AS median_bedrooms
    FROM ranked_bedrooms
    WHERE row_num IN (FLOOR((total_count + 1) / 2), FLOOR((total_count + 2) / 2))
    GROUP BY ocean_proximity;

    -- 2. Limpieza, Imputación, Feature Engineering y Carga
    INSERT INTO fact_housing_districts (
        longitude,
        latitude,
        housing_median_age,
        total_rooms,
        total_bedrooms,
        population,
        households,
        median_income,
        median_house_value,
        ocean_proximity_id,
        rooms_per_household,
        bedrooms_per_room,
        population_per_household
    )
    SELECT
        s.longitude,
        s.latitude,
        s.housing_median_age,
        s.total_rooms,
        COALESCE(s.total_bedrooms, m.median_bedrooms) AS total_bedrooms,
        s.population,
        s.households,
        s.median_income,
        s.median_house_value,
        d.ocean_proximity_id,

        -- Feature Engineering
        IF(s.households > 0, ROUND(s.total_rooms / s.households, 4), NULL),
        IF(s.total_rooms > 0, ROUND(COALESCE(s.total_bedrooms, m.median_bedrooms) / s.total_rooms, 4), NULL),
        IF(s.households > 0, ROUND(s.population / s.households, 4), NULL)

    FROM stg_housing s
    INNER JOIN dim_ocean_proximity d ON s.ocean_proximity = d.ocean_proximity_code
    LEFT JOIN tmp_bedrooms_median m ON s.ocean_proximity = m.ocean_proximity
    WHERE s.median_income > 0
      AND s.median_house_value > 0;

    SET v_rows_processed = ROW_COUNT();

    -- 3. Registro en Auditoría
    INSERT INTO housing_audit_log (district_id, action_type, message, execution_user)
    VALUES (NULL, 'ETL_BATCH', CONCAT('Carga completada en MySQL. Filas procesadas: ', v_rows_processed), USER());

    -- Limpieza
    DROP TEMPORARY TABLE IF EXISTS tmp_bedrooms_median;

    SELECT CONCAT('Proceso completado con éxito. Total registros procesados: ', v_rows_processed) AS Result;
END//

DELIMITER ;

LOAD DATA LOCAL INFILE '/Users/francocarrasco/Downloads/housing 2.csv'
INTO TABLE stg_housing
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(longitude, latitude, housing_median_age, total_rooms, @vtotal_bedrooms, population, households, median_income, median_house_value, ocean_proximity)
SET total_bedrooms = NULLIF(TRIM(@vtotal_bedrooms), '');

CALL sp_process_and_load_housing();

SET GLOBAL local_infile = 1;