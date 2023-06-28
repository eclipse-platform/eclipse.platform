/*******************************************************************************
 * Copyright (c) 2003, 2017 IBM Corporation and others.
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
 *     QNX Software Systems - Mikhail Khodjaiants - Bug 114664
 *******************************************************************************/
package org.eclipse.debug.internal.ui.sourcelookup.browsers;

import java.io.File;

import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.sourcelookup.SourceLookupUIMessages;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.FrameworkUtil;

/**
 * The dialog for selecting the external folder for which a source container will be created.
 *
 * @since 3.0
 */
public class DirectorySourceContainerDialog extends TitleAreaDialog {

	private static final String LAST_PATH_SETTING = "EXT_FOLDER_LAST_PATH_SETTING"; //$NON-NLS-1$
	private static final String LAST_SUBDIR_SETTING = "EXT_FOLDER_LAST_SUBDIR_SETTING"; //$NON-NLS-1$

	private String fDirectory;
	private boolean fSearchSubfolders = true;

	private Text fDirText;
	private Button fSubfoldersButton;

	private boolean fNewContainer = true;

	/**
	 * Creates a dialog to select a new file system folder.
	 *
	 * @param shell shell
	 */
	public DirectorySourceContainerDialog(Shell shell) {
		this(shell, IInternalDebugCoreConstants.EMPTY_STRING,
				PlatformUI.getDialogSettingsProvider(FrameworkUtil.getBundle(DirectorySourceContainerDialog.class))
						.getDialogSettings().getBoolean(LAST_SUBDIR_SETTING));
		fNewContainer = true;
	}

	/**
	 * Creates a dialog to edit file system folder.
	 *
	 * @param shell shell
	 * @param directory directory to edit or empty string
	 * @param searchSubfolders whether the search sub-folders button should be checked
	 * @param newContainer
	 */
	public DirectorySourceContainerDialog(Shell shell, String directory, boolean searchSubfolders) {
		super(shell);
		setShellStyle(getShellStyle() | SWT.RESIZE );
		fDirectory = directory;
		fSearchSubfolders = searchSubfolders;
		fNewContainer = false;
	}

	/**
	 * Returns the result of the dialog.open() operation
	 * @return the dialog.open() result
	 */
	public String getDirectory() {
		return fDirectory;
	}

	/**
	 * Returns whether the 'search subfolders' option is selected.
	 *
	 * @return whether the 'search subfolders' option is selected
	 */
	public boolean isSearchSubfolders() {
		return fSearchSubfolders;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Image image = (fNewContainer) ? DebugPluginImages.getImage(IInternalDebugUIConstants.IMG_ADD_SRC_DIR_WIZ) :
			DebugPluginImages.getImage(IInternalDebugUIConstants.IMG_EDIT_SRC_DIR_WIZ);
		setTitle(SourceLookupUIMessages.DirectorySourceContainerDialog_2);
		setMessage(SourceLookupUIMessages.DirectorySourceContainerDialog_3);
		setTitleImage(image);
		Composite parentComposite = (Composite)super.createDialogArea(parent);
		Font font = parentComposite.getFont();
		Composite composite = new Composite(parentComposite, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		composite.setFont(font);

		Composite dirComposite = new Composite(composite, SWT.NONE);
		layout = new GridLayout(2, false);
		dirComposite.setLayout(layout);
		dirComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		dirComposite.setFont(font);

		Label label = new Label(dirComposite, SWT.NONE);
		label.setText(SourceLookupUIMessages.DirectorySourceContainerDialog_4);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		label.setLayoutData(data);
		label.setFont(font);

		fDirText = new Text(dirComposite, SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 1;
		fDirText.setLayoutData(data);
		fDirText.setFont(font);
		fDirText.addModifyListener(e -> validate());

		Button button = new Button(dirComposite, SWT.PUSH);
		button.setText(SourceLookupUIMessages.DirectorySourceContainerDialog_5);
		data = new GridData();
		int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		Point minSize = button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		data.widthHint = Math.max(widthHint, minSize.x);
		button.setLayoutData(data);
		button.setFont(JFaceResources.getDialogFont());
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				browse();
			}
		});

		fSubfoldersButton = new Button(composite, SWT.CHECK);
		fSubfoldersButton.setText(SourceLookupUIMessages.DirectorySourceContainerDialog_6);

		return parentComposite;
	}

	@Override
	protected void configureShell(Shell newShell) {
		String title = null;
		if (fNewContainer) {
			title = SourceLookupUIMessages.DirectorySourceContainerDialog_7;
		} else {
			title = SourceLookupUIMessages.DirectorySourceContainerDialog_8;
		}
		newShell.setText(title);
		super.configureShell( newShell );
	}

	@Override
	protected Control createContents(Composite parent) {
		Control c = super.createContents(parent);
		fDirText.setText(fDirectory);
		fSubfoldersButton.setSelection(fSearchSubfolders);
		validate();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(c, IDebugHelpContextIds.SELECT_DIRECTORY_SOURCE_CONTAINER_DIALOG);
		return c;
	}

	@Override
	protected void okPressed() {
		fDirectory = fDirText.getText().trim();
		fSearchSubfolders = fSubfoldersButton.getSelection();
		IDialogSettings dialogSettings = PlatformUI
				.getDialogSettingsProvider(FrameworkUtil.getBundle(DirectorySourceContainerDialog.class))
				.getDialogSettings();
		dialogSettings.put(LAST_PATH_SETTING, fDirectory);
		dialogSettings.put(LAST_SUBDIR_SETTING, fSearchSubfolders);
		super.okPressed();
	}

	private void browse() {
		String last = fDirText.getText().trim();
		if (last.length() == 0) {
			IDialogSettings dialogSettings = PlatformUI
					.getDialogSettingsProvider(FrameworkUtil.getBundle(DirectorySourceContainerDialog.class))
					.getDialogSettings();
			last = dialogSettings.get(LAST_PATH_SETTING);
		}
		if (last == null) {
			last = IInternalDebugCoreConstants.EMPTY_STRING;
		}
		DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.SINGLE | SWT.SHEET);
		dialog.setText(SourceLookupUIMessages.DirectorySourceContainerDialog_0);
		dialog.setMessage(SourceLookupUIMessages.DirectorySourceContainerDialog_1);
		dialog.setFilterPath(last);
		String result = dialog.open();
		if (result == null) {
			return;
		}
		fDirText.setText(result);
	}

	private void validate() {
		File file = new File(fDirText.getText().trim());
		getButton(IDialogConstants.OK_ID).setEnabled(file.isDirectory() && file.exists());
	}
}
