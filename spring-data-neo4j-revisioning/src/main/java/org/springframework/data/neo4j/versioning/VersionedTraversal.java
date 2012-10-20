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

import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;

public class VersionedTraversal extends Traversal {

    public static VersionedTraversalDescription description(TraversalDescription delegate) {
        return new VersionedTraversalDescriptionImpl(delegate);
    }

    public static VersionedTraversalDescription description(TraversalDescription delegate, long revision) {
        return new VersionedTraversalDescriptionImpl(delegate, revision);
    }

    public static VersionedTraversalDescription description() {
        return new VersionedTraversalDescriptionImpl();
    }

    public static VersionedTraversalDescription description(long revision) {
        return new VersionedTraversalDescriptionImpl(revision);
    }
}
