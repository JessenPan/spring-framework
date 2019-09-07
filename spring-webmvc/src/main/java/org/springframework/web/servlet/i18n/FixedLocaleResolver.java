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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;

/**
 * 此实现类总是返回一个固定的默认locale值.默认是当前JVM的默认locale
 * <p>
 * 注意：不支持{@link org.springframework.web.servlet.LocaleResolver#setLocale(HttpServletRequest, HttpServletResponse, Locale)}方法
 * ,因为固定的locale不能被修改
 *
 * @author Juergen Hoeller
 * @see #setDefaultLocale
 * @since 1.1
 */
public class FixedLocaleResolver extends AbstractLocaleResolver {

    /**
     * 创建一个实例，同时设置固定的locale为jvm默认地区值
     *
     * @see #setDefaultLocale
     */
    public FixedLocaleResolver() {
    }

    /**
     * 创建一个固定的locale，设置locale为指定locale
     *
     * @param locale 指定的固定locale
     */
    public FixedLocaleResolver(Locale locale) {
        setDefaultLocale(locale);
    }


    public Locale resolveLocale(HttpServletRequest request) {
        Locale locale = getDefaultLocale();
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return locale;
    }

    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        throw new UnsupportedOperationException("Cannot change fixed locale - use a different locale resolution strategy");
    }

}
