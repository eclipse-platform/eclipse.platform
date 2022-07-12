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

/**
 * A view which contributes a frame to the help webapp
 * @since 3.5
 */

public abstract class AbstractFrame implements Comparable<AbstractFrame> {

	/**
	 * Constant returned from getFrameLocation() function to indicate that
	 * the frame should be created below the content frame or the Main Help Toolbar
	 */
	public static final int BELOW_CONTENT = 1;
	public static final int HELP_TOOLBAR = 2;

	/**
	 * Function which defines the frame location
	 * @return a constant defined in this class which indicates the location of this frame
	 */
	public abstract int getLocation();

	/**
	 *
	 * @return a non translated name which is the name of this frame
	 */
	public abstract String getName();

	/**
	 * @return a URL path, relative to /help which is the
	 * location of the jsp files in the advanced presentation
	 */
	public abstract String getURL();

	/**
	 * @return a string which will be used in the rows or cols attribute of a
	 * frameset in the html
	 */
	public String getSize() {
		return "*"; //$NON-NLS-1$
	}

	/**
	 * @return true if the frame should be shown in the advanced presentation
	 */
	public boolean isVisible() {
		return true;
	}

	/**
	 * allows the attributes of this frame other than name and src to be specified
	 * @return a list of attributes
	 */
	public String getFrameAttributes() {
		return "\"marginwidth=\"1\" marginheight=\"1\" frameborder=\"1\" scrolling=\"no\""; //$NON-NLS-1$
	}

	/**
	 * @since 3.7
	 */
	@Override
	public final int compareTo(AbstractFrame o) {
		if (o != null) {
			String objectName = o.getName();
			return (getName().compareTo(objectName));
		}
		return 0;
	}

}
