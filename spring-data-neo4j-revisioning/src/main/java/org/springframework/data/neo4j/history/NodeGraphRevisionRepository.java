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

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.kernel.Traversal;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.history.Revision;
import org.springframework.data.history.Revisions;
import org.springframework.data.neo4j.conversion.EndResult;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.neo4j.repository.NodeGraphRepositoryImpl;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.versioning.RevisionManager;
import org.springframework.data.neo4j.versioning.VersionedNeo4jTemplate;
import org.springframework.data.neo4j.versioning.VersionedTraversalDescription;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Peter Leibiger
 * @since 17.09.12
 */
public class NodeGraphRevisionRepository<T>
        extends NodeGraphRepositoryImpl<T> implements GraphRevisionRepository<T> {

    private final GraphRepository<Rev> revRepository;

    public NodeGraphRevisionRepository(final Class<T> clazz, final Neo4jTemplate template) {
        super(clazz, template);
        revRepository = template.repositoryFor(Rev.class);
    }

    public VersionedNeo4jTemplate getTemplate() {
        return (VersionedNeo4jTemplate) super.template;
    }

    public Node getById(long id, long revisionNumber) {
        return getTemplate().getNode(id, revisionNumber);
    }

    @Override
    public long count(long revisionNumber) {
        return getTemplate().count(clazz, revisionNumber);
    }

    @Override
    public EndResult<T> findAll(long revisionNumber) {
        return getTemplate().findAll(clazz, revisionNumber);
    }

    @Override
    public T findOne(Long id) {
        try {
            return getTemplate().findOne(id, clazz);
        } catch (DataRetrievalFailureException e) {
            return null;
        }
    }

    @Override
    public T findOne(long id, long revisionNumber) {
        try {
            return getTemplate().findOne(id, clazz, revisionNumber);
        } catch (DataRetrievalFailureException e) {
            return null;
        }
    }

    @Override
    public boolean exists(long id, long revisionNumber) {
        try {
            return getById(id, revisionNumber) != null;
        } catch (DataRetrievalFailureException e) {
            return false;
        }
    }

    @Override
    public EndResult<T> findAll(Sort sort, long revisionNumber) {
        return findAll(revisionNumber);
    }

    @Override
    public Page<T> findAll(Pageable pageable, long revisionNumber) {
        int count = pageable.getPageSize();
        int offset = pageable.getOffset();
        EndResult<T> foundEntities = findAll(pageable.getSort(), revisionNumber);
        final Iterator<T> iterator = foundEntities.iterator();
        final PageImpl<T> page = extractPage(pageable, count, offset, iterator);
        foundEntities.finish();
        return page;
    }

    @Override
    public <N> Iterable<T> findAllByTraversal(N startNode, VersionedTraversalDescription traversalDescription) {
        return getTemplate().traverse(startNode, clazz, traversalDescription);
    }

    @Override
    public Revision<Long, T> findLastChangeRevision(Long id) {
        final Node node = template.getNode(id);
        if (RevisionManager.isVersioned(node)) {
            return buildRevision(node);
        }
        throw new RuntimeException("Node is not revisioned");
    }

    @Override
    public Revisions<Long, T> findRevisions(Long id) {
        final Node node = template.getNode(id);
        if (RevisionManager.isVersioned(node)) {
            return new Revisions<Long, T>(findAllRevisions(node));
        }
        throw new RuntimeException("Node is not revisioned");
    }

    @Override
    public Page<Revision<Long, T>> findRevisions(Long id, Pageable pageable) {
        final Node node = template.getNode(id);
        if (RevisionManager.isVersioned(node)) {
            final List<Path> paths = new ArrayList<Path>();
            final Iterator<Path> it = template.traverse(node, Traversal.description().relationships(RevisionManager.PREV_REV_REL_TYPE, Direction.OUTGOING)).iterator();
            int total = subPath(pageable.getOffset(), pageable.getPageSize(), it, paths);
            if (it.hasNext()) {
                total++;
            }
            final List<Revision<Long, T>> revisions = new ArrayList<Revision<Long, T>>();
            final Result<Path> path = template.traverse(node, Traversal.description().relationships(RevisionManager.PREV_REV_REL_TYPE, Direction.OUTGOING));
            for (Path p : path) {
                revisions.add(buildRevision(p.endNode()));
            }
            return new PageImpl<Revision<Long, T>>(revisions, pageable, total);
        }
        throw new RuntimeException("Node is not revisioned");
    }

    private List<Revision<Long, T>> findAllRevisions(Node node) {
        final List<Revision<Long, T>> revisions = new ArrayList<Revision<Long, T>>();
        final Result<Path> path = template.traverse(node, Traversal.description().relationships(RevisionManager.PREV_REV_REL_TYPE, Direction.OUTGOING));
        for (Path p : path) {
            revisions.add(buildRevision(p.endNode()));
        }
        return revisions;
    }

    private Revision<Long, T> buildRevision(Node node) {
        final Long number = RevisionManager.getRevisionNumber(node);
        final EndResult<Rev> result = revRepository.findAllByPropertyValue("number", number);
        final Rev rev = result.single();
        return new Revision<Long, T>(new GraphRevisionMetadata(rev), template.projectTo(node, clazz));
    }

    private int subPath(int skip, int limit, Iterator<Path> source, final List<Path> list) {
        int count = 0;
        while (source.hasNext()) {
            count++;
            Path p = source.next();
            if (skip > 0) {
                skip--;
            } else {
                list.add(p);
                limit--;
            }
            if (limit + skip == 0) {
                break;
            }
        }
        return count;
    }

    private PageImpl<T> extractPage(Pageable pageable, int count, int offset, Iterator<T> iterator) {
        final List<T> result = new ArrayList<T>(count);
        int total = subList(offset, count, iterator, result);
        if (iterator.hasNext()) total++;
        return new PageImpl<T>(result, pageable, total);
    }

    private int subList(int skip, int limit, Iterator<T> source, final List<T> list) {
        int count = 0;
        while (source.hasNext()) {
            count++;
            T t = source.next();
            if (skip > 0) {
                skip--;
            } else {
                list.add(t);
                limit--;
            }
            if (limit + skip == 0) break;
        }
        return count;
    }
}
