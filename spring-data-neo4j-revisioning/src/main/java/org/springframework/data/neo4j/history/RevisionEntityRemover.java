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
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.core.TypeRepresentationStrategy;
import org.springframework.data.neo4j.support.mapping.EntityRemover;
import org.springframework.data.neo4j.support.mapping.EntityStateHandler;
import org.springframework.data.neo4j.versioning.RevisionManager;

public class RevisionEntityRemover extends EntityRemover {

    private EntityStateHandler entityStateHandler;
    private TypeRepresentationStrategy<Node> nodeTypeRepresentationStrategy;
    private TypeRepresentationStrategy<Relationship> relationshipTypeRepresentationStrategy;
    private final GraphDatabase graphDatabase;

    public RevisionEntityRemover(EntityStateHandler entityStateHandler, TypeRepresentationStrategy<Node> nodeTypeRepresentationStrategy, TypeRepresentationStrategy<Relationship> relationshipTypeRepresentationStrategy, GraphDatabase graphDatabase) {
        super(entityStateHandler, nodeTypeRepresentationStrategy, relationshipTypeRepresentationStrategy, graphDatabase);
        this.entityStateHandler = entityStateHandler;
        this.nodeTypeRepresentationStrategy = nodeTypeRepresentationStrategy;
        this.relationshipTypeRepresentationStrategy = relationshipTypeRepresentationStrategy;
        this.graphDatabase = graphDatabase;
    }

    public void removeNodeEntity(Object entity) {
        Node node = entityStateHandler.getPersistentState(entity, Node.class);
        if (node == null) return;
        removeNode(node);
    }

    private void removeNode(Node node) {
        nodeTypeRepresentationStrategy.preEntityRemoval(node);
        for (Relationship relationship : node.getRelationships()) {
            removeRelationship(relationship);
        }
        if (isVersioned(node)) {
            setDeletedIfValid(node);
        } else {
            graphDatabase.remove(node);
        }
    }

    public void removeRelationshipEntity(Object entity) {
        Relationship relationship = entityStateHandler.getPersistentState(entity, Relationship.class);
        if (relationship == null) return;
        removeRelationship(relationship);
    }

    private void removeRelationship(Relationship relationship) {
        relationshipTypeRepresentationStrategy.preEntityRemoval(relationship);
        if (isVersioned(relationship)) {
            setDeletedIfValid(relationship);
        } else {
            graphDatabase.remove(relationship);
        }
    }

    public void removeRelationshipBetween(Object start, Object target, String type) {
        removeRelationship(entityStateHandler.getRelationshipBetween(start, target, type));
    }

    public void remove(Object entity) {
        if (entity instanceof Node) {
            removeNode((Node)entity);
            return;
        }
        if (entity instanceof Relationship) {
            removeRelationship((Relationship)entity);
            return;
        }
        final Class<?> type = entity.getClass();
        if (entityStateHandler.isNodeEntity(type)) {
            removeNodeEntity(entity);
            return;
        }
        if (entityStateHandler.isRelationshipEntity(type)) {
            removeRelationshipEntity(entity);
            return;
        }
        throw new IllegalArgumentException("@NodeEntity or @RelationshipEntity annotation required on domain class"+type);
    }

    private boolean isVersioned(PropertyContainer container) {
        try {
            return RevisionManager.isVersioned(container) && RevisionManager.getRevisionRange(container) != null;
        } catch (NotFoundException e) {
            return false;
        }
    }

    private void setDeletedIfValid(PropertyContainer container) {
        if (!RevisionManager.isDeleted(container)) {
            if (RevisionManager.hasValidRevision(container)) {
                RevisionManager.setDeleted(container);
            } else {
                throw new IllegalArgumentException("Deleting the historic or deleted state of an @NodeEntity or @RelationshipEntity is not possible");
            }
        }
    }
}
