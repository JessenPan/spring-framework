/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.web.servlet;

import org.springframework.web.servlet.support.RequestContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;

/**
 * //pattern 策略模式
 * 基于web请求的local解析接口.基于策略模式接口.
 * <p>
 * 用来通过httpServletReq获取locale信息以及通过httpServletRequest和httpServletResponse修改locale信息
 * <p>
 * 此接口的实现可以基于请求头，session或者cookies等。
 * 默认实现是{@link org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver},
 * 简单的使用http头来实现
 * <p>
 * 使用{@link RequestContext#getLocale()}获取在controllers和views中使用的locale,
 * 获取到的locale和具体使用的解析策略已经解耦
 *
 * @author Juergen Hoeller
 * @since 27.02.2003
 */
public interface LocaleResolver {

    /**
     * 通过指定的httpRequest获取当前的locale.
     * 可能返回一个默认的locale作为降级策略
     *
     * @param request 被用来解析locale的http请求
     * @return 当前请求的locale, 从不会返回null
     */
    Locale resolveLocale(HttpServletRequest request);

    /**
     * 把httpResponse,httpRequest的locale设置为指定的locale.
     * //tips 未被签名指出的runtimeException最好能在注释中体现
     *
     * @param request  用来被修改locale的http请求
     * @param response 用来被修改locale的http返回
     * @param locale   用来设置值得新locale.如果为null，则清除locale
     * @throws UnsupportedOperationException 如果不支持此操作，抛出此异常
     */
    void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale);

}
