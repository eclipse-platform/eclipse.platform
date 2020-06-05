/*******************************************************************************
 * Copyright (c) 2010, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.wizards;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.internal.core.patch.DiffProject;
import org.eclipse.compare.internal.patch.WorkspacePatcher;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;

public class PatchInaccessibleProjectsPage extends WizardPage {

	private CheckboxTableViewer checkList;
	private Button checkAllButton;
	private Button uncheckAllButton;

	private WorkspacePatcher fPatcher;

	public final static String PATCH_INACCESSIBLE_PROJECTS_NAME = "PatchInaccessibleProjectsPage"; //$NON-NLS-1$

	public PatchInaccessibleProjectsPage(WorkspacePatcher patcher) {
		super(PATCH_INACCESSIBLE_PROJECTS_NAME,
				TeamUIMessages.PatchInaccessibleProjectsPage_title, null);
		setMessage(TeamUIMessages.PatchInaccessibleProjectsPage_message);
		fPatcher = patcher;
	}

	@Override
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout(3, false));
		composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL
				| GridData.HORIZONTAL_ALIGN_FILL));
		setControl(composite);
		Font parentFont = composite.getFont();

		checkList = CheckboxTableViewer.newCheckList(composite, SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.BORDER);
		checkList.setContentProvider(ArrayContentProvider.getInstance());
		checkList.setLabelProvider(new WorkbenchLabelProvider() {
			@Override
			public Color getForeground(Object element) {
				if (element instanceof IProject
						&& !((IProject) element).exists())
					return Display.getCurrent().getSystemColor(
							SWT.COLOR_WIDGET_NORMAL_SHADOW);
				return super.getForeground(element);
			}

			@Override
			protected String decorateText(String input, Object element) {
				if (element instanceof IProject
						&& !((IProject) element).exists())
					return input
							+ NLS.bind(
									TeamUIMessages.PatchInaccessibleProjectsPage_projectDoesNotExistInWorkspace,
									""); //$NON-NLS-1$
				return input;
			}
		});
		checkList.addCheckStateListener(event -> {
			IProject project = (IProject) event.getElement();
			if (event.getChecked() && !project.exists())
				checkList.setChecked(project, false);
		});
		checkList
				.setComparator(new ResourceComparator(ResourceComparator.NAME));

		Table table = checkList.getTable();
		GridData data = new GridData(GridData.VERTICAL_ALIGN_FILL
				| GridData.HORIZONTAL_ALIGN_FILL);
		data.horizontalSpan = 3;
		data.grabExcessHorizontalSpace = true;
		data.grabExcessVerticalSpace = true;
		table.setLayoutData(data);

		checkAllButton = new Button(composite, SWT.NONE);
		checkAllButton
				.setText(TeamUIMessages.PatchInaccessibleProjectsPage_selectExisting);
		checkAllButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setAllChecked(true);
			}
		});
		checkAllButton.setFont(parentFont);
		setButtonLayoutData(checkAllButton);

		uncheckAllButton = new Button(composite, SWT.NONE);
		uncheckAllButton
				.setText(TeamUIMessages.PatchInaccessibleProjectsPage_deselectAll);
		uncheckAllButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setAllChecked(false);
			}
		});
		uncheckAllButton.setFont(parentFont);
		setButtonLayoutData(uncheckAllButton);

		updateControls();
	}

	private void updateControls() {
		DiffProject[] diffProjects = fPatcher.getDiffProjects();
		List<IProject> projects = new ArrayList<>();
		if (diffProjects != null) {
			for (DiffProject diffProject : diffProjects) {
				IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(diffProject.getName());
				if (!project.isAccessible())
					projects.add(project);
			}
		}
		checkList.setInput(projects.toArray(new IProject[] {}));
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible)
			updateControls();
	}

	@Override
	public IWizardPage getNextPage() {
		// Skipping the patch parsed page in case this one is displayed
		Control control = getControl();
		if (control != null && control.isVisible())
			return null;
		return super.getNextPage();
	}

	public IProject[] getSelectedProjects() {
		Object elements[] = checkList.getCheckedElements();
		List<IProject> projects = new ArrayList<>();
		for (Object element : elements) {
			projects.add((IProject) element);
		}
		return projects.toArray(new IProject[] {});
	}

	private void setAllChecked(boolean checked) {
		int count = checkList.getTable().getItemCount();
		for (int i = 0; i < count; i++) {
			IProject project = (IProject) checkList.getElementAt(i);
			if (project.exists())
				checkList.setChecked(project, checked);
		}
	}

}
