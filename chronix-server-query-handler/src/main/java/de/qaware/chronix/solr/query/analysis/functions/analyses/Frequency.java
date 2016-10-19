/*
 * GNU GENERAL PUBLIC LICENSE
 *                        Version 2, June 1991
 *
 *  Copyright (C) 1989, 1991 Free Software Foundation, Inc., <http://fsf.org/>
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *  Everyone is permitted to copy and distribute verbatim copies
 *  of this license document, but changing it is not allowed.
 */
package de.qaware.chronix.solr.query.analysis.functions.analyses;

import de.qaware.chronix.converter.common.LongList;
import de.qaware.chronix.solr.query.analysis.functions.ChronixAnalysis;
import de.qaware.chronix.solr.query.analysis.functions.FunctionType;
import de.qaware.chronix.solr.query.analysis.functions.FunctionValueMap;
import de.qaware.chronix.timeseries.MetricTimeSeries;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * The frequency detection analysis.
 * Checks if the occurrence within a defined window (in minutes) is above a defined threshold.
 *
 * @author f.lautenschlager
 */
public final class Frequency implements ChronixAnalysis<MetricTimeSeries> {

    private final long windowSize;
    private final long windowThreshold;

    /**
     * Constructs a frequency detection
     *
     * @param windowSize      the window size in minutes
     * @param windowThreshold the threshold per window
     */
    public Frequency(long windowSize, long windowThreshold) {
        this.windowSize = windowSize;
        this.windowThreshold = windowThreshold;
    }

    /**
     * The frequency detector splits a time series into windows, counts the data points, and checks if the delta
     * between two windows is above a predefined threshold.
     * <p>
     * The frequency detector splits a time series using the constructor argument.
     *
     * @param functionValueMap
     * @return true if the time series has a pair of windows 1 and 2 where 2 has th
     */
    @Override
    public void execute(MetricTimeSeries timeSeries, FunctionValueMap functionValueMap) {

        LongList timestamps = timeSeries.getTimestamps();

        final List<Long> currentWindow = new ArrayList<>();
        final List<Integer> windowCount = new ArrayList<>();

        //start and end of the window
        long windowStart = timestamps.get(0);
        //calculate the end
        long windowEnd = Instant.ofEpochMilli(windowStart).plus(windowSize, ChronoUnit.MINUTES).toEpochMilli();

        for (int i = 1; i < timeSeries.size(); i++) {
            long current = timestamps.get(i);
            //Add the occurrence of the current window.
            if (current > windowStart - 1 && current < (windowEnd)) {
                currentWindow.add(current);
            } else {
                //We reached the end. Lets add it to the window count
                windowCount.add(currentWindow.size());
                windowStart = current;
                windowEnd = Instant.ofEpochMilli(windowStart).plus(windowSize, ChronoUnit.MINUTES).toEpochMilli();
                currentWindow.clear();
            }
        }
        //we are done, add the last window
        windowCount.add(currentWindow.size());

        //check deltas
        for (int i = 1; i < windowCount.size(); i++) {

            int former = windowCount.get(i - 1);
            int current = windowCount.get(i);

            //The threshold
            int result = current - former;
            if (result >= windowThreshold) {
                //add the time series as there are more points per window than the threshold
                functionValueMap.add(this, true, null);
                return;
            }
        }
        //Nothing bad found
        functionValueMap.add(this, false, null);

    }

    @Override
    public String[] getArguments() {
        return new String[]{"window size=" + windowSize, "window threshold=" + windowThreshold};
    }

    @Override
    public FunctionType getType() {
        return FunctionType.FREQUENCY;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("windowSize", windowSize)
                .append("windowThreshold", windowThreshold)
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
        Frequency rhs = (Frequency) obj;
        return new EqualsBuilder()
                .append(this.windowSize, rhs.windowSize)
                .append(this.windowThreshold, rhs.windowThreshold)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(windowSize)
                .append(windowThreshold)
                .toHashCode();
    }
}
