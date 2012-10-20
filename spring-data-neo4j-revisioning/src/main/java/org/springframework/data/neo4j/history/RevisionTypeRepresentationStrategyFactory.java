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

import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.core.NodeTypeRepresentationStrategy;
import org.springframework.data.neo4j.core.RelationshipTypeRepresentationStrategy;
import org.springframework.data.neo4j.support.index.IndexProvider;
import org.springframework.data.neo4j.support.typerepresentation.TypeRepresentationStrategyFactory;

public class RevisionTypeRepresentationStrategyFactory extends TypeRepresentationStrategyFactory {

    private final IndexingRevisionNodeTypeRepresentationStrategy nodeTypeRepresentationStrategy;

    private final IndexingRevisionRelationshipTypeRepresentationStrategy relationshipTypeRepresentationStrategy;

    public RevisionTypeRepresentationStrategyFactory(GraphDatabase graphDatabase) {
        super(graphDatabase);
        nodeTypeRepresentationStrategy = new IndexingRevisionNodeTypeRepresentationStrategy(graphDatabase, null);
        relationshipTypeRepresentationStrategy = new IndexingRevisionRelationshipTypeRepresentationStrategy(graphDatabase, null);
    }

    public RevisionTypeRepresentationStrategyFactory(GraphDatabase graphDatabase, IndexProvider indexProvider) {
        super(graphDatabase, indexProvider);
        nodeTypeRepresentationStrategy = new IndexingRevisionNodeTypeRepresentationStrategy(graphDatabase, indexProvider);
        relationshipTypeRepresentationStrategy = new IndexingRevisionRelationshipTypeRepresentationStrategy(graphDatabase, indexProvider);

    }

    @Override
    public NodeTypeRepresentationStrategy getNodeTypeRepresentationStrategy() {
        return nodeTypeRepresentationStrategy;
    }

    @Override
    public RelationshipTypeRepresentationStrategy getRelationshipTypeRepresentationStrategy() {
        return relationshipTypeRepresentationStrategy;
    }
}