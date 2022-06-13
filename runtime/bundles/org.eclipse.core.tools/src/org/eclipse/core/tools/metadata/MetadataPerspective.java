/*******************************************************************************
 * Copyright (c) 2002, 2005 IBM Corporation and others.
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
package org.eclipse.core.tools.metadata;

import org.eclipse.ui.*;

/**
 * A perspective for working with metadata files.
 */
public class MetadataPerspective implements IPerspectiveFactory {
	/**
	 * Creates a layout containing the following views: Metadata Tree, Dump Contents
	 * and Dump Summary, with no editor area.
	 *
	 * @see org.eclipse.ui.IPerspectiveFactory#createInitialLayout(
	 * org.eclipse.ui.IPageLayout)
	 */
	@Override
	public void createInitialLayout(IPageLayout layout) {
		String editorAreaId = layout.getEditorArea();

		layout.setEditorAreaVisible(false);

		IFolderLayout left = layout.createFolder("left", IPageLayout.LEFT, (float) 0.30, editorAreaId); //$NON-NLS-1$
		left.addView(MetadataTreeView.VIEW_ID);

		IFolderLayout right = layout.createFolder("right", IPageLayout.RIGHT, (float) 0.5, editorAreaId); //$NON-NLS-1$
		right.addView(DumpContentsView.VIEW_ID);

		IFolderLayout bottom = layout.createFolder("bottom", IPageLayout.BOTTOM, (float) 0.70, "right"); //$NON-NLS-1$ //$NON-NLS-2$
		bottom.addView(DumpSummaryView.VIEW_ID);

	}
}
