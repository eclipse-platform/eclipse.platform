/*******************************************************************************
 * Copyright (c) 2002, 2018 IBM Corporation and others.
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
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * Implements the Metadata Spy view. This view allows the user to browse the
 * known files under a specified directory. "Known files" are those whose names
 * are known by the DumperFactory, thus they can be dumped.
 */
public class MetadataTreeView extends SpyView {

	/**
	 * A JFace widget used to display a tree corresponding to the metadata
	 * directory structure (containing only files of known types).
	 */
	protected AbstractTreeViewer viewer;

	/** The currently selected metadata dir. */
	protected File metadataPath;

	/** The id by which this view is known in the plug-in registry */
	public static final String VIEW_ID = MetadataTreeView.class.getName();

	/**
	 * Dump file action implementation.
	 */
	private class DumpFileAction extends Action {
		/**
		 * Constructs a DumpFileAction, initializing text and tool tip text.
		 */
		DumpFileAction() {
			this.setText("Dump Selected File"); //$NON-NLS-1$
			this.setToolTipText("Dump selected file"); //$NON-NLS-1$
		}

		/**
		 * This action activates the Dump Contents view with the current selected
		 * file in this view (if any)
		 *
		 * @see org.eclipse.jface.action.IAction#run()
		 */
		@Override
		public void run() {
			IStructuredSelection sel = MetadataTreeView.this.viewer.getStructuredSelection();
			if (sel == null || sel.isEmpty())
				return;

			TreeContentProviderNode fileNode = (TreeContentProviderNode) sel.getFirstElement();

			File file = (File) fileNode.getValue();
			// we have no interest in directories
			if (!file.isFile())
				return;

			try {
				DumpContentsView dumpView = (DumpContentsView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(DumpContentsView.VIEW_ID);
				dumpView.setFile(file);
			} catch (PartInitException e) {
				ErrorUtil.showErrorMessage(e.getMessage(), "Error opening view"); //$NON-NLS-1$
				ErrorUtil.logException(e, "Error opening view"); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Select metadata root action implementation.
	 */
	private class SelectMetadataLocationAction extends Action {
		/**
		 * Initializes action's text and tool tip text.
		 */
		SelectMetadataLocationAction() {
			this.setText("Select metadata location..."); //$NON-NLS-1$
			this.setToolTipText("Select metadata location..."); //$NON-NLS-1$
		}

		/**
		 * Executes action, opening a file dialog so the user can select which
		 * metadata directory will be browsed.
		 *
		 * @see org.eclipse.jface.action.IAction#run()
		 */
		@Override
		public void run() {
			DirectoryDialog dirDialog = new DirectoryDialog(viewer.getControl().getShell());

			dirDialog.setText("Open a metadata directory"); //$NON-NLS-1$
			dirDialog.setMessage("Please choose a directory where to look for metadata files"); //$NON-NLS-1$
			if (metadataPath != null)
				dirDialog.setFilterPath(metadataPath.getAbsolutePath());

			String dirPath = dirDialog.open();

			if (dirPath == null)
				return;

			setMetadataRoot(new File(dirPath));
		}
	}

	/**
	 * Creates this view widget and actions.
	 *
	 * @param parent the parent control
	 * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent);

		// sets a content provider for the viewer
		DumperFactory factory = DumperFactory.getInstance();
		String[] fileNames = factory.getRegisteredFileNames();
		viewer.setContentProvider(new MetadataTreeContentProvider(fileNames));

		// creates actions
		final IAction dumpFileAction = new DumpFileAction();
		final IAction selectMetadataLocationAction = new SelectMetadataLocationAction();

		// adds actions to the menu bar
		IMenuManager barMenuManager = getViewSite().getActionBars().getMenuManager();
		barMenuManager.add(selectMetadataLocationAction);
		barMenuManager.add(dumpFileAction);

		// creates a context menu with actions and adds it to the viewer control
		MenuManager menuMgr = new MenuManager();
		menuMgr.add(selectMetadataLocationAction);
		menuMgr.add(dumpFileAction);

		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);

		// associates double-click to dump file action
		viewer.addDoubleClickListener(event -> dumpFileAction.run());
	}

	/**
	 * Rebuilds the viewer content provider.
	 *
	 * @param file
	 * @see WorkspaceContentProvider#inputChanged(Viewer, Object, Object)
	 */
	protected void setMetadataRoot(final File file) {
		if (file == null)
			return;
		if (!file.isDirectory()) {
			ErrorUtil.showErrorMessage("Selected item is not a directory", "Error selecting metadata location"); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}
		metadataPath = file;
		viewer.setInput(file);
	}

}
