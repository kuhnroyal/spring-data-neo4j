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
import org.springframework.data.neo4j.repository.GraphMetamodelEntityInformation;
import org.springframework.data.neo4j.support.Neo4jTemplate;

/**
 * @author Peter Leibiger
 * @since 17.09.12
 */
public class GraphRevisionMetamodelEntityInformation<S extends PropertyContainer, T> extends GraphMetamodelEntityInformation<S, T>
        implements GraphRevisionEntityInformation<S, T> {

    private final boolean revisioned;

    public GraphRevisionMetamodelEntityInformation(Class domainClass, Neo4jTemplate template) {
        super(domainClass, template);
        revisioned = getJavaType().getAnnotation(Revisioned.class) != null;
    }

    @Override
    public boolean isRevisionedEntity() {
        return revisioned;
    }
}