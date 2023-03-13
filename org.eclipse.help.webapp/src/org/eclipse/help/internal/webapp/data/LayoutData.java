/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
 *     George Suaridze <suag@1c.ru> (1C-Soft LLC) - Bug 560168
 *******************************************************************************/
package org.eclipse.help.internal.webapp.data;

import java.util.ArrayList;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.base.MissingContentManager;
import org.eclipse.help.internal.base.remote.RemoteStatusData;
import org.eclipse.help.internal.webapp.HelpWebappPlugin;
import org.eclipse.help.webapp.AbstractView;
import org.eclipse.osgi.service.localization.BundleLocalization;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

public class LayoutData extends RequestData {

	private static final String VIEW_EXTENSION_POINT = "org.eclipse.help.webapp.view"; //$NON-NLS-1$
	private String query = ""; //$NON-NLS-1$
	private AbstractView[] views;

	public LayoutData(ServletContext context, HttpServletRequest request,
			HttpServletResponse response) {
		super(context, request, response);

		// initialize the query string
		String qs = request.getQueryString();
		// Remove any live help token
		if (qs != null) {
			qs = qs.replaceFirst("^token=[a-z0-9-]{36}", ""); //$NON-NLS-1$ //$NON-NLS-2$
			qs = qs.replaceFirst("&token=[a-z0-9-]{36}", ""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (qs != null && qs.length() > 0)
			query = "?" + qs; //$NON-NLS-1$
	}

	public String getQuery() {
		return query;
	}

	public String getBannerURL() {
		String banner = preferences.getBanner();
		if (banner == null || banner.trim().length() == 0) {
			banner = "about:blank"; //$NON-NLS-1$
		} else if (banner.startsWith("http:/") || banner.startsWith("https:/")) { //$NON-NLS-1$ //$NON-NLS-2$
		} else if (banner.startsWith("file:/") || banner.startsWith("jar:file:/")) { //$NON-NLS-1$ //$NON-NLS-2$
			banner = "topic/" + banner; //$NON-NLS-1$
		} else {
			banner = "topic" + banner; //$NON-NLS-1$
		}
		return banner;
	}

	public String getBannerHeight() {
		String height = preferences.getBannerHeight();
		if (height == null || height.length() == 0) {
			height = "0"; //$NON-NLS-1$
		}
		return height;
	}

	public String getFooterURL() {
		String footer = preferences.getFooter();
		if (footer == null || footer.trim().length() == 0) {
			footer = "about:blank"; //$NON-NLS-1$
		} else if (footer.startsWith("http:/") || footer.startsWith("https:/")) { //$NON-NLS-1$ //$NON-NLS-2$
		} else if (footer.startsWith("file:/") || footer.startsWith("jar:file:/")) { //$NON-NLS-1$ //$NON-NLS-2$
			footer = "topic/" + footer; //$NON-NLS-1$
		} else {
			footer = "topic" + footer; //$NON-NLS-1$
		}
		return footer;
	}

	public String getFooterHeight() {
		String height = preferences.getFooterHeight();
		if (height == null || height.length() == 0) {
			height = "0"; //$NON-NLS-1$
		}
		return height;
	}

	/**
	 * @return the text to be added to the rows attribute of the frameset
	 */
	public String getFooterRowText() {
		String height = getFooterHeight();
		if ("0".equals(height)) { //$NON-NLS-1$
			return ""; //$NON-NLS-1$
		}
		return "," + height; //$NON-NLS-1$
	}

	public String getContentURL() {
		String navHref = request.getParameter("nav"); //$NON-NLS-1$
		if (navHref != null) {
			return "../nav" + navHref; //$NON-NLS-1$
		}
		String topicHref = request.getParameter("topic"); //$NON-NLS-1$
		if (topicHref == null || topicHref.length() == 0) {
			if (BaseHelpSystem.getMode()!=BaseHelpSystem.MODE_INFOCENTER && RemoteStatusData.isAnyRemoteHelpUnavailable()) {
				return "../topic/"+HelpWebappPlugin.PLUGIN_ID+'/'+MissingContentManager.REMOTE_STATUS_HREF; //$NON-NLS-1$
			}
			if (MissingContentManager.getInstance().isUnresolvedPlaceholders()) {
			String helpMissingPage = MissingContentManager.getInstance().getHelpMissingPage(false);
			if (helpMissingPage != null) {
				return "../topic" + helpMissingPage; //$NON-NLS-1$
			}
			}
			return UrlUtil.getHelpURL(preferences.getHelpHome());
		}
		else {
			TocData tocData = new TocData(context, request, response);
			String topic = tocData.getSelectedTopicWithPath();
			if (topic == null || !UrlUtil.isValidTopicParamOrWasOpenedFromHelpDisplay(topic)) {
				return UrlUtil.getHelpURL(preferences.getHelpHome());
			}
			return  topic;
		}
	}

	/**
	 * Return array of length 0 if no views
	 */
	public AbstractView[] getViews() {
		if (views != null)
			return views;

		View tocview = new View("toc", //$NON-NLS-1$
				"", //$NON-NLS-1$
				preferences.getImagesDirectory() + "/contents_view.svg", 'C', !HelpPlugin.getTocManager().isTocLoaded(getLocale())); //$NON-NLS-1$
		View indexview = null;
		View searchview = new View("search", //$NON-NLS-1$
				"", //$NON-NLS-1$
				preferences.getImagesDirectory() + "/search_results_view.svg", 'R', false); //$NON-NLS-1$
		View bookmarksview = null;

		if (preferences.isIndexView())
			indexview = new View("index", //$NON-NLS-1$
					"", //$NON-NLS-1$
					preferences.getImagesDirectory() + "/index_view.svg", 'I', false); //$NON-NLS-1$
		if (preferences.isBookmarksView())
			bookmarksview = new View("bookmarks", //$NON-NLS-1$
					"", //$NON-NLS-1$
					preferences.getImagesDirectory() + "/bookmarks_view.svg", (char)0, false); //$NON-NLS-1$

		ArrayList<AbstractView> viewList = new ArrayList<>();
		viewList.add(tocview);
		if (indexview != null) {
			viewList.add(indexview);
		}
		viewList.add(searchview);
		if (bookmarksview !=null) {
			viewList.add(bookmarksview);
		}

		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] elements = registry
				.getConfigurationElementsFor(VIEW_EXTENSION_POINT);
		for (IConfigurationElement element : elements) {
			Object obj = null;
			try {
				obj = element.createExecutableExtension("class"); //$NON-NLS-1$
			} catch (CoreException e) {
				Platform.getLog(getClass()).error("Create extension failed:[" //$NON-NLS-1$
						+ VIEW_EXTENSION_POINT + "].", e); //$NON-NLS-1$
			}
			if (obj instanceof AbstractView view) {
				viewList.add(view);
			}
		}

		views = viewList.toArray(new AbstractView[viewList.size()]);
		return views;
	}

	public String getVisibleView() {
		String requestedView = request.getParameter("tab"); //$NON-NLS-1$
		AbstractView[] allViews = getViews();
		for (AbstractView allView : allViews) {
			if (allView.getName().equals(requestedView)) {
				return requestedView;
			}
		}
		return "toc"; //$NON-NLS-1$
	}

	public AbstractView getCurrentView() {
		String name = request.getParameter("view"); //$NON-NLS-1$
		views = getViews();
		for (AbstractView view : views)
			if (view.getName().equals(name))
				return view;
		return null;
	}

	public String getWindowTitle() {
		String titlePref = preferences.getTitleResource();
		int slash = titlePref.indexOf('/');
		if (slash > 0) {
			String resourceContainer = titlePref.substring(0, slash);
			String resource = titlePref.substring(slash + 1);
			try {
				Bundle bundle = Platform.getBundle(resourceContainer);

				BundleContext bundleContext = FrameworkUtil.getBundle(LayoutData.class).getBundleContext();
				ServiceReference<?> ref = bundleContext.getServiceReference(BundleLocalization.class.getName());
				BundleLocalization localization = (BundleLocalization) bundleContext.getService(ref);
				return localization.getLocalization(bundle, locale).getString(resource);
			} catch (Exception e) {
				// Fall through
			}
		}

		if (preferences.isWindowTitlePrefix()) {
			return ServletResources.getString("browserTitle", //$NON-NLS-1$
					BaseHelpSystem.getProductName(), request);
		}
		return BaseHelpSystem.getProductName();
	}

	/**
	 * Returns the URL of a JSP file in the advanced presentation
	 */
	public String getAdvancedURL(AbstractView view, String fileSuffix) {
		return createURL(view.getURL(), view.getName(), fileSuffix);
	}

	private String createURL(String path, String viewName, String fileSuffix) {
		if (path == null || path.length() == 0) {
			return viewName + fileSuffix;
		}
		if (path.charAt(path.length() -1) != '/') {
			path = path + '/';
		}
		return request.getContextPath() + path + viewName + fileSuffix;
	}

	public String getImageURL(AbstractView view) {
		String filename = view.getImageURL();
		if (filename.length() != 0 && filename.charAt(0) == '/') {
			return request.getContextPath() + filename;
		}
		return filename;
	}

	public String getTitle(AbstractView view) {
		return view.getTitle(UrlUtil.getLocaleObj(request, null));
	}
}
