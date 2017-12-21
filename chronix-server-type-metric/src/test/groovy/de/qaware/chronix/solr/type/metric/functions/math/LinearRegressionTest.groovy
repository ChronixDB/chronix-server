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
package de.qaware.chronix.solr.type.metric.functions.math

import de.qaware.chronix.converter.common.DoubleList
import de.qaware.chronix.converter.common.LongList
import spock.lang.Specification

/**
 * Unit test for the regression class
 * @author f.lautenschlager
 */
class LinearRegressionTest extends Specification {
    def "test slope"() {
        given:
        def times = new LongList()
        def values = new DoubleList()
        100.times {
            times.add(it as long)
            values.add(it * 2 as double)
        }

        when:
        def slope = new LinearRegression(times, values).slope()

        then:
        slope == 2d
    }
}
