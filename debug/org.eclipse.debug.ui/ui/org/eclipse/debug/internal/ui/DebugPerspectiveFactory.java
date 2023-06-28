/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui;


import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.progress.IProgressConstants;
import org.eclipse.ui.texteditor.templates.TemplatesView;

/**
 * The debug perspective factory.
 */
public class DebugPerspectiveFactory implements IPerspectiveFactory {

	/**
	 * @see IPerspectiveFactory#createInitialLayout(IPageLayout)
	 */
	@Override
	public void createInitialLayout(IPageLayout layout) {

		String editorArea = layout.getEditorArea();

		IFolderLayout navFolder = layout.createFolder(IInternalDebugUIConstants.ID_NAVIGATOR_FOLDER_VIEW, IPageLayout.LEFT, (float) 0.25, editorArea);
		navFolder.addView(IDebugUIConstants.ID_DEBUG_VIEW);
		navFolder.addPlaceholder(IPageLayout.ID_PROJECT_EXPLORER);

		IFolderLayout toolsFolder = layout.createFolder(IInternalDebugUIConstants.ID_TOOLS_FOLDER_VIEW, IPageLayout.BOTTOM, (float) 0.75, editorArea);
		toolsFolder.addView(IConsoleConstants.ID_CONSOLE_VIEW);
		toolsFolder.addView(IPageLayout.ID_PROBLEM_VIEW);
		toolsFolder.addPlaceholder(IDebugUIConstants.ID_REGISTER_VIEW);
		toolsFolder.addPlaceholder(IPageLayout.ID_BOOKMARKS);
		toolsFolder.addPlaceholder(IProgressConstants.PROGRESS_VIEW_ID);

		IFolderLayout outlineFolder = layout.createFolder(IInternalDebugUIConstants.ID_OUTLINE_FOLDER_VIEW, IPageLayout.RIGHT, (float) 0.65, editorArea);
		outlineFolder.addView(IDebugUIConstants.ID_VARIABLE_VIEW);
		outlineFolder.addView(IDebugUIConstants.ID_BREAKPOINT_VIEW);
		outlineFolder.addView(IDebugUIConstants.ID_EXPRESSION_VIEW);
		outlineFolder.addPlaceholder(IPageLayout.ID_OUTLINE);
		outlineFolder.addPlaceholder(IPageLayout.ID_PROP_SHEET);

		layout.addShowViewShortcut(IProgressConstants.PROGRESS_VIEW_ID);
		layout.addShowViewShortcut(TemplatesView.ID);

		layout.addActionSet(IDebugUIConstants.LAUNCH_ACTION_SET);
		layout.addActionSet(IDebugUIConstants.DEBUG_ACTION_SET);
		layout.addActionSet(IPageLayout.ID_NAVIGATE_ACTION_SET);

		setContentsOfShowViewMenu(layout);
	}

	/**
	 * Sets the initial contents of the "Show View" menu.
	 */
	protected void setContentsOfShowViewMenu(IPageLayout layout) {
		layout.addShowViewShortcut(IDebugUIConstants.ID_DEBUG_VIEW);
		layout.addShowViewShortcut(IDebugUIConstants.ID_VARIABLE_VIEW);
		layout.addShowViewShortcut(IDebugUIConstants.ID_BREAKPOINT_VIEW);
		layout.addShowViewShortcut(IDebugUIConstants.ID_EXPRESSION_VIEW);
		layout.addShowViewShortcut(IPageLayout.ID_OUTLINE);
		layout.addShowViewShortcut(IConsoleConstants.ID_CONSOLE_VIEW);
		layout.addShowViewShortcut(IPageLayout.ID_PROBLEM_VIEW);
		layout.addShowViewShortcut(IPageLayout.ID_PROJECT_EXPLORER);
	}
}
