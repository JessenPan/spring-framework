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

package org.springframework.web.servlet.i18n;

import org.springframework.web.servlet.LocaleResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;

/**
 * 此类使用http请求头的accept-language来获取locale
 * <p>
 * 此类不支持设置locale,因为accept-language是客户端的操作系统本身设置，无法被改变
 *
 * @author Juergen Hoeller
 * @see javax.servlet.http.HttpServletRequest#getLocale()
 * @since 27.02.2003
 */
public class AcceptHeaderLocaleResolver implements LocaleResolver {

    public Locale resolveLocale(HttpServletRequest request) {
        return request.getLocale();
    }

    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        throw new UnsupportedOperationException("Cannot change HTTP accept header - use a different locale resolution strategy");
    }

}
