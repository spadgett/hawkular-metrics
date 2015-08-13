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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hawkular.metrics.core.api.AvailabilityType;
import org.hawkular.metrics.core.api.DataPoint;
import org.hawkular.metrics.core.api.Interval;
import org.hawkular.metrics.core.api.Metric;
import org.hawkular.metrics.core.api.MetricId;
import org.hawkular.metrics.core.api.MetricType;
import org.hawkular.metrics.core.api.Tenant;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;

import rx.Observable;

/**
 * @author John Sanda
 */
public interface DataAccess {
    Observable<ResultSet> insertTenant(String tenantId);

    Observable<ResultSet> insertTenant(Tenant tenant);

    Observable<ResultSet> findAllTenantIds();

    Observable<ResultSet> findTenantIds(long time);

    Observable<ResultSet> insertTenantId(long time, String id);

    Observable<ResultSet> findTenant(String id);

    Observable<ResultSet> deleteTenantsBucket(long time);

    ResultSetFuture insertMetricInMetricsIndex(Metric metric);

    Observable<ResultSet> findMetric(MetricId id);

    Observable<ResultSet> addTagsAndDataRetention(Metric metric);

    Observable<ResultSet> getMetricTags(MetricId id, long dpart);

    Observable<ResultSet> addTags(Metric metric, Map<String, String> tags);

    Observable<ResultSet> deleteTags(Metric metric, Set<String> tags);

    Observable<ResultSet> updateTagsInMetricsIndex(Metric metric, Map<String, String> additions,
            Set<String> deletions);

    <T> ResultSetFuture updateMetricsIndex(List<Metric<T>> metrics);

    <T> Observable<Integer> updateMetricsIndexRx(Observable<Metric<T>> metrics);

    Observable<ResultSet> findMetricsInMetricsIndex(String tenantId, MetricType type);

    Observable<Integer> insertData(Metric<Double> metric, int ttl);

    Observable<Integer> insertCounterData(Metric<Long> counter, int ttl);

    Observable<ResultSet> findCounterData(MetricId id, long startTime, long endTime);

    Observable<ResultSet> findData(MetricId id, long startTime, long endTime);

    Observable<ResultSet> findData(Metric<Double> metric, long startTime, long endTime, Order order);

    Observable<ResultSet> findData(MetricId id, long startTime, long endTime,
            boolean includeWriteTime);

    Observable<ResultSet> findData(Metric<Double> metric, long timestamp, boolean includeWriteTime);

    Observable<ResultSet> findAvailabilityData(Metric<AvailabilityType> metric, long startTime, long endTime);

    Observable<ResultSet> findAvailabilityData(Metric<AvailabilityType> metric, long startTime, long endTime,
            boolean includeWriteTime);

    Observable<ResultSet> findAvailabilityData(Metric<AvailabilityType> metric, long timestamp);

    Observable<ResultSet> deleteGaugeMetric(String tenantId, String metric, Interval interval, long dpart);

    Observable<ResultSet> findAllGaugeMetrics();

    Observable<ResultSet> insertGaugeTag(String tag, String tagValue, Metric<Double> metric,
            Observable<TTLDataPoint<Double>> data);

    Observable<ResultSet> insertAvailabilityTag(String tag, String tagValue,
            Metric<AvailabilityType> metric, Observable<TTLDataPoint<AvailabilityType>> data);

    Observable<ResultSet> updateDataWithTag(Metric metric, DataPoint dataPoint, Map<String, String> tags);

    Observable<ResultSet> findGaugeDataByTag(String tenantId, String tag, String tagValue);

    Observable<ResultSet> findAvailabilityByTag(String tenantId, String tag, String tagValue);

    Observable<Integer> insertAvailabilityData(Metric<AvailabilityType> metric, int ttl);

    Observable<ResultSet> findAvailabilityData(MetricId id, long startTime, long endTime);

    ResultSetFuture findDataRetentions(String tenantId, MetricType type);

    Observable<ResultSet> updateRetentionsIndex(String tenantId, MetricType type, Map<String, Integer> retentions);

    ResultSetFuture updateRetentionsIndex(Metric metric);

    Observable<ResultSet> insertIntoMetricsTagsIndex(Metric metric, Map<String, String> tags);

    Observable<ResultSet> deleteFromMetricsTagsIndex(Metric metric, Map<String, String> tags);

    Observable<ResultSet> findMetricsByTagName(String tenantId, String tag);

    Observable<ResultSet> findMetricsByTagNameValue(String tenantId, String tag, String tvalue);
}
