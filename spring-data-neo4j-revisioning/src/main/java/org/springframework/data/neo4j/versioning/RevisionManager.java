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

import org.joda.time.DateTime;
import org.neo4j.graphdb.*;
import org.springframework.data.neo4j.history.Rev;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

public class RevisionManager {

    public static final String PROPERTY_DELETED = "__DELETED_ENTITY__";

    public static final String PROPERTY_REVISIONED = "__REVISION_ENTITY__";

    public static final String PROPERTY_VALID_FROM = "__VALID_FROM__";

    public static final String PROPERTY_VALID_TO = "__VALID_TO__";

    public static final String INDEX_VALID_FROM = "__VALID_FROM__";

    public static final String INDEX_VALID_TO = "__VALID_TO__";

    public static final RelationshipType PREV_REV_REL_TYPE = DynamicRelationshipType.withName("__PREV_REV__");

    public static final RelationshipType REVISIONS_REL_TYPE = DynamicRelationshipType.withName("__REVISIONS__");

    public static final Long ANY = -1l;

    public static final Long LATEST = Long.MAX_VALUE;

    private VersionedNeo4jTemplate template;

    private final RevisionProvider<? extends Rev> provider;

    private final PlatformTransactionManager transactionManager;

    private Rev current;

    public RevisionManager(RevisionProvider<? extends Rev> provider, PlatformTransactionManager transactionManager, VersionedNeo4jTemplate template) {
        this.provider = provider;
        this.transactionManager = transactionManager;
        this.template = template;
    }

    synchronized boolean createMainRevisionNode() {
        final Relationship versions = template.getReferenceNode().getSingleRelationship(REVISIONS_REL_TYPE, Direction.OUTGOING);

        final Rev rev;
        if (versions == null) {
            rev = createRevision(0l, new DateTime());
            new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) {
                    template.save(rev);
                    final Node current = template.getNode(rev.getId());
                    template.getReferenceNode().createRelationshipTo(current, REVISIONS_REL_TYPE);
                }
            });
        } else {
            final Node current = versions.getEndNode();
            rev = template.projectTo(current, Rev.class);
        }
        this.current = rev;
        return versions == null;
    }

    synchronized Long increaseVersion() {
        return new TransactionTemplate(transactionManager).execute(new TransactionCallback<Long>() {
            @Override
            public Long doInTransaction(TransactionStatus status) {
                final Long newVersion = getCurrentRevisionNumber() + 1;
                final Rev rev = createRevision(newVersion, new DateTime());
                template.save(rev);

                final Node newRev = template.getNode(rev.getId());
                final Node oldRev = template.getNode(getCurrentRevision().getId());
                oldRev.getSingleRelationship(REVISIONS_REL_TYPE, Direction.INCOMING).delete();
                newRev.createRelationshipTo(oldRev, PREV_REV_REL_TYPE);
                template.getReferenceNode().createRelationshipTo(newRev, REVISIONS_REL_TYPE);
                current = rev;
                return newVersion;
            }
        });
    }

    private Rev createRevision(Long revision, DateTime date) {
        final Rev rev = provider.getRevision();
        rev.setDate(date);
        rev.setNumber(revision);
        return rev;
    }

    public synchronized long getCurrentRevisionNumber() {
        return getCurrentRevision().getNumber();
    }

    public synchronized <R extends Rev> R getCurrentRevision() {
        if (current == null) {
            createMainRevisionNode();
        }
        return (R) current;
    }

    public static Long getRevisionNumber(PropertyContainer pc) {
        if (isVersioned(pc)) {
            return (Long) pc.getProperty(PROPERTY_VALID_FROM);
        }
        return null;
    }

    public static boolean isVersioned(PropertyContainer container) {
        return container.hasProperty(PROPERTY_REVISIONED);
    }

    public static boolean isDeleted(PropertyContainer container) {
        return container.hasProperty(PROPERTY_DELETED);
    }

    public static void setDeleted(PropertyContainer container) {
        if (container != null && container.hasProperty(PROPERTY_VALID_FROM)) {
            container.setProperty(PROPERTY_DELETED, "");
        }
    }

    public static void setRevisioned(PropertyContainer container) {
        container.setProperty(PROPERTY_REVISIONED, "");
    }

    public static boolean hasValidRevision(PropertyContainer propertyContainer) {
        return hasValidRevision(propertyContainer, Long.MAX_VALUE);
    }

    public static boolean hasValidRevision(PropertyContainer propertyContainer, Long revision) {
        final Range range = getRevisionRange(propertyContainer);
        return range != null && range.contains(revision);
    }

    public static Range getRevisionRange(final PropertyContainer propertyContainer) {
        if (propertyContainer == null) {
            return null;
        }
        final Object from = propertyContainer.getProperty(PROPERTY_VALID_FROM, null);
        final Object to = propertyContainer.getProperty(PROPERTY_VALID_TO, null);
        if (from == null || to == null) {
            return null;
        }
        return new Range((Long) from, (Long) to);
    }

    static void setStartRevision(final PropertyContainer entity, final long startVersion) {
        entity.setProperty(PROPERTY_VALID_FROM, startVersion);
    }

    static void setEndRevision(final PropertyContainer entity, final long endVersion) {
        entity.setProperty(PROPERTY_VALID_TO, endVersion);
    }

    public static long getStartRevision(final PropertyContainer entity) {
        return (Long) entity.getProperty(PROPERTY_VALID_FROM, -1L);
    }

    public static long getEndRevision(final PropertyContainer entity) {
        return (Long) entity.getProperty(PROPERTY_VALID_TO, -1L);
    }

    static void setRevisionRange(final PropertyContainer propertyContainer, final Range range) {
        setStartRevision(propertyContainer, range.from());
        setEndRevision(propertyContainer, range.to());
    }
}
