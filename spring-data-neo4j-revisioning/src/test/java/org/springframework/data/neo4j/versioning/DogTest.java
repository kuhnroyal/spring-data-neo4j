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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.data.neo4j.versioning.model.Car;
import org.springframework.data.neo4j.versioning.model.Dog;
import org.springframework.data.neo4j.versioning.model.Ownership;
import org.springframework.data.neo4j.versioning.model.Person;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:revision-test-context.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class DogTest {

    @Autowired
    VersionedNeo4jTemplate template;

    @Autowired
    RevisionManager versionManager;

    @Autowired
    PlatformTransactionManager transactionManager;

    Person thomas, peter;

    Car audi, lambo, trabbi;

    Dog schnuffi;

    @BeforeTransaction
    public void cleanDb() throws Exception {
        Neo4jHelper.cleanDb(template);
    }

    @Before
    public void setUp() throws Exception {
        versionManager.createMainRevisionNode();
        audi = new Car();
        audi.setBrand("Audi R8");
        audi.setSpeed(280);
        lambo = new Car();
        audi.setBrand("Lamborghini Murciélago");
        audi.setSpeed(300);
        trabbi = new Car();
        audi.setBrand("Trabbi");
        audi.setSpeed(50);
        save(audi, lambo, trabbi);

        thomas = new Person();
        thomas.setName("Thomas");
        thomas.setAge(26);
        peter = new Person();
        peter.setName("Peter");
        peter.setAge(28);
        save(thomas, peter);

        Ownership oa = new Ownership();
        oa.setCar(audi);
        oa.setPerson(peter);
        Ownership ol = new Ownership();
        ol.setCar(lambo);
        ol.setPerson(peter);
        Ownership ot = new Ownership();
        ot.setCar(trabbi);
        ot.setPerson(thomas);
        save(oa, ol, ot);

        schnuffi = new Dog();
        schnuffi.setName("Schnuffi");
        schnuffi.setMaster(thomas);
        save(schnuffi);
    }

    public void save(final Object... obj) {
        new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                for (Object o : obj) {
                    template.save(o);
                }
            }
        });
    }

    public void delete(final Object... obj) {
        new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                for (Object o : obj) {
                    template.delete(o);
                }
            }
        });
    }

    @Test
    @Ignore
    public void testDogShouldBeNullAfterDelete() {
        Person p = template.findOne(thomas.getId(), Person.class);
        assertNotNull(p.getDog());
        assertEquals(schnuffi.getId(), p.getDog().getId());
        final long rev = versionManager.getCurrentRevisionNumber();

        delete(p.getDog());

        p = template.findOne(thomas.getId(), Person.class);
        assertNull(p.getDog());

        p = template.findOne(thomas.getId(), Person.class, rev);
        assertNotNull(p.getDog());
        assertEquals(schnuffi.getId(), p.getDog().getId());
    }

    @Test
    @Ignore
    public void testCars() {
        final HashSet<Ownership> set = template.findAll(Ownership.class).as(HashSet.class);
        assertEquals(3, set.size());
        for (Ownership ownership : set) {
            System.out.println(ownership.getPerson().getId());
        }

        Person p = template.findOne(peter.getId(), Person.class);
        Iterable<Ownership> ownerships = p.getOwnerships();
        List<Ownership> list = new ArrayList<Ownership>();
        for (Ownership o : ownerships) {
            list.add(o);
        }
        assertEquals(2, list.size());
        final long rev = versionManager.getCurrentRevisionNumber();

        delete(audi);

        p = template.findOne(peter.getId(), Person.class);
        ownerships = p.getOwnerships();
        list = new ArrayList<Ownership>();
        for (Ownership o : ownerships) {
            list.add(o);
            final Car car = o.getCar();
            assertNotNull(car);
            assertNotNull(template.getRelationship(o.getId()));
            System.out.println(car.getBrand());
        }
        assertEquals(1, list.size());

        p = template.findOne(peter.getId(), Person.class, rev);
        ownerships = p.getOwnerships();
        list = new ArrayList<Ownership>();
        for (Ownership o : ownerships) {
            list.add(o);
        }
        assertEquals(2, list.size());
    }
}
