package org.springframework.test.web.servlet;

import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.ServletContext;

/**
 * Builds a {@link MockHttpServletRequest}.
 * <p>
 * <p>See static, factory methods in
 * {@code org.springframework.test.web.server.request.MockMvcRequestBuilders}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public interface RequestBuilder {

    /**
     * Build the request.
     *
     * @param servletContext the {@link ServletContext} to use to create the request
     * @return the request
     */
    MockHttpServletRequest buildRequest(ServletContext servletContext);

}
