/*
 * GNU GENERAL PUBLIC LICENSE
 *                        Version 2, June 1991
 *
 *  Copyright (C) 1989, 1991 Free Software Foundation, Inc., <http://fsf.org/>
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *  Everyone is permitted to copy and distribute verbatim copies
 *  of this license document, but changing it is not allowed.
 */
package de.qaware.chronix.solr.query.analysis.functions

import spock.lang.Specification

/**
 * Unit test for the analysis type enum
 * @author f.lautenschlager
 */
class FunctionTypeTest extends Specification {

    def "test analyses types"() {
        when:
        def result = FunctionType.isAggregation(type)

        then:
        result == expected

        where:
        type << [FunctionType.MIN, FunctionType.MAX, FunctionType.AVG, FunctionType.DEV, FunctionType.P, FunctionType.TREND, FunctionType.OUTLIER, FunctionType.FREQUENCY, FunctionType.FASTDTW]
        expected << [true, true, true, true, true, false, false, false, false]
    }
}
