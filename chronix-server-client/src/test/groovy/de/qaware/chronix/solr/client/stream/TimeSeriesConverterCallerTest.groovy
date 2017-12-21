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
package de.qaware.chronix.solr.client.stream
import de.qaware.chronix.Schema
import de.qaware.chronix.converter.BinaryTimeSeries
import de.qaware.chronix.solr.test.converter.DefaultTimeSeriesConverter
import org.apache.solr.common.SolrDocument
import spock.lang.Specification

import java.time.Instant
import java.time.temporal.ChronoUnit
/**
 * Unit test for the document converter
 * @author f.lautenschlager
 */
class TimeSeriesConverterCallerTest extends Specification {

    def "test call convert document for an remote solr"() {
        given:
        def start = Instant.now().toEpochMilli()
        def end = Instant.now().plusSeconds(180).toEpochMilli()

        //Create a solr document
        def solrDocument = new SolrDocument()
        solrDocument.addField(Schema.START, start)
        solrDocument.addField(Schema.END, end)
        solrDocument.addField(Schema.DATA, "someBytes".bytes)
        solrDocument.addField("SomeField", ChronoUnit.SECONDS.toString());
        solrDocument.addField("SomeList", Arrays.asList("Hello", "I", "like", "Chronix"));

        def converter = new TimeSeriesConverterCaller(solrDocument, new DefaultTimeSeriesConverter(), start, end)

        when:
        BinaryTimeSeries ts = converter.call()

        then:
        ts.get(Schema.START) == start
        ts.get(Schema.END) == end
        ts.get(Schema.DATA) == "someBytes".bytes
        ts.get("SomeField") == ChronoUnit.SECONDS.toString()
        ts.get("SomeList") == Arrays.asList("Hello", "I", "like", "Chronix")
    }

}
