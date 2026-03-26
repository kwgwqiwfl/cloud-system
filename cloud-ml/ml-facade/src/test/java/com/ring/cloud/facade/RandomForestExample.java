package com.ring.cloud.facade;

import org.apache.spark.ml.classification.RandomForestClassificationModel;
import org.apache.spark.ml.classification.RandomForestClassifier;
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator;
import org.apache.spark.ml.feature.*;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

public class RandomForestExample {
    public static void main(String[] args) {
        SparkSession spark = SparkSession.builder().appName("RandomForestExample").master("local").getOrCreate();

        // 加载CSV数据
        Dataset<Row> data1 = spark.read().format("csv")
                .option("header", "true") // 使用第一行作为标题
                .load("D:\\crawl\\testforest2.csv");
        String[] inputStrs = new String[]{"l1", "l2", "l3", "l4"};
        String[] featCols = new String[]{"l11", "l21", "l31", "l41"};
        StringIndexer indexer = new StringIndexer()
                .setInputCols(inputStrs)
                .setOutputCols(featCols);
        Dataset<Row> trainData = indexer.fit(data1).transform(data1);

        VectorAssembler assembler = new VectorAssembler()
                .setInputCols(featCols)
                .setOutputCol("features");
        Dataset<Row> data = assembler.transform(trainData);

        // 特征列和标签列
        String[] columns = new String[]{"feature0", "feature1", "feature2", "feature3"}; // 假设的特征列名
        String labelColumn = "l5"; // 假设的标签列名

        // 使用StringIndexer对标签进行编号
        StringIndexerModel labelIndexer = new StringIndexer()
                .setInputCols(new String[]{labelColumn})
                .setOutputCols(new String[]{"indexedLabel"})
                .fit(data);

        Dataset<Row> data2 = labelIndexer.transform(data);

        // 使用VectorIndexer对特征进行编号
        VectorIndexerModel featureIndexer = new VectorIndexer()
                .setInputCol("features")
                .setOutputCol("indexedFeatures")
                .fit(labelIndexer.transform(data));

        Dataset<Row> data3 = featureIndexer.transform(data2);

        // 创建随机森林分类器
        RandomForestClassifier classifier = new RandomForestClassifier()
                .setLabelCol("indexedLabel")
                .setFeaturesCol("indexedFeatures")
                .setNumTrees(100)
                .setMaxBins(32).setMaxDepth(8).setFeatureSubsetStrategy("auto").setImpurity("gini")
                ;
        // 划分训练集和测试集
        Dataset<Row>[] splits = data3.randomSplit(new double[]{0.9, 0.1});
        Dataset<Row> t1 = splits[0];
        Dataset<Row> t2 = splits[1];
        // 训练模型
        RandomForestClassificationModel model = classifier.fit(t1);
        Dataset<Row> data4 = model.transform(t2);
        // 评估模型
        MulticlassClassificationEvaluator evaluator = new MulticlassClassificationEvaluator()
                .setLabelCol("indexedLabel")
                .setPredictionCol("prediction")
                .setMetricName("accuracy");
        double accuracy = evaluator.evaluate(data4);
        System.out.println("Test Error = " + (1.0 - accuracy));
        System.out.println("Test Error = " + (1.0 - accuracy));

        // 创建IndexToString转换器将索引转换回原始标签
        IndexToString labelConverter = new IndexToString()
                .setInputCol("prediction")
                .setOutputCol("predictedLabel")
                .setLabels(labelIndexer.labels());

//        // 应用转换器转换预测结果
//        Dataset<Row> predictionResult = labelConverter.transform(data4);
//
//        // 显示预测结果
//        predictionResult.select("predictedLabel", "l5").show(5);



        // 读取要预测的CSV文件，预测数据
        Dataset<Row> data6 = spark.read().format("csv")
                .option("header", "true")
                .load("D:\\crawl\\testforest1.csv");
        Dataset<Row> data7 = indexer.fit(data6).transform(data6);
        Dataset<Row> data8 = assembler.transform(data7);
        Dataset<Row> data9 = featureIndexer.transform(data8);
        // 应用转换器转换预测结果
        Dataset<Row> predictionResult2 = labelConverter.transform(model.transform(data9));
        // 显示预测结果
        predictionResult2.select("predictedLabel").show(7);

        spark.stop();
    }
}