/*******************************************************************************
 * Copyright (c) 2005, 2013 IBM Corporation and others.
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
 *     Bjorn Freeman-Benson - initial API and implementation
 *     Mohamed Hussein (Mentor Graphics) - Added s/getWarningMessage (Bug 386673)
 *******************************************************************************/
package org.eclipse.debug.examples.ui.pda.launcher;


import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.examples.core.pda.DebugCorePlugin;
import org.eclipse.debug.examples.ui.pda.DebugUIPlugin;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ResourceListSelectionDialog;


/**
 * Tab to specify the PDA program to run/debug.
 */
public class PDAMainTab extends AbstractLaunchConfigurationTab {

	private Text fProgramText;
	private Button fProgramButton;

	@Override
	public void createControl(Composite parent) {
		Font font = parent.getFont();

		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);
		GridLayout topLayout = new GridLayout();
		topLayout.verticalSpacing = 0;
		topLayout.numColumns = 3;
		comp.setLayout(topLayout);
		comp.setFont(font);

		createVerticalSpacer(comp, 3);

		Label programLabel = new Label(comp, SWT.NONE);
		programLabel.setText("&Program:"); //$NON-NLS-1$
		GridData gd = new GridData(GridData.BEGINNING);
		programLabel.setLayoutData(gd);
		programLabel.setFont(font);

		fProgramText = new Text(comp, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fProgramText.setLayoutData(gd);
		fProgramText.setFont(font);
		fProgramText.addModifyListener(event -> updateLaunchConfigurationDialog());

		fProgramButton = createPushButton(comp, "&Browse...", null); //$NON-NLS-1$
		fProgramButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				browsePDAFiles();
			}
		});
	}

	/**
	 * Open a resource chooser to select a PDA program
	 */
	protected void browsePDAFiles() {
		ResourceListSelectionDialog dialog = new ResourceListSelectionDialog(getShell(), ResourcesPlugin.getWorkspace().getRoot(), IResource.FILE);
		dialog.setTitle("PDA Program"); //$NON-NLS-1$
		dialog.setMessage("Select PDA Program"); //$NON-NLS-1$
		if (dialog.open() == Window.OK) {
			Object[] files = dialog.getResult();
			IFile file = (IFile) files[0];
			fProgramText.setText(file.getFullPath().toString());
		}

	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		//#ifdef ex1
//#		// TODO: Exercise 1 - retrieve the program path attribute from the launch configuration
//#		String program = null;
//#		if (program != null) {
//#			fProgramText.setText(program);
//#		}
		//#else
		try {
			String program = null;
			program = configuration.getAttribute(DebugCorePlugin.ATTR_PDA_PROGRAM, (String)null);
			if (program != null) {
				fProgramText.setText(program);
			}
		} catch (CoreException e) {
			setErrorMessage(e.getMessage());
		}
		//#endif
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		String program = fProgramText.getText().trim();
		if (program.length() == 0) {
			program = null;
		}
		//#ifdef ex1
//#		// TODO: Exercise 1 - update the launch configuration with the path to
//#		//   currently specified program
		//#else
		configuration.setAttribute(DebugCorePlugin.ATTR_PDA_PROGRAM, program);
		//#endif

		// perform resource mapping for contextual launch
		IResource[] resources = null;
		if (program!= null) {
			IPath path = IPath.fromOSString(program);
			IResource res = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
			if (res != null) {
				resources = new IResource[]{res};
			}
		}
		configuration.setMappedResources(resources);
	}

	@Override
	public String getName() {
		return "Main"; //$NON-NLS-1$
	}

	@Override
	public boolean isValid(ILaunchConfiguration launchConfig) {
		setErrorMessage(null);
		setWarningMessage(null);
		setMessage(null);
		String text = fProgramText.getText();
		//#ifdef ex1
//#		// TODO: Exercise 1 - validate the currently specified program exists and is not
//#		//	empty, providing the user with feedback.
		//#else
		if (text.length() > 0) {
			IPath path = IPath.fromOSString(text);
			IResource member = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
			if (member == null) {
				setErrorMessage("Specified program does not exist"); //$NON-NLS-1$
				return false;
			} else if (member.getType() != IResource.FILE) {
				setWarningMessage("Specified program is not a file."); //$NON-NLS-1$
			}
		} else {
			setMessage("Specify a program"); //$NON-NLS-1$
		}
		//#endif
		return true;
	}

	@Override
	public Image getImage() {
		return DebugUIPlugin.getDefault().getImageRegistry().get(DebugUIPlugin.IMG_OBJ_PDA);
	}
}
