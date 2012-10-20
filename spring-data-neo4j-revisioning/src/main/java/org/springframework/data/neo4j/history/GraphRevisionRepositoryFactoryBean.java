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

import org.neo4j.graphdb.PropertyContainer;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.data.neo4j.repository.GraphRepositoryFactoryBean;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.mapping.Neo4jMappingContext;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.history.RevisionRepository;

import java.io.Serializable;

/**
 * {@link FactoryBean} creating {@link RevisionRepository} instances.
 *
 * @author Peter Leibiger
 * @since 17.09.12
 */
public class GraphRevisionRepositoryFactoryBean<S extends PropertyContainer> extends
        GraphRepositoryFactoryBean<S, NodeGraphRevisionRepository<Object>, Object> {

    private Neo4jMappingContext mappingContext;

    @Override
    public void setNeo4jMappingContext(Neo4jMappingContext mappingContext) {
        this.mappingContext = mappingContext;
        super.setNeo4jMappingContext(mappingContext);
    }

    @Override
    protected RepositoryFactorySupport createRepositoryFactory(Neo4jTemplate template) {
        return new GraphRevisionRepositoryFactory(template, mappingContext);
    }
}
