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
package org.hawkular.metrics.api.jaxrs.handler;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.hawkular.metrics.api.jaxrs.filter.TenantFilter.TENANT_HEADER_NAME;
import static org.hawkular.metrics.api.jaxrs.util.ApiUtils.badRequest;
import static org.hawkular.metrics.api.jaxrs.util.ApiUtils.requestToAvailabilities;
import static org.hawkular.metrics.api.jaxrs.util.ApiUtils.requestToCounters;
import static org.hawkular.metrics.api.jaxrs.util.ApiUtils.requestToGauges;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.hawkular.metrics.api.jaxrs.ApiError;
import org.hawkular.metrics.api.jaxrs.model.Availability;
import org.hawkular.metrics.api.jaxrs.model.Counter;
import org.hawkular.metrics.api.jaxrs.model.Gauge;
import org.hawkular.metrics.api.jaxrs.param.Tags;
import org.hawkular.metrics.api.jaxrs.request.MetricDefinition;
import org.hawkular.metrics.api.jaxrs.request.MixedMetricsRequest;
import org.hawkular.metrics.api.jaxrs.util.ApiUtils;
import org.hawkular.metrics.core.api.Metric;
import org.hawkular.metrics.core.api.MetricType;
import org.hawkular.metrics.core.api.MetricsService;

import com.google.common.collect.ImmutableList;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import rx.Observable;
import rx.schedulers.Schedulers;


/**
 * Interface to deal with metrics
 * @author Heiko W. Rupp
 */
@Path("/metrics")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Api(value = "", description = "Metrics related REST interface")
public class MetricHandler {

    @Inject
    private MetricsService metricsService;

    @HeaderParam(TENANT_HEADER_NAME)
    private String tenantId;

    @SuppressWarnings("rawtypes")
    @GET
    @Path("/")
    @ApiOperation(value = "Find tenant's metric definitions.", notes = "Does not include any metric values. ",
                  response = List.class, responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully retrieved at least one metric definition."),
            @ApiResponse(code = 204, message = "No metrics found."),
            @ApiResponse(code = 400, message = "Invalid type parameter type.", response = ApiError.class),
            @ApiResponse(code = 500, message = "Failed to retrieve metrics due to unexpected error.",
                         response = ApiError.class)
    })
    public Response findMetrics(
            @ApiParam(value = "Queried metric type",
                    required = false,
                      allowableValues = "[gauge, availability, counter]")
            @QueryParam("type") MetricType metricType,
            @ApiParam(value = "List of tags filters", required = false) @QueryParam("tags") Tags tags) {

        if (metricType != null && !MetricType.userTypes().contains(metricType)) {
            return badRequest(new ApiError("Incorrect type param"));
        }

        Observable<Metric> metricObservable = (tags == null) ? metricsService.findMetrics(tenantId, metricType)
                : metricsService.findMetricsWithFilters(tenantId, tags.getTags(), metricType);

        try {
            return metricObservable
                .map(MetricDefinition::new)
                .toList()
                .map(ApiUtils::collectionToResponse)
                .toBlocking()
                .last();
        } catch (Exception e) {
            return ApiUtils.serverError(e);
        }
    }

    @POST
    @Path("/data")
    @ApiOperation(value = "Add data for multiple metrics in a single call.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Adding data succeeded."),
            @ApiResponse(code = 400, message = "Missing or invalid payload.", response = ApiError.class),
            @ApiResponse(code = 500, message = "Unexpected error happened while storing the data",
                    response = ApiError.class)
    })
    public Response addMetricsData(
            @ApiParam(value = "List of metrics", required = true) MixedMetricsRequest metricsRequest
    ) {
        List<Gauge> gauges = metricsRequest.getGauges();
        List<Counter> counters = metricsRequest.getCounters();
        List<Availability> availabilities = metricsRequest.getAvailabilities();

        if ((gauges == null || gauges.isEmpty()) && (availabilities == null || availabilities.isEmpty())
                && (counters == null || counters.isEmpty())) {
            return ApiUtils.emptyPayload();
        }

        Collection<Observable<Void>> observables = new ArrayList<>();
        if (gauges != null && !gauges.isEmpty()) {
            gauges = ImmutableList.copyOf(gauges);
            observables.add(metricsService
                    .addGaugeData(requestToGauges(tenantId, gauges).subscribeOn(Schedulers.computation())));
        }
        if (counters != null && !counters.isEmpty()) {
            counters = ImmutableList.copyOf(counters);
            observables.add(metricsService
                    .addCounterData(requestToCounters(tenantId, counters).subscribeOn(Schedulers.computation())));
        }
        if (availabilities != null && !availabilities.isEmpty()) {
            availabilities = ImmutableList.copyOf(availabilities);
            observables.add(metricsService
                    .addAvailabilityData(requestToAvailabilities(tenantId, ImmutableList.copyOf(availabilities))
                            .subscribeOn(Schedulers.computation())));
        }

        try {
            return Observable.merge(observables).map(ApiUtils::simpleOKResponse).toBlocking().last();
        } catch (Exception e) {
            return ApiUtils.serverError(e);
        }
    }
}