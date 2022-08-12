/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.team.ui.synchronize.ISynchronizeView;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class TeamSynchronizingPerspective implements IPerspectiveFactory {

	public final static String ID = "org.eclipse.team.ui.TeamSynchronizingPerspective"; //$NON-NLS-1$

	@Override
	public void createInitialLayout(IPageLayout layout) {
		defineActions(layout);
		defineLayout(layout);
	}

	/**
	 * Defines the initial actions for a page.
	 * @param layout the page layout
	 */
	public void defineActions(IPageLayout layout) {

		// Add "new wizards".
		layout.addNewWizardShortcut("org.eclipse.ui.wizards.new.project"); //$NON-NLS-1$
		layout.addNewWizardShortcut("org.eclipse.ui.wizards.new.folder"); //$NON-NLS-1$
		layout.addNewWizardShortcut("org.eclipse.ui.wizards.new.file"); //$NON-NLS-1$

		// Add "show views".
		layout.addShowViewShortcut(ISynchronizeView.VIEW_ID);
		layout.addShowViewShortcut(IPageLayout.ID_PROJECT_EXPLORER);
		layout.addShowViewShortcut(IPageLayout.ID_OUTLINE);
		layout.addShowViewShortcut(IPageLayout.ID_TASK_LIST);
		layout.addShowViewShortcut(IPageLayout.ID_PROBLEM_VIEW);

		// Add "action sets"
		layout.addActionSet("org.eclipse.team.ui.actionSet"); //$NON-NLS-1$

		// Add "perspective short cuts"
		layout.addPerspectiveShortcut("org.eclipse.ui.resourcePerspective"); //$NON-NLS-1$
	}

	/**
	 * Defines the initial layout for a page.
	 * @param layout the page layout
	 */
	public void defineLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		IFolderLayout top = layout.createFolder("top", IPageLayout.LEFT, 0.45f, editorArea);	//$NON-NLS-1$
		top.addView(ISynchronizeView.VIEW_ID);
		IFolderLayout top2 = layout.createFolder("top2", IPageLayout.BOTTOM, 0.80f, editorArea);	//$NON-NLS-1$
		top2.addView(IHistoryView.VIEW_ID);
		top2.addView(IPageLayout.ID_TASK_LIST);
		top2.addView(IPageLayout.ID_PROBLEM_VIEW);
		layout.setEditorAreaVisible(true);
	}
}
