/*
 * Copyright 2007 the original author or authors.
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

package org.springframework.test.context.junit4.spr3896;

import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * JUnit 4 based integration test for verifying support for the
 * {@link ContextConfiguration#inheritLocations() inheritLocations} flag of
 * {@link ContextConfiguration @ContextConfiguration} indirectly proposed in <a
 * href="http://opensource.atlassian.com/projects/spring/browse/SPR-3896"
 * target="_blank">SPR-3896</a>.
 *
 * @author Sam Brannen
 * @since 2.5
 */
@ContextConfiguration("BeanOverridingDefaultLocationsInheritedTests-context.xml")
public class BeanOverridingExplicitLocationsInheritedTests extends ExplicitLocationsBaseTests {

    @Test
    @Override
    public void verifyEmployeeSetFromBaseContextConfig() {
        assertNotNull("The employee should have been autowired.", this.employee);
        assertEquals("The employee bean should have been overridden.", "Yoda", this.employee.getName());
    }
}
