/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.team.internal.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.resources.team.FileModificationValidationContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.core.DefaultFileModificationValidator;
import org.eclipse.team.internal.ui.dialogs.DetailsDialog;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Override the default file modification validator to prompt to
 * make read-only files writable
 */
public class DefaultUIFileModificationValidator extends DefaultFileModificationValidator {

	public static class FileListDialog extends DetailsDialog {

		private final IFile[] files;

		public static boolean openQuestion(Shell shell, IFile[] files) {
			FileListDialog dialog = new FileListDialog(shell, files);
			int code = dialog.open();
			return code == OK;
		}

		public FileListDialog(Shell parentShell, IFile[] files) {
			super(parentShell, TeamUIMessages.DefaultUIFileModificationValidator_0);
			this.files = files;
			setImageKey(DLG_IMG_WARNING);
		}

		@Override
		protected void createMainDialogArea(Composite parent) {
			createWrappingLabel(parent, TeamUIMessages.DefaultUIFileModificationValidator_1);
		}

		@Override
		protected Composite createDropDownDialogArea(Composite parent) {
			Composite composite = createComposite(parent);
			createWrappingLabel(composite, TeamUIMessages.DefaultUIFileModificationValidator_2);
			org.eclipse.swt.widgets.List fileList = new org.eclipse.swt.widgets.List(composite, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
			GridData data = new GridData ();
			data.heightHint = 75;
			data.horizontalAlignment = GridData.FILL;
			data.grabExcessHorizontalSpace = true;
			fileList.setLayoutData(data);
			fileList.setFont(parent.getFont());
			for (IFile file : files) {
				fileList.add(file.getFullPath().toString());
			}
			return composite;
		}

		@Override
		protected void updateEnablements() {
			// Nothing to do
		}

		@Override
		protected boolean includeCancelButton() {
			return false;
		}

		@Override
		protected boolean includeOkButton() {
			return false;
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			createButton(parent, IDialogConstants.YES_ID, IDialogConstants.YES_LABEL, true);
			createButton(parent, IDialogConstants.NO_ID, IDialogConstants.NO_LABEL, true);
			super.createButtonsForButtonBar(parent);
		}

		@Override
		protected void buttonPressed(int id) {
			if (IDialogConstants.YES_ID == id) {
				okPressed();
			} else if (IDialogConstants.NO_ID == id) {
				cancelPressed();
			} else {
				super.buttonPressed(id);
			}
		}
	}

	@Override
	public IStatus validateEdit(final IFile[] allFiles, FileModificationValidationContext context) {
		final IFile[] readOnlyFiles = getReadOnlyFiles(allFiles);
		if (readOnlyFiles.length > 0 && context != null) {
			final Shell shell = getShell(context);
			final boolean[] ok = new boolean[] { false };
			if (readOnlyFiles.length == 1) {
				syncExec(() -> ok[0] = MessageDialog.openQuestion(shell, TeamUIMessages.DefaultUIFileModificationValidator_3, NLS.bind(TeamUIMessages.DefaultUIFileModificationValidator_4, new String[] { readOnlyFiles[0].getFullPath().toString() })));
			} else {
				syncExec(() -> ok[0] = FileListDialog.openQuestion(shell, readOnlyFiles));
			}
			if (ok[0]) {
				setWritable(readOnlyFiles);
			}
		} else if (readOnlyFiles.length > 0 && context == null) {
			if (isMakeWrittableWhenContextNotProvided()) {
				setWritable(readOnlyFiles);
			}
		}
		return getStatus(readOnlyFiles);
	}

	private Shell getShell(FileModificationValidationContext context) {
		if (context.getShell() != null)
			return (Shell)context.getShell();
		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
		if (window != null) {
			return window.getShell();
		}
		return null;
	}

	private static void syncExec(Runnable runnable) {
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(runnable);
	}

	@Override
	public IStatus validateSave(IFile file) {
		if (file.isReadOnly() && isMakeWrittableWhenContextNotProvided()) {
			IFile[] readOnlyFiles = new IFile[] { file };
			setWritable(readOnlyFiles);
			return getStatus(readOnlyFiles);
		} else {
			return getDefaultStatus(file);
		}
	}

	private boolean isMakeWrittableWhenContextNotProvided() {
		return TeamUIPlugin.getPlugin().getPreferenceStore().getBoolean(IPreferenceIds.MAKE_FILE_WRITTABLE_IF_CONTEXT_MISSING);
	}

	private IFile[] getReadOnlyFiles(IFile[] files) {
		List<IFile> result = new ArrayList<>();
		for (IFile file : files) {
			if (file.isReadOnly()) {
				result.add(file);
			}
		}
		return result.toArray(new IFile[result.size()]);
	}

	protected IStatus setWritable(final IFile[] files) {
		for (IFile file : files) {
			ResourceAttributes attributes = file.getResourceAttributes();
			if (attributes != null) {
				attributes.setReadOnly(false);
			}
			try {
				file.setResourceAttributes(attributes);
			} catch (CoreException e) {
				return e.getStatus();
			}
		}
		return Status.OK_STATUS;
	}
}
