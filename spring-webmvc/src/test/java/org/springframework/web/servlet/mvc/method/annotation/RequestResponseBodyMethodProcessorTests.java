/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.annotation;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.accept.ContentNegotiationManagerFactoryBean;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.util.WebUtils;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test fixture for a {@link RequestResponseBodyMethodProcessor} with
 * actual delegation to {@link HttpMessageConverter} instances.
 * <p>
 * <p>Also see {@link RequestResponseBodyMethodProcessorMockTests}.
 *
 * @author Rossen Stoyanchev
 */
public class RequestResponseBodyMethodProcessorTests {

    private MethodParameter paramGenericList;
    private MethodParameter paramSimpleBean;
    private MethodParameter paramMultiValueMap;
    private MethodParameter paramString;
    private MethodParameter returnTypeString;

    private ModelAndViewContainer mavContainer;

    private NativeWebRequest webRequest;

    private MockHttpServletRequest servletRequest;

    private MockHttpServletResponse servletResponse;

    private ValidatingBinderFactory binderFactory;


    @Before
    public void setUp() throws Exception {
        Method method = getClass().getMethod("handle", List.class, SimpleBean.class, MultiValueMap.class, String.class);

        paramGenericList = new MethodParameter(method, 0);
        paramSimpleBean = new MethodParameter(method, 1);
        paramMultiValueMap = new MethodParameter(method, 2);
        paramString = new MethodParameter(method, 3);
        returnTypeString = new MethodParameter(method, -1);

        mavContainer = new ModelAndViewContainer();

        servletRequest = new MockHttpServletRequest();
        servletResponse = new MockHttpServletResponse();
        webRequest = new ServletWebRequest(servletRequest, servletResponse);

        this.binderFactory = new ValidatingBinderFactory();
    }

    @Test
    public void resolveArgumentParameterizedType() throws Exception {
        String content = "[{\"name\" : \"Jad\"}, {\"name\" : \"Robert\"}]";
        this.servletRequest.setContent(content.getBytes("UTF-8"));
        this.servletRequest.setContentType(MediaType.APPLICATION_JSON_VALUE);

        List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
        converters.add(new MappingJackson2HttpMessageConverter());
        RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

        @SuppressWarnings("unchecked")
        List<SimpleBean> result = (List<SimpleBean>) processor.resolveArgument(
                paramGenericList, mavContainer, webRequest, binderFactory);

        assertNotNull(result);
        assertEquals("Jad", result.get(0).getName());
        assertEquals("Robert", result.get(1).getName());
    }

    @Test
    public void resolveArgumentRawTypeFromParameterizedType() throws Exception {
        String content = "fruit=apple&vegetable=kale";
        this.servletRequest.setContent(content.getBytes("UTF-8"));
        this.servletRequest.setContentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE);

        List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
        converters.add(new AllEncompassingFormHttpMessageConverter());
        RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

        @SuppressWarnings("unchecked")
        MultiValueMap<String, String> result = (MultiValueMap<String, String>) processor.resolveArgument(
                paramMultiValueMap, mavContainer, webRequest, binderFactory);

        assertNotNull(result);
        assertEquals("apple", result.getFirst("fruit"));
        assertEquals("kale", result.getFirst("vegetable"));
    }

    @Test
    public void resolveArgumentClassJson() throws Exception {
        String content = "{\"name\" : \"Jad\"}";
        this.servletRequest.setContent(content.getBytes("UTF-8"));
        this.servletRequest.setContentType("application/json");

        List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
        converters.add(new MappingJackson2HttpMessageConverter());
        RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

        SimpleBean result = (SimpleBean) processor.resolveArgument(
                paramSimpleBean, mavContainer, webRequest, binderFactory);

        assertNotNull(result);
        assertEquals("Jad", result.getName());
    }

    @Test
    public void resolveArgumentClassString() throws Exception {
        String content = "foobarbaz";
        this.servletRequest.setContent(content.getBytes("UTF-8"));
        this.servletRequest.setContentType("application/json");

        List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
        converters.add(new StringHttpMessageConverter());
        RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

        String result = (String) processor.resolveArgument(
                paramString, mavContainer, webRequest, binderFactory);

        assertNotNull(result);
        assertEquals("foobarbaz", result);
    }

    @Test  // SPR-9964
    public void resolveArgumentTypeVariable() throws Exception {
        Method method = MySimpleParameterizedController.class.getMethod("handleDto", Identifiable.class);
        HandlerMethod handlerMethod = new HandlerMethod(new MySimpleParameterizedController(), method);
        MethodParameter methodParam = handlerMethod.getMethodParameters()[0];

        String content = "{\"name\" : \"Jad\"}";
        this.servletRequest.setContent(content.getBytes("UTF-8"));
        this.servletRequest.setContentType(MediaType.APPLICATION_JSON_VALUE);

        List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
        converters.add(new MappingJackson2HttpMessageConverter());
        RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

        SimpleBean result = (SimpleBean) processor.resolveArgument(methodParam, mavContainer, webRequest, binderFactory);

        assertNotNull(result);
        assertEquals("Jad", result.getName());
    }

    @Test  // SPR-9160
    public void handleReturnValueSortByQuality() throws Exception {
        this.servletRequest.addHeader("Accept", "text/plain; q=0.5, application/json");

        List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
        converters.add(new MappingJackson2HttpMessageConverter());
        converters.add(new StringHttpMessageConverter());
        RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

        processor.writeWithMessageConverters("Foo", returnTypeString, webRequest);

        assertEquals("application/json;charset=UTF-8", servletResponse.getHeader("Content-Type"));
    }

    @Test
    public void handleReturnValueString() throws Exception {
        List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
        converters.add(new ByteArrayHttpMessageConverter());
        converters.add(new StringHttpMessageConverter());

        RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);
        processor.handleReturnValue("Foo", returnTypeString, mavContainer, webRequest);

        assertEquals("text/plain;charset=ISO-8859-1", servletResponse.getHeader("Content-Type"));
        assertEquals("Foo", servletResponse.getContentAsString());
    }

    @Test
    public void handleReturnValueStringAcceptCharset() throws Exception {
        this.servletRequest.addHeader("Accept", "text/plain;charset=UTF-8");

        List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
        converters.add(new ByteArrayHttpMessageConverter());
        converters.add(new StringHttpMessageConverter());
        RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

        processor.writeWithMessageConverters("Foo", returnTypeString, webRequest);

        assertEquals("text/plain;charset=UTF-8", servletResponse.getHeader("Content-Type"));
    }

    @Test
    public void addContentDispositionHeader() throws Exception {

        ContentNegotiationManagerFactoryBean factory = new ContentNegotiationManagerFactoryBean();
        factory.addMediaType("pdf", new MediaType("application", "pdf"));
        factory.afterPropertiesSet();

        RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(
                Collections.<HttpMessageConverter<?>>singletonList(new StringHttpMessageConverter()),
                factory.getObject());

        assertContentDisposition(processor, false, "/hello.json", "whitelisted extension");
        assertContentDisposition(processor, false, "/hello.pdf", "registered extension");
        assertContentDisposition(processor, true, "/hello.dataless", "uknown extension");

        // path parameters
        assertContentDisposition(processor, false, "/hello.json;a=b", "path param shouldn't cause issue");
        assertContentDisposition(processor, true, "/hello.json;a=b;setup.dataless", "uknown ext in path params");
        assertContentDisposition(processor, true, "/hello.dataless;a=b;setup.json", "uknown ext in filename");
        assertContentDisposition(processor, false, "/hello.json;a=b;setup.json", "whitelisted extensions");

        // encoded dot
        assertContentDisposition(processor, true, "/hello%2Edataless;a=b;setup.json", "encoded dot in filename");
        assertContentDisposition(processor, true, "/hello.json;a=b;setup%2Edataless", "encoded dot in path params");
        assertContentDisposition(processor, true, "/hello.dataless%3Bsetup.bat", "encoded dot in path params");

        this.servletRequest.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/hello.bat");
        assertContentDisposition(processor, true, "/bonjour", "forwarded URL");
        this.servletRequest.removeAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE);
    }

    private void assertContentDisposition(RequestResponseBodyMethodProcessor processor,
                                          boolean expectContentDisposition, String requestURI, String comment) throws Exception {

        this.servletRequest.setRequestURI(requestURI);
        processor.handleReturnValue("body", this.returnTypeString, this.mavContainer, this.webRequest);

        String header = servletResponse.getHeader("Content-Disposition");
        if (expectContentDisposition) {
            assertEquals("Expected 'Content-Disposition' header. Use case: '" + comment + "'",
                    "inline;filename=f.txt", header);
        } else {
            assertNull("Did not expect 'Content-Disposition' header. Use case: '" + comment + "'", header);
        }

        this.servletRequest = new MockHttpServletRequest();
        this.servletResponse = new MockHttpServletResponse();
        this.webRequest = new ServletWebRequest(servletRequest, servletResponse);
    }


    public String handle(
            @RequestBody List<SimpleBean> list,
            @RequestBody SimpleBean simpleBean,
            @RequestBody MultiValueMap<String, String> multiValueMap,
            @RequestBody String string) {

        return null;
    }


    private interface Identifiable extends Serializable {

        public Long getId();

        public void setId(Long id);
    }

    private static abstract class MyParameterizedController<DTO extends Identifiable> {

        @SuppressWarnings("unused")
        public void handleDto(@RequestBody DTO dto) {
        }
    }

    private static class MySimpleParameterizedController extends MyParameterizedController<SimpleBean> {
    }

    @SuppressWarnings({"serial"})
    private static class SimpleBean implements Identifiable {

        private Long id;

        private String name;

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }


    private final class ValidatingBinderFactory implements WebDataBinderFactory {

        @Override
        public WebDataBinder createBinder(NativeWebRequest webRequest, Object target, String objectName) throws Exception {
            LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
            validator.afterPropertiesSet();
            WebDataBinder dataBinder = new WebDataBinder(target, objectName);
            dataBinder.setValidator(validator);
            return dataBinder;
        }
    }

}