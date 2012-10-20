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

import org.springframework.data.neo4j.annotation.EndNode;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.RelationshipEntity;
import org.springframework.data.neo4j.annotation.RelationshipType;
import org.springframework.data.neo4j.annotation.StartNode;
import org.springframework.data.neo4j.history.Revisioned;

import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;

@Revisioned
@RelationshipEntity
public class Friendship {

    @GraphId
    private Long id;

    @StartNode
    private Person p1;

    @EndNode
    private Person p2;

    @RelationshipType
    private String type;

    @Indexed
    private int years;

    public Friendship() {
    }

    public Friendship(Person p1, Person p2) {
        this.p1 = p1;
        this.p2 = p2;
    }

    public Friendship(Person p1, Person p2, int years) {
        this.p1 = p1;
        this.p2 = p2;
        this.years = years;
    }


    public Friendship(Person start, Person end, String type) {
        this.p1 = start;
        this.p2 = end;
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Person getP1() {
        return p1;
    }

    public void setP1(Person p1) {
        this.p1 = p1;
    }

    public Person getP2() {
        return p2;
    }

    public void setP2(Person p2) {
        this.p2 = p2;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getYears() {
        return years;
    }

    public void setYears(int years) {
        this.years = years;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Friendship that = (Friendship) o;

        if (years != that.years) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (p1 != null ? !p1.equals(that.p1) : that.p1 != null) return false;
        if (p2 != null ? !p2.equals(that.p2) : that.p2 != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (p1 != null ? p1.hashCode() : 0);
        result = 31 * result + (p2 != null ? p2.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + years;
        return result;
    }
}
