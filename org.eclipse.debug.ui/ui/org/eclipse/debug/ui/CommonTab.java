/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
 *     Stephan Wahlbrink <sw@wahlbrink.eu> - Bug 471829
 *     Axel Richard (Obeo) - Bug 41353 - Launch configurations prototypes
 *******************************************************************************/
package org.eclipse.debug.ui;

import static org.eclipse.swt.accessibility.AccessibleListener.getNameAdapter;
import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationManager;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationsMessages;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchGroupExtension;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchHistory;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.ide.IDEEncoding;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;
import org.osgi.framework.FrameworkUtil;

/**
 * Launch configuration tab used to specify the location a launch configuration
 * is stored in, whether it should appear in the favorites list, and perspective
 * switching behavior for an associated launch.
 * <p>
 * Clients may call {@link #setHelpContextId(String)} on this tab prior to control
 * creation to alter the default context help associated with this tab.
 * </p>
 * <p>
 * Clients may instantiate this class.
 * </p>
 * @since 2.0
 * @noextend This class is not intended to be subclassed by clients.
 */
public class CommonTab extends AbstractLaunchConfigurationTab {

	/**
	 * Constant representing the id of the {@link IDialogSettings} location for the {@link ContainerSelectionDialog} used
	 * on this tab
	 *
	 * @since 3.6
	 */
	private static final String SHARED_LAUNCH_CONFIGURATON_DIALOG = IDebugUIConstants.PLUGIN_ID
			+ ".SHARED_LAUNCH_CONFIGURATON_DIALOG"; //$NON-NLS-1$
	private static final String WORKSPACE_SELECTION_DIALOG = IDebugUIConstants.PLUGIN_ID
			+ ".WORKSPACE_SELECTION_DIALOG"; //$NON-NLS-1$

	/**
	 * This attribute exists solely for the purpose of making sure that invalid shared locations
	 * can be revertible. This attribute is not saveable and will never appear in a saved
	 * launch configuration.
	 * @since 3.3
	 */
	private static final String BAD_CONTAINER = "bad_container_name"; //$NON-NLS-1$

	// Local/shared UI widgets
	private Composite fIoComposit;
	private Button fLocalRadioButton;
	private Button fSharedRadioButton;
	private Text fSharedLocationText;
	private Button fSharedLocationButton;
	private Button fLaunchInBackgroundButton;
	private Button fTerminateDescendantsButton;
	private Button fDefaultEncodingButton;
	private Button fAltEncodingButton;
	private Button forceSystemEncodingButton;
	private Combo fEncodingCombo;
	private Button fConsoleOutput;
	private Button fFileOutput;
	private Button fFileBrowse;
	private Text fFileText;
	private Button fVariables;
	private Button fAppend;
	private Button fMergeOutput;
	private Button fWorkspaceBrowse;

	private Button fInputFileCheckButton;
	private Text fInputFileLocationText;
	private Button fInputFileBrowse;
	private Button fInputVariables;
	private Button fInputWorkspaceBrowse;

	/**
	 * Check box list for specifying favorites
	 */
	private CheckboxTableViewer fFavoritesTable;

	/**
	 * Modify listener that simply updates the owning launch configuration dialog.
	 */
	private ModifyListener fBasicModifyListener = evt -> scheduleUpdateJob();

	/**
	 * Constructs a new tab with default context help.
	 */
	public CommonTab() {
		super();
		setHelpContextId(IDebugHelpContextIds.LAUNCH_CONFIGURATION_DIALOG_COMMON_TAB);
	}

	@Override
	public void createControl(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), getHelpContextId());
		comp.setLayout(new GridLayout(2, true));
		comp.setFont(parent.getFont());

		createSharedConfigComponent(comp);
		createFavoritesComponent(comp);
		createEncodingComponent(comp);
		createOutputCaptureComponent(comp);
		createLaunchInBackgroundComponent(comp);
		createTerminateDescendantsButtonComponent(comp);
	}

	/**
	 * Returns the {@link IDialogSettings} for the given id
	 *
	 * @param id the id of the dialog settings to get
	 * @return the {@link IDialogSettings} to pass into the {@link ContainerSelectionDialog}
	 * @since 3.6
	 */
	IDialogSettings getDialogBoundsSettings(String id) {
		IDialogSettings settings = PlatformUI.getDialogSettingsProvider(FrameworkUtil.getBundle(CommonTab.class))
				.getDialogSettings();
		IDialogSettings section = settings.getSection(id);
		if (section == null) {
			section = settings.addNewSection(id);
		}
		return section;
	}

	/**
	 * Creates the favorites control
	 * @param parent the parent composite to add this one to
	 * @since 3.2
	 */
	private void createFavoritesComponent(Composite parent) {
		Group favComp = SWTFactory.createGroup(parent, LaunchConfigurationsMessages.CommonTab_Display_in_favorites_menu__10, 1, 1, GridData.FILL_BOTH);
		fFavoritesTable = CheckboxTableViewer.newCheckList(favComp, SWT.CHECK | SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		Control table = fFavoritesTable.getControl();
		GridData gd = new GridData(GridData.FILL_BOTH);
		table.setLayoutData(gd);
		table.setFont(parent.getFont());
		fFavoritesTable.setContentProvider(new FavoritesContentProvider());
		fFavoritesTable.setLabelProvider(new FavoritesLabelProvider());
		fFavoritesTable.addCheckStateListener(event -> updateLaunchConfigurationDialog());
	}

	/**
	 * Creates the shared config component
	 * @param parent the parent composite to add this component to
	 * @since 3.2
	 */
	private void createSharedConfigComponent(Composite parent) {
		Group group = SWTFactory.createGroup(parent, LaunchConfigurationsMessages.CommonTab_0, 3, 2, GridData.FILL_HORIZONTAL);
		Composite comp = SWTFactory.createComposite(group, parent.getFont(), 3, 3, GridData.FILL_BOTH, 0, 0);
		fLocalRadioButton = createRadioButton(comp, LaunchConfigurationsMessages.CommonTab_L_ocal_3);
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		fLocalRadioButton.setLayoutData(gd);
		fSharedRadioButton = createRadioButton(comp, LaunchConfigurationsMessages.CommonTab_S_hared_4);
		fSharedRadioButton.addSelectionListener(widgetSelectedAdapter(e -> handleSharedRadioButtonSelected()));

		fSharedLocationText = SWTFactory.createSingleText(comp, 1);
		fSharedLocationText.getAccessible().addAccessibleListener(
				getNameAdapter(e -> e.result = LaunchConfigurationsMessages.CommonTab_S_hared_4));

		fSharedLocationText.addModifyListener(fBasicModifyListener);
		fSharedLocationButton = createPushButton(comp, LaunchConfigurationsMessages.CommonTab__Browse_6, null);
		fSharedLocationButton.addSelectionListener(widgetSelectedAdapter(e -> handleSharedLocationButtonSelected()));

		fLocalRadioButton.setSelection(true);
		setSharedEnabled(false);
	}

	/**
	 * Creates the component set for the capture output composite
	 * @param parent the parent to add this component to
	 */
	private void createOutputCaptureComponent(Composite parent) {
		Group group = SWTFactory.createGroup(parent, LaunchConfigurationsMessages.CommonTab_4, 5, 2, GridData.FILL_HORIZONTAL);
		createInputCaptureComponent(group);
		Composite comp = SWTFactory.createComposite(group, group.getFont(), 5, 5, GridData.FILL_BOTH, 0, 0);
		fIoComposit = comp;
		fFileOutput = createCheckButton(comp, LaunchConfigurationsMessages.CommonTab_6);
		fFileOutput.setLayoutData(new GridData(SWT.BEGINNING, SWT.NORMAL, false, false));
		fFileOutput.addSelectionListener(widgetSelectedAdapter(e -> {
			enableOuputCaptureWidgets(fFileOutput.getSelection());
			updateLaunchConfigurationDialog();
		}));
		fFileText = SWTFactory.createSingleText(comp, 4);
		fFileText.getAccessible()
				.addAccessibleListener(getNameAdapter(e -> e.result = LaunchConfigurationsMessages.CommonTab_6));
		fFileText.addModifyListener(fBasicModifyListener);

		Composite bcomp = SWTFactory.createComposite(comp, 3, 5, GridData.HORIZONTAL_ALIGN_END);
		GridLayout ld = (GridLayout)bcomp.getLayout();
		ld.marginHeight = 1;
		ld.marginWidth = 0;
		fWorkspaceBrowse = createPushButton(bcomp, LaunchConfigurationsMessages.CommonTab_12, null);
		fWorkspaceBrowse.addSelectionListener(widgetSelectedAdapter(e -> {
			ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getShell(), new WorkbenchLabelProvider(),
					new WorkbenchContentProvider());
			dialog.setTitle(LaunchConfigurationsMessages.CommonTab_13);
			dialog.setMessage(LaunchConfigurationsMessages.CommonTab_14);
			dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
			dialog.setComparator(new ResourceComparator(ResourceComparator.NAME));
			dialog.setDialogBoundsSettings(getDialogBoundsSettings(WORKSPACE_SELECTION_DIALOG),
					Dialog.DIALOG_PERSISTSIZE);
			if (dialog.open() == IDialogConstants.OK_ID) {
				IResource resource = (IResource) dialog.getFirstResult();
				if (resource != null) {
					String arg = resource.getFullPath().toString();
					String fileLoc = VariablesPlugin.getDefault().getStringVariableManager()
							.generateVariableExpression("workspace_loc", arg); //$NON-NLS-1$
					fFileText.setText(fileLoc);
				}
			}
		}));
		fFileBrowse = createPushButton(bcomp, LaunchConfigurationsMessages.CommonTab_7, null);
		fFileBrowse.addSelectionListener(widgetSelectedAdapter(e -> {
			String filePath = fFileText.getText();
			FileDialog dialog = new FileDialog(getShell(), SWT.SAVE | SWT.SHEET);
			filePath = dialog.open();
			if (filePath != null) {
				fFileText.setText(filePath);
			}
		}));
		fVariables = createPushButton(bcomp, LaunchConfigurationsMessages.CommonTab_9, null);
		fVariables.addSelectionListener(widgetSelectedAdapter(e -> {
			StringVariableSelectionDialog dialog = new StringVariableSelectionDialog(getShell());
			dialog.open();
			String variable = dialog.getVariableExpression();
			if (variable != null) {
				fFileText.insert(variable);
			}
		}));
		fAppend = createCheckButton(comp, LaunchConfigurationsMessages.CommonTab_11);

		GridData gd = new GridData(SWT.LEFT, SWT.TOP, true, false);
		gd.horizontalSpan = 5;
		fAppend.setLayoutData(gd);
		fAppend.addSelectionListener(widgetSelectedAdapter(e -> updateLaunchConfigurationDialog()));
	}

	private void createInputCaptureComponent(Composite parent){
		Composite comp1 = SWTFactory.createComposite(parent, parent.getFont(), 5, 5, GridData.FILL_BOTH, 0, 0);
		fConsoleOutput = createCheckButton(comp1, LaunchConfigurationsMessages.CommonTab_5);
		fConsoleOutput.addSelectionListener(widgetSelectedAdapter(e -> updateLaunchConfigurationDialog()));

		Composite comp = SWTFactory.createComposite(comp1, comp1.getFont(), 5, 5, GridData.FILL_BOTH, 0, 0);
		fInputFileCheckButton = createCheckButton(comp, LaunchConfigurationsMessages.CommonTab_17);
		GridData gd = new GridData(SWT.BEGINNING, SWT.NORMAL, false, false);
		gd.horizontalSpan = 3;

		fInputFileCheckButton.setLayoutData(gd);
		fInputFileCheckButton.addSelectionListener(widgetSelectedAdapter(e -> {
			handleInputFileButtonSelected();
			updateLaunchConfigurationDialog();
		}));

		fInputFileLocationText = SWTFactory.createSingleText(comp, 2);
		fInputFileLocationText.getAccessible()
				.addAccessibleListener(getNameAdapter(e -> e.result = LaunchConfigurationsMessages.CommonTab_17));
		fInputFileLocationText.addModifyListener(fBasicModifyListener);
		Composite bcomp = SWTFactory.createComposite(comp, 3, 5, GridData.HORIZONTAL_ALIGN_END);
		GridLayout ld = (GridLayout) bcomp.getLayout();
		ld.marginHeight = 1;
		ld.marginWidth = 0;
		fInputWorkspaceBrowse = createPushButton(bcomp, LaunchConfigurationsMessages.CommonTab_16, null);
		fInputWorkspaceBrowse.addSelectionListener(widgetSelectedAdapter(e -> {
			ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getShell(), new WorkbenchLabelProvider(),
					new WorkbenchContentProvider());
			dialog.setTitle(LaunchConfigurationsMessages.CommonTab_13);
			dialog.setValidator(selection -> {
				if (selection.length == 0) {
					return new Status(IStatus.ERROR, DebugUIPlugin.getUniqueIdentifier(), 0,
							IInternalDebugCoreConstants.EMPTY_STRING, null);
				}
				for (Object f : selection) {
					if (!(f instanceof IFile)) {
						return new Status(IStatus.ERROR, DebugUIPlugin.getUniqueIdentifier(), 0,
								IInternalDebugCoreConstants.EMPTY_STRING, null);
					}
				}
				return new Status(IStatus.OK, DebugUIPlugin.getUniqueIdentifier(), 0,
						IInternalDebugCoreConstants.EMPTY_STRING, null);
			});
			dialog.setMessage(LaunchConfigurationsMessages.CommonTab_18);
			dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
			dialog.setComparator(new ResourceComparator(ResourceComparator.NAME));
			dialog.setDialogBoundsSettings(getDialogBoundsSettings(WORKSPACE_SELECTION_DIALOG),
					Dialog.DIALOG_PERSISTSIZE);
			if (dialog.open() == IDialogConstants.OK_ID) {
				IResource resource = (IResource) dialog.getFirstResult();
				if (resource != null) {
					String arg = resource.getFullPath().toString();
					String fileLoc = VariablesPlugin.getDefault().getStringVariableManager()
							.generateVariableExpression("workspace_loc", arg); //$NON-NLS-1$
					fInputFileLocationText.setText(fileLoc);
				}
			}
		}));
		fInputFileBrowse = createPushButton(bcomp, LaunchConfigurationsMessages.CommonTab_19, null);
		fInputFileBrowse.addSelectionListener(widgetSelectedAdapter(e -> {
			String filePath = fInputFileLocationText.getText();
			FileDialog dialog = new FileDialog(getShell(), SWT.OK | SWT.SHEET);
			filePath = dialog.open();
			if (filePath != null) {
				fInputFileLocationText.setText(filePath);
			}
		}));
		fInputVariables = createPushButton(bcomp, LaunchConfigurationsMessages.CommonTab_20, null);
		fInputVariables.addSelectionListener(widgetSelectedAdapter(e -> {
			StringVariableSelectionDialog dialog = new StringVariableSelectionDialog(getShell());
			dialog.open();
			String variable = dialog.getVariableExpression();
			if (variable != null) {
				fInputFileLocationText.insert(variable);
			}
		}));

		setInputFileEnabled(false);
	}
	/**
	 * Enables or disables the output capture widgets based on the the specified enablement
	 * @param enable if the output capture widgets should be enabled or not
	 * @since 3.2
	 */
	private void enableOuputCaptureWidgets(boolean enable) {
		fFileText.setEnabled(enable);
		fFileBrowse.setEnabled(enable);
		fWorkspaceBrowse.setEnabled(enable);
		fVariables.setEnabled(enable);
		fAppend.setEnabled(enable);
	}

	/**
	 * Returns the default encoding for the specified config
	 * @param config the configuration to get the encoding for
	 * @return the default encoding
	 *
	 * @since 3.4
	 */
	private String getDefaultEncoding(ILaunchConfiguration config) {
		try {
			IResource[] resources = config.getMappedResources();
			if(resources != null && resources.length > 0) {
				IResource res = resources[0];
				if(res instanceof IFile) {
					return ((IFile)res).getCharset();
				}
				else if(res instanceof IContainer) {
					return ((IContainer)res).getDefaultCharset();
				}
			}
		}
		catch(CoreException ce) {
			DebugUIPlugin.log(ce);
		}
		return ResourcesPlugin.getEncoding();
	}

	/**
	 * Creates the encoding component
	 * @param parent the parent to add this composite to
	 */
	private void createEncodingComponent(Composite parent) {
		Group group = SWTFactory.createGroup(parent, LaunchConfigurationsMessages.CommonTab_1, 2, 1, GridData.FILL_BOTH);

		forceSystemEncodingButton = createRadioButton(group, IInternalDebugCoreConstants.EMPTY_STRING);
		GridData gd = new GridData(SWT.BEGINNING, SWT.NORMAL, true, false);
		gd.horizontalSpan = 2;
		forceSystemEncodingButton.setLayoutData(gd);

		fDefaultEncodingButton = createRadioButton(group, IInternalDebugCoreConstants.EMPTY_STRING);
		gd = new GridData(SWT.BEGINNING, SWT.NORMAL, true, false);
		gd.horizontalSpan = 2;
		fDefaultEncodingButton.setLayoutData(gd);

		fAltEncodingButton = createRadioButton(group, LaunchConfigurationsMessages.CommonTab_3);
		fAltEncodingButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

		fEncodingCombo = new Combo(group, SWT.NONE);
		fEncodingCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fEncodingCombo.setFont(parent.getFont());
		List<String> allEncodings = IDEEncoding.getIDEEncodings();
		String[] encodingArray = allEncodings.toArray(new String[0]);
		fEncodingCombo.setItems(encodingArray);
		if (encodingArray.length > 0) {
			fEncodingCombo.select(0);
		}
		fEncodingCombo.getAccessible()
				.addAccessibleListener(getNameAdapter(e -> e.result = LaunchConfigurationsMessages.CommonTab_3));

		SelectionListener listener = widgetSelectedAdapter(e -> {
			if (e.getSource() instanceof Button) {
				Button button = (Button) e.getSource();
				if (button.getSelection()) {
					updateLaunchConfigurationDialog();
					fEncodingCombo.setEnabled(fAltEncodingButton.getSelection());
				}
			} else {
				updateLaunchConfigurationDialog();
			}
		});
		forceSystemEncodingButton.addSelectionListener(listener);
		fAltEncodingButton.addSelectionListener(listener);
		fDefaultEncodingButton.addSelectionListener(listener);
		fEncodingCombo.addSelectionListener(listener);
		fEncodingCombo.addKeyListener(KeyListener.keyReleasedAdapter(e -> scheduleUpdateJob()));
	}

	/**
	 * Returns whether or not the given encoding is valid.
	 *
	 * @param enc
	 *            the encoding to validate
	 * @return <code>true</code> if the encoding is valid, <code>false</code>
	 *         otherwise
	 */
	private boolean isValidEncoding(String enc) {
		try {
			return Charset.isSupported(enc);
		} catch (IllegalCharsetNameException e) {
			// This is a valid exception
			return false;
		}
	}

	/**
	 * Creates the controls needed to edit the launch in background
	 * attribute of an external tool
	 *
	 * @param parent the composite to create the controls in
	 */
	protected void createLaunchInBackgroundComponent(Composite parent) {
		fLaunchInBackgroundButton = createCheckButton(parent, LaunchConfigurationsMessages.CommonTab_10);
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		data.horizontalSpan = 2;
		fLaunchInBackgroundButton.setLayoutData(data);
		fLaunchInBackgroundButton.setFont(parent.getFont());
		fLaunchInBackgroundButton.addSelectionListener(widgetSelectedAdapter(e -> updateLaunchConfigurationDialog()));
	}

	/**
	 * Creates the controls needed to edit the terminate descendants attribute of an
	 * external tool
	 *
	 * @param parent the composite to create the controls in
	 */
	private void createTerminateDescendantsButtonComponent(Composite parent) {
		fTerminateDescendantsButton = createCheckButton(parent,
				LaunchConfigurationsMessages.CommonTab_AttributeLabel_TerminateDescendants);
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		data.horizontalSpan = 2;
		fTerminateDescendantsButton.setLayoutData(data);
		fTerminateDescendantsButton.setFont(parent.getFont());
		fTerminateDescendantsButton.addSelectionListener(widgetSelectedAdapter(e -> updateLaunchConfigurationDialog()));
	}

	/**
	 * handles the shared radio button being selected
	 */
	private void handleSharedRadioButtonSelected() {
		setSharedEnabled(isShared());
		updateLaunchConfigurationDialog();
	}

	/**
	 * handles the input file  being selected
	 */
	private void handleInputFileButtonSelected() {
		setInputFileEnabled(isInputFile());
		updateLaunchConfigurationDialog();
	}

	/**
	 * Sets the widgets for specifying that a launch configuration is to be shared to the enable value
	 * @param enable the enabled value for
	 */
	private void setSharedEnabled(boolean enable) {
		fSharedLocationText.setEnabled(enable);
		fSharedLocationButton.setEnabled(enable);
	}

	private void setInputFileEnabled(boolean enable) {
		fInputFileLocationText.setEnabled(enable);
		fInputFileBrowse.setEnabled(enable);
		fInputWorkspaceBrowse.setEnabled(enable);
		fInputVariables.setEnabled(enable);
	}

	private String getDefaultSharedConfigLocation(ILaunchConfiguration config) {
		String path = IInternalDebugCoreConstants.EMPTY_STRING;
		try {
			IResource[] mappedResources = config.getMappedResources();
			if(mappedResources != null) {
				IProject  proj;
				for (IResource resource : mappedResources) {
					proj = resource.getProject();
					if(proj != null && proj.isAccessible()) {
						return proj.getFullPath().toOSString();
					}
				}
			}
		}
		catch (CoreException e) {DebugUIPlugin.log(e);}
		return path;
	}

	/**
	 * if the shared radio button is selected, indicating that the launch configuration is to be shared
	 * @return true if the radio button is selected, false otherwise
	 */
	private boolean isShared() {
		return fSharedRadioButton.getSelection();
	}

	/**
	 * if the input file button is selected, indicating that the launch will
	 * take input file as stdin
	 *
	 * @return true if the check button is selected, false otherwise
	 */
	private boolean isInputFile() {
		return fInputFileCheckButton.getSelection();
	}

	/**
	 * Handles the shared location button being selected
	 */
	private void handleSharedLocationButtonSelected() {
		String currentContainerString = fSharedLocationText.getText();
		IContainer currentContainer = getContainer(currentContainerString);
		ContainerSelectionDialog dialog = new ContainerSelectionDialog(getShell(),
				   currentContainer,
				   false,
				   LaunchConfigurationsMessages.CommonTab_Select_a_location_for_the_launch_configuration_13);
		dialog.showClosedProjects(false);
		dialog.setDialogBoundsSettings(getDialogBoundsSettings(SHARED_LAUNCH_CONFIGURATON_DIALOG), Dialog.DIALOG_PERSISTSIZE);
		dialog.open();
		Object[] results = dialog.getResult();
		if ((results != null) && (results.length > 0) && (results[0] instanceof IPath)) {
			IPath path = (IPath)results[0];
			String containerName = path.toOSString();
			fSharedLocationText.setText(containerName);
		}
	}

	/**
	 * gets the container form the specified path
	 * @param path the path to get the container from
	 * @return the container for the specified path or null if one cannot be determined
	 */
	private IContainer getContainer(String path) {
		Path containerPath = new Path(path);
		return (IContainer) getWorkspaceRoot().findMember(containerPath);
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		boolean isShared = !configuration.isLocal();
		fSharedRadioButton.setSelection(isShared);
		fLocalRadioButton.setSelection(!isShared);
		setSharedEnabled(isShared);
		fSharedLocationText.setText(getDefaultSharedConfigLocation(configuration));
		if(isShared) {
			String containerName = IInternalDebugCoreConstants.EMPTY_STRING;
			IFile file = configuration.getFile();
			if (file != null) {
				IContainer parent = file.getParent();
				if (parent != null) {
					containerName = parent.getFullPath().toOSString();
				}
			}
			fSharedLocationText.setText(containerName);
		}
		updateFavoritesFromConfig(configuration);
		updateLaunchInBackground(configuration);
		updateEncoding(configuration);
		updateConsoleOutput(configuration);

		boolean terminateDescendants = getAttribute(configuration, DebugPlugin.ATTR_TERMINATE_DESCENDANTS, true);
		fTerminateDescendantsButton.setSelection(terminateDescendants);
	}

	/**
	 * Updates the console output form the local configuration
	 * @param configuration the local configuration
	 */
	private void updateConsoleOutput(ILaunchConfiguration configuration) {
		boolean outputToConsole = getAttribute(configuration, IDebugUIConstants.ATTR_CAPTURE_IN_CONSOLE, true);
		String stdinFromFile = getAttribute(configuration, IDebugUIConstants.ATTR_CAPTURE_STDIN_FILE, (String) null);
		String outputFile = getAttribute(configuration, IDebugUIConstants.ATTR_CAPTURE_IN_FILE, (String) null);
		boolean append = getAttribute(configuration, IDebugUIConstants.ATTR_APPEND_TO_FILE, false);
		boolean mergeOutput = getAttribute(configuration, DebugPlugin.ATTR_MERGE_OUTPUT, false);
		boolean supportsMergeOutput = false;
		try {
			supportsMergeOutput = configuration.getType().supportsOutputMerging();
		} catch (CoreException e) {
		}

		fConsoleOutput.setSelection(outputToConsole);
		fAppend.setSelection(append);
		if (supportsMergeOutput) {
			fMergeOutput = createCheckButton(fIoComposit, LaunchConfigurationsMessages.CommonTab_21);
			GridData gd = new GridData(SWT.LEFT, SWT.TOP, true, false);
			gd.horizontalSpan = 5;
			fMergeOutput.setLayoutData(gd);
			fMergeOutput.addSelectionListener(widgetSelectedAdapter(e -> updateLaunchConfigurationDialog()));
			fMergeOutput.setSelection(mergeOutput);
		}
		else if (fMergeOutput != null) {
			fMergeOutput.dispose();
			fMergeOutput = null;
		}
		boolean haveOutputFile= outputFile != null;
		if (haveOutputFile) {
			fFileText.setText(outputFile);
		}
		fFileOutput.setSelection(haveOutputFile);
		enableOuputCaptureWidgets(haveOutputFile);

		boolean haveInputFile = stdinFromFile != null;
		if (haveInputFile) {
			fInputFileLocationText.setText(stdinFromFile);
		}
		fInputFileCheckButton.setSelection(haveInputFile);
		setInputFileEnabled(haveInputFile);
	}

	/**
	 * Updates the launch on background check button
	 * @param configuration the local launch configuration
	 */
	protected void updateLaunchInBackground(ILaunchConfiguration configuration) {
		fLaunchInBackgroundButton.setSelection(isLaunchInBackground(configuration));
	}

	/**
	 * Updates the encoding
	 * @param configuration the local configuration
	 */
	private void updateEncoding(ILaunchConfiguration configuration) {
		boolean forceSystemEncoding = getAttribute(configuration, DebugPlugin.ATTR_FORCE_SYSTEM_CONSOLE_ENCODING,
				false);

		String encoding = getAttribute(configuration, DebugPlugin.ATTR_CONSOLE_ENCODING, (String) null);
		String defaultEncoding = getDefaultEncoding(configuration);
		Charset nativeEncoding = Platform.getSystemCharset();

		forceSystemEncodingButton
				.setText(MessageFormat.format(LaunchConfigurationsMessages.CommonTab_22, nativeEncoding.name()));
		forceSystemEncodingButton.pack();
		fDefaultEncodingButton.setText(MessageFormat.format(LaunchConfigurationsMessages.CommonTab_2, defaultEncoding));
		fDefaultEncodingButton.pack();
		if (forceSystemEncoding) {
			forceSystemEncodingButton.setSelection(true);
			fDefaultEncodingButton.setSelection(false);
			fAltEncodingButton.setSelection(false);
			fEncodingCombo.setEnabled(false);
		} else {
			forceSystemEncodingButton.setSelection(false);
			if (encoding != null) {
				fAltEncodingButton.setSelection(true);
				fDefaultEncodingButton.setSelection(false);
				if (!encoding.isBlank()) {
					fEncodingCombo.setText(encoding);
				}
				fEncodingCombo.setEnabled(true);
			} else {
				fDefaultEncodingButton.setSelection(true);
				fAltEncodingButton.setSelection(false);
				fEncodingCombo.setEnabled(false);
			}
		}
	}

	/**
	 * Returns whether the given configuration should be launched in the background.
	 *
	 * @param configuration the configuration
	 * @return whether the configuration is configured to launch in the background
	 */
	public static boolean isLaunchInBackground(ILaunchConfiguration configuration) {
		return getAttribute(configuration, IDebugUIConstants.ATTR_LAUNCH_IN_BACKGROUND, true);
	}

	/**
	 * Updates the favorites selections from the local configuration
	 * @param config the local configuration
	 */
	@SuppressWarnings("deprecation")
	private void updateFavoritesFromConfig(ILaunchConfiguration config) {
		fFavoritesTable.setInput(config);
		fFavoritesTable.setCheckedElements(new Object[]{});
		List<String> groups = getAttribute(config, IDebugUIConstants.ATTR_FAVORITE_GROUPS, new ArrayList<>());

		if (groups.isEmpty()) {
			// check old attributes for backwards compatible
			if (getAttribute(config, IDebugUIConstants.ATTR_DEBUG_FAVORITE, false)) {
				groups.add(IDebugUIConstants.ID_DEBUG_LAUNCH_GROUP);
			}
			if (getAttribute(config, IDebugUIConstants.ATTR_RUN_FAVORITE, false)) {
				groups.add(IDebugUIConstants.ID_RUN_LAUNCH_GROUP);
			}
		}
		if (!groups.isEmpty()) {
			List<LaunchGroupExtension> list = new ArrayList<>();
			for (String id : groups) {
				LaunchGroupExtension extension = getLaunchConfigurationManager().getLaunchGroup(id);
				if (extension != null) {
					list.add(extension);
				}
			}
			fFavoritesTable.setCheckedElements(list.toArray());
		}
	}

	/**
	 * Updates the configuration form the local shared config working copy
	 * @param config the local shared config working copy
	 */
	private void updateConfigFromLocalShared(ILaunchConfigurationWorkingCopy config) {
		if (isShared()) {
			String containerPathString = fSharedLocationText.getText();
			IContainer container = getContainer(containerPathString);
			if(container == null) {
				//we need to force an attribute to allow the invalid container path to be revertable
				config.setAttribute(BAD_CONTAINER, containerPathString);
			}
			else {
				config.setContainer(container);
			}
		} else {
			config.setContainer(null);
		}
	}

	/**
	 * Convenience accessor
	 * @return the singleton {@link LaunchConfigurationManager}
	 */
	protected LaunchConfigurationManager getLaunchConfigurationManager() {
		return DebugUIPlugin.getDefault().getLaunchConfigurationManager();
	}

	/**
	 * Update the favorite settings.
	 *
	 * NOTE: set to <code>null</code> instead of <code>false</code> for backwards compatibility
	 *  when comparing if content is equal, since 'false' is default
	 * 	and will be missing for older configurations.
	 * @param config the configuration to update
	 */
	@SuppressWarnings("deprecation")
	private void updateConfigFromFavorites(ILaunchConfigurationWorkingCopy config) {
		Object[] checked = fFavoritesTable.getCheckedElements();
		boolean debug = getAttribute(config, IDebugUIConstants.ATTR_DEBUG_FAVORITE, false);
		boolean run = getAttribute(config, IDebugUIConstants.ATTR_RUN_FAVORITE, false);
		if (debug || run) {
			// old attributes
			List<LaunchGroupExtension> groups = new ArrayList<>();
			int num = 0;
			if (debug) {
				groups.add(getLaunchConfigurationManager().getLaunchGroup(IDebugUIConstants.ID_DEBUG_LAUNCH_GROUP));
				num++;
			}
			if (run) {
				num++;
				groups.add(getLaunchConfigurationManager().getLaunchGroup(IDebugUIConstants.ID_RUN_LAUNCH_GROUP));
			}
			// see if there are any changes
			if (num == checked.length) {
				boolean different = false;
				for (Object checked1 : checked) {
					if (!groups.contains(checked1)) {
						different = true;
						break;
					}
				}
				if (!different) {
					return;
				}
			}
		}
		config.setAttribute(IDebugUIConstants.ATTR_DEBUG_FAVORITE, (String) null);
		config.setAttribute(IDebugUIConstants.ATTR_RUN_FAVORITE, (String) null);
		List<String> groups = null;
		for (Object c : checked) {
			LaunchGroupExtension group = (LaunchGroupExtension) c;
			if (groups == null) {
				groups = new ArrayList<>();
			}
			groups.add(group.getIdentifier());
		}
		config.setAttribute(IDebugUIConstants.ATTR_FAVORITE_GROUPS, groups);
	}

	private static boolean getAttribute(ILaunchConfiguration config, String attribute, boolean defaultValue) {
		try {
			return config.getAttribute(attribute, defaultValue);
		} catch (CoreException ce) {
			DebugUIPlugin.log(ce);
			return defaultValue;
		}
	}

	private static String getAttribute(ILaunchConfiguration config, String attribute, String defaultValue) {
		try {
			return config.getAttribute(attribute, defaultValue);
		} catch (CoreException ce) {
			DebugUIPlugin.log(ce);
			return defaultValue;
		}
	}

	private static List<String> getAttribute(ILaunchConfiguration config, String attribute, List<String> defaultValue) {
		try {
			return config.getAttribute(attribute, defaultValue);
		} catch (CoreException ce) {
			DebugUIPlugin.log(ce);
			return defaultValue;
		}
	}

	/**
	 * Convenience method for getting the workspace root.
	 * @return the singleton {@link IWorkspaceRoot}
	 */
	private IWorkspaceRoot getWorkspaceRoot() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}

	@Override
	public boolean isValid(ILaunchConfiguration config) {
		setMessage(null);
		setErrorMessage(null);

		return validateLocalShared() && validateRedirectFile() && validateEncoding() && validateStdinFile();
	}

	/**
	 * validates the encoding selection
	 * @return true if the validate encoding is allowable, false otherwise
	 */
	private boolean validateEncoding() {
		if (fAltEncodingButton.getSelection() && fEncodingCombo.getSelectionIndex() == -1
				&& !isValidEncoding(fEncodingCombo.getText().trim())) {
			setErrorMessage(LaunchConfigurationsMessages.CommonTab_15);
			return false;
		}
		return true;
	}

	/**
	 * Validates if the redirect file is valid
	 * @return true if the filename is not zero, false otherwise
	 */
	private boolean validateRedirectFile() {
		if(fFileOutput.getSelection()) {
			int len = fFileText.getText().trim().length();
			if (len == 0) {
				setErrorMessage(LaunchConfigurationsMessages.CommonTab_8);
				return false;
			}
		}
		return true;
	}

	/**
	 * validates the local shared config file location
	 * @return true if the local shared file exists, false otherwise
	 */
	private boolean validateLocalShared() {
		if (isShared()) {
			String path = fSharedLocationText.getText().trim();
			IContainer container = getContainer(path);
			if (container == null || container.equals(ResourcesPlugin.getWorkspace().getRoot())) {
				setErrorMessage(LaunchConfigurationsMessages.CommonTab_Invalid_shared_configuration_location_14);
				return false;
			} else if (!container.getProject().isOpen()) {
				setErrorMessage(LaunchConfigurationsMessages.CommonTab_Cannot_save_launch_configuration_in_a_closed_project__1);
				return false;
			}
		}
		return true;
	}

	/**
	 * validates the stdin file location
	 *
	 * @return true if the stdin file exists, false otherwise
	 */
	private boolean validateStdinFile() {
		if (isInputFile()) {
			int len = fInputFileLocationText.getText().trim().length();
			if (len == 0) {
				setErrorMessage(LaunchConfigurationsMessages.CommonTab_Invalid_stdin_file_location_15);
				return false;
			}
		}
		return true;
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		config.setContainer(null);
		setAttribute(IDebugUIConstants.ATTR_LAUNCH_IN_BACKGROUND, config, true, true);
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		updateConfigFromLocalShared(configuration);
		updateConfigFromFavorites(configuration);

		boolean launchInBackground = fLaunchInBackgroundButton.getSelection();
		setAttribute(IDebugUIConstants.ATTR_LAUNCH_IN_BACKGROUND, configuration, launchInBackground, true);

		boolean terminateDescendants = fTerminateDescendantsButton.getSelection();
		setAttribute(DebugPlugin.ATTR_TERMINATE_DESCENDANTS, configuration, terminateDescendants, true);

		configuration.setAttribute(DebugPlugin.ATTR_FORCE_SYSTEM_CONSOLE_ENCODING,
				forceSystemEncodingButton.getSelection());
		String encoding = null;
		if (!forceSystemEncodingButton.getSelection() && fAltEncodingButton.getSelection()) {
			encoding = fEncodingCombo.getText().trim();
		}
		configuration.setAttribute(DebugPlugin.ATTR_CONSOLE_ENCODING, encoding);
		boolean captureOutput = false;
		if (fConsoleOutput.getSelection()) {
			captureOutput = true;
			configuration.setAttribute(IDebugUIConstants.ATTR_CAPTURE_IN_CONSOLE, (String) null);
		} else {
			configuration.setAttribute(IDebugUIConstants.ATTR_CAPTURE_IN_CONSOLE, false);
		}
		if (fInputFileCheckButton.getSelection()) {
			configuration.setAttribute(IDebugUIConstants.ATTR_CAPTURE_STDIN_FILE, fInputFileLocationText.getText());
		} else {
			configuration.setAttribute(IDebugUIConstants.ATTR_CAPTURE_STDIN_FILE, (String) null);
		}
		if (fFileOutput.getSelection()) {
			captureOutput = true;
			String file = fFileText.getText();
			configuration.setAttribute(IDebugUIConstants.ATTR_CAPTURE_IN_FILE, file);
			if(fAppend.getSelection()) {
				configuration.setAttribute(IDebugUIConstants.ATTR_APPEND_TO_FILE, true);
			} else {
				configuration.setAttribute(IDebugUIConstants.ATTR_APPEND_TO_FILE, (String)null);
			}
		} else {
			configuration.setAttribute(IDebugUIConstants.ATTR_CAPTURE_IN_FILE, (String)null);
		}
		if (fMergeOutput != null) {
			if (fMergeOutput.getSelection()) {
				configuration.setAttribute(DebugPlugin.ATTR_MERGE_OUTPUT, true);
			} else {
				configuration.setAttribute(DebugPlugin.ATTR_MERGE_OUTPUT, (String) null);
			}
		}

		if (!captureOutput) {
			configuration.setAttribute(DebugPlugin.ATTR_CAPTURE_OUTPUT, false);
		} else {
			configuration.setAttribute(DebugPlugin.ATTR_CAPTURE_OUTPUT, (String)null);
		}
	}

	@Override
	public String getName() {
		return LaunchConfigurationsMessages.CommonTab__Common_15;
	}

	/**
	 * @since 3.3
	 */
	@Override
	public String getId() {
		return "org.eclipse.debug.ui.commonTab"; //$NON-NLS-1$
	}

	@Override
	public boolean canSave() {
		return validateLocalShared();
	}

	@Override
	public Image getImage() {
		return DebugUITools.getImage(IInternalDebugUIConstants.IMG_OBJS_COMMON_TAB);
	}

	@Override
	public void activated(ILaunchConfigurationWorkingCopy workingCopy) {}

	@Override
	public void deactivated(ILaunchConfigurationWorkingCopy workingCopy) {}

	/**
	 * @since 3.13
	 */
	@Override
	protected void initializeAttributes() {
		super.initializeAttributes();
		getAttributesLabelsForPrototype().put(DebugPlugin.ATTR_CONSOLE_ENCODING, LaunchConfigurationsMessages.CommonTab_AttributeLabel_ConsoleEncoding);
		getAttributesLabelsForPrototype().put(DebugPlugin.ATTR_CAPTURE_OUTPUT, LaunchConfigurationsMessages.CommonTab_AttributeLabel_CaptureOutput);
		getAttributesLabelsForPrototype().put(IDebugUIConstants.ATTR_CAPTURE_IN_CONSOLE, LaunchConfigurationsMessages.CommonTab_AttributeLabel_CaptureInConsole);
		getAttributesLabelsForPrototype().put(IDebugUIConstants.ATTR_CAPTURE_STDIN_FILE, LaunchConfigurationsMessages.CommonTab_AttributeLabel_CaptureStdInFile);
		getAttributesLabelsForPrototype().put(IDebugUIConstants.ATTR_CAPTURE_IN_FILE, LaunchConfigurationsMessages.CommonTab_AttributeLabel_CaptureInFile);
		getAttributesLabelsForPrototype().put(IDebugUIConstants.ATTR_APPEND_TO_FILE, LaunchConfigurationsMessages.CommonTab_AttributeLabel_AppendToFile);
		getAttributesLabelsForPrototype().put(IDebugUIConstants.ATTR_LAUNCH_IN_BACKGROUND, LaunchConfigurationsMessages.CommonTab_AttributeLabel_LaunchInBackground);
		getAttributesLabelsForPrototype().put(IDebugUIConstants.ATTR_FAVORITE_GROUPS, LaunchConfigurationsMessages.CommonTab_AttributeLabel_FavoriteGroups);
		getAttributesLabelsForPrototype().put(DebugPlugin.ATTR_TERMINATE_DESCENDANTS, LaunchConfigurationsMessages.CommonTab_AttributeLabel_TerminateDescendants);
	}

	/**
	 * Content provider for the favorites table
	 */
	class FavoritesContentProvider implements IStructuredContentProvider {

		@Override
		public Object[] getElements(Object inputElement) {
			ILaunchGroup[] groups = DebugUITools.getLaunchGroups();
			List<ILaunchGroup> possibleGroups = new ArrayList<>();
			ILaunchConfiguration configuration = (ILaunchConfiguration)inputElement;
			for (ILaunchGroup extension : groups) {
				LaunchHistory history = getLaunchConfigurationManager().getLaunchHistory(extension.getIdentifier());
				if (history != null && history.accepts(configuration)) {
					possibleGroups.add(extension);
				}
			}
			return possibleGroups.toArray();
		}

		@Override
		public void dispose() {}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}

	}

	/**
	 * Provides the labels for the favorites table
	 *
	 */
	class FavoritesLabelProvider implements ITableLabelProvider {

		private Map<Object, Image> fImages = new HashMap<>();

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return fImages.computeIfAbsent(element, e -> {
				ImageDescriptor descriptor = ((LaunchGroupExtension) e).getImageDescriptor();
				return descriptor != null ? descriptor.createImage() : null;
			});
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			String label = ((LaunchGroupExtension)element).getLabel();
			return DebugUIPlugin.removeAccelerators(label);
		}

		@Override
		public void addListener(ILabelProviderListener listener) {}

		@Override
		public void dispose() {
			for (Image image : fImages.values()) {
				image.dispose();
			}
		}

		@Override
		public boolean isLabelProperty(Object element, String property) {return false;}

		@Override
		public void removeListener(ILabelProviderListener listener) {}
	}

}
