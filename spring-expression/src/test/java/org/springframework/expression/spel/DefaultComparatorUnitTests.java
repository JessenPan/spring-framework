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
package org.springframework.expression.spel;

import org.junit.Test;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypeComparator;
import org.springframework.expression.spel.support.StandardTypeComparator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for type comparison
 *
 * @author Andy Clement
 */
public class DefaultComparatorUnitTests {

    @Test
    public void testPrimitives() throws EvaluationException {
        TypeComparator comparator = new StandardTypeComparator();
        // primitive int
        assertTrue(comparator.compare(1, 2) < 0);
        assertTrue(comparator.compare(1, 1) == 0);
        assertTrue(comparator.compare(2, 1) > 0);

        assertTrue(comparator.compare(1.0d, 2) < 0);
        assertTrue(comparator.compare(1.0d, 1) == 0);
        assertTrue(comparator.compare(2.0d, 1) > 0);

        assertTrue(comparator.compare(1.0f, 2) < 0);
        assertTrue(comparator.compare(1.0f, 1) == 0);
        assertTrue(comparator.compare(2.0f, 1) > 0);

        assertTrue(comparator.compare(1L, 2) < 0);
        assertTrue(comparator.compare(1L, 1) == 0);
        assertTrue(comparator.compare(2L, 1) > 0);

        assertTrue(comparator.compare(1, 2L) < 0);
        assertTrue(comparator.compare(1, 1L) == 0);
        assertTrue(comparator.compare(2, 1L) > 0);

        assertTrue(comparator.compare(1L, 2L) < 0);
        assertTrue(comparator.compare(1L, 1L) == 0);
        assertTrue(comparator.compare(2L, 1L) > 0);
    }

    @Test
    public void testNulls() throws EvaluationException {
        TypeComparator comparator = new StandardTypeComparator();
        assertTrue(comparator.compare(null, "abc") < 0);
        assertTrue(comparator.compare(null, null) == 0);
        assertTrue(comparator.compare("abc", null) > 0);
    }

    @Test
    public void testObjects() throws EvaluationException {
        TypeComparator comparator = new StandardTypeComparator();
        assertTrue(comparator.compare("a", "a") == 0);
        assertTrue(comparator.compare("a", "b") < 0);
        assertTrue(comparator.compare("b", "a") > 0);
    }

    @Test
    public void testCanCompare() throws EvaluationException {
        TypeComparator comparator = new StandardTypeComparator();
        assertTrue(comparator.canCompare(null, 1));
        assertTrue(comparator.canCompare(1, null));

        assertTrue(comparator.canCompare(2, 1));
        assertTrue(comparator.canCompare("abc", "def"));
        assertTrue(comparator.canCompare("abc", 3));
        assertFalse(comparator.canCompare(String.class, 3));
    }

}
