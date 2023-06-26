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

import java.io.File;
import org.eclipse.core.runtime.*;
import org.eclipse.core.tools.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;

/**
 * Dump Contents Spy view. This view shows the contents resulting of a dumping
 * process.
 */
public class DumpContentsView extends SpyView {

	/** Stores the initially assigned view title. */
	private String initialTitle;

	/** The JFace widget that shows the current selected file dump contents. */
	protected TextViewer viewer;

	/** The file names registered in the DumperFactory. */
	protected String[] registeredFileNames;

	/** The dumper factory used to instantiate dumpers. */
	private DumperFactory dumperFactory;

	/** The current selected file. */
	private File currentFile;

	/** The id by which this view is known in the plug-in registry */
	public static final String VIEW_ID = DumpContentsView.class.getName();

	/**
	 * Constructs a DumpContentsView.
	 */
	public DumpContentsView() {
		dumperFactory = DumperFactory.getInstance();
		this.registeredFileNames = dumperFactory.getRegisteredFileNames();
	}

	/**
	 * Creates this view widget and actions.
	 *
	 * @param parent the parent control
	 * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(final Composite parent) {
		// creates a read-only text viewer
		viewer = new TextViewer(parent, SWT.V_SCROLL | SWT.H_SCROLL);
		viewer.setDocument(new Document());
		viewer.setEditable(false);

		// creates the actions
		final IAction loadFileAction = new LoadFileAction();
		final IAction copySelectionAction = new CopyTextSelectionAction(viewer);
		final IAction clearContentsAction = new ClearTextAction(viewer.getDocument());

		// adds actions to the menu bar
		IMenuManager barMenuManager = getViewSite().getActionBars().getMenuManager();
		barMenuManager.add(loadFileAction);
		barMenuManager.add(copySelectionAction);
		barMenuManager.add(clearContentsAction);

		// creates a context menu with actions and adds it to the viewer control
		MenuManager menuMgr = new MenuManager();
		menuMgr.add(loadFileAction);
		menuMgr.add(copySelectionAction);
		menuMgr.add(clearContentsAction);
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
	}

	/**
	 * Sets the file to be dumped. The view state will be updated to reflect
	 * changes.
	 *
	 * @param file the file to be dumped
	 */
	public void setFile(File file) {
		IDumper dumper = null;

		// tries to get the associated file dumper
		try {
			dumper = dumperFactory.getDumper(file.getName());
		} catch (DumpException de) {
			ErrorUtil.logException(de, null);
			ErrorUtil.showErrorMessage(de.getMessage(), "Error creating file dumper"); //$NON-NLS-1$
			return;
		}

		// dumps file
		IDump dump = dumper.dump(file);
		if (dump.isFailed()) {
			Status status = new Status(IStatus.ERROR, DumpContentsView.class, 0, "Error during file dump", //$NON-NLS-1$
					dump.getFailureReason());
			ILog.of(DumpContentsView.class).log(status);
			String message = "File dumping did not complete successfully. Reason: " + dump.getFailureReason(); //$NON-NLS-1$
			ErrorUtil.showErrorMessage(message, "Error during file dump"); //$NON-NLS-1$
		}

		// loads the new dump object
		load(dump);
	}

	/**
	 * Rebuilds the view with the dump object provided.
	 *
	 * @param dump a dump object to be shown on this view
	 */
	private void load(IDump dump) {

		this.currentFile = dump.getFile();

		// now it is safe to get the part title
		// (during createPartControl it gets Workbench window title)
		if (initialTitle == null)
			this.initialTitle = this.getTitle();

		// updates title and title tool tip
		this.setContentDescription(initialTitle + " : " + this.currentFile.getName()); //$NON-NLS-1$
		this.setTitleToolTip("Dump contents for " + this.currentFile.getAbsolutePath()); //$NON-NLS-1$

		// updates viewer
		viewer.getDocument().set(dump.getContents().toString());

		// tries to show summary information in Dump Summary view
		IWorkbenchPage page = this.getSite().getPage();
		String summaryId = DumpSummaryView.VIEW_ID;

		// maybe the summary view is already open
		DumpSummaryView summaryView = (DumpSummaryView) page.findView(summaryId);

		// if it is not the case and the dump failed, tries to open it
		if (summaryView == null && dump.isFailed())
			try {
				summaryView = (DumpSummaryView) page.showView(summaryId);
			} catch (PartInitException pie) {
				ErrorUtil.logException(pie, "Error opening view"); //$NON-NLS-1$
			}

		// if the Dump Summary view is available, updates it
		if (summaryView != null)
			summaryView.load(dump);
	}

	/**
	 * File load action implementation.
	 */
	private class LoadFileAction extends Action {
		/** Sets action text and tool tip. */
		LoadFileAction() {
			this.setText("Load File..."); //$NON-NLS-1$
			this.setToolTipText("Load file..."); //$NON-NLS-1$
		}

		/**
		 * Executes this action, opening a file dialog so the user can select the
		 * file to be dumped. If a file is successfully selected, opens it by
		 * calling <code>#setFile(File)</code>.
		 *
		 * @see DumpContentsView#setFile
		 * @see org.eclipse.jface.action.IAction#run()
		 */
		@Override
		public void run() {
			// opens a file dialog
			FileDialog fileDialog = new FileDialog(viewer.getControl().getShell(), SWT.OPEN);
			fileDialog.setText("Please choose a metadata file to view"); //$NON-NLS-1$
			fileDialog.setFilterExtensions(DumpContentsView.this.registeredFileNames);

			// if a file hasn't been selected, ignore
			String filePath = fileDialog.open();
			if (filePath == null)
				return;

			// opens the selected file
			DumpContentsView.this.setFile(new File(filePath));
		}
	}

}
