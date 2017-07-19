/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Brock Janiczak  (brockj@tpg.com.au) - Bug 144067 Repository types not sorted in the share project wizard
 *******************************************************************************/
package org.eclipse.team.internal.ui.wizards;


import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.internal.ui.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.activities.ITriggerPoint;
import org.eclipse.ui.activities.WorkbenchActivityHelper;
import org.eclipse.ui.model.*;

/**
 * The main page of the configure project wizard. It contains a table
 * which lists possible team providers with which to configure the project.
 * The user may select one and press "Next", which will display a provider-
 * specific wizard page.
 */
public class ConfigureProjectWizardMainPage extends WizardPage {
	private Table table;
	private Button showAllToggle;
	private TableViewer viewer;
	private AdaptableList wizards;
	private AdaptableList disabledWizards;
	private IProject[] projects;
	private String description;

	private IWizard selectedWizard;

	private IDialogSettings settings;
	private final static String SELECTED_WIZARD_ID = "selectedWizardId"; //$NON-NLS-1$
	private String selectedWizardId;

	/**
	 * Create a new ConfigureProjectWizardMainPage
	 *
	 * @param pageName  the name of the page
	 * @param title  the title of the page
	 * @param titleImage  the image for the page title
	 * @param wizards  the wizards to populate the table with
	 * @param disabledWizards the list of wizards that are disabled via capabilities
	 */
	public ConfigureProjectWizardMainPage(String pageName, String title, ImageDescriptor titleImage, AdaptableList wizards, AdaptableList disabledWizards) {
		this(pageName,title,titleImage,wizards,disabledWizards, TeamUIMessages.ConfigureProjectWizardMainPage_selectRepository);
	}

	/**
	 * Create a new ConfigureProjectWizardMainPage
	 *
	 * @param pageName  the name of the page
	 * @param title  the title of the page
	 * @param titleImage  the image for the page title
	 * @param wizards  the wizards to populate the table with
	 * @param disabledWizards the list of wizards that are disabled via capabilities
	 * @param description The string to use as a description label
	 */
	public ConfigureProjectWizardMainPage(String pageName, String title, ImageDescriptor titleImage, AdaptableList wizards, AdaptableList disabledWizards, String description) {
		super(pageName, title, titleImage);
		this.wizards = wizards;
		this.disabledWizards = disabledWizards;
		this.description = description;
	}

	public IWizard getSelectedWizard() {
		return selectedWizard;
	}

	@Override
	public boolean canFlipToNextPage() {
		return selectedWizard != null && selectedWizard.getPageCount() > 0;
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		setControl(composite);

		// set F1 help
        PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.SHARE_PROJECT_PAGE);

		Label label = new Label(composite, SWT.LEFT);
		label.setText(description);
		GridData data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		label.setLayoutData(data);

		table = new Table(composite, SWT.SINGLE | SWT.BORDER);
		data = new GridData(GridData.FILL_BOTH);
		data.heightHint = table.getItemHeight() * 7;
		table.setLayoutData(data);
		viewer = new TableViewer(table);
		viewer.setContentProvider(new WorkbenchContentProvider());
		viewer.setLabelProvider(new WorkbenchLabelProvider());
		viewer.addSelectionChangedListener(event -> {
			// Initialize the wizard so we can tell whether to enable the Next button
			ISelection selection = event.getSelection();
			if (selection == null || !(selection instanceof IStructuredSelection)) {
				selectedWizard = null;
				selectedWizardId = null;
				setPageComplete(false);
				return;
			}
			IStructuredSelection ss = (IStructuredSelection)selection;
			if (ss.size() != 1) {
				selectedWizard = null;
				selectedWizardId = null;
				setPageComplete(false);
				return;
			}
			ConfigurationWizardElement selectedElement = (ConfigurationWizardElement)ss.getFirstElement();
			try {
				selectedWizard = selectedElement.createExecutableExtension(getUnsharedProjects());
				selectedWizardId = selectedElement.getID();
			} catch (CoreException e) {
				return;
			}
			selectedWizard.addPages();

			// Ask the container to update button enablement
			setPageComplete(true);
		});
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				getWizard().getContainer().showPage(getNextPage());
			}
		});
		viewer.setComparator(new ViewerComparator() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				if (e1 instanceof ConfigurationWizardElement && e2 instanceof ConfigurationWizardElement) {
					ConfigurationWizardElement wizard1 = (ConfigurationWizardElement) e1;
					ConfigurationWizardElement wizard2 = (ConfigurationWizardElement) e2;
					return wizard1.getLabel(wizard1).compareToIgnoreCase(wizard2.getLabel(wizard2));
				}
				return super.compare(viewer, e1, e2);
			}
		});

		if(disabledWizards.size() > 0) {
			showAllToggle = new Button(composite, SWT.CHECK);
			showAllToggle.setText(TeamUIMessages.ConfigureProjectWizard_showAll);
			showAllToggle.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					ArrayList all = new ArrayList(Arrays.asList(wizards.getChildren()));
					if(showAllToggle.getSelection()) {
						all.addAll(Arrays.asList(disabledWizards.getChildren()));
					}
					viewer.setInput(new AdaptableList(all));
				}
			});
		}

		if(wizards.size() == 0 && showAllToggle != null) {
			showAllToggle.setSelection(true);
			ArrayList all = new ArrayList(Arrays.asList(wizards.getChildren()));
			all.addAll(Arrays.asList(disabledWizards.getChildren()));
			viewer.setInput(new AdaptableList(all));
		} else {
			viewer.setInput(wizards);
		}
		initializeWizardSelection();
        Dialog.applyDialogFont(parent);
	}

	/* package */ IProject[] getUnsharedProjects() {
		java.util.List unshared = new ArrayList();
		for (int i = 0; i < projects.length; i++) {
			IProject project = projects[i];
			if (!RepositoryProvider.isShared(project))
				unshared.add(project);
		}
		return (IProject[]) unshared.toArray(new IProject[unshared.size()]);
	}

	/**
	 * The <code>WizardSelectionPage</code> implementation of
	 * this <code>IWizardPage</code> method returns the first page
	 * of the currently selected wizard if there is one.
	 *
	 * @see WizardPage#getNextPage
	 */
	@Override
	public IWizardPage getNextPage() {
		if (selectedWizard == null) return null;
		if(! WorkbenchActivityHelper.allowUseOf(getTriggerPoint(), ((IStructuredSelection)viewer.getSelection()).getFirstElement())) return null;
		return selectedWizard.getStartingPage();
	}

	private ITriggerPoint getTriggerPoint() {
		return PlatformUI.getWorkbench()
			.getActivitySupport().getTriggerPointManager()
			.getTriggerPoint(TeamUIPlugin.TRIGGER_POINT_ID);
	}

	public void setProjects(IProject[] projects) {
		this.projects = projects;
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			table.setFocus();
		}
	}

	private void initializeWizardSelection() {
		String selectedWizardId = null;

		IDialogSettings dialogSettings = TeamUIPlugin.getPlugin().getDialogSettings();
		this.settings = dialogSettings.getSection("ConfigureProjectWizard"); //$NON-NLS-1$
		if (this.settings == null) {
			this.settings = dialogSettings.addNewSection("ConfigureProjectWizard"); //$NON-NLS-1$
		}
		if (settings != null)
			selectedWizardId = settings.get(SELECTED_WIZARD_ID);

		if (selectedWizardId==null)
			return;

		// TODO: any checks here?
		Object[] children = ((AdaptableList) viewer.getInput()).getChildren();

		for (int i = 0; i < children.length; i++) {
			try {
				ConfigurationWizardElement element = (ConfigurationWizardElement)children[i];
				if (element.getID().equals(selectedWizardId)) {
					viewer.setSelection(new StructuredSelection(element));
					return;
				}
			} catch(ClassCastException e) {
				// ignore
			}
		}
	}

	/*package*/ void performFinish() {
		settings.put(SELECTED_WIZARD_ID, selectedWizardId);
	}
}
