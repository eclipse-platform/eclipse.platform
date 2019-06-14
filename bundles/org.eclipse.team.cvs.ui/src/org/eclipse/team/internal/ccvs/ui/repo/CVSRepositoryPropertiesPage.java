/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.repo;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.window.Window;
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
import org.eclipse.team.internal.ccvs.core.connection.CVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.util.KnownRepositories;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ccvs.ui.wizards.ConfigurationWizardMainPage;
import org.eclipse.team.internal.ui.SWTUtils;
import org.eclipse.team.internal.ui.dialogs.DetailsDialogWithProjects;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.PropertyPage;

public class CVSRepositoryPropertiesPage extends PropertyPage {
	private static final String FAKE_PASSWORD = "*********"; //$NON-NLS-1$
	ICVSRepositoryLocation location;
	
	// Widgets
	Text userText;
	Text passwordText;
	Combo methodType;
	Text hostText;
	Text pathText;
	// Port
	private Text portText;
	private Button useDefaultPort;
	private Button useCustomPort;
	
	// Caching password
	private Button allowCachingButton;
	private boolean allowCaching = false;
	
	boolean passwordChanged;
	boolean connectionInfoChanged;

	IUserInfo info;

	// Label
	private Button useLocationAsLabel;
	private Button useCustomLabel;
	private Text labelText;
			
	@Override
	protected Control createContents(Composite parent) {
		initialize();
		
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		composite.setLayout(layout);

		// Repository Label
		// create a composite to ensure the radio buttons come in the correct order
		Composite labelGroup = new Composite(composite, SWT.NONE);
		GridData data = new GridData();
		data.horizontalSpan = 3;
		labelGroup.setLayoutData(data);
		layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		labelGroup.setLayout(layout);
		Listener labelListener = event -> updateWidgetEnablements();
		useLocationAsLabel = createRadioButton(labelGroup, CVSUIMessages.CVSRepositoryPropertiesPage_useLocationAsLabel, 3); 
		useCustomLabel = createRadioButton(labelGroup, CVSUIMessages.CVSRepositoryPropertiesPage_useCustomLabel, 1); 
		useCustomLabel.addListener(SWT.Selection, labelListener);
		labelText = createTextField(labelGroup);
		labelText.addListener(SWT.Modify, labelListener);
		
		// Add some extra space
		createLabel(composite, "", 3); //$NON-NLS-1$
		
		createLabel(composite, CVSUIMessages.CVSPropertiesPage_connectionType, 1); 
		methodType = createCombo(composite);
		
		createLabel(composite, CVSUIMessages.CVSPropertiesPage_user, 1); 
		userText = createTextField(composite);
		
		createLabel(composite, CVSUIMessages.CVSPropertiesPage_password, 1); 
		passwordText = createPasswordField(composite);
			
		createLabel(composite, CVSUIMessages.CVSPropertiesPage_host, 1); 
		hostText = createTextField(composite);
		
		createLabel(composite, CVSUIMessages.CVSPropertiesPage_path, 1); 
		pathText = createTextField(composite);
		
		// Port number
		// create a composite to ensure the radio buttons come in the correct order
		Composite portGroup = new Composite(composite, SWT.NONE);
		data = new GridData();
		data.horizontalSpan = 3;
		portGroup.setLayoutData(data);
		layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		portGroup.setLayout(layout);
		useDefaultPort = createRadioButton(portGroup, CVSUIMessages.ConfigurationWizardMainPage_useDefaultPort, 3); 
		useCustomPort = createRadioButton(portGroup, CVSUIMessages.ConfigurationWizardMainPage_usePort, 1); 
		portText = createTextField(portGroup);

		// Add some extra space
		createLabel(composite, "", 3); //$NON-NLS-1$

		allowCachingButton = new Button(composite, SWT.CHECK);
		allowCachingButton.setText(CVSUIMessages.UserValidationDialog_6); 
		data = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		data.horizontalSpan = 3;
		allowCachingButton.setLayoutData(data);
		allowCachingButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				allowCaching = allowCachingButton.getSelection();
			}
		});
		
		Link link = SWTUtils.createPreferenceLink(getShell(), composite, CVSUIMessages.ConfigurationWizardMainPage_9, CVSUIMessages.ConfigurationWizardMainPage_10);
		link.setLayoutData(data);
		
		// Add some extra space
		createLabel(composite, "", 3); //$NON-NLS-1$
		
		initializeValues();
		updateWidgetEnablements();
		Listener connectionInfoChangedListener = event -> {
			connectionInfoChanged = true;
			updateWidgetEnablements();
		};
		passwordText.addListener(SWT.Modify, event -> passwordChanged = !passwordText.getText().equals(FAKE_PASSWORD));
		userText.addListener(SWT.Modify, connectionInfoChangedListener);
		methodType.addListener(SWT.Modify, connectionInfoChangedListener);
		hostText.addListener(SWT.Modify, connectionInfoChangedListener);
		portText.addListener(SWT.Modify, connectionInfoChangedListener);
		useCustomPort.addListener(SWT.Selection, connectionInfoChangedListener);
		pathText.addListener(SWT.Modify, connectionInfoChangedListener);
		
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.REPOSITORY_LOCATION_PROPERTY_PAGE);
		Dialog.applyDialogFont(parent);
		return composite;
	}
	
	/**
	 * Utility method that creates a combo box
	 *
	 * @param parent  the parent for the new label
	 * @return the new widget
	 */
	protected Combo createCombo(Composite parent) {
		Combo combo = new Combo(parent, SWT.READ_ONLY);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		data.horizontalSpan = 2;
		combo.setLayoutData(data);
		return combo;
	}
	/**
	 * Utility method that creates a label instance
	 * and sets the default layout data.
	 *
	 * @param parent  the parent for the new label
	 * @param text  the text for the new label
	 * @return the new label
	 */
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
	 * Create a text field specific for this application
	 *
	 * @param parent  the parent of the new text field
	 * @return the new text field
	 */
	protected Text createTextField(Composite parent) {
		Text text = new Text(parent, SWT.SINGLE | SWT.BORDER);
		return layoutTextField(text);
	}
	/**
	 * Create a password field specific for this application
	 *
	 * @param parent  the parent of the new text field
	 * @return the new text field
	 */
	protected Text createPasswordField(Composite parent) {
		Text text = new Text(parent, SWT.SINGLE | SWT.BORDER | SWT.PASSWORD);
		return layoutTextField(text);
	}
	/**
	 * Layout a text or password field specific for this application
	 *
	 * @param parent  the parent of the new text field
	 * @return the new text field
	 */
	protected Text layoutTextField(Text text) {
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.verticalAlignment = GridData.CENTER;
		data.grabExcessVerticalSpace = false;
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		data.horizontalSpan = 2;
		text.setLayoutData(data);
		return text;
	}

	/**
	 * Utility method to create a radio button
	 * 
	 * @param parent  the parent of the radio button
	 * @param label  the label of the radio button
	 * @param span  the number of columns to span
	 * @return the created radio button
	 */
	protected Button createRadioButton(Composite parent, String label, int span) {
		Button button = new Button(parent, SWT.RADIO);
		button.setText(label);
		GridData data = new GridData();
		data.horizontalSpan = span;
		button.setLayoutData(data);
		return button;
	}
	/**
	 * Initializes the page
	 */
	private void initialize() {
		location = null;
		IAdaptable element = getElement();
		if (element instanceof ICVSRepositoryLocation) {
			location = (ICVSRepositoryLocation)element;
		} else {
			Object adapter = element.getAdapter(ICVSRepositoryLocation.class);
			if (adapter instanceof ICVSRepositoryLocation) {
				location = (ICVSRepositoryLocation)adapter;
			}
		}
	}
	/**
	 * Set the initial values of the widgets
	 */
	private void initializeValues() {
		passwordChanged = false;
		connectionInfoChanged = false;
		
		IConnectionMethod[] methods = CVSRepositoryLocation.getPluggedInConnectionMethods();
		for (IConnectionMethod method : methods) {
			methodType.add(method.getName());
		}
		String method = location.getMethod().getName();
		methodType.select(methodType.indexOf(method));
		info = location.getUserInfo(true);
		userText.setText(info.getUsername());
		passwordText.setText(FAKE_PASSWORD);
		hostText.setText(location.getHost());
		int port = location.getPort();
		if (port == ICVSRepositoryLocation.USE_DEFAULT_PORT) {
			useDefaultPort.setSelection(true);
			useCustomPort.setSelection(false);
			portText.setEnabled(false);
		} else {
			useDefaultPort.setSelection(false);
			useCustomPort.setSelection(true);
			portText.setText("" + port); //$NON-NLS-1$
		}
		pathText.setText(location.getRootDirectory());
		allowCachingButton.setSelection(location.getUserInfoCached());
		allowCaching = allowCachingButton.getSelection();
		
		// get the repository label
		String label = null;
		RepositoryRoot root = CVSUIPlugin.getPlugin().getRepositoryManager().getRepositoryRootFor(location);
		label = root.getName();
		useLocationAsLabel.setSelection(label == null);
		useCustomLabel.setSelection(!useLocationAsLabel.getSelection());
		if (label == null) {
			label = location.getLocation(true);
		}
		labelText.setText(label);
	}
	
	private boolean performConnectionInfoChanges() {
		// Set the caching mode of the location
		if (!connectionInfoChanged) {
			location.setAllowCaching(allowCaching);
			if (!passwordChanged) {
				((CVSRepositoryLocation)location).updateCache();
			}
		}
		// Don't do anything else if there wasn't a password or connection change
		if (!passwordChanged && !connectionInfoChanged) return true;
		
		try {
			// Check if the password was the only thing to change.
			if (passwordChanged && !connectionInfoChanged) {
				CVSRepositoryLocation oldLocation = (CVSRepositoryLocation)location;
				oldLocation.setPassword(getNewPassword());
				if (allowCaching) {
					oldLocation.updateCache();
				}
				passwordChanged = false;
				return true;
			}
		
			// Otherwise change the connection info and the password
			// This operation is done inside a workspace operation in case the sharing
			// info for existing projects is changed
			if (!(location.getHost().equals(hostText.getText()) && location.getRootDirectory().equals(pathText.getText()))) {
				// The host or path has changed
				if (!MessageDialog.openConfirm(getShell(), 
						CVSUIMessages.CVSRepositoryPropertiesPage_0,  
						CVSUIMessages.CVSRepositoryPropertiesPage_1)) { 
					return false;
				}
			}
			final boolean[] result = new boolean[] { false };
			final ProgressMonitorDialog progressMonitorDialog = new ProgressMonitorDialog(getShell());
			progressMonitorDialog.run(false, false, new WorkspaceModifyOperation(null) {
				@Override
				public void execute(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						// Create a new repository location with the new information
						CVSRepositoryLocation newLocation = CVSRepositoryLocation.fromProperties(createProperties());
						newLocation.setEncoding(location.getEncoding());
						location.setAllowCaching(allowCaching);
						try {
							// For each project shared with the old location, set connection info to the new one
							List projects = new ArrayList();
							IProject[] allProjects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
							for (IProject project : allProjects) {
								RepositoryProvider teamProvider = RepositoryProvider.getProvider(project, CVSProviderPlugin.getTypeId());
								if (teamProvider != null) {
									CVSTeamProvider cvsProvider = (CVSTeamProvider)teamProvider;
									if (cvsProvider.getCVSWorkspaceRoot().getRemoteLocation().equals(location)) {
										projects.add(project);
									}
								}
							}
							if (projects.size() > 0) {
								// To do: warn the user
								DetailsDialogWithProjects dialog = new DetailsDialogWithProjects(
									progressMonitorDialog.getShell(), 
									CVSUIMessages.CVSRepositoryPropertiesPage_Confirm_Project_Sharing_Changes_1, 
									CVSUIMessages.CVSRepositoryPropertiesPage_There_are_projects_in_the_workspace_shared_with_this_repository_2, 
									NLS.bind(CVSUIMessages.CVSRepositoryPropertiesPage_sharedProject, new String[] { location.toString() }), 
									(IProject[]) projects.toArray(new IProject[projects.size()]),
									true,
									Dialog.DLG_IMG_WARNING);
								int r = dialog.open();
								if (r != Window.OK) {
									result[0] = false;
									return;
								}
								monitor.beginTask(null, 1000 * projects.size());
								try {
									Iterator it = projects.iterator();
									while (it.hasNext()) {
										IProject project = (IProject)it.next();
										RepositoryProvider teamProvider = RepositoryProvider.getProvider(project, CVSProviderPlugin.getTypeId());
										CVSTeamProvider cvsProvider = (CVSTeamProvider)teamProvider;
										cvsProvider.setRemoteRoot(newLocation, Policy.subMonitorFor(monitor, 1000));
									}
								} finally {
									monitor.done();
								}
							}
							
							// Dispose the old repository location
							CVSUIPlugin.getPlugin().getRepositoryManager().replaceRepositoryLocation(location, newLocation);
							
						} finally {
							// Even if we failed, ensure that the new location appears in the repo view.
							newLocation = (CVSRepositoryLocation)KnownRepositories.getInstance().addRepository(newLocation, !KnownRepositories.getInstance().isKnownRepository(newLocation.getLocation()));
						}
						
						// Set the location of the page to the new location in case Apply was chosen
						location = newLocation;
						connectionInfoChanged = false;
						passwordChanged = false;
					} catch (TeamException e) {
						throw new InvocationTargetException(e);
					}
					result[0] = true;
				}
			});
			return result[0];
		} catch (InvocationTargetException e) {
			handle(e);
		} catch (InterruptedException e) {
		}
		return false; /* we only get here if an exception occurred */
	}
	
	private void performNonConnectionInfoChanges() {
		recordNewLabel((CVSRepositoryLocation)location);
	}

	@Override
	public boolean performOk() {
		if (performConnectionInfoChanges()) {
			performNonConnectionInfoChanges();
			return true;
		}
		return false;
	}
	
	@Override
	protected void performDefaults() {
		super.performDefaults();
		initializeValues();
	}
	
	/**
	 * Shows the given errors to the user.
	 */
	protected void handle(Throwable e) {
		CVSUIPlugin.openError(getShell(), null, null, e);
	}
	
	/**
	 * Updates widget enablements and sets error message if appropriate.
	 */
	protected void updateWidgetEnablements() {
		if (useLocationAsLabel.getSelection()) {
			labelText.setEnabled(false);
		} else {
			labelText.setEnabled(true);
		}
		if (useDefaultPort.getSelection()) {
			portText.setEnabled(false);
		} else {
			portText.setEnabled(true);
		}
		validateFields();
	}
	
	private void validateFields() {
		if (labelText.isEnabled()) {
			if (labelText.getText().length() == 0) {
				setValid(false);
				return;
			}
		}
		String user = userText.getText();
		IStatus status = ConfigurationWizardMainPage.validateUserName(user);
		if (!isStatusOK(status)) {
			return;
		}

		String host = hostText.getText();
		status = ConfigurationWizardMainPage.validateHost(host);
		if (!isStatusOK(status)) {
			return;
		}

		if (portText.isEnabled()) {
			String port = portText.getText();
			status = ConfigurationWizardMainPage.validatePort(port);
			if (!isStatusOK(status)) {
				return;
			}
		}

		String pathString = pathText.getText();
		status = ConfigurationWizardMainPage.validatePath(pathString);
		if (!isStatusOK(status)) {
			return;
		}
		
		try {
			CVSRepositoryLocation l = CVSRepositoryLocation.fromProperties(createProperties());
			if (!l.equals(location) && KnownRepositories.getInstance().isKnownRepository(l.getLocation())) {
				setErrorMessage(CVSUIMessages.ConfigurationWizardMainPage_0); 
				setValid(false);
				return;
			}
		} catch (CVSException e) {
			CVSUIPlugin.log(e);
			// Let it pass. Creation should fail
		}
		
		setErrorMessage(null);
		setValid(true);
	}
	
	private boolean isStatusOK(IStatus status) {
		if (!status.isOK()) {
			setErrorMessage(status.getMessage());
			setValid(false);
			return false;
		}
		return true;
	}
	
	private void recordNewLabel(CVSRepositoryLocation location) {
		String newLabel = getNewLabel(location);
		if (newLabel == null) {
			String oldLabel = getOldLabel(location);
			if (oldLabel == null || oldLabel.equals(location.getLocation())) {
				return;
			}
		} else if (newLabel.equals(getOldLabel(location))) {
			return;
		}
		CVSUIPlugin.getPlugin().getRepositoryManager().setLabel(location, newLabel);
		CVSUIPlugin.broadcastPropertyChange(new PropertyChangeEvent(this, 
				CVSUIPlugin.P_DECORATORS_CHANGED, null, null));

	}
	private String getOldLabel(CVSRepositoryLocation location) {
		return CVSUIPlugin.getPlugin().getRepositoryManager().getRepositoryRootFor(location).getName();
	}
	private String getNewLabel(CVSRepositoryLocation location) {
		String label = null;
		if (useCustomLabel.getSelection()) {
			label = labelText.getText();
			if (label.equals(location.getLocation())) {
				label = null;
			}
		}
		return label;
	}
	/* internal use only */ String getNewPassword() {
		return passwordText.getText();
	}
	
	private Properties createProperties() {
		Properties result = new Properties();
		result.setProperty("connection", methodType.getText()); //$NON-NLS-1$
		result.setProperty("user", userText.getText()); //$NON-NLS-1$
		if (passwordChanged) {
			result.setProperty("password", passwordText.getText()); //$NON-NLS-1$
		}
		result.setProperty("host", hostText.getText()); //$NON-NLS-1$
		if (useCustomPort.getSelection()) {
			result.setProperty("port", portText.getText()); //$NON-NLS-1$
		}
		result.setProperty("root", pathText.getText()); //$NON-NLS-1$
		return result;
	}
}

