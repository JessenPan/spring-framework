/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.mock.web.portlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

import javax.portlet.*;
import java.io.IOException;

/**
 * Mock implementation of the {@link javax.portlet.PortletRequestDispatcher} interface.
 *
 * @author John A. Lewis
 * @author Juergen Hoeller
 * @since 2.0
 */
public class MockPortletRequestDispatcher implements PortletRequestDispatcher {

    private final Log logger = LogFactory.getLog(getClass());

    private final String url;


    /**
     * Create a new MockPortletRequestDispatcher for the given URL.
     *
     * @param url the URL to dispatch to.
     */
    public MockPortletRequestDispatcher(String url) {
        Assert.notNull(url, "URL must not be null");
        this.url = url;
    }


    public void include(RenderRequest request, RenderResponse response) throws PortletException, IOException {
        include((PortletRequest) request, (PortletResponse) response);
    }

    public void include(PortletRequest request, PortletResponse response) throws PortletException, IOException {
        Assert.notNull(request, "Request must not be null");
        Assert.notNull(response, "Response must not be null");
        if (!(response instanceof MockMimeResponse)) {
            throw new IllegalArgumentException("MockPortletRequestDispatcher requires MockMimeResponse");
        }
        ((MockMimeResponse) response).setIncludedUrl(this.url);
        if (logger.isDebugEnabled()) {
            logger.debug("MockPortletRequestDispatcher: including URL [" + this.url + "]");
        }
    }

    public void forward(PortletRequest request, PortletResponse response) throws PortletException, IOException {
        Assert.notNull(request, "Request must not be null");
        Assert.notNull(response, "Response must not be null");
        if (!(response instanceof MockMimeResponse)) {
            throw new IllegalArgumentException("MockPortletRequestDispatcher requires MockMimeResponse");
        }
        ((MockMimeResponse) response).setForwardedUrl(this.url);
        if (logger.isDebugEnabled()) {
            logger.debug("MockPortletRequestDispatcher: forwarding to URL [" + this.url + "]");
        }
    }

}
