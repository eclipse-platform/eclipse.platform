/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
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
package org.eclipse.debug.examples.ui.midi.launcher;


import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.examples.core.midi.launcher.MidiLaunchDelegate;
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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ResourceListSelectionDialog;


/**
 * Tab to specify the MIDI file to play.
 *
 * @since 1.0
 */
public class MidiMainTab extends AbstractLaunchConfigurationTab {

	private Text fFileText;
	private Button fFileButton;

	private Button fExceptions;
	private Button fHandled;
	private Button fUnhandled;

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
		programLabel.setText("&Midi File:"); //$NON-NLS-1$
		GridData gd = new GridData(GridData.BEGINNING);
		programLabel.setLayoutData(gd);
		programLabel.setFont(font);

		fFileText = new Text(comp, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fFileText.setLayoutData(gd);
		fFileText.setFont(font);
		fFileText.addModifyListener(event -> updateLaunchConfigurationDialog());

		fFileButton = createPushButton(comp, "&Browse...", null); //$NON-NLS-1$
		fFileButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				browseMidiFiles();
			}
		});

		new Label(comp, SWT.NONE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;

		Group test = new Group(comp, SWT.NONE);
		test.setText("Exceptions"); //$NON-NLS-1$
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		test.setLayoutData(gd);
		test.setLayout(new GridLayout());
		fExceptions = new Button(test, SWT.CHECK);
		fExceptions.setText("&Throw an exception during launch for testing purposes"); //$NON-NLS-1$
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		fExceptions.setLayoutData(gd);
		fExceptions.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fHandled.setEnabled(fExceptions.getSelection());
				fUnhandled.setEnabled(fExceptions.getSelection());
				updateLaunchConfigurationDialog();
			}
		});
		fHandled = new Button(test, SWT.RADIO);
		fHandled.setText("Throw a handled e&xception during launch to re-open launch dialog"); //$NON-NLS-1$
		SelectionAdapter sa = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateLaunchConfigurationDialog();
			}
		};
		fHandled.addSelectionListener(sa);
		fUnhandled = new Button(test, SWT.RADIO);
		fUnhandled.setText("Throw an &unhandled exception during launch to open error dialog"); //$NON-NLS-1$
		fUnhandled.addSelectionListener(sa);
	}

	/**
	 * Open a resource chooser to select a MIDI file
	 */
	protected void browseMidiFiles() {
		ResourceListSelectionDialog dialog = new ResourceListSelectionDialog(getShell(), ResourcesPlugin.getWorkspace().getRoot(), IResource.FILE);
		dialog.setTitle("MIDI File"); //$NON-NLS-1$
		dialog.setMessage("Select MIDI File"); //$NON-NLS-1$
		if (dialog.open() == Window.OK) {
			Object[] files = dialog.getResult();
			IFile file = (IFile) files[0];
			fFileText.setText(file.getFullPath().toString());
		}

	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			String file = null;
			file = configuration.getAttribute(MidiLaunchDelegate.ATTR_MIDI_FILE, (String)null);
			if (file != null) {
				fFileText.setText(file);
			}
			String excep = configuration.getAttribute(MidiLaunchDelegate.ATTR_THROW_EXCEPTION, (String)null);
			fExceptions.setSelection(excep != null);
			fHandled.setEnabled(excep != null);
			fUnhandled.setEnabled(excep != null);
			if (excep != null) {
				fHandled.setSelection(excep.equals(MidiLaunchDelegate.HANDLED));
				fUnhandled.setSelection(excep.equals(MidiLaunchDelegate.UNHANDLED));
			} else {
				fHandled.setSelection(true);
			}
		} catch (CoreException e) {
			setErrorMessage(e.getMessage());
		}
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		String file = fFileText.getText().trim();
		if (file.length() == 0) {
			file = null;
		}
		IResource[] resources = null;
		if (file!= null) {
			IPath path = IPath.fromOSString(file);
			IResource res = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
			if (res != null) {
				resources = new IResource[]{res};
			}
		}
		configuration.setAttribute(MidiLaunchDelegate.ATTR_MIDI_FILE, file);
		configuration.setMappedResources(resources);

		// exception handling
		if (fExceptions.getSelection()) {
			if (fHandled.getSelection()) {
				configuration.setAttribute(MidiLaunchDelegate.ATTR_THROW_EXCEPTION, MidiLaunchDelegate.HANDLED);
			} else {
				configuration.setAttribute(MidiLaunchDelegate.ATTR_THROW_EXCEPTION, MidiLaunchDelegate.UNHANDLED);
			}
		} else {
			configuration.removeAttribute(MidiLaunchDelegate.ATTR_THROW_EXCEPTION);
		}
	}

	@Override
	public String getName() {
		return "Main"; //$NON-NLS-1$
	}

	@Override
	public boolean isValid(ILaunchConfiguration launchConfig) {
		setErrorMessage(null);
		setMessage(null);
		String text = fFileText.getText();
		if (text.length() > 0) {
			IPath path = IPath.fromOSString(text);
			if (ResourcesPlugin.getWorkspace().getRoot().findMember(path) == null) {
				setErrorMessage("File does not exist"); //$NON-NLS-1$
				return false;
			}
		} else {
			setMessage("Select a MIDI file"); //$NON-NLS-1$
		}
		return true;
	}

	@Override
	public Image getImage() {
		return DebugUIPlugin.getDefault().getImageRegistry().get(DebugUIPlugin.IMG_OBJ_MIDI);
	}
}
