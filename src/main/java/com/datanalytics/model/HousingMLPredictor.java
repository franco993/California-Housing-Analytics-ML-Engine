package com.datanalytics.model;

import org.apache.spark.ml.Pipeline;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.PipelineStage;
import org.apache.spark.ml.evaluation.RegressionEvaluator;
import org.apache.spark.ml.feature.StringIndexer;
import org.apache.spark.ml.feature.VectorAssembler;
import org.apache.spark.ml.regression.RandomForestRegressor;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import java.util.Properties;

/**
 * Entrenamiento y evaluación de un modelo de Machine Learning distribuido (Random Forest Regresor)
 * sobre el dataset California Housing utilizando Apache Spark MLlib.
 */

public class HousingMLPredictor {

    public static void main(String[] args) {
        // 1. Crear sesión de Apache Spark
        SparkSession spark = SparkSession.builder()
                .appName("Housing-ML-Predictor")
                .master("local[*]")
                .getOrCreate();

        spark.sparkContext().setLogLevel("ERROR");

        // 2. Parámetros de conexión a MySQL
        String jdbcUrl = "jdbc:mysql://localhost:3306/housing_db?allowLoadLocalInfile=true";
        Properties connectionProperties = new Properties();
        connectionProperties.put("user", "root");
        connectionProperties.put("password", "root");
        connectionProperties.put("driver", "com.mysql.cj.jdbc.Driver");

        System.out.println(">>> Cargando datos desde MySQL...");
        Dataset<Row> rawData = spark.read()
                .jdbc(jdbcUrl, "fact_housing_districts", connectionProperties);

        // 3. Preprocesamiento e Ingenería de Variables
        StringIndexer oceanIndexer = new StringIndexer()
                .setInputCol("ocean_proximity")
                .setOutputCol("ocean_proximity_idx")
                .setHandleInvalid("keep");

        VectorAssembler assembler = new VectorAssembler()
                .setInputCols(new String[]{
                        "longitude",
                        "latitude",
                        "housing_median_age",
                        "total_rooms",
                        "total_bedrooms",
                        "population",
                        "households",
                        "median_income",
                        "ocean_proximity_idx"
                })
                .setOutputCol("features")
                .setHandleInvalid("skip");

        // 4. Configurar algoritmo de regresión (Random Forest)
        RandomForestRegressor rf = new RandomForestRegressor()
                .setLabelCol("median_house_value")
                .setFeaturesCol("features")
                .setNumTrees(100)
                .setMaxDepth(10)
                .setSeed(42);

        // 5. Ensamblar el Pipeline
        Pipeline pipeline = new Pipeline()
                .setStages(new PipelineStage[]{oceanIndexer, assembler, rf});

        // 6. División de datos (Train / Test split 80% - 20%)
        Dataset<Row>[] splits = rawData.randomSplit(new double[]{0.8, 0.2}, 42L);
        Dataset<Row> trainData = splits[0];
        Dataset<Row> testData = splits[1];

        System.out.println(">>> Entrenando modelo Random Forest Regressor...");
        PipelineModel model = pipeline.fit(trainData);

        // 7. Generar predicciones en el conjunto de prueba
        System.out.println(">>> Generando predicciones...");
        Dataset<Row> predictions = model.transform(testData);

        // 8. Evaluación de rendimiento (RMSE y R2)
        RegressionEvaluator evaluatorR2 = new RegressionEvaluator()
                .setLabelCol("median_house_value")
                .setPredictionCol("prediction")
                .setMetricName("r2");

        RegressionEvaluator evaluatorRMSE = new RegressionEvaluator()
                .setLabelCol("median_house_value")
                .setPredictionCol("prediction")
                .setMetricName("rmse");

        double r2 = evaluatorR2.evaluate(predictions);
        double rmse = evaluatorRMSE.evaluate(predictions);

        System.out.println("\n==================================================");
        System.out.println("   MÉTRICAS DE EVALUACIÓN DEL MODELO ML");
        System.out.println("==================================================");
        System.out.println("Coeficiente de Determinación (R2) : " + String.format("%.4f", r2));
        System.out.println("Raíz del Error Cuadrático Medio (RMSE): $" + String.format("%.2f", rmse));
        System.out.println("==================================================\n");

        // Muestra de predicciones
        predictions.select("median_house_value", "prediction", "median_income", "ocean_proximity")
                .show(10, false);

        spark.stop();
    }
}
