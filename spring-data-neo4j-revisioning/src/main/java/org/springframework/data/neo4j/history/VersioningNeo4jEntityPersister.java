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
import org.springframework.core.convert.ConversionService;
import org.springframework.data.neo4j.mapping.MappingPolicy;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.mapping.EntityStateHandler;
import org.springframework.data.neo4j.support.mapping.EntityTools;
import org.springframework.data.neo4j.support.mapping.Neo4jEntityPersister;
import org.springframework.data.neo4j.support.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.versioning.RevisionManager;

public class VersioningNeo4jEntityPersister extends Neo4jEntityPersister {

    public VersioningNeo4jEntityPersister(ConversionService conversionService, EntityTools<Node> nodeEntityTools, EntityTools<Relationship> relationshipEntityTools, Neo4jMappingContext mappingContext, EntityStateHandler entityStateHandler) {
        super(conversionService, nodeEntityTools, relationshipEntityTools, mappingContext, entityStateHandler);
    }

    @Override
    public Object persist(Object entity, MappingPolicy mappingPolicy, Neo4jTemplate template, org.neo4j.graphdb.RelationshipType annotationProvidedRelationshipType) {
        final PropertyContainer pc = getPersistentState(entity);
        if (entity.getClass().isAnnotationPresent(Revisioned.class)) {
            checkDeletedState(pc, entity.getClass());
        }
        return super.persist(entity, mappingPolicy, template, annotationProvidedRelationshipType);
    }

    @Override
    public <S extends PropertyContainer, T> T createEntityFromState(S state, Class<T> type, MappingPolicy mappingPolicy, Neo4jTemplate template) {
        setVersionedProperty(state, type);
        return super.createEntityFromState(state, type, mappingPolicy, template);
    }

    private void checkDeletedState(PropertyContainer container, Class<?> type) {
        if (container != null && type.isAnnotationPresent(Revisioned.class)) {
            if (RevisionManager.isDeleted(container) || !RevisionManager.hasValidRevision(container)) {
                throw new IllegalArgumentException("Changing the historic state of an entity is not allowed");
            }
        }
    }

    private void setVersionedProperty(PropertyContainer container, Class<?> type) {
        if (container != null && type.isAnnotationPresent(Revisioned.class) && !RevisionManager.isVersioned(container)) {
            RevisionManager.setRevisioned(container);
        }
    }
}