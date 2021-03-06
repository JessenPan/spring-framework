/*
 * Copyright 2002-2007 the original author or authors.
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

import java.util.Locale;

/**
 * 接口{@link LocaleResolver}的抽象实现类.
 * 提供设置默认locale的功能
 *
 * @author Juergen Hoeller
 * @since 1.2.9
 */
public abstract class AbstractLocaleResolver implements LocaleResolver {

    //默认的locale
    private Locale defaultLocale;

    /**
     * 返回降级用的默认locale值
     */
    protected Locale getDefaultLocale() {
        return this.defaultLocale;
    }

    /**
     * 设置降级使用的locale,即默认locale
     */
    public void setDefaultLocale(Locale defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

}
