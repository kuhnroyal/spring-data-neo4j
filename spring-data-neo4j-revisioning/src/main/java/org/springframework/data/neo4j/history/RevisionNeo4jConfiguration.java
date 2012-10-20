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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.support.MappingInfrastructureFactoryBean;
import org.springframework.data.neo4j.support.typerepresentation.TypeRepresentationStrategyFactory;
import org.springframework.data.neo4j.versioning.*;

import javax.validation.Validator;

@Configuration
@EnableNeo4jRepositories
public class RevisionNeo4jConfiguration extends Neo4jConfiguration {

    @Autowired(required = false)
    private Validator validator;

    @Bean
    public VersionedNeo4jTemplate neo4jTemplate() throws Exception {
        return new VersionedNeo4jTemplate(mappingInfrastructure().getObject());
    }

    @Bean
    @Override
    public MappingInfrastructureFactoryBean mappingInfrastructure() throws Exception {
        MappingInfrastructureFactoryBean factoryBean = new RevisionMappingInfrastructureBean();
        factoryBean.setGraphDatabaseService(getGraphDatabaseService());
        factoryBean.setTypeRepresentationStrategyFactory(typeRepresentationStrategyFactory());
        factoryBean.setConversionService(neo4jConversionService());
        factoryBean.setMappingContext(neo4jMappingContext());
        factoryBean.setEntityStateHandler(entityStateHandler());

        factoryBean.setNodeEntityStateFactory(nodeEntityStateFactory());
        factoryBean.setNodeTypeRepresentationStrategy(nodeTypeRepresentationStrategy());
        factoryBean.setNodeEntityInstantiator(graphEntityInstantiator());

        factoryBean.setRelationshipEntityStateFactory(relationshipEntityStateFactory());
        factoryBean.setRelationshipTypeRepresentationStrategy(relationshipTypeRepresentationStrategy());
        factoryBean.setRelationshipEntityInstantiator(graphRelationshipInstantiator());

        factoryBean.setTransactionManager(neo4jTransactionManager());
        factoryBean.setGraphDatabase(graphDatabase());

        factoryBean.setIndexProvider(indexProvider());

        if (validator != null) {
            factoryBean.setValidator(validator);
        }
        return factoryBean;
    }

    @Bean
    public RevisionProvider<? extends Rev> revisionProvider() throws Exception {
        return new RevisionProviderImpl();
    }

    @Bean
    public RevisionTransactionEventHandler versioningTransactionHandler() throws Exception {
        final RevisionTransactionEventHandler handler = new RevisionTransactionEventHandler(revisionManager(), indexProvider());
        getGraphDatabaseService().registerTransactionEventHandler(handler);
        return handler;
    }

    @Bean
    public RevisionManager revisionManager() throws Exception {
        return new RevisionManager(revisionProvider(), neo4jTransactionManager(), neo4jTemplate());
    }

    @Bean
    public TypeRepresentationStrategyFactory typeRepresentationStrategyFactory() throws Exception {
        return new RevisionTypeRepresentationStrategyFactory(graphDatabase(), indexProvider());
    }
}
