/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
 *     Brock Janiczak <brockj@tpg.com.au> - Bug 185708 Provide link to open SSH/SSH2/proxy preferences from Connection wizard
 *     Brock Janiczak <brockj@tpg.com.au> - Bug 107025 [Wizards] expose the 'paste cvs connection' easter egg
 *     Remy Chi Jian Suen <remy.suen@gmail.com> - Bug 274284 - 'Add CVS Repository' dialog is clipped at the bottom
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.wizards;

import java.util.*;
import java.util.List;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.fieldassist.*;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.connection.CVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.util.KnownRepositories;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ui.SWTUtils;
import org.eclipse.ui.PlatformUI;

/**
 * Wizard page for entering information about a CVS repository location.
 */
public class ConfigurationWizardMainPage extends CVSWizardPage {
	private static final String ANONYMOUS_USER = "anonymous"; //$NON-NLS-1$

	private boolean showValidate;
	private boolean validate;

	// Widgets

	// Connection Method
	private Combo connectionMethodCombo;
	// User
	private Combo userCombo;
	// Password
	private Text passwordText;
	// Port
	private Text portText;
	private Button useDefaultPort;
	private Button useCustomPort;
	// Host
	private Combo hostCombo;
	// Repository Path
	private Combo repositoryPathCombo;
	// Validation
	private Button validateButton;
	// Caching password
	private Button allowCachingButton;
	private boolean allowCaching = false;

	private static final int COMBO_HISTORY_LENGTH = 5;

	private Properties properties = null;

	// The previously created repository.
	// It is recorded when asked for and
	// nulled when the page is changed.
	private ICVSRepositoryLocation location;

	// The previously created repository.
	// It is recorded when fields are changed
	private ICVSRepositoryLocation oldLocation;

	// Dialog store id constants
	private static final String STORE_USERNAME_ID =
		"ConfigurationWizardMainPage.STORE_USERNAME_ID";//$NON-NLS-1$
	private static final String STORE_HOSTNAME_ID =
		"ConfigurationWizardMainPage.STORE_HOSTNAME_ID";//$NON-NLS-1$
	private static final String STORE_PATH_ID =
		"ConfigurationWizardMainPage.STORE_PATH_ID";//$NON-NLS-1$
	private static final String STORE_DONT_VALIDATE_ID =
		"ConfigurationWizardMainPage.STORE_DONT_VALIDATE_ID";//$NON-NLS-1$

	// In case the page was launched from a different wizard
	private IDialogSettings settings;

	/**
	 * ConfigurationWizardMainPage constructor.
	 * 
	 * @param pageName  the name of the page
	 * @param title  the title of the page
	 * @param titleImage  the image for the page
	 */
	public ConfigurationWizardMainPage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
	}
	/**
	 * Adds an entry to a history, while taking care of duplicate history items
	 * and excessively long histories.  The assumption is made that all histories
	 * should be of length <code>ConfigurationWizardMainPage.COMBO_HISTORY_LENGTH</code>.
	 *
	 * @param history the current history
	 * @param newEntry the entry to add to the history
	 * @return the history with the new entry appended
	 */
	private String[] addToHistory(String[] history, String newEntry) {
		ArrayList<String> l = new ArrayList<>(Arrays.asList(history));
		addToHistory(l, newEntry);
		String[] r = new String[l.size()];
		l.toArray(r);
		return r;
	}
	@Override
	protected IDialogSettings getDialogSettings() {
		return settings;
	}
	protected void setDialogSettings(IDialogSettings settings) {
		this.settings = settings;
	}
	/**
	 * Adds an entry to a history, while taking care of duplicate history items
	 * and excessively long histories.  The assumption is made that all histories
	 * should be of length <code>ConfigurationWizardMainPage.COMBO_HISTORY_LENGTH</code>.
	 *
	 * @param history the current history
	 * @param newEntry the entry to add to the history
	 */
	private void addToHistory(List<String> history, String newEntry) {
		history.remove(newEntry);
		history.add(0,newEntry);

		// since only one new item was added, we can be over the limit
		// by at most one item
		if (history.size() > COMBO_HISTORY_LENGTH)
			history.remove(COMBO_HISTORY_LENGTH);
	}
	/**
	 * Creates the UI part of the page.
	 * 
	 * @param parent  the parent of the created widgets
	 */
	@Override
	public void createControl(Composite parent) {
		Composite composite = createComposite(parent, 2, false);
		// set F1 help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.SHARING_NEW_REPOSITORY_PAGE);

		Listener listener = event -> {
			if (location != null) {
				oldLocation = location;
				location = null;
			}
			if (event.widget == hostCombo) {
				String hostText = hostCombo.getText();
				if (hostText.length() > 0 && hostText.charAt(0) == ':') {
					try {
						CVSRepositoryLocation newLocation = CVSRepositoryLocation.fromString(hostText);
						connectionMethodCombo.setText(newLocation.getMethod().getName());
						repositoryPathCombo.setText(newLocation.getRootDirectory());
						int port = newLocation.getPort();
						if (port == ICVSRepositoryLocation.USE_DEFAULT_PORT) {
							useDefaultPort.setSelection(true);
							useCustomPort.setSelection(false);
						} else {
							useCustomPort.setSelection(true);
							useDefaultPort.setSelection(false);
							portText.setText(String.valueOf(port));
						}

						userCombo.setText(newLocation.getUsername());
						// passwordText.setText(newLocation.xxx);
						hostCombo.setText(newLocation.getHost());
					} catch (CVSException e) {
						CVSUIPlugin.log(e);
					}
				}
			}
			updateWidgetEnablements();
		};

		Group g = createGroup(composite, CVSUIMessages.ConfigurationWizardMainPage_Location_1);

		// Host name
		createLabel(g, CVSUIMessages.ConfigurationWizardMainPage_host);
		hostCombo = createEditableCombo(g);
		ControlDecoration decoration = new ControlDecoration(hostCombo, SWT.TOP | SWT.LEFT);
		FieldDecoration infoDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION);
		decoration.setImage(infoDecoration.getImage());
		decoration.setDescriptionText(CVSUIMessages.ConfigurationWizardMainPage_8);
		decoration.setShowOnlyOnFocus(true);

		((GridLayout)g.getLayout()).horizontalSpacing = decoration.getMarginWidth() + infoDecoration.getImage().getBounds().width;

		hostCombo.addListener(SWT.Selection, listener);
		hostCombo.addListener(SWT.Modify, listener);

		// Repository Path
		createLabel(g, CVSUIMessages.ConfigurationWizardMainPage_repositoryPath);
		repositoryPathCombo = createEditableCombo(g);
		repositoryPathCombo.addListener(SWT.Selection, listener);
		repositoryPathCombo.addListener(SWT.Modify, listener);

		g = createGroup(composite, CVSUIMessages.ConfigurationWizardMainPage_Authentication_2);

		// User name
		createLabel(g, CVSUIMessages.ConfigurationWizardMainPage_userName);
		userCombo = createEditableCombo(g);
		userCombo.addListener(SWT.Selection, listener);
		userCombo.addListener(SWT.Modify, listener);

		// Password
		createLabel(g, CVSUIMessages.ConfigurationWizardMainPage_password);
		passwordText = createPasswordField(g);
		passwordText.addListener(SWT.Modify, listener);

		g = createGroup(composite, CVSUIMessages.ConfigurationWizardMainPage_Connection_3);

		// Connection type
		createLabel(g, CVSUIMessages.ConfigurationWizardMainPage_connection);
		connectionMethodCombo = createCombo(g);
		connectionMethodCombo.addListener(SWT.Selection, listener);

		// Port number
		// create a composite to ensure the radio buttons come in the correct order
		Composite portGroup = new Composite(g, SWT.NONE);
		GridData data = new GridData();
		data.horizontalSpan = 2;
		portGroup.setLayoutData(data);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		portGroup.setLayout(layout);
		useDefaultPort = createRadioButton(portGroup, CVSUIMessages.ConfigurationWizardMainPage_useDefaultPort, 2);
		useCustomPort = createRadioButton(portGroup, CVSUIMessages.ConfigurationWizardMainPage_usePort, 1);
		useCustomPort.addListener(SWT.Selection, listener);
		portText = createTextField(portGroup);
		portText.addListener(SWT.Modify, listener);

		// create a composite to ensure the validate button is in its own tab group
		if (showValidate) {
			Composite validateButtonTabGroup = new Composite(composite, SWT.NONE);
			data = new GridData();
			data.horizontalSpan = 2;
			validateButtonTabGroup.setLayoutData(data);
			validateButtonTabGroup.setLayout(new FillLayout());

			validateButton = new Button(validateButtonTabGroup, SWT.CHECK);
			validateButton.setText(CVSUIMessages.ConfigurationWizardAutoconnectPage_validate);
			validateButton.addListener(SWT.Selection, e -> validate = validateButton.getSelection());
		}

		allowCachingButton = new Button(composite, SWT.CHECK);
		allowCachingButton.setText(CVSUIMessages.UserValidationDialog_6);
		data = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		data.horizontalSpan = 2;
		allowCachingButton.setLayoutData(data);
		allowCachingButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				allowCaching = allowCachingButton.getSelection();
			}
		});

		Link link = SWTUtils.createPreferenceLink(getShell(), composite, CVSUIMessages.ConfigurationWizardMainPage_9, CVSUIMessages.ConfigurationWizardMainPage_10);
		data = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		data.horizontalSpan = 2;
		link.setLayoutData(data);

		SWTUtils.createPreferenceLink(getShell(), composite,
				"org.eclipse.team.cvs.ui.ExtMethodPreferencePage", //$NON-NLS-1$
				new String[] { "org.eclipse.team.cvs.ui.cvs", //$NON-NLS-1$
			"org.eclipse.team.cvs.ui.ExtMethodPreferencePage", //$NON-NLS-1$
			"org.eclipse.jsch.ui.SSHPreferences", //$NON-NLS-1$
		"org.eclipse.ui.net.NetPreferences" }, //$NON-NLS-1$
		CVSUIMessages.ConfigurationWizardMainPage_7);

		initializeValues();
		updateWidgetEnablements();
		hostCombo.setFocus();

		setControl(composite);
		Dialog.applyDialogFont(parent);
	}

	/**
	 * Utility method to create an editable combo box
	 * 
	 * @param parent  the parent of the combo box
	 * @return the created combo
	 */
	protected Combo createEditableCombo(Composite parent) {
		Combo combo = new Combo(parent, SWT.NULL);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		combo.setLayoutData(data);
		return combo;
	}

	protected Group createGroup(Composite parent, String text) {
		Group group = new Group(parent, SWT.NULL);
		group.setText(text);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		//data.widthHint = GROUP_WIDTH;

		group.setLayoutData(data);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		group.setLayout(layout);
		return group;
	}

	/*
	 * Create a Properties node that contains everything needed to create a repository location
	 */
	private Properties createProperties() {
		Properties result = new Properties();
		result.setProperty("connection", connectionMethodCombo.getText()); //$NON-NLS-1$
		result.setProperty("user", userCombo.getText()); //$NON-NLS-1$
		result.setProperty("password", passwordText.getText()); //$NON-NLS-1$
		result.setProperty("host", hostCombo.getText()); //$NON-NLS-1$
		if (useCustomPort.getSelection()) {
			result.setProperty("port", portText.getText()); //$NON-NLS-1$
		}
		result.setProperty("root", repositoryPathCombo.getText()); //$NON-NLS-1$
		return result;
	}

	/**
	 * Create a new location with the information entered on the page.
	 * The location will exists and can be set for connecting but is not
	 * registered for persistence. This method must be called from the UI
	 * thread.
	 * @return a location or <code>null</code>
	 * @throws CVSException
	 */
	public ICVSRepositoryLocation getLocation() throws CVSException {
		if (location == null) {
			if (!isPageComplete()) return null;
			location = CVSRepositoryLocation.fromProperties(createProperties());
			if (location.equals(oldLocation)) {
				location = oldLocation;
			}
			location.setAllowCaching(allowCaching);
			oldLocation = null;
			saveWidgetValues();
		}
		return location;
	}

	/**
	 * Initializes states of the controls.
	 */
	private void initializeValues() {
		// Set remembered values
		IDialogSettings settings = getDialogSettings();
		if (settings != null) {
			String[] hostNames = settings.getArray(STORE_HOSTNAME_ID);
			if (hostNames != null) {
				for (String hostName : hostNames) {
					hostCombo.add(hostName);
				}
			}
			String[] paths = settings.getArray(STORE_PATH_ID);
			if (paths != null) {
				for (String path : paths) {
					repositoryPathCombo.add(path);
				}
			}
			String[] userNames = settings.getArray(STORE_USERNAME_ID);
			if (userNames != null) {
				for (String userName : userNames) {
					userCombo.add(userName);
				}
			}
			userCombo.add(ANONYMOUS_USER);
			if (showValidate) {
				validate = !settings.getBoolean(STORE_DONT_VALIDATE_ID);
				validateButton.setSelection(validate);
			}
		}

		// Initialize other values and widget states
		IConnectionMethod[] methods = CVSRepositoryLocation.getPluggedInConnectionMethods();
		for (IConnectionMethod method : methods) {
			connectionMethodCombo.add(method.getName());
		}

		// pserver is a default connection method
		int defaultIndex = connectionMethodCombo.indexOf("pserver") != -1 ? connectionMethodCombo.indexOf("pserver") : 0;  //$NON-NLS-1$ //$NON-NLS-2$

		connectionMethodCombo.select(defaultIndex);
		useDefaultPort.setSelection(true);

		if(properties != null) {
			String method = properties.getProperty("connection"); //$NON-NLS-1$
			if (method == null) {
				connectionMethodCombo.select(defaultIndex);
			} else {
				connectionMethodCombo.select(connectionMethodCombo.indexOf(method));
			}

			String user = properties.getProperty("user"); //$NON-NLS-1$
			if (user != null) {
				userCombo.setText(user);
			}

			String password = properties.getProperty("password"); //$NON-NLS-1$
			if (password != null) {
				passwordText.setText(password);
			}

			String host = properties.getProperty("host"); //$NON-NLS-1$
			if (host != null) {
				hostCombo.setText(host);
			}

			String port = properties.getProperty("port"); //$NON-NLS-1$
			if (port != null) {
				useCustomPort.setSelection(true);
				portText.setText(port);
			}

			String repositoryPath = properties.getProperty("root"); //$NON-NLS-1$
			if (repositoryPath != null) {
				repositoryPathCombo.setText(repositoryPath);
			}
		}
	}
	/**
	 * Saves the widget values
	 */
	private void saveWidgetValues() {
		// Update history
		IDialogSettings settings = getDialogSettings();
		if (settings != null) {
			String userName = userCombo.getText();
			if (!userName.equals(ANONYMOUS_USER)) {
				String[] userNames = settings.getArray(STORE_USERNAME_ID);
				if (userNames == null) userNames = new String[0];
				userNames = addToHistory(userNames, userName);
				settings.put(STORE_USERNAME_ID, userNames);
			}
			String[] hostNames = settings.getArray(STORE_HOSTNAME_ID);
			if (hostNames == null) hostNames = new String[0];
			hostNames = addToHistory(hostNames, hostCombo.getText());
			settings.put(STORE_HOSTNAME_ID, hostNames);

			String[] paths = settings.getArray(STORE_PATH_ID);
			if (paths == null) paths = new String[0];
			paths = addToHistory(paths, repositoryPathCombo.getText());
			settings.put(STORE_PATH_ID, paths);

			if (showValidate) {
				settings.put(STORE_DONT_VALIDATE_ID, !validate);
			}
		}
	}

	public void setShowValidate(boolean showValidate) {
		this.showValidate = showValidate;
	}

	/**
	 * Sets the properties for the repository connection
	 * 
	 * @param properties  the properties or null
	 */
	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	/**
	 * Updates widget enablements and sets error message if appropriate.
	 */
	protected void updateWidgetEnablements() {
		if (useDefaultPort.getSelection()) {
			portText.setEnabled(false);
		} else {
			portText.setEnabled(true);
		}

		validateFields();
	}
	/**
	 * Validates the contents of the editable fields and set page completion
	 * and error messages appropriately.
	 */
	protected void validateFields() {
		String user = userCombo.getText();
		IStatus status = validateUserName(user);
		if (!isStatusOK(status)) {
			return;
		}

		String host = hostCombo.getText();
		status = validateHost(host);
		if (!isStatusOK(status)) {
			return;
		}

		if (portText.isEnabled()) {
			String port = portText.getText();
			status = validatePort(port);
			if (!isStatusOK(status)) {
				return;
			}
		}

		String pathString = repositoryPathCombo.getText();
		status = validatePath(pathString);
		if (!isStatusOK(status)) {
			return;
		}

		try {
			CVSRepositoryLocation l = CVSRepositoryLocation.fromProperties(createProperties());
			if (!l.equals(oldLocation) && KnownRepositories.getInstance().isKnownRepository(l.getLocation())) {
				setErrorMessage(CVSUIMessages.ConfigurationWizardMainPage_0);
				setPageComplete(false);
				return;
			}
		} catch (CVSException e) {
			CVSUIPlugin.log(e);
			// Let it pass. Creation should fail
		}

		// Everything passed so we're good to go
		setErrorMessage(null);
		setPageComplete(true);
	}

	private boolean isStatusOK(IStatus status) {
		if (!status.isOK()) {
			if (status.getCode() == REQUIRED_FIELD) {
				// Don't set the message for an empty field
				setErrorMessage(null);
			} else {
				setErrorMessage(status.getMessage());
			}
			setPageComplete(false);
			return false;
		}
		return true;
	}

	public boolean getValidate() {
		return validate;
	}
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			hostCombo.setFocus();
		}
	}

	public static final int REQUIRED_FIELD = 1;
	public static final int INVALID_FIELD_CONTENTS = 2;
	public static final IStatus validateUserName(String user) {
		if (user.length() == 0) {
			return new Status(IStatus.ERROR, CVSUIPlugin.ID, REQUIRED_FIELD, CVSUIMessages.ConfigurationWizardMainPage_1, null);
		}
		// removed the @ sign check since, so the UI can allow full kerberos names
		if (user.indexOf(':') != -1) {
			return new Status(IStatus.ERROR, CVSUIPlugin.ID, INVALID_FIELD_CONTENTS,
					CVSUIMessages.ConfigurationWizardMainPage_invalidUserName, null);
		}
		if (user.startsWith(" ") || user.endsWith(" ")) { //$NON-NLS-1$ //$NON-NLS-2$
			return new Status(IStatus.ERROR, CVSUIPlugin.ID, INVALID_FIELD_CONTENTS,
					CVSUIMessages.ConfigurationWizardMainPage_6, null);
		}
		return Status.OK_STATUS;
	}
	public static final IStatus validateHost(String host) {
		if (host.length() == 0) {
			return new Status(IStatus.ERROR, CVSUIPlugin.ID, REQUIRED_FIELD, CVSUIMessages.ConfigurationWizardMainPage_2, null);
		}
		if (host.indexOf(':') != -1) {
			return new Status(IStatus.ERROR, CVSUIPlugin.ID, INVALID_FIELD_CONTENTS,
					CVSUIMessages.ConfigurationWizardMainPage_invalidHostName, null);
		}
		if (host.startsWith(" ") || host.endsWith(" ")) { //$NON-NLS-1$ //$NON-NLS-2$
			return new Status(IStatus.ERROR, CVSUIPlugin.ID, INVALID_FIELD_CONTENTS,
					CVSUIMessages.ConfigurationWizardMainPage_5, null);
		}
		return Status.OK_STATUS;
	}
	public static final IStatus validatePort(String port) {
		if (port.length() == 0) {
			return new Status(IStatus.ERROR, CVSUIPlugin.ID, REQUIRED_FIELD, CVSUIMessages.ConfigurationWizardMainPage_3, null);
		}
		try {
			Integer.parseInt(port);
		} catch (NumberFormatException e) {
			return new Status(IStatus.ERROR, CVSUIPlugin.ID, INVALID_FIELD_CONTENTS,
					CVSUIMessages.ConfigurationWizardMainPage_invalidPort, null);
		}
		return Status.OK_STATUS;
	}
	public static final IStatus validatePath(String pathString) {
		if (pathString.length() == 0) {
			return new Status(IStatus.ERROR, CVSUIPlugin.ID, REQUIRED_FIELD, CVSUIMessages.ConfigurationWizardMainPage_4, null);
		}
		IPath path = new Path(null, pathString);
		String[] segments = path.segments();
		for (String string : segments) {
			if (string.charAt(0) == ' ' || string.charAt(string.length() -1) == ' ') {
				return new Status(IStatus.ERROR, CVSUIPlugin.ID, INVALID_FIELD_CONTENTS,
						CVSUIMessages.ConfigurationWizardMainPage_invalidPathWithSpaces, null);
			}
		}
		// look for // and inform the user that we support use of C:\cvs\root instead of /c//cvs/root
		if (pathString.contains("//")) { //$NON-NLS-1$
			if (pathString.indexOf("//") == 2) { //$NON-NLS-1$
				// The user is probably trying to specify a CVSNT path
				return new Status(IStatus.ERROR, CVSUIPlugin.ID, INVALID_FIELD_CONTENTS,
						CVSUIMessages.ConfigurationWizardMainPage_useNTFormat, null);
			} else {
				return new Status(IStatus.ERROR, CVSUIPlugin.ID, INVALID_FIELD_CONTENTS,
						CVSUIMessages.ConfigurationWizardMainPage_invalidPathWithSlashes, null);
			}
		}
		if (pathString.endsWith("/")) { //$NON-NLS-1$
			return new Status(IStatus.ERROR, CVSUIPlugin.ID, INVALID_FIELD_CONTENTS,
					CVSUIMessages.ConfigurationWizardMainPage_invalidPathWithTrailingSlash, null);
		}
		return Status.OK_STATUS;
	}
}
