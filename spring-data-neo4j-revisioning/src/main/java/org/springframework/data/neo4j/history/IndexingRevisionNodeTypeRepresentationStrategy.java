/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.neo4j.history;

import org.neo4j.graphdb.Node;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.core.NodeTypeRepresentationStrategy;
import org.springframework.data.neo4j.support.index.IndexProvider;
import org.springframework.data.neo4j.support.typerepresentation.AbstractIndexingTypeRepresentationStrategy;

public class IndexingRevisionNodeTypeRepresentationStrategy extends AbstractRevisionIndexingTypeRepresentationStrategy<Node> implements
        NodeTypeRepresentationStrategy {

    public static final String INDEX_NAME = "__types_rev__";

    public IndexingRevisionNodeTypeRepresentationStrategy(GraphDatabase graphDb, IndexProvider indexProvider) {
        super(graphDb, indexProvider, INDEX_NAME, Node.class);
    }
}