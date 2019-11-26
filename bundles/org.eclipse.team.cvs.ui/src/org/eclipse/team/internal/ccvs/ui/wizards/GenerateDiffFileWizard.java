/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
 *     Benjamin Muskalla (b.muskalla@gmx.net) - Bug 149672 [Patch] Create Patch wizard should remember previous settings
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.wizards;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.Team;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IDiffVisitor;
import org.eclipse.team.core.mapping.provider.ResourceDiffTree;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.core.client.Diff;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.IHelpContextIds;
import org.eclipse.team.internal.ccvs.ui.operations.*;
import org.eclipse.team.internal.ccvs.ui.subscriber.CreatePatchWizardParticipant;
import org.eclipse.team.internal.ccvs.ui.subscriber.WorkspaceSynchronizeParticipant;
import org.eclipse.team.internal.core.subscribers.SubscriberSyncInfoCollector;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.views.navigator.ResourceComparator;

/**
 * A wizard for creating a patch file by running the CVS diff command.
 */
public class GenerateDiffFileWizard extends Wizard {

	//The initial size of this wizard.
	private final static int INITIAL_WIDTH = 300;
	private final static int INITIAL_HEIGHT = 350;

	public static void run(IWorkbenchPart part, final IResource[] resources, boolean unifiedSelectionEnabled) {
		final String title = CVSUIMessages.GenerateCVSDiff_title;
		final GenerateDiffFileWizard wizard = new GenerateDiffFileWizard(part,resources, unifiedSelectionEnabled);
		wizard.setWindowTitle(title);
		WizardDialog dialog = new WizardDialog(part.getSite().getShell(), wizard);
		dialog.setMinimumPageSize(INITIAL_WIDTH, INITIAL_HEIGHT);
		dialog.open();
	}

	public static void run(IWorkbenchPart part, final IResource[] resources) {
		GenerateDiffFileWizard.run(part,resources,true);
	}

	/**
	 * Page to select a patch file. Overriding validatePage was necessary to allow
	 * entering a file name that already exists.
	 */
	public class LocationPage extends WizardPage {

		/**
		 * The possible locations to save a patch.
		 */
		public final static int CLIPBOARD = 1;
		public final static int FILESYSTEM = 2;
		public final static int WORKSPACE = 3;

		/**
		 * GUI controls for clipboard (cp), filesystem (fs) and workspace (ws).
		 */
		private Button cpRadio;

		private Button fsRadio;
		protected Text fsPathText;
		private Button fsBrowseButton;
		private boolean fsBrowsed = false;

		private Button wsRadio;
		protected Text wsPathText;
		private Button wsBrowseButton;
		private boolean wsBrowsed = false;

		protected CreatePatchWizardParticipant fParticipant;
		private Button chgSelectAll;
		private Button chgDeselectAll;

		/**
		 * State information of this page, updated by the listeners.
		 */
		protected boolean canValidate;
		protected boolean pageValid;
		protected IContainer wsSelectedContainer;
		protected IPath[] foldersToCreate;
		protected int selectedLocation;

		/**
		 * The default values store used to initialize the selections.
		 */
		private final DefaultValuesStore store;


		class LocationPageContentProvider extends BaseWorkbenchContentProvider {
			//Never show closed projects
			boolean showClosedProjects=false;

			@Override
			public Object[] getChildren(Object element) {
				if (element instanceof IWorkspace) {
					// check if closed projects should be shown
					IProject[] allProjects = ((IWorkspace) element).getRoot().getProjects();
					if (showClosedProjects)
						return allProjects;

					ArrayList<IProject> accessibleProjects = new ArrayList<>();
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

		class WorkspaceDialog extends TitleAreaDialog {

			protected TreeViewer wsTreeViewer;
			protected Text wsFilenameText;
			protected Image dlgTitleImage;

			private boolean modified = false;

			public WorkspaceDialog(Shell shell) {
				super(shell);
			}

			@Override
			protected Control createContents(Composite parent) {
				Control control = super.createContents(parent);
				setTitle(CVSUIMessages.WorkspacePatchDialogTitle);
				setMessage(CVSUIMessages.WorkspacePatchDialogDescription);
				//create title image
				dlgTitleImage = CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_WIZBAN_DIFF).createImage();
				setTitleImage(dlgTitleImage);

				return control;
			}

			@Override
			protected Control createDialogArea(Composite parent){
				Composite parentComposite = (Composite) super.createDialogArea(parent);

				// create a composite with standard margins and spacing
				Composite composite = new Composite(parentComposite, SWT.NONE);
				GridLayout layout = new GridLayout();
				layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
				layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
				layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
				layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
				composite.setLayout(layout);
				composite.setLayoutData(new GridData(GridData.FILL_BOTH));
				composite.setFont(parentComposite.getFont());

				getShell().setText(CVSUIMessages.GenerateDiffFileWizard_9);

				wsTreeViewer = new TreeViewer(composite, SWT.BORDER);
				final GridData gd= new GridData(SWT.FILL, SWT.FILL, true, true);
				gd.widthHint= 550;
				gd.heightHint= 250;
				wsTreeViewer.getTree().setLayoutData(gd);

				wsTreeViewer.setContentProvider(new LocationPageContentProvider());
				wsTreeViewer.setComparator(new ResourceComparator(ResourceComparator.NAME));
				wsTreeViewer.setLabelProvider(new WorkbenchLabelProvider());
				wsTreeViewer.setInput(ResourcesPlugin.getWorkspace());

				//Open to whatever is selected in the workspace field
				IPath existingWorkspacePath = new Path(wsPathText.getText());
				//Ensure that this workspace path is valid
				IResource selectedResource = ResourcesPlugin.getWorkspace().getRoot().findMember(existingWorkspacePath);
				if (selectedResource != null) {
					wsTreeViewer.expandToLevel(selectedResource, 0);
					wsTreeViewer.setSelection(new StructuredSelection(selectedResource));
				}

				final Composite group = new Composite(composite, SWT.NONE);
				layout = new GridLayout(2, false);
				layout.marginWidth = 0;
				group.setLayout(layout);
				group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

				final Label label = new Label(group, SWT.NONE);
				label.setLayoutData(new GridData());
				label.setText(CVSUIMessages.Fi_le_name__9);

				wsFilenameText = new Text(group,SWT.BORDER);
				wsFilenameText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

				setupListeners();

				return parent;
			}

			@Override
			protected Button createButton(Composite parent, int id,
					String label, boolean defaultButton) {
				Button button = super.createButton(parent, id, label,
						defaultButton);
				if (id == IDialogConstants.OK_ID) {
					button.setEnabled(false);
				}
				return button;
			}

			private void validateDialog() {
				String fileName = wsFilenameText.getText();

				if (fileName.isEmpty()) {
					if (modified) {
						setErrorMessage(CVSUIMessages.GenerateDiffFileWizard_2);
						getButton(IDialogConstants.OK_ID).setEnabled(false);
						return;
					} else {
						setErrorMessage(null);
						getButton(IDialogConstants.OK_ID).setEnabled(false);
						return;
					}
				}

				// make sure that the filename is valid
				if (!(ResourcesPlugin.getWorkspace().validateName(fileName,
						IResource.FILE)).isOK() && modified) {
					setErrorMessage(CVSUIMessages.GenerateDiffFileWizard_5);
					getButton(IDialogConstants.OK_ID).setEnabled(false);
					return;
				}

				// Make sure that a container has been selected
				if (getSelectedContainer() == null) {
					setErrorMessage(CVSUIMessages.GenerateDiffFileWizard_0);
					getButton(IDialogConstants.OK_ID).setEnabled(false);
					return;
				} else {
					IWorkspace workspace = ResourcesPlugin.getWorkspace();
					IPath fullPath = wsSelectedContainer.getFullPath().append(
							fileName);
					if (workspace.getRoot().getFolder(fullPath).exists()) {
						setErrorMessage(CVSUIMessages.GenerateDiffFileWizard_FolderExists);
						getButton(IDialogConstants.OK_ID).setEnabled(false);
						return;
					}
				}

				setErrorMessage(null);
				getButton(IDialogConstants.OK_ID).setEnabled(true);
			}

			@Override
			protected void okPressed() {
				IFile file = wsSelectedContainer.getFile(new Path(
						wsFilenameText.getText()));
				if (file != null)
					wsPathText.setText(file.getFullPath().toString());

				validatePage();
				super.okPressed();
			}

			private IContainer getSelectedContainer() {
				Object obj = wsTreeViewer.getStructuredSelection().getFirstElement();
				if (obj instanceof IContainer) {
					wsSelectedContainer = (IContainer) obj;
				} else if (obj instanceof IFile) {
					wsSelectedContainer = ((IFile) obj).getParent();
				}
				return wsSelectedContainer;
			}

			@Override
			protected void cancelPressed() {
				validatePage();
				super.cancelPressed();
			}

			@Override
			public boolean close() {
				if (dlgTitleImage != null)
					dlgTitleImage.dispose();
				return super.close();
			}

			void setupListeners(){
				wsTreeViewer.addSelectionChangedListener(
						event -> {
							IStructuredSelection s = event.getStructuredSelection();
							Object obj=s.getFirstElement();
							if (obj instanceof IContainer)
								wsSelectedContainer = (IContainer) obj;
							else if (obj instanceof IFile){
								IFile tempFile = (IFile) obj;
								wsSelectedContainer = tempFile.getParent();
								wsFilenameText.setText(tempFile.getName());
							}
							validateDialog();
						});

				wsTreeViewer.addDoubleClickListener(
						event -> {
							ISelection s= event.getSelection();
							if (s instanceof IStructuredSelection) {
								Object item = ((IStructuredSelection)s).getFirstElement();
								if (wsTreeViewer.getExpandedState(item))
									wsTreeViewer.collapseToLevel(item, 1);
								else
									wsTreeViewer.expandToLevel(item, 1);
							}
							validateDialog();
						});

				wsFilenameText.addModifyListener(e -> {
					modified = true;
					validateDialog();
				});
			}
		}

		LocationPage(String pageName, String title, ImageDescriptor image, DefaultValuesStore store) {
			super(pageName, title, image);
			setPageComplete(false);
			this.store= store;
			this.canValidate=false;
		}

		/**
		 * Allow the user to finish if a valid file has been entered.
		 */
		protected boolean validatePage() {

			if (!canValidate)
				return false;

			switch (selectedLocation) {
			case WORKSPACE:
				pageValid= validateWorkspaceLocation();
				break;
			case FILESYSTEM:
				pageValid= validateFilesystemLocation();
				break;
			case CLIPBOARD:
				pageValid= true;
				break;
			}

			if ((resources = getSelectedResources()).length == 0) {
				pageValid = false;
				setErrorMessage(CVSUIMessages.GenerateDiffFileWizard_noChangesSelected);
			}

			/**
			 * Avoid draw flicker by clearing error message
			 * if all is valid.
			 */
			if (pageValid) {
				setMessage(null);
				setErrorMessage(null);
			}
			setPageComplete(pageValid);
			return pageValid;
		}

		/**
		 * The following conditions must hold for the file system location
		 * to be valid:
		 * - the path must be valid and non-empty
		 * - the path must be absolute
		 * - the specified file must be of type file
		 * - the parent must exist (new folders can be created via the browse button)
		 */
		private boolean validateFilesystemLocation() {
			final String pathString= fsPathText.getText().trim();
			if (pathString.length() == 0 || !new Path("").isValidPath(pathString)) { //$NON-NLS-1$
				if (fsBrowsed)
					setErrorMessage(CVSUIMessages.GenerateDiffFileWizard_0);
				else
					setErrorMessage(CVSUIMessages.GenerateDiffFileWizard_browseFilesystem);
				return false;
			}

			final File file= new File(pathString);
			if (!file.isAbsolute()) {
				setErrorMessage(CVSUIMessages.GenerateDiffFileWizard_0);
				return false;
			}

			if (file.isDirectory()) {
				setErrorMessage(CVSUIMessages.GenerateDiffFileWizard_2);
				return false;
			}

			if (pathString.endsWith("/") || pathString.endsWith("\\")) {  //$NON-NLS-1$//$NON-NLS-2$
				setErrorMessage(CVSUIMessages.GenerateDiffFileWizard_3);
				return false;
			}

			final File parent= file.getParentFile();
			if (!(parent.exists() && parent.isDirectory())) {
				setErrorMessage(CVSUIMessages.GenerateDiffFileWizard_3);
				return false;
			}
			return true;
		}

		/**
		 * The following conditions must hold for the workspace location to be valid:
		 * - a parent must be selected in the workspace tree view
		 * - the resource name must be valid
		 */
		private boolean validateWorkspaceLocation() {
			//make sure that the field actually has a filename in it - making
			//sure that the user has had a chance to browse the workspace first
			if (wsPathText.getText().equals("")){ //$NON-NLS-1$
				if (selectedLocation ==WORKSPACE && wsBrowsed)
					setErrorMessage(CVSUIMessages.GenerateDiffFileWizard_5);
				else
					setErrorMessage(CVSUIMessages.GenerateDiffFileWizard_4);
				return false;
			}

			//Make sure that all the segments but the last one (i.e. project + all
			//folders) exist - file doesn't have to exist. It may have happened that
			//some folder refactoring has been done since this path was last saved.
			//
			// The path will always be in format project/{folders}*/file - this
			// is controlled by the workspace location dialog and by
			// validatePath method when path has been entered manually.


			IPath pathToWorkspaceFile = new Path(wsPathText.getText());
			IStatus status = ResourcesPlugin.getWorkspace().validatePath(wsPathText.getText(), IResource.FILE);
			if (status.isOK()) {
				//Trim file name from path
				IPath containerPath = pathToWorkspaceFile.removeLastSegments(1);
				IResource container =ResourcesPlugin.getWorkspace().getRoot().findMember(containerPath);
				if (container == null) {
					if (selectedLocation == WORKSPACE)
						setErrorMessage(CVSUIMessages.GenerateDiffFileWizard_4);
					return false;
				} else if (!container.isAccessible()) {
					if (selectedLocation == WORKSPACE)
						setErrorMessage(CVSUIMessages.GenerateDiffFileWizard_ProjectClosed);
					return false;
				} else {
					if (ResourcesPlugin.getWorkspace().getRoot().getFolder(
							pathToWorkspaceFile).exists()) {
						setErrorMessage(CVSUIMessages.GenerateDiffFileWizard_FolderExists);
						return false;
					}
				}
			} else {
				setErrorMessage(status.getMessage());
				return false;
			}

			return true;
		}

		/**
		 * Answers a full path to a file system file or <code>null</code> if the user
		 * selected to save the patch in the clipboard.
		 */
		public File getFile() {
			if (pageValid && selectedLocation == FILESYSTEM) {
				return new File(fsPathText.getText().trim());
			}
			if (pageValid && selectedLocation == WORKSPACE) {
				final String filename= wsPathText.getText().trim();
				IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
				final IFile file= root.getFile(new Path(filename));
				return file.getLocation().toFile();
			}
			return null;
		}

		/**
		 * Answers the workspace string entered in the dialog or <code>null</code> if the user
		 * selected to save the patch in the clipboard or file system.
		 */
		public String getWorkspaceLocation() {

			if (pageValid && selectedLocation == WORKSPACE) {
				final String filename= wsPathText.getText().trim();
				return filename;
			}
			return null;
		}

		/**
		 * Get the selected workspace resource if the patch is to be saved in the
		 * workspace, or null otherwise.
		 */
		public IResource getResource() {
			if (pageValid && selectedLocation == WORKSPACE) {
				IPath pathToWorkspaceFile = new Path(wsPathText.getText().trim());
				//Trim file name from path
				IPath containerPath = pathToWorkspaceFile.removeLastSegments(1);
				return ResourcesPlugin.getWorkspace().getRoot().findMember(containerPath);
			}
			return null;
		}

		/**
		 * Allow the user to chose to save the patch to the workspace or outside
		 * of the workspace.
		 */
		@Override
		public void createControl(Composite parent) {

			final Composite composite= new Composite(parent, SWT.NULL);
			composite.setLayout(new GridLayout());
			setControl(composite);
			initializeDialogUnits(composite);

			// set F1 help
			PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.PATCH_SELECTION_PAGE);

			//Create a location group
			setupLocationControls(composite);

			initializeDefaultValues();

			fParticipant = new CreatePatchWizardParticipant(new ResourceScope(((GenerateDiffFileWizard)this.getWizard()).resources), (GenerateDiffFileWizard) this.getWizard());
			try {
				getAllOutOfSync();
			} catch (CVSException e) {}

			final PixelConverter converter= new PixelConverter(parent);
			createChangesArea(composite, converter);

			createSelectionButtons(composite);

			Dialog.applyDialogFont(parent);

			/**
			 * Ensure the page is in a valid state.
			 */
			/*if (!validatePage()) {
				store.storeRadioSelection(CLIPBOARD);
				initializeDefaultValues();
				validatePage();
			}
			pageValid= true;*/
			validatePage();

			updateEnablements();
			setupListeners();
		}


		private void createSelectionButtons(Composite composite) {
			final Composite buttonGroup = new Composite(composite,SWT.NONE);
			GridLayout layout = new GridLayout();
			layout.numColumns = 2;
			layout.marginWidth = 0;
			layout.marginHeight = 0;
			layout.horizontalSpacing = 0;
			layout.verticalSpacing = 0;
			buttonGroup.setLayout(layout);
			GridData data = new GridData(GridData.HORIZONTAL_ALIGN_END
					| GridData.VERTICAL_ALIGN_CENTER);
			buttonGroup.setLayoutData(data);

			chgSelectAll = createSelectionButton(CVSUIMessages.GenerateDiffFileWizard_SelectAll, buttonGroup);
			chgDeselectAll = createSelectionButton(CVSUIMessages.GenerateDiffFileWizard_DeselectAll, buttonGroup);
		}

		private Button createSelectionButton(String buttonName, Composite buttonGroup) {
			Button button = new Button(buttonGroup,SWT.PUSH);
			button.setText(buttonName);
			GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
			int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
			Point minSize = button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
			data.widthHint = Math.max(widthHint, minSize.x);
			button.setLayoutData(data);
			return button;
		}


		/**
		 * Setup the controls for the location.
		 */
		private void setupLocationControls(final Composite parent) {
			final Composite composite = new Composite(parent, SWT.NULL);
			GridLayout gridLayout = new GridLayout();
			gridLayout.numColumns = 3;
			composite.setLayout(gridLayout);
			composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			// clipboard
			GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
			gd.horizontalSpan = 3;
			cpRadio = new Button(composite, SWT.RADIO);
			cpRadio.setText(CVSUIMessages.Save_To_Clipboard_2);
			cpRadio.setLayoutData(gd);

			// filesystem
			fsRadio = new Button(composite, SWT.RADIO);
			fsRadio.setText(CVSUIMessages.Save_In_File_System_3);

			fsPathText = new Text(composite, SWT.BORDER);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			fsPathText.setLayoutData(gd);

			fsBrowseButton = new Button(composite, SWT.PUSH);
			fsBrowseButton.setText(CVSUIMessages.Browse____4);
			GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
			int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
			Point minSize = fsBrowseButton.computeSize(SWT.DEFAULT,
					SWT.DEFAULT, true);
			data.widthHint = Math.max(widthHint, minSize.x);
			fsBrowseButton.setLayoutData(data);

			// workspace
			wsRadio = new Button(composite, SWT.RADIO);
			wsRadio.setText(CVSUIMessages.Save_In_Workspace_7);

			wsPathText = new Text(composite, SWT.BORDER);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			wsPathText.setLayoutData(gd);

			wsBrowseButton = new Button(composite, SWT.PUSH);
			wsBrowseButton.setText(CVSUIMessages.Browse____4);
			data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
			widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
			minSize = fsBrowseButton
			.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
			data.widthHint = Math.max(widthHint, minSize.x);
			wsBrowseButton.setLayoutData(data);

			// change the cpRadio layout to be of the same height as other rows' layout
			((GridData)cpRadio.getLayoutData()).heightHint = minSize.y;
		}

		private ParticipantPagePane fPagePane;
		private PageBook bottomChild;
		private ISynchronizePageConfiguration fConfiguration;

		private void createChangesArea(Composite parent, PixelConverter converter) {

			int size = fParticipant.getSyncInfoSet().size();
			if (size > getFileDisplayThreshold()) {
				// Create a page book to allow eventual inclusion of changes
				bottomChild = new PageBook(parent, SWT.NONE);
				bottomChild.setLayoutData(SWTUtils.createGridData(SWT.DEFAULT, SWT.DEFAULT, SWT.FILL, SWT.FILL, true, false));
				// Create composite for showing the reason for not showing the changes and a button to show them
				Composite changeDesc = new Composite(bottomChild, SWT.NONE);
				changeDesc.setLayout(SWTUtils.createGridLayout(1, converter, SWTUtils.MARGINS_NONE));
				SWTUtils.createLabel(changeDesc, NLS.bind(CVSUIMessages.CommitWizardCommitPage_1, new String[] { Integer.toString(size), Integer.toString(getFileDisplayThreshold()) }));
				Button showChanges = new Button(changeDesc, SWT.PUSH);
				showChanges.setText(CVSUIMessages.CommitWizardCommitPage_5);
				showChanges.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						showChangesPane();
					}
				});
				showChanges.setLayoutData(new GridData());
				bottomChild.showPage(changeDesc);
			} else {
				final Composite composite= new Composite(parent, SWT.NONE);
				composite.setLayout(SWTUtils.createGridLayout(1, converter, SWTUtils.MARGINS_NONE));
				composite.setLayoutData(SWTUtils.createGridData(SWT.DEFAULT, SWT.DEFAULT, SWT.FILL, SWT.FILL, true, true));

				createPlaceholder(composite);

				Control c = createChangesPage(composite, fParticipant);
				c.setLayoutData(SWTUtils.createHVFillGridData());
			}
		}

		protected void showChangesPane() {
			Control c = createChangesPage(bottomChild, fParticipant);
			bottomChild.setLayoutData(SWTUtils.createGridData(SWT.DEFAULT, SWT.DEFAULT, SWT.FILL, SWT.FILL, true, true));
			bottomChild.showPage(c);
			Dialog.applyDialogFont(getControl());
			((Composite)getControl()).layout();
		}

		private Control createChangesPage(final Composite composite, WorkspaceSynchronizeParticipant participant) {
			fConfiguration= participant.createPageConfiguration();
			fPagePane= new ParticipantPagePane(getShell(), true /* modal */, fConfiguration, participant);
			Control control = fPagePane.createPartControl(composite);
			return control;
		}

		@Override
		public void dispose() {
			if (fPagePane != null)
				fPagePane.dispose();
			if (fParticipant != null)
				fParticipant.dispose();
			super.dispose();
		}

		private int getFileDisplayThreshold() {
			return CVSUIPlugin.getPlugin().getPreferenceStore().getInt(ICVSUIConstants.PREF_COMMIT_FILES_DISPLAY_THRESHOLD);
		}

		private void createPlaceholder(final Composite composite) {
			final Composite placeholder= new Composite(composite, SWT.NONE);
			placeholder.setLayoutData(new GridData(SWT.DEFAULT, convertHorizontalDLUsToPixels(IDialogConstants.VERTICAL_SPACING) /3));
		}
		/**
		 * Initialize the controls with the saved default values which are
		 * obtained from the DefaultValuesStore.
		 */
		private void initializeDefaultValues() {

			selectedLocation= store.getLocationSelection();

			updateRadioButtons();

			/**
			 * Text fields.
			 */
			// We need to ensure that we have a valid workspace path - user
			//could have altered workspace since last time this was saved
			wsPathText.setText(store.getWorkspacePath());
			if(!validateWorkspaceLocation()) {
				wsPathText.setText(""); //$NON-NLS-1$

				//Don't open wizard with an error - instead change selection
				//to clipboard
				if (selectedLocation == WORKSPACE){
					//clear the error message caused by the workspace not having
					//any workspace path entered
					setErrorMessage(null);
					selectedLocation=CLIPBOARD;
					updateRadioButtons();
				}
			}
			// Do the same thing for the filesystem field
			fsPathText.setText(store.getFilesystemPath());
			if (!validateFilesystemLocation()) {
				fsPathText.setText(""); //$NON-NLS-1$
				if (selectedLocation == FILESYSTEM) {
					setErrorMessage(null);
					selectedLocation = CLIPBOARD;
					updateRadioButtons();
				}
			}

		}

		private void updateRadioButtons() {
			/**
			 * Radio buttons
			 */
			cpRadio.setSelection(selectedLocation == CLIPBOARD);
			fsRadio.setSelection(selectedLocation == FILESYSTEM);
			wsRadio.setSelection(selectedLocation == WORKSPACE);
		}

		/**
		 * Setup all the listeners for the controls.
		 */
		private void setupListeners() {

			cpRadio.addListener(SWT.Selection, event -> {
				selectedLocation= CLIPBOARD;
				validatePage();
				updateEnablements();
			});
			fsRadio.addListener(SWT.Selection, event -> {
				selectedLocation= FILESYSTEM;
				validatePage();
				updateEnablements();
			});

			wsRadio.addListener(SWT.Selection, event -> {
				selectedLocation= WORKSPACE;
				validatePage();
				updateEnablements();
			});

			ModifyListener pathTextModifyListener = e -> validatePage();
			fsPathText.addModifyListener(pathTextModifyListener);
			wsPathText.addModifyListener(pathTextModifyListener);

			fsBrowseButton.addListener(SWT.Selection, event -> {
				final FileDialog dialog = new FileDialog(getShell(), SWT.PRIMARY_MODAL | SWT.SAVE);
				if (pageValid) {
					final File file= new File(fsPathText.getText());
					dialog.setFilterPath(file.getParent());
				}
				dialog.setText(CVSUIMessages.Save_Patch_As_5);
				dialog.setFileName(CVSUIMessages.patch_txt_6);
				final String path = dialog.open();
				fsBrowsed = true;
				if (path != null) {
					fsPathText.setText(new Path(path).toOSString());
				}
				validatePage();
			});



			wsBrowseButton.addListener(SWT.Selection, event -> {
				final WorkspaceDialog dialog = new WorkspaceDialog(getShell());
				wsBrowsed = true;
				dialog.open();
				validatePage();
			});


			chgSelectAll.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					initCheckedItems();
					//Only bother changing isPageComplete state if the current state
					//is not enabled
					if (!isPageComplete())
						setPageComplete(validatePage());
				}
			});

			chgDeselectAll.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					ISynchronizePage page = fConfiguration.getPage();
					if (page != null){
						Viewer viewer = page.getViewer();
						if (viewer instanceof CheckboxTreeViewer) {
							CheckboxTreeViewer treeViewer =(CheckboxTreeViewer)viewer;
							treeViewer.setCheckedElements(new Object[0]);
						}
					}
					//Only bother changing isPageComplete state if the current state
					//is enabled
					if (isPageComplete())
						setPageComplete(validatePage());
				}
			});

			ISynchronizePage page = fConfiguration.getPage();
			if (page != null) {
				Viewer viewer = page.getViewer();
				if (viewer instanceof CheckboxTreeViewer) {
					((CheckboxTreeViewer)viewer).addCheckStateListener(event -> setPageComplete(validatePage()));
				}
			}
		}

		protected void initCheckedItems() {
			ISynchronizePage page = fConfiguration.getPage();
			if (page != null) {
				Viewer viewer = page.getViewer();
				if (viewer instanceof CheckboxTreeViewer) {
					TreeItem[] items=((CheckboxTreeViewer)viewer).getTree().getItems();
					for (TreeItem item : items) {
						((CheckboxTreeViewer)viewer).setChecked(item.getData(), true);
					}
				}
			}
		}

		protected IResource[] getSelectedResources() {
			ISynchronizePage page = fConfiguration.getPage();
			if (page != null) {
				Viewer viewer = page.getViewer();
				if (viewer instanceof CheckboxTreeViewer) {
					Object[] elements = ((CheckboxTreeViewer)viewer).getCheckedElements();
					IResource[]selectedResources = Utils.getResources(elements);
					ArrayList<IResource> result = new ArrayList<>();
					for (IResource resource : selectedResources) {
						if (fConfiguration.getSyncInfoSet().getSyncInfo(resource) != null) {
							result.add(resource);
						}
					}
					return result.toArray(new IResource[result.size()]);
				}
			}
			return new IResource[0];
		}

		/**
		 * Enable and disable controls based on the selected radio button.
		 */
		public void updateEnablements() {
			fsBrowseButton.setEnabled(selectedLocation == FILESYSTEM);
			fsPathText.setEnabled(selectedLocation == FILESYSTEM);
			if (selectedLocation == FILESYSTEM)
				fsBrowsed=false;
			wsPathText.setEnabled(selectedLocation == WORKSPACE);
			wsBrowseButton.setEnabled(selectedLocation == WORKSPACE);
			if (selectedLocation == WORKSPACE)
				wsBrowsed=false;
		}

		public int getSelectedLocation() {
			return selectedLocation;
		}

		private SyncInfoSet getAllOutOfSync() throws CVSException {
			final SubscriberSyncInfoCollector syncInfoCollector = fParticipant.getSubscriberSyncInfoCollector();
			//WaitForChangesJob waits for the syncInfoCollector to get all the changes
			//before checking off the tree items and validating the page
			class WaitForChangesJob extends Job{
				LocationPage fLocationPage;

				public WaitForChangesJob(LocationPage page) {
					super(""); //$NON-NLS-1$
					fLocationPage=page;
				}
				@Override
				public IStatus run(IProgressMonitor monitor) {
					monitor.beginTask(CVSUIMessages.CommitWizard_4, IProgressMonitor.UNKNOWN);
					syncInfoCollector.waitForCollector(monitor);
					Utils.syncExec((Runnable) () -> {
						fLocationPage.initCheckedItems();
						fLocationPage.canValidate=true;
						fLocationPage.validatePage();
					}, getControl());
					monitor.done();
					return Status.OK_STATUS;
				}
			}
			WaitForChangesJob job =new WaitForChangesJob(this);
			//Don't need the job in the UI, make it a system job
			job.setSystem(true);
			job.schedule();
			return fParticipant.getSyncInfoSet();
		}

		public IFile findBinaryFile() {
			try {
				final IFile[] found = new IFile[1];
				fParticipant.getSubscriber().accept(resources, IResource.DEPTH_INFINITE, (IDiffVisitor) diff -> {
					if (isBinaryFile(diff))
						found[0] = getFile(diff);
					return true;
				});
				return found[0];
			} catch (CoreException e) {
				CVSUIPlugin.log(e);
			}
			return null;
		}

		protected boolean isBinaryFile(IDiff diff) {
			IFile file = getFile(diff);
			if (file != null) {
				ICVSFile cvsFile = CVSWorkspaceRoot.getCVSFileFor(file);
				try {
					byte[] bytes = cvsFile.getSyncBytes();
					if (bytes != null) {
						return ResourceSyncInfo.getKeywordMode(bytes).toMode().equals(
								Command.KSUBST_BINARY.toMode());
					}
				} catch (CVSException e) {
					CVSUIPlugin.log(e);
				}
				return (Team.getFileContentManager().getType(file) == Team.BINARY);
			}
			return false;
		}

		protected IFile getFile(IDiff diff) {
			IResource resource = ResourceDiffTree.getResourceFor(diff);
			if (resource instanceof IFile) {
				IFile file = (IFile) resource;
				return file;
			}
			return null;
		}

		public void removeBinaryFiles() {
			try {
				final List<IFile> nonBinaryFiles = new ArrayList<>();
				fParticipant.getSubscriber().accept(resources, IResource.DEPTH_INFINITE, (IDiffVisitor) diff -> {
					if (!isBinaryFile(diff)) {
						IFile file = getFile(diff);
						if (file != null)
							nonBinaryFiles.add(file);
					}
					return true;
				});
				resources = nonBinaryFiles
				.toArray(new IResource[nonBinaryFiles.size()]);
			} catch (CoreException e) {
				CVSUIPlugin.log(e);
			}
		}

	}

	/**
	 * Page to select the options for creating the patch.
	 */
	private class OptionsPage extends WizardPage {

		/**
		 * The possible file format to save a patch.
		 */
		public final static int FORMAT_UNIFIED = 1;
		public final static int FORMAT_CONTEXT = 2;
		public final static int FORMAT_STANDARD = 3;

		/**
		The possible root of the patch
		 */
		public final static int ROOT_WORKSPACE = 1;
		public final static int ROOT_PROJECT = 2;
		public final static int ROOT_SELECTION = 3;

		private Button unifiedDiffOption;
		private Button unified_workspaceRelativeOption; //multi-patch format
		private Button unified_projectRelativeOption; //full project path
		private Button unified_selectionRelativeOption; //use path of whatever is selected
		private Button contextDiffOption;
		private Button regularDiffOption;
		private final RadioButtonGroup diffTypeRadioGroup = new RadioButtonGroup();
		private final RadioButtonGroup unifiedRadioGroup = new RadioButtonGroup();

		private boolean patchHasCommonRoot=true;
		protected IPath patchRoot=ResourcesPlugin.getWorkspace().getRoot().getFullPath();

		private final DefaultValuesStore store;

		/**
		 * Constructor for PatchFileCreationOptionsPage.
		 * 
		 * @param pageName
		 *            the name of the page
		 * 
		 * @param title
		 *            the title for this wizard page, or <code>null</code> if
		 *            none
		 * @param titleImage
		 *            the image descriptor for the title of this wizard page, or
		 *            <code>null</code> if none
		 * @param store
		 *            the value store where the page stores it's data
		 */
		protected OptionsPage(String pageName, String title,
				ImageDescriptor titleImage, DefaultValuesStore store) {
			super(pageName, title, titleImage);
			this.store = store;
		}

		@Override
		public void createControl(Composite parent) {
			Composite composite= new Composite(parent, SWT.NULL);
			GridLayout layout= new GridLayout();
			composite.setLayout(layout);
			composite.setLayoutData(new GridData());
			setControl(composite);

			// set F1 help
			PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.PATCH_OPTIONS_PAGE);

			Group diffTypeGroup = new Group(composite, SWT.NONE);
			layout = new GridLayout();
			diffTypeGroup.setLayout(layout);
			GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
			diffTypeGroup.setLayoutData(data);
			diffTypeGroup.setText(CVSUIMessages.Diff_output_format_12);

			unifiedDiffOption = new Button(diffTypeGroup, SWT.RADIO);
			unifiedDiffOption.setText(CVSUIMessages.Unified__format_required_by_Compare_With_Patch_feature__13);

			contextDiffOption = new Button(diffTypeGroup, SWT.RADIO);
			contextDiffOption.setText(CVSUIMessages.Context_14);
			regularDiffOption = new Button(diffTypeGroup, SWT.RADIO);
			regularDiffOption.setText(CVSUIMessages.Standard_15);

			diffTypeRadioGroup.add(FORMAT_UNIFIED, unifiedDiffOption);
			diffTypeRadioGroup.add(FORMAT_CONTEXT, contextDiffOption);
			diffTypeRadioGroup.add(FORMAT_STANDARD, regularDiffOption);

			//Unified Format Options
			Group unifiedGroup = new Group(composite, SWT.None);
			layout = new GridLayout();
			unifiedGroup.setLayout(layout);
			data = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
			unifiedGroup.setLayoutData(data);
			unifiedGroup.setText(CVSUIMessages.GenerateDiffFileWizard_10);

			unified_workspaceRelativeOption = new Button(unifiedGroup, SWT.RADIO);
			unified_workspaceRelativeOption.setText(CVSUIMessages.GenerateDiffFileWizard_6);
			unified_workspaceRelativeOption.setSelection(true);

			unified_projectRelativeOption = new Button(unifiedGroup, SWT.RADIO);
			unified_projectRelativeOption.setText(CVSUIMessages.GenerateDiffFileWizard_7);

			unified_selectionRelativeOption = new Button(unifiedGroup, SWT.RADIO);
			unified_selectionRelativeOption.setText(CVSUIMessages.GenerateDiffFileWizard_8);

			unifiedRadioGroup.add(ROOT_WORKSPACE, unified_workspaceRelativeOption);
			unifiedRadioGroup.add(ROOT_PROJECT, unified_projectRelativeOption);
			unifiedRadioGroup.add(ROOT_SELECTION, unified_selectionRelativeOption);

			Dialog.applyDialogFont(parent);

			initializeDefaultValues();

			//add listeners
			unifiedDiffOption.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					setEnableUnifiedGroup(true);
					updateEnablements();
					diffTypeRadioGroup.setSelection(FORMAT_UNIFIED, false);
				}
			});

			contextDiffOption.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					setEnableUnifiedGroup(false);
					updateEnablements();
					diffTypeRadioGroup.setSelection(FORMAT_CONTEXT, false);
				}
			});

			regularDiffOption.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					setEnableUnifiedGroup(false);
					updateEnablements();
					diffTypeRadioGroup.setSelection(FORMAT_STANDARD, false);
				}
			});

			unified_workspaceRelativeOption
			.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					unifiedRadioGroup.setSelection(ROOT_WORKSPACE, false);
				}
			});

			unified_projectRelativeOption
			.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					unifiedRadioGroup.setSelection(ROOT_PROJECT, false);
				}
			});

			unified_selectionRelativeOption
			.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					unifiedRadioGroup.setSelection(ROOT_SELECTION, false);
				}
			});

			calculatePatchRoot();
			updateEnablements();

			// update selection
			diffTypeRadioGroup.selectEnabledOnly();
			unifiedRadioGroup.selectEnabledOnly();
		}

		public int getFormatSelection() {
			return diffTypeRadioGroup.getSelected();
		}

		public int getRootSelection() {
			return unifiedRadioGroup.getSelected();
		}

		private void initializeDefaultValues() {
			// Radio buttons for format
			diffTypeRadioGroup.setSelection(store.getFormatSelection(), true);
			// Radio buttons for patch root
			unifiedRadioGroup.setSelection(store.getRootSelection(), true);

			if (store.getFormatSelection() != FORMAT_UNIFIED) {
				setEnableUnifiedGroup(false);
			}
		}


		protected void updateEnablements() {
			if (!patchHasCommonRoot){
				diffTypeRadioGroup.setEnablement(false, new int[] {
						FORMAT_CONTEXT, FORMAT_STANDARD }, FORMAT_UNIFIED);
				unifiedRadioGroup.setEnablement(false, new int[] {
						ROOT_PROJECT, ROOT_SELECTION }, ROOT_WORKSPACE);
			}

			// temporary until we figure out best way to fix synchronize view
			// selection
			if (!unifiedSelectionEnabled)
				unifiedRadioGroup.setEnablement(false, new int[] {ROOT_SELECTION});
		}

		private void calculatePatchRoot(){
			//check to see if this is a multi select patch, if so disable
			IResource[] tempResources = ((GenerateDiffFileWizard)this.getWizard()).resources;

			//Guard for quick cancellation to avoid ArrayOutOfBounds (see Bug# 117234)
			if (tempResources == null)
				return;

			if (tempResources.length > 1){
				//Check to see is the selected resources are contained by the same parent (climbing
				//parent by parent to the project root)
				//If so, then allow selection relative patches -> set the relative path to the common
				//parent [also allow project relative patches]
				//If parents are different projects, allow only multiproject selection

				patchHasCommonRoot=true;
				int segmentMatch=-1;
				IPath path = tempResources[0].getFullPath().removeLastSegments(1);
				for (int i = 1; i < tempResources.length; i++) {
					int segments=path.matchingFirstSegments(tempResources[i].getFullPath());
					//Keep track of the lowest number of matches that were found - the common
					//path will be this number
					if (segmentMatch == -1 ||
							segmentMatch>segments){
						segmentMatch=segments;
					}
					//However, if no segments for any one resource - break out of the loop
					if (segments == 0){
						patchHasCommonRoot=false;
						break;
					}
				}
				if (patchHasCommonRoot){
					IPath tempPath = path.uptoSegment(segmentMatch);
					/*IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
					while (!root.exists(tempPath) &&
							!tempPath.isRoot()){
						tempPath = tempPath.removeLastSegments(1);
					}*/
					patchRoot=tempPath;
				}
			} else {
				IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

				//take the file name off the path and use that as the patch root
				//patchRoot = tempResources[0].getFullPath().removeLastSegments(1);
				IPath fullPath = tempResources[0].getFullPath();
				IResource resource = root.findMember(fullPath);

				//keep trimming the path until we find something that can be used as the
				//patch root
				while (resource == null &&
						!(resource instanceof IWorkspaceRoot)){
					fullPath=fullPath.removeLastSegments(1);
					resource=root.findMember(fullPath);
				}
				patchRoot = resource.getFullPath();
				if (resource.getType() == IResource.FILE)
					patchRoot =resource.getFullPath().removeLastSegments(1);
			}
		}
		/**
		 * Return the list of Diff command options configured on this page.
		 */
		public LocalOption[] getOptions() {
			List<LocalOption> options = new ArrayList<>(5);
			/*  if(includeNewFilesOptions.getSelection()) {
				options.add(Diff.INCLUDE_NEWFILES);
			}
			if(!recurseOption.getSelection()) {
				options.add(Command.DO_NOT_RECURSE);
			}*/

			//Add new files for now
			options.add(Diff.INCLUDE_NEWFILES);

			if(unifiedDiffOption.getSelection()) {
				options.add(Diff.UNIFIED_FORMAT);
			} else if(contextDiffOption.getSelection()) {
				options.add(Diff.CONTEXT_FORMAT);
			}

			return options.toArray(new LocalOption[options.size()]);
		}
		protected void setEnableUnifiedGroup(boolean enabled){
			unifiedRadioGroup.setEnablement(enabled, new int[] {
					ROOT_WORKSPACE, ROOT_PROJECT, ROOT_SELECTION });

			//temporary until we figure out best way to fix synchronize view selection
			if (!unifiedSelectionEnabled)
				unifiedRadioGroup.setEnablement(false, new int[] {ROOT_SELECTION});
		}
	}

	/**
	 * Class to retrieve and store the default selected values.
	 */
	private final class DefaultValuesStore {

		private static final String PREF_LAST_SELECTION= "org.eclipse.team.internal.ccvs.ui.wizards.GenerateDiffFileWizard.PatchFileSelectionPage.lastselection"; //$NON-NLS-1$
		private static final String PREF_LAST_FS_PATH= "org.eclipse.team.internal.ccvs.ui.wizards.GenerateDiffFileWizard.PatchFileSelectionPage.filesystem.path"; //$NON-NLS-1$
		private static final String PREF_LAST_WS_PATH= "org.eclipse.team.internal.ccvs.ui.wizards.GenerateDiffFileWizard.PatchFileSelectionPage.workspace.path"; //$NON-NLS-1$
		private static final String PREF_LAST_AO_FORMAT = "org.eclipse.team.internal.ccvs.ui.wizards.GenerateDiffFileWizard.OptionsPage.diff.format"; //$NON-NLS-1$
		private static final String PREF_LAST_AO_ROOT = "org.eclipse.team.internal.ccvs.ui.wizards.GenerateDiffFileWizard.OptionsPage.patch.root"; //$NON-NLS-1$


		private final IDialogSettings dialogSettings;

		public DefaultValuesStore() {
			dialogSettings= CVSUIPlugin.getPlugin().getDialogSettings();
		}

		public int getLocationSelection() {
			int value= LocationPage.CLIPBOARD;
			try {
				value= dialogSettings.getInt(PREF_LAST_SELECTION);
			} catch (NumberFormatException e) {
				// ignore
			}

			switch (value) {
			case LocationPage.FILESYSTEM:
			case LocationPage.WORKSPACE:
			case LocationPage.CLIPBOARD:
				return value;
			default:
				return LocationPage.CLIPBOARD;
			}
		}

		public String getFilesystemPath() {
			final String path= dialogSettings.get(PREF_LAST_FS_PATH);
			return path != null ? path : "";  //$NON-NLS-1$
		}

		public String getWorkspacePath() {
			final String path= dialogSettings.get(PREF_LAST_WS_PATH);
			return path != null ? path : ""; //$NON-NLS-1$
		}


		public int getFormatSelection() {
			int value = OptionsPage.FORMAT_UNIFIED;
			try {
				value = dialogSettings.getInt(PREF_LAST_AO_FORMAT);
			} catch (NumberFormatException e) {
			}

			switch (value) {
			case OptionsPage.FORMAT_UNIFIED:
			case OptionsPage.FORMAT_CONTEXT:
			case OptionsPage.FORMAT_STANDARD:
				return value;
			default:
				return OptionsPage.FORMAT_UNIFIED;
			}
		}

		public int getRootSelection() {
			int value = OptionsPage.ROOT_WORKSPACE;
			try {
				value = dialogSettings.getInt(PREF_LAST_AO_ROOT);
			} catch (NumberFormatException e) {
			}

			switch (value) {
			case OptionsPage.ROOT_WORKSPACE:
			case OptionsPage.ROOT_PROJECT:
			case OptionsPage.ROOT_SELECTION:
				return value;
			default:
				return OptionsPage.ROOT_WORKSPACE;
			}
		}

		public void storeLocationSelection(int defaultSelection) {
			dialogSettings.put(PREF_LAST_SELECTION, defaultSelection);
		}

		public void storeFilesystemPath(String path) {
			dialogSettings.put(PREF_LAST_FS_PATH, path);
		}

		public void storeWorkspacePath(String path) {
			dialogSettings.put(PREF_LAST_WS_PATH, path);
		}

		public void storeOutputFormat(int selection) {
			dialogSettings.put(PREF_LAST_AO_FORMAT, selection);
		}

		public void storePatchRoot(int selection) {
			dialogSettings.put(PREF_LAST_AO_ROOT, selection);
		}
	}

	private LocationPage locationPage;
	private OptionsPage optionsPage;

	protected IResource[] resources;
	private final DefaultValuesStore defaultValuesStore;
	private final IWorkbenchPart part;

	//temporary until we figure out best way to fix synchronize view selection
	protected boolean unifiedSelectionEnabled;

	public GenerateDiffFileWizard(IWorkbenchPart part, IResource[] resources, boolean unifiedSelectionEnabled) {
		super();
		this.part = part;
		this.resources = resources;
		setWindowTitle(CVSUIMessages.GenerateCVSDiff_title);
		initializeDefaultPageImageDescriptor();
		defaultValuesStore= new DefaultValuesStore();
		this.unifiedSelectionEnabled=unifiedSelectionEnabled;
	}

	@Override
	public void addPages() {
		String pageTitle = CVSUIMessages.GenerateCVSDiff_pageTitle;
		String pageDescription = CVSUIMessages.GenerateCVSDiff_pageDescription;
		locationPage = new LocationPage(pageTitle, pageTitle, CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_WIZBAN_DIFF), defaultValuesStore);
		locationPage.setDescription(pageDescription);
		addPage(locationPage);

		pageTitle = CVSUIMessages.Advanced_options_19;
		pageDescription = CVSUIMessages.Configure_the_options_used_for_the_CVS_diff_command_20;
		optionsPage = new OptionsPage(pageTitle, pageTitle, CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_WIZBAN_DIFF), defaultValuesStore);
		optionsPage.setDescription(pageDescription);
		addPage(optionsPage);
	}

	/**
	 * Declares the wizard banner iamge descriptor
	 */
	protected void initializeDefaultPageImageDescriptor() {
		final String iconPath= "icons/full/"; //$NON-NLS-1$
		try {
			final URL installURL = CVSUIPlugin.getPlugin().getBundle().getEntry("/"); //$NON-NLS-1$
			final URL url = new URL(installURL, iconPath + "wizards/newconnect_wiz.gif");	//$NON-NLS-1$
			ImageDescriptor desc = ImageDescriptor.createFromURL(url);
			setDefaultPageImageDescriptor(desc);
		} catch (MalformedURLException e) {
			// Should not happen.  Ignore.
		}
	}

	@Override
	public boolean needsProgressMonitor() {
		return true;
	}

	/**
	 * Completes processing of the wizard. If this method returns <code>
	 * true</code>, the wizard will close; otherwise, it will stay active.
	 */
	@Override
	public boolean performFinish() {

		final int location= locationPage.getSelectedLocation();

		final File file= location != LocationPage.CLIPBOARD? locationPage.getFile() : null;

		if (!(file == null || validateFile(file))) {
			return false;
		}

		//Is this a multi-patch?
		boolean multiPatch=false;
		if (optionsPage.unifiedDiffOption.getSelection() && optionsPage.unified_workspaceRelativeOption.getSelection())
			multiPatch=true;


		//If not a multipatch, patch should use project relative or selection relative paths[default]?
		boolean useProjectRelativePaths=false;
		if (optionsPage.unifiedDiffOption.getSelection() &&
				optionsPage.unified_projectRelativeOption.getSelection())
			useProjectRelativePaths=true;

		IFile binFile = locationPage.findBinaryFile();
		if (binFile != null) {
			int result = promptToIncludeBinary(binFile);
			if (result == 2)
				return false;
			if (result == 1)
				locationPage.removeBinaryFiles();
		}

		/**
		 * Perform diff operation.
		 */
		try {
			if (file != null) {
				generateDiffToFile(file,multiPatch,useProjectRelativePaths);
			} else {
				generateDiffToClipboard(multiPatch,useProjectRelativePaths);
			}
		} catch (TeamException e) {}

		/**
		 * Refresh workspace if necessary and save default selection.
		 */
		switch (location) {

		case LocationPage.WORKSPACE:
			final String workspaceResource= locationPage.getWorkspaceLocation();
			if (workspaceResource != null){
				defaultValuesStore.storeLocationSelection(LocationPage.WORKSPACE);
				defaultValuesStore.storeWorkspacePath(workspaceResource);
				/* try {
					workspaceResource.getParent().refreshLocal(IResource.DEPTH_ONE, null);
				} catch(CoreException e) {
					CVSUIPlugin.openError(getShell(), CVSUIMessages.GenerateCVSDiff_error, null, e);
					return false;
				} */
			} else {
				//Problem with workspace location, open with clipboard next time
				defaultValuesStore.storeLocationSelection(LocationPage.CLIPBOARD);
			}
			break;

		case LocationPage.FILESYSTEM:
			defaultValuesStore.storeFilesystemPath(file.getPath());
			defaultValuesStore.storeLocationSelection(LocationPage.FILESYSTEM);
			break;

		case LocationPage.CLIPBOARD:
			defaultValuesStore.storeLocationSelection(LocationPage.CLIPBOARD);
			break;

		default:
			return false;
		}


		/**
		 * Save default selections of Options Page
		 */

		defaultValuesStore.storeOutputFormat(optionsPage.getFormatSelection());
		defaultValuesStore.storePatchRoot(optionsPage.getRootSelection());

		return true;
	}

	private int promptToIncludeBinary(IFile file) {
		MessageDialog dialog = new MessageDialog(getShell(), CVSUIMessages.GenerateDiffFileWizard_11, null, // accept
				// the default window icon
				NLS.bind(CVSUIMessages.GenerateDiffFileWizard_12, file.getFullPath()), MessageDialog.WARNING, new String[] { IDialogConstants.YES_LABEL,
			IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL }, 1); // no is the default
		return dialog.open();
	}

	private void generateDiffToClipboard(boolean multiPatch, boolean useProjectRelativePaths) throws TeamException {
		DiffOperation diffop = new ClipboardDiffOperation(part,RepositoryProviderOperation.asResourceMappers(resources, IResource.DEPTH_ZERO),optionsPage.getOptions(),multiPatch, useProjectRelativePaths, optionsPage.patchRoot);
		try {
			diffop.run();
		} catch (InvocationTargetException e) {}
		catch (InterruptedException e) {}
	}

	private void generateDiffToFile(File file, boolean multiPatch, boolean useProjectRelativePaths) throws TeamException {
		DiffOperation diffop = null;
		if (locationPage.selectedLocation == LocationPage.WORKSPACE){
			diffop = new WorkspaceFileDiffOperation(part,RepositoryProviderOperation.asResourceMappers(resources, IResource.DEPTH_ZERO),optionsPage.getOptions(),file, multiPatch, useProjectRelativePaths, optionsPage.patchRoot);
		}
		else {
			diffop = new FileDiffOperation(part,RepositoryProviderOperation.asResourceMappers(resources, IResource.DEPTH_ZERO),optionsPage.getOptions(),file, multiPatch, useProjectRelativePaths, optionsPage.patchRoot);
		}

		try {
			diffop.run();
		} catch (InvocationTargetException e) {}
		catch (InterruptedException e) {}
	}

	public boolean validateFile(File file) {

		if (file == null)
			return false;

		/**
		 * Consider file valid if it doesn't exist for now.
		 */
		if (!file.exists())
			return true;

		/**
		 * The file exists.
		 */
		if (!file.canWrite()) {
			final String title= CVSUIMessages.GenerateCVSDiff_1;
			final String msg= CVSUIMessages.GenerateCVSDiff_2;
			final MessageDialog dialog= new MessageDialog(getShell(), title, null, msg, MessageDialog.ERROR, new String[] { IDialogConstants.OK_LABEL }, 0);
			dialog.open();
			return false;
		}

		final String title = CVSUIMessages.GenerateCVSDiff_overwriteTitle;
		final String msg = CVSUIMessages.GenerateCVSDiff_overwriteMsg;
		final MessageDialog dialog = new MessageDialog(getShell(), title, null, msg, MessageDialog.QUESTION, new String[] { IDialogConstants.YES_LABEL, IDialogConstants.CANCEL_LABEL }, 0);
		dialog.open();
		if (dialog.getReturnCode() != 0)
			return false;

		return true;
	}

	public LocationPage getLocationPage() {
		return locationPage;
	}

	/**
	 * The class maintain proper selection of radio button within the group:
	 * <ul>
	 * <li>Only one button can be selected at the time.</li>
	 * <li>Disabled button can't be selected unless all buttons in the group
	 * are disabled.</li>
	 * </ul>
	 */
	/*private*/ class RadioButtonGroup {

		/**
		 * List of buttons in the group. Both radio groups contain 3 elements.
		 */
		private List<Button> buttons = new ArrayList<>(3);

		/**
		 * Index of the selected button.
		 */
		private int selected = 0;

		/**
		 * Add a button to the group. While adding a new button the method
		 * checks if there is only one button selected in the group.
		 * 
		 * @param buttonCode
		 *            A button's code (eg. <code>ROOT_WORKSPACE</code>). To get
		 *            an index we need to subtract 1 from it.
		 * @param button
		 *            A button to add.
		 */
		public void add(int buttonCode, Button button) {
			if (button != null && (button.getStyle() & SWT.RADIO) != 0) {
				if (button.getSelection() && !buttons.isEmpty()) {
					deselectAll();
					selected = buttonCode - 1;
				}
				buttons.add(buttonCode - 1, button);
			}
		}

		/**
		 * Returns selected button's code.
		 * 
		 * @return Selected button's code.
		 */
		public int getSelected() {
			return selected + 1;
		}

		/**
		 * Set selection to the given button. When
		 * <code>selectEnabledOnly</code> flag is true the returned value can
		 * differ from the parameter when a button we want to set selection to
		 * is disabled and there are other buttons which are enabled.
		 * 
		 * @param buttonCode
		 *            A button's code (eg. <code>ROOT_WORKSPACE</code>). To get
		 *            an index we need to subtract 1 from it.
		 * @return Code of the button to which selection was finally set.
		 */
		public int setSelection(int buttonCode, boolean selectEnabledOnly) {
			deselectAll();

			buttons.get(buttonCode - 1).setSelection(true);
			selected = buttonCode - 1;
			if (selectEnabledOnly)
				selected = selectEnabledOnly() - 1;
			return getSelected();
		}

		/**
		 * Make sure that only an enabled radio button is selected.
		 * 
		 * @return A code of the selected button.
		 */
		public int selectEnabledOnly() {
			deselectAll();

			Button selectedButton = buttons.get(selected);
			if (!selectedButton.isEnabled()) {
				// if the button is disabled, set selection to an enabled one
				for (Iterator iterator = buttons.iterator(); iterator.hasNext();) {
					Button b = (Button) iterator.next();
					if (b.isEnabled()) {
						b.setSelection(true);
						selected = buttons.indexOf(b);
						return selected + 1;
					}
				}
				// if none found, reset the initial selection
				selectedButton.setSelection(true);
			} else {
				// because selection has been cleared, set it again
				selectedButton.setSelection(true);
			}
			// return selected button's code so the value can be stored
			return getSelected();
		}

		/**
		 * Enable or disable given buttons.
		 * 
		 * @param enabled
		 *            Indicates whether to enable or disable the buttons.
		 * @param buttonsToChange
		 *            Buttons to enable/disable.
		 * @param defaultSelection
		 *            The button to select if the currently selected button
		 *            becomes disabled.
		 */
		public void setEnablement(boolean enabled, int[] buttonsToChange,
				int defaultSelection) {

			// enable (or disable) given buttons
			for (int i = 0; i < buttonsToChange.length; i++) {
				this.buttons.get(buttonsToChange[i] - 1)
				.setEnabled(enabled);
			}
			// check whether the selected button is enabled
			if (!this.buttons.get(selected).isEnabled()) {
				if (defaultSelection != -1)
					// set the default selection and check if it's enabled
					setSelection(defaultSelection, true);
				else
					// no default selection is given, select any enabled button
					selectEnabledOnly();
			}
		}

		/**
		 * Enable or disable given buttons with no default selection. The selection
		 * will be set to an enabled button using the <code>selectEnabledOnly</code> method.
		 * 
		 * @param enabled Indicates whether to enable or disable the buttons.
		 * @param buttonsToChange Buttons to enable/disable.
		 */
		public void setEnablement(boolean enabled, int[] buttonsToChange) {
			// -1 means that no default selection is given
			setEnablement(enabled, buttonsToChange, -1);
		}

		/**
		 * Deselect all buttons in the group.
		 */
		private void deselectAll() {
			// clear all selections
			for (Iterator iterator = buttons.iterator(); iterator.hasNext();)
				((Button) iterator.next()).setSelection(false);
		}
	}

}
