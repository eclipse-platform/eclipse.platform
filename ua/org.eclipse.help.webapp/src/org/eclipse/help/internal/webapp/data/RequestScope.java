/*******************************************************************************
 * Copyright (c) 2010, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.help.internal.webapp.data;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.help.HelpSystem;
import org.eclipse.help.base.AbstractHelpScope;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.base.HelpBaseResources;
import org.eclipse.help.internal.base.IHelpBaseConstants;
import org.eclipse.help.internal.base.scope.EnablementScope;
import org.eclipse.help.internal.base.scope.FilterScope;
import org.eclipse.help.internal.base.scope.IntersectionScope;
import org.eclipse.help.internal.base.scope.ScopeRegistry;
import org.eclipse.help.internal.base.scope.UniversalScope;
import org.eclipse.help.internal.base.scope.WorkingSetScope;
import org.eclipse.help.internal.util.ProductPreferences;
import org.eclipse.help.internal.util.URLCoder;
import org.eclipse.help.internal.webapp.servlet.CookieUtil;
import org.eclipse.help.internal.webapp.servlet.WebappWorkingSetManager;
import org.osgi.service.prefs.BackingStoreException;

public class RequestScope {

	private static final String SCOPE_PARAMETER_NAME = "scope"; //$NON-NLS-1$
	private static final String SCOPE_COOKIE_NAME = "filter"; //$NON-NLS-1$

	/**
	 * Gets a scope object based upon the preferences and request
	 * @param isSearchFilter is true if this filter will be used to filter search results.
	 * Search results are already filtered by search scope and if this parameter is true search
	 * scopes will not be considered
	 * @return
	 */
	public static AbstractHelpScope getScope(HttpServletRequest req, HttpServletResponse resp, boolean isSearchFilter ) {
		AbstractHelpScope[] scopeArray;
		scopeArray = getActiveScopes(req, resp, isSearchFilter);
		switch (scopeArray.length) {
		case 0:
			return new UniversalScope();
		case 1:
			return scopeArray[0];
		default:
			return new IntersectionScope(
					scopeArray);
		}
	}

	public static AbstractHelpScope[] getActiveScopes(HttpServletRequest req,
			HttpServletResponse resp, boolean isSearchFilter) {
		AbstractHelpScope[] scopeArray;
		String scopeString;
		List<AbstractHelpScope> scopes = new ArrayList<>();
		if (ProductPreferences.useEnablementFilters()) {
			scopes.add(new FilterScope()); // Workbench is always filtered, infocenter may be
		}
		scopeString = getScopeString(req);
		if (scopeString != null) {
			AbstractHelpScope scope = ScopeRegistry.getInstance().parseScopePhrase(scopeString);
			if (scope != null) {
				scopes.add(scope);
			}
		}
		// If not in infocenter mode test whether disabled topics should be displayed
		if (!HelpSystem.isShared() && HelpBasePlugin.getActivitySupport().isFilteringEnabled()) {
			scopes.add(new EnablementScope());
		}
		// Add filter by search scope if not called from Help View
		boolean isHelpViewTopic = "/ntopic".equals(req.getServletPath()); //$NON-NLS-1$
		if (!isSearchFilter  && !isHelpViewTopic) {
			// Try for a working set
			try {
				WebappWorkingSetManager manager = new WebappWorkingSetManager(req,
						resp, UrlUtil.getLocale(req, resp));
				String wset = manager.getCurrentWorkingSet();
				if (wset != null && wset.length() > 0) {
					WorkingSetScope workingSetScope = new WorkingSetScope(wset,
							manager, HelpBaseResources.SearchScopeFilterName);
					scopes.add(workingSetScope);
				}
			} catch (Exception e) {
			}
		}
		scopeArray = scopes.toArray(new AbstractHelpScope[scopes.size()]);
		return scopeArray;
	}

	private static String getScopeString(HttpServletRequest req) {
		String scopeString;
		if (HelpSystem.isShared()) {
			scopeString = getScopeFromCookies(req);
		} else {
			scopeString = getScopeFromPreferences();
			if (scopeString == null) {
				scopeString = ScopeRegistry.ENABLEMENT_SCOPE_ID;
			}
		}
		return scopeString;
	}

	public static void setScopeFromRequest(HttpServletRequest request, HttpServletResponse response) {
		// See if there is a scope parameter, if so save as cookie or preference
		String[] phrases = request.getParameterValues(SCOPE_PARAMETER_NAME);
		String scopeStr = ""; //$NON-NLS-1$
		if (phrases!=null){
//			AbstractHelpScope scope = ScopeRegistry.getInstance().parseScopePhrases(phrases);
			for (int p=0;p<phrases.length;p++)
			{
				if (!(phrases[p].startsWith("(") && !phrases[p].startsWith("("))) //$NON-NLS-1$ //$NON-NLS-2$
					phrases[p] = '('+phrases[p]+')';
				scopeStr+=phrases[p];
				if (p<phrases.length-1)
					scopeStr+=ScopeRegistry.SCOPE_AND;
			}
		}
		CookieUtil.deleteObsoleteCookies(request, response);
		saveScope(scopeStr, request, response);
	}

	public static void saveScope(String scope, HttpServletRequest request, HttpServletResponse response) {
		if (HelpSystem.isShared()) {
			if (response != null) {
				CookieUtil.setCookieValue(SCOPE_COOKIE_NAME, URLCoder.compactEncode(scope), request, response);
			}
		} else {
			IEclipsePreferences pref = InstanceScope.INSTANCE.getNode(HelpBasePlugin.PLUGIN_ID);
			pref.put(IHelpBaseConstants.P_KEY_HELP_SCOPE, scope);
			try {
				pref.flush();
			} catch (BackingStoreException e) {
			}
		}
	}

	private static String getScopeFromCookies(HttpServletRequest request) {
		return getValueFromCookies(request, SCOPE_COOKIE_NAME);
	}

	private static String getValueFromCookies(HttpServletRequest request, String cookieName) {
		// check if scope was passed earlier in this session
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (cookieName.equals(cookie.getName())) {
					return URLCoder.decode(cookie.getValue());
				}
			}
		}
		return null;
	}

	private static String getScopeFromPreferences() {
		String scope = Platform.getPreferencesService().getString
			 (HelpBasePlugin.PLUGIN_ID, IHelpBaseConstants.P_KEY_HELP_SCOPE, null, null);
		return scope;
	}

	public static boolean filterBySearchScope(HttpServletRequest request) {
		return true;
	}

	public static boolean getFlag(HttpServletRequest request, String flagName ) {
		String value;
		if (HelpSystem.isShared()) {
			value = getValueFromCookies(request, flagName);
		} else {
			value = Platform.getPreferencesService().getString
					(HelpBasePlugin.PLUGIN_ID, flagName + "Webapp", null, null); //$NON-NLS-1$
		}
		if (value == null) {
			return Platform.getPreferencesService().getBoolean
					(HelpBasePlugin.PLUGIN_ID, flagName, false, null);
		}
		return "true".equalsIgnoreCase(value); //$NON-NLS-1$
	}

	public static void setFlag(HttpServletRequest request,
							   HttpServletResponse response,
							   String flagName,
							   boolean value)
	{
		if (HelpSystem.isShared()) {
			CookieUtil.setCookieValueWithoutPath(flagName, Boolean.toString(value), request, response);
		} else {
			IEclipsePreferences pref = InstanceScope.INSTANCE.getNode(HelpBasePlugin.PLUGIN_ID);
			pref.putBoolean(flagName  + "Webapp", value ); //$NON-NLS-1$
			try {
				pref.flush();
			} catch (BackingStoreException e) {
			}
		}
	}

}
