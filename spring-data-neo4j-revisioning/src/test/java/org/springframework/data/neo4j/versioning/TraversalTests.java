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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.data.neo4j.versioning.model.Person;
import org.springframework.data.neo4j.versioning.model.Persons;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:revision-test-context.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class TraversalTests {

    @Autowired
    VersionedNeo4jTemplate template;

    @Autowired
    RevisionManager versionManager;

    @Autowired
    PlatformTransactionManager transactionManager;

    @BeforeTransaction
    public void cleanDb() throws Exception {
        Neo4jHelper.cleanDb(template);
    }

    @Before
    public void setUp() throws Exception {
        versionManager.createMainRevisionNode();
        for (Person p : Persons.getPersons()) {
            p.setId(null);
            save(p);
        }

        Person donald = Persons.getPersons().get(0);
        for (Person p : Persons.getPersons()) {
            if (!"Donald".equals(p.getName())) {
                save(donald.knows(p));
            }
        }
    }

    public void save(final Object o) {
        new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                template.save(o);
            }
        });
    }

    public void delete(final Object o) {
        new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                template.delete(o);
            }
        });
    }

    public void deleteAll(final Iterable<?> os) {
        new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                for (Object o : os) {
                    template.delete(o);
                }
            }
        });
    }

    @Test
    public void testFindAllFriendsByTraversal() {
        final Long id = Persons.getPersons().get(0).getId();
        final Node donald = template.getNode(id);
        final List<Person> friends = new ArrayList<Person>();
        final Iterable<Path> paths = template.traversalDescription().relationships(DynamicRelationshipType.withName("knows")).traverse(donald);
        for (Path path : paths) {
            if (path.length() > 1) {
                fail();
            }
            for (Relationship rel : path.relationships()) {
                if (rel.isType(RevisionManager.PREV_REV_REL_TYPE)) {
                    fail();
                }
            }
            if (path.length() == 1) {
                friends.add(template.findOne(path.endNode().getId(), Person.class));
            }
        }
        assertEquals(7, friends.size());
    }

    @Test
    public void testDeleteSomeFriendshipsAndFindAllFriendsByTraversal() {
        final Long id = Persons.getPersons().get(0).getId();
        final Node donald = template.getNode(id);

        List<Person> friends = new ArrayList<Person>();
        Iterable<Path> paths = VersionedTraversal.description().relationships(DynamicRelationshipType.withName("knows")).traverse(donald);
        for (final Path path : paths) {
            if (path.length() == 1) {
                final long id1 = path.endNode().getId();
                final Person one = template.findOne(id1, Person.class);
                friends.add(one);
            }
        }
        assertEquals(7, friends.size());

        final long before = versionManager.getCurrentRevisionNumber();

        final Iterator<Relationship> it = donald.getRelationships(DynamicRelationshipType.withName("knows")).iterator();
        delete(it.next());
        delete(it.next());

        final long after = versionManager.getCurrentRevisionNumber();
        assertEquals(before, after - 2);

        friends = new ArrayList<Person>();
        paths = template.traversalDescription().relationships(DynamicRelationshipType.withName("knows")).traverse(donald);
        for (Path path : paths) {
            if (path.length() == 1) {
                friends.add(template.findOne(path.endNode().getId(), Person.class));
            }
        }
        assertEquals(5, friends.size());

        friends = new ArrayList<Person>();
        paths = template.traversalDescription(before).relationships(DynamicRelationshipType.withName("knows")).traverse(donald);
        for (Path path : paths) {
            if (path.length() == 1) {
                friends.add(template.findOne(path.endNode().getId(), Person.class));
            }
        }
        assertEquals(7, friends.size());
    }
}
