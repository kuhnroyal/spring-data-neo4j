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
import org.neo4j.graphdb.traversal.*;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.helpers.Predicate;
import org.neo4j.kernel.StandardExpander;
import org.neo4j.kernel.impl.traversal.TraversalDescriptionImpl;

import java.util.Comparator;

public class VersionedTraversalDescriptionImpl implements VersionedTraversalDescription {

    private final TraversalDescription delegate;

    public VersionedTraversalDescriptionImpl(TraversalDescription delegate, final long revision) {
        final RelationshipExpander expander = StandardExpander.DEFAULT.addRelationshipFilter(new Predicate<Relationship>() {
            @Override
            public boolean accept(Relationship relationship) {
                if (!RevisionManager.ANY.equals(revision)) {
                    // check for versioned relationships and exclude if it has no valid version
                    final Range range = RevisionManager.getRevisionRange(relationship);
                    if (range != null && !range.contains(revision)) {
                        return false;
                    }
                }
                return true;
            }
        });
        this.delegate = delegate.expand(expander);
    }

    public VersionedTraversalDescriptionImpl(TraversalDescription delegate) {
        this(delegate, RevisionManager.LATEST);
    }

    public VersionedTraversalDescriptionImpl(long revision) {
        this(new TraversalDescriptionImpl(), revision);
    }

    public VersionedTraversalDescriptionImpl() {
        this(RevisionManager.LATEST);
    }

    @Override
    public TraversalDescription uniqueness(UniquenessFactory uniquenessFactory) {
        return delegate.uniqueness(uniquenessFactory);
    }

    @Override
    public TraversalDescription uniqueness(UniquenessFactory uniquenessFactory, Object o) {
        return delegate.uniqueness(uniquenessFactory, o);
    }

    @Override
    public TraversalDescription evaluator(Evaluator evaluator) {
        return delegate.evaluator(evaluator);
    }

    @Override
    public TraversalDescription evaluator(PathEvaluator pathEvaluator) {
        return delegate.evaluator(pathEvaluator);
    }

    @Override
    public TraversalDescription order(BranchOrderingPolicy branchOrderingPolicy) {
        return delegate.order(branchOrderingPolicy);
    }

    @Override
    public TraversalDescription depthFirst() {
        return delegate.depthFirst();
    }

    @Override
    public TraversalDescription breadthFirst() {
        return delegate.breadthFirst();
    }

    @Override
    public TraversalDescription relationships(RelationshipType relationshipType) {
        return delegate.relationships(relationshipType);
    }

    @Override
    public TraversalDescription relationships(RelationshipType relationshipType, Direction direction) {
        return delegate.relationships(relationshipType, direction);
    }

    @Override
    public TraversalDescription expand(PathExpander<?> pathExpander) {
        return delegate.expand(pathExpander);
    }

    @Override
    public <STATE> TraversalDescription expand(PathExpander<STATE> statePathExpander, InitialStateFactory<STATE> stateInitialStateFactory) {
        return delegate.expand(statePathExpander, stateInitialStateFactory);
    }

    @Override
    public <STATE> TraversalDescription expand(PathExpander<STATE> statePathExpander, InitialBranchState<STATE> stateInitialBranchState) {
        return delegate.expand(statePathExpander, stateInitialBranchState);
    }

    @Override
    public TraversalDescription expand(RelationshipExpander relationshipExpander) {
        return delegate.expand(relationshipExpander);
    }

    @Override
    public TraversalDescription sort(Comparator<? super Path> comparator) {
        return delegate.sort(comparator);
    }

    @Override
    public TraversalDescription reverse() {
        return delegate.reverse();
    }

    @Override
    public Traverser traverse(Node node) {
        return delegate.traverse(node);
    }

    @Override
    public Traverser traverse(Node... nodes) {
        return delegate.traverse(nodes);
    }
}
