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

package org.springframework.web.servlet.i18n;

import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;

/**
 * 在用户session中存储locale值，如果没有值，使用http头中值作为降级策略
 * <p>
 * 自定义的controller能够覆盖原来的locale，通过调用{@link SessionLocaleResolver#setLocale(HttpServletRequest, HttpServletResponse, Locale)}
 * 方法即可
 *
 * @author Juergen Hoeller
 * @see #setDefaultLocale
 * @see #setLocale
 * @since 27.02.2003
 */
public class SessionLocaleResolver extends AbstractLocaleResolver {

    /**
     * 在session中保持locale的属性key
     *
     * @see org.springframework.web.servlet.support.RequestContext#getLocale
     * @see org.springframework.web.servlet.support.RequestContextUtils#getLocale
     */
    //TODO 修改访问级别
    public static final String LOCALE_SESSION_ATTRIBUTE_NAME = SessionLocaleResolver.class.getName() + ".LOCALE";


    public Locale resolveLocale(HttpServletRequest request) {
        //获取session中保持的属性值
        Locale locale = (Locale) WebUtils.getSessionAttribute(request, LOCALE_SESSION_ATTRIBUTE_NAME);
        if (locale == null) {
            locale = determineDefaultLocale(request);
        }
        return locale;
    }

    /**
     * 先获取手动设置的默认locale，如无值，返回http请求头中的值
     *
     * @param request 解析默认locale的请求
     * @return 默认locale，不会返回null
     * @see #setDefaultLocale
     * @see javax.servlet.http.HttpServletRequest#getLocale()
     */
    protected Locale determineDefaultLocale(HttpServletRequest request) {
        Locale defaultLocale = getDefaultLocale();
        if (defaultLocale == null) {
            defaultLocale = request.getLocale();
        }
        return defaultLocale;
    }

    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        WebUtils.setSessionAttribute(request, LOCALE_SESSION_ATTRIBUTE_NAME, locale);
    }

}
