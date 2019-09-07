/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.web.portlet.mvc;

import org.springframework.web.portlet.HandlerAdapter;
import org.springframework.web.portlet.ModelAndView;
import org.springframework.web.portlet.context.PortletContextAware;
import org.springframework.web.portlet.util.PortletUtils;

import javax.portlet.*;

/**
 * Adapter to use the Controller workflow interface with the generic DispatcherPortlet.
 * <p>
 * <p>This is an SPI class, not used directly by application code.
 *
 * @author Juergen Hoeller
 * @author John A. Lewis
 * @see org.springframework.web.portlet.DispatcherPortlet
 * @see Controller
 * @see ResourceAwareController
 * @see EventAwareController
 * @since 2.0
 */
public class SimpleControllerHandlerAdapter implements HandlerAdapter, PortletContextAware {

    private PortletContext portletContext;


    public void setPortletContext(PortletContext portletContext) {
        this.portletContext = portletContext;
    }


    public boolean supports(Object handler) {
        return (handler instanceof Controller);
    }

    public void handleAction(ActionRequest request, ActionResponse response, Object handler)
            throws Exception {

        ((Controller) handler).handleActionRequest(request, response);
    }

    public ModelAndView handleRender(RenderRequest request, RenderResponse response, Object handler)
            throws Exception {

        return ((Controller) handler).handleRenderRequest(request, response);
    }

    public ModelAndView handleResource(ResourceRequest request, ResourceResponse response, Object handler)
            throws Exception {

        if (handler instanceof ResourceAwareController) {
            return ((ResourceAwareController) handler).handleResourceRequest(request, response);
        } else {
            // equivalent to Portlet 2.0 GenericPortlet
            PortletUtils.serveResource(request, response, this.portletContext);
            return null;
        }
    }

    public void handleEvent(EventRequest request, EventResponse response, Object handler) throws Exception {
        if (handler instanceof EventAwareController) {
            ((EventAwareController) handler).handleEventRequest(request, response);
        } else {
            // if no event processing method was found just keep render params
            response.setRenderParameters(request);
        }
    }

}
