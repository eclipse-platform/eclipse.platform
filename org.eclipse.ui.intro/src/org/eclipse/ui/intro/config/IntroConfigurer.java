/***************************************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
 *
 * This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - initial API and implementation
 **************************************************************************************************/

package org.eclipse.ui.intro.config;

import java.util.Map;

import org.eclipse.ui.internal.intro.impl.model.IntroModelRoot;
import org.eclipse.ui.intro.IIntroSite;


/**
 * Classes that extend this abstract class are used to configure <code>CustomizableIntroPart</code>.
 * Since it is possible for multiple products to use the same intro configuration, this class allows
 * them to customize some aspects of the intro content so that it looks different for different
 * products even though they all share the same intro implementation and content.
 *
 * @since 3.2
 */

public abstract class IntroConfigurer {

	/**
	 * The identifier of the named group where the configurer can contribute local tool bar actions.
	 *
	 * @see #init(IIntroSite, Map)
	 */
	public static final String TB_ADDITIONS = "additions"; //$NON-NLS-1$

	protected Map<String, String> themeProperties;
	protected IIntroSite site;

	private IntroModelRoot model;

	/**
	 * Provides the opportunity for the configurer to contribute to the action bars and to fetch
	 * presentation theme properties.
	 *
	 * @param site
	 *            the intro part's site
	 * @param themeProperties
	 *            properties of the current theme that can be used by the configurer, or
	 *            <code>null</code> if no theme is currently active or the active theme has no
	 *            properties.
	 */
	public void init(IIntroSite site, Map<String, String> themeProperties) {
		this.themeProperties = themeProperties;
		this.site = site;
	}

	/**
	 * Internal method to associate the corresponding intro configuration model. May be called
	 * independently of {@link #init(IIntroSite, Map)}.
	 *
	 * @noreference
	 */
	final public void bind(IntroModelRoot model) {
		// The configurer may vary its returned results based on the theme
		// properties
		this.model = model;
		if (model != null && model.getTheme() != null) {
			themeProperties = model.getTheme().getProperties();
		}
	}

	/**
	 * Returns the value of the theme property with a given name.
	 *
	 * @param name
	 *            the theme property name
	 * @return the value of the property or <code>null</code> if property is not found, the theme
	 *         does not have properties or no theme is currently active.
	 */

	protected String getThemeProperty(String name) {
		if (themeProperties == null)
			return null;
		String value = themeProperties.get(name);
		if (value != null && model != null)
			value = model.resolveVariables(value);
		return value;
	}

	/**
	 * Returns the value of the variable defined by the configurer. This variable can appear in XML
	 * content files in attribute names and values of elements. Whenever $variable$ is encountered
	 * in the content, it is evaluated using this class by passing 'variable' to this method and
	 * substituting the result in the content.
	 *
	 * @param variableName
	 *            the name of the substitution variable
	 * @return the value to substitute in place of a variable or <code>null</code> if the variable
	 *         cannot be resolved.
	 */
	public abstract String getVariable(String variableName);

	/**
	 * Returns the children of computed groups. Groups marked as computed will be completed at run
	 * time when the group is asked to provide children.
	 *
	 * @param pageId
	 *            the identifier of the page in which this group appears
	 * @param groupId
	 *            the identifier of the group group within the page
	 * @return an array of intro elements for this group. Each intro element should contain only
	 *         legal elements and attributes according to the intro content schema. Returns an empty
	 *         array for no children.
	 */
	public abstract IntroElement[] getGroupChildren(String pageId, String groupId);

	/**
	 * Returns an array of elements that will be used to build launch bar short cut links. Override
	 * this method if the intro launch bar has been marked as computed.
	 *
	 * @return an array of elements that will be used to dynamically build shortcut links.
	 */
	public IntroElement[] getLaunchBarShortcuts() {
		return new IntroElement[] {};
	}

	/**
	 * Resolves an incomplete path in the form "page_id/@" where page_id represents the identifier
	 * of the target page. The configurator should complete the path according to its internal
	 * resolution mechanism. The final path must point at an anchor in the referenced page.
	 *
	 * @param extensionId
	 *            the id specified for the config extension
	 * @param path
	 *            the incomplete path specified for the config extension
	 * @return the complete path that points at the anchor element in the referenced page, or
	 *         <code>null</code> if the path cannot be resolved or the extension should be hidden.
	 */
	public abstract String resolvePath(String extensionId, String path);

	/**
	 * Returns the style value that will be mixed in with the original style of the extension.
	 * Themes can use this feature to render certain extensions differently.
	 *
	 * @param pageId
	 *            the identifier of the target page that this extension is contributed into
	 * @param extensionId
	 *            the identifier of the extension to provide the mixin style for.
	 * @return the style to add to the original extension style or <code>null</code> if no mixin
	 *         style is found for this extension.
	 */
	public String getMixinStyle(String pageId, String extensionId) {
		return null;
	}

	/**
	 * Return true if {@code pageId} is the configured start page.
	 *
	 * @param pageId
	 *            the page identifier
	 * @since 3.5
	 */
	protected boolean isStartPage(String pageId) {
		return pageId.equals(model.getStartPageId());

	}


}