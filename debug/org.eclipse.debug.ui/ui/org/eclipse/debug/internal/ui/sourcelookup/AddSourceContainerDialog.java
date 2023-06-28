/*******************************************************************************
 * Copyright (c) 2003, 2018 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.sourcelookup;

import java.util.ArrayList;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.sourcelookup.ISourceContainerBrowser;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.PlatformUI;

/**
 * The dialog for adding new source containers. Presents the user with a list of
 * source container types and allows them to select one.
 *
 * @since 3.0
 */
public class AddSourceContainerDialog extends TitleAreaDialog {

	private TableViewer fViewer;
	private SourceContainerViewer fSourceContainerViewer;
	private ISourceLookupDirector fDirector;

	/**
	 * Constructor
	 * @param shell the shell to open this dialog on
	 * @param viewer the view associated with this dialog
	 * @param director the backing director
	 */
	public AddSourceContainerDialog(Shell shell, SourceContainerViewer viewer, ISourceLookupDirector director) {
		super(shell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		fSourceContainerViewer=viewer;
		fDirector = director;
	}

	@Override
	protected Control createDialogArea(Composite parent) {

		getShell().setText(SourceLookupUIMessages.addSourceLocation_title);
		setTitle(SourceLookupUIMessages.addSourceLocation_description);
		setTitleImage(DebugPluginImages.getImage(IInternalDebugUIConstants.IMG_ADD_SRC_LOC_WIZ));
		setMessage(SourceLookupUIMessages.AddSourceContainerDialog_select_source_container);

		Composite comp = (Composite) super.createDialogArea(parent);
		GridData gd= new GridData(GridData.FILL_BOTH);
		GridLayout topLayout = new GridLayout();
		topLayout.numColumns = 1;
		comp.setLayout(topLayout);
		comp.setLayoutData(gd);

		ISourceContainerType[] types = filterTypes(DebugPlugin.getDefault().getLaunchManager().getSourceContainerTypes());

		fViewer = new TableViewer(comp, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.SINGLE);
		final Table table = fViewer.getTable();
		gd = new GridData(GridData.FILL_BOTH);
		table.setLayoutData(gd);

		fViewer.addDoubleClickListener(event -> okPressed());

		fViewer.setLabelProvider(new SourceContainerLabelProvider());
		fViewer.setContentProvider(ArrayContentProvider.getInstance());
		fViewer.setComparator(new ViewerComparator());
		fViewer.addSelectionChangedListener(event -> {
			IStructuredSelection selection = event.getStructuredSelection();
			if (!selection.isEmpty()) {
				ISourceContainerType type = (ISourceContainerType) selection.getFirstElement();
				setMessage(type.getDescription());
				getButton(IDialogConstants.OK_ID).setEnabled(true);
			}
			else {
				getButton(IDialogConstants.OK_ID).setEnabled(false);
				setMessage(SourceLookupUIMessages.AddSourceContainerDialog_select_source_container);
			}
		});
		if(types.length != 0) {
			fViewer.setInput(types);
		}
		Dialog.applyDialogFont(comp);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getShell(), IDebugHelpContextIds.ADD_SOURCE_CONTAINER_DIALOG);
		return comp;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		Table table = fViewer.getTable();
		if(table.getItemCount() > 0) {
			fViewer.setSelection(new StructuredSelection(table.getItem(0).getData()));
		}
	}

	@Override
	protected void okPressed() {
		ISourceContainerType type = (ISourceContainerType) fViewer.getStructuredSelection().getFirstElement();
		if (type != null) {
			ISourceContainerBrowser browser = DebugUITools.getSourceContainerBrowser(type.getId());
			if (browser != null) {
				ISourceContainer[] results = browser.addSourceContainers(getShell(), fDirector);
				if (results != null && results.length > 0) {
					fSourceContainerViewer.addEntries(results);
					super.okPressed();
				}
				else {
					return;
				}
			}
		}
		super.okPressed();
	}

	/**
	 * Removes types without browsers from the provided list of types.
	 * @param types the complete list of source container types
	 * @return the list of source container types that have browsers
	 */
	private ISourceContainerType[] filterTypes(ISourceContainerType[] types){
		ArrayList<ISourceContainerType> validTypes = new ArrayList<>();
		for (ISourceContainerType type : types) {
			if (fDirector.supportsSourceContainerType(type)) {
				ISourceContainerBrowser sourceContainerBrowser = DebugUITools.getSourceContainerBrowser(type.getId());
				if(sourceContainerBrowser != null && sourceContainerBrowser.canAddSourceContainers(fDirector)) {
					validTypes.add(type);
				}
			}
		}
		return validTypes.toArray(new ISourceContainerType[validTypes.size()]);

	}
}
