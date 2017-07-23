---
layout: blog
title: "Spark Streaming 中两个 reduceByKeyAndWindow 导致数据缺失"
---

Spark Streaming 中, 如果连续使用两个 reduceByKeyAndWindow, window 和 slide 的设置在一些情况下, 任务会被正常调度, 但是数据缺失.

测试代码:

```java
public class TwoReduceWindow {

    public static void main(String[] args) {
        SparkConf sparkConf = new SparkConf().setAppName("TwoReduceWindow");
        JavaStreamingContext jssc = new JavaStreamingContext(sparkConf, new Duration(1000));

        // generate rdds
        System.out.println("Generate rdds ...");

        Queue<JavaRDD<Integer>> queue = new LinkedList<JavaRDD<Integer>>();
        Random rand = new Random();
        for (int i = 0; i < 1000; i++) {
            List<Integer> list = Lists.newArrayList();

            for (int j = 0; j < 1000; j++) {
                list.add(rand.nextInt(100));
            }
            JavaRDD<Integer> rdd = jssc.sparkContext().parallelize(list);
            queue.add(rdd);
        }

        // generate streaming
        System.out.println("Generate streaming...");
        JavaDStream<Integer> stream = jssc.queueStream(queue);

        JavaPairDStream<Integer, Integer> pairStream = stream.mapToPair(new PairFunction<Integer, Integer, Integer>() {
            @Override
            public Tuple2<Integer, Integer> call(Integer n) {
                return new Tuple2<Integer, Integer>(n, n % 10);
            }
        });

        int window01 = 2000;
        int slide01 = 1000;
        int window02 = 4000;
        int slide02 = 2000;

        JavaPairDStream<Integer, Integer> rdStream = pairStream.reduceByKeyAndWindow(
                new Function2<Integer, Integer, Integer>() {
                    @Override
                    public Integer call(Integer m, Integer n) throws Exception {
                        return m + n;
                    }
                },
                new Duration(window01),
                new Duration(slide01)
        );

        JavaPairDStream<Integer, Integer> moreRdStream = rdStream.reduceByKeyAndWindow(
                new Function2<Integer, Integer, Integer>() {
                    @Override
                    public Integer call(Integer m, Integer n) throws Exception {
                        return m + n;
                    }
                },
                new Duration(window02),
                new Duration(slide02)
        );

        moreRdStream.foreachRDD(new Function<JavaPairRDD<Integer, Integer>, Void>() {
            @Override
            public Void call(JavaPairRDD<Integer, Integer> rdd) throws Exception {
                rdd.foreach(new VoidFunction<Tuple2<Integer, Integer>>() {
                    @Override
                    public void call(Tuple2<Integer, Integer> tuple) throws Exception {
                        System.out.println("tuple: " + tuple.toString());
                    }
                });
                return null;
            }
        });

        jssc.start();
        jssc.awaitTermination();

    }
}
```

下面是一组测试:

```
| window01 | slide01 | window02 | slide02 | 结果     |
| 2000     | 1000    | 4000     | 2000    | 正常输出 |
| 2000     | 2000    | 4000     | 2000    | 数据缺失 |
| 2000     | 2000    | 4000     | 4000    | 数据缺失 |
| 2000     | 2000    | 8000     | 4000    | 正常输出 |
```

Spark-1.1 和 Spark-1.2 都存在这个问题, Spark-1.3 中都可以正常输出.

估计是前两个版本的一个 bug.