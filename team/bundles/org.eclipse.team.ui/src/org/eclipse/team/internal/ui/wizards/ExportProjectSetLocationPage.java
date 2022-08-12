/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
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

import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class ExportProjectSetLocationPage extends TeamWizardPage {

	Combo fileCombo;
	protected IFile workspaceFile;
	protected String file = ""; //$NON-NLS-1$
	Button browseButton;

	private boolean saveToFileSystem;
	private Button fileRadio;
	private Button workspaceRadio;

	protected Text workspaceText;

	public ExportProjectSetLocationPage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
		setDescription(TeamUIMessages.ExportProjectSetMainPage_description);
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = createComposite(parent, 1);
		initializeDialogUnits(composite);

		Group locationGroup = new Group(composite, SWT.None);
		GridLayout layout = new GridLayout();
		locationGroup.setLayout(layout);
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
		locationGroup.setLayoutData(data);
		locationGroup.setText(TeamUIMessages.ExportProjectSetMainPage_Project_Set_File_Name__3);

		createExportToFile(locationGroup);

		createExportToWorkspace(locationGroup);

		saveToFileSystem = true;

		setControl(composite);
		updateEnablement();
		Dialog.applyDialogFont(parent);
	}

	private void createExportToFile(Composite composite) {
		fileRadio = new Button(composite, SWT.RADIO);
		fileRadio.setText(TeamUIMessages.ExportProjectSetMainPage_FileButton);
		fileRadio.addListener(SWT.Selection, event -> {
			saveToFileSystem = true;
			file = fileCombo.getText();
			updateEnablement();
		});

		Composite inner = new Composite(composite, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 0;
		inner.setLayout(layout);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
		inner.setLayoutData(data);

		fileCombo = createDropDownCombo(inner);
		file = PsfFilenameStore.getInstance().getSuggestedDefault();
		fileCombo.setItems(PsfFilenameStore.getInstance().getHistory());
		fileCombo.setText(file);
		fileCombo.addListener(SWT.Modify, event -> {
			file = fileCombo.getText();
			updateEnablement();
		});

		browseButton = new Button(inner, SWT.PUSH);
		browseButton.setText(TeamUIMessages.ExportProjectSetMainPage_Browse_4);
		data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		data.widthHint = Math.max(widthHint, browseButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		browseButton.setLayoutData(data);
		browseButton.addListener(SWT.Selection, event -> {
			if (!isSaveToFileSystem())
				saveToFileSystem = true;

			FileDialog d = new FileDialog(getShell(), SWT.SAVE);
			d.setFilterExtensions(new String[] {"*.psf"}); //$NON-NLS-1$
			d.setFilterNames(new String[] {TeamUIMessages.ExportProjectSetMainPage_Project_Set_Files_3});
			d.setFileName(TeamUIMessages.ExportProjectSetMainPage_default);
			String fileName = getFileName();
			if (fileName != null) {
				int separator = fileName.lastIndexOf(File.separatorChar);
				if (separator != -1) {
					fileName = fileName.substring(0, separator);
				}
			}
			d.setFilterPath(fileName);
			String f = d.open();
			if (f != null) {
				fileCombo.setText(f);
				file = f;
			}
		});
	}

	private void createExportToWorkspace(Composite composite) {
		workspaceRadio = new Button(composite, SWT.RADIO);
		workspaceRadio.setText(TeamUIMessages.ExportProjectSetMainPage_WorkspaceButton);
		workspaceRadio.addListener(SWT.Selection, event -> {
			saveToFileSystem = false;
			updateEnablement();
		});

		final Composite nameGroup = new Composite(composite, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 0;
		nameGroup.setLayout(layout);
		final GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
		nameGroup.setLayoutData(data);

		workspaceText = createTextField(nameGroup);
		workspaceText.setEditable(false);
		workspaceText.addListener(SWT.Modify, event -> {
			file = workspaceFile.getLocation().toString();
			updateEnablement();
		});
		Button wsBrowseButton = new Button(nameGroup, SWT.PUSH);
		GridData gd = new GridData();
		gd.horizontalAlignment = GridData.FILL;
		int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		gd.widthHint = Math.max(widthHint, wsBrowseButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		wsBrowseButton.setLayoutData(gd);
		wsBrowseButton.setText(TeamUIMessages.ExportProjectSetMainPage_Browse);
		wsBrowseButton.addListener(SWT.Selection, event -> {
			if (isSaveToFileSystem())
				saveToFileSystem = false;

			WorkspaceDialog d = new WorkspaceDialog(getShell());
			d.open();
		});
	}

	private void updateEnablement() {
		boolean complete;
		//update radio buttons
		fileRadio.setSelection(saveToFileSystem);
		workspaceRadio.setSelection(!saveToFileSystem);

		if (file.length() == 0) {
			setErrorMessage(TeamUIMessages.ExportProjectSetMainPage_specifyFile);
			complete = false;
		} else {
			File f = new File(file);
			if (f.isDirectory()) {
				setErrorMessage(TeamUIMessages.ExportProjectSetMainPage_You_have_specified_a_folder_5);
				complete = false;
			} else {
				if (!isSaveToFileSystem() && workspaceFile == null) {
					setErrorMessage(TeamUIMessages.ExportProjectSetMainPage_specifyFile);
					complete = false;
				} else {
					complete = true;
				}
			}
		}

		if (!isSaveToFileSystem() && workspaceFile != null) {
			complete = true;
		}

		if (complete) {
			setErrorMessage(null);
			setDescription(TeamUIMessages.ExportProjectSetMainPage_description);
		}
		setPageComplete(complete);
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			fileCombo.setFocus();
		}
	}

	public boolean isSaveToFileSystem() {
		return saveToFileSystem;
	}

	public void refreshWorkspaceFile(IProgressMonitor monitor) throws CoreException {
		if (workspaceFile != null)
			workspaceFile.refreshLocal(IResource.DEPTH_ONE, monitor);
	}

	public String getFileName() {
		return file;
	}

	public void setFileName(String file) {
		if (file != null) {
			this.file = file;
		}
	}

	class WorkspaceDialog extends TitleAreaDialog {

		protected TreeViewer wsTreeViewer;
		protected Text wsFilenameText;
		protected IContainer wsContainer;
		protected Image dlgTitleImage;

		private Button okButton;

		public WorkspaceDialog(Shell shell) {
			super(shell);
		}

		@Override
		protected Control createContents(Composite parent) {
			Control control = super.createContents(parent);
			setTitle(TeamUIMessages.ExportProjectSetMainPage_WorkspaceDialogTitle);
			setMessage(TeamUIMessages.ExportProjectSetMainPage_WorkspaceDialogTitleMessage);

			return control;
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			Composite composite = (Composite) super.createDialogArea(parent);

			GridLayout layout = new GridLayout();
			layout.numColumns = 1;
			composite.setLayout(layout);
			final GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
			composite.setLayoutData(data);

			getShell().setText(TeamUIMessages.ExportProjectSetMainPage_WorkspaceDialogMessage);

			wsTreeViewer = new TreeViewer(composite, SWT.BORDER);
			final GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
			gd.widthHint = 550;
			gd.heightHint = 250;
			wsTreeViewer.getTree().setLayoutData(gd);

			wsTreeViewer.setContentProvider(new LocationPageContentProvider());
			wsTreeViewer.setLabelProvider(new WorkbenchLabelProvider());
			wsTreeViewer.setInput(ResourcesPlugin.getWorkspace());

			final Composite group = new Composite(composite, SWT.NONE);
			layout = new GridLayout(2, false);
			layout.marginWidth = 0;
			group.setLayout(layout);
			group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

			final Label label = new Label(group, SWT.NONE);
			label.setLayoutData(new GridData());
			label.setText(TeamUIMessages.ExportProjectSetMainPage_WorkspaceDialogFilename);

			wsFilenameText = new Text(group, SWT.BORDER);
			wsFilenameText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
			wsFilenameText.setText("projectSet.psf"); //$NON-NLS-1$

			setupListeners();

			return parent;
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			super.createButtonsForButtonBar(parent);
			okButton = getButton(IDialogConstants.OK_ID);
		}

		@Override
		protected void okPressed() {
			//Make sure that a container has been selected
			if (wsContainer == null) {
				getSelectedContainer();
			}
			//Assert.isNotNull(wsContainer);

			workspaceFile = wsContainer.getFile(new Path(wsFilenameText.getText()));
			if (workspaceFile != null) {
				workspaceText.setText(workspaceFile.getFullPath().toString());
			}
			//this.page.validatePage();
			//workspaceText.setText(wsFilenameText.getText());
			super.okPressed();
		}

		private void getSelectedContainer() {
			Object obj = wsTreeViewer.getStructuredSelection().getFirstElement();

			if (obj instanceof IContainer)
				wsContainer = (IContainer) obj;
			else if (obj instanceof IFile) {
				wsContainer = ((IFile) obj).getParent();
			}
		}

		@Override
		protected void cancelPressed() {
			//this.page.validatePage();
			getSelectedContainer();
			super.cancelPressed();
		}

		@Override
		public boolean close() {
			/*     if (dlgTitleImage != null)
			 dlgTitleImage.dispose();*/
			return super.close();
		}

		void setupListeners() {
			wsTreeViewer.addSelectionChangedListener(event -> {
				IStructuredSelection s = event.getStructuredSelection();
				Object obj = s.getFirstElement();
				if (obj != null) {

				}
				if (obj instanceof IContainer)
					wsContainer = (IContainer) obj;
				else if (obj instanceof IFile) {
					IFile tempFile = (IFile) obj;
					wsContainer = tempFile.getParent();
					wsFilenameText.setText(tempFile.getName());
				}
			});

			wsTreeViewer.addDoubleClickListener(event -> {
				ISelection s = event.getSelection();
				if (s instanceof IStructuredSelection) {
					Object item = ((IStructuredSelection) s).getFirstElement();
					if (wsTreeViewer.getExpandedState(item))
						wsTreeViewer.collapseToLevel(item, 1);
					else
						wsTreeViewer.expandToLevel(item, 1);
				}
			});

			wsFilenameText.addModifyListener(e -> {
				String patchName = wsFilenameText.getText();
				if (patchName.trim().isEmpty()) {
					okButton.setEnabled(false);
					setErrorMessage(TeamUIMessages.ExportProjectSetMainPage_WorkspaceDialogErrorNoFilename);
				} else if (!(ResourcesPlugin.getWorkspace().validateName(patchName, IResource.FILE)).isOK()) {
					//make sure that the filename does not contain more than one segment
					okButton.setEnabled(false);
					setErrorMessage(TeamUIMessages.ExportProjectSetMainPage_WorkspaceDialogErrorFilenameSegments);
				} else {
					okButton.setEnabled(true);
					setErrorMessage(null);
				}
			});
		}
	}


	class LocationPageContentProvider extends BaseWorkbenchContentProvider {
		//Never show closed projects
		boolean showClosedProjects = false;

		@Override
		public Object[] getChildren(Object element) {
			if (element instanceof IWorkspace) {
				// check if closed projects should be shown
				IProject[] allProjects = ((IWorkspace) element).getRoot().getProjects();
				if (showClosedProjects)
					return allProjects;

				ArrayList accessibleProjects = new ArrayList();
				for (IProject project : allProjects) {
					if (project.isOpen()) {
						accessibleProjects.add(project);
					}
				}
				return accessibleProjects.toArray();
			}

			return super.getChildren(element);
		}
	}


	public void validateEditWorkspaceFile(Shell shell) throws TeamException {
		if (workspaceFile == null || ! workspaceFile.exists() || !workspaceFile.isReadOnly())
			return;
		IStatus status = ResourcesPlugin.getWorkspace().validateEdit(new IFile[] {workspaceFile}, shell);
		if (!status.isOK()) {
			throw new TeamException(status);
		}
	}
}
