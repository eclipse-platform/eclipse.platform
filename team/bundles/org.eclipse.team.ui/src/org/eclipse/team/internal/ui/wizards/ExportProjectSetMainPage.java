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
 *     Patrick Dempsey <pd@bandxi.com> - Bug 177813 Export Team Project Set does not updateEnablement() when switching bewteen Project and Working sets
 *******************************************************************************/
package org.eclipse.team.internal.ui.wizards;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ITreePathContentProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.internal.ui.IHelpContextIds;
import org.eclipse.team.internal.ui.SWTUtils;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.IWorkingSetSelectionDialog;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.views.navigator.ResourceComparator;

public class ExportProjectSetMainPage extends TeamWizardPage {

	PageBook book;
	ProjectPage projectPage;
	WorkingSetPage workingSetPage;

	IExportProjectSetPage selectedPage;

	Button exportWorkingSets;

	ArrayList<IProject> passedInSelectedProjects = new ArrayList<>();

	class ProjectContentProvider implements ITreePathContentProvider{

		@Override
		public Object[] getChildren(TreePath parentPath) {
			Object obj = parentPath.getLastSegment();
			if (obj instanceof IWorkingSet){
				return ((IWorkingSet)obj).getElements();
			}
			return null;
		}

		@Override
		public TreePath[] getParents(Object element) {
			if (element instanceof IProject){
				ArrayList<IWorkingSet> treePaths = new ArrayList<>();
				IWorkingSet[] workingSets = PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSets();
				for (IWorkingSet workingSet : workingSets) {
					IAdaptable[] elements = workingSet.getElements();
					for (IAdaptable d : elements) {
						if (d.equals(element)) {
							treePaths.add(workingSet);
							break;
						}
					}
				}
				return treePaths.toArray(new TreePath[treePaths.size()]);
			}
			return null;
		}

		@Override
		public boolean hasChildren(TreePath path) {
			Object obj = path.getLastSegment();
			if (obj instanceof IWorkingSet)
				return true;

			return false;
		}

		@Override
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof IWorkspaceRoot) {
				IWorkspaceRoot root = (IWorkspaceRoot) inputElement;
				List<IProject> projectList = new ArrayList<>();
				IProject[] workspaceProjects = root.getProjects();
				for (IProject workspaceProject : workspaceProjects) {
					if (isProjectExportable(workspaceProject)) {
						projectList.add(workspaceProject);
					}
				}
				return projectList.toArray(new IProject[projectList.size()]);
			} else if (inputElement instanceof IWorkingSetManager){
				IWorkingSetManager manager = (IWorkingSetManager) inputElement;
				IWorkingSet[] allSets = manager.getAllWorkingSets();
				ArrayList<IWorkingSet> resourceSets = new ArrayList<>();
				for (IWorkingSet set : allSets) {
					if (isWorkingSetSupported(set)) {
						resourceSets.add(set);
					}
				}

				return resourceSets.toArray(new IWorkingSet[resourceSets.size()]);
			} else if (inputElement instanceof IAdaptable){
				IProject[] tempProjects = getProjectsForObject(inputElement);
				if (tempProjects != null)
					return tempProjects;
			}
			else if (inputElement instanceof IAdaptable[]){
				IAdaptable[] tempAdaptable = (IAdaptable[]) inputElement;
				return getProjectsForAdaptables(tempAdaptable);
			} else if (inputElement instanceof HashSet){
				Set<IProject> tempList = new HashSet<>();
				HashSet inputElementSet = (HashSet) inputElement;
				for (Object element : inputElementSet) {
					IProject[] projects = getProjectsForObject(element);
					if (projects != null)
						tempList.addAll(Arrays.asList(projects));
				}

				return tempList.toArray(new IProject[tempList.size()]);
			}

			return null;
		}

		@Override
		public void dispose() {

		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

		}

	}

	private class ExportProjectSetLabelProvider extends WorkbenchLabelProvider {

		@Override
		public Color getForeground(Object element) {
			if (element instanceof IProject
					&& !isProjectExportable((IProject) element)) {
				return Display.getCurrent().getSystemColor(
						SWT.COLOR_WIDGET_NORMAL_SHADOW);
			}
			return super.getForeground(element);
		}
	}

	private IProject[] getProjectsForObject(Object object) {
		ResourceMapping resourceMapping = Utils.getResourceMapping(object);
		if (resourceMapping != null) {
			return resourceMapping.getProjects();
		} else {
			IResource resource = Utils.getResource(object);
			if (resource != null && resource.getType() != IResource.ROOT)
				return new IProject[] { resource.getProject() };
		}
		return null;
	}

	private IProject[] getProjectsForAdaptables(IAdaptable[] adaptable) {
		Set<IProject> projectSet = new HashSet<>();
		for (IAdaptable a : adaptable) {
			IProject[] projects = getProjectsForObject(a);
			if (projects != null)
				projectSet.addAll(Arrays.asList(projects));
		}
		if (!projectSet.isEmpty())
			return projectSet.toArray(new IProject[0]);

		return null;
	}

	private static boolean isWorkingSetSupported(IWorkingSet workingSet) {
		if (!workingSet.isEmpty() && !workingSet.isAggregateWorkingSet()) {
			IAdaptable[] elements = workingSet.getElements();
			for (IAdaptable element : elements) {
				IResource resource = ResourceUtil.getResource(element);
				if (resource != null)
					// support a working set if it contains at least one resource
					return true;
			}
		}
		return false;
	}

	private boolean isProjectExportable(IProject project) {
		return RepositoryProvider.getProvider(project) != null;
	}

	public ExportProjectSetMainPage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
		setDescription(TeamUIMessages.ExportProjectSetMainPage_Initial_description);
	}

	@Override
	public void createControl(Composite parent) {
		Composite c = SWTUtils.createHVFillComposite(parent, 0);

		//Add the export working set section
		exportWorkingSets(c);

		book = new PageBook(c, SWT.NONE);
		book.setLayoutData(SWTUtils.createHVFillGridData());
		// set F1 help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(book, IHelpContextIds.EXPORT_PROJECT_SET_PAGE);

		workingSetPage = new WorkingSetPage();
		workingSetPage.createControl(book);

		projectPage = new ProjectPage();

		// filter out unexportable projects
		List passedInExportableProjects = new ArrayList();
		for (IProject project : passedInSelectedProjects) {
			if (isProjectExportable(project))
				passedInExportableProjects.add(project);
		}
		// pass in selected, exportable projects
		projectPage.getSelectedProjects().addAll(passedInExportableProjects);
		projectPage.getReferenceCountProjects().addAll(passedInExportableProjects);

		projectPage.createControl(book);

		setControl(c);
		book.showPage(projectPage.getControl());

		selectedPage = projectPage;

		Dialog.applyDialogFont(parent);
	}

	private void exportWorkingSets(Composite composite) {
		exportWorkingSets = new Button(composite, SWT.CHECK | SWT.LEFT);
		exportWorkingSets.setText(TeamUIMessages.ExportProjectSetMainPage_ExportWorkingSets);

		exportWorkingSets.setSelection(false);
		exportWorkingSets.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (((Button) e.widget).getSelection()){
					book.showPage(workingSetPage.getControl());
					selectedPage = workingSetPage;
					workingSetPage.refresh();
					workingSetPage.updateEnablement();
				} else {
					book.showPage(projectPage.getControl());
					selectedPage = projectPage;
					projectPage.updateEnablement();
				}
			}
		});
	}

	public IWorkingSet[] getSelectedWorkingSets(){
		return (IWorkingSet[]) selectedPage.getWorkingSet().toArray(new IWorkingSet[selectedPage.getWorkingSet().size()]);
	}

	public IProject[] getSelectedProjects() {
		return (IProject[]) selectedPage.getSelectedProjects().toArray(new IProject[selectedPage.getSelectedProjects().size()]);
	}

	public IProject[] getReferenceCountProjects() {
		return (IProject[]) selectedPage.getReferenceCountProjects().toArray(new IProject[selectedPage.getReferenceCountProjects().size()]);
	}

	public void setSelectedProjects(IProject[] selectedProjects) {
		passedInSelectedProjects.addAll(Arrays.asList(selectedProjects));
	}

	private interface IExportProjectSetPage {
		HashSet getSelectedProjects();

		ArrayList getReferenceCountProjects();

		ArrayList getWorkingSet();
	}

	private class ProjectPage extends Page implements IExportProjectSetPage {
		private Composite projectComposite;

		private CheckboxTableViewer tableViewer;
		private Table table;

		HashSet selectedProjects = new HashSet();
		ArrayList referenceCountProjects = new ArrayList();
		ArrayList selectedWorkingSet = new ArrayList();

		@Override
		public void createControl(Composite parent) {

			projectComposite = SWTUtils.createHVFillComposite(parent, 1);
			initializeDialogUnits(projectComposite);

			//Adds the project table
			addProjectSection(projectComposite);
			initializeProjects();
			// don't shown an error when the page become visible the first time
			setPageComplete(selectedProjects.size() > 0);
		}

		@Override
		public Control getControl() {
			return projectComposite;
		}

		@Override
		public void setFocus() {
			projectComposite.setFocus();
		}

		private void addProjectSection(Composite composite) {

			createLabel(composite, TeamUIMessages.ExportProjectSetMainPage_Select_the_projects_to_include_in_the_project_set__2);

			table = new Table(composite, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
			tableViewer = new CheckboxTableViewer(table);
			table.setLayout(new TableLayout());
			GridData data = new GridData(GridData.FILL_BOTH);
			data.heightHint = 300;
			table.setLayoutData(data);
			tableViewer.setContentProvider(new ProjectContentProvider());
			tableViewer.setLabelProvider(new WorkbenchLabelProvider());
			tableViewer.setComparator(new ResourceComparator(ResourceComparator.NAME));
			tableViewer.addCheckStateListener(event -> {
				Object temp = event.getElement();
				if (temp instanceof IProject){
					IProject project = (IProject) event.getElement();
					if (event.getChecked()) {
						selectedProjects.add(project);
						referenceCountProjects.add(project);
					} else {
						selectedProjects.remove(project);
						referenceCountProjects.remove(project);
					}
				} else if (temp instanceof IWorkingSet){
					IWorkingSet workingSet = (IWorkingSet) temp;
					if (event.getChecked()){
						IAdaptable[] elements1 = workingSet.getElements();
						Collections.addAll(selectedProjects, elements1);
					} else {
						IAdaptable[] elements2 = workingSet.getElements();
						for (IAdaptable element : elements2) {
							selectedProjects.remove(element);
						}
					}
				}
				updateEnablement();
			});

			Composite buttonComposite = new Composite(composite, SWT.NONE);
			GridLayout layout = new GridLayout();
			layout.numColumns = 2;
			layout.marginWidth = 0;
			buttonComposite.setLayout(layout);
			data = new GridData(SWT.FILL, SWT.FILL, true, false);
			buttonComposite.setLayoutData(data);

			Button selectAll = new Button(buttonComposite, SWT.PUSH);
			data = new GridData();
			data.verticalAlignment = GridData.BEGINNING;
			data.horizontalAlignment = GridData.END;
			int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
			data.widthHint = Math.max(widthHint, selectAll.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
			selectAll.setLayoutData(data);
			selectAll.setText(TeamUIMessages.ExportProjectSetMainPage_SelectAll);
			selectAll.addListener(SWT.Selection, event -> {
				tableViewer.setAllChecked(true);
				selectedProjects.clear();
				Object[] checked = tableViewer.getCheckedElements();
				Collections.addAll(selectedProjects, checked);
				updateEnablement();
			});

			Button deselectAll = new Button(buttonComposite, SWT.PUSH);
			data = new GridData();
			data.verticalAlignment = GridData.BEGINNING;
			data.horizontalAlignment = GridData.END;
			widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
			data.widthHint = Math.max(widthHint, deselectAll.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
			deselectAll.setLayoutData(data);
			deselectAll.setText(TeamUIMessages.ExportProjectSetMainPage_DeselectAll);
			deselectAll.addListener(SWT.Selection, event -> {
				tableViewer.setAllChecked(false);
				selectedProjects.clear();
				updateEnablement();
			});
		}

		private void initializeProjects() {
			tableViewer.setInput(ResourcesPlugin.getWorkspace().getRoot());

			// Check any necessary projects
			if (selectedProjects != null) {
				tableViewer.setCheckedElements(selectedProjects.toArray(new IProject[selectedProjects.size()]));
			}
		}

		private void updateEnablement() {
			boolean complete = selectedProjects.size() > 0;

			if (complete) {
				setErrorMessage(null);
				setDescription(TeamUIMessages.ExportProjectSetMainPage_description);
			} else {
				setErrorMessage(TeamUIMessages.ExportProjectSetMainPage_A_project_must_be_selected);
			}
			setPageComplete(complete);
		}

		@Override
		public ArrayList getReferenceCountProjects() {
			return referenceCountProjects;
		}

		@Override
		public HashSet getSelectedProjects() {
			return selectedProjects;
		}

		@Override
		public ArrayList getWorkingSet() {
			return selectedWorkingSet;
		}

	}

	private class WorkingSetPage extends Page implements IExportProjectSetPage {

		private Composite projectComposite;
		private Table wsTable;
		private CheckboxTableViewer wsTableViewer;

		private Table table;
		private TableViewer tableViewer;


		HashSet<IProject> selectedProjects = new HashSet<>();
		ArrayList<IProject> referenceCountProjects = new ArrayList<>();
		ArrayList selectedWorkingSet = new ArrayList();

		/**
		 * Indicates whether the page has been displayed. If this is the first
		 * time don't show an error until the user made the first modification.
		 */
		private boolean pageShown = false;

		@Override
		public void createControl(Composite parent) {

			projectComposite = SWTUtils.createHVFillComposite(parent, 1);
			initializeDialogUnits(projectComposite);

			Label label = createLabel (projectComposite, TeamUIMessages.ExportProjectSetMainPage_SelectButton);
			GridData grid = (GridData) label.getLayoutData();
			label.setData(grid);

			SashForm form = new SashForm(projectComposite, SWT.HORIZONTAL);
			form.setLayout(new FillLayout());
			GridData data = new GridData(GridData.FILL_BOTH);
			form.setLayoutData(data);

			// Adds the working set table
			addWorkingSetSection(form);

			addProjectSection(form);

			form.setWeights(new int[] { 50, 50 });

			addButtons(projectComposite);
			setPageComplete(false);
		}

		private void addProjectSection(Composite composite) {
			table = new Table(composite, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
			tableViewer = new TableViewer(table);
			table.setLayout(new TableLayout());
			GridData data = new GridData(GridData.FILL_BOTH);
			data.heightHint = 300;
			table.setLayoutData(data);
			tableViewer.setContentProvider(new ProjectContentProvider());
			tableViewer.setLabelProvider(new ExportProjectSetLabelProvider());
			tableViewer.setComparator(new ResourceComparator(ResourceComparator.NAME));
		}

		private void addWorkingSetSection(Composite projectComposite) {

			wsTable = new Table(projectComposite, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
			wsTableViewer = new CheckboxTableViewer(wsTable);
			wsTable.setLayout(new TableLayout());
			GridData data = new GridData(GridData.FILL_BOTH);
			data.heightHint = 300;
			wsTable.setLayoutData(data);
			wsTableViewer.setContentProvider(new ProjectContentProvider());
			wsTableViewer.setLabelProvider(new WorkbenchLabelProvider());
			wsTableViewer.addCheckStateListener(event -> {
				Object temp = event.getElement();
				if (temp instanceof IWorkingSet){
					IWorkingSet workingSet = (IWorkingSet) temp;
					if (event.getChecked()){
						workingSetAdded(workingSet);
						//Add the selected project to the table viewer
						tableViewer.setInput(selectedProjects);
					} else {
						workingSetRemoved(workingSet);
						//Add the selected project to the table viewer
						tableViewer.setInput(selectedProjects);
					}
				}
				updateEnablement();
			});

			wsTableViewer.setInput(PlatformUI.getWorkbench().getWorkingSetManager());
		}

		private void addButtons(Composite projectComposite){

			Composite buttonComposite = new Composite(projectComposite, SWT.NONE);
			GridLayout layout = new GridLayout();
			layout.numColumns = 3;
			layout.marginWidth = 0;
			buttonComposite.setLayout(layout);
			GridData data = new GridData(SWT.FILL, SWT.FILL, false, false);
			buttonComposite.setLayoutData(data);

			Button selectAll = new Button(buttonComposite, SWT.PUSH);
			data = new GridData();
			data.verticalAlignment = GridData.FILL;
			data.horizontalAlignment = GridData.FILL;
			int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
			data.widthHint = Math.max(widthHint, selectAll.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
			selectAll.setLayoutData(data);
			selectAll.setText(TeamUIMessages.ExportProjectSetMainPage_SelectAll);
			selectAll.addListener(SWT.Selection, event -> {
				wsTableViewer.setAllChecked(true);

				selectedProjects.clear();
				selectedWorkingSet.clear();
				Object[] checked = wsTableViewer.getCheckedElements();
				for (Object c : checked) {
					selectedWorkingSet.add(c);
					if (c instanceof IWorkingSet) {
						IWorkingSet ws = (IWorkingSet) c;
						IAdaptable[] elements = ws.getElements();
						addProjects(elements);
					}
					tableViewer.setInput(selectedProjects);
				}
				updateEnablement();
			});

			Button deselectAll = new Button(buttonComposite, SWT.PUSH);
			data = new GridData();
			data.verticalAlignment = GridData.FILL;
			data.horizontalAlignment = GridData.FILL;
			widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
			data.widthHint = Math.max(widthHint, deselectAll.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
			deselectAll.setLayoutData(data);
			deselectAll.setText(TeamUIMessages.ExportProjectSetMainPage_DeselectAll);
			deselectAll.addListener(SWT.Selection, event -> {
				wsTableViewer.setAllChecked(false);
				selectedWorkingSet.clear();
				selectedProjects.clear();
				referenceCountProjects.clear();
				tableViewer.setInput(selectedProjects);
				updateEnablement();
			});

			Button newWorkingSet = new Button(buttonComposite, SWT.PUSH);
			data = new GridData();
			data.verticalAlignment = GridData.FILL;
			data.horizontalAlignment = GridData.FILL;
			widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
			data.widthHint = Math.max(widthHint, deselectAll.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
			newWorkingSet.setLayoutData(data);
			newWorkingSet.setText(TeamUIMessages.ExportProjectSetMainPage_EditButton);
			newWorkingSet.addListener(SWT.Selection, event -> {
				final IWorkingSetManager workingSetManager = PlatformUI.getWorkbench().getWorkingSetManager();
				IWorkingSetSelectionDialog wsWizard = workingSetManager.createWorkingSetSelectionDialog(getShell(), false);
				if (wsWizard != null) {
					IPropertyChangeListener propListener = null;
					try {
						//add event listener
						propListener = event1 -> {

						};

						workingSetManager.addPropertyChangeListener(propListener);
						wsWizard.open();
						//recalculate working sets
						selectedWorkingSet.clear();
						referenceCountProjects.clear();
						selectedProjects.clear();
						wsTableViewer.setInput(workingSetManager);
						Object[] checked = wsTableViewer.getCheckedElements();
						for (Object c : checked) {
							selectedWorkingSet.add(c);
							if (c instanceof IWorkingSet) {
								IWorkingSet ws = (IWorkingSet) c;
								IAdaptable[] elements = ws.getElements();
								addProjects(elements);
							}
						}

						wsTableViewer.setInput(workingSetManager);
						tableViewer.setInput(selectedProjects);
					} finally {
						if (propListener != null)
							workingSetManager.removePropertyChangeListener(propListener);
					}
				}
			});
		}

		@Override
		public Control getControl() {
			return projectComposite;
		}

		@Override
		public void setFocus() {
			projectComposite.setFocus();
		}

		public void refresh(){
			wsTableViewer.setInput(PlatformUI.getWorkbench().getWorkingSetManager());
		}

		private void updateEnablement() {
			boolean complete = selectedProjects.size() > 0
					&& selectedWorkingSet.size() > 0;
			boolean allExportable = complete;

			// check if there is at least one exportable project selected
			if (complete || !pageShown) {
				complete = false;
				for (IProject selectedProject : selectedProjects) {
					if (isProjectExportable(selectedProject)) {
						complete = true;
					} else {
						allExportable = false;
					}
				}

				if (!complete && !pageShown) {
					setErrorMessage(null);
					setMessage(TeamUIMessages.ExportProjectSetMainPage_Initial_description);
				} else if (complete || !pageShown) {
					if (allExportable) {
						setErrorMessage(null);
						setMessage(TeamUIMessages.ExportProjectSetMainPage_description);
					} else {
						setErrorMessage(null);
						setMessage(TeamUIMessages.ExportProjectSetMainPage_warning, IMessageProvider.WARNING);
					}
				} else {
					setErrorMessage(TeamUIMessages.ExportProjectSetMainPage_None_of_the_selected_working_sets_have_an_available_project_to_export);
				}
			} else {
				setErrorMessage(TeamUIMessages.ExportProjectSetMainPage_A_working_set_must_be_selected);
			}

			setPageComplete(complete);
			pageShown = true;
		}

		@Override
		public ArrayList getReferenceCountProjects() {
			return referenceCountProjects;
		}

		@Override
		public HashSet getSelectedProjects() {
			return selectedProjects;
		}

		@Override
		public ArrayList getWorkingSet() {
		return selectedWorkingSet;
		}

		private void workingSetAdded(IWorkingSet workingSet) {
			IAdaptable[] elements = workingSet.getElements();
			selectedWorkingSet.add(workingSet);
			addProjects(elements);
		}

		private void workingSetRemoved(IWorkingSet workingSet) {
			IAdaptable[] elements = workingSet.getElements();
			selectedWorkingSet.remove(workingSet);

			Set<IProject> tempSet = new HashSet<>();
			for (IAdaptable element : elements) {
				IProject[] projects = getProjectsForObject(element);
				if (projects != null)
					tempSet.addAll(Arrays.asList(projects));
			}

			if (!tempSet.isEmpty()) {
				selectedProjects.removeAll(tempSet);
				for (Object element : tempSet) {
					referenceCountProjects.remove(element);
				}
				selectedProjects.addAll(referenceCountProjects);
			}
		}

		private void addProjects(IAdaptable[] elements) {
			Set<IProject> tempSet = new HashSet<>();
			for (IAdaptable element : elements) {
				IProject[] projects = getProjectsForObject(element);
				if (projects != null)
					tempSet.addAll(Arrays.asList(projects));
			}

			selectedProjects.addAll(tempSet);
			referenceCountProjects.addAll(tempSet);
		}
	}
}
