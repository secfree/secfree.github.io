---
layout: blog
title: "Metrics 用 Ganglia 展示 demo"
---

# maven 

<br />

dependency 添加

    ```
    <dependency>
        <groupId>io.dropwizard.metrics</groupId>
        <artifactId>metrics-core</artifactId>
        <version>3.1.0</version>
    </dependency>

    <dependency>
        <groupId>io.dropwizard.metrics</groupId>
        <artifactId>metrics-ganglia</artifactId>
        <version>3.1.0</version>
    </dependency>
    ```

---

# demo code

<br />

```java
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ganglia.GangliaReporter;
import info.ganglia.gmetric4j.gmetric.GMetric;
import org.apache.log4j.Logger;

import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Demo {
    private static Logger logger = Logger.getLogger(Demo.class);
    static final MetricRegistry metrics = new MetricRegistry();

    public static void main(String args[]) throws Exception{
        Demo dm = new Demo();
        dm.startReport();

        Random rd = new Random();
        final Queue<Integer> queue = new ArrayBlockingQueue<Integer>(1000);

        // A meter measures the rate of events over time (e.g., “requests per second”).
        Meter rates = metrics.meter("requests");
        // A gauge is an instantaneous measurement of a value.
        // For example, we may want to measure the number of pending jobs in a queue:
        metrics.register(MetricRegistry.name(Demo.class, "queue", "size"),
                new Gauge<Integer>() {
                    public Integer getValue() {
                        return queue.size();
                    }
                });

        while (true) {
            rates.mark();
            queue.offer(rd.nextInt());
            Thread.sleep(Math.abs(rd.nextInt()) % 1000);
            if ((rd.nextInt() % 100) > 95 ) {
                queue.clear();
            }
        }
    }


    public void startReport() {
        // Use Ganglia as reporting backend
        final GMetric ganglia;
        try {
            // hostname and port should be read from config
            ganglia = new GMetric(
                    "127.0.0.1", 8649, GMetric.UDPAddressingMode.MULTICAST, 1);
        } catch (Exception e) {
            logger.error(e);
            return;
        }

        // build reporter with given rates and durations
        final GangliaReporter reporter = GangliaReporter.forRegistry(metrics)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build(ganglia);
        // period: the amount of time between polls
        reporter.start(1, TimeUnit.MINUTES);
    }
}

```

---

# Demo effect on Ganglia

<br />

- rates

    ![]( {{ site.url}}/downloads/metrics_ganglia_rate.png )

- size

    ![]( {{ site.url}}/downloads/metrics_ganglia_size.png )

---

# Refer

<br />

1. [Metrics Getting Started](http://metrics.dropwizard.io/3.1.0/getting-started/)
1. [Reporting to Ganglia](http://metrics.dropwizard.io/3.1.0/manual/ganglia/)
