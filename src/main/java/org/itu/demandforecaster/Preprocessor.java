package org.itu.demandforecaster;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.ml.feature.OneHotEncoder;
import org.apache.spark.ml.feature.StringIndexer;
import org.apache.spark.ml.feature.VectorAssembler;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.SQLContext;

/**
 * Created by Group 1.
 */

public class Preprocessor {
    /**
     * Attributes
     */

    // DataFrames generated by data files.
    protected DataFrame rawData;
    protected DataFrame trainingData;
    protected DataFrame testData;
    protected DataFrame processedData;

    // Objects for data preprocessing.
    public StringIndexer categoryIndexer;
    public OneHotEncoder dayOfWeekEncoder, categoryEncoder;
    public VectorAssembler vectorAssembler;

    // Debugging purposes only.
    public DataFrame debug;

    /**
     * When a new object is derived, it sets up necessary indexers and encoders
     * according to the necessary columns. Basically, it extracts necessary
     * features for prediction model.
     */
    public Preprocessor(){
        categoryIndexer = new StringIndexer()
                .setInputCol("category")
                .setOutputCol("categoryIndex");

        dayOfWeekEncoder = new OneHotEncoder()
                .setInputCol("dayOfWeek")
                .setOutputCol("dayOfWeekVector");

        categoryEncoder = new OneHotEncoder()
                .setInputCol("categoryIndex")
                .setOutputCol("categoryVector");

        String[] cols = {"categoryVector", "dayOfWeekVector"};
        vectorAssembler = new VectorAssembler()
                .setInputCols(cols)
                .setOutputCol("features");
    }

    public void loadData() {
        SparkConf sparkConf = Configuration.getSparkConfig().config;
        JavaSparkContext sparkContext = new JavaSparkContext(sparkConf);
        SQLContext sqlContext = new SQLContext(sparkContext);

        rawData = sqlContext.read()
                .format("com.databricks.spark.csv")
                .option("header", "true")
                .option("inferSchema", "true")
                .load("transactions.csv")
                .repartition(6);
        rawData.registerTempTable("RawTrainData");
        processedData = sqlContext.sql("SELECT amount label, category, weekofyear(date) weekOfYear, " +
                "date_format(date, 'EEEE') dayOfWeek FROM RawTrainData").na().drop();

        // After loading training data some of it will be split as test data. Here 90% of data taken as training data.
        DataFrame[] splits = processedData.randomSplit(new double[]{0.9, 0.1});

        trainingData = splits[0];
        testData = splits[1];
    }

    /**
     * Getters and setters.
     */

    public DataFrame getRawData() {
        return rawData;
    }

    public void setRawData(DataFrame rawData) {
        this.rawData = rawData;
    }

    public DataFrame getTestData() {
        return testData;
    }

    public void setTestData(DataFrame testData) {
        this.testData = testData;
    }

    public DataFrame getProcessedData() {
        return processedData;
    }

    public void setProcessedData(DataFrame processedData) {
        this.processedData = processedData;
    }
}
