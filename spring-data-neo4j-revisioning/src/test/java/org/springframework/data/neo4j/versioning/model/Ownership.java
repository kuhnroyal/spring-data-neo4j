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

import org.springframework.data.neo4j.annotation.*;
import org.springframework.data.neo4j.history.Revisioned;

@Revisioned
@RelationshipEntity(type = "owns")
public class Ownership {

    @GraphId
    private Long id;

    @Fetch
    @StartNode
    private Person person;

    @Fetch
    @EndNode
    private Car car;

    public Ownership() {
    }

    public Ownership(Person person, Car car) {
        this.person = person;
        this.car = car;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public Car getCar() {
        return car;
    }

    public void setCar(Car car) {
        this.car = car;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Ownership ownership = (Ownership) o;

        if (car != null ? !car.equals(ownership.car) : ownership.car != null) return false;
        if (id != null ? !id.equals(ownership.id) : ownership.id != null) return false;
        if (person != null ? !person.equals(ownership.person) : ownership.person != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (person != null ? person.hashCode() : 0);
        result = 31 * result + (car != null ? car.hashCode() : 0);
        return result;
    }
}
