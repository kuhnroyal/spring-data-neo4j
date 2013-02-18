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

package org.springframework.data.neo4j.aspects.support.typerepresentation;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.aspects.support.EntityTestBase;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.support.mapping.StoredEntityType;
import org.springframework.data.neo4j.support.typerepresentation.IndexingNodeTypeRepresentationStrategy;
import org.springframework.test.context.CleanContextCacheTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/neo4j/aspects/support/Neo4jGraphPersistenceTests-context.xml",
        "classpath:org/springframework/data/neo4j/aspects/support/IndexingTypeRepresentationStrategyOverride-context.xml"})
@TestExecutionListeners({CleanContextCacheTestExecutionListener.class, DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class})
public class IndexingNodeTypeRepresentationStrategyTests extends EntityTestBase {

	@Autowired
	private IndexingNodeTypeRepresentationStrategy nodeTypeRepresentationStrategy;

    @Autowired
    Neo4jTemplate neo4jTemplate;
    @Autowired
    Neo4jMappingContext ctx;

	private Thing thing;
	private SubThing subThing;
    private StoredEntityType thingType;
    private StoredEntityType subThingType;

    @BeforeTransaction
	public void cleanDb() {
		super.cleanDb();
	}

	@Before
	public void setUp() throws Exception {
		if (thing == null) {
			createThingsAndLinks();
		}
        thingType = typeOf(Thing.class);
        subThingType = typeOf(SubThing.class);
    }

	@Test
	@Transactional
	public void testPostEntityCreation() throws Exception {
		Index<Node> typesIndex = graphDatabaseService.index().forNodes(IndexingNodeTypeRepresentationStrategy.INDEX_NAME);
		IndexHits<Node> thingHits = typesIndex.get(IndexingNodeTypeRepresentationStrategy.INDEX_KEY, thingType.getAlias());
		assertEquals(set(node(thing), node(subThing)), IteratorUtil.addToCollection((Iterable<Node>)thingHits, new HashSet<Node>()));
		IndexHits<Node> subThingHits = typesIndex.get(IndexingNodeTypeRepresentationStrategy.INDEX_KEY, subThingType.getAlias());
		assertEquals(node(subThing), subThingHits.getSingle());
		assertEquals(thingType.getAlias(), node(thing).getProperty(IndexingNodeTypeRepresentationStrategy.TYPE_PROPERTY_NAME));
		assertEquals(subThingType.getAlias(), node(subThing).getProperty(IndexingNodeTypeRepresentationStrategy.TYPE_PROPERTY_NAME));
		thingHits.close();
		subThingHits.close();
	}

	@Test
	public void testPreEntityRemoval() throws Exception {
        manualCleanDb();
        createThingsAndLinks();
		Index<Node> typesIndex = graphDatabaseService.index().forNodes(IndexingNodeTypeRepresentationStrategy.INDEX_NAME);
		IndexHits<Node> thingHits;
		IndexHits<Node> subThingHits;

        Transaction tx = graphDatabaseService.beginTx();
        try
        {
            nodeTypeRepresentationStrategy.preEntityRemoval(node(thing));
            tx.success();
        }
        finally
        {
            tx.finish();
        }

		thingHits = typesIndex.get(IndexingNodeTypeRepresentationStrategy.INDEX_KEY, thingType.getAlias());
		assertEquals(node(subThing), thingHits.getSingle());
		subThingHits = typesIndex.get(IndexingNodeTypeRepresentationStrategy.INDEX_KEY, subThingType.getAlias());
		assertEquals(node(subThing), subThingHits.getSingle());

        tx = graphDatabaseService.beginTx();
        try {
            nodeTypeRepresentationStrategy.preEntityRemoval(node(subThing));
            tx.success();
        }
        finally
        {
            tx.finish();
        }

		thingHits = typesIndex.get(IndexingNodeTypeRepresentationStrategy.INDEX_KEY, thingType.getAlias());
        assertNull(thingHits.getSingle());
		subThingHits = typesIndex.get(IndexingNodeTypeRepresentationStrategy.INDEX_KEY, subThingType.getAlias());
        assertNull(subThingHits.getSingle());
	}

	@Test
	@Transactional
	public void testFindAll() throws Exception {

		assertEquals("Did not find all things.",
                new HashSet<PropertyContainer>(Arrays.asList(neo4jTemplate.getPersistentState(subThing), neo4jTemplate.getPersistentState(thing))),
                IteratorUtil.addToCollection(nodeTypeRepresentationStrategy.findAll(thingType), new HashSet<Node>()));
	}

	@Test
	@Transactional
	public void testCount() throws Exception {
		assertEquals(2, nodeTypeRepresentationStrategy.count(thingType));
	}

	@Test
	@Transactional
	public void testGetJavaType() throws Exception {
		assertEquals(thingType.getAlias(), nodeTypeRepresentationStrategy.readAliasFrom(node(thing)));
		assertEquals(subThingType.getAlias(), nodeTypeRepresentationStrategy.readAliasFrom(node(subThing)));
		assertEquals(Thing.class, neo4jTemplate.getStoredJavaType(node(thing)));
		assertEquals(SubThing.class, neo4jTemplate.getStoredJavaType(node(subThing)));
	}

	@Test
	@Transactional
	public void testCreateEntityAndInferType() throws Exception {
        Thing newThing = neo4jTemplate.createEntityFromStoredType(node(thing), neo4jTemplate.getMappingPolicy(thing));
        assertEquals(thing, newThing);
    }

	@Test
	@Transactional
	public void testCreateEntityAndSpecifyType() throws Exception {
        Thing newThing = neo4jTemplate.createEntityFromState(node(subThing), Thing.class, neo4jTemplate.getMappingPolicy(subThing));
        assertEquals(subThing, newThing);
    }

    @Test
    @Transactional
	public void testProjectEntity() throws Exception {
        Unrelated other = neo4jTemplate.projectTo(node(thing), Unrelated.class);
        assertEquals("thing", other.getName());
	}

	private Node node(Thing thing) {
        return getNodeState(thing);
	}

	private Thing createThingsAndLinks() {
		Transaction tx = graphDatabaseService.beginTx();
		try {
            Node n1 = graphDatabaseService.createNode();
            thing = neo4jTemplate.setPersistentState(new Thing(),n1);
			nodeTypeRepresentationStrategy.writeTypeTo(n1, neo4jTemplate.getEntityType(Thing.class));
            thing.setName("thing");
            Node n2 = graphDatabaseService.createNode();
            subThing = neo4jTemplate.setPersistentState(new SubThing(),n2);
			nodeTypeRepresentationStrategy.writeTypeTo(n2, neo4jTemplate.getEntityType(SubThing.class));
            subThing.setName("subThing");
			tx.success();
			return thing;
		} finally {
			tx.finish();
		}
	}

    @NodeEntity
    public static class Unrelated {
        String name;

        public String getName() {
            return name;
        }
    }

	@NodeEntity
	public static class Thing {
		String name;

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

	public static class SubThing extends Thing {
    }
}
