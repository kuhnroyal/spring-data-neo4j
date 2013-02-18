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

package org.springframework.data.neo4j.rest.integration;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.springframework.data.neo4j.config.NullTransactionManager;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.rest.support.RestTestHelper;
import org.springframework.data.neo4j.template.NeoTraversalTests;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

public class RestNeoTraversalTests extends NeoTraversalTests
{
    private static RestTestHelper testHelper;

    @BeforeClass
    public static void startServer() throws Exception
    {
        testHelper = new RestTestHelper();
        testHelper.startServer();
    }

    @AfterClass
    public static void stopServer() throws Exception
    {
        testHelper.shutdownServer();
    }

    @Override
    protected PlatformTransactionManager createTransactionManager()
    {
        return new JtaTransactionManager(new NullTransactionManager());
    }

    @Override
    protected GraphDatabase createGraphDatabase() throws Exception
    {
        testHelper.cleanDb();
        return testHelper.createGraphDatabase();
    }
}
