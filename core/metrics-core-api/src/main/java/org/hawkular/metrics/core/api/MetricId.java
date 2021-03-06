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
package org.hawkular.metrics.core.api;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Objects;

/**
 * @author John Sanda
 */
public class MetricId<T> {
    private final String tenantId;
    private final MetricType<T> type;
    private final String name;
    private final Interval interval;

    public MetricId(String tenantId, MetricType<T> type, String name) {
        this(tenantId, type, name, Interval.NONE);
    }

    public MetricId(String tenantId, MetricType<T> type, String name, Interval interval) {
        checkArgument(tenantId != null, "tenantId is null");
        checkArgument(type != null, "type is null");
        checkArgument(name != null, "name is null");
        checkArgument(interval != null, "interval is null");
        this.tenantId = tenantId;
        this.type = type;
        this.name = name;
        this.interval = interval;
    }

    public String getTenantId() {
        return tenantId;
    }

    public MetricType<T> getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public Interval getInterval() {
        return interval;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MetricId<?> metricId = (MetricId<?>) o;
        return java.util.Objects.equals(name, metricId.name) &&
                java.util.Objects.equals(interval, metricId.interval) &&
                java.util.Objects.equals(tenantId, metricId.tenantId) &&
                java.util.Objects.equals(type, metricId.type);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(name, interval, tenantId, type);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("tenantId", tenantId)
                .add("type", type)
                .add("name", name)
                .add("interval", interval)
                .omitNullValues()
                .toString();
    }
}
