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

package org.springframework.http.client;

import org.junit.Test;
import org.springframework.http.HttpMethod;

import java.net.ProtocolException;

public class BufferedSimpleHttpRequestFactoryTests extends AbstractHttpRequestFactoryTestCase {

    @Override
    protected ClientHttpRequestFactory createRequestFactory() {
        return new SimpleClientHttpRequestFactory();
    }

    @Override
    @Test
    public void httpMethods() throws Exception {
        try {
            assertHttpMethod("patch", HttpMethod.PATCH);
        } catch (ProtocolException ex) {
            // Currently HttpURLConnection does not support HTTP PATCH
        }
    }

}
