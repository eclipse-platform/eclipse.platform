/*******************************************************************************
 * Copyright (c) 2004, 2013 Richard Hoefter and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Richard Hoefter (richard.hoefter@web.de) - initial API and implementation, bug 95296, bug 288830
 *     IBM Corporation - adapted to wizard export page
 *******************************************************************************/

package org.eclipse.ant.internal.ui.datatransfer;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class AntBuildfileExportPage extends WizardPage {

	private CheckboxTableViewer fTableViewer;
	private final List<IJavaProject> fSelectedJavaProjects = new ArrayList<>();
	private Button compatibilityCheckbox;
	private Button compilerCheckbox;
	private Text buildfilenameText;
	private Text junitdirText;

	public AntBuildfileExportPage() {
		super("AntBuildfileExportWizardPage"); //$NON-NLS-1$
		setPageComplete(false);
		setTitle(DataTransferMessages.AntBuildfileExportPage_0);
		setDescription(DataTransferMessages.AntBuildfileExportPage_1);
	}

	@Override
	public void createControl(Composite parent) {

		initializeDialogUnits(parent);

		Composite workArea = new Composite(parent, SWT.NONE);
		setControl(workArea);

		workArea.setLayout(new GridLayout());
		workArea.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));

		Label titel = new Label(workArea, SWT.NONE);
		titel.setText(DataTransferMessages.AntBuildfileExportPage_2);

		Composite listComposite = new Composite(workArea, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 0;
		layout.makeColumnsEqualWidth = false;
		listComposite.setLayout(layout);

		listComposite.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.FILL_BOTH));

		// TODO set F1 help

		Table table = new Table(listComposite, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		fTableViewer = new CheckboxTableViewer(table);
		table.setLayout(new TableLayout());
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.heightHint = 300;
		table.setLayoutData(data);
		fTableViewer.setContentProvider(new WorkbenchContentProvider() {
			@Override
			public Object[] getElements(Object element) {
				if (element instanceof IJavaProject[]) {
					return (IJavaProject[]) element;
				}
				return null;
			}
		});
		fTableViewer.setLabelProvider(new WorkbenchLabelProvider());
		fTableViewer.addCheckStateListener(event -> {
			if (event.getChecked()) {
				fSelectedJavaProjects.add((IJavaProject) event.getElement());
			} else {
				fSelectedJavaProjects.remove(event.getElement());
			}
			updateEnablement();
		});

		initializeProjects();
		createSelectionButtons(listComposite);
		createCheckboxes(workArea);
		createTextFields(workArea);
		setControl(workArea);
		updateEnablement();
		Dialog.applyDialogFont(parent);
	}

	private void createSelectionButtons(Composite composite) {

		Composite buttonsComposite = new Composite(composite, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		buttonsComposite.setLayout(layout);

		buttonsComposite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

		Button selectAll = new Button(buttonsComposite, SWT.PUSH);
		selectAll.setText(DataTransferMessages.AntBuildfileExportPage_11);
		selectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (int i = 0; i < fTableViewer.getTable().getItemCount(); i++) {
					fSelectedJavaProjects.add((IJavaProject) fTableViewer.getElementAt(i));
				}
				fTableViewer.setAllChecked(true);
				updateEnablement();
			}
		});
		setButtonLayoutData(selectAll);

		Button deselectAll = new Button(buttonsComposite, SWT.PUSH);
		deselectAll.setText(DataTransferMessages.AntBuildfileExportPage_12);
		deselectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fSelectedJavaProjects.clear();
				fTableViewer.setAllChecked(false);
				updateEnablement();
			}
		});
		setButtonLayoutData(deselectAll);
	}

	private void createCheckboxes(Composite composite) {

		compatibilityCheckbox = new Button(composite, SWT.CHECK);
		compatibilityCheckbox.setSelection(true);
		compatibilityCheckbox.setText(DataTransferMessages.AntBuildfileExportPage_13);
		compatibilityCheckbox.setToolTipText(DataTransferMessages.AntBuildfileExportPage_14);

		compilerCheckbox = new Button(composite, SWT.CHECK);
		compilerCheckbox.setSelection(true);
		compilerCheckbox.setText(DataTransferMessages.AntBuildfileExportPage_15);
	}

	private void createTextFields(Composite composite) {

		// buildfilename and junitdir group
		Composite containerGroup = new Composite(composite, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		containerGroup.setLayout(layout);
		containerGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));

		// label
		Label buildfilenameLabel = new Label(containerGroup, SWT.NONE);
		buildfilenameLabel.setText(DataTransferMessages.AntBuildfileExportPage_16);

		// text field
		buildfilenameText = new Text(containerGroup, SWT.SINGLE | SWT.BORDER);
		buildfilenameText.setText("build.xml"); //$NON-NLS-1$
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
		buildfilenameText.setLayoutData(data);

		// label
		Label junitdirLabel = new Label(containerGroup, SWT.NONE);
		junitdirLabel.setText(DataTransferMessages.AntBuildfileExportPage_17);

		// text field
		junitdirText = new Text(containerGroup, SWT.SINGLE | SWT.BORDER);
		junitdirText.setText("junit"); //$NON-NLS-1$
		junitdirText.setLayoutData(data);

		ModifyListener listener = e -> updateEnablement();
		buildfilenameText.addModifyListener(listener);
		junitdirText.addModifyListener(listener);
	}

	private void initializeProjects() {
		IWorkspaceRoot rootWorkspace = ResourcesPlugin.getWorkspace().getRoot();
		IJavaModel javaModel = JavaCore.create(rootWorkspace);
		IJavaProject[] javaProjects;
		try {
			javaProjects = javaModel.getJavaProjects();
		}
		catch (JavaModelException e) {
			javaProjects = new IJavaProject[0];
		}
		fTableViewer.setInput(javaProjects);
		// Check any necessary projects
		if (fSelectedJavaProjects != null) {
			fTableViewer.setCheckedElements(fSelectedJavaProjects.toArray(new IJavaProject[fSelectedJavaProjects.size()]));
		}
	}

	private void updateEnablement() {
		boolean complete = true;
		if (fSelectedJavaProjects.isEmpty()) {
			setErrorMessage(DataTransferMessages.AntBuildfileExportPage_18);
			complete = false;
		}
		try {
			List<String> projectsWithErrors = new ArrayList<>();
			List<String> projectsWithWarnings = new ArrayList<>();
			findCyclicProjects(getProjects(false), projectsWithErrors, projectsWithWarnings);
			if (projectsWithErrors.size() > 0) {
				String message = DataTransferMessages.AntBuildfileExportPage_cycle_error_in_projects;
				if (projectsWithErrors.size() == 1) {
					message = DataTransferMessages.AntBuildfileExportPage_cycle_error_in_project;
				}
				setErrorMessage(MessageFormat.format(message, new Object[] { ExportUtil.toString(projectsWithErrors, ", ") })); //$NON-NLS-1$
				complete = false;
			} else if (projectsWithWarnings.size() > 0) {
				String message = DataTransferMessages.AntBuildfileExportPage_cycle_warning_in_projects;
				if (projectsWithWarnings.size() == 1) {
					message = DataTransferMessages.AntBuildfileExportPage_cycle_warning_in_project;
				}
				setMessage(MessageFormat.format(message, new Object[] { ExportUtil.toString(projectsWithWarnings, ", ") }), WARNING); //$NON-NLS-1$
			} else {
				setMessage(null);
			}
		}
		catch (CoreException e) {
			// do nothing
		}
		if (buildfilenameText.getText().length() == 0) {
			setErrorMessage(DataTransferMessages.AntBuildfileExportPage_19);
			complete = false;
		}
		if (junitdirText.getText().length() == 0) {
			setErrorMessage(DataTransferMessages.AntBuildfileExportPage_20);
			complete = false;
		}
		if (complete) {
			setErrorMessage(null);
		}
		setPageComplete(complete);
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			fTableViewer.getTable().setFocus();
		}
	}

	protected void setSelectedProjects(List<IJavaProject> selectedJavaProjects) {
		fSelectedJavaProjects.addAll(selectedJavaProjects);
	}

	/**
	 * Convert Eclipse Java projects to Ant build files. Displays error dialogs.
	 */
	public boolean generateBuildfiles() {
		setErrorMessage(null);
		final List<String> projectNames = new ArrayList<>();
		final Set<IJavaProject> projects;
		try {
			projects = getProjects(true);
			if (projects.isEmpty()) {
				return false;
			}
		}
		catch (JavaModelException e) {
			AntUIPlugin.log(e);
			setErrorMessage(MessageFormat.format(DataTransferMessages.AntBuildfileExportPage_10, new Object[] { e.toString() }));
			return false;
		}
		IRunnableWithProgress runnable = pm -> {
			SubMonitor localmonitor = SubMonitor.convert(pm, DataTransferMessages.AntBuildfileExportPage_creating_build_files, projects.size());
			Exception problem = null;
			try {
				BuildFileCreator.setOptions(buildfilenameText.getText(), junitdirText.getText(), compatibilityCheckbox.getSelection(), compilerCheckbox.getSelection());
				projectNames.addAll(BuildFileCreator.createBuildFiles(projects, getShell(), localmonitor.newChild(projects.size())));
			}
			catch (JavaModelException e1) {
				problem = e1;
			}
			catch (TransformerConfigurationException e2) {
				problem = e2;
			}
			catch (ParserConfigurationException e3) {
				problem = e3;
			}
			catch (TransformerException e4) {
				problem = e4;
			}
			catch (IOException e5) {
				problem = e5;
			}
			catch (CoreException e6) {
				problem = e6;
			}

			if (problem != null) {
				AntUIPlugin.log(problem);
				setErrorMessage(MessageFormat.format(DataTransferMessages.AntBuildfileExportPage_10, new Object[] { problem.toString() }));
			}
		};

		try {
			getContainer().run(false, false, runnable);
		}
		catch (InvocationTargetException e) {
			AntUIPlugin.log(e);
			return false;
		}
		catch (InterruptedException e) {
			AntUIPlugin.log(e);
			return false;
		}
		if (getErrorMessage() != null) {
			return false;
		}
		return true;
	}

	/**
	 * Get projects to write buildfiles for. Opens confirmation dialog.
	 * 
	 * @param displayConfirmation
	 *            if set to true a dialog prompts for confirmation before overwriting files
	 * @return set of project names
	 */
	private Set<IJavaProject> getProjects(boolean displayConfirmation) throws JavaModelException {
		// collect all projects to create buildfiles for
		Set<IJavaProject> projects = new TreeSet<>(ExportUtil.getJavaProjectComparator());
		Iterator<IJavaProject> javaProjects = fSelectedJavaProjects.iterator();
		while (javaProjects.hasNext()) {
			IJavaProject javaProject = javaProjects.next();
			projects.addAll(ExportUtil.getClasspathProjectsRecursive(javaProject));
			projects.add(javaProject);
		}

		// confirm overwrite
		List<String> confirmOverwrite = getConfirmOverwriteSet(projects);
		if (displayConfirmation && confirmOverwrite.size() > 0) {
			String message = DataTransferMessages.AntBuildfileExportPage_3 + ExportUtil.NEWLINE
					+ ExportUtil.toString(confirmOverwrite, ExportUtil.NEWLINE);
			if (!MessageDialog.openQuestion(getShell(), DataTransferMessages.AntBuildfileExportPage_4, message)) {
				return new TreeSet<>(ExportUtil.getJavaProjectComparator());
			}
		}
		return projects;
	}

	/**
	 * Splits a set of given projects into a list of projects that have cyclic dependency errors and a list of projects that have cyclic dependency
	 * warnings.
	 */
	private void findCyclicProjects(Set<IJavaProject> projects, List<String> errors, List<String> warnings) throws CoreException {
		for (IJavaProject javaProject : projects) {
			IMarker marker = ExportUtil.getCyclicDependencyMarker(javaProject);
			if (marker != null) {
				Integer severityAttr = (Integer) marker.getAttribute(IMarker.SEVERITY);
				if (severityAttr != null) {
					switch (severityAttr.intValue()) {
						case IMarker.SEVERITY_ERROR:
							errors.add(javaProject.getProject().getName());
							break;
						case IMarker.SEVERITY_WARNING:
							warnings.add(javaProject.getProject().getName());
							break;
						default:
							break;
					}
				}
			}
		}
	}

	/**
	 * Get list of projects which have already a buildfile that was not created by the buildfile export.
	 * 
	 * @param javaProjects
	 *            list of IJavaProject objects
	 * @return set of project names
	 */
	private List<String> getConfirmOverwriteSet(Set<IJavaProject> javaProjects) {
		List<String> result = new ArrayList<>(javaProjects.size());
		for (IJavaProject project : javaProjects) {
			String projectRoot = ExportUtil.getProjectRoot(project);
			if (ExportUtil.existsUserFile(projectRoot + buildfilenameText.getText())) {
				result.add(project.getProject().getName());
			}
		}
		return result;
	}
}