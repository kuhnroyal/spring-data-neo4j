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

import org.springframework.data.neo4j.repository.GraphRepositoryFactory;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.mapping.Neo4jMappingContext;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryMetadata;

import java.io.Serializable;

/**
 * @author Peter Leibiger
 * @since 17.09.12
 */
public class GraphRevisionRepositoryFactory extends GraphRepositoryFactory {

    private final Neo4jTemplate template;

    /**
     * Creates a new {@link org.springframework.data.neo4j.repository.GraphRepositoryFactory} from the given {@link org.springframework.data.neo4j.support.Neo4jTemplate} and
     * {@link org.springframework.data.mapping.context.MappingContext}.
     *
     * @param template       must not be {@literal null}.
     * @param mappingContext must not be {@literal null}.
     */
    public GraphRevisionRepositoryFactory(Neo4jTemplate template, Neo4jMappingContext mappingContext) {
        super(template, mappingContext);
        this.template = template;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected Object getTargetRepository(RepositoryMetadata metadata, Neo4jTemplate template) {
        final Class<?> type = metadata.getDomainType();
        final GraphRevisionEntityInformation entityInformation = (GraphRevisionEntityInformation) getEntityInformation(type);
        // todo entityInformation.isGraphBacked();
        if (entityInformation.isRevisionedEntity()) {
            if (entityInformation.isNodeEntity()) {
                return new NodeGraphRevisionRepository(type, template);
            } else {
                return new RelationshipGraphRevisionRepository(type, template);
            }
        }
        return super.getTargetRepository(metadata, template);
    }

    @Override
    protected Class<?> getRepositoryBaseClass(RepositoryMetadata repositoryMetadata) {
        final Class<?> domainClass = repositoryMetadata.getDomainType();

        @SuppressWarnings("rawtypes")
        final GraphRevisionEntityInformation entityInformation = (GraphRevisionEntityInformation) getEntityInformation(domainClass);
        if (entityInformation.isRevisionedEntity()) {
            if (entityInformation.isNodeEntity()) {
                return NodeGraphRevisionRepository.class;
            }
            if (entityInformation.isRelationshipEntity()) {
                return RelationshipGraphRevisionRepository.class;
            }
        }
        return super.getRepositoryBaseClass(repositoryMetadata);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T, ID extends Serializable> EntityInformation<T, ID> getEntityInformation(Class<T> type) {
        return new GraphRevisionMetamodelEntityInformation(type, template);
    }
}
