/*
 * Copyright (C) 2018 QAware GmbH
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package de.qaware.chronix.solr.type.metric.functions.aggregations;

import de.qaware.chronix.server.functions.ChronixAggregation;
import de.qaware.chronix.server.functions.FunctionCtx;
import de.qaware.chronix.server.types.ChronixTimeSeries;
import de.qaware.chronix.timeseries.MetricTimeSeries;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.List;

/**
 * The minimum aggregation
 *
 * @author f.lautenschlager
 */
public class Min implements ChronixAggregation<MetricTimeSeries> {

    /**
     * Calculates the minimum value of the first time series.
     *
     * @param timeSeriesList list with time series
     * @return the minimum or 0 if the list is empty
     */
    @Override
    public void execute(List<ChronixTimeSeries<MetricTimeSeries>> timeSeriesList, FunctionCtx functionCtx) {
        for (ChronixTimeSeries<MetricTimeSeries> chronixTimeSeries : timeSeriesList) {

            MetricTimeSeries timeSeries = chronixTimeSeries.getRawTimeSeries();

            //If it is empty, we return NaN
            if (timeSeries.size() <= 0) {
                functionCtx.add(this, Double.NaN, chronixTimeSeries.getJoinKey());
                continue;
            }

            //Else calculate the analysis value
            int size = timeSeries.size();
            double min = timeSeries.getValue(0);

            for (int i = 1; i < size; i++) {
                double next = timeSeries.getValue(i);
                if (next < min) {
                    min = next;
                }
            }
            functionCtx.add(this, min, chronixTimeSeries.getJoinKey());
        }
    }

    @Override
    public String getQueryName() {
        return "min";
    }

    @Override
    public String getType() {
        return "metric";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        return new EqualsBuilder()
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .toHashCode();
    }
}
