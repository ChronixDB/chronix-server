/*
 * GNU GENERAL PUBLIC LICENSE
 *                        Version 2, June 1991
 *
 *  Copyright (C) 1989, 1991 Free Software Foundation, Inc., <http://fsf.org/>
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *  Everyone is permitted to copy and distribute verbatim copies
 *  of this license document, but changing it is not allowed.
 */
package de.qaware.chronix.solr.ingestion.format;

import com.google.common.collect.Lists;
import de.qaware.chronix.timeseries.MetricTimeSeries;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class OpenTsdbHttpFormatParserTest {
    private OpenTsdbHttpFormatParser sut;

    @Before
    public void setUp() throws Exception {
        sut = new OpenTsdbHttpFormatParser();
    }

    @Test
    public void testSingle() throws Exception {
        try (InputStream stream = GraphiteFormatParserTest.class.getResourceAsStream("/opentsdb-http-api-single.json")) {
            assertNotNull(stream);
            List<MetricTimeSeries> series = Lists.newArrayList(sut.parse(stream));

            // We should have one metric
            assertThat(series.size(), is(1));

            MetricTimeSeries metric = series.get(0);
            assertThat(metric.getMetric(), is("sys.cpu.nice"));
            assertThat(metric.getTimestamps().size(), is(1));
            assertThat(metric.getValues().get(0), is(18.0));
            assertThat(metric.getTimestamps().get(0), is(1346846400000L));
        }
    }

    @Test
    public void testMultiple() throws Exception {
        try (InputStream stream = GraphiteFormatParserTest.class.getResourceAsStream("/opentsdb-http-api-multiple.json")) {
            assertNotNull(stream);
            List<MetricTimeSeries> series = Lists.newArrayList(sut.parse(stream));

            // We should have two metrics
            assertThat(series.size(), is(2));

            MetricTimeSeries first = series.get(0);
            assertThat(first.getMetric(), is("sys.cpu.nice"));
            assertThat(first.getTimestamps().size(), is(2));
            assertThat(first.getValues().get(0), is(1.0));
            assertThat(first.getTimestamps().get(0), is(1346846400000L));
            assertThat(first.getValues().get(1), is(2.0));
            assertThat(first.getTimestamps().get(1), is(1346846400100L));

            MetricTimeSeries second = series.get(1);
            assertThat(second.getMetric(), is("sys.cpu.load"));
            assertThat(second.getTimestamps().size(), is(1));
            assertThat(second.getValues().get(0), is(2.0));
            assertThat(second.getTimestamps().get(0), is(1346846400000L));
        }
    }
}