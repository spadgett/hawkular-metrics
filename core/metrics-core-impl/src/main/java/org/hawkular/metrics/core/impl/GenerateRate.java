/*
 * Copyright 2014-2015 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hawkular.metrics.core.impl;

import org.hawkular.metrics.core.api.DataPoint;
import org.hawkular.metrics.core.api.Metric;
import org.hawkular.metrics.core.api.MetricId;
import org.hawkular.metrics.core.api.MetricsService;
import org.hawkular.metrics.tasks.api.Task2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Action1;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.singletonList;
import static org.hawkular.metrics.core.api.MetricType.COUNTER;
import static org.hawkular.metrics.core.api.MetricType.COUNTER_RATE;

/**
 * @author jsanda
 */
public class GenerateRate implements Action1<Task2> {

    private static final Logger logger = LoggerFactory.getLogger(GenerateRate.class);

    private MetricsService metricsService;

    public GenerateRate(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @Override
    public void call(Task2 task) {
        logger.info("Generating rate for {}", task);
        String tenant = task.getParameters().get("tenant");
        long start = task.getTrigger().getTriggerTime();
        long end = start + TimeUnit.MINUTES.toMillis(1);

        Observable<Metric<Double>> rates = metricsService.findMetrics(tenant, COUNTER)
                .flatMap(counter -> metricsService.findCounterData(counter.getId(), start, end)
                        .take(1)
                        .map(dataPoint -> ((dataPoint.getValue().doubleValue() / (end - start) * 1000)))
                        .map(rate -> new Metric<>(new MetricId(tenant, COUNTER_RATE, counter.getId().getName()),
                                singletonList(new DataPoint<>(start, rate)))));
        Observable<Void> updates = metricsService.addGaugeData(rates);

        CountDownLatch latch = new CountDownLatch(1);

        updates.subscribe(
                aVoid -> {},
                t -> {
                    logger.warn("There was an error persisting rates for {tenant= " + tenant + ", start= " +
                        start + ", end= " + end + "}", t);
                    latch.countDown();
                },
                () -> {
                    logger.debug("Successfully persisted rate data for {tenant= " + tenant + ", start= " +
                        start + ", end= " + end + "}");
                    latch.countDown();
                }
        );

        try {
            latch.await();
        } catch (InterruptedException e) {
        }
    }

}
