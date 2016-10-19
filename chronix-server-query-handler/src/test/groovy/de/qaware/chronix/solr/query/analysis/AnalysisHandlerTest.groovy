/*
 * GNU GENERAL PUBLIC LICENSE
 *                        Version 2, June 1991
 *
 *  Copyright (C) 1989, 1991 Free Software Foundation, Inc., <http://fsf.org/>
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *  Everyone is permitted to copy and distribute verbatim copies
 *  of this license document, but changing it is not allowed.
 */
package de.qaware.chronix.solr.query.analysis

import de.qaware.chronix.converter.common.Compression
import de.qaware.chronix.converter.serializer.protobuf.ProtoBufMetricTimeSeriesSerializer
import de.qaware.chronix.solr.query.ChronixQueryParams
import de.qaware.chronix.solr.query.analysis.functions.aggregations.Max
import de.qaware.chronix.solr.query.analysis.functions.analyses.FastDtw
import de.qaware.chronix.solr.query.analysis.functions.analyses.Trend
import de.qaware.chronix.solr.query.analysis.functions.transformation.Add
import de.qaware.chronix.solr.query.analysis.providers.SolrDocListProvider
import de.qaware.chronix.timeseries.MetricTimeSeries
import org.apache.solr.common.SolrDocument
import org.apache.solr.common.params.ModifiableSolrParams
import org.apache.solr.core.PluginInfo
import org.apache.solr.request.SolrQueryRequest
import org.apache.solr.response.SolrQueryResponse
import org.apache.solr.schema.IndexSchema
import org.apache.solr.schema.SchemaField
import org.apache.solr.search.DocSlice
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.ByteBuffer
import java.time.Instant
import java.util.function.Function

/**
 * Unit test for the analysis handler.
 * @author f.lautenschlager
 */
class AnalysisHandlerTest extends Specification {

    def "test handle function request"() {
        given:
        def request = Mock(SolrQueryRequest)
        def indexSchema = Mock(IndexSchema)

        indexSchema.getFields() >> new HashMap<String, SchemaField>()
        request.getSchema() >> indexSchema
        request.getParams() >> params

        def docListMock = Stub(DocListProvider)
        docListMock.doSimpleQuery(_, _, _, _) >> { new DocSlice(0i, 0, [] as int[], [] as float[], 0, 0) }

        def analysisHandler = new AnalysisHandler(docListMock)

        when:
        analysisHandler.handleRequestBody(request, Mock(SolrQueryResponse))

        then:
        noExceptionThrown()

        where:
        params << [new ModifiableSolrParams().add("q", "host:laptop AND start:NOW")
                           .add("rows", "0"),
                   new ModifiableSolrParams().add("q", "host:laptop AND start:NOW")
                           .add("fq", "ag=max").add(ChronixQueryParams.QUERY_START_LONG, "0")
                           .add(ChronixQueryParams.QUERY_END_LONG, String.valueOf(Long.MAX_VALUE)),
                   new ModifiableSolrParams().add("q", "host:laptop AND start:NOW").add("fl", "myfield,start,end,data,metric")
                           .add("fq", "analysis=fastdtw:(metric:* AND start:NOW),10,0.5").add(ChronixQueryParams.QUERY_START_LONG, "0")
                           .add(ChronixQueryParams.QUERY_END_LONG, String.valueOf(Long.MAX_VALUE)),
                   new ModifiableSolrParams().add("q", "host:laptop AND start:NOW")
                           .add("fq", "analysis=trend").add(ChronixQueryParams.QUERY_START_LONG, "0")
                           .add(ChronixQueryParams.QUERY_END_LONG, String.valueOf(Long.MAX_VALUE)),
        ]

    }

    def "test get fields"() {
        given:
        def docListMock = Stub(DocListProvider)
        def analysisHandler = new AnalysisHandler(docListMock)

        when:
        def fields = analysisHandler.getFields(concatedFields, new HashMap<String, SchemaField>())

        then:
        fields == result

        where:
        concatedFields << [null, "myField,start,end,data,metric"]
        result << [new HashSet<>(), ["myField", "start", "end", "data", "metric"] as Set<String>]
    }

    @Shared
    def functions = new QueryFunctions<>()

    @Unroll
    def "test single time series for #queryFunction"() {
        given:
        def docListMock = Stub(DocListProvider)
        def analysisHandler = new AnalysisHandler(docListMock)
        def start = Instant.now()
        Map<String, List<SolrDocument>> timeSeriesRecords = new HashMap<>()
        timeSeriesRecords.put("something", solrDocument(start))

        def request = Mock(SolrQueryRequest)
        request.params >> new ModifiableSolrParams().add("q", "host:laptop AND start:NOW")
                .add("fq", "function=max").add(ChronixQueryParams.QUERY_START_LONG, "0")
                .add(ChronixQueryParams.QUERY_END_LONG, String.valueOf(Long.MAX_VALUE))
        function()

        Function<SolrDocument, String> key = new JoinFunction(null);


        when:
        def result = analysisHandler.analyze(request, functions, key, timeSeriesRecords, false)

        then:
        result.size() == 1
        result.get(0).get(resultKey) == expectedResult

        where:
        queryFunction << ["function=max",
                          "function=trend",
                          "function=add:5"]
        function << [{ -> functions.addAggregation(new Max()) },
                     { -> functions.addAnalysis(new Trend()) },
                     { -> functions.addTransformation(new Add(5)) }]

        resultKey << ["0_function_max",
                      "0_function_trend",
                      "0_function_add"]

        expectedResult << [4713, null, ["value=5.0"]]
    }

    def "test function with multiple time series"() {
        given:
        def docListMock = Stub(DocListProvider)
        def analysisHandler = new AnalysisHandler(docListMock)
        def start = Instant.now();

        Map<String, List<SolrDocument>> timeSeriesRecords = new HashMap<>()
        timeSeriesRecords.put("something", solrDocument(start))

        Map<String, List<SolrDocument>> timeSeriesRecordsFromSubQuery = new HashMap<>()
        timeSeriesRecordsFromSubQuery.put("something", solrDocument(start))
        timeSeriesRecordsFromSubQuery.put("something-other", solrDocument(start))

        def request = Mock(SolrQueryRequest)
        def indexSchema = Mock(IndexSchema)

        indexSchema.getFields() >> new HashMap<String, SchemaField>()
        request.getSchema() >> indexSchema

        request.params >> new ModifiableSolrParams().add("q", "host:laptop AND start:NOW")
                .add("fq", "ag=max").add(ChronixQueryParams.QUERY_START_LONG, "0")
                .add(ChronixQueryParams.QUERY_END_LONG, String.valueOf(Long.MAX_VALUE))
        def analyses = new QueryFunctions<>()
        analyses.addAnalysis(new FastDtw("ignored", 1, 0.8))
        Function<SolrDocument, String> key = new JoinFunction(null);

        when:
        analysisHandler.metaClass.collectDocuments = { -> return timeSeriesRecordsFromSubQuery }
        def result = analysisHandler.analyze(request, analyses, key, timeSeriesRecords, false)

        then:
        result.size() == 0
    }

    def "test get description"() {
        given:
        def analysisHandler = new AnalysisHandler(new SolrDocListProvider())

        when:
        def description = analysisHandler.getDescription()

        then:
        description == "Chronix Aggregation Request Handler"
    }

    def "test init and inform"() {
        given:
        def pluginInfo = Mock(PluginInfo.class)
        def analysisHandler = new AnalysisHandler(new SolrDocListProvider())

        when:
        analysisHandler.init(pluginInfo)
        analysisHandler.inform(null)

        then:
        thrown NullPointerException
    }

    List<SolrDocument> solrDocument(Instant start) {
        def result = new ArrayList<SolrDocument>()
        def ts = new MetricTimeSeries.Builder("test")
                .point(start.toEpochMilli(), 4711)
                .point(start.plusSeconds(1).toEpochMilli(), 4712)
                .point(start.plusSeconds(2).toEpochMilli(), 4713)
                .build()
        SolrDocument doc = new SolrDocument()
        doc.put("start", start.toEpochMilli())
        doc.put("end", start.plusSeconds(10).toEpochMilli())
        doc.put("metric", "test")
        def data = ProtoBufMetricTimeSeriesSerializer.to(ts.points().iterator())
        def compressed = Compression.compress(data)
        doc.put("data", ByteBuffer.wrap(compressed))

        result.add(doc)
        return result
    }
}
