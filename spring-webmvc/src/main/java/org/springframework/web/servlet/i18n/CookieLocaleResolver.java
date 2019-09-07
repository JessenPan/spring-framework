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

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.util.CookieGenerator;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;

/**
 * 使用发送到客户端的cookie里的locale值
 * <p>
 * 使用cookie解析locale的实现在没有用户session的会话特别有用.
 * <p>
 * 自定义controller可以调用{@link #setLocale(HttpServletRequest, HttpServletResponse, Locale)}
 * 来修改locale
 *
 * @author Juergen Hoeller
 * @author Jean-Pierre Pawlak
 * @see #setDefaultLocale
 * @see #setLocale
 * @since 27.02.2003
 */
public class CookieLocaleResolver extends AbstractLocaleResolver implements LocaleResolver {

    /**
     * 在httpRequest的属性map中保持当前locale的key
     * <p>
     * 如果在当前请求中locale已经被修改了,那么此key则被用来重写cookie值
     *
     * @see org.springframework.web.servlet.support.RequestContext#getLocale
     */
    public static final String LOCALE_REQUEST_ATTRIBUTE_NAME = CookieLocaleResolver.class.getName() + ".LOCALE";

    /**
     * 在没有设置的情况下，默认的cookie名称
     */
    //TODO 修改访问级别
    public static final String DEFAULT_COOKIE_NAME = CookieLocaleResolver.class.getName() + ".LOCALE";

    private CookieGenerator cookieGenerator;

    public CookieLocaleResolver() {
        cookieGenerator = new CookieGenerator();
        cookieGenerator.setCookieName(DEFAULT_COOKIE_NAME);
    }

    public CookieLocaleResolver(CookieGenerator cookieGenerator) {
        Assert.notNull(cookieGenerator);
        this.cookieGenerator = cookieGenerator;
        if (StringUtils.isEmpty(cookieGenerator.getCookieName())) {
            cookieGenerator.setCookieName(DEFAULT_COOKIE_NAME);
        }
    }


    public Locale resolveLocale(HttpServletRequest request) {
        //在http属性中是否已经有了被解析过的locale
        Locale locale = (Locale) request.getAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME);
        if (locale != null) {
            return locale;
        }

        //去除locale对应的cookie
        Cookie cookie = WebUtils.getCookie(request, cookieGenerator.getCookieName());
        if (cookie != null) {
            //把cookie值解析为对应的locale对象
            locale = StringUtils.parseLocaleString(cookie.getValue());
            if (locale != null) {
                request.setAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME, locale);
                return locale;
            }
        }

        return determineDefaultLocale(request);
    }

    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        if (locale != null) {
            //设置新的locale值 
            request.setAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME, locale);
            cookieGenerator.addCookie(response, locale.toString());
        } else {
            //删除已经存在的locale
            request.setAttribute(LOCALE_REQUEST_ATTRIBUTE_NAME, determineDefaultLocale(request));
            cookieGenerator.removeCookie(response);
        }
    }

    /**
     * Determine the default locale for the given request,
     * 断定指定request对应的默认locale.
     * <p>
     * 默认实现先会获取设置的默认locale，如果没有设置，则返回http请求头中的值
     *
     * @param request 获取默认locale的httpRequest
     * @return 默认locale，非null值
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

}
