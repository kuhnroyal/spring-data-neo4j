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

package org.springframework.data.neo4j.history;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.conversion.EndResult;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.history.RevisionRepository;

@NoRepositoryBean
public interface GraphRevisionRepository<T> extends
        RevisionRepository<T, Long, Long>, GraphRepository<T>, VersionedTraversalRepository<T> {

    T findOne(long id, long revisionNumber);

    boolean exists(long id, long revisionNumber);

    EndResult<T> findAll(long revisionNumber);

    long count(long revisionNumber);

    EndResult<T> findAll(Sort sort, long revisionNumber);

    Page<T> findAll(Pageable pageable, long revisionNumber);
}
