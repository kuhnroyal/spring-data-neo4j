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
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.history.Rev;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.data.neo4j.versioning.model.AuditedRevision;
import org.springframework.data.neo4j.versioning.model.AuditedRevisionProvider;
import org.springframework.data.neo4j.versioning.model.Person;
import org.springframework.data.neo4j.versioning.model.Persons;
import org.springframework.data.neo4j.versioning.repository.AuditedRevisionRepository;
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

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:revisionProvider-test-context.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class AuditedPersonTemplateTest {

    @Autowired
    VersionedNeo4jTemplate template;

    @Autowired
    RevisionManager versionManager;

    @Autowired
    AuditedRevisionProvider provider;

    @Autowired
    AuditedRevisionRepository repository;

    @Autowired
    PlatformTransactionManager transactionManager;

    @BeforeTransaction
    public void cleanDb() throws Exception {
        Neo4jHelper.cleanDb(template);
    }

    @Before
    public void setUp() throws Exception {
        provider.setPerson(null);
        versionManager.createMainRevisionNode();
        Person donald = null;
        for (Person p : Persons.getPersons()) {
            p.setId(null);
            save(p);
            if (donald == null) {
                donald = p;
                provider.setPerson(donald);
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
    public void testRevisionCount() {
        assertEquals(8l, versionManager.getCurrentRevisionNumber());
    }

    @Test
    public void testEntityCountLastesRevisionDefault() {
        assertEquals(8, template.count(Person.class));
    }


    @Test
    public void testEntityCountPreviousRevision() {
        assertEquals(7, template.count(Person.class, versionManager.getCurrentRevisionNumber() - 1));
    }

    @Test
    public void testEntityCountZeroRevision() {
        assertEquals(0, template.count(Person.class, 0l));
    }

    @Test
    public void testEntityCountFirstRevision() {
        assertEquals(1, template.count(Person.class, 1l));
    }

    @Test
    public void firstShouldBeDonald() {
        assertEquals("Donald", template.findAll(Person.class, 1l).iterator().next().getName());
    }

    @Test
    public void testFindOne() {
        Node node = (Node) template.lookup(Person.NAME_INDEX, "name", "Donald").single();
        assertNotNull(node);
        final long id = node.getId();

        Person person = template.findOne(id, Person.class);
        assertNotNull(person);
        assertEquals("Donald", person.getName());

        delete(person);

        person = template.findOne(id, Person.class);
        assertNull(person);

        person = template.findOne(id, Person.class, versionManager.getCurrentRevisionNumber() - 1);
        assertNotNull(person);
        assertEquals("Donald", person.getName());
    }

    @Test
    public void renameDonaldToPluto() {
        Node node = (Node) template.lookup(Person.NAME_INDEX, "name", "Donald").singleOrNull();
        assertNotNull(node);

        Person person = template.projectTo(node, Person.class);
        assertNotNull(person);
        assertEquals("Donald", person.getName());

        final long oldRev = versionManager.getCurrentRevisionNumber();

        person.setName("Pluto");
        save(person);

        final long newRev = versionManager.getCurrentRevisionNumber();
        assertEquals(oldRev, newRev - 1);

        node = (Node) template.lookup(Person.NAME_INDEX, "name", "Donald").singleOrNull();
        assertNull(node);

        node = (Node) template.lookup(Person.NAME_INDEX, "name", "Pluto").singleOrNull();
        assertNotNull(node);
        person = template.projectTo(node, Person.class);
        assertNotNull(person);
        assertEquals("Pluto", person.getName());

        assertEquals(newRev, node.getProperty(RevisionManager.PROPERTY_VALID_FROM));
        assertEquals(Long.MAX_VALUE, node.getProperty(RevisionManager.PROPERTY_VALID_TO));

        assertTrue(node.hasRelationship(RevisionManager.PREV_REV_REL_TYPE, Direction.OUTGOING));
        final Relationship rel = node.getSingleRelationship(RevisionManager.PREV_REV_REL_TYPE, Direction.OUTGOING);
        assertNotNull(rel);
        final Node donald = rel.getEndNode();
        assertEquals(donald.getProperty("name"), "Donald");
        assertEquals(oldRev, donald.getProperty(RevisionManager.PROPERTY_VALID_TO));
        assertEquals(1l, donald.getProperty(RevisionManager.PROPERTY_VALID_FROM));

        boolean foundDonald = false;
        for (Person p : template.findAll(Person.class, oldRev)) {
            if (p.getName().equals("Pluto")) {
                fail();
            }
            if (p.getName().equals("Donald")) {
                foundDonald = true;
            }
        }
        assertTrue(foundDonald);

        boolean foundPluto = false;
        for (Person p : template.findAll(Person.class)) {
            if (p.getName().equals("Pluto")) {
                foundPluto = true;
            }
            if (p.getName().equals("Donald")) {
                fail();
            }
        }
        assertTrue(foundPluto);
    }

    @Test
    public void renameMultipleTimes() {
        Node node = (Node) template.lookup(Person.NAME_INDEX, "name", "Donald").singleOrNull();
        assertNotNull(node);

        Person person = template.projectTo(node, Person.class);
        assertNotNull(person);
        assertEquals("Donald", person.getName());

        final long first = versionManager.getCurrentRevisionNumber();

        person.setName("Pluto");
        save(person);

        final long second = versionManager.getCurrentRevisionNumber();
        assertEquals(first, second - 1);

        person.setName("Tick");
        save(person);

        final long third = versionManager.getCurrentRevisionNumber();
        assertEquals(first, third - 2);

        person.setName("Trick");
        save(person);

        final long fourth = versionManager.getCurrentRevisionNumber();
        assertEquals(first, fourth - 3);

        person.setName("Track");
        save(person);

        final long fifth = versionManager.getCurrentRevisionNumber();
        assertEquals(first, fifth - 4);

        boolean found = false;
        List<Person> all = template.findAll(Person.class).as(ArrayList.class);
        for (Person p : all) {
            if (p.getName().equals("Track")) {
                found = true;
            }
        }
        assertEquals(8, all.size());
        assertTrue(found);

        found = false;
        all = template.findAll(Person.class, fourth).as(ArrayList.class);
        for (Person p : all) {
            if (p.getName().equals("Trick")) {
                found = true;
            }
        }
        assertEquals(8, all.size());
        assertTrue(found);

        found = false;
        all = template.findAll(Person.class, third).as(ArrayList.class);
        for (Person p : all) {
            if (p.getName().equals("Tick")) {
                found = true;
            }
        }
        assertEquals(8, all.size());
        assertTrue(found);

        found = false;
        all = template.findAll(Person.class, second).as(ArrayList.class);
        for (Person p : all) {
            if (p.getName().equals("Pluto")) {
                found = true;
            }
        }
        assertEquals(8, all.size());
        assertTrue(found);

        found = false;
        all = template.findAll(Person.class, first).as(ArrayList.class);
        node = null;
        for (Person p : all) {
            if (p.getName().equals("Donald")) {
                found = true;
                node = template.getNode(p.getId(), first);
            }
        }
        assertEquals(8, all.size());
        assertTrue(found);

        assertNotNull(node);
        assertTrue(node.hasRelationship(RevisionManager.PREV_REV_REL_TYPE, Direction.INCOMING));
        assertFalse(node.hasRelationship(RevisionManager.PREV_REV_REL_TYPE, Direction.OUTGOING));

        node = node.getSingleRelationship(RevisionManager.PREV_REV_REL_TYPE, Direction.INCOMING).getStartNode();
        assertNotNull(node);
        assertTrue(node.hasRelationship(RevisionManager.PREV_REV_REL_TYPE, Direction.INCOMING));
        assertEquals("Pluto", node.getProperty("name"));

        node = node.getSingleRelationship(RevisionManager.PREV_REV_REL_TYPE, Direction.INCOMING).getStartNode();
        assertNotNull(node);
        assertTrue(node.hasRelationship(RevisionManager.PREV_REV_REL_TYPE, Direction.INCOMING));
        assertEquals("Tick", node.getProperty("name"));

        node = node.getSingleRelationship(RevisionManager.PREV_REV_REL_TYPE, Direction.INCOMING).getStartNode();
        assertNotNull(node);
        assertTrue(node.hasRelationship(RevisionManager.PREV_REV_REL_TYPE, Direction.INCOMING));
        assertEquals("Trick", node.getProperty("name"));

        node = node.getSingleRelationship(RevisionManager.PREV_REV_REL_TYPE, Direction.INCOMING).getStartNode();
        assertNotNull(node);
        assertFalse(node.hasRelationship(RevisionManager.PREV_REV_REL_TYPE, Direction.INCOMING));
        assertEquals("Track", node.getProperty("name"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void modifyHistoricStateShouldFail() {
        Node node = (Node) template.lookup(Person.NAME_INDEX, "name", "Donald").singleOrNull();
        assertNotNull(node);

        Person donald = template.projectTo(node, Person.class);
        assertNotNull(donald);
        assertEquals("Donald", donald.getName());

        final long oldRev = versionManager.getCurrentRevisionNumber();

        donald.setName("Pluto");
        save(donald);

        final long newRev = versionManager.getCurrentRevisionNumber();
        assertEquals(oldRev, newRev - 1);

        Person oldDonald = null;
        for (Person p : template.findAll(Person.class, oldRev)) {
            if ("Donald".equals(p.getName())) {
                oldDonald = p;
            }
        }
        assertNotNull(oldDonald);

        oldDonald.setName("Foo");
        save(oldDonald);
    }

    @Test(expected = IllegalArgumentException.class)
    public void modifyDeletedStateShouldFail() {
        Node node = (Node) template.lookup(Person.NAME_INDEX, "name", "Donald").single();
        assertNotNull(node);

        Person donald = template.projectTo(node, Person.class);
        assertNotNull(donald);
        assertEquals("Donald", donald.getName());

        final long oldRev = versionManager.getCurrentRevisionNumber();

        delete(donald);

        final long newRev = versionManager.getCurrentRevisionNumber();
        assertEquals(oldRev, newRev - 1);

        Person oldDonald = null;
        for (Person p : template.findAll(Person.class, oldRev)) {
            if ("Donald".equals(p.getName())) {
                oldDonald = p;
            }
        }
        assertNotNull(oldDonald);

        oldDonald.setName("Pluto");
        save(oldDonald);
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteHistoricStateShouldFail() {
        Node node = (Node) template.lookup(Person.NAME_INDEX, "name", "Donald").single();
        assertNotNull(node);

        Person donald = template.projectTo(node, Person.class);
        assertNotNull(donald);
        assertEquals("Donald", donald.getName());

        final long oldRev = versionManager.getCurrentRevisionNumber();

        donald.setName("Pluto");
        save(donald);

        final long newRev = versionManager.getCurrentRevisionNumber();
        assertEquals(oldRev, newRev - 1);

        Person oldDonald = null;
        for (Person p : template.findAll(Person.class, oldRev)) {
            if ("Donald".equals(p.getName())) {
                oldDonald = p;
            }
        }
        assertNotNull(oldDonald);

        delete(oldDonald);
    }

    @Test
    public void deleteDeletedStateShouldDoNothing() {
        Node node = (Node) template.lookup(Person.NAME_INDEX, "name", "Donald").single();
        assertNotNull(node);

        Person donald = template.projectTo(node, Person.class);
        assertNotNull(donald);
        assertEquals("Donald", donald.getName());

        final long oldRev = versionManager.getCurrentRevisionNumber();

        delete(donald);

        final long newRev = versionManager.getCurrentRevisionNumber();
        assertEquals(oldRev, newRev - 1);

        Person oldDonald = null;
        for (Person p : template.findAll(Person.class, oldRev)) {
            if ("Donald".equals(p.getName())) {
                oldDonald = p;
            }
        }
        assertNotNull(oldDonald);

        delete(oldDonald);
        assertEquals(newRev, versionManager.getCurrentRevisionNumber());
    }

    @Test
    public void testDelete() {
        final List<Person> before = template.findAll(Person.class).as(ArrayList.class);
        final Person delete = before.get(3);

        delete(delete);

        final List<Person> after = template.findAll(Person.class).as(ArrayList.class);
        assertTrue(before.contains(delete));
        assertFalse(after.contains(delete));
        assertEquals(before.size() - 1, after.size());
    }


    @Test
    public void testDeleteAllAndGoBack() {
        final List<Person> before = template.findAll(Person.class).as(ArrayList.class);

        long beforeRev = versionManager.getCurrentRevisionNumber();

        deleteAll(before);

        long afterRev = versionManager.getCurrentRevisionNumber();
        assertEquals(beforeRev, afterRev - 1);

        final List<Person> after = template.findAll(Person.class).as(ArrayList.class);
        assertEquals(0, after.size());

        // go back one revision
        final List<Person> beforeAfter = template.findAll(Person.class, versionManager.getCurrentRevisionNumber() - 1).as(ArrayList.class);
        assertEquals(before.size(), beforeAfter.size());
    }

    @Test
    public void revisionCountShouldEqualAuditedRevisionCount() {
        final long revs = template.count(Rev.class);
        final long aRevs = template.count(AuditedRevision.class);
        assertEquals(9, aRevs);
        assertEquals(revs, aRevs);
    }

    @Test
    public void firstAndSecondRevisionPersonShouldBeNull() {
        final Iterator<AuditedRevision> it = template.findAll(AuditedRevision.class).iterator();
        final AuditedRevision first = it.next();
        assertNull(first.getAuthor());
        final AuditedRevision second = it.next();
        assertNull(second.getAuthor());
    }

    @Test
    public void otherRevisionsPersonShouldBeDonald() {
        final Iterator<AuditedRevision> it = template.findAll(AuditedRevision.class).iterator();
        int count = 0;
        int donaldCount = 0;
        while (it.hasNext()) {
            final AuditedRevision rev = it.next();
            Person p = rev.getAuthor();
            count++;
            if (p != null) {
                template.fetch(p);
                assertEquals("Donald", p.getName());
                donaldCount++;
            }
        }
        assertEquals(7, donaldCount);
        assertEquals(donaldCount, count - 2);
    }

    @Test
    public void testRepository() {
        final Iterator<AuditedRevision> it = repository.findAll().iterator();
        int count = 0;
        int donaldCount = 0;
        while (it.hasNext()) {
            final AuditedRevision rev = it.next();
            Person p = rev.getAuthor();
            count++;
            if (p != null) {
                template.fetch(p);
                assertEquals("Donald", p.getName());
                donaldCount++;
            }
        }
        assertEquals(7, donaldCount);
        assertEquals(donaldCount, count - 2);
    }
}
