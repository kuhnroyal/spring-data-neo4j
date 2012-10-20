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
import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.neo4j.history.Rev;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.data.neo4j.versioning.model.Person;
import org.springframework.data.neo4j.versioning.model.Persons;
import org.springframework.data.neo4j.versioning.repository.PersonRepository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:revision-test-context.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class PersonRepositoryTest {

    @Autowired
    VersionedNeo4jTemplate template;

    @Autowired
    PersonRepository repository;

    @Autowired
    RevisionManager versionManager;

    @BeforeTransaction
    public void cleanDb() throws Exception {
        Neo4jHelper.cleanDb(template);
    }

    @Before
    public void setUp() throws Exception {
        versionManager.createMainRevisionNode();
        for (Person p : Persons.getPersons()) {
            p.setId(null);
            repository.save(p);
        }
    }

    @Test
    public void testRevisionCount() {
        assertEquals(8l, versionManager.getCurrentRevisionNumber());
    }

    @Test
    public void testEntityCountLastesRevisionDefault() {
        assertEquals(8, repository.count());
    }

    @Test
    public void testEntityCountLastesRevisionManual() {
        assertEquals(8, repository.count(versionManager.getCurrentRevisionNumber()));
    }

    @Test
    public void testEntityCountPreviousRevision() {
        assertEquals(7, repository.count(versionManager.getCurrentRevisionNumber() - 1));
    }

    @Test
    public void testEntityCountZeroRevision() {
        assertEquals(0, repository.count(0l));
    }

    @Test
    public void testEntityCountFirstRevision() {
        assertEquals(1, repository.count(1l));
    }

    @Test
    public void firstShouldBeDonald() {
        assertEquals("Donald", repository.findAll(1l).iterator().next().getName());
    }

    @Test
    public void testFindOne() {
        Person person = repository.findByPropertyValue("name", "Donald");
        assertNotNull(person);
        final long id = person.getId();

        person = repository.findOne(id);
        assertNotNull(person);
        assertEquals("Donald", person.getName());

        repository.delete(person);

        person = repository.findOne(id);
        assertNull(person);

        person = repository.findOne(id, versionManager.getCurrentRevisionNumber() - 1);
        assertNotNull(person);
        assertEquals("Donald", person.getName());
    }


    @Test
    public void testExists() {
        Person person = repository.findByPropertyValue("name", "Donald");
        assertNotNull(person);
        final long id = person.getId();

        assertTrue(repository.exists(id));

        repository.delete(person);

        assertFalse(repository.exists(id));
        assertTrue(repository.exists(id, versionManager.getCurrentRevisionNumber() - 1));
    }

    @Test
    public void renameDonaldToPluto() {
        Person person = repository.findByPropertyValue("name", "Donald");
        assertNotNull(person);
        assertEquals("Donald", person.getName());

        final Rev oldRev = versionManager.getCurrentRevision();

        person.setName("Pluto");
        repository.save(person);

        final Rev newRev = versionManager.getCurrentRevision();
        assertEquals(((long) oldRev.getNumber()), newRev.getNumber() - 1l);

        person = repository.findByPropertyValue("name", "Donald");
        assertNull(person);

        person = repository.findByPropertyValue("name", "Pluto");
        assertNotNull(person);
        assertEquals("Pluto", person.getName());

        boolean foundDonald = false;
        for (Person p : repository.findAll(oldRev.getNumber())) {
            if (p.getName().equals("Pluto")) {
                fail();
            }
            if (p.getName().equals("Donald")) {
                foundDonald = true;
            }
        }
        assertTrue(foundDonald);

        boolean foundPluto = false;
        for (Person p : repository.findAll()) {
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
        Person person = repository.findByPropertyValue("name", "Donald");
        assertNotNull(person);

        final long first = versionManager.getCurrentRevisionNumber();

        person.setName("Pluto");
        repository.save(person);

        final long second = versionManager.getCurrentRevisionNumber();
        assertEquals(first, second - 1);

        person.setName("Tick");
        repository.save(person);

        final long third = versionManager.getCurrentRevisionNumber();
        assertEquals(first, third - 2);

        person.setName("Trick");
        repository.save(person);

        final long fourth = versionManager.getCurrentRevisionNumber();
        assertEquals(first, fourth - 3);

        person.setName("Track");
        repository.save(person);

        final long fifth = versionManager.getCurrentRevisionNumber();
        assertEquals(first, fifth - 4);

        boolean found = false;
        List<Person> all = new ArrayList<Person>(IteratorUtil.asCollection(repository.findAll()));
        for (Person p : all) {
            if (p.getName().equals("Track")) {
                found = true;
            }
        }
        assertEquals(8, all.size());
        assertTrue(found);

        found = false;
        all = new ArrayList<Person>(IteratorUtil.asCollection(repository.findAll(fourth)));
        for (Person p : all) {
            if (p.getName().equals("Trick")) {
                found = true;
            }
        }
        assertEquals(8, all.size());
        assertTrue(found);

        found = false;
        all = new ArrayList<Person>(IteratorUtil.asCollection(repository.findAll(third)));
        for (Person p : all) {
            if (p.getName().equals("Tick")) {
                found = true;
            }
        }
        assertEquals(8, all.size());
        assertTrue(found);

        found = false;
        all = new ArrayList<Person>(IteratorUtil.asCollection(repository.findAll(second)));
        for (Person p : all) {
            if (p.getName().equals("Pluto")) {
                found = true;
            }
        }
        assertEquals(8, all.size());
        assertTrue(found);

        found = false;
        all = new ArrayList<Person>(IteratorUtil.asCollection(repository.findAll(first)));
        Node node = null;
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

    @Test(expected = InvalidDataAccessApiUsageException.class)
    public void modifyHistoricStateShouldFail() {
        Person donald = repository.findByPropertyValue("name", "Donald");
        assertNotNull(donald);

        final long oldRev = versionManager.getCurrentRevisionNumber();

        donald.setName("Pluto");
        repository.save(donald);

        final long newRev = versionManager.getCurrentRevisionNumber();
        assertEquals(oldRev, newRev - 1);

        Person oldDonald = null;
        for (Person p : repository.findAll(oldRev)) {
            if ("Donald".equals(p.getName())) {
                oldDonald = p;
            }
        }
        assertNotNull(oldDonald);

        oldDonald.setName("Foo");
        repository.save(oldDonald);
    }


    @Test(expected = InvalidDataAccessApiUsageException.class)
    public void modifyDeletedStateShouldFail() {
        Person donald = repository.findByPropertyValue("name", "Donald");
        assertNotNull(donald);

        final long oldRev = versionManager.getCurrentRevisionNumber();

        repository.delete(donald);

        final long newRev = versionManager.getCurrentRevisionNumber();
        assertEquals(oldRev, newRev - 1);

        Person oldDonald = null;
        for (Person p : repository.findAll(oldRev)) {
            if ("Donald".equals(p.getName())) {
                oldDonald = p;
            }
        }
        assertNotNull(oldDonald);

        oldDonald.setName("Foo");
        repository.save(oldDonald);
    }

    @Test(expected = InvalidDataAccessApiUsageException.class)
    public void deleteHistoricStateShouldFail() {
        Person donald = repository.findByPropertyValue("name", "Donald");
        assertNotNull(donald);

        final long oldRev = versionManager.getCurrentRevisionNumber();

        donald.setName("Pluto");
        repository.save(donald);

        final long newRev = versionManager.getCurrentRevisionNumber();
        assertEquals(oldRev, newRev - 1);

        Person oldDonald = null;
        for (Person p : repository.findAll(oldRev)) {
            if ("Donald".equals(p.getName())) {
                oldDonald = p;
            }
        }
        assertNotNull(oldDonald);

        repository.delete(oldDonald);
    }

    @Test
    public void deleteDeletedStateShouldDoNothing() {
        Person donald = repository.findByPropertyValue("name", "Donald");
        assertNotNull(donald);

        final long oldRev = versionManager.getCurrentRevisionNumber();

        repository.delete(donald);

        final long newRev = versionManager.getCurrentRevisionNumber();
        assertEquals(oldRev, newRev - 1);

        Person oldDonald = null;
        for (Person p : repository.findAll(oldRev)) {
            if ("Donald".equals(p.getName())) {
                oldDonald = p;
            }
        }
        assertNotNull(oldDonald);

        repository.delete(oldDonald);
        assertEquals(newRev, versionManager.getCurrentRevisionNumber());
    }

    @Test
    public void testDelete() {
        final List<Person> before = new ArrayList<Person>();
        for (Person p : repository.findAll()) {
            before.add(p);
        }
        final Person delete = before.get(3);

        repository.delete(delete);

        final List<Person> after = new ArrayList<Person>();
        for (Person p : repository.findAll()) {
            after.add(p);
        }
        assertTrue(before.contains(delete));
        assertFalse(after.contains(delete));
        assertEquals(before.size() - 1, after.size());
    }


    @Test
    public void testDeleteAllAndGoBack() {
        final List<Person> before = new ArrayList<Person>();
        for (Person p : repository.findAll()) {
            before.add(p);
        }

        final long beforeRev = versionManager.getCurrentRevisionNumber();

        repository.delete(before);

        final long afterRev = versionManager.getCurrentRevisionNumber();
        assertEquals(beforeRev, afterRev - 1);

        final List<Person> after = new ArrayList<Person>();
        for (Person p : repository.findAll()) {
            after.add(p);
        }
        assertEquals(0, after.size());

        // go back one revision
        final List<Person> beforeAfter = new ArrayList<Person>();
        for (Person p : repository.findAll(beforeRev)) {
            beforeAfter.add(p);
        }

        assertEquals(before.size(), beforeAfter.size());
    }
}
