package com.datanalytics.ml;

import ai.djl.Model;
import ai.djl.metric.Metrics;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Activation;
import ai.djl.nn.SequentialBlock;
import ai.djl.nn.core.Linear;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.EasyTrain;
import ai.djl.training.Trainer;
import ai.djl.training.dataset.ArrayDataset;
import ai.djl.training.loss.Loss;
import ai.djl.training.optimizer.Optimizer;
import ai.djl.training.tracker.Tracker;

public class HousingDeepLearningMLP {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Inicializando Red Neuronal Perceptrón Multicapa (MLP) con DJL ===");

        int numFeatures = 8;
        int batchSize = 32;
        int epochs = 20;

        // 1. Arquitectura de la Red Neuronal
        SequentialBlock block = new SequentialBlock();
        block.add(Linear.builder().setUnits(64).build())
                .add(Activation.reluBlock())
                .add(Linear.builder().setUnits(32).build())
                .add(Activation.reluBlock())
                .add(Linear.builder().setUnits(16).build())
                .add(Activation.reluBlock())
                .add(Linear.builder().setUnits(1).build());

        try (Model model = Model.newInstance("housing-mlp")) {
            model.setBlock(block);

            NDManager manager = NDManager.newBaseManager();
            NDArray xData = manager.randomNormal(new Shape(1000, numFeatures));
            NDArray yData = manager.randomNormal(new Shape(1000, 1));

            ArrayDataset dataset = new ArrayDataset.Builder()
                    .setData(xData)
                    .optLabels(yData)
                    .setSampling(batchSize, true)
                    .build();

            DefaultTrainingConfig config = new DefaultTrainingConfig(Loss.l2Loss())
                    .optOptimizer(Optimizer.adam().optLearningRateTracker(Tracker.fixed(0.001f)).build())
                    .optDevices(manager.getEngine().getDevices());

            try (Trainer trainer = model.newTrainer(config)) {
                trainer.initialize(new Shape(batchSize, numFeatures));
                trainer.setMetrics(new Metrics());

                System.out.println("=== Entrenando Perceptrón Multicapa ===");
                for (int epoch = 0; epoch < epochs; epoch++) {
                    for (var batch : trainer.iterateDataset(dataset)) {
                        EasyTrain.trainBatch(trainer, batch);
                        batch.close();
                    }
                    trainer.step();
                    System.out.printf("Epoch %2d/%d finalizada exitosamente%n", epoch + 1, epochs);
                }
            }
            System.out.println("=== Entrenamiento de Red Neuronal Completado ===");
        }
    }
}