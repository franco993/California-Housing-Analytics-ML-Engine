DELIMITER //

DROP TRIGGER IF EXISTS trg_fact_housing_quality_insert

CREATE TRIGGER trg_fact_housing_quality_insert
BEFORE INSERT ON fact_housing_districts
FOR EACH ROW
BEGIN
    -- Validaciones de Calidad de Datos
    IF NEW.median_income <= 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Error de Calidad de Datos: median_income debe ser mayor a 0.';
    END IF;

    IF NEW.housing_median_age < 0 OR NEW.housing_median_age > 100 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Error de Calidad de Datos: housing_median_age fuera de rango [0-100].';
    END IF;
END

DROP TRIGGER IF EXISTS trg_fact_housing_audit_insert

CREATE TRIGGER trg_fact_housing_audit_insert
AFTER INSERT ON fact_housing_districts
FOR EACH ROW
BEGIN
    -- Registrar entrada en log de auditoría
    INSERT INTO housing_audit_log (district_id, action_type, message, execution_user)
    VALUES (NEW.district_id, 'INSERT', CONCAT('Nuevo distrito registrado con median_house_value = ', NEW.median_house_value), USER());
END

DELIMITER ;