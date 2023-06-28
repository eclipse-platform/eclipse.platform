/*******************************************************************************
 * Copyright (c) 2003, 2009 IBM Corporation and others.
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
package org.eclipse.debug.ui.sourcelookup;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.sourcelookup.SourceLookupPanel;
import org.eclipse.debug.internal.ui.sourcelookup.SourceLookupUIMessages;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.FrameworkUtil;

/**
 * A dialog for editing the source lookup path of a
 * source lookup director.
 * <p>
 * This class may be instantiated.
 * </p>
 * @since 3.0
 * @noextend This class is not intended to be subclassed by clients.
 */
public class SourceLookupDialog extends TitleAreaDialog {

	private SourceLookupPanel fPanel;
	private ISourceLookupDirector fDirector;

	/**
	 * Constructs a dialog to edit the source lookup path managed by the
	 * given source lookup director. Persists the resulting source lookup
	 * path on the launch configuration associated with the given source
	 * lookup director.
	 *
	 * @param shell shell to parent the dialog
	 * @param director source lookup director managing the source lookup
	 *  path to be edited
	 */
	public SourceLookupDialog(Shell shell, ISourceLookupDirector director) {
		super(shell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		fDirector = director;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		// create a composite with standard margins and spacing
		setTitle(SourceLookupUIMessages.manageSourceDialog_description);
		setTitleImage(DebugPluginImages.getImage(IInternalDebugUIConstants.IMG_EDIT_SRC_LOC_WIZ));
		setMessage(SourceLookupUIMessages.SourceLookupDialog_add_edit_remove);
		Composite composite = new Composite(parent, SWT.NONE);

		GridLayout layout = new GridLayout();
		layout.marginHeight =
			convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth =
			convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		layout.verticalSpacing =
			convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		layout.horizontalSpacing =
			convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);
		composite.setFont(parent.getFont());
		fPanel = new SourceLookupPanel();
		fPanel.createControl(composite);
		fPanel.initializeFrom(fDirector);
		Dialog.applyDialogFont(composite);
		ILaunchConfiguration config = fDirector.getLaunchConfiguration();
		if(config != null && config.isReadOnly()) {
			setErrorMessage(SourceLookupUIMessages.SourceLookupDialog_0+config.getName()+SourceLookupUIMessages.SourceLookupDialog_1);
		}
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite,  IDebugHelpContextIds.EDIT_SOURCELOOKUP_DIALOG);

		return composite;
	}

	@Override
	protected void okPressed() {
		ILaunchConfiguration config = fDirector.getLaunchConfiguration();
		ILaunchConfigurationWorkingCopy copy = null;
		if(config != null) {
			try {
				copy = config.getWorkingCopy();
				fPanel.performApply(copy);
				copy.doSave();
			}
			catch (CoreException e) {DebugUIPlugin.log(e);}
		}
		super.okPressed();
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(SourceLookupUIMessages.manageSourceDialog_title);
	}

	@Override
	public boolean close() {
		fPanel.dispose();
		return super.close();
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		IDialogSettings settings = PlatformUI
				.getDialogSettingsProvider(FrameworkUtil.getBundle(SourceLookupDialog.class)).getDialogSettings();
		IDialogSettings section = settings.getSection(getClass().getName());
		if (section == null) {
			section = settings.addNewSection(getClass().getName());
		}
		return section;
	}
}
