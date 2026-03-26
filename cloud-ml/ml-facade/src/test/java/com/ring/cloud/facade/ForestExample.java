package com.ring.cloud.facade;

import org.apache.spark.ml.Pipeline;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.PipelineStage;
import org.apache.spark.ml.classification.RandomForestClassifier;
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator;
import org.apache.spark.ml.feature.IndexToString;
import org.apache.spark.ml.feature.StringIndexer;
import org.apache.spark.ml.feature.StringIndexerModel;
import org.apache.spark.ml.feature.VectorAssembler;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

public class ForestExample {
    public static void main(String[] args) {
        // 创建一个 SparkSession
        SparkSession spark = SparkSession
                .builder().master("local")
                .appName("ForestExample")
                .getOrCreate();
        // 读取CSV文件，训练和测试模型数据
        Dataset<Row> data = spark.read().format("csv")
                .option("header", "true")
                .load("D:\\crawl\\testforest2.csv");

        String[] inputStrs = new String[]{"l1", "l2", "l3", "l4"};
        String[] featCols = new String[]{"l11", "l21", "l31", "l41"};
        StringIndexer indexer = new StringIndexer()
                .setInputCols(inputStrs)
                .setOutputCols(featCols);
        Dataset<Row> trainData = indexer.fit(data).transform(data);

        VectorAssembler assembler = new VectorAssembler()
                .setInputCols(featCols)
                .setOutputCol("features");
        Dataset<Row> trainData1 = assembler.transform(trainData);

        StringIndexerModel labelIndexer = new StringIndexer()
                .setInputCol("l5")
                .setOutputCol("indexedLabel").fit(trainData1);

        // 划分训练集和测试集
        Dataset<Row>[] splits = trainData1.randomSplit(new double[]{0.9, 0.1});
        Dataset<Row> trainingData = splits[0];
        Dataset<Row> testData = splits[1];

        // 训练随机森林模型
        RandomForestClassifier classifier = new RandomForestClassifier()
                .setLabelCol("indexedLabel")
                .setFeaturesCol("features")
                .setNumTrees(10);

        // 创建IndexToString转换器将索引转换回原始标签
        IndexToString labelConverter = new IndexToString()
                .setInputCol("prediction")
                .setOutputCol("predictedLabel")
                .setLabels(labelIndexer.labels());
        // 管道
        Pipeline pipeline = new Pipeline()
                .setStages(new PipelineStage[] {labelIndexer, classifier, labelConverter});
        // 训练模型
        PipelineModel model = pipeline.fit(trainingData);
        // 读取要预测的CSV文件，预测数据
        Dataset<Row> data1 = spark.read().format("csv")
                .option("header", "true")
                .load("D:\\crawl\\testforest1.csv");
        Dataset<Row> predictData = indexer.fit(data1).transform(data1);
        // 测试模型
        Dataset<Row> predictionDF = model.transform(assembler.transform(predictData));
        // 应用转换器转换预测结果
        Dataset<Row> predictionResult = labelConverter.transform(predictionDF);
        // 显示预测结果
        predictionResult.select("predictedLabel", "l5").show();
        spark.stop();

    }
}
