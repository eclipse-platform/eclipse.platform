/*******************************************************************************
 *  Copyright (c) 2006, 2019 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ui.internal.intro.universal;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.internal.util.ProductPreferences;
import org.eclipse.help.internal.util.SequenceResolver;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.internal.intro.impl.model.AbstractIntroIdElement;
import org.eclipse.ui.internal.intro.impl.model.ExtensionMap;
import org.eclipse.ui.internal.intro.universal.contentdetect.ContentDetector;
import org.eclipse.ui.internal.intro.universal.util.ImageUtil;
import org.eclipse.ui.internal.intro.universal.util.PreferenceArbiter;
import org.eclipse.ui.intro.IIntroSite;
import org.eclipse.ui.intro.config.IntroConfigurer;
import org.eclipse.ui.intro.config.IntroElement;
import org.osgi.framework.Bundle;

/**
 * This class provides for dynamic configuration of the shared intro
 * implementation based on the data file associated with the product.
 *
 * @since 3.2
 */
public class UniversalIntroConfigurer extends IntroConfigurer implements
		IUniversalIntroConstants {

	private IntroData primaryIntroData;
	private IntroData[] secondaryIntroData;
	private SequenceResolver<IntroElement> sequenceResolver;

	public UniversalIntroConfigurer() {
		loadData();
	}

	@Override
	public String getVariable(String variableName) {
		if (variableName.equals(HIGH_CONTRAST)) {
			boolean highContrast = ImageUtil.isHighContrast();
			if (highContrast)
				return variableName;
			else
				return ""; //$NON-NLS-1$
		}
		IProduct product = Platform.getProduct();
		if (product != null) {
			// try product property first
			String value = getProductProperty(product, variableName);
			if (value != null) {
				value = resolveVariable(product.getDefiningBundle(), value);
				return value;
			}
			// if intro description for the page is not defined
			// return a blank string to prevent the variable
			// from showing up in the page
			if (variableName.startsWith(VAR_INTRO_DESCRIPTION_PREFIX))
				return ""; //$NON-NLS-1$
			// nothing - try preferences
			// try to prefix with a product id first
			String key = product.getId() + "_" + variableName; //$NON-NLS-1$
			value = Platform.getPreferencesService().getString(UniversalIntroPlugin.PLUGIN_ID,  key, "", null); //$NON-NLS-1$
			if (value.length() == 0) {
				// try direct variable name
				key = variableName;
				value = Platform.getPreferencesService().getString(UniversalIntroPlugin.PLUGIN_ID,  key, "", null); //$NON-NLS-1$
			}
			if (value.length() > 0)
				value = resolveVariable(product.getDefiningBundle(), value);
			else {
				// pass it to the theme
				value = getThemeProperty(variableName);
			}
			return value;
		}
		return null;
	}

	@Override
	public String getMixinStyle(String pageId, String extensionId) {
		// if active product has a preference, use it
		if (primaryIntroData != null) {
			int importance = getImportance(primaryIntroData, pageId, extensionId);
			if (importance >= 0) {
				return ExtensionData.IMPORTANCE_STYLE_TABLE[importance];
			}
		}
		// else, find the most referenced importance style from other products
		int[] importanceRefs = new int[ExtensionData.IMPORTANCE_TABLE.length];
		for (int i=0;i<secondaryIntroData.length;++i) {
			IntroData data = secondaryIntroData[i];
			int importance = getImportance(data, pageId, extensionId);
			if (importance >= 0) {
				++importanceRefs[importance];
			}
		}
		int maxIndex = 0;
		for (int i=1;i<importanceRefs.length;++i) {
			if (importanceRefs[i] > importanceRefs[maxIndex]) {
				maxIndex = i;
			}
		}
		if (importanceRefs[maxIndex] > 0) {
			return ExtensionData.IMPORTANCE_STYLE_TABLE[maxIndex];
		}
		// nobody has a preference
		return null;
	}

	/*
	 * Returns the given extension's importance as specified by the
	 * given intro data.
	 */
	private int getImportance(IntroData data, String pageId, String extensionId) {
		String pluginId = ExtensionMap.getInstance().getPluginId(extensionId);
		if (ContentDetector.isNew(pluginId)) {
			updateStartPage(pageId);
			return ExtensionData.NEW;
		}
		PageData pdata = data.getPage(pageId);
		if (pdata != null) {
			ExtensionData ed = pdata.findExtension(extensionId, false);
			if (ed != null) {
				return ed.getImportance();
			}
		}
		// none specified
		return -1;
	}

	/*
	 * Modify the start page if this is a root page and it's position
	 * in the root page list is earlier than the current start page
	 */
	private void updateStartPage(String pageId) {
		String currentStartPage = ExtensionMap.getInstance().getStartPage();
		String ids = getVariable(VAR_INTRO_ROOT_PAGES);
		if (ids != null) {
			StringTokenizer stok = new StringTokenizer(ids, ","); //$NON-NLS-1$
			while (stok.hasMoreTokens()) {
				String id = stok.nextToken().trim();
				if (id.equals(pageId)) {
					ExtensionMap.getInstance().setStartPage(pageId);
					return;
				}
				if (id.equals(currentStartPage)) {
					// The current start page has higher priority than the new page
					return;
				}
			}
		}
	}

	private String resolveVariable(Bundle bundle, String value) {
		if (value == null) {
			return null;
		}
		try {
			if (value.startsWith("intro:")) { //$NON-NLS-1$
				bundle = UniversalIntroPlugin.getDefault().getBundle();
				return resolveFile(bundle, value.substring(6));
			} else if (value.startsWith("product:")) { //$NON-NLS-1$
				return resolveFile(bundle, value.substring(8));
			}
		} catch (IOException e) {
			// just use the value as-is
		}
		return value;
	}

	private String resolveFile(Bundle bundle, String path) throws IOException {
		String prefixedPath = getThemePrefixedPath(path);
		URL url = null;
		if (prefixedPath != null) {
			url = bundle.getEntry(prefixedPath);
		}
		if (url == null) {
			url = bundle.getEntry(path);
		}
		if (url != null) {
			URL localURL = FileLocator.toFileURL(url);
			return localURL.toString();
		}
		return null;
	}

	/**
	 * Prefix the file component of the given path with the theme's id. For
	 * example, with theme <code>org.eclipse.ui.intro.universal.solstice</code>:
	 * <ul>
	 * <li>foo &rarr; org.eclipse.ui.intro.universal.solstice/foo
	 * </ul>
	 *
	 * @param path
	 *            the path
	 * @return same path with a prefixed theme directory component
	 */
	private String getThemePrefixedPath(String path) {
		String prefix = themeProperties != null ? themeProperties.get(AbstractIntroIdElement.ATT_ID) : null;
		prefix = prefix == null ? "" : prefix.trim(); //$NON-NLS-1$
		if (prefix.length() == 0) {
			return null;
		}
		return prefix + IPath.SEPARATOR + path;
	}

	private String getProductProperty(IProduct product, String variableName) {
		String value = product.getProperty(variableName);
		if (value == null) {
			// return default values
			if (variableName.equals(VAR_INTRO_BACKGROUND_IMAGE))
				return "css/graphics/root/welcomebckgrd.jpg"; //$NON-NLS-1$
		}
		return value;
	}

	@Override
	public IntroElement[] getGroupChildren(String pageId, String groupId) {
		// root-like pages have more details on the page and action links
		boolean rootLike = pageId.equals(ID_ROOT) || isStartPage(pageId) || pageId.equals(ID_STANDBY);
		if (rootLike && groupId.equals(DIV_PAGE_LINKS))
				return getRootPageLinks(pageId.equals(ID_STANDBY));
		if (rootLike && groupId.equals(DIV_ACTION_LINKS))
				return getRootPageActionLinks(pageId.equals(ID_STANDBY));
		// other pages
		if (groupId.equals(DIV_PAGE_LINKS))
			return getNavLinks(pageId);
		if (groupId.equals(DIV_LAYOUT_TOP_LEFT) || groupId.equals(DIV_LAYOUT_TOP_RIGHT)
				|| groupId.equals(DIV_LAYOUT_BOTTOM_LEFT) || groupId.equals(DIV_LAYOUT_BOTTOM_RIGHT))
			return getContent(pageId, groupId);
		return new IntroElement[0];
	}

	@Override
	public IntroElement[] getLaunchBarShortcuts() {
		ArrayList<IntroElement> links = new ArrayList<>();
		String ids = getVariable(VAR_INTRO_ROOT_PAGES);
		if (ids != null) {
			StringTokenizer stok = new StringTokenizer(ids, ","); //$NON-NLS-1$
			while (stok.hasMoreTokens()) {
				String id = stok.nextToken().trim();
				IntroElement page = createLaunchBarShortcut(id);
				if (page != null)
					links.add(page);
			}
		}
		return links.toArray(new IntroElement[links.size()]);
	}

	private IntroElement[] getRootPageLinks(boolean standby) {
		ArrayList<IntroElement> links = new ArrayList<>();
		String ids = getVariable(VAR_INTRO_ROOT_PAGES);
		if (ids != null) {
			StringTokenizer stok = new StringTokenizer(ids, ","); //$NON-NLS-1$
			while (stok.hasMoreTokens()) {
				String id = stok.nextToken().trim();
				IntroElement page = createRootPageLink(id, standby);
				if (page != null)
					links.add(page);
			}
		}
		// add workbench link if so configured by the theme
		String wb = getVariable(VAR_WORKBENCH_AS_ROOT_LINK);
		if (wb!=null && wb.equalsIgnoreCase("true")) { //$NON-NLS-1$
			IntroElement page = createRootPageLink(ID_WORKBENCH, standby);
			if (page !=null)
				links.add(page);
		}
		return links.toArray(new IntroElement[links.size()]);
	}

	private IntroElement[] getRootPageActionLinks(boolean standby) {
		String wb = getVariable(VAR_WORKBENCH_AS_ROOT_LINK);
		// only create the workbench link if
		// not already configured as a root link
		if (wb==null || !wb.equalsIgnoreCase("true")) { //$NON-NLS-1$
			IntroElement page = createRootPageLink(ID_WORKBENCH, standby);
			if (page !=null)
				return new IntroElement[] { page };
		}
		return new IntroElement [0];
	}

	private IntroElement[] getNavLinks(String pageId) {
		ArrayList<IntroElement> links = new ArrayList<>();
		String ids = getVariable(VAR_INTRO_ROOT_PAGES);
		/*
		 * In high contrast mode the workbench link must be generated in the nav links
		 * otherwise it will not show
		 */
		if (ImageUtil.isHighContrast()) {
			ids = ids + ',' + IUniversalIntroConstants.ID_WORKBENCH;
		}
		if (ids != null) {
			StringTokenizer stok = new StringTokenizer(ids, ","); //$NON-NLS-1$
			int [] counter = new int [1];
			while (stok.hasMoreTokens()) {
				String id = stok.nextToken().trim();
				IntroElement page = createNavLink(id, pageId, counter);
				if (page != null)
					links.add(page);
			}
		}

		return links.toArray(new IntroElement[links.size()]);
	}

	private IntroElement createRootPageLink(String id, boolean standby) {

		if (id.equals(ID_OVERVIEW))
			return createRootLink(
					Messages.SharedIntroConfigurer_overview_name,
					createPageURL(id, standby), id, "overview_img", Messages.SharedIntroConfigurer_overview_alt, //$NON-NLS-1$
					Messages.SharedIntroConfigurer_overview_tooltip, "left"); //$NON-NLS-1$
		if (id.equals(ID_FIRSTSTEPS))
			return createRootLink(
					Messages.SharedIntroConfigurer_firststeps_name,
					createPageURL(id, standby), id, "firststeps_img", Messages.SharedIntroConfigurer_firststeps_alt, //$NON-NLS-1$
					Messages.SharedIntroConfigurer_firststeps_tooltip, "left"); //$NON-NLS-1$
		if (id.equals(ID_TUTORIALS))
			return createRootLink(
					Messages.SharedIntroConfigurer_tutorials_name,
					createPageURL(id, standby), id, "tutorials_img", Messages.SharedIntroConfigurer_tutorials_alt, //$NON-NLS-1$
					Messages.SharedIntroConfigurer_tutorials_tooltip, "left"); //$NON-NLS-1$
		if (id.equals(ID_SAMPLES))
			return createRootLink(
					Messages.SharedIntroConfigurer_samples_name,
					createPageURL(id, standby), id, "samples_img", Messages.SharedIntroConfigurer_samples_alt, Messages.SharedIntroConfigurer_samples_tooltip, "right"); //$NON-NLS-1$ //$NON-NLS-2$
		if (id.equals(ID_WHATSNEW))
			return createRootLink(
					Messages.SharedIntroConfigurer_whatsnew_name,
					createPageURL(id, standby), id, "whatsnew_img", Messages.SharedIntroConfigurer_whatsnew_alt, //$NON-NLS-1$
					Messages.SharedIntroConfigurer_whatsnew_tooltip, "right"); //$NON-NLS-1$
		if (id.equals(ID_MIGRATE))
			return createRootLink(
					Messages.SharedIntroConfigurer_migrate_name,
					createPageURL(id, standby), id, "migrate_img",  Messages.SharedIntroConfigurer_migrate_alt, Messages.SharedIntroConfigurer_migrate_tooltip, "right"); //$NON-NLS-1$ //$NON-NLS-2$
		if (id.equals(ID_WEBRESOURCES))
			return createRootLink(
					Messages.SharedIntroConfigurer_webresources_name,
					createPageURL(id, standby), id,"webresources_img",  //$NON-NLS-1$
					Messages.SharedIntroConfigurer_webresources_alt,
					Messages.SharedIntroConfigurer_webresources_tooltip,
					"right"); //$NON-NLS-1$
		if (id.equals(ID_WORKBENCH))
			return createRootLink(
					Messages.SharedIntroConfigurer_workbench_name,
					"http://org.eclipse.ui.intro/switchToLaunchBar", id, "workbench_img",  //$NON-NLS-1$ //$NON-NLS-2$
					Messages.SharedIntroConfigurer_workbench_alt,
					Messages.SharedIntroConfigurer_workbench_tooltip,
					"right"); //$NON-NLS-1$
		return null;
	}

	private IntroElement createNavLink(String id, String pageId, int [] counter) {
		if (id.equals(ID_OVERVIEW))
			return createNavLink(Messages.SharedIntroConfigurer_overview_nav,
					createPageURL(id, false), id, "left nav_link"+(++counter[0])); //$NON-NLS-1$
		if (id.equals(ID_FIRSTSTEPS))
			return createNavLink(Messages.SharedIntroConfigurer_firststeps_nav,
					createPageURL(id, false), id, "left  nav_link"+(++counter[0])); //$NON-NLS-1$
		if (id.equals(ID_TUTORIALS))
			return createNavLink(Messages.SharedIntroConfigurer_tutorials_nav,
					createPageURL(id, false), id, "left nav_link"+(++counter[0])); //$NON-NLS-1$
		if (id.equals(ID_SAMPLES))
			return createNavLink(Messages.SharedIntroConfigurer_samples_nav,
					createPageURL(id, false), id, "right nav_link"+(++counter[0])); //$NON-NLS-1$
		if (id.equals(ID_WHATSNEW))
			return createNavLink(Messages.SharedIntroConfigurer_whatsnew_nav,
					createPageURL(id, false), id, "right nav_link"+(++counter[0])); //$NON-NLS-1$
		if (id.equals(ID_MIGRATE))
			return createNavLink(Messages.SharedIntroConfigurer_migrate_nav,
					createPageURL(id, false), id, "right nav_link"+(++counter[0])); //$NON-NLS-1$
		if (id.equals(ID_WEBRESOURCES))
			return createNavLink(
					Messages.SharedIntroConfigurer_webresources_nav,
					createPageURL(id, false), id, "right nav_link"+(++counter[0])); //$NON-NLS-1$
		if (id.equals(ID_WORKBENCH))
			return createNavLink(
					Messages.SharedIntroConfigurer_workbench_name,
					"http://org.eclipse.ui.intro/switchToLaunchBar", //$NON-NLS-1$
					id,
					"right nav_link"+(++counter[0])); //$NON-NLS-1$
		return null;
	}

	private String createPageURL(String id, boolean standby) {
		String url = "http://org.eclipse.ui.intro/showPage?id=" + id; //$NON-NLS-1$
		if (standby)
			url += "&standby=false"; //$NON-NLS-1$
		return url;
	}

	private IntroElement createLaunchBarShortcut(String id) {
		if (id.equals(ID_OVERVIEW))
			return createShortcutLink(
					getThemeProperty(LAUNCHBAR_OVERVIEW_ICON), Messages.SharedIntroConfigurer_overview_nav,
					id);
		if (id.equals(ID_FIRSTSTEPS))
			return createShortcutLink(
					getThemeProperty(LAUNCHBAR_FIRSTSTEPS_ICON), Messages.SharedIntroConfigurer_firststeps_nav,
					id);
		if (id.equals(ID_TUTORIALS))
			return createShortcutLink(
					getThemeProperty(LAUNCHBAR_TUTORIALS_ICON), Messages.SharedIntroConfigurer_tutorials_nav,
					id);
		if (id.equals(ID_SAMPLES))
			return createShortcutLink(
					getThemeProperty(LAUNCHBAR_SAMPLES_ICON), Messages.SharedIntroConfigurer_samples_nav,
					id);
		if (id.equals(ID_WHATSNEW))
			return createShortcutLink(
					getThemeProperty(LAUNCHBAR_WHATSNEW_ICON), Messages.SharedIntroConfigurer_whatsnew_nav,
					id);
		if (id.equals(ID_MIGRATE))
			return createShortcutLink(
					getThemeProperty(LAUNCHBAR_MIGRATE_ICON), Messages.SharedIntroConfigurer_migrate_nav,
					id);
		if (id.equals(ID_WEBRESOURCES))
			return createShortcutLink(
					getThemeProperty(LAUNCHBAR_WEBRESOURCES_ICON), Messages.SharedIntroConfigurer_webresources_nav,
					id);
		return null;
	}

	private IntroElement createRootLink(String name, String url, String id,
			String imgId, String imgAlt, String imgText,
			String styleId) {
		IntroElement element = new IntroElement("link"); //$NON-NLS-1$
		element.setAttribute("label", name); //$NON-NLS-1$
		element.setAttribute("url", url); //$NON-NLS-1$
		element.setAttribute("id", id); //$NON-NLS-1$
		element.setAttribute("style-id", styleId);//$NON-NLS-1$
		IntroElement img = new IntroElement("img"); //$NON-NLS-1$
		img.setAttribute("id", imgId); //$NON-NLS-1$
		img.setAttribute("style-id", "content-img"); //$NON-NLS-1$ //$NON-NLS-2$
		boolean highContrast = ImageUtil.isHighContrast();
		if (highContrast) {
			String key = HIGH_CONTRAST_PREFIX+id;
			String value = getVariable(key);
			if (value!=null)
				img.setAttribute("src", value); //$NON-NLS-1$
		}
		img.setAttribute("alt", imgAlt); //$NON-NLS-1$
		img.setAttribute("title", ""); //$NON-NLS-1$ //$NON-NLS-2$
		IntroElement text = new IntroElement("text"); //$NON-NLS-1$
		text.setValue(imgText);
		element.addChild(img);
		element.addChild(text);
		return element;
	}

	private IntroElement createNavLink(String label, String url, String id,
			String styleId) {
		IntroElement element = new IntroElement("link"); //$NON-NLS-1$
		element.setAttribute("label", label); //$NON-NLS-1$
		element.setAttribute("url", url); //$NON-NLS-1$
		element.setAttribute("id", id); //$NON-NLS-1$
		boolean highContrast = ImageUtil.isHighContrast();
		if (highContrast) {
			IntroElement img = new IntroElement("img"); //$NON-NLS-1$
			img.setAttribute("style-id", "content-img"); //$NON-NLS-1$ //$NON-NLS-2$
			String key = HIGH_CONTRAST_NAV_PREFIX+id;
			String value = getVariable(key);
			if (value!=null)
				img.setAttribute("src", value); //$NON-NLS-1$
			img.setAttribute("alt", label); //$NON-NLS-1$
			element.addChild(img);
			styleId += " "+HIGH_CONTRAST; //$NON-NLS-1$
		}
		element.setAttribute("style-id", styleId); //$NON-NLS-1$
		return element;
	}

	private IntroElement createShortcutLink(String icon, String tooltip,
			String id) {
		IntroElement element = new IntroElement("shortcut"); //$NON-NLS-1$
		element.setAttribute("icon", icon); //$NON-NLS-1$
		element.setAttribute("tooltip", tooltip); //$NON-NLS-1$
		element.setAttribute("url", createPageURL(id, false)); //$NON-NLS-1$
		return element;
	}

	private void loadData() {
		// load the active product's intro data first
		IProduct product = Platform.getProduct();
		if (product != null) {
			String dataFile = getVariable(VAR_INTRO_DATA);
			if (dataFile != null) {
				primaryIntroData = new IntroData(product.getId(), dataFile, true);
			}
		}
		// load all other installed (but not running) products' intro data
		List<IntroData> result = new ArrayList<>();
		Properties[] prefs = ProductPreferences.getProductPreferences(false);
		for (int i=0;i<prefs.length;++i) {
			String key = UniversalIntroPlugin.PLUGIN_ID + '/' + VAR_INTRO_DATA;
			String dataFile = prefs[i].getProperty(key);
			if (dataFile != null) {
				String pluginId = ProductPreferences.getPluginId(prefs[i]);
				Bundle bundle = Platform.getBundle(pluginId);
				if (bundle != null) {
					String pid = ProductPreferences.getProductId(prefs[i]);
					dataFile = resolveVariable(bundle, dataFile);
					result.add(new IntroData(pid, dataFile, false));
				}
			}
		}
		secondaryIntroData = result.toArray(new IntroData[result.size()]);
	}

	private IntroElement[] getContent(String pageId, String groupId) {
		List<IntroElement> result = new ArrayList<>();
		if (!ContentDetector.getNewContributors().isEmpty()) {
			// Add a new content fallback anchor
			IntroElement fallback = new IntroElement("anchor"); //$NON-NLS-1$
			fallback.setAttribute("id", NEW_CONTENT_ANCHOR); //$NON-NLS-1$
			result.add(fallback);
		}
		List<IntroElement> anchors = getAnchors(pageId, groupId);
		if (anchors != null) {
			result.addAll(anchors);
		}
		// Add the fallback anchor
		IntroElement fallback = new IntroElement("anchor"); //$NON-NLS-1$
		fallback.setAttribute("id", DEFAULT_ANCHOR); //$NON-NLS-1$
		result.add(fallback);
		return result.toArray(new IntroElement[result.size()]);
	}

	private List<IntroElement> getAnchors(String pageId, String groupId) {
		List<IntroElement> primaryAnchors = null;
		if (primaryIntroData != null) {
			primaryAnchors = getAnchors(primaryIntroData, pageId, groupId);
		}
		if (primaryAnchors == null) {
			primaryAnchors = Collections.emptyList();
		}
		List<List<IntroElement>> secondaryAnchors = new ArrayList<>();
		for (int i=0;i<secondaryIntroData.length;++i) {
			IntroData idata = secondaryIntroData[i];
			List<IntroElement> anchors = getAnchors(idata, pageId, groupId);
			if (anchors != null) {
				secondaryAnchors.add(anchors);
			}
		}

		if (sequenceResolver == null) {
			sequenceResolver = new SequenceResolver<>();
		}
		return sequenceResolver.getSequence(primaryAnchors, secondaryAnchors);
	}

	private List<IntroElement> getAnchors(IntroData data, String pageId, String groupId) {
		PageData pdata = data.getPage(pageId);
		if (pdata != null) {
			List<IntroElement> anchors = new ArrayList<>();
			pdata.addAnchors(anchors, groupId);
			return anchors;
		}
		return null;
	}

	@Override
	public String resolvePath(String extensionId, String path) {
		boolean extensionRelativePath = false;
		IPath ipath = IPath.fromOSString(path);
		String pageId = ipath.segment(0);
		String s2 = ipath.segment(1);
		// if it's "@extension_id" then target that extension instead
		if (s2.startsWith("@") && s2.length() > 1) { //$NON-NLS-1$
			extensionId = s2.substring(1);
		}
		if (!s2.equals("@")) { //$NON-NLS-1$
			extensionRelativePath = true;
		}
		if (!isHidden(extensionId, pageId)) {
			String resolvedPath = resolveExtensionPath(extensionId, pageId);
			if (resolvedPath != null) {
				if (extensionRelativePath) {
					// not done - use the resolved extension path to complete the source path
					IPath p2 = IPath.fromOSString(resolvedPath);
					IPath p1 = ipath.removeFirstSegments(2);
					// remove the last anchor and append the relative path from the extension
					resolvedPath = p2.removeLastSegments(1).append(p1).toString();
				}
				return resolvedPath;
			}
			return pageId + DEFAULT_CONTENT_PATH;
		}
		return null;
	}

	private String resolveExtensionPath(String extensionId, String pageId) {
		// does the active product have a preference?
		if (primaryIntroData != null) {
			PageData pdata = primaryIntroData.getPage(pageId);
			if (pdata != null) {
				String path = pdata.resolvePath(extensionId);
				if (path != null) {
					return path;
				}
			}
		}
		// if not, do the others have preferences?
		PreferenceArbiter arbiter = new PreferenceArbiter();
		for (int i=0;i<secondaryIntroData.length;++i) {
			IntroData idata = secondaryIntroData[i];
			PageData pdata = idata.getPage(pageId);
			if (pdata != null) {
				arbiter.consider(pdata.resolvePath(extensionId));
			}
		}
		String path = (String)arbiter.getWinner();
		if (path != null) {
			return path;
		}
		// there was no clear winner; fall back to the default
		return resolveDefaultPath(pageId, extensionId);
	}

	private String resolveDefaultPath(String pageId, String extensionId) {
		String pluginId = ExtensionMap.getInstance().getPluginId(extensionId);
		if (ContentDetector.isNew(pluginId)) {
			return pageId + IUniversalIntroConstants.NEW_CONTENT_PATH;
		}
		// does the active product have a preference?
		if (primaryIntroData != null) {
			PageData pdata = primaryIntroData.getPage(pageId);
			if (pdata != null) {
				String path = pdata.resolveDefaultPath();
				if (path != null) {
					return path;
				}
			}
		}
		// if not, do the others have preferences?
		PreferenceArbiter arbiter = new PreferenceArbiter();
		for (int i=0;i<secondaryIntroData.length;++i) {
			IntroData idata = secondaryIntroData[i];
			PageData pdata = idata.getPage(pageId);
			if (pdata != null) {
				arbiter.consider(pdata.resolveDefaultPath());
			}
		}
		return (String)arbiter.getWinner();
	}

	private boolean isHidden(String extensionId, String pageId) {
		if (primaryIntroData != null) {
			PageData pdata = primaryIntroData.getPage(pageId);
			if (pdata != null) {
				return pdata.isHidden(extensionId);
			}
		}
		return false;
	}

	@Override
	public void init(IIntroSite site, Map<String, String> themeProperties) {
		super.init(site, themeProperties);
		Action customizeAction = new CustomizeAction(site);
		customizeAction.setText(Messages.SharedIntroConfigurer_customize_label);
		customizeAction
				.setToolTipText(Messages.SharedIntroConfigurer_customize_text);
		customizeAction.setImageDescriptor(ImageUtil
				.createImageDescriptor("full/elcl16/configure.png")); //$NON-NLS-1$
		site.getActionBars().getToolBarManager().appendToGroup(TB_ADDITIONS,
				customizeAction);
	}
}
