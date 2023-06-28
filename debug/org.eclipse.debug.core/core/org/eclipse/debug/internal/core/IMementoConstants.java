/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.debug.internal.core;

public interface IMementoConstants {
	// These persistence constant is stored in XML. Do not
	// change it.
	String TAG_FACTORY_ID = "factoryID"; //$NON-NLS-1$

	String TAG_EDITOR_STATE = "editorState"; //$NON-NLS-1$

	String MEMENTO_ITEM = "item"; //$NON-NLS-1$

	String TAG_EDIT_PAGE_ID = "editPageId"; //$NON-NLS-1$

	String TAG_NAME = "name"; //$NON-NLS-1$

	String TAG_LABEL = "label"; //$NON-NLS-1$

	String TAG_ID = "id"; //$NON-NLS-1$

	String TAG_LAUNCH_CONFIGURATION_WORKING_SET = "launchConfigurationWorkingSet"; //$NON-NLS-1$

}
