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
package de.qaware.chronix.solr.compaction

import org.apache.lucene.document.Document
import org.apache.solr.common.SolrInputDocument
import spock.lang.Specification

/**
 * Test case for {@link CompactionResult}.
 *
 * @author alex.christ
 */
class CompactionResultTest extends Specification {
    def "test getter"() {
        given:
        def originalDocuments = [new Document()] as Set
        def resultingDocuments = [new SolrInputDocument()] as Set

        when:
        def result = new CompactionResult(originalDocuments, resultingDocuments)

        then:
        result.inputDocuments == originalDocuments
        result.outputDocuments == resultingDocuments
    }
}
