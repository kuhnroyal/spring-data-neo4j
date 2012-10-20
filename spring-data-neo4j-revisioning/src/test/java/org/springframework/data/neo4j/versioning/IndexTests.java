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
import org.neo4j.graphdb.Node;
import org.neo4j.index.lucene.QueryContext;
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
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:revision-test-context.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class IndexTests {

    @Autowired
    VersionedNeo4jTemplate template;

    @Autowired
    RevisionManager versionManager;

    @Autowired
    PlatformTransactionManager transactionManager;

    Person donald;

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

        donald = Persons.getPersons().get(0);
        for (Person p : Persons.getPersons()) {
            if (!"Donald".equals(p.getName())) {
                save(donald.knows(p));
            }
        }
    }

    public <T> T save(final T o) {
        return new TransactionTemplate(transactionManager).execute(new TransactionCallback<T>() {
            @Override
            public T doInTransaction(TransactionStatus status) {
                return template.save(o);
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
    @Ignore
    // not sure if this is actually the wanted behavior, index needs to be revisioned as well
    public void testOldIndexValueShouldBeFound() {
        final Node before = (Node) template.lookup(Person.class, "age", QueryContext.numericRange("age", 32, 32)).single();
        assertEquals(donald.getName(), before.getProperty("name"));

        donald.setAge(33);
        save(donald);

        final Node after = (Node) template.lookup(Person.class, "age", QueryContext.numericRange("age", 32, 32)).single();
        assertEquals(donald.getName(), after.getProperty("name"));
        assertFalse(before.equals(after));
        assertFalse(before.getId() == after.getId());
    }

    @Test
    public void testNewIndexValueShouldBeFound() {
        final Node before = (Node) template.lookup(Person.class, "age", QueryContext.numericRange("age", 32, 32)).single();
        assertEquals(donald.getName(), before.getProperty("name"));

        donald.setAge(33);
        save(donald);

        final Node after = (Node) template.lookup(Person.class, "age", QueryContext.numericRange("age", 33, 33)).single();
        assertEquals(donald.getName(), after.getProperty("name"));
        assertEquals(before, after);
        assertEquals(before, after);
    }
}
