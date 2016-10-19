/*
 * GNU GENERAL PUBLIC LICENSE
 *                        Version 2, June 1991
 *
 *  Copyright (C) 1989, 1991 Free Software Foundation, Inc., <http://fsf.org/>
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *  Everyone is permitted to copy and distribute verbatim copies
 *  of this license document, but changing it is not allowed.
 */
package de.qaware.chronix.solr.query.analysis.functions.transformation

import de.qaware.chronix.solr.query.analysis.functions.FunctionType
import de.qaware.chronix.solr.query.analysis.functions.FunctionValueMap
import de.qaware.chronix.timeseries.MetricTimeSeries
import spock.lang.Specification

import java.time.Instant

/**
 * Unit test for the derivative transformation
 *
 * @author f.lautenschlager
 */
class DerivativeTest extends Specification {
    def "test transform"() {
        given:
        def timeSeriesBuilder = new MetricTimeSeries.Builder("Derivative time series")
        def derivative = new Derivative()
        def analysisResult = new FunctionValueMap(1, 1, 1);

        timeSeriesBuilder.point(dateOf("2016-05-23T10:51:00.000Z"), 5)
        timeSeriesBuilder.point(dateOf("2016-05-23T10:51:01.000Z"), 4)

        timeSeriesBuilder.point(dateOf("2016-05-23T10:51:06.500Z"), 6)
        timeSeriesBuilder.point(dateOf("2016-05-23T10:51:07.000Z"), 10)
        timeSeriesBuilder.point(dateOf("2016-05-23T10:51:08.000Z"), 31)
        timeSeriesBuilder.point(dateOf("2016-05-23T10:51:09.000Z"), 9)
        timeSeriesBuilder.point(dateOf("2016-05-23T10:51:10.000Z"), 2)

        timeSeriesBuilder.point(dateOf("2016-05-23T10:51:15.000Z"), 1)
        timeSeriesBuilder.point(dateOf("2016-05-23T10:51:16.000Z"), 5)

        def timeSeries = timeSeriesBuilder.build()
        when:
        derivative.execute(timeSeries, analysisResult)

        then:
        timeSeries.size() == 7
    }

    def long dateOf(def format) {
        Instant.parse(format as String).toEpochMilli()
    }

    def "test getType"() {
        expect:
        new Derivative().getType() == FunctionType.DERIVATIVE;
    }

    def "test getArguments"() {
        expect:
        new Derivative().getArguments().length == 0
    }

    def "test equals and hash code"() {
        expect:
        def function = new Derivative();
        !function.equals(null)
        !function.equals(new Object())
        function.equals(function)
        function.equals(new Derivative())
        new Derivative().hashCode() == new Derivative().hashCode()
    }
}
