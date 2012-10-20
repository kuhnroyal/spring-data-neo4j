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
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.collection.ClosableIterable;
import org.springframework.data.neo4j.core.TypeRepresentationStrategy;
import org.springframework.data.neo4j.support.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.support.mapping.StoredEntityType;
import org.springframework.data.neo4j.support.typerepresentation.TypeRepresentationStrategies;

public class RevisionTypeRepresentationStrategies extends TypeRepresentationStrategies implements VersionedTypeRepresentationStrategy<PropertyContainer> {

    public RevisionTypeRepresentationStrategies(Neo4jMappingContext mappingContext, TypeRepresentationStrategy<Node> nodeTypeRepresentationStrategy, TypeRepresentationStrategy<Relationship> relationshipTypeRepresentationStrategy) {
        super(mappingContext, nodeTypeRepresentationStrategy, relationshipTypeRepresentationStrategy);
    }

    @Override
    public <U> ClosableIterable<PropertyContainer> findAll(StoredEntityType type, long revisionNumber) {
        return (ClosableIterable<PropertyContainer>) ((VersionedTypeRepresentationStrategy) getTypeRepresentationStrategy(type)).findAll(type, revisionNumber);
    }

    @Override
    public long count(StoredEntityType type, long revisionNumber) {
        return ((VersionedTypeRepresentationStrategy) getTypeRepresentationStrategy(type)).count(type, revisionNumber);
    }

    @SuppressWarnings("unchecked")
    private <T> TypeRepresentationStrategy<?> getTypeRepresentationStrategy(StoredEntityType type) {
        if (type.isNodeEntity()) {
            return getNodeTypeRepresentationStrategy();
        } else if (type.isRelationshipEntity()) {
            return getRelationshipTypeRepresentationStrategy();
        }
        throw new IllegalArgumentException("Type is not @NodeEntity nor @RelationshipEntity.");
    }
}
