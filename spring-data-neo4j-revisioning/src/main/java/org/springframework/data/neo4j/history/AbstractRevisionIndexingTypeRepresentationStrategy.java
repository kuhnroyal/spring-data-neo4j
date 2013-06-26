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

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.TermQuery;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.helpers.collection.ClosableIterable;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.support.index.ClosableIndexHits;
import org.springframework.data.neo4j.support.index.IndexProvider;
import org.springframework.data.neo4j.support.index.IndexType;
import org.springframework.data.neo4j.support.mapping.StoredEntityType;
import org.springframework.data.neo4j.versioning.RevisionManager;

public abstract class AbstractRevisionIndexingTypeRepresentationStrategy<S extends PropertyContainer> implements VersionedTypeRepresentationStrategy<S> {

    public static final String TYPE_PROPERTY_NAME = "__type__";
    public static final String INDEX_KEY = "className";
    protected String INDEX_NAME;
    protected final GraphDatabase graphDb;
    protected final IndexProvider indexProvider;
    private final Class<? extends PropertyContainer> clazz;
    private Index<S> typesIndex;

    public AbstractRevisionIndexingTypeRepresentationStrategy(GraphDatabase graphDb, IndexProvider indexProvider,
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
    public void preEntityRemoval(S state) {
        remove(state);
    }

    private void remove(S state) {
        try {
            typesIndex.remove(state);
        } catch (IllegalStateException ise) {
            typesIndex = createTypesIndex();
            typesIndex.remove(state);
        }
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
            addToTypesIndex(element, superType);
        }
    }

    private void add(S element, Object value) {
        try {
            typesIndex.add(element, INDEX_KEY, value);
        } catch (IllegalStateException ise) {
            typesIndex = createTypesIndex();
            typesIndex.add(element, INDEX_KEY, value);
        }
    }

    @Override
    public <U> ClosableIterable<S> findAll(StoredEntityType type) {
        return findAll(type, RevisionManager.LATEST);
    }

    @Override
    public <U> ClosableIterable<S> findAll(StoredEntityType type, long revisionNumber) {
        return new ClosableIndexHits<S>(revisionQuery(type, revisionNumber));
    }

    @Override
    public long count(StoredEntityType type) {
        return count(type, RevisionManager.LATEST);
    }

    @Override
    public long count(StoredEntityType type, long revisionNumber) {
        return count(revisionQuery(type, revisionNumber));
    }

    private long count(IndexHits<S> hits) {
        int count = 0;
        while (hits.hasNext()) {
            hits.next();
            count++;
        }
        return count;
    }

    private IndexHits<S> revisionQuery(StoredEntityType type, long revisionNumber) {
        String value = type.getAlias().toString();
        if (indexProvider != null) {
            value = indexProvider.createIndexValueForType(type.getAlias());
        }
        if (type.getType().isAnnotationPresent(Revisioned.class)) {
            final BooleanQuery query = new BooleanQuery();
            query.add(new TermQuery(new Term(INDEX_KEY, value)), BooleanClause.Occur.MUST);
            if (!RevisionManager.ANY.equals(revisionNumber)) {
                query.add(NumericRangeQuery.newLongRange(RevisionManager.INDEX_VALID_FROM, 0l, revisionNumber, true, true), BooleanClause.Occur.MUST);
                query.add(NumericRangeQuery.newLongRange(RevisionManager.INDEX_VALID_TO, revisionNumber, RevisionManager.LATEST, true, true), BooleanClause.Occur.MUST);
            }
            return typesIndex.query(query);
        } else {
            return typesIndex.get(INDEX_KEY, value);
        }
    }
}