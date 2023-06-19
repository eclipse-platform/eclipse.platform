/*******************************************************************************
 * Copyright (c) 2004, 2016 IBM Corporation and others.
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
package org.eclipse.ui.internal.intro.impl.swt;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.internal.intro.impl.model.AbstractBaseIntroElement;
import org.eclipse.ui.internal.intro.impl.model.AbstractIntroContainer;
import org.eclipse.ui.internal.intro.impl.model.AbstractIntroElement;
import org.eclipse.ui.internal.intro.impl.model.AbstractIntroIdElement;
import org.eclipse.ui.internal.intro.impl.model.AbstractIntroPage;
import org.eclipse.ui.internal.intro.impl.model.IntroGroup;
import org.eclipse.ui.internal.intro.impl.model.IntroImage;
import org.eclipse.ui.internal.intro.impl.model.IntroLink;
import org.eclipse.ui.internal.intro.impl.model.IntroModelRoot;
import org.eclipse.ui.internal.intro.impl.model.IntroText;
import org.eclipse.ui.internal.intro.impl.model.loader.ModelLoaderUtil;
import org.eclipse.ui.internal.intro.impl.util.ImageUtil;
import org.eclipse.ui.internal.intro.impl.util.Log;
import org.osgi.framework.Bundle;

public class PageStyleManager extends SharedStyleManager {

	private AbstractIntroPage page;
	private Map<Properties, StyleContext> altStyleContexts = new HashMap<>();
	private IntroModelRoot root;


	/**
	 * Constructor used when page styles need to be loaded. The plugin's bundle
	 * is retrieved from the page model class. The default properties are
	 * assumed to be the presentation shared properties. The inherrited
	 * properties are properties that we got from included and extension styles.
	 *
	 * @param modelRoot
	 */
	public PageStyleManager(AbstractIntroPage page, Properties sharedProperties) {
		this.page = page;
		context = new StyleContext();
		context.bundle = page.getBundle();

		// honor shared-style.
		if (page.injectSharedStyle())
			properties = new Properties(sharedProperties);
		else
			properties = new Properties();
		String altStyle = page.getAltStyle();
		if (altStyle != null) {
			load(properties, altStyle, context);
		}

		// AltStyles Hashtable has alt-styles as keys, the bundles as
		// values.
		Map<String, Bundle> altStyles = page.getAltStyles();
		if (altStyles != null) {
			for (Entry<String, Bundle> entry : altStyles.entrySet()) {
				String style = entry.getKey();
				Properties inheritedProperties = new Properties();
				Bundle bundle = entry.getValue();
				StyleContext sc = new StyleContext();
				sc.bundle = bundle;
				load(inheritedProperties, style, sc);
				altStyleContexts.put(inheritedProperties, sc);
			}
		}

		// cache root
		root = (IntroModelRoot) page.getParentPage().getParent();
	}


	// Override parent method to include alt-styles. Use implicit keys as well.
	@Override
	public String getProperty(String key) {
		return getProperty(key, true);
	}

	// Override parent method to include alt-styles. If useImplicit is true, we
	// try to resolve a key without its pageId.
	private String getProperty(String key, boolean useImplicitKey) {
		Properties aProperties = findPropertyOwner(key);
		String value = super.doGetProperty(aProperties, key);
		if (useImplicitKey) {
			if (value == null && page.getId() != null
					&& key.startsWith(page.getId())) {
				// did not find the key as-is. Trim pageId and try again.
				String relativeKey = key.substring(page.getId().length());
				return getProperty(relativeKey);
			}
		}
		return value;
	}


	/**
	 * Finds a Properties that represents an inherited shared style, or this
	 * current pages style. If the given key is not found, the pageId is trimmed
	 * from the begining of the key, and the key is looked up again. If key does
	 * not start with a pageId, lookup only the key as is.
	 *
	 * @param key
	 * @return
	 */
	private Properties findPropertyOwner(String key) {
		// search for the key in this page's properties first.
		if (properties.containsKey(key))
			return properties;

		// search inherited properties second.
		for (Properties aProperties : altStyleContexts.keySet()) {
			if (aProperties.containsKey(key))
				return aProperties;
		}
		// we did not find the key. Return the local properties anyway.
		return properties;
	}



	/**
	 * Finds the context from which this key was loaded. If the key is not from
	 * an inherited alt style, then use the context corresponding to this page.
	 *
	 * @param key
	 * @return
	 */

	@Override
	protected StyleContext getAssociatedContext(String key) {
		Properties aProperties = findPropertyOwner(key);
		StyleContext context = altStyleContexts.get(aProperties);
		if (context != null)
			return context;
		return super.getAssociatedContext(key);
	}


	/*
	 * For number of columns, do not return 1 as the default, to allow for
	 * further processing. At the root page level, getting a 0 as ncolumns means
	 * that the number of columns is the number of children. At the page level,
	 * default is 1.
	 */
	public int getPageNumberOfColumns() {
		return getIntProperty(page, ".layout.ncolumns", 0); //$NON-NLS-1$
	}


	public int getNumberOfColumns(IntroGroup group) {
		return getIntProperty(group, ".layout.ncolumns", 0); //$NON-NLS-1$
	}

	public boolean getEqualWidth(IntroGroup group) {
		return getBooleanProperty(group, ".layout.equalWidth", false); //$NON-NLS-1$
	}

	public int getPageVerticalSpacing() {
		return getIntProperty(page, ".layout.vspacing", 5); //$NON-NLS-1$
	}

	public int getVerticalSpacing(IntroGroup group) {
		return getIntProperty(group, ".layout.vspacing", 5); //$NON-NLS-1$
	}

	public int getPageHorizantalSpacing() {
		return getIntProperty(page, ".layout.hspacing", 5); //$NON-NLS-1$
	}

	public int getHorizantalSpacing(IntroGroup group) {
		return getIntProperty(group, ".layout.hspacing", 5); //$NON-NLS-1$
	}

	public int getColSpan(AbstractBaseIntroElement element) {
		return getIntProperty(element, ".layout.colspan", 1); //$NON-NLS-1$
	}

	public int getRowSpan(AbstractBaseIntroElement element) {
		return getIntProperty(element, ".layout.rowspan", 1); //$NON-NLS-1$
	}

	private int getIntProperty(AbstractBaseIntroElement element,
			String qualifier, int defaultValue) {
		StringBuilder buff = ModelLoaderUtil.createPathToElementKey(element, true);
		if (buff == null)
			return defaultValue;
		String key = buff.append(qualifier).toString();
		return getIntProperty(key, defaultValue);
	}

	private boolean getBooleanProperty(AbstractBaseIntroElement element,
			String qualifier, boolean defaultValue) {
		StringBuilder buff = ModelLoaderUtil.createPathToElementKey(element, true);
		if (buff == null)
			return defaultValue;
		String key = buff.append(qualifier).toString();
		return getBooleanProperty(key, defaultValue);
	}

	private int getIntProperty(String key, int defaulValue) {
		int intValue = defaulValue;
		String value = getProperty(key);
		if (value == null)
			return intValue;

		try {
			intValue = Integer.parseInt(value);
		} catch (NumberFormatException e) {
			Log.error("Failed to parse key: " + key + " as an integer.", e); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return intValue;
	}

	private boolean getBooleanProperty(String key, boolean defaultValue) {
		boolean booleanValue = defaultValue;
		String value = getProperty(key);
		if (value != null)
			booleanValue = value.equalsIgnoreCase("true"); //$NON-NLS-1$
		return booleanValue;
	}


	/**
	 * Finds the description text of the given group. Looks for the Text child
	 * element whos id is specified as follows:
	 * <pre>{@code
	 * <pageId>. <path_to_group>.description-id= <id of child description Text element>
	 * }</pre>
	 * If not found, use the default description style.
	 *
	 * Returns null if no default style found, or any id in path is null.
	 *
	 * @param group
	 * @return
	 */
	public String getDescription(IntroGroup group) {
		StringBuilder buff = ModelLoaderUtil.createPathToElementKey(group, true);
		if (buff == null)
			return null;
		String key = buff.append(".description-id").toString(); //$NON-NLS-1$
		return doGetDescription(group, key);
	}


	/**
	 * Finds the description text of the associated page. Looks for the Text
	 * child element whos id is specified as follows:
	 * <pre>{@code
	 * <pageId>.description-id= <id of child description Text element>
	 * }</pre>
	 * If not found, use the default description style.
	 *
	 * Returns null if no default style found, or any id in path is null.
	 *
	 * @param group
	 * @return
	 */
	public String getPageDescription() {
		if (page.getId() == null)
			return null;
		String key = page.getId() + ".description-id"; //$NON-NLS-1$
		return doGetDescription(page, key);
	}

	private String doGetDescription(AbstractIntroContainer parent, String key) {
		String path = getProperty(key);
		String description = null;
		if (path != null)
			description = findTextFromPath(parent, path);
		if (description != null)
			return description;
		return findTextFromStyleId(parent, getDescriptionStyleId());
	}

	private String getDescriptionStyleId() {
		String key = "description-style-id"; //$NON-NLS-1$
		return getProperty(key);
	}

	/**
	 * Finds the subtitle of the associated page. Looks for the Text child
	 * element whose id is specified as follows:
	 * <pre>{@code
	 * <pageId>.description-id= <id of child description Text element>
	 * }</pre>
	 * If not found, use the default description style.
	 *
	 * @param group
	 * @return
	 */
	public String getPageSubTitle() {
		String key = page.getId() + ".subtitle-id"; //$NON-NLS-1$
		String path = getProperty(key);
		String description = null;
		if (path != null)
			description = findTextFromPath(page, path);
		if (description != null)
			return description;
		return findTextFromStyleId(page, getPageSubTitleStyleId());
	}

	private String getPageSubTitleStyleId() {
		String key = "subtitle-style-id"; //$NON-NLS-1$
		return getProperty(key);
	}

	private String findTextFromPath(AbstractIntroContainer parent, String path) {
		AbstractIntroElement child = parent.findTarget(root, path);
		if (child != null && child.isOfType(AbstractIntroElement.TEXT)) {
			makeFiltered(child);
			return ((IntroText) child).getText();
		}
		return null;
	}

	/**
	 * Returns the first direct child text element with the given style-id.
	 *
	 * @return
	 */
	private String findTextFromStyleId(AbstractIntroContainer parent,
			String styleId) {
		IntroText[] allText = (IntroText[]) parent
			.getChildrenOfType(AbstractIntroElement.TEXT);
		for (int i = 0; i < allText.length; i++) {
			if (allText[i].getStyleId() == null)
				// not all elements have style id.
				continue;
			if (allText[i].getStyleId().equals(styleId)) {
				makeFiltered(allText[i]);
				return allText[i].getText();
			}
		}
		return null;
	}

	/**
	 * Util method to check model type, and filter model element out if it is of
	 * the correct type.
	 *
	 * @param element
	 */
	private AbstractIntroElement makeFiltered(AbstractIntroElement element) {
		if (element.isOfType(AbstractIntroElement.BASE_ELEMENT))
			((AbstractBaseIntroElement) element).setFilterState(true);
		return element;
	}



	public boolean getShowLinkDescription() {
		String key = page.getId() + ".show-link-description"; //$NON-NLS-1$
		String value = getProperty(key);
		if (value == null) {
			key = ".show-link-description"; //$NON-NLS-1$
			value = getProperty(key);
		}
		if (value == null)
			value = "true"; //$NON-NLS-1$
		return value.toLowerCase().equals("true"); //$NON-NLS-1$
	}

	public boolean showHomePageNavigation() {
		String key = page.getId() + ".show-home-page-navigation"; //$NON-NLS-1$
		String value = getProperty(key);
		if (value == null) {
			key = ".show-home-page-navigation"; //$NON-NLS-1$
			value = getProperty(key);
		}
		if (value == null)
			value = "true"; //$NON-NLS-1$
		return value.equalsIgnoreCase("true"); //$NON-NLS-1$
	}


	public Color getColor(FormToolkit toolkit, AbstractBaseIntroElement element) {
		StringBuilder buff = ModelLoaderUtil.createPathToElementKey(element, true);
		if (buff == null)
			return null;
		String key = buff.append(".font.fg").toString(); //$NON-NLS-1$
		return getColor(toolkit, key);
	}

	public Color getBackgrond(FormToolkit toolkit, AbstractBaseIntroElement element) {
		StringBuilder buff = ModelLoaderUtil.createPathToElementKey(element, true);
		if (buff == null)
			return null;
		String key = buff.append(".bg").toString(); //$NON-NLS-1$
		return getColor(toolkit, key);
	}

	public boolean isBold(IntroText text) {
		String value = null;
		/*
		StringBuilder buff = ModelLoaderUtil.createPathToElementKey(text, true);
		if (buff != null) {
			String key = buff.append(".font.bold").toString(); //$NON-NLS-1$
			value = getProperty(key);
			if (value != null)
				return value.toLowerCase().equals("true"); //$NON-NLS-1$
			else {
				buff = ModelLoaderUtil.createPathToElementKey(text, true);
			}
		}
		*/
		value = getPropertyValue(text, ".font.bold"); //$NON-NLS-1$
		if (value == null) {
			// bold is not specified by ID. Check to see if there is a style-id
			// specified for bold.
			value = getProperty("bold-style-id"); //$NON-NLS-1$
			if (value != null && text.getStyleId() != null)
				return text.getStyleId().equals(value);
		}
		return false;
	}

	private String getPropertyValue(AbstractIntroIdElement element, String suffix) {
		StringBuilder buff = ModelLoaderUtil.createPathToElementKey(element, true);
		if (buff != null) {
			String key = buff.append(suffix).toString();
			String value = getProperty(key);
			if (value != null)
				return value;
			// try the page.id key
			buff = ModelLoaderUtil.createPathToElementKey(element, false);
			if (buff!= null) {
				key = buff.append(suffix).toString();
				value = getProperty(key);
				return value;
			}
		}
		return null;
	}

	public static Font getBannerFont() {
		return JFaceResources.getBannerFont();
	}

	public static Font getHeaderFont() {
		return JFaceResources.getHeaderFont();
	}

	/**
	 * Retrieves an image for a link in a page. If not found, uses the page's
	 * default link image. If still not found, uses the passed default.
	 *
	 * @param link
	 * @param qualifier
	 * @return
	 */
	public Image getImage(IntroLink link, String qualifier, String defaultKey) {
		// try the Id first
		String key = createImageByIdKey(page, link, qualifier);
		String value = getProperty(key, false);
		if (value==null) {
			key = createImageKey(page, link, qualifier);
			// special case where we have to handle this because extended code does
			// not go through getProperty() in this method.
			value = getProperty(key, false);
		}
		if (value == null && page.getId() != null
				&& key.startsWith(page.getId()))
			// did not use the key as-is. Trim pageId and try again.
			key = key.substring(page.getId().length());

		// pageKey can not become an implicit key.
		String pageKey = createImageKey(page, null, qualifier);

		return getImage(key, pageKey, defaultKey);
	}

	private String createImageKey(AbstractIntroPage page, IntroLink link,
			String qualifier) {
		StringBuilder buff = null;
		if (link != null) {
			buff = ModelLoaderUtil.createPathToElementKey(link, true);
			if (buff == null)
				return ""; //$NON-NLS-1$
		} else {
			buff = new StringBuilder();
			buff.append(page.getId());
		}
		buff.append("."); //$NON-NLS-1$
		buff.append(qualifier);
		return buff.toString();
	}

	private String createImageByIdKey(AbstractIntroPage page, IntroLink link,
			String qualifier) {
		if (link==null || link.getId()==null)
			return ""; //$NON-NLS-1$
		StringBuilder buff = new StringBuilder();
		buff.append(page.getId());
		buff.append("."); //$NON-NLS-1$
		buff.append(link.getId());
		buff.append("."); //$NON-NLS-1$
		buff.append(qualifier);
		return buff.toString();
	}

	public Image getImage(IntroImage introImage) {
		String imageLocation = introImage.getSrcAsIs();
		StringBuilder buff = ModelLoaderUtil.createPathToElementKey(introImage, true);
		String key;
		if (buff == null) {
			key = "//" + imageLocation; //$NON-NLS-1$
		} else {
			key = buff!=null?buff.toString():null;
		}
		if (ImageUtil.hasImage(key))
			return ImageUtil.getImage(key);
		// key not already registered.
		if (buff != null) {
			StyleContext acontext = getAssociatedContext(key);
			if (acontext.inTheme) {
					ImageUtil.registerImage(key, acontext.path, imageLocation);
				return ImageUtil.getImage(key);
			}
		}
		Bundle bundle = introImage.getBundle();
		if (FileLocator.find(bundle, IPath.fromOSString(imageLocation), null) == null) {
			return null;
		}
		ImageUtil.registerImage(key, bundle, imageLocation);
		return ImageUtil.getImage(key);

	}


}
