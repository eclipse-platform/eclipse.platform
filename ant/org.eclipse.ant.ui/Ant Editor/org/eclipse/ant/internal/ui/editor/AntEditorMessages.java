/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.ant.internal.ui.editor;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class AntEditorMessages {

	private static final String BUNDLE_NAME = "org.eclipse.ant.internal.ui.editor.AntEditorMessages"; //$NON-NLS-1$

	private static final ResourceBundle fgResourceBundle = ResourceBundle.getBundle(BUNDLE_NAME);

	private AntEditorMessages() {
	}

	public static String getString(String key) {
		try {
			return fgResourceBundle.getString(key);
		}
		catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}

	protected static ResourceBundle getResourceBundle() {
		return fgResourceBundle;
	}
}
