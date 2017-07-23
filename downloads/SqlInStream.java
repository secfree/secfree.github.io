package dzq;

import com.google.common.collect.Lists;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.sql.Row;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.Time;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.json.JSONObject;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.SQLContext;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

public class SqlInStream {

    public static void main(String[] args) {
        SparkConf sparkConf = new SparkConf().setAppName("TestWindowSlide");
        JavaStreamingContext jssc = new JavaStreamingContext(sparkConf, new Duration(10000));
        JavaSparkContext ctx = new JavaSparkContext(sparkConf);
        final SQLContext sqlContext = new SQLContext(ctx);

        // generate rdds
        System.out.println("Generate rdds ...");

        Queue<JavaRDD<String>> queue = new LinkedList<JavaRDD<String>>();
        Random rand = new Random();
        for (int i=0; i<100; i++) {
            List<String> list = Lists.newArrayList();
            JSONObject jso;

            for(int j=0; j<1000; j++) {
                jso = new JSONObject();
                jso.put("id", rand.nextInt(100000));
                jso.put("key", rand.nextInt(100));
                jso.put("value", rand.nextInt(100));
                list.add(jso.toString());
            }
            JavaRDD<String> rdd = jssc.sparkContext().parallelize(list);
            queue.add(rdd);
        }

        // generate streaming
        System.out.println("Generate streaming...");
        JavaDStream<String> stream = jssc.queueStream(queue);

        // search in stream RDD
        stream.foreachRDD(new Function2<JavaRDD<String>, Time, Void>() {
            @Override
            public Void call(JavaRDD<String> rdd, Time time) throws Exception {
                DataFrame df = sqlContext.jsonRDD(rdd);
                df.registerTempTable("tmp");
                DataFrame searchResult = sqlContext.sql("select * from tmp where key < 7 and value > 90");
                searchResult.toJavaRDD().foreach(new VoidFunction<Row>() {
                    @Override
                    public void call(Row row) throws Exception {
                        System.out.println(row.toString());
                    }
                });
                System.out.println(rdd.toString());
                return null;
            }
        });

        jssc.start();
        jssc.awaitTermination();
    }
}
