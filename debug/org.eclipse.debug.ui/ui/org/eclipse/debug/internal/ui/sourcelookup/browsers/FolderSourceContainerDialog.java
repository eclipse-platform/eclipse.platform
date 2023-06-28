/*******************************************************************************
 * Copyright (c) 2003, 2007 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.sourcelookup.browsers;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.sourcelookup.SourceLookupUIMessages;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.views.navigator.ResourceComparator;
import org.osgi.framework.FrameworkUtil;

/**
 * The dialog for selecting the folder for which a source container will be created.
 *
 * @since 3.0
 */
public class FolderSourceContainerDialog extends ElementTreeSelectionDialog {

	/**
	 * Constant to persist the state of the search subfolders button
	 *
	 * @since 3.2
	 */
	private static final String LAST_SUBDIR_SETTING = "EXT_FOLDER_SOURCE_LAST_SUBDIR_SETTING"; //$NON-NLS-1$

	/**
	 * Lets us control searching subfolders
	 *
	 * @since 3.2
	 */
	private Button fSubfoldersButton;

	/**
	 * stored value whether to search subfolders or not
	 *
	 * @since 3.2
	 */
	private boolean fSearchSubfolders = false;

	/**
	 * We need to add in the new control for indicating whether to search sub folders or not
	 *
	 * @since 3.2
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite parentc = (Composite)super.createDialogArea(parent);
		fSubfoldersButton = new Button(parentc, SWT.CHECK);
		fSubfoldersButton.setText(SourceLookupUIMessages.DirectorySourceContainerDialog_6);
		fSubfoldersButton.setSelection(fSearchSubfolders);
		return parentc;
	}

	/**
	 * Sets the dialog values for its construction
	 * @param parent the parent of the dialog
	 * @param labelProvider the label provider for the content of the tree in the dialog
	 * @param contentProvider the provider of the tree content for the dialog
	 */
	public FolderSourceContainerDialog(Shell parent, ILabelProvider labelProvider, ITreeContentProvider contentProvider) {
		super(parent, labelProvider, contentProvider);
		setTitle(SourceLookupUIMessages.folderSelection_title);	//
		setInput(ResourcesPlugin.getWorkspace().getRoot());
		setComparator(new ResourceComparator(ResourceComparator.NAME));
		ISelectionStatusValidator validator= selection -> {
			for (Object f : selection) {
				if (!(f instanceof IFolder)) {
					return new Status(IStatus.ERROR, DebugUIPlugin.getUniqueIdentifier(), -1, SourceLookupUIMessages.sourceSearch_folderSelectionError, null); //
				}
			}
			return new Status(IStatus.OK, DebugUIPlugin.getUniqueIdentifier(), 0, IInternalDebugCoreConstants.EMPTY_STRING, null);
		};
		setValidator(validator);
		setDoubleClickSelects(true);
		setAllowMultiple(true);
		setMessage(SourceLookupUIMessages.folderSelection_label);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent,  IDebugHelpContextIds.ADD_FOLDER_CONTAINER_DIALOG);
		IDialogSettings dialogSettings = PlatformUI
				.getDialogSettingsProvider(FrameworkUtil.getBundle(FolderSourceContainerDialog.class))
				.getDialogSettings();
		setSearchSubfolders(dialogSettings.getBoolean(LAST_SUBDIR_SETTING));
		addFilter(new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if(!(element instanceof IFolder)) {
					if(element instanceof IProject) {
						return ((IProject)element).isAccessible();
					}
					return false;
				}
				return true;
			}
		});
	}

	/**
	 * Returns whether the 'search subfolders' option is selected.
	 *
	 * @since 3.2
	 * @return true if the search subfolders button is selected, false otherwise.
	 */
	public boolean isSearchSubfolders() {
		return fSearchSubfolders;
	}

	/**
	 * Sets whether the 'search subfolders' option is selected.
	 *
	 * @param subfolders
	 * @since 3.2
	 */
	public void setSearchSubfolders(boolean subfolders) {
		fSearchSubfolders = subfolders;
	}

	@Override
	protected void okPressed() {
		fSearchSubfolders = fSubfoldersButton.getSelection();
		PlatformUI.getDialogSettingsProvider(FrameworkUtil.getBundle(FolderSourceContainerDialog.class))
				.getDialogSettings().put(LAST_SUBDIR_SETTING, fSearchSubfolders);
		super.okPressed();
	}

}
