/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.editor.actions;


import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class AntEditorActionMessages {

	private static final String BUNDLE_NAME = "org.eclipse.ant.internal.ui.editor.actions.AntEditorActionMessages"; //$NON-NLS-1$

	private static final ResourceBundle fgResourceBundle = ResourceBundle.getBundle(BUNDLE_NAME);

	private AntEditorActionMessages() {
	}

	public static ResourceBundle getResourceBundle() {
		return fgResourceBundle;
	}
	public static String getString(String key) {
		try {
			return fgResourceBundle.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
}
