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
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.data.neo4j.versioning.model.Friendship;
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

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:revision-test-context.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class FriendshipTemplateTest {

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
    public void testRevisionCount() {
        assertEquals(15, versionManager.getCurrentRevisionNumber());
    }

    @Test
    public void testFindAll() {
        assertEquals(7, template.count(Friendship.class));
    }

//    @Test
//    public void testFindAllPrevious() {
//        assertEquals(6, template.count(Friendship.class, Revisions.get(14)));
//    }
//
//    @Test
//    public void testFindAllBefore() {
//        assertEquals(0, template.count(Friendship.class, Revisions.get(8)));
//    }
//
//    @Test
//    public void testFindAllAfter() {
//        assertEquals(1, template.count(Friendship.class, Revisions.get(9)));
//    }
//
//    @Test
//    public void findOne() {
//        final long id = ((Friendship) template.findAll(Friendship.class).as(ArrayList.class).get(0)).getId();
//
//        Friendship friendship = template.findOne(id, Friendship.class);
//        assertNotNull(friendship);
//
//        delete(friendship);
//
//        friendship = template.findOne(id, Friendship.class);
//        assertNull(friendship);
//
//        friendship = template.findOne(id, Friendship.class, Revisions.get(versionManager.getCurrentRevisionValue() - 1));
//        assertNotNull(friendship);
//    }
//
//    @Test
//    public void deleteOne() {
//        final List<Friendship> friendships = template.findAll(Friendship.class).as(ArrayList.class);
//        final Person other = friendships.get(5).getP2();
//
//        delete(friendships.get(5));
//
//        final List<Friendship> newFriendships = template.findAll(Friendship.class).as(ArrayList.class);
//
//        for (Friendship f : newFriendships) {
//            if (f.getP2().equals(other)) {
//                fail();
//            }
//        }
//        assertEquals(friendships.size(), newFriendships.size() + 1);
//
//        final List<Friendship> oldFriendships = template.findAll(Friendship.class, Revisions.get(versionManager.getCurrentRevisionValue() - 1)).as(ArrayList.class);
//
//        boolean found = false;
//        for (Friendship f : oldFriendships) {
//            if (f.getP2().equals(other)) {
//                found = true;
//            }
//        }
//        assertTrue(found);
//        assertEquals(friendships.size(), oldFriendships.size());
//    }
//
//    @Test
//    public void deleteAll() {
//        final List<Friendship> friendships = template.findAll(Friendship.class).as(ArrayList.class);
//
//        deleteAll(friendships);
//
//        final List<Friendship> newFriendships = template.findAll(Friendship.class).as(ArrayList.class);
//        assertEquals(0, newFriendships.size());
//
//        final List<Friendship> oldFriendships = template.findAll(Friendship.class, Revisions.get(versionManager.getCurrentRevisionValue() - 1)).as(ArrayList.class);
//        assertEquals(friendships.size(), oldFriendships.size());
//    }
//
//    @Test
//    public void deleteDeletedStateShouldDoNothing() {
//        final Friendship friendship = (Friendship) template.findAll(Friendship.class).as(ArrayList.class).get(5);
//        final long rev = versionManager.getCurrentRevisionValue();
//
//        delete(friendship);
//        assertEquals(rev + 1, versionManager.getCurrentRevisionValue());
//
//        delete(friendship);
//        assertEquals(rev + 1, versionManager.getCurrentRevisionValue());
//    }

    @Test(expected = IllegalArgumentException.class)
    public void modifyDeletedStateShouldFail() {
        final Friendship friendship = (Friendship) template.findAll(Friendship.class).as(ArrayList.class).get(5);

        delete(friendship);

        friendship.setYears(7);
        save(friendship);
    }

    @Test
    public void modifyDeletedStateShouldNotFail() {
        final Friendship friendship = (Friendship) template.findAll(Friendship.class).as(ArrayList.class).get(5);

        friendship.setYears(7);
        save(friendship);
    }
}
