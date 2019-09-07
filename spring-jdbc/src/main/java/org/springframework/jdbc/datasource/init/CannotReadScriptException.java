/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.jdbc.datasource.init;

import org.springframework.core.io.support.EncodedResource;

/**
 * Thrown by {@link ResourceDatabasePopulator} if one of its SQL scripts cannot
 * be read during population.
 *
 * @author Keith Donald
 * @since 3.0
 */
@SuppressWarnings("serial")
public class CannotReadScriptException extends RuntimeException {

    /**
     * Construct a new {@code CannotReadScriptException}.
     *
     * @param resource the resource that cannot be read from
     * @param cause    the underlying cause of the resource access failure
     */
    public CannotReadScriptException(EncodedResource resource, Throwable cause) {
        super("Cannot read SQL script from " + resource, cause);
    }

}
