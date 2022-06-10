/*******************************************************************************
 * Copyright (c) 2002, 2006 IBM Corporation and others.
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

import java.io.File;
import org.eclipse.core.tools.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;

/**
 * Dump Summary Spy view. This view shows the outcome of a dumping.
 */
public class DumpSummaryView extends SpyView {

	/** Stores the initially assigned view title. */
	private String initialTitle;

	/** The JFace widget that shows the current selected file dump summary */
	private TextViewer viewer;

	/** The id by which this view is known in the plug-in registry */
	public static final String VIEW_ID = DumpSummaryView.class.getName();

	/**
	 * Creates this view widget and actions.
	 *
	 * @param parent the parent control
	 * @see org.eclipse.ui.IWorkbenchPart#createPartControl
	 * (org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(final Composite parent) {

		// creates a read-only text viewer
		viewer = new TextViewer(parent, SWT.V_SCROLL | SWT.H_SCROLL);
		viewer.setDocument(new Document());
		viewer.setEditable(false);

		final IAction copySelectionAction = new CopyTextSelectionAction(viewer);
		final IAction clearContentsAction = new ClearTextAction(viewer.getDocument());

		// adds actions to the menu bar
		IMenuManager barMenuManager = getViewSite().getActionBars().getMenuManager();
		barMenuManager.add(copySelectionAction);
		barMenuManager.add(clearContentsAction);

		// creates a context menu with actions and adds it to the viewer control
		MenuManager menuMgr = new MenuManager();
		menuMgr.add(copySelectionAction);
		menuMgr.add(clearContentsAction);
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
	}

	/**
	 * Rebuilds the view with the dump object provided. Only summary information
	 * is made available.
	 *
	 * @param dump a dump object describing the result of a dumping process.
	 */
	void load(IDump dump) {

		// now is safe to get the part title
		if (initialTitle == null)
			this.initialTitle = this.getTitle();

		// sets title and tool tip
		File file = dump.getFile();
		this.setContentDescription(initialTitle + " : " + file.getName()); //$NON-NLS-1$
		this.setTitleToolTip("Dump summary for file " + file.getAbsolutePath()); //$NON-NLS-1$

		// generates text to be shown on this view
		StringBuilder output = new StringBuilder();
		if (dump.isFailed())
			output.append(dump.getFailureReason().toString());
		else
			output.append("No errors. "); //$NON-NLS-1$
		output.append(dump.getOffset());
		output.append('/');
		output.append(dump.getFile().length());
		output.append(" byte(s) read"); //$NON-NLS-1$

		// updates the view contents
		viewer.getDocument().set(output.toString());
	}

}
