/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.util.ProductPreferences;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Preferences for webapp
 */
public class WebappPreferences {
	/**
	 * Constructor.
	 */
	public WebappPreferences() {
	}
	/**
	 * @return String - URL of banner page or null
	 */
	public String getBanner() {
		return getPreferenceString("banner"); //$NON-NLS-1$
	}

	public String getBannerHeight() {
		return getPreferenceString("banner_height"); //$NON-NLS-1$
	}

	/**
	 * @return String - URL of footer page or null
	 */
	public String getFooter() {
		return getPreferenceString("footer"); //$NON-NLS-1$
	}

	public String getFooterHeight() {
		return getPreferenceString("footer_height"); //$NON-NLS-1$
	}

	public String getHelpHome() {
		return getPreferenceString("help_home"); //$NON-NLS-1$
	}

	public boolean isIndexView() {
		return ProductPreferences.getBoolean(HelpBasePlugin.getDefault(), "indexView"); //$NON-NLS-1$
	}

	public boolean isBookmarksView() {
		return BaseHelpSystem.getMode() != BaseHelpSystem.MODE_INFOCENTER
				&& ProductPreferences.getBoolean(HelpBasePlugin.getDefault(), "bookmarksView"); //$NON-NLS-1$
	}

	public boolean isBookmarksAction() {
		return ProductPreferences.getBoolean(HelpBasePlugin.getDefault(), "bookmarksView"); //$NON-NLS-1$
	}

	public String getImagesDirectory() {
		String imagesDirectory = getPreferenceString("imagesDirectory"); //$NON-NLS-1$
		if (imagesDirectory != null && imagesDirectory.startsWith("/")) //$NON-NLS-1$
			imagesDirectory = UrlUtil.getHelpURL(imagesDirectory);
		return imagesDirectory;

	}

	public String getToolbarBackground() {
		return getPreferenceString("advanced.toolbarBackground"); //$NON-NLS-1$
	}

	public String getToolbarFont() {
		return getPreferenceString("advanced.toolbarFont"); //$NON-NLS-1$
	}

	public String getViewBackground() {
		return getPreferenceString("advanced.viewBackground"); //$NON-NLS-1$
	}

	public String getViewBackgroundStyle() {
		String viewBackground = getPreferenceString("advanced.viewBackground"); //$NON-NLS-1$
		if (viewBackground == null || viewBackground.length() == 0) {
			return (""); //$NON-NLS-1$
		}
		return "background-color: " + viewBackground + ";";  //$NON-NLS-1$ //$NON-NLS-2$
	}

	public String getViewFont() {
		return getPreferenceString("advanced.viewFont"); //$NON-NLS-1$
	}

	public String getTitleResource() {
		return getPreferenceString("titleResource"); //$NON-NLS-1$
	}

	public String getQuickPrintMaxConnections(){
		return getPreferenceString("maxConnections"); //$NON-NLS-1$
	}

	public String getQuickPrintMaxTopics(){
		return getPreferenceString("maxTopics"); //$NON-NLS-1$
	}

	public boolean isWindowTitlePrefix() {
		return ProductPreferences.getBoolean(HelpBasePlugin.getDefault(), "windowTitlePrefix"); //$NON-NLS-1$
	}
	public boolean isDontConfirmShowAll() {
		return getBooleanPreference("dontConfirmShowAll"); //$NON-NLS-1$
	}
	public void setDontConfirmShowAll(boolean dontconfirm) {
		setBooleanPreference("dontConfirmShowAll", dontconfirm); //$NON-NLS-1$
	}
	public boolean isActiveHelp() {
		return ProductPreferences.getBoolean(HelpBasePlugin.getDefault(), "activeHelp"); //$NON-NLS-1$
	}

	public boolean isIndexInstruction() {
		return ProductPreferences.getBoolean(HelpBasePlugin.getDefault(), "indexInstruction"); //$NON-NLS-1$
	}

	public boolean isIndexButton() {
		return ProductPreferences.getBoolean(HelpBasePlugin.getDefault(), "indexButton"); //$NON-NLS-1$
	}

	public boolean isIndexPlusMinus() {
		return ProductPreferences.getBoolean(HelpBasePlugin.getDefault(), "indexPlusMinus"); //$NON-NLS-1$
	}

	public boolean isIndexExpandAll() {
		return ProductPreferences.getBoolean(HelpBasePlugin.getDefault(), "indexExpandAll"); //$NON-NLS-1$
	}
	public boolean isHighlightDefault() {
		return getBooleanPreference("default_highlight"); //$NON-NLS-1$
	}
	public void setHighlightDefault(boolean highlight) {
		setBooleanPreference("default_highlight", highlight); //$NON-NLS-1$
	}

	public boolean isRestrictTopicParameter() {
		return getBooleanPreference("restrictTopicParameter"); //$NON-NLS-1$
	}

	private String getPreferenceString(String key) {
		return Platform.getPreferencesService().getString(HelpBasePlugin.PLUGIN_ID, key, "", null); //$NON-NLS-1$
	}

	private boolean getBooleanPreference(String key) {
		return Platform.getPreferencesService().getBoolean(HelpBasePlugin.PLUGIN_ID, key, false, null);
	}

	private void setBooleanPreference(String key, boolean value) {
		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(HelpBasePlugin.PLUGIN_ID);
		prefs.putBoolean(key, value);
		try {
			prefs.flush();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
	}

}
