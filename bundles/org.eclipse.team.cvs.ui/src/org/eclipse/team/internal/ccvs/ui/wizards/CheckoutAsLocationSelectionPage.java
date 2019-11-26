/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
 *     Philippe Ombredanne - bug 84808
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.wizards;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFolder;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.IHelpContextIds;
import org.eclipse.ui.PlatformUI;

/**
 * @author Administrator
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class CheckoutAsLocationSelectionPage extends CVSWizardPage {

	public static final String NAME = "CheckoutAsLocationSelectionPage"; //$NON-NLS-1$
	
	private Button browseButton;
	private Combo locationPathField;
	private Label locationLabel;
	private boolean useDefaults = true;
	private ICVSRemoteFolder[] remoteFolders;
	private String targetLocation;
	private IProject singleProject;
	
	// constants
	private static final int SIZING_TEXT_FIELD_WIDTH = 250;
	private static final int COMBO_HISTORY_LENGTH = 5;
	
	// store id constants
	private static final String STORE_PREVIOUS_LOCATIONS =
		"CheckoutAsLocationSelectionPage.STORE_PREVIOUS_LOCATIONS";//$NON-NLS-1$
	
	/**
	 * @param pageName
	 * @param title
	 * @param titleImage
	 * @param description
	 */
	public CheckoutAsLocationSelectionPage(ImageDescriptor titleImage, ICVSRemoteFolder[] remoteFolders) {
		super(NAME, CVSUIMessages.CheckoutAsLocationSelectionPage_title, titleImage, CVSUIMessages.CheckoutAsLocationSelectionPage_description); // 
		this.remoteFolders = remoteFolders;
	}

	/**
	 * @return
	 */
	private boolean isSingleFolder() {
		return remoteFolders.length == 1;
	}
	
	/**
	 * @param string
	 */
	public void setProject(IProject project) {
		singleProject = project;
		setLocationForSelection(true);
	}
	
	/**
	 * @param string
	 */
	public void setProjectName(String string) {
		if (string == null || string.equals(".")) return; //$NON-NLS-1$
		if (singleProject != null && singleProject.getName().equals(string)) return;
		setProject(ResourcesPlugin.getWorkspace().getRoot().getProject(string));
	}
	
	private IProject getSingleProject() {
		if (singleProject == null) {
			setProjectName(getPreferredFolderName(remoteFolders[0]));
		}
		return singleProject;
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite composite= createComposite(parent, 1, false);
		setControl(composite);
		// required in order to use setButtonLayoutData
		initializeDialogUnits(composite);
		
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.CHECKOUT_LOCATION_SELECTION_PAGE);

		final Button useDefaultsButton =
			new Button(composite, SWT.CHECK | SWT.RIGHT);
		useDefaultsButton.setText(CVSUIMessages.CheckoutAsLocationSelectionPage_useDefaultLabel); 
		useDefaultsButton.setSelection(this.useDefaults);

		createUserSpecifiedProjectLocationGroup(composite, !this.useDefaults);
		initializeValues();

		SelectionListener listener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				useDefaults = useDefaultsButton.getSelection();
				browseButton.setEnabled(!useDefaults);
				locationPathField.setEnabled(!useDefaults);
				locationLabel.setEnabled(!useDefaults);
				setLocationForSelection(true);
				setErrorMessage(useDefaults ? null : checkValidLocation());
			}
		};
		useDefaultsButton.addSelectionListener(listener);
		Dialog.applyDialogFont(parent);
	}
	
	/**
	 * Creates the project location specification controls.
	 *
	 * @return the parent of the widgets created
	 * @param projectGroup the parent composite
	 * @param enabled - sets the initial enabled state of the widgets
	 */
	private Composite createUserSpecifiedProjectLocationGroup(Composite parent, boolean enabled) {
	
		// This group needs 3 columns
		Composite projectGroup = createComposite(parent, 3, true);
		
		// location label
		locationLabel = new Label(projectGroup, SWT.NONE);
		if (isSingleFolder()) {
			locationLabel.setText(CVSUIMessages.CheckoutAsLocationSelectionPage_locationLabel); 
		} else {
			locationLabel.setText(CVSUIMessages.CheckoutAsLocationSelectionPage_parentDirectoryLabel); 
		}
		locationLabel.setEnabled(enabled);

		// project location entry field
		locationPathField = new Combo(projectGroup, SWT.DROP_DOWN);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = SIZING_TEXT_FIELD_WIDTH;
		locationPathField.setLayoutData(data);
		locationPathField.setEnabled(enabled);

		// browse button
		this.browseButton = new Button(projectGroup, SWT.PUSH);
		this.browseButton.setText(CVSUIMessages.CheckoutAsLocationSelectionPage_browseLabel); 
		this.browseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				handleLocationBrowseButtonPressed();
			}
		});
		this.browseButton.setEnabled(enabled);
		setButtonLayoutData(this.browseButton);

		// Set the initial value first before listener
		// to avoid handling an event during the creation.
		setLocationForSelection(true);
		locationPathField.addModifyListener(e -> setErrorMessage(checkValidLocation()));
		return projectGroup;
	}
	
	/**
	 * Initializes states of the controls.
	 */
	private void initializeValues() {
		// Set remembered values
		IDialogSettings settings = getDialogSettings();
		if (settings != null) {
			String[] previouseLocations = settings.getArray(STORE_PREVIOUS_LOCATIONS);
			if (previouseLocations != null) {
				for (String previouseLocation : previouseLocations) {
					if (isSingleFolder()) {
						locationPathField.add(new Path(previouseLocation).append(getSingleProject().getName()).toOSString());
					} else {
						locationPathField.add(previouseLocation);
					}
				}
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
			String[] previouseLocations = settings.getArray(STORE_PREVIOUS_LOCATIONS);
			if (previouseLocations == null) previouseLocations = new String[0];
			if(isSingleFolder())
				previouseLocations = addToHistory(previouseLocations, new Path(locationPathField.getText()).removeLastSegments(1).toOSString());
			else
				previouseLocations = addToHistory(previouseLocations, locationPathField.getText());
			settings.put(STORE_PREVIOUS_LOCATIONS, previouseLocations);
		}
	}

	/**
	 * Adds an entry to a history, while taking care of duplicate history items
	 * and excessively long histories.  The assumption is made that all histories
	 * should be of length <code>CheckoutAsLocationSelectionPage.COMBO_HISTORY_LENGTH</code>.
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
	
	/**
	 * Adds an entry to a history, while taking care of duplicate history items
	 * and excessively long histories.  The assumption is made that all histories
	 * should be of length <code>CheckoutAsLocationSelectionPage.COMBO_HISTORY_LENGTH</code>.
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
	 * Check if the entry in the widget location is valid. If it is valid return null. Otherwise
	 * return a string that indicates the problem.
	 */
	private String checkValidLocation() {

		if (useDefaults) {
			targetLocation = null;
			return null;
		} else {
			targetLocation = locationPathField.getText();
			if (targetLocation.isEmpty()) {
				return(CVSUIMessages.CheckoutAsLocationSelectionPage_locationEmpty); 
			}
			else{
				IPath path = new Path("");//$NON-NLS-1$
				if (!path.isValidPath(targetLocation)) {
					return CVSUIMessages.CheckoutAsLocationSelectionPage_invalidLocation; 
				}
			}

			if (isSingleFolder()) {
				IStatus locationStatus =
					ResourcesPlugin.getWorkspace().validateProjectLocation(
						getSingleProject(),
						new Path(targetLocation));
	
				if (!locationStatus.isOK())
					return locationStatus.getMessage();
			} else {
				for (ICVSRemoteFolder remoteFolder : remoteFolders) {
					String projectName = getPreferredFolderName(remoteFolder);
					IStatus locationStatus = ResourcesPlugin.getWorkspace().validateProjectLocation(
							ResourcesPlugin.getWorkspace().getRoot().getProject(projectName),
							new Path(targetLocation).append(projectName));
					if (!locationStatus.isOK())
						return locationStatus.getMessage();
				}
			}

			return null;
		}
	}
	
	/**
	 * Set the location to the default location if we are set to useDefaults.
	 */
	private void setLocationForSelection(boolean changed) {
		if (useDefaults) {
			IPath defaultPath = null;
			if (isSingleFolder()) {
				IProject singleProject = getSingleProject();
				if (singleProject != null) {
					try {
						defaultPath = singleProject.getDescription().getLocation();
					} catch (CoreException e) {
						// ignore
					}
					if (defaultPath == null) {
						defaultPath = Platform.getLocation().append(singleProject.getName());
					}
				}
			} else {
				defaultPath = Platform.getLocation();
			}
			if (defaultPath != null) {
				locationPathField.setText(defaultPath.toOSString());
			}
			targetLocation = null;
		} else if (changed) {
			IPath location = null;
			IProject project = getSingleProject();
			if (project != null) {
				try {
					location = project.getDescription().getLocation();
				} catch (CoreException e) {
					// ignore the exception
				}
			}
			if (location == null) {
				targetLocation = null;
				locationPathField.setText(""); //$NON-NLS-1$
			} else {
				if (isSingleFolder()) {
					targetLocation = location.toOSString();
				} else {
					targetLocation = location.removeLastSegments(1).toOSString();
				}
				locationPathField.setText(targetLocation);
			}
		}
	}
	
	/**
	 *	Open an appropriate directory browser
	 */
	private void handleLocationBrowseButtonPressed() {
		DirectoryDialog dialog = new DirectoryDialog(locationPathField.getShell());
		if (isSingleFolder()) {
			dialog.setMessage(NLS.bind(CVSUIMessages.CheckoutAsLocationSelectionPage_messageForSingle, new String[] { getSingleProject().getName() })); 
		} else {
			dialog.setMessage(NLS.bind(CVSUIMessages.CheckoutAsLocationSelectionPage_messageForMulti, new String[] { Integer.valueOf(remoteFolders.length).toString() })); 
		}
	
		String dirName = locationPathField.getText();
		if (!dirName.isEmpty()) {
			File path = new File(dirName);
			if (path.exists())
				dialog.setFilterPath(dirName);
		}

		String selectedDirectory = dialog.open();
		if (selectedDirectory != null) {
			if (isSingleFolder()) {
				locationPathField.setText(new Path(selectedDirectory).append(getSingleProject().getName()).toOSString());
			} else {
				locationPathField.setText(new Path(selectedDirectory).toOSString());
			}
		}
		targetLocation = locationPathField.getText();
	}
	
	/**
	 * Return the custom location for a single project. In this case, the specified
	 * location is used as the location of the project.
	 * 
	 * @param project
	 * @return
	 */
	public String getTargetLocation() {
		if (isCustomLocationSpecified()) {
			saveWidgetValues();
			return targetLocation;
		}
		return null;
	}

	/**
	 * @return
	 */
	private boolean isCustomLocationSpecified() {
		return !useDefaults;
	}

}
