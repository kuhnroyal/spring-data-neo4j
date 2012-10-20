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
package org.springframework.data.neo4j.versioning;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.helpers.collection.ClosableIterable;
import org.springframework.data.neo4j.conversion.EndResult;
import org.springframework.data.neo4j.conversion.QueryResultBuilder;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.history.RevisionTypeRepresentationStrategies;
import org.springframework.data.neo4j.support.Infrastructure;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.mapping.Neo4jPersistentEntityImpl;
import org.springframework.transaction.PlatformTransactionManager;

import static org.springframework.data.neo4j.support.ParameterCheck.notNull;

public class VersionedNeo4jTemplate extends Neo4jTemplate {

    public VersionedNeo4jTemplate(final GraphDatabase graphDatabase, PlatformTransactionManager transactionManager) {
        super(graphDatabase, transactionManager);
    }

    public VersionedNeo4jTemplate(final GraphDatabase graphDatabase) {
        super(graphDatabase);
    }

    public VersionedNeo4jTemplate(final GraphDatabaseService graphDatabaseService) {
        super(graphDatabaseService);
    }

    public VersionedNeo4jTemplate(Infrastructure infrastructure) {
        super(infrastructure);
    }

    @Override
    public <T> T findOne(long id, final Class<T> entityClass) {
        return findOne(id, entityClass, RevisionManager.LATEST);
    }

    public <T> T findOne(long id, final Class<T> entityClass, final Long revision) {
        final Neo4jPersistentEntityImpl<?> persistentEntity = getInfrastructure().getMappingContext().getPersistentEntity(entityClass);
        if (persistentEntity.isNodeEntity()) {
            final Node node = getNode(id, revision);
            if (node == null) {
                return null;
            }
            return createEntityFromState(node, entityClass, persistentEntity.getMappingPolicy());
        } else if (persistentEntity.isRelationshipEntity()) {
            final Relationship relationship = getRelationship(id, revision);
            if (relationship == null) {
                return null;
            }
            return createEntityFromState(relationship, entityClass, persistentEntity.getMappingPolicy());
        }
        throw new IllegalArgumentException("provided entity type is not annotated with @NodeEntiy nor @RelationshipEntity");
    }

    @Override
    public <T> EndResult<T> findAll(Class<T> entityClass) {
        return findAll(entityClass, RevisionManager.LATEST);
    }

    public <T> EndResult<T> findAll(Class<T> entityClass, final Long revision) {
        notNull(entityClass, "entity type");
        final ClosableIterable<PropertyContainer> all = ((RevisionTypeRepresentationStrategies) getInfrastructure().getTypeRepresentationStrategies()).findAll(getEntityType(entityClass), revision);
        return new QueryResultBuilder<PropertyContainer>(all, getDefaultConverter()).to(entityClass);
    }

    @Override
    public <T> long count(Class<T> entityClass) {
        return count(entityClass, RevisionManager.LATEST);
    }

    public <T> long count(Class<T> entityClass, final Long revision) {
        notNull(entityClass, "entity type");
        return ((RevisionTypeRepresentationStrategies) getInfrastructure().getTypeRepresentationStrategies()).count(getEntityType(entityClass), revision);
    }

    // TODO return VersionedNode?
    @Override
    public Node getNode(long id) {
        return getNode(id, RevisionManager.LATEST);
    }

    // TODO return VersionedNode?
    public Node getNode(long id, final Long  revision) {
        return getResult(revision, super.getNode(id));
    }

    // TODO return VersionedRelationship?
    @Override
    public Relationship getRelationship(long id) {
        return getRelationship(id, RevisionManager.LATEST);
    }

    // TODO return VersionedRelationship
    public Relationship getRelationship(long id, final Long  revision) {
        return getResult(revision, super.getRelationship(id));
    }

    private <T extends PropertyContainer> T getResult(final Long revision, T pc) {
        if (RevisionManager.ANY.equals(revision) || RevisionManager.getRevisionRange(pc) == null || RevisionManager.hasValidRevision(pc, revision)) {
            return pc;
        }
        return null;
    }

    @Override
    public Result<Path> traverse(Node startNode, TraversalDescription traversal) {
        // TODO do something?
        return super.traverse(startNode, traversal);
    }

    public Result<Path> traverse(Node startNode, VersionedTraversalDescription traversal) {
        return super.traverse(startNode, traversal);
    }

    @Override
    public <T> Iterable<T> traverse(Object entity, Class<?> targetType, TraversalDescription traversalDescription) {
        // TODO do something?
        return super.traverse(entity, targetType, traversalDescription);
    }

    public <T> Iterable<T> traverse(Object entity, Class<?> targetType, VersionedTraversalDescription traversalDescription) {
        return super.traverse(entity, targetType, traversalDescription);
    }

    @Override
    public Result<Path> traverse(Object start, TraversalDescription traversal) {
        // TODO do something?
        return super.traverse(start, traversal);
    }

    public Result<Path> traverse(Object start, VersionedTraversalDescription traversal) {
        return super.traverse(start, traversal);
    }

    @Override
    public VersionedTraversalDescription traversalDescription() {
        return new VersionedTraversalDescriptionImpl();
    }

    public VersionedTraversalDescription traversalDescription(final Long  revision) {
        return new VersionedTraversalDescriptionImpl(revision);
    }

    public VersionedTraversalDescription traversalDescription(final TraversalDescription delegate) {
        return new VersionedTraversalDescriptionImpl(delegate);
    }

    public VersionedTraversalDescription traversalDescription(final TraversalDescription delegate, final long revision) {
        return new VersionedTraversalDescriptionImpl(delegate, revision);
    }
}
