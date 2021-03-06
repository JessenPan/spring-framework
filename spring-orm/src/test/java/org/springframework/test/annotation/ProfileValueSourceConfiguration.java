/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.test.annotation;

import java.lang.annotation.*;

/**
 * <p>
 * ProfileValueSourceConfiguration is a class-level annotation which is used to
 * specify what type of {@link ProfileValueSource} to use when retrieving
 * <em>profile values</em> configured via the
 * {@link IfProfileValue @IfProfileValue} annotation.
 * </p>
 *
 * @author Sam Brannen
 * @see ProfileValueSource
 * @see IfProfileValue
 * @see ProfileValueUtils
 * @since 2.5
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
public @interface ProfileValueSourceConfiguration {

    /**
     * <p>
     * The type of {@link ProfileValueSource} to use when retrieving
     * <em>profile values</em>.
     * </p>
     *
     * @see SystemProfileValueSource
     */
    Class<? extends ProfileValueSource> value() default SystemProfileValueSource.class;

}
