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

package org.eclipse.ui.internal.intro.impl.model;

import org.osgi.framework.Bundle;
import org.w3c.dom.Element;

/**
 * An intro image element.
 */
public class IntroInclude extends AbstractIntroElement {

	protected static final String TAG_INCLUDE = "include"; //$NON-NLS-1$

	private static final String ATT_CONFIG_ID = "configId"; //$NON-NLS-1$
	protected static final String ATT_PATH = "path"; //$NON-NLS-1$
	/**
	 * boolean attribute, default is false.
	 */
	private static final String ATT_MERGE_STYLE = "merge-style"; //$NON-NLS-1$

	private String configId;
	private String path;
	private boolean mergeStyle = false;

	IntroInclude(Element element, Bundle bundle) {
		super(element, bundle);
		configId = getAttribute(element, ATT_CONFIG_ID);
		path = getAttribute(element, ATT_PATH);
		String mergeStyleString = getAttribute(element, ATT_MERGE_STYLE);
		mergeStyle = (mergeStyleString != null && mergeStyleString
			.equalsIgnoreCase("true")) ? true : false; //$NON-NLS-1$
	}

	/**
	 * @return Returns the configId.
	 */
	public String getConfigId() {
		return configId;
	}

	/**
	 * @return Returns the mergeStyle.
	 */
	public boolean getMergeStyle() {
		return mergeStyle;
	}

	/**
	 * @return Returns the path.
	 */
	public String getPath() {
		return path;
	}

	@Override
	public int getType() {
		return AbstractIntroElement.INCLUDE;
	}

}
