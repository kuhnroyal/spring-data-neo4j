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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.data.neo4j.versioning.model.Friendship;
import org.springframework.data.neo4j.versioning.model.Person;
import org.springframework.data.neo4j.versioning.model.Persons;
import org.springframework.data.neo4j.versioning.repository.FriendshipRepository;
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
public class FriendshipRepositoryTest {

    @Autowired
    PersonRepository personRepository;

    @Autowired
    FriendshipRepository repository;

    @Autowired
    VersionedNeo4jTemplate template;

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
            personRepository.save(p);
        }

        Person donald = Persons.getPersons().get(0);
        for (Person p : Persons.getPersons()) {
            if (!"Donald".equals(p.getName())) {
                repository.save(donald.knows(p));
            }
        }
    }

    @Test
    public void testRevisionCount() {
        assertEquals(15, versionManager.getCurrentRevisionNumber());
    }

    @Test
    public void testFindAllPrevious() {
        assertEquals(6, repository.count(14l));
    }

    @Test
    public void testFindAll() {
        assertEquals(7, repository.count());
    }

    @Test
    public void testFindAllBefore() {
        assertEquals(0, repository.count(8l));
    }

    @Test
    public void testFindAllAfter() {
        assertEquals(1, repository.count(9l));
    }

    @Test
    public void findOne() {
        final long id = ((Friendship) repository.findAll().as(ArrayList.class).get(0)).getId();

        Friendship friendship = repository.findOne(id);
        assertNotNull(friendship);

        repository.delete(friendship);

        friendship = repository.findOne(id);
        assertNull(friendship);

        friendship = repository.findOne(id, versionManager.getCurrentRevisionNumber() - 1);
        assertNotNull(friendship);
    }

    @Test
    public void deleteOne() {
        final List<Friendship> friendships = repository.findAll().as(ArrayList.class);
        final Person other = friendships.get(5).getP2();

        repository.delete(friendships.get(5));

        final List<Friendship> newFriendships = repository.findAll().as(ArrayList.class);

        for (Friendship f : newFriendships) {
            if (f.getP2().equals(other)) {
                fail();
            }
        }
        assertEquals(friendships.size(), newFriendships.size() + 1);

        final List<Friendship> oldFriendships = repository.findAll(versionManager.getCurrentRevisionNumber() - 1).as(ArrayList.class);

        boolean found = false;
        for (Friendship f : oldFriendships) {
            if (f.getP2().equals(other)) {
                found = true;
            }
        }
        assertTrue(found);
        assertEquals(friendships.size(), oldFriendships.size());
    }

    @Test
    public void deleteAll() {
        final List<Friendship> friendships = repository.findAll().as(ArrayList.class);

        repository.delete(friendships);

        final List<Friendship> newFriendships = repository.findAll().as(ArrayList.class);
        assertEquals(0, newFriendships.size());

        final List<Friendship> oldFriendships = repository.findAll(versionManager.getCurrentRevisionNumber() - 1).as(ArrayList.class);
        assertEquals(friendships.size(), oldFriendships.size());
    }

    @Test
    public void deleteDeletedStateShouldDoNothing() {
        final Friendship friendship = (Friendship) repository.findAll().as(ArrayList.class).get(5);
        final long rev = versionManager.getCurrentRevisionNumber();

        repository.delete(friendship);
        assertEquals(rev + 1, versionManager.getCurrentRevisionNumber());

        repository.delete(friendship);
        assertEquals(rev + 1, versionManager.getCurrentRevisionNumber());
    }

    @Test(expected = InvalidDataAccessApiUsageException.class)
    public void modifyDeletedStateShouldFail() {
        final Friendship friendship = (Friendship) repository.findAll().as(ArrayList.class).get(5);

        repository.delete(friendship);

        friendship.setYears(7);
        repository.save(friendship);
    }

    @Test
    public void modifyShouldNotFail() {
        final Friendship friendship = (Friendship) repository.findAll().as(ArrayList.class).get(5);

        friendship.setYears(7);
        repository.save(friendship);
    }
}
