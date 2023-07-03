/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corporation and others.
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
 * A view which contributes a view to the help webapp
 * @since 3.5
 */

public abstract class AbstractView {

	/**
	 *
	 * @return a non translated name which is root name of the
	 * View and Toolbar jsp files used in this view. For example if the name
	 * is toc the help system will look for the files tocView.jsp
	 * and tocToolbar.jsp
	 */
	public abstract String getName();

	/**
	 * @return a URL path, relative to /help which is the
	 * location of the jsp files in the advanced presentation
	 */
	public abstract String getURL();

	/**
	 * @return a URL path, relative to /help which is the
	 * location of the jsp files in the basic presentation
	 * @deprecated Help webapp no longer has basic view.
	 */
	@Deprecated
	public String getBasicURL() {
		return getURL();
	}

	/**
	 * @return a URL relative to /help which is the location
	 * of the 16x16 image icon which will appear in the tab
	 */
	public abstract String getImageURL();

	/**
	 * @return a character which can be used as an accesskey to
	 * navigate directly to this view, or (char)0 if no
	 * acceskey is specified
	 */
	public abstract char getKey();

	/**
	 * Used to allow for views whose loading is deferred until
	 * their contents are visible
	 * @return true if this view has deferred loading
	 */
	public boolean isDeferred() {
		return false;
	}

	/**
	 * A user visible title for the view which will appear in the tooltip
	 * @param locale the locale of the client
	 * @return the tooltip text to be used in this locale
	 */
	public abstract String getTitle(Locale locale);

	/**
	 * @return true if the view should be shown in the advanced presentation
	 */
	public boolean isVisible() {
		return true;
	}

	/**
	 * @return true if the view should be shown in the basic presentation
	 * @deprecated Help webapp no longer has basic view.
	 */
	@Deprecated
	public boolean isVisibleBasic() {
		return true;
	}

}
