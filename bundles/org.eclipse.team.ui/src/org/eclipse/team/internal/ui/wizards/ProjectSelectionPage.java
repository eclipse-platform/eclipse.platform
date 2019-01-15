/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.internal.ui.ITeamUIImages;
import org.eclipse.team.internal.ui.SWTUtils;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.IConfigurationWizard;
import org.eclipse.team.ui.IConfigurationWizardExtension;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.AdaptableList;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;

/**
 * Page that supports the sharing of multiple projects for those repository providers
 * that have not adapted their {@link IConfigurationWizard} to {@link IConfigurationWizardExtension}.
 */
public class ProjectSelectionPage extends WizardPage {

	private final IProject[] projects;
	private final ConfigurationWizardElement element;
	private Button shareButton;
	private TableViewer projectViewer;
	private AdaptableList projectList;

	protected ProjectSelectionPage(IProject[] projects, ConfigurationWizardElement element) {
		super("projectSelectionPage", //$NON-NLS-1$
				NLS.bind(TeamUIMessages.ProjectSelectionPage_1, element.getLabel(null)),
				TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_WIZBAN_SHARE));
		setDescription(NLS.bind(TeamUIMessages.ProjectSelectionPage_0, element.getLabel(null)));
		this.projects = projects;
		this.element = element;
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = SWTUtils.createHVFillComposite(parent, SWTUtils.MARGINS_DIALOG, 2);
		createProjectList(composite);
		createShareButton(composite);
		updateEnablements();
		setControl(composite);
	}

	private void createProjectList(Composite composite) {
		projectViewer = new TableViewer(composite, SWT.SINGLE | SWT.BORDER);
		projectViewer.getControl().setLayoutData(SWTUtils.createHVFillGridData());
		projectList = new AdaptableList(projects);
		projectViewer.setContentProvider(new WorkbenchContentProvider());
		projectViewer.setLabelProvider(new WorkbenchLabelProvider());
		projectViewer.setComparator(new ResourceComparator(ResourceComparator.NAME));
		projectViewer.setInput(projectList);
		projectViewer.getTable().select(0);
		projectViewer.addSelectionChangedListener(event -> updateEnablements());
	}

	private void createShareButton(Composite composite) {
		shareButton = new Button(composite, SWT.PUSH);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.END;
		gridData.verticalAlignment = SWT.TOP;
		shareButton.setLayoutData(gridData);
		shareButton.setText(TeamUIMessages.ProjectSelectionPage_2);
		shareButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				shareSelectedProject();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// Ignore
			}
		});
	}

	/* package */ void shareSelectedProject() {
		IProject project = getSelectedProject();
		if (project != null) {
			try {
				IConfigurationWizard wizard = (IConfigurationWizard)element.createExecutableExtension();
				wizard.init(PlatformUI.getWorkbench(), project);
				ConfigureProjectWizard.openWizard(getShell(), wizard);
				updateProjectList(project);
				if (projectList.size() == 0) {
					// TODO: Can we close the outer wizard from here?
				}
			} catch (CoreException e) {
				ErrorDialog.openError(getShell(), null, null, e.getStatus());
			}
		}
	}

	private void updateProjectList(IProject project) {
		if (RepositoryProvider.isShared(project)) {
			projectList.remove(project);
			projectViewer.refresh();
			if (hasUnsharedProjects()) {
				projectViewer.getTable().select(0);
			}
			updateEnablements();
		}
	}

	/* package */ void updateEnablements() {
		shareButton.setEnabled(getSelectedProject() != null);
	}

	private IProject getSelectedProject() {
		ISelection selection = projectViewer.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection) selection;
			return (IProject)ss.getFirstElement();
		}
		return null;
	}

	public boolean hasUnsharedProjects() {
		return projectList.size() > 0;
	}

}
