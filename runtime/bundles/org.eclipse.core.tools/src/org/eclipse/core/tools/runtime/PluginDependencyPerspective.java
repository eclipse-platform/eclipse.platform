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
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tools.runtime;

import org.eclipse.ui.*;

/**
 * A perspective for working with metadata files.
 */
public class PluginDependencyPerspective implements IPerspectiveFactory {
	/**
	 * Creates a layout containing the following views:
	 * 	PluginListView and
	 * 	PluginDependencyView
	 * @see org.eclipse.ui.IPerspectiveFactory#createInitialLayout(org.eclipse.ui.IPageLayout)
	 */
	@Override
	public void createInitialLayout(IPageLayout layout) {
		String editorAreaId = layout.getEditorArea();

		layout.setEditorAreaVisible(false);

		IFolderLayout left = layout.createFolder("left", IPageLayout.LEFT, (float) 0.30, editorAreaId); //$NON-NLS-1$
		left.addView(PluginListView.VIEW_ID);

		IFolderLayout right = layout.createFolder("right", IPageLayout.RIGHT, (float) 0.7, editorAreaId); //$NON-NLS-1$
		right.addView(PluginDependencyView.VIEW_ID);
	}

}
