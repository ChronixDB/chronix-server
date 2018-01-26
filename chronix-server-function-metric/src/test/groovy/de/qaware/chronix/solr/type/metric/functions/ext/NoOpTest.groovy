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
package de.qaware.chronix.solr.type.metric.functions.ext

import de.qaware.chronix.server.functions.FunctionCtx
import de.qaware.chronix.solr.type.metric.ChronixMetricTimeSeries
import de.qaware.chronix.timeseries.MetricTimeSeries
import spock.lang.Specification

/**
 * NoOp unit test
 * @author f.lautenschlager
 */
class NoOpTest extends Specification {

    def "execute"() {

        given:
        def noOp = new NoOp()
        def timeSeries = new MetricTimeSeries.Builder("noop", "metric").build()
        def ctsList = [new ChronixMetricTimeSeries("bla", timeSeries)]
        def functionCtx = new FunctionCtx(1, 1, 1, 1)

        when:
        noOp.execute(ctsList, functionCtx)

        then:
        timeSeries == timeSeries

    }

    def "getQueryName"() {
        expect:
        new NoOp().queryName == "noop"
    }

    def "getTimeSeriesType"() {
        expect:
        new NoOp().type == "metric"
    }
}
