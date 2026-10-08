package com.datanalytics.ml;

import org.apache.spark.ml.Pipeline;
import org.apache.spark.ml.PipelineStage;
import org.apache.spark.ml.evaluation.RegressionEvaluator;
import org.apache.spark.ml.feature.StringIndexer;
import org.apache.spark.ml.feature.VectorAssembler;
import org.apache.spark.ml.tuning.CrossValidator;
import org.apache.spark.ml.tuning.CrossValidatorModel;
import org.apache.spark.ml.tuning.ParamGridBuilder;
import org.apache.spark.ml.regression.RandomForestRegressor;
import org.apache.spark.ml.regression.GBTRegressor;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import java.util.Properties;

public class HousingMLPipeline {

    public static void main(String[] args) {
        SparkSession spark = SparkSession.builder()
                .appName("Housing-ML-Pipeline")
                .master("local[*]")
                .getOrCreate();

        spark.sparkContext().setLogLevel("ERROR");

        String jdbcUrl = "jdbc:mysql://localhost:3306/housing_db?allowPublicKeyRetrieval=true&useSSL=false";
        Properties props = new Properties();
        props.put("user", "root");
        props.put("password", "Rheu1sjx8$");
        props.put("driver", "com.mysql.cj.jdbc.Driver");

        Dataset<Row> rawData = spark.read().jdbc(jdbcUrl, "stg_housing_processed", props);

        Dataset<Row>[] splits = rawData.randomSplit(new double[]{0.8, 0.2}, 42L);
        Dataset<Row> trainData = splits[0];
        Dataset<Row> testData = splits[1];

        StringIndexer oceanIndexer = new StringIndexer()
                .setInputCol("ocean_proximity")
                .setOutputCol("ocean_proximity_idx");

        VectorAssembler assembler = new VectorAssembler()
                .setInputCols(new String[]{
                        "longitude", "latitude", "housing_median_age", "total_rooms",
                        "total_bedrooms", "population", "households", "median_income",
                        "rooms_per_household", "bedrooms_per_room", "population_per_household",
                        "ocean_proximity_idx"
                })
                .setOutputCol("features");

        RandomForestRegressor rf = new RandomForestRegressor()
                .setLabelCol("median_house_value")
                .setFeaturesCol("features");

        Pipeline rfPipeline = new Pipeline().setStages(new PipelineStage[]{oceanIndexer, assembler, rf});

        ParamGridBuilder paramGridRf = new ParamGridBuilder()
                .addGrid(rf.numTrees(), new int[]{20, 50})
                .addGrid(rf.maxDepth(), new int[]{5, 10});

        CrossValidator cvRf = new CrossValidator()
                .setEstimator(rfPipeline)
                .setEvaluator(new RegressionEvaluator().setLabelCol("median_house_value").setMetricName("rmse"))
                .setEstimatorParamMaps(paramGridRf.build())
                .setNumFolds(3);

        System.out.println("=== Entrenando Random Forest con CrossValidator ===");
        CrossValidatorModel cvRfModel = cvRf.fit(trainData);
        Dataset<Row> rfPredictions = cvRfModel.transform(testData);

        RegressionEvaluator evaluator = new RegressionEvaluator().setLabelCol("median_house_value");
        System.out.println("Random Forest RMSE: " + evaluator.setMetricName("rmse").evaluate(rfPredictions));
        System.out.println("Random Forest R2:   " + evaluator.setMetricName("r2").evaluate(rfPredictions));

        GBTRegressor gbt = new GBTRegressor()
                .setLabelCol("median_house_value")
                .setFeaturesCol("features");

        Pipeline gbtPipeline = new Pipeline().setStages(new PipelineStage[]{oceanIndexer, assembler, gbt});

        ParamGridBuilder paramGridGbt = new ParamGridBuilder()
                .addGrid(gbt.maxIter(), new int[]{20, 50})
                .addGrid(gbt.maxDepth(), new int[]{3, 5});

        CrossValidator cvGbt = new CrossValidator()
                .setEstimator(gbtPipeline)
                .setEvaluator(new RegressionEvaluator().setLabelCol("median_house_value").setMetricName("rmse"))
                .setEstimatorParamMaps(paramGridGbt.build())
                .setNumFolds(3);

        System.out.println("\n=== Entrenando GBT Regressor con CrossValidator ===");
        CrossValidatorModel cvGbtModel = cvGbt.fit(trainData);
        Dataset<Row> gbtPredictions = cvGbtModel.transform(testData);

        System.out.println("GBT Regressor RMSE: " + evaluator.setMetricName("rmse").evaluate(gbtPredictions));
        System.out.println("GBT Regressor R2:   " + evaluator.setMetricName("r2").evaluate(gbtPredictions));

        spark.stop();
    }
}
