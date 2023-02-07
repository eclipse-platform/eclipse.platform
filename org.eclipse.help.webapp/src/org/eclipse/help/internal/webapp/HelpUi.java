/*******************************************************************************
 * Copyright (c) 2021 Holger Voormann and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.help.internal.webapp;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.help.internal.webapp.data.ServletResources;
import org.eclipse.help.internal.webapp.data.UrlUtil;

public class HelpUi {

    private static final Pattern LET_PATTERN = Pattern.compile("<%(string|html|js):([^%>]++)%>"); //$NON-NLS-1$

    private static final String[] KEEP_PLACEHOLDERS = { "{0}", "{1}", "{2}" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

    /**
     * Resolves following kind of placeholders in the given template where {@code property_name} is the name of the
     * property to retrieve from the <a href="WebappResources.properties">{@code WebappResources.properties}</a> file:
     * <ul>
     *   <li>{@code <%string:property_name%>} - (plain) localized string</li>
     *   <li>{@code <%html:property_name%>} - HTML encoded localized string</li>
     *   <li>{@code <%js:property_name%>} - JavaScript encoded localized string</li>
     * </ul>
     * @param template the template containing placeholders to resolve
     * @param request the request object to get the language/locale from to localize Strings
     * @return the input template in which the placeholders were resolved
     */
    public static String resolve(String template, HttpServletRequest request) {
        final Matcher matcher = LET_PATTERN.matcher(template);
        // when template loaded/cached: reset();
        boolean found = matcher.find();
        if (found) {
            StringBuilder result = new StringBuilder();
            do {
                String func = matcher.group(1);
                String param = matcher.group(2);
                if ("string".equals(func)) { //$NON-NLS-1$
                    String resolved = ServletResources.getString(param, KEEP_PLACEHOLDERS, request);
                    append(matcher, result, resolved);
                } else if ("html".equals(func)) { //$NON-NLS-1$
                    String resolved = ServletResources.getString(param, KEEP_PLACEHOLDERS, request);
                    append(matcher, result, UrlUtil.htmlEncode(resolved));
                } else if ("js".equals(func)) { //$NON-NLS-1$
                    String resolved = ServletResources.getString(param, KEEP_PLACEHOLDERS, request);
                    String encoded = UrlUtil.JavaScriptEncode(resolved).replace("\\u0020", " "); //$NON-NLS-1$//$NON-NLS-2$
                    append(matcher, result, encoded);
                } else {
                    matcher.appendReplacement(result, ""); //$NON-NLS-1$
                }
                found = matcher.find();
            } while (found);
            matcher.appendTail(result);
            return result.toString();
        }
        return template;
    }

    private static void append(Matcher matcher, StringBuilder result, String string) {
        matcher.appendReplacement(result, Matcher.quoteReplacement(string));
    }

}
