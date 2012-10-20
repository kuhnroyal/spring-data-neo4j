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
import org.neo4j.graphdb.Relationship;
import org.springframework.data.neo4j.mapping.EntityInstantiator;
import org.springframework.data.neo4j.support.Infrastructure;
import org.springframework.data.neo4j.support.MappingInfrastructure;
import org.springframework.data.neo4j.support.MappingInfrastructureFactoryBean;
import org.springframework.data.neo4j.support.mapping.EntityRemover;
import org.springframework.data.neo4j.support.mapping.EntityTools;
import org.springframework.data.neo4j.support.mapping.Neo4jEntityPersister;
import org.springframework.data.neo4j.support.node.EntityStateFactory;

public class RevisionMappingInfrastructureBean extends MappingInfrastructureFactoryBean {

    private RevisionTypeRepresentationStrategies strategies;

    private MappingInfrastructure mappingInfrastructure;

    private RevisionEntityRemover entityRemover;

    private VersioningNeo4jEntityPersister entityPersister;

    private EntityInstantiator<Relationship> relationshipEntityInstantiator;

    private EntityInstantiator<Node> nodeEntityInstantiator;

    private EntityStateFactory<Relationship> relationshipEntityStateFactory;

    private EntityStateFactory<Node> nodeEntityStateFactory;

    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        strategies = new RevisionTypeRepresentationStrategies(getMappingContext(), getNodeTypeRepresentationStrategy(), getRelationshipTypeRepresentationStrategy());
        entityRemover = new RevisionEntityRemover(getEntityStateHandler(), getNodeTypeRepresentationStrategy(), getRelationshipTypeRepresentationStrategy(), getGraphDatabase());
        EntityTools<Node> nodeEntityTools = new EntityTools<Node>(getNodeTypeRepresentationStrategy(), nodeEntityStateFactory, nodeEntityInstantiator, getMappingContext());
        EntityTools<Relationship> relationshipEntityTools = new EntityTools<Relationship>(getRelationshipTypeRepresentationStrategy(), relationshipEntityStateFactory, relationshipEntityInstantiator, getMappingContext());
        entityPersister = new VersioningNeo4jEntityPersister(getConversionService(), nodeEntityTools, relationshipEntityTools, getMappingContext(), getEntityStateHandler());
        mappingInfrastructure = new MappingInfrastructure(getGraphDatabase(), getGraphDatabaseService(), getIndexProvider(),
                getResultConverter(), getTransactionManager(), getTypeRepresentationStrategies(), getEntityRemover(),
                getEntityPersister(), getEntityStateHandler(), getCypherQueryExecutor(), getMappingContext(),
                getRelationshipTypeRepresentationStrategy(), getNodeTypeRepresentationStrategy(), getValidator(), getConversionService());
    }

    @Override
    public void setRelationshipEntityInstantiator(EntityInstantiator<Relationship> relationshipEntityInstantiator) {
        super.setRelationshipEntityInstantiator(relationshipEntityInstantiator);
        this.relationshipEntityInstantiator = relationshipEntityInstantiator;
    }

    @Override
    public void setNodeEntityInstantiator(EntityInstantiator<Node> nodeEntityInstantiator) {
        super.setNodeEntityInstantiator(nodeEntityInstantiator);
        this.nodeEntityInstantiator = nodeEntityInstantiator;
    }

    @Override
    public void setRelationshipEntityStateFactory(EntityStateFactory<Relationship> relationshipEntityStateFactory) {
        super.setRelationshipEntityStateFactory(relationshipEntityStateFactory);
        this.relationshipEntityStateFactory = relationshipEntityStateFactory;
    }

    @Override
    public void setNodeEntityStateFactory(EntityStateFactory<Node> nodeEntityStateFactory) {
        super.setNodeEntityStateFactory(nodeEntityStateFactory);
        this.nodeEntityStateFactory = nodeEntityStateFactory;
    }

    @Override
    public RevisionTypeRepresentationStrategies getTypeRepresentationStrategies() {
        return strategies;
    }

    @Override
    public EntityRemover getEntityRemover() {
        return entityRemover;
    }

    @Override
    public Neo4jEntityPersister getEntityPersister() {
        return entityPersister;
    }

    @Override
    public Infrastructure getObject() {
        return mappingInfrastructure;
    }
}
