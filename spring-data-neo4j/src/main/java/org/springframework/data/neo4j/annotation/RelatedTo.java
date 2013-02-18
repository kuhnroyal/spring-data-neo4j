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

package org.springframework.data.neo4j.annotation;

import org.springframework.data.annotation.Reference;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for {@link org.springframework.data.neo4j.annotation.NodeEntity} fields that relate to other entities via
 * relationships. Works for one-to-one and one-to-many relationships. It is optionally possible to define the relationship type,
 * relationship direction and target class (required for one-many-relationships).
 *
 * Collection based one-to-many relationships return managed collections that reflect addition and removal to the underlying relationships.
 *
 * Examples:
 * <pre>
 * &#64;RelatedTo(elementClass=Person.class)
 * Collection&lt;Person&gt; friends;
 * &#64;RelatedTo(type=&quot;partner&quot;)
 * Person spouse;
 * </pre>

 * @author Michael Hunger
 * @since 27.08.2010
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD,ElementType.METHOD})
@Reference
public @interface RelatedTo {
    /**
     * @return name of the relationship type, optional, can be inferred from the field name
     */
    String type() default "";

    /**
     * @return direction for the relationship, by default outgoing
     */
    // FQN is a fix for javac compiler bug http://bugs.sun.com/view_bug.do?bug_id=6512707
    org.neo4j.graphdb.Direction direction() default org.neo4j.graphdb.Direction.OUTGOING;

    /**
     * @return target class, possible to specify it optionally or with use of {@link #enforceTargetType}
     */
    Class<?> elementClass() default Object.class;

    /**
     * Used to discriminate between relationships with the same type based on end node type (inferred or from {@link #elementClass}
     */
    boolean enforceTargetType() default false;
}
