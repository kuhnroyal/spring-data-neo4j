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
package org.springframework.data.neo4j.versioning.model;

import java.util.ArrayList;
import java.util.List;

public class Persons {

    private static List<Person> persons;

    static {
        persons = new ArrayList<Person>();
        persons.add(new Person("Donald", 32));
        persons.add(new Person("Daisy", 28));
        persons.add(new Person("Dagobert", 99));
        persons.add(new Person("Emil", 17));
        persons.add(new Person("Lisa", 3));
        persons.add(new Person("Erwin", 100));
        persons.add(new Person("David", 50));
        persons.add(new Person("Michael", 77));
    }

    public static List<Person> getPersons() {
        return persons;
    }
}
