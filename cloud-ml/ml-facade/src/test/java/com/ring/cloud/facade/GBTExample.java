package com.ring.cloud.facade;

import org.apache.spark.ml.Pipeline;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.PipelineStage;
import org.apache.spark.ml.classification.DecisionTreeClassifier;
import org.apache.spark.ml.classification.GBTClassificationModel;
import org.apache.spark.ml.classification.GBTClassifier;
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator;
import org.apache.spark.ml.feature.*;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

public class GBTExample {
    public static void main(String[] args) {
        // 创建一个 SparkSession
        SparkSession spark = SparkSession
                .builder().master("local")
                .appName("GBTExample")
                .getOrCreate();

        // 读取CSV文件
        Dataset<Row> data = spark.read().format("csv")
                .option("header", "true")
                .load("D:\\crawl\\2024.csv");

        // 读取CSV文件
        Dataset<Row> datatest = spark.read().format("csv")
                .option("header", "true")
                .load("D:\\crawl\\2024test.csv");

        String[] inputStrs = new String[]{"主客", "进球数", "犯规", "黄牌", "红牌", "控球率", "过人次数", "评分", "传球次数", "成功次数"
                , "成功率", "射门", "射正"};
        String[] outputStrs = new String[]{"主客1", "进球数1", "犯规1", "黄牌1", "红牌1", "控球率1", "过人次数1", "评分1", "传球次数1"
                , "成功次数1", "成功率1", "射门1", "射正1"};
        String[] outputStrs2 = new String[]{"主客1", "控球率1"
                , "成功次数1", "成功率1", "射门1", "射正1", "传球次数1"};
        StringIndexer indexer = new StringIndexer()
                .setInputCols(inputStrs)
                .setOutputCols(outputStrs);
        // 输入的DataFrame包含许多列，每列对应一个特征，可以用来预测目标列。
        // Spark MLlib 要求将所有输入合并成一列，该列的值是一个向量。
        // 使用VectorAssembler将所有特征列组合成一个向量
        // 除了目标列以外，所有其他列都作为输入特征，因此产生的DataFrame有一个新的“featureVector”
//        String[] semStrs = new String[]{"主客1", "犯规1", "黄牌1", "红牌1", "控球率1", "过人次数1", "评分1", "传球次数1"
//                , "成功次数1", "成功率1", "射门1", "射正1"};
        VectorAssembler assembler = new VectorAssembler()
                .setInputCols(outputStrs2)
                .setOutputCol("featureVector");
        Dataset<Row> output = indexer.fit(data).transform(data);
        Dataset<Row> outputtest = indexer.fit(datatest).transform(datatest);
//        output = assembler.transform(output);
//        // 应用VectorAssembler转换
//        output.select("featureVector").show(3);

        // 将数据集拆分为训练集和测试集
        Dataset<Row>[] splits = output.randomSplit(new double[]{0.9, 0.1});
        Dataset<Row> trainingData = splits[0];
        Dataset<Row> testData = splits[1];

        // 决策树分类器
        DecisionTreeClassifier classifier = new DecisionTreeClassifier()
                .setLabelCol("进球数1")
                .setFeaturesCol("featureVector");

        // 管道
        Pipeline pipeline = new Pipeline()
                .setStages(new PipelineStage[] {assembler, classifier});

        // 训练模型
        PipelineModel model = pipeline.fit(trainingData);

        // 测试模型
        Dataset<Row> predictionDF = model.transform(outputtest);
        predictionDF.select("prediction").show(5);

        spark.stop();


//        // 定义 GBT 分类器
//        GBTClassifier gbt = new GBTClassifier()
//                .setLabelCol("比赛")
//                .setFeaturesCol("featureVector")
//                .setPredictionCol("进球数")
//                .setMaxIter(10)
//                .setFeatureSubsetStrategy("auto");
//
//        // 创建管道
//        Pipeline pipeline = new Pipeline().setStages(new PipelineStage[]{assembler, gbt});
//
//        // 训练模型
//        PipelineModel model = pipeline.fit(trainingData);
//
//        // 进行预测
//        Dataset<Row> predictions = model.transform(testData);
//
//        // 选择样例行显示
//        predictions.select("进球数", "比赛", "features").show(3);
//
//        // 评估模型
//        MulticlassClassificationEvaluator evaluator = new MulticlassClassificationEvaluator()
//                .setLabelCol("比赛")
//                .setPredictionCol("进球数")
//                .setMetricName("accuracy");
//        double accuracy = evaluator.evaluate(predictions);
//        System.out.println("Test Error = " + (1.0 - accuracy));
//
//        // 获取训练得到的 GBT 模型
//        GBTClassificationModel gbtModel = (GBTClassificationModel) (model.stages()[2]);
//        System.out.println("Learned classification GBT model:\n" + gbtModel.toDebugString());
//
//        spark.stop();
    }
}
