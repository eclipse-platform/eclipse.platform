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
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.ui.repo.RepositoryComparator;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class CVSProjectPropertiesPage extends CVSPropertiesPage {
	IProject project;
	ICVSRepositoryLocation oldLocation;
	ICVSRepositoryLocation newLocation = null;

	private static final int TABLE_HEIGHT_HINT = 150;
	
	// Widgets
	Text methodText;
	Text userText;
	Text hostText;
	Text pathText;
	Text moduleText;
	Text portText;
	Text tagText;
	private Button fetchButton;
	private Button watchEditButton;
	
	IUserInfo info;
	CVSTeamProvider provider;
	private boolean fetch;
	private boolean watchEdit;

	public static boolean isCompatible(ICVSRepositoryLocation location,
			ICVSRepositoryLocation oldLocation) {
		return CVSRepositoryLocationMatcher.isCompatible(location, oldLocation,
				false);
	}
	
	private class RepositorySelectionDialog extends Dialog {
		ICVSRepositoryLocation[] allLocations;
		ICVSRepositoryLocation[] compatibleLocations;
		ICVSRepositoryLocation selectedLocation;
		
		TableViewer viewer;
		Button okButton;
		boolean showCompatible = true;
		
		public RepositorySelectionDialog(Shell shell, ICVSRepositoryLocation oldLocation) {
			super(shell);
			initialize(oldLocation);
		}
		private void initialize(ICVSRepositoryLocation oldLocation) {
			allLocations = CVSUIPlugin.getPlugin().getRepositoryManager().getKnownRepositoryLocations();
			List locations = new ArrayList();
			for (ICVSRepositoryLocation location : allLocations) {
				if (isCompatible(location, oldLocation)) {
					locations.add(location);
				}
			}
			compatibleLocations = (ICVSRepositoryLocation[]) locations.toArray(new ICVSRepositoryLocation[locations.size()]);
		}
		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			// create OK and Cancel buttons by default
			okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
			okButton.setEnabled(false);
			createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		}
		@Override
		protected Control createDialogArea(Composite parent) {
			parent.getShell().setText(CVSUIMessages.CVSProjectPropertiesPage_Select_a_Repository_1); 
			Composite composite = (Composite) super.createDialogArea(parent);
		
			createLabel(composite, CVSUIMessages.CVSProjectPropertiesPage_Select_a_CVS_repository_location_to_share_the_project_with__2, 1); 
			Table table = new Table(composite, SWT.SINGLE | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
			GridData data = new GridData(GridData.FILL_HORIZONTAL);
			data.heightHint = TABLE_HEIGHT_HINT;
			table.setLayoutData(data);
			viewer = new TableViewer(table);
			viewer.setLabelProvider(new WorkbenchLabelProvider());
			viewer.setComparator(new RepositoryComparator());
			viewer.setContentProvider(new WorkbenchContentProvider() {
				@Override
				public Object[] getElements(Object inputElement) {
					if (showCompatible) {
						return compatibleLocations;
					} else {
						return allLocations;
					}
				}
			});
			viewer.addSelectionChangedListener(event -> {
				IStructuredSelection selection = event.getStructuredSelection();
				if (selection.isEmpty()) {
					selectedLocation = null;
					okButton.setEnabled(false);
				} else {
					selectedLocation = (ICVSRepositoryLocation)selection.getFirstElement();
					okButton.setEnabled(true);
				}
			});
			viewer.addDoubleClickListener(event -> okPressed());
			viewer.setInput(compatibleLocations);
			
			final Button compatibleButton = createCheckBox(composite, CVSUIMessages.CVSProjectPropertiesPage_31); 
			compatibleButton.setSelection(showCompatible);
			compatibleButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					showCompatible = compatibleButton.getSelection();
					viewer.refresh();
				}
			});

			Dialog.applyDialogFont(parent);
			
			return composite;
		}
		@Override
		protected void cancelPressed() {
			selectedLocation = null;
			super.cancelPressed();
		}
		public ICVSRepositoryLocation getLocation() {
			return selectedLocation;
		}
	}
	
	@Override
	protected Control createContents(Composite parent) {
		initialize();
		
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);
		
		Label label = createLabel(composite, CVSUIMessages.CVSProjectPropertiesPage_connectionType, 1); 
		methodText= createReadOnlyText(composite, ""); //$NON-NLS-1$
		
		label = createLabel(composite, CVSUIMessages.CVSProjectPropertiesPage_user, 1); 
		userText= createReadOnlyText(composite, ""); //$NON-NLS-1$
		
		label = createLabel(composite, CVSUIMessages.CVSRepositoryLocationPropertySource_host, 1); 
		hostText= createReadOnlyText(composite, ""); //$NON-NLS-1$
		
		label = createLabel(composite, CVSUIMessages.CVSPropertiesPage_port, 1); 
		portText= createReadOnlyText(composite, ""); //$NON-NLS-1$
		
		label = createLabel(composite, CVSUIMessages.CVSRepositoryLocationPropertySource_root, 1); 
		pathText= createReadOnlyText(composite, ""); //$NON-NLS-1$
		
		label = createLabel(composite, CVSUIMessages.CVSPropertiesPage_module, 1); 
		moduleText= createReadOnlyText(composite, ""); //$NON-NLS-1$
		
		label = createLabel(composite, CVSUIMessages.CVSPropertiesPage_tag, 1); 
		tagText= createReadOnlyText(composite, ""); //$NON-NLS-1$
		
		createLabel(composite, "", 1); //$NON-NLS-1$
		
		// Should absent directories be fetched on update
		fetchButton = createCheckBox(composite, CVSUIMessages.CVSProjectPropertiesPage_fetchAbsentDirectoriesOnUpdate); 
		fetchButton.addListener(SWT.Selection, event -> fetch = fetchButton.getSelection());
		
		// Should the project be configured for watch/edit
		watchEditButton = createCheckBox(composite, CVSUIMessages.CVSProjectPropertiesPage_configureForWatchEdit); 
		watchEditButton.addListener(SWT.Selection, event -> watchEdit = watchEditButton.getSelection());
		
		createLabel(composite, "", 1); //$NON-NLS-1$
		createLabel(composite, "", 1); //$NON-NLS-1$
		createLabel(composite, "", 1); //$NON-NLS-1$
		createLabel(composite, "", 1); //$NON-NLS-1$
		
		label = new Label(composite, SWT.WRAP);
		label.setText(CVSUIMessages.CVSProjectPropertiesPage_You_can_change_the_sharing_of_this_project_to_another_repository_location__However__this_is_only_possible_if_the_new_location_is___compatible____on_the_same_host_with_the_same_repository_path___1); 
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = 200;
		data.horizontalSpan = 2;
		label.setLayoutData(data);
		
		Button changeButton = new Button(composite, SWT.PUSH);
		changeButton.setText(CVSUIMessages.CVSProjectPropertiesPage_Change_Sharing_5); 
		changeButton.addListener(SWT.Selection, e -> {
			RepositorySelectionDialog dialog = new RepositorySelectionDialog(getShell(), oldLocation);
			dialog.open();
			ICVSRepositoryLocation location = dialog.getLocation();
			if (location == null) return;
			newLocation = location;
			initializeValues(newLocation);
		});
		
		initializeValues(oldLocation);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.PROJECT_PROPERTY_PAGE);
		Dialog.applyDialogFont(parent);
		return composite;
	}
	/**
	 * Utility method that creates a label instance
	 * and sets the default layout data.
	 *
	 * @param parent  the parent for the new label
	 * @param text  the text for the new label
	 * @return the new label
	 */
	@Override
	protected Label createLabel(Composite parent, String text, int span) {
		Label label = new Label(parent, SWT.LEFT);
		label.setText(text);
		GridData data = new GridData();
		data.horizontalSpan = span;
		data.horizontalAlignment = GridData.FILL;
		label.setLayoutData(data);
		return label;
	}

	/**
	 * Creates a new checkbox instance and sets the default layout data.
	 *
	 * @param group  the composite in which to create the checkbox
	 * @param label  the string to set into the checkbox
	 * @return the new checkbox
	 */ 
	protected Button createCheckBox(Composite group, String label) {
		Button button = new Button(group, SWT.CHECK | SWT.LEFT);
		button.setText(label);
		GridData data = new GridData();
		data.horizontalSpan = 2;
		button.setLayoutData(data);
		return button;
	}
	/**
	 * Initializes the page
	 */
	private void initialize() {
		// Get the project that is the source of this property page
		project = null;
		IAdaptable element = getElement();
		if (element instanceof IProject) {
			project = (IProject)element;
		} else {
			Object adapter = element.getAdapter(IProject.class);
			if (adapter instanceof IProject) {
				project = (IProject)adapter;
			}
		}
		// Do some pre-checks to ensure we're in a good state
		provider = (CVSTeamProvider)RepositoryProvider.getProvider(project, CVSProviderPlugin.getTypeId());
		if (provider == null) return;
		CVSWorkspaceRoot cvsRoot = provider.getCVSWorkspaceRoot();
		try {
			oldLocation = cvsRoot.getRemoteLocation();
			fetch = provider.getFetchAbsentDirectories();
			watchEdit = provider.isWatchEditEnabled();
		} catch (TeamException e) {
			handle(e);
		}
	}
	/**
	 * Set the initial values of the widgets
	 */
	private void initializeValues(ICVSRepositoryLocation location) {
		if (provider == null) return;
		CVSWorkspaceRoot cvsRoot = provider.getCVSWorkspaceRoot();
		ICVSFolder folder = cvsRoot.getLocalRoot();
		
		try {
			if (!folder.isCVSFolder()) return;
			methodText.setText(location.getMethod().getName());
			info = location.getUserInfo(true);
			userText.setText(info.getUsername());
			hostText.setText(location.getHost());
			int port = location.getPort();
			if (port == ICVSRepositoryLocation.USE_DEFAULT_PORT) {
				portText.setText(CVSUIMessages.CVSPropertiesPage_defaultPort); 
			} else {
				portText.setText("" + port); //$NON-NLS-1$
			}
			pathText.setText(location.getRootDirectory());
			FolderSyncInfo syncInfo = folder.getFolderSyncInfo();
			if (syncInfo == null) return;
			String label = syncInfo.getRepository();
			if (label.equals(FolderSyncInfo.VIRTUAL_DIRECTORY)) {
				label = NLS.bind(CVSUIMessages.CVSPropertiesPage_virtualModule, new String[] { label }); 
			}
			moduleText.setText(label);
			fetchButton.setSelection(fetch);
			watchEditButton.setSelection(watchEdit);
		} catch (TeamException e) {
			handle(e);
		}
		
		initializeTag();
	}
	
	private void initializeTag() {
		provider = (CVSTeamProvider)RepositoryProvider.getProvider(project, CVSProviderPlugin.getTypeId());
		if (provider == null) return;
		try {
			ICVSFolder local = CVSWorkspaceRoot.getCVSFolderFor(project);
			CVSTag tag = local.getFolderSyncInfo().getTag();
			
			tagText.setText(getTagLabel(tag));

		} catch (TeamException e) {
			handle(e);
		}
	}
	@Override
	public boolean performOk() {
		final boolean[] changeReadOnly = { false };
		try {
			if (fetch != provider.getFetchAbsentDirectories())
				provider.setFetchAbsentDirectories(fetch);
			if (watchEdit != provider.isWatchEditEnabled()) {
				provider.setWatchEditEnabled(watchEdit);
				changeReadOnly[0] = true;
			}
		} catch (CVSException e) {
			handle(e);
		}
		if (newLocation == null && !changeReadOnly[0]) {
			return true;
		}
		try {
			if (newLocation != null && !isCompatible(newLocation, oldLocation)) {
				if (!MessageDialog.openQuestion(getShell(), CVSUIMessages.CVSProjectPropertiesPage_32, CVSUIMessages.CVSProjectPropertiesPage_33)) { // 
					return false;
				}
			}
			new ProgressMonitorDialog(getShell()).run(true, true, monitor -> {
				try {
					monitor.beginTask(CVSUIMessages.CVSProjectPropertiesPage_progressTaskName,  
					((newLocation == null)?0:100) + (changeReadOnly[0]?100:0));
					if (newLocation != null)
						provider.setRemoteRoot(newLocation, Policy.subMonitorFor(monitor, 100));
					if (changeReadOnly[0])
						setReadOnly(watchEdit, Policy.infiniteSubMonitorFor(monitor, 100));
				} catch (TeamException e) {
					throw new InvocationTargetException(e);
				}
			});
			newLocation = null;
			if (changeReadOnly[0]) {
				CVSUIPlugin.broadcastPropertyChange(new PropertyChangeEvent(this, CVSUIPlugin.P_DECORATORS_CHANGED, null, null));
			}
		} catch (InvocationTargetException e) {
			handle(e);
		} catch (InterruptedException e) {
			return false;
		}
			
		return true;
	}
	/**
	 * @param watchEdit
	 */
	protected void setReadOnly(final boolean watchEdit, final IProgressMonitor monitor) throws CVSException {
		monitor.beginTask(null, 512);
		String taskName = watchEdit?
			CVSUIMessages.CVSProjectPropertiesPage_setReadOnly: 
			CVSUIMessages.CVSProjectPropertiesPage_clearReadOnly; 
		monitor.subTask(taskName);
		ICVSFolder root = CVSWorkspaceRoot.getCVSFolderFor(project);
		root.accept(new ICVSResourceVisitor() {
			@Override
			public void visitFile(ICVSFile file) throws CVSException {
				// only change managed, unmodified files
				if (file.isManaged() && !file.isModified(null))
					file.setReadOnly(watchEdit);
				monitor.worked(1);
			}

			@Override
			public void visitFolder(ICVSFolder folder) throws CVSException {
				folder.acceptChildren(this);
			}
		});
		monitor.done();
	}
	/**
	 * Shows the given errors to the user.
	 */
	protected void handle(Throwable t) {
		CVSUIPlugin.openError(getShell(), null, null, t);
	}
}

