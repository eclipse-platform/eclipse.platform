/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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

package org.eclipse.help.webapp;

import java.util.Locale;

/**
 * A class which contributes a button to the help webapp
 * @since 3.5
 */

public abstract class AbstractButton implements Comparable<AbstractButton> {

	/**
	 * Gets the id which will be assigned to the image of the button in the
	 * generated HTML
	 * @return a string that consists of alphanumeric characters only with no spaces
	 */
	public abstract String getId();

	/**
	 * @return a URL relative to /help which is the location
	 * of the 16x16 image icon which will appear in the tab
	 */
	public abstract String getImageURL();

	/**
	 * A user visible description of the button which will appear in the tooltip
	 * @param locale the locale of the client
	 * @return the tooltip text to be used in this locale
	 */
	public abstract String getTooltip(Locale locale);

	/**
	 * a JavaScript function which will be called when the button is pressed
	 * @return the name of a JavaScript function
	 */
	public abstract String getAction();

	/**
	 * The state of a button which is visible but not depressed
	 */
	public static final String BUTTON_OUT = "off"; //$NON-NLS-1$

	/**
	 * The state of a button which is visible and depressed
	 */
	public static final String BUTTON_IN = "on"; //$NON-NLS-1$

	/**
	 * The state of a button which is hidden
	 */
	public static final String BUTTON_HIDDEN = "hidden"; //$NON-NLS-1$

	/**
	 * Get the state of a button
	 * @return one of <code>BUTTON_OUT</code>, <code>BUTTON_IN</code>, or
	 * <code>BUTTON_HIDDEN</code>.
	 */
	public String getState() {
		return BUTTON_OUT;
	}

	/**
	 * Get the location of the a javascript file to be included in any
	 * jsp file which uses this button
	 * @return a URL path, relative to /help or <code>null</code> if there.
	 */
	public String getJavaScriptURL() {
		return null;
	}

	/**
	 * Toolbar name for the content pane, which shows help pages
	 */
	public static final String CONTENT_TOOLBAR = "content"; //$NON-NLS-1$

	/**
	 * Toolbar name for the table of contents
	 */
	public static final String TOC_TOOLBAR = "toc"; //$NON-NLS-1$

	/**
	 * Toolbar name for the keyword index
	 */
	public static final String INDEX_TOOLBAR = "index"; //$NON-NLS-1$

	/**
	 * Toolbar name for search results
	 */
	public static final String SEARCH_TOOLBAR = "search"; //$NON-NLS-1$

	/**
	 * Toolbar name for bookmarks
	 */
	public static final String BOOKMARKS_TOOLBAR = "bookmarks"; //$NON-NLS-1$

	/**
	 * Determines whether this button should be true if the button should be added
	 * to particular toolbar
	 * @param toolbarName Name of the toolbar.
	 * @return true
	 */
	public boolean isAddedToToolbar(String toolbarName) {
		return true;
	}

	/**
	 * @since 3.7
	 */
	@Override
	public final int compareTo(AbstractButton o) {
		if (o != null) {
			String objectName = o.getId();
			return (getId().compareTo(objectName));
		}
		return 0;
	}
}
