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

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.event.PropertyEntry;
import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.graphdb.index.Index;
import org.neo4j.index.lucene.ValueContext;
import org.springframework.data.neo4j.history.IndexingRevisionNodeTypeRepresentationStrategy;
import org.springframework.data.neo4j.history.IndexingRevisionRelationshipTypeRepresentationStrategy;
import org.springframework.data.neo4j.support.index.IndexProvider;
import org.springframework.data.neo4j.support.typerepresentation.IndexingNodeTypeRepresentationStrategy;
import org.springframework.data.neo4j.support.typerepresentation.IndexingRelationshipTypeRepresentationStrategy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RevisionTransactionEventHandler implements TransactionEventHandler<Object> {

    private final RevisionManager revisionManager;

    private final IndexProvider indexProvider;

    public RevisionTransactionEventHandler(RevisionManager revisionManager, IndexProvider indexProvider) {
        this.revisionManager = revisionManager;
        this.indexProvider = indexProvider;
    }

    public Object beforeCommit(final TransactionData data) throws Exception {
          final Set<Node> nodesCreated = processCreatedNodes(data.createdNodes());
          final Set<Relationship> relsCreated = processCreatedRelationships(data.createdRelationships());
          final Set<Relationship> relsDeleted = processMarkedDeletedRelationships(data.assignedRelationshipProperties());
          final Set<Node> nodesDeleted = new HashSet<Node>();
          final Map<Node, Map<String, Object>> properties = findModifiedProperties(nodesCreated, nodesDeleted, data);

          if (!nodesCreated.isEmpty() || !relsCreated.isEmpty() || !relsDeleted.isEmpty() || !nodesDeleted.isEmpty() || !properties.isEmpty()) {
              final long before = revisionManager.getCurrentRevisionNumber();
              final long version = revisionManager.increaseVersion();
              for (final Node node : nodesCreated) {
                  final Range range = Range.range(version);
                  RevisionManager.setRevisionRange(node, range);
                  updateNodeIndex(node, range, null);
              }
              for (final Relationship rel : relsCreated) {
                  final Range range = Range.range(version);
                  RevisionManager.setRevisionRange(rel, range);
                  updateRelationshipIndex(rel, range, null);
              }
              for (final Relationship rel : relsDeleted) {
                  final Range old = RevisionManager.getRevisionRange(rel);
                  RevisionManager.setEndRevision(rel, version - 1);
                  updateRelationshipIndex(rel, RevisionManager.getRevisionRange(rel), old);
              }
              for (final Node node : nodesDeleted) {
                  final Range oldRange = RevisionManager.getRevisionRange(node);
                  RevisionManager.setEndRevision(node, version - 1);
                  final Range newRange = RevisionManager.getRevisionRange(node);
                  updateNodeIndex(node, newRange, oldRange);
              }
              rotateProperties(version, properties);
              final long after = revisionManager.getCurrentRevisionNumber();
              if (before != after - 1) {
                  throw new RuntimeException("Revision changed during transaction");
              }
          }
          return null;
      }

      private Set<Node> processCreatedNodes(final Iterable<Node> createdNodes) {
          final Set<Node> created = new HashSet<Node>();
          for (final Node node : createdNodes) {
              if (RevisionManager.isVersioned(node)) {
                  created.add(node);
              }
          }
          return created;
      }

      private Set<Relationship> processCreatedRelationships(final Iterable<Relationship> createdRelationships) {
          final Set<Relationship> created = new HashSet<Relationship>();
          for (final Relationship relationship : createdRelationships) {
              if (RevisionManager.isVersioned(relationship)) {
                  created.add(relationship);
              }
          }
          return created;
      }

      private Set<Relationship> processMarkedDeletedRelationships(final Iterable<PropertyEntry<Relationship>> relationshipProperties) {
          final Set<Relationship> deleted = new HashSet<Relationship>();
          for (final PropertyEntry<Relationship> relationshipPropertyEntry : relationshipProperties) {
              if (relationshipPropertyEntry.key().equals(RevisionManager.PROPERTY_DELETED)) {
                  deleted.add(relationshipPropertyEntry.entity());
              }
          }
          return deleted;
      }

      private Map<Node, Map<String, Object>> findModifiedProperties(final Set<Node> created, final Set<Node> deleted, final TransactionData data) {
          final Map<Node, Map<String, Object>> modifiedPropsByNode = new HashMap<Node, Map<String, Object>>();
          for (final PropertyEntry<Node> nodePropertyEntry : data.assignedNodeProperties()) {
              final Node node = nodePropertyEntry.entity();
              if (!data.isDeleted(node) && RevisionManager.isVersioned(node) && !created.contains(node)) {
                  if (nodePropertyEntry.key().equals(RevisionManager.PROPERTY_DELETED)) {
                      deleted.add(node);
                      continue;
                  }
                  if (isInternalProperty(nodePropertyEntry)) {
                      continue;
                  }
                  addEntryToMap(nodePropertyEntry, modifiedPropsByNode);
              }
          }
          for (final PropertyEntry<Node> nodePropertyEntry : data.removedNodeProperties()) {
              final Node node = nodePropertyEntry.entity();
              if (!data.isDeleted(node) && RevisionManager.isVersioned(node) && !created.contains(node)) {
                  if (isInternalProperty(nodePropertyEntry)) {
                      continue;
                  }
                  addEntryToMap(nodePropertyEntry, modifiedPropsByNode);
              }
          }
          for (final Node node : deleted) {
              modifiedPropsByNode.remove(node);
          }
          return modifiedPropsByNode;
      }

      private boolean isInternalProperty(final PropertyEntry<Node> nodePropertyEntry) {
          return nodePropertyEntry.key().equals(RevisionManager.PROPERTY_VALID_TO)
                  || nodePropertyEntry.key().equals(RevisionManager.PROPERTY_VALID_FROM);
      }

      private void addEntryToMap(final PropertyEntry<Node> nodePropertyEntry, final Map<Node, Map<String, Object>> modifiedPropsByNode) {
          Map<String, Object> modifiedProps = modifiedPropsByNode.get(nodePropertyEntry.entity());
          if (modifiedProps == null) {
              modifiedProps = new HashMap<String, Object>();
              modifiedPropsByNode.put(nodePropertyEntry.entity(), modifiedProps);
          }
          modifiedProps.put(nodePropertyEntry.key(), nodePropertyEntry.previouslyCommitedValue());
      }

      private void rotateProperties(final long version, final Map<Node, Map<String, Object>> modifiedPropsByNode) {
          for (final Map.Entry<Node, Map<String, Object>> nodeEntry : modifiedPropsByNode.entrySet()) {
              final Node mainNode = nodeEntry.getKey();
              final Node newHistoricNode = mainNode.getGraphDatabase().createNode();
              copyProps(mainNode, newHistoricNode, nodeEntry.getValue());
              insertFirstInChain(mainNode, newHistoricNode, version);
          }
      }

      private void copyProps(final Node node, final Node newNode, final Map<String, Object> oldValues) {
          for (final String propKey : node.getPropertyKeys()) {
              newNode.setProperty(propKey, node.getProperty(propKey, null));
          }
          for (final Map.Entry<String, Object> propEntry : oldValues.entrySet()) {
              final String key = propEntry.getKey();
              final Object value = propEntry.getValue();
              if (value == null) {
                  newNode.removeProperty(key);
              } else {
                  newNode.setProperty(key, value);
              }
          }
      }

      private void insertFirstInChain(final Node mainNode, final Node newHistoricNode, final long version) {
          Relationship prevVersionRel = mainNode.getSingleRelationship(RevisionManager.PREV_REV_REL_TYPE, Direction.OUTGOING);
          if (prevVersionRel != null) {
              newHistoricNode.createRelationshipTo(prevVersionRel.getOtherNode(mainNode), RevisionManager.PREV_REV_REL_TYPE);
              prevVersionRel.delete();
          }
          mainNode.createRelationshipTo(newHistoricNode, RevisionManager.PREV_REV_REL_TYPE);
          final Range mainOld = RevisionManager.getRevisionRange(mainNode);
          final Range mainNew = Range.range(version);
          RevisionManager.setRevisionRange(mainNode, mainNew);
          updateNodeIndex(mainNode, mainNew, mainOld);

          RevisionManager.setStartRevision(newHistoricNode, mainOld.from());
          RevisionManager.setEndRevision(newHistoricNode, version - 1);
          final Range hisNodeNew = RevisionManager.getRevisionRange(newHistoricNode);
          updateNodeIndex(newHistoricNode, hisNodeNew, null);
      }

      public void afterCommit(TransactionData data, Object state) {
      }

      public void afterRollback(TransactionData data, Object state) {
      }

      private void updateNodeIndex(final Node node, final Range range, final Range oldRange) {
          final Index<Node> index = indexProvider.getIndex(IndexingRevisionNodeTypeRepresentationStrategy.INDEX_NAME);
          if (oldRange != null) {
              index.remove(node);
          }
          index.add(node, IndexingNodeTypeRepresentationStrategy.INDEX_KEY, node.getProperty(IndexingNodeTypeRepresentationStrategy.TYPE_PROPERTY_NAME));
          index.add(node, RevisionManager.INDEX_VALID_FROM, ValueContext.numeric(range.from()));
          index.add(node, RevisionManager.INDEX_VALID_TO, ValueContext.numeric(range.to()));
      }

      private void updateRelationshipIndex(final Relationship relationship, final Range range, final Range oldRange) {
          final Index<Relationship> index = indexProvider.getIndex(IndexingRevisionRelationshipTypeRepresentationStrategy.INDEX_NAME);
          if (oldRange != null) {
              index.remove(relationship);
          }
          index.add(relationship, IndexingRelationshipTypeRepresentationStrategy.INDEX_KEY, relationship.getProperty(IndexingRelationshipTypeRepresentationStrategy.TYPE_PROPERTY_NAME));
          index.add(relationship, RevisionManager.INDEX_VALID_FROM, ValueContext.numeric(range.from()));
          index.add(relationship, RevisionManager.INDEX_VALID_TO, ValueContext.numeric(range.to()));
      }
}