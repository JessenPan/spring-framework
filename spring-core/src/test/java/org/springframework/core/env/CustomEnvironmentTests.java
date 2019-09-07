/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.core.env;

import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit tests covering the extensibility of {@link AbstractEnvironment}.
 *
 * @author Chris Beams
 * @since 3.1
 */
public class CustomEnvironmentTests {

    // -- tests relating to customizing reserved default profiles ----------------------

    @Test
    public void control() {
        Environment env = new AbstractEnvironment() {
        };
        assertThat(env.acceptsProfiles(AbstractEnvironment.RESERVED_DEFAULT_PROFILE_NAME), is(true));
    }

    @Test
    public void withNoReservedDefaultProfile() {
        class CustomEnvironment extends AbstractEnvironment {
            @Override
            protected Set<String> getReservedDefaultProfiles() {
                return Collections.emptySet();
            }
        }

        Environment env = new CustomEnvironment();
        assertThat(env.acceptsProfiles(AbstractEnvironment.RESERVED_DEFAULT_PROFILE_NAME), is(false));
    }

    @Test
    public void withSingleCustomReservedDefaultProfile() {
        class CustomEnvironment extends AbstractEnvironment {
            @Override
            protected Set<String> getReservedDefaultProfiles() {
                return Collections.singleton("rd1");
            }
        }

        Environment env = new CustomEnvironment();
        assertThat(env.acceptsProfiles(AbstractEnvironment.RESERVED_DEFAULT_PROFILE_NAME), is(false));
        assertThat(env.acceptsProfiles("rd1"), is(true));
    }

    @Test
    public void withMultiCustomReservedDefaultProfile() {
        class CustomEnvironment extends AbstractEnvironment {
            @Override
            @SuppressWarnings("serial")
            protected Set<String> getReservedDefaultProfiles() {
                return new HashSet<String>() {{
                    add("rd1");
                    add("rd2");
                }};
            }
        }

        ConfigurableEnvironment env = new CustomEnvironment();
        assertThat(env.acceptsProfiles(AbstractEnvironment.RESERVED_DEFAULT_PROFILE_NAME), is(false));
        assertThat(env.acceptsProfiles("rd1", "rd2"), is(true));

        // finally, issue additional assertions to cover all combinations of calling these
        // methods, however unlikely.
        env.setDefaultProfiles("d1");
        assertThat(env.acceptsProfiles("rd1", "rd2"), is(false));
        assertThat(env.acceptsProfiles("d1"), is(true));

        env.setActiveProfiles("a1", "a2");
        assertThat(env.acceptsProfiles("d1"), is(false));
        assertThat(env.acceptsProfiles("a1", "a2"), is(true));

        env.setActiveProfiles();
        assertThat(env.acceptsProfiles("d1"), is(true));
        assertThat(env.acceptsProfiles("a1", "a2"), is(false));

        env.setDefaultProfiles();
        assertThat(env.acceptsProfiles(AbstractEnvironment.RESERVED_DEFAULT_PROFILE_NAME), is(false));
        assertThat(env.acceptsProfiles("rd1", "rd2"), is(false));
        assertThat(env.acceptsProfiles("d1"), is(false));
        assertThat(env.acceptsProfiles("a1", "a2"), is(false));
    }


    // -- tests relating to customizing property sources -------------------------------
}
