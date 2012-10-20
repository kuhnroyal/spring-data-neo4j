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

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.*;
import org.springframework.data.neo4j.history.Revisioned;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.lang.Iterable;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;

@Revisioned
@NodeEntity
public class Person {

    public static final String NAME_INDEX = "person_name_index";

    @GraphId
    private Long id;

    @Indexed(indexName = NAME_INDEX)
    @Size(min = 3, max = 20)
    private String name;

    @Max(100)
    @Min(0)
    @Indexed
    private int age;

    @Fetch
    @RelatedToVia(type = "knows", elementClass = Friendship.class)
    private Iterable<Friendship> friendships;

    @Fetch
    @RelatedToVia(type = "owns", direction = Direction.OUTGOING)
    private Iterable<Ownership> ownerships;

    @Fetch
    @RelatedTo(type = "master", direction = Direction.OUTGOING)
    private Dog dog;

    public Person() {
    }

    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public Iterable<Friendship> getFriendships() {
        return friendships;
    }

    public void setFriendships(Iterable<Friendship> friendships) {
        this.friendships = friendships;
    }

    public Iterable<Ownership> getOwnerships() {
        return ownerships;
    }

    public void setOwnerships(Iterable<Ownership> ownerships) {
        this.ownerships = ownerships;
    }

    public Friendship knows(Person p) {
        return new Friendship(this, p, "knows");
    }

    public Ownership owns(Car car) {
        return new Ownership(this, car);
    }

    public Dog getDog() {
        return dog;
    }

    public void setDog(Dog dog) {
        this.dog = dog;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Person person = (Person) o;

        if (age != person.age) return false;
        if (id != null ? !id.equals(person.id) : person.id != null) return false;
        if (name != null ? !name.equals(person.name) : person.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + age;
        return result;
    }
}
