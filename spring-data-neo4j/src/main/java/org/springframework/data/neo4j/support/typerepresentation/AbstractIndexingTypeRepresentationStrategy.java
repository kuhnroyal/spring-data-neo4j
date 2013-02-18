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
package org.springframework.data.neo4j.support.typerepresentation;

import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.helpers.collection.ClosableIterable;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.core.TypeRepresentationStrategy;
import org.springframework.data.neo4j.support.index.ClosableIndexHits;
import org.springframework.data.neo4j.support.index.IndexProvider;
import org.springframework.data.neo4j.support.index.IndexType;
import org.springframework.data.neo4j.support.mapping.StoredEntityType;

import java.lang.Object;

public abstract class AbstractIndexingTypeRepresentationStrategy<S extends PropertyContainer> implements
        TypeRepresentationStrategy<S> {

    public static final String TYPE_PROPERTY_NAME = "__type__";
    public static final String INDEX_KEY = "className";
    protected String INDEX_NAME;
    protected final GraphDatabase graphDb;
    protected final IndexProvider indexProvider;
    private final Class<? extends PropertyContainer> clazz;
    private Index<S> typesIndex;

    public AbstractIndexingTypeRepresentationStrategy(GraphDatabase graphDb, IndexProvider indexProvider,
                                                      final String indexName, final Class<? extends PropertyContainer> clazz) {
        this.graphDb = graphDb;
        this.indexProvider = indexProvider;
        INDEX_NAME = indexName;
        this.clazz = clazz;
        typesIndex = createTypesIndex();
    }

    @SuppressWarnings("unchecked")
    private Index<S> createTypesIndex() {
        return (Index<S>) graphDb.createIndex(clazz, INDEX_NAME, IndexType.SIMPLE);
    }

    @Override
    public void writeTypeTo(S state, StoredEntityType type) {
        if (type.getAlias().equals(state.getProperty(TYPE_PROPERTY_NAME, null))) return; // already there
        addToTypesIndex(state, type);
        state.setProperty(TYPE_PROPERTY_NAME, type.getAlias());
    }

    @Override
    public long count(StoredEntityType type) {
        long count = 0;
        Object value = type.getAlias();
        final IndexHits<S> hits = get(value);
        while (hits.hasNext()) {
            hits.next();
            count++;
        }
        return count;
    }

    private IndexHits<S> get(Object value) {
        try {
            return typesIndex.get(INDEX_KEY, value);
        } catch(IllegalStateException ise) {
            typesIndex=createTypesIndex();
            return typesIndex.get(INDEX_KEY, value);
        }
    }

    @Override
    public void preEntityRemoval(S state) {
        remove(state);
    }

    private void remove(S state) {
        try {
            typesIndex.remove(state);
        } catch(IllegalStateException ise) {
            typesIndex=createTypesIndex();
            typesIndex.remove(state);
        }
    }

    @Override
    public <U> ClosableIterable<S> findAll(StoredEntityType type) {
        return findAllRelBacked(type);
    }

    @Override
    public Object readAliasFrom(S propertyContainer) {
        if (propertyContainer == null)
            throw new IllegalArgumentException("Relationship or Node is null");
        return propertyContainer.getProperty(TYPE_PROPERTY_NAME);
    }

    protected void addToTypesIndex(S element, StoredEntityType type) {
        if (type == null) return;
        Object value = type.getAlias();
        if (indexProvider != null) {
            value = indexProvider.createIndexValueForType(type.getAlias());
        }
        add(element, value);
        for (StoredEntityType superType : type.getSuperTypes()) {
            addToTypesIndex(element,superType);
        }
    }

    private void add(S element, Object value) {
        try {
            typesIndex.add(element, INDEX_KEY, value);
        } catch(IllegalStateException ise) {
            typesIndex = createTypesIndex();
            typesIndex.add(element, INDEX_KEY, value);
        }
    }

    @SuppressWarnings("hiding")
    private ClosableIterable<S> findAllRelBacked(StoredEntityType type) {
        Object value = type.getAlias();
        if (indexProvider != null)
            value = indexProvider.createIndexValueForType(type.getAlias());

        final IndexHits<S> allEntitiesOfType = get(value);
        return new ClosableIndexHits<S>(allEntitiesOfType);
    }

}
