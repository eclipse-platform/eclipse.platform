/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui;

import java.util.Map;
import java.util.Properties;

import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.connection.CVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.ui.wizards.AlternativeLocationWizard;
import org.eclipse.ui.PlatformUI;

public class ConfigureRepositoryLocationsDialog extends TitleAreaDialog {

	private Image dlgTitleImage;
	private ConfigureRepositoryLocationsTable fConfigureRepositoryLocationsTable;

	/**
	 * Creates a new AlternativeRepositoryDialog.
	 * 
	 * @param parentShell
	 *            the parent shell
	 * @param alternativesMap
	 *            Map with a repository location (ICVSRepositoryLocation) from
	 *            the Team Project Set as a key and list of alternatives found
	 *            (also ICVSRepositoryLocation) as a value.
	 * @param message
	 *            a message to display to the user
	 */
	public ConfigureRepositoryLocationsDialog(Shell parentShell, Map alternativesMap) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		fConfigureRepositoryLocationsTable = new ConfigureRepositoryLocationsTable(
				alternativesMap);
	}

	@Override
	protected Control createContents(Composite parent) {
		Control contents = super.createContents(parent);
		setTitle(CVSUIMessages.ConfigureRepositoryLocationsWizard_title);
		setMessage(CVSUIMessages.ConfigureRepositoryLocationsWizard_message);
		dlgTitleImage = CVSUIPlugin.getPlugin().getImageDescriptor(
				ICVSUIConstants.IMG_WIZBAN_NEW_LOCATION).createImage();
		setTitleImage(dlgTitleImage);
		return contents;
	}

	@Override
	public boolean close() {
		if (dlgTitleImage != null) {
			dlgTitleImage.dispose();
		}
		return super.close();
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(CVSUIMessages.ConfigureRepositoryLocationsWizard_title);
		// set F1 help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(shell,
				IHelpContextIds.ALTERNATIVE_REPOSITORY_DIALOG);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		initializeDialogUnits(parent);

		final Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));

		GridData childData = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(childData);

		Composite table = fConfigureRepositoryLocationsTable.createControl(composite);
		GridData gridData = new GridData(GridData.VERTICAL_ALIGN_FILL);
		gridData.horizontalSpan = 2;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		table.setLayoutData(gridData);
		
		final Button showOnlyCompatibleLocationsButton = new Button(composite,
				SWT.CHECK);
		showOnlyCompatibleLocationsButton
				.setText(CVSUIMessages.ConfigureRepositoryLocationsWizard_showOnlyCompatible);
		showOnlyCompatibleLocationsButton.setSelection(true);
		showOnlyCompatibleLocationsButton.addListener(SWT.Selection,
				event -> fConfigureRepositoryLocationsTable
						.setShowOnlyCompatibleLocations(showOnlyCompatibleLocationsButton
								.getSelection()));
		showOnlyCompatibleLocationsButton.setLayoutData(new GridData(
				SWT.BEGINNING, SWT.NONE, false, false));
		
		final Button createLocationButton = new Button(composite, SWT.PUSH);
		createLocationButton
				.setText(CVSUIMessages.ConfigureRepositoryLocationsWizard_createLocation);
		createLocationButton
				.setToolTipText(CVSUIMessages.ConfigureRepositoryLocationsWizard_createLocationTooltip);
		createLocationButton.addListener(SWT.Selection, event -> {

			CVSRepositoryLocation selectedAlternativeRepository = fConfigureRepositoryLocationsTable
					.getSelectedAlternativeRepository();

			Properties properties = new Properties();
			properties
					.put(
							"connection", selectedAlternativeRepository.getMethod().getName()); //$NON-NLS-1$
			properties.put(
					"user", selectedAlternativeRepository.getUsername()); //$NON-NLS-1$
			// TODO: retrieve password (if available) and use it to prime
			// the field
			// properties.put("password", ""); //$NON-NLS-1$
			properties.put("host", selectedAlternativeRepository.getHost()); //$NON-NLS-1$
			int port = selectedAlternativeRepository.getPort();
			if (port != ICVSRepositoryLocation.USE_DEFAULT_PORT)
				properties.put("port", String.valueOf(port)); //$NON-NLS-1$
			properties
					.put(
							"root", selectedAlternativeRepository.getRootDirectory()); //$NON-NLS-1$

			AlternativeLocationWizard wizard = new AlternativeLocationWizard(
					properties);
			wizard.setSwitchPerspectives(false);
			WizardDialog dialog = new ConfigureRepositoryLocationsWizardDialog(
					getShell(), wizard);
			dialog.open();

			ICVSRepositoryLocation location = wizard.getLocation();
			if (location != null)
				fConfigureRepositoryLocationsTable
						.addAlternativeRepository(location);
		});
		createLocationButton.setEnabled(fConfigureRepositoryLocationsTable
				.getSelection().getFirstElement() != null);
		createLocationButton.setLayoutData(new GridData(SWT.END, SWT.NONE,
				false, false));
		
		fConfigureRepositoryLocationsTable.getViewer().addSelectionChangedListener(
				event -> {
					IStructuredSelection sel = event.getStructuredSelection();
					Object firstElement = sel.getFirstElement();
					createLocationButton.setEnabled(firstElement != null);
				});

		return composite;
	}

	public Map getSelected() {
		return fConfigureRepositoryLocationsTable.getSelected();
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		String sectionName = getClass().getName() + "_dialogBounds"; //$NON-NLS-1$
		IDialogSettings settings = CVSUIPlugin.getPlugin().getDialogSettings();
		IDialogSettings section = settings.getSection(sectionName);
		if (section == null) {
			section = settings.addNewSection(sectionName);
			section.put("DIALOG_HEIGHT", 300); //$NON-NLS-1$
			section.put("DIALOG_WIDTH", 600); //$NON-NLS-1$
		}
		return section;
	}

	@Override
	protected int getDialogBoundsStrategy() {
		return DIALOG_PERSISTLOCATION | DIALOG_PERSISTSIZE;
	}
	
	/**
	 * This class is made only to change Wizard's default "Finish" button label
	 * to "Create".
	 */
	private class ConfigureRepositoryLocationsWizardDialog extends WizardDialog {

		public ConfigureRepositoryLocationsWizardDialog(Shell parentShell,
				IWizard newWizard) {
			super(parentShell, newWizard);
		}

		@Override
		protected Button createButton(Composite parent, int id, String label,
				boolean defaultButton) {
			if (id == IDialogConstants.FINISH_ID)
				label = CVSUIMessages.ConfigureRepositoryLocationsWizardDialog_finish;
			return super.createButton(parent, id, label, defaultButton);
		}

	}
}
