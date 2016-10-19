/*
 * GNU GENERAL PUBLIC LICENSE
 *                        Version 2, June 1991
 *
 *  Copyright (C) 1989, 1991 Free Software Foundation, Inc., <http://fsf.org/>
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *  Everyone is permitted to copy and distribute verbatim copies
 *  of this license document, but changing it is not allowed.
 */
package de.qaware.chronix.solr.query.analysis.functions.transformation;

import de.qaware.chronix.solr.query.analysis.functions.ChronixTransformation;
import de.qaware.chronix.solr.query.analysis.functions.FunctionType;
import de.qaware.chronix.solr.query.analysis.functions.FunctionValueMap;
import de.qaware.chronix.timeseries.MetricTimeSeries;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * The subtract transformation
 *
 * @author f.lautenschlager
 */
public final class Subtract implements ChronixTransformation<MetricTimeSeries> {

    private final double value;

    /**
     * Constructs the subtract transformation.
     * The value is subtracted from each time series value
     *
     * @param value the value that is subtracted
     */
    public Subtract(double value) {
        this.value = value;
    }

    /**
     * Subtracts the value from each time series value
     *
     * @param timeSeries the time series that is transformed
     * @return the transformed time series
     */
    @Override
    public void execute(MetricTimeSeries timeSeries, FunctionValueMap functionValueMap) {
        long[] timestamps = timeSeries.getTimestampsAsArray();
        double[] values = timeSeries.getValuesAsArray();

        timeSeries.clear();

        for (int i = 0; i < values.length; i++) {
            values[i] -= value;
        }

        timeSeries.addAll(timestamps, values);
        functionValueMap.add(this);
    }

    @Override
    public FunctionType getType() {
        return FunctionType.SUB;
    }

    @Override
    public String[] getArguments() {
        return new String[]{"value=" + value};
    }


    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("value", value)
                .toString();
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
        Subtract rhs = (Subtract) obj;
        return new EqualsBuilder()
                .append(this.value, rhs.value)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(value)
                .toHashCode();
    }
}
