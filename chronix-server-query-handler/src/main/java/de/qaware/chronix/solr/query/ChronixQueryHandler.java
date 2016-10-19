/*
 * GNU GENERAL PUBLIC LICENSE
 *                        Version 2, June 1991
 *
 *  Copyright (C) 1989, 1991 Free Software Foundation, Inc., <http://fsf.org/>
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *  Everyone is permitted to copy and distribute verbatim copies
 *  of this license document, but changing it is not allowed.
 */
package de.qaware.chronix.solr.query;

import de.qaware.chronix.Schema;
import de.qaware.chronix.converter.common.MetricTSSchema;
import de.qaware.chronix.solr.query.analysis.AnalysisHandler;
import de.qaware.chronix.solr.query.analysis.providers.SolrDocListProvider;
import de.qaware.chronix.solr.query.date.DateQueryParser;
import org.apache.solr.common.StringUtils;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.core.PluginInfo;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.RequestHandlerBase;
import org.apache.solr.handler.component.SearchHandler;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.util.plugin.PluginInfoInitialized;
import org.apache.solr.util.plugin.SolrCoreAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * The date range query handler to convert date expression and
 * delegate the query to the default search handler
 *
 * @author f.lautenschlager
 */
public class ChronixQueryHandler extends RequestHandlerBase implements SolrCoreAware, PluginInfoInitialized {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChronixQueryHandler.class);

    private static final Set<String> REQUIRED_FIELDS = new HashSet<>();
    /**
     * The default solr search handler
     */
    private final SearchHandler searchHandler = new SearchHandler();

    /**
     * The analysis handler
     */
    private final SearchHandler analysisHandler = new AnalysisHandler(new SolrDocListProvider());

    /**
     * The date range parser
     */
    private final DateQueryParser dateRangeParser = new DateQueryParser(new String[]{ChronixQueryParams.DATE_START_FIELD, ChronixQueryParams.DATE_END_FIELD});


    static {
        REQUIRED_FIELDS.add(Schema.DATA);
        REQUIRED_FIELDS.add(Schema.START);
        REQUIRED_FIELDS.add(Schema.END);
        REQUIRED_FIELDS.add(MetricTSSchema.METRIC);
    }

    @Override
    public void init(PluginInfo info) {
        searchHandler.init(info);
        analysisHandler.init(info);
    }

    @Override
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse rsp) throws Exception {
        LOGGER.debug("Handling request {}", req);
        final ModifiableSolrParams modifiableSolrParams = new ModifiableSolrParams(req.getParams());

        final String originQuery = modifiableSolrParams.get(CommonParams.Q);

        final long[] startAndEnd = dateRangeParser.getNumericQueryTerms(originQuery);
        final long queryStart = or(startAndEnd[0], -1L, 0L);
        final long queryEnd = or(startAndEnd[1], -1L, Long.MAX_VALUE);

        modifiableSolrParams.set(ChronixQueryParams.QUERY_START_LONG, String.valueOf(queryStart));
        modifiableSolrParams.set(ChronixQueryParams.QUERY_END_LONG, String.valueOf(queryEnd));
        final String query = dateRangeParser.replaceRangeQueryTerms(originQuery);
        modifiableSolrParams.set(CommonParams.Q, query);

        //Set the min required fields if the user define a sub set of fields
        final String fields = modifiableSolrParams.get(CommonParams.FL);
        modifiableSolrParams.set(CommonParams.FL, requestedFields(fields, req.getSchema().getFields().keySet()));
        //Set the updated query
        req.setParams(modifiableSolrParams);

        //check the filter queries
        final String[] filterQueries = modifiableSolrParams.getParams(CommonParams.FQ);

        //if we have an function query or someone wants the data as json
        if (contains(filterQueries, ChronixQueryParams.FUNCTION_PARAM) || contains(ChronixQueryParams.DATA_AS_JSON, fields) || contains(filterQueries,ChronixQueryParams.JOIN_PARAM)) {
            LOGGER.debug("Request is an analysis request.");
            analysisHandler.handleRequestBody(req, rsp);
        } else {
            //let the default search handler do its work
            LOGGER.debug("Request is a default request");
            searchHandler.handleRequestBody(req, rsp);
        }

        //add the converted start and end to the response
        rsp.getResponseHeader().add(ChronixQueryParams.QUERY_START_LONG, queryStart);
        rsp.getResponseHeader().add(ChronixQueryParams.QUERY_END_LONG, queryEnd);
    }

    private <T> T or(T value, T condition, T or) {
        if (value.equals(condition)) {
            return or;
        } else {
            return value;
        }
    }

    /**
     * Gets the requested fields.
     * Joins the REQUIRED_FIELDS_WITH_DATA and the user defined fields.
     * E.g.:
     * user requested fields: userField
     * => data,start,end,metric,userField
     *
     * @param fl     the solr fl param
     * @param schema the solr schema
     * @return the user defined fields and the required fields, or null if fl is null
     */
    private String requestedFields(String fl, Set<String> schema) {
        //As a result Solr will return everything
        if (fl == null || fl.isEmpty()) {
            return null;
        }

        //the user wants a single additional field
        if (fl.indexOf('-') == -1 && fl.indexOf('+') == -1) {
            //hence we return the required fields and the requested fields
            return fl + ChronixQueryParams.JOIN_SEPARATOR + String.join(ChronixQueryParams.JOIN_SEPARATOR, REQUIRED_FIELDS);
        } else {
            //the user removes or adds a field to all fields
            Set<String> fields = new HashSet<>(Arrays.asList(fl.split(ChronixQueryParams.JOIN_SEPARATOR)));

            //Check if we have only fields to remove
            Set<String> resultingFields = new HashSet<>(schema);

            //If a user requests the data as json (fl=dataAsJson)
            if (fl.contains(ChronixQueryParams.DATA_AS_JSON)) {
                //if the field is dataAsJson -> add it to the fields.
                resultingFields.add(ChronixQueryParams.DATA_AS_JSON);
            }

            //remove fields that are marked with minus sign '-'
            for (String field : fields) {
                //we only remove the fields. We have already added all fields
                if (field.indexOf('-') > -1) {
                    //one can remove the data field
                    resultingFields.remove(field.substring(1));
                }

            }
            return String.join(ChronixQueryParams.JOIN_SEPARATOR, resultingFields);

        }
    }

    /**
     * Checks if the given string array (filter queries) contains the given identifier.
     *
     * @param filterQueries the filter queries
     * @param identifier    the identifier
     * @return true if the filter queries contains the identifier, otherwise false.
     */
    private boolean contains(String[] filterQueries, String identifier) {
        if (filterQueries == null) {
            return false;
        }

        for (String filterQuery : filterQueries) {
            if (filterQuery.contains(identifier)) {
                return true;
            }
        }
        return false;
    }

    private boolean contains(String field, String fields) {
        return !(StringUtils.isEmpty(field) || StringUtils.isEmpty(fields)) && fields.contains(field);
    }


    @Override
    public String getDescription() {
        return "Chronix range query handler. Delegates to the default search handler";
    }

    @Override
    public void inform(SolrCore core) {
        searchHandler.inform(core);
        analysisHandler.inform(core);
    }
}
