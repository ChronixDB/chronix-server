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
package de.qaware.chronix.solr.type.metric.functions.transformation

import de.qaware.chronix.server.functions.FunctionCtx
import de.qaware.chronix.solr.type.metric.ChronixMetricTimeSeries
import de.qaware.chronix.timeseries.MetricTimeSeries
import spock.lang.Specification

import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Unit test for the value transformation
 * @author f.lautenschlager
 */
class ScaleTest extends Specification {

    def "test scale"() {
        given:
        def timeSeriesBuilder = new MetricTimeSeries.Builder("Scale", "metric")
        def now = Instant.now()

        100.times {
            timeSeriesBuilder.point(now.plus(it, ChronoUnit.SECONDS).toEpochMilli(), it + 1)
        }

        def scale = new Scale()
        scale.setArguments(["2"] as String[])
        def timeSeries = new ChronixMetricTimeSeries("", timeSeriesBuilder.build())
        def analysisResult = new FunctionCtx(1, 1, 1)

        when:
        scale.execute([timeSeries] as List, analysisResult)

        then:
        100.times {
            timeSeries.getRawTimeSeries().getValue(it) == (it + 1) * 2
        }
    }

    def "test getType"() {
        when:
        def scale = new Scale()
        scale.setArguments(["2"] as String[])

        then:
        scale.getQueryName() == "scale"
    }

    def "test getArguments"() {
        when:
        def scale = new Scale()
        scale.setArguments(["2"] as String[])
        then:
        scale.getArguments()[0] == "value=2.0"
    }

    def "test equals and hash code"() {
        given:
        def function = new Scale()
        function.setArguments(["4"] as String[])

        def sameFunction = new Scale()
        sameFunction.setArguments(["4"] as String[])

        def otherFunction = new Scale()
        otherFunction.setArguments(["2"] as String[])

        expect:
        function != null
        function != new Object()
        function == function
        function == sameFunction
        function.hashCode() == sameFunction.hashCode()
        function.hashCode() != otherFunction.hashCode()
    }

    def "test string representation"() {
        expect:
        def function = new Scale()
        function.setArguments(["4"] as String[])
        function.toString().contains("value")
    }
}
