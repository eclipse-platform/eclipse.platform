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
 *     dakshinamurthy.karra@gmail.com - bug 165371
 *******************************************************************************/
package org.eclipse.ui.externaltools.internal.launchConfigurations;


import org.eclipse.core.externaltools.internal.IExternalToolConstants;
import org.eclipse.core.externaltools.internal.launchConfigurations.ExternalToolsCoreUtil;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.RefreshTab;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.IWorkingSetEditWizard;
import org.eclipse.ui.externaltools.internal.model.BuilderUtils;
import org.eclipse.ui.externaltools.internal.model.ExternalToolsPlugin;
import org.eclipse.ui.externaltools.internal.model.IExternalToolsHelpContextIds;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;

public class ExternalToolsBuilderTab extends AbstractLaunchConfigurationTab {

	protected Button afterClean;
	protected Button fDuringClean;
	protected Button autoBuildButton;
	protected Button manualBuild;
	protected Button workingSetButton;
	protected Button specifyResources;
	protected Button fLaunchInBackgroundButton;
	protected IWorkingSet workingSet;
	protected ILaunchConfiguration fConfiguration;

	private boolean fCreateBuildScheduleComponent= true;

	// Console Output widgets
	private Button fConsoleOutput;
	private Button fFileOutput;
	private Button fFileBrowse;
	private Text fFileText;
	private Button fVariables;
	private Button fAppend;
	private Button fWorkspaceBrowse;

	/**
	 * Constructor
	 */
	public ExternalToolsBuilderTab() {
		setHelpContextId(IExternalToolsHelpContextIds.EXTERNAL_TOOLS_LAUNCH_CONFIGURATION_DIALOG_BUILDER_TAB);
	}

	/**
	 * Constructor
	 * @param createBuildScheduleComponent
	 */
	public ExternalToolsBuilderTab(boolean createBuildScheduleComponent) {
		fCreateBuildScheduleComponent= createBuildScheduleComponent;
		setHelpContextId(IExternalToolsHelpContextIds.EXTERNAL_TOOLS_LAUNCH_CONFIGURATION_DIALOG_BUILDER_TAB);
	}

	protected SelectionListener selectionListener= new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent e) {
			boolean enabled= !fCreateBuildScheduleComponent || autoBuildButton.getSelection() || manualBuild.getSelection();
			workingSetButton.setEnabled(enabled);
			specifyResources.setEnabled(enabled && workingSetButton.getSelection());
			updateLaunchConfigurationDialog();
		}
	};

	@Override
	public void createControl(Composite parent) {
		Composite mainComposite = new Composite(parent, SWT.NONE);
		setControl(mainComposite);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), getHelpContextId());

		GridLayout layout = new GridLayout();
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		layout.numColumns = 2;
		layout.makeColumnsEqualWidth = false;
		mainComposite.setLayout(layout);
		mainComposite.setLayoutData(gridData);
		mainComposite.setFont(parent.getFont());
		createOutputCaptureComponent(mainComposite);
		createLaunchInBackgroundComposite(mainComposite);
		createBuildScheduleComponent(mainComposite);
	}

	/**
	 * Creates the controls needed to edit the launch in background
	 * attribute of an external tool
	 *
	 * @param parent the composite to create the controls in
	 */
	protected void createLaunchInBackgroundComposite(Composite parent) {
		fLaunchInBackgroundButton = createCheckButton(parent, ExternalToolsLaunchConfigurationMessages.ExternalToolsBuilderTab_14);
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		data.horizontalSpan = 2;
		fLaunchInBackgroundButton.setLayoutData(data);
		fLaunchInBackgroundButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateLaunchConfigurationDialog();
			}
		});
	}

	protected void createBuildScheduleComponent(Composite parent) {
		if (fCreateBuildScheduleComponent) {
			Label label= new Label(parent, SWT.NONE);
			label.setText(ExternalToolsLaunchConfigurationMessages.ExternalToolsBuilderTab_Run_this_builder_for__1);
			label.setFont(parent.getFont());
			afterClean= createButton(parent, selectionListener, ExternalToolsLaunchConfigurationMessages.ExternalToolsBuilderTab__Full_builds_2, ExternalToolsLaunchConfigurationMessages.ExternalToolsBuilderTab_Full, 2);
			manualBuild= createButton(parent, selectionListener, ExternalToolsLaunchConfigurationMessages.ExternalToolsBuilderTab__Incremental_builds_4, ExternalToolsLaunchConfigurationMessages.ExternalToolsBuilderTab_Inc, 2);
			autoBuildButton= createButton(parent, selectionListener, ExternalToolsLaunchConfigurationMessages.ExternalToolsBuilderTab__Auto_builds__Not_recommended__6, ExternalToolsLaunchConfigurationMessages.ExternalToolsBuilderTab_Auto, 2);
			fDuringClean= createButton(parent, selectionListener, ExternalToolsLaunchConfigurationMessages.ExternalToolsBuilderTab_0, ExternalToolsLaunchConfigurationMessages.ExternalToolsBuilderTab_1, 2);

			createVerticalSpacer(parent, 2);
		}

		workingSetButton= createButton(parent, selectionListener, ExternalToolsLaunchConfigurationMessages.ExternalToolsBuilderTab_workingSet_label, ExternalToolsLaunchConfigurationMessages.ExternalToolsBuilderTab_workingSet_tooltip, 1);
		specifyResources= createPushButton(parent, ExternalToolsLaunchConfigurationMessages.ExternalToolsBuilderTab_13, null);
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
		specifyResources.setLayoutData(gd);
		specifyResources.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectResources();
			}
		});
		Label label= new Label(parent, SWT.NONE);
		label.setText(ExternalToolsLaunchConfigurationMessages.ExternalToolsBuilderTab_2);
		label.setFont(parent.getFont());
	}

	private void createOutputCaptureComponent(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText(ExternalToolsLaunchConfigurationMessages.ExternalToolsBuilderTab_17);
		GridData gd = new GridData(SWT.FILL, SWT.NONE, true, false);
		gd.horizontalSpan = 2;
		group.setLayoutData(gd);
		GridLayout layout = new GridLayout(5, false);
		group.setLayout(layout);
		group.setFont(parent.getFont());

		fConsoleOutput = createCheckButton(group, ExternalToolsLaunchConfigurationMessages.ExternalToolsBuilderTab_18);
		gd = new GridData(SWT.BEGINNING, SWT.NORMAL, true, false);
		gd.horizontalSpan = 5;
		fConsoleOutput.setLayoutData(gd);

		fConsoleOutput.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateLaunchConfigurationDialog();
			}
		});

		fFileOutput = createCheckButton(group, ExternalToolsLaunchConfigurationMessages.ExternalToolsBuilderTab_19);
		fFileOutput.setLayoutData(new GridData(SWT.BEGINNING, SWT.NORMAL, false, false));

		fFileText = new Text(group, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(SWT.FILL, SWT.NORMAL, true, false);
		gd.horizontalSpan = 4;
		fFileText.setLayoutData(gd);
		fFileText.setFont(parent.getFont());

		Label spacer = new Label(group,SWT.NONE);
		gd = new GridData(SWT.FILL, SWT.NORMAL, true, false);
		gd.horizontalSpan=2;
		spacer.setLayoutData(gd);
		fWorkspaceBrowse = createPushButton(group, ExternalToolsLaunchConfigurationMessages.ExternalToolsBuilderTab_20, null);
		fFileBrowse = createPushButton(group, ExternalToolsLaunchConfigurationMessages.ExternalToolsBuilderTab_21, null);
		fVariables = createPushButton(group, ExternalToolsLaunchConfigurationMessages.ExternalToolsBuilderTab_22, null);

		spacer = new Label(group,SWT.NONE);
		spacer.setLayoutData(new GridData(SWT.FILL, SWT.NORMAL, false, false));
		fAppend = createCheckButton(group, ExternalToolsLaunchConfigurationMessages.ExternalToolsBuilderTab_23);
		gd = new GridData(SWT.LEFT, SWT.TOP, true, false);
		gd.horizontalSpan = 4;
		fAppend.setLayoutData(gd);

		fFileOutput.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean enabled = fFileOutput.getSelection();
				fFileText.setEnabled(enabled);
				fFileBrowse.setEnabled(enabled);
				fWorkspaceBrowse.setEnabled(enabled);
				fVariables.setEnabled(enabled);
				fAppend.setEnabled(enabled);
				updateLaunchConfigurationDialog();
			}
		});

		fAppend.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateLaunchConfigurationDialog();
			}
		});

		fWorkspaceBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getShell(), new WorkbenchLabelProvider(), new WorkbenchContentProvider());
				dialog.setTitle(ExternalToolsLaunchConfigurationMessages.ExternalToolsBuilderTab_24);
				dialog.setMessage(ExternalToolsLaunchConfigurationMessages.ExternalToolsBuilderTab_25);
				dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
				dialog.setComparator(new ResourceComparator(ResourceComparator.NAME));
				int buttonId = dialog.open();
				if (buttonId == IDialogConstants.OK_ID) {
					IResource resource = (IResource) dialog.getFirstResult();
					String arg = resource.getFullPath().toString();
					String fileLoc = VariablesPlugin.getDefault().getStringVariableManager().generateVariableExpression("workspace_loc", arg); //$NON-NLS-1$
					fFileText.setText(fileLoc);
				}
			}
		});

		fFileBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String filePath = fFileText.getText();
				FileDialog dialog = new FileDialog(getShell(), SWT.SAVE | SWT.SHEET);

				filePath = dialog.open();
				if (filePath != null) {
					fFileText.setText(filePath);
				}
			}
		});

		fFileText.addModifyListener(e -> updateLaunchConfigurationDialog());

		fVariables.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				StringVariableSelectionDialog dialog = new StringVariableSelectionDialog(getShell());
				dialog.open();
				String variable = dialog.getVariableExpression();
				if (variable != null) {
					fFileText.insert(variable);
				}
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
	}

	/*
	 * Creates a check button in the given composite with the given text
	 */
	protected Button createButton(Composite parent, SelectionListener listener, String text, String tooltipText, int columns) {
		Button button= createCheckButton(parent, text);
		button.setToolTipText(tooltipText);
		button.addSelectionListener(listener);
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = columns;
		button.setLayoutData(gd);
		return button;
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		StringBuilder buffer= new StringBuilder(IExternalToolConstants.BUILD_TYPE_FULL);
		buffer.append(',');
		buffer.append(IExternalToolConstants.BUILD_TYPE_INCREMENTAL);
		buffer.append(',');
		configuration.setAttribute(IExternalToolConstants.ATTR_RUN_BUILD_KINDS, buffer.toString());
		configuration.setAttribute(IDebugUIConstants.ATTR_LAUNCH_IN_BACKGROUND, false);
		configuration.setAttribute(IExternalToolConstants.ATTR_TRIGGERS_CONFIGURED, true);
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		fConfiguration= configuration;
		if (fCreateBuildScheduleComponent) {
			afterClean.setSelection(false);
			manualBuild.setSelection(false);
			autoBuildButton.setSelection(false);
			fDuringClean.setSelection(false);
		}

		String buildKindString= null;
		String buildScope= null;
		try {
			buildKindString= configuration.getAttribute(IExternalToolConstants.ATTR_RUN_BUILD_KINDS, IExternalToolConstants.EMPTY_STRING);
			buildScope= configuration.getAttribute(IExternalToolConstants.ATTR_BUILDER_SCOPE, (String)null);
		} catch (CoreException e) {
		}

		workingSetButton.setSelection(buildScope != null);
		workingSetButton.setEnabled(buildScope != null);

		if (buildScope != null) {
			workingSet = RefreshTab.getWorkingSet(buildScope);
		}

		if (fCreateBuildScheduleComponent) {
			for (int buildType : BuilderUtils.buildTypesToArray(buildKindString)) {
				switch (buildType) {
					case IncrementalProjectBuilder.FULL_BUILD:
						afterClean.setSelection(true);
						break;
					case IncrementalProjectBuilder.INCREMENTAL_BUILD:
						manualBuild.setSelection(true);
						break;
					case IncrementalProjectBuilder.AUTO_BUILD:
						autoBuildButton.setSelection(true);
						break;
					case IncrementalProjectBuilder.CLEAN_BUILD:
						fDuringClean.setSelection(true);
						break;
					default:
						break;
				}
			}
		}

		boolean enabled= true;
		if (fCreateBuildScheduleComponent) {
			enabled= autoBuildButton.getSelection() || manualBuild.getSelection();
		}
		workingSetButton.setEnabled(enabled);
		specifyResources.setEnabled(enabled && workingSetButton.getSelection());
		updateRunInBackground(configuration);
		updateConsoleOutput(configuration);
	}

	protected void updateRunInBackground(ILaunchConfiguration configuration) {
		fLaunchInBackgroundButton.setSelection(ExternalToolsCoreUtil.isAsynchronousBuild(configuration));
	}

	private void updateConsoleOutput(ILaunchConfiguration configuration) {
		boolean outputToConsole = true;
		String outputFile = null;
		boolean append = false;

		try {
			outputToConsole = configuration.getAttribute(IDebugUIConstants.ATTR_CAPTURE_IN_CONSOLE, true);
			outputFile = configuration.getAttribute(IDebugUIConstants.ATTR_CAPTURE_IN_FILE, (String)null);
			append = configuration.getAttribute(IDebugUIConstants.ATTR_APPEND_TO_FILE, false);
		} catch (CoreException e) {
		}

		fConsoleOutput.setSelection(outputToConsole);
		fAppend.setSelection(append);
		boolean haveOutputFile= outputFile != null;
		if (haveOutputFile) {
			fFileText.setText(outputFile);
		}
		fFileOutput.setSelection(haveOutputFile);
		fFileText.setEnabled(haveOutputFile);
		fFileBrowse.setEnabled(haveOutputFile);
		fWorkspaceBrowse.setEnabled(haveOutputFile);
		fVariables.setEnabled(haveOutputFile);
		fAppend.setEnabled(haveOutputFile);
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		if (fCreateBuildScheduleComponent) {
			StringBuilder buffer= new StringBuilder();
			if (afterClean.getSelection()) {
				buffer.append(IExternalToolConstants.BUILD_TYPE_FULL).append(',');
			}
			if (manualBuild.getSelection()){
				buffer.append(IExternalToolConstants.BUILD_TYPE_INCREMENTAL).append(',');
			}
			if (autoBuildButton.getSelection()) {
				buffer.append(IExternalToolConstants.BUILD_TYPE_AUTO).append(',');
			}

			if (fDuringClean.getSelection()) {
				buffer.append(IExternalToolConstants.BUILD_TYPE_CLEAN);
			}
			configuration.setAttribute(IExternalToolConstants.ATTR_RUN_BUILD_KINDS, buffer.toString());
		}
		if (workingSetButton.getSelection()) {
			String scope = RefreshTab.getRefreshAttribute(workingSet);
			configuration.setAttribute(IExternalToolConstants.ATTR_BUILDER_SCOPE, scope);
		} else {
			configuration.setAttribute(IExternalToolConstants.ATTR_BUILDER_SCOPE, (String)null);
		}
		configuration.setAttribute(IDebugUIConstants.ATTR_LAUNCH_IN_BACKGROUND, fLaunchInBackgroundButton.getSelection());

		boolean captureOutput = false;
		if (fConsoleOutput.getSelection()) {
			captureOutput = true;
			configuration.setAttribute(IDebugUIConstants.ATTR_CAPTURE_IN_CONSOLE, (String)null);
		} else {
			configuration.setAttribute(IDebugUIConstants.ATTR_CAPTURE_IN_CONSOLE, false);
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

		if (!captureOutput) {
			configuration.setAttribute(DebugPlugin.ATTR_CAPTURE_OUTPUT, false);
		} else {
			configuration.setAttribute(DebugPlugin.ATTR_CAPTURE_OUTPUT, (String)null);
		}
	}

	@Override
	public String getName() {
		return ExternalToolsLaunchConfigurationMessages.ExternalToolsBuilderTab_Build_Options_9;
	}

	@Override
	public Image getImage() {
		return PlatformUI.getWorkbench().getSharedImages().getImage(IDE.SharedImages.IMG_OBJ_PROJECT);
	}

	@Override
	public boolean isValid(ILaunchConfiguration launchConfig) {
		setErrorMessage(null);
		setMessage(null);
		if (fCreateBuildScheduleComponent) {
			boolean buildKindSelected= afterClean.getSelection() || manualBuild.getSelection() || autoBuildButton.getSelection() || fDuringClean.getSelection();
			if (!buildKindSelected) {
				setErrorMessage(ExternalToolsLaunchConfigurationMessages.ExternalToolsBuilderTab_buildKindError);
				return false;
			}
		}
		if (workingSetButton.getSelection() && (workingSet == null || workingSet.getElements().length == 0)) {
			setErrorMessage(ExternalToolsLaunchConfigurationMessages.ExternalToolsBuilderTab_16);
			return false;
		}

		return validateRedirectFile();
	}

	@Override
	public boolean canSave() {
		return isValid(null);
	}

	/**
	 * Prompts the user to select the working set that triggers the build.
	 */
	private void selectResources() {
		IWorkingSetManager workingSetManager= PlatformUI.getWorkbench().getWorkingSetManager();

		if (workingSet == null){
			workingSet = workingSetManager.createWorkingSet(ExternalToolsLaunchConfigurationMessages.ExternalToolsBuilderTab_15, new IAdaptable[0]);
		}
		IWorkingSetEditWizard wizard= workingSetManager.createWorkingSetEditWizard(workingSet);
		WizardDialog dialog = new WizardDialog(ExternalToolsPlugin.getStandardDisplay().getActiveShell(), wizard);
		dialog.create();

		if (dialog.open() == Window.CANCEL) {
			return;
		}
		workingSet = wizard.getSelection();
		updateLaunchConfigurationDialog();
	}

	@Override
	public void activated(ILaunchConfigurationWorkingCopy workingCopy) {
		// do nothing on activation
	}

	@Override
	public void deactivated(ILaunchConfigurationWorkingCopy workingCopy) {
		// do nothing on deactivation
	}

	private boolean validateRedirectFile() {
		if(fFileOutput.getSelection()) {
			int len = fFileText.getText().trim().length();
			if (len == 0) {
				setErrorMessage(ExternalToolsLaunchConfigurationMessages.ExternalToolsBuilderTab_26);
				return false;
			}
		}
		return true;
	}
}