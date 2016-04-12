/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Patrick Dempsey <pd@bandxi.com> - Bug 177813 Export Team Project Set does not updateEnablement() when switching bewteen Project and Working sets
 *******************************************************************************/
package org.eclipse.team.internal.ui.wizards;

import java.util.*;
import java.util.List;

import org.eclipse.core.resources.*;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.internal.ui.*;
import org.eclipse.ui.*;
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

	ArrayList passedInSelectedProjects = new ArrayList();

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
				ArrayList treePaths = new ArrayList();
				IWorkingSet[] workingSets = TeamUIPlugin.getPlugin().getWorkbench().getWorkingSetManager().getWorkingSets();
				for (int i = 0; i < workingSets.length; i++) {
					IAdaptable[] elements = workingSets[i].getElements();
					for (int j = 0; j < elements.length; j++) {
						if (elements[j].equals(element)){
							treePaths.add(workingSets[i]);
							break;
						}
					}
				}
				return (TreePath[]) treePaths.toArray(new TreePath[treePaths.size()]);
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
				List projectList = new ArrayList();
				IProject[] workspaceProjects = root.getProjects();
				for (int i = 0; i < workspaceProjects.length; i++) {
					if (isProjectExportable(workspaceProjects[i])) {
						projectList.add(workspaceProjects[i]);
					}
				}
				return projectList.toArray(new IProject[projectList.size()]);
			} else if (inputElement instanceof IWorkingSetManager){
				IWorkingSetManager manager = (IWorkingSetManager) inputElement;
				IWorkingSet[] allSets = manager.getAllWorkingSets();
				ArrayList resourceSets = new ArrayList();
				for (int i = 0; i < allSets.length; i++)
					if (isWorkingSetSupported(allSets[i]))
						resourceSets.add(allSets[i]);

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
				Set tempList = new HashSet();
				HashSet inputElementSet = (HashSet) inputElement;
				for (Iterator iterator = inputElementSet.iterator(); iterator.hasNext();) {
					IProject[] projects = getProjectsForObject(iterator.next());
					if (projects != null)
						tempList.addAll(Arrays.asList(projects));
				}

				return (IProject[]) tempList.toArray(new IProject[tempList.size()]);
			}

			return null;
		}

		@Override
		public void dispose() {

		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

		}

	};

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
		Set projectSet = new HashSet();
		for (int i = 0; i < adaptable.length; i++) {
			IProject[] projects = getProjectsForObject(adaptable[i]);
			if (projects != null)
				projectSet.addAll(Arrays.asList(projects));
		}
		if (!projectSet.isEmpty())
			return (IProject[]) projectSet.toArray(new IProject[0]);

		return null;
	}

	private static boolean isWorkingSetSupported(IWorkingSet workingSet) {
		if (!workingSet.isEmpty() && !workingSet.isAggregateWorkingSet()) {
			IAdaptable[] elements = workingSet.getElements();
			for (int i = 0; i < elements.length; i++) {
				IResource resource = ResourceUtil.getResource(elements[i]);
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

	/*
	 * @see IDialogPage#createControl(Composite)
	 */
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
		for (Iterator iterator = passedInSelectedProjects.iterator(); iterator.hasNext();) {
			IProject project = (IProject) iterator.next();
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
			tableViewer.addCheckStateListener(new ICheckStateListener() {
				@Override
				public void checkStateChanged(CheckStateChangedEvent event) {
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
							IAdaptable[] elements = workingSet.getElements();
							for (int i = 0; i < elements.length; i++) {
								selectedProjects.add(elements[i]);
							}
						} else {
							IAdaptable[] elements = workingSet.getElements();
							for (int i = 0; i < elements.length; i++) {
								selectedProjects.remove(elements[i]);
							}
						}
					}
					updateEnablement();
				}
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
			selectAll.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					tableViewer.setAllChecked(true);
					selectedProjects.removeAll(selectedProjects);
					Object[] checked = tableViewer.getCheckedElements();
					for (int i = 0; i < checked.length; i++) {
						selectedProjects.add(checked[i]);
					}
					updateEnablement();
				}
			});

			Button deselectAll = new Button(buttonComposite, SWT.PUSH);
			data = new GridData();
			data.verticalAlignment = GridData.BEGINNING;
			data.horizontalAlignment = GridData.END;
			widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
			data.widthHint = Math.max(widthHint, deselectAll.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
			deselectAll.setLayoutData(data);
			deselectAll.setText(TeamUIMessages.ExportProjectSetMainPage_DeselectAll);
			deselectAll.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					tableViewer.setAllChecked(false);
					selectedProjects.removeAll(selectedProjects);
					updateEnablement();
				}
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


		HashSet selectedProjects = new HashSet();
		ArrayList referenceCountProjects = new ArrayList();
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
			wsTableViewer.addCheckStateListener(new ICheckStateListener() {
				@Override
				public void checkStateChanged(CheckStateChangedEvent event) {
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
				}
			});

			wsTableViewer.setInput(TeamUIPlugin.getPlugin().getWorkbench().getWorkingSetManager());
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
			selectAll.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					wsTableViewer.setAllChecked(true);

					selectedProjects.removeAll(selectedProjects);
					selectedWorkingSet.removeAll(selectedWorkingSet);
					Object[] checked = wsTableViewer.getCheckedElements();
					for (int i = 0; i < checked.length; i++) {
						selectedWorkingSet.add(checked[i]);
						if (checked[i] instanceof IWorkingSet){
							IWorkingSet ws = (IWorkingSet) checked[i];
							IAdaptable[] elements = ws.getElements();
							addProjects(elements);
						}
						tableViewer.setInput(selectedProjects);
					}
					updateEnablement();
				}
			});

			Button deselectAll = new Button(buttonComposite, SWT.PUSH);
			data = new GridData();
			data.verticalAlignment = GridData.FILL;
			data.horizontalAlignment = GridData.FILL;
			widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
			data.widthHint = Math.max(widthHint, deselectAll.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
			deselectAll.setLayoutData(data);
			deselectAll.setText(TeamUIMessages.ExportProjectSetMainPage_DeselectAll);
			deselectAll.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					wsTableViewer.setAllChecked(false);
					selectedWorkingSet.removeAll(selectedWorkingSet);
					selectedProjects.removeAll(selectedProjects);
					referenceCountProjects.removeAll(referenceCountProjects);
					tableViewer.setInput(selectedProjects);
					updateEnablement();
				}
			});

			Button newWorkingSet = new Button(buttonComposite, SWT.PUSH);
			data = new GridData();
			data.verticalAlignment = GridData.FILL;
			data.horizontalAlignment = GridData.FILL;
			widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
			data.widthHint = Math.max(widthHint, deselectAll.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
			newWorkingSet.setLayoutData(data);
			newWorkingSet.setText(TeamUIMessages.ExportProjectSetMainPage_EditButton);
			newWorkingSet.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					final IWorkingSetManager workingSetManager = TeamUIPlugin.getPlugin().getWorkbench().getWorkingSetManager();
					IWorkingSetSelectionDialog wsWizard = workingSetManager.createWorkingSetSelectionDialog(getShell(), false);
					if (wsWizard != null) {
						IPropertyChangeListener propListener = null;
						try {
							//add event listener
							propListener = new  IPropertyChangeListener() {
								@Override
								public void propertyChange(PropertyChangeEvent event) {

								}};

							workingSetManager.addPropertyChangeListener(propListener);
							wsWizard.open();
							//recalculate working sets
							selectedWorkingSet.removeAll(selectedWorkingSet);
							referenceCountProjects.removeAll(selectedProjects);
							selectedProjects.removeAll(selectedProjects);
							wsTableViewer.setInput(workingSetManager);
							Object[] checked = wsTableViewer.getCheckedElements();
							for (int i = 0; i < checked.length; i++) {
								selectedWorkingSet.add(checked[i]);
								if (checked[i] instanceof IWorkingSet) {
									IWorkingSet ws = (IWorkingSet) checked[i];
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
			wsTableViewer.setInput(TeamUIPlugin.getPlugin().getWorkbench().getWorkingSetManager());
		}

		private void updateEnablement() {
			boolean complete = selectedProjects.size() > 0
					&& selectedWorkingSet.size() > 0;
			boolean allExportable = complete;

			// check if there is at least one exportable project selected
			if (complete || !pageShown) {
				complete = false;
				for (Iterator iterator = selectedProjects.iterator(); iterator
						.hasNext();) {
					IProject selectedProject = (IProject) iterator.next();
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

			Set tempSet = new HashSet();
			for (int i = 0; i < elements.length; i++) {
				IProject[] projects = getProjectsForObject(elements[i]);
				if (projects != null)
					tempSet.addAll(Arrays.asList(projects));
			}

			if (!tempSet.isEmpty()) {
				selectedProjects.removeAll(tempSet);
				for (Iterator iterator = tempSet.iterator(); iterator.hasNext();) {
					Object element = (Object) iterator.next();
					referenceCountProjects.remove(element);
				}
				selectedProjects.addAll(referenceCountProjects);
			}
		}

		private void addProjects(IAdaptable[] elements) {
			Set tempSet = new HashSet();
			for (int j = 0; j < elements.length; j++) {
				IProject[] projects = getProjectsForObject(elements[j]);
				if (projects != null)
					tempSet.addAll(Arrays.asList(projects));
			}

			selectedProjects.addAll(tempSet);
			referenceCountProjects.addAll(tempSet);
		}
	}
}
