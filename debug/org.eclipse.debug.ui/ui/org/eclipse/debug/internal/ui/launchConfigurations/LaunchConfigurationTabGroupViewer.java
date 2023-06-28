/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
 *     Mohamed Hussein (Mentor Graphics) - Added getWarningMessage (Bug 386673)
 *     Robert Roth - Used consistent apply button location (Bug 432832)
 *     Lucas Bullen (Red Hat Inc.) - Added export configuration message (Bug 518652)
 *     Axel Richard (Obeo) - Bug 41353 - Launch configurations prototypes
 *******************************************************************************/
package org.eclipse.debug.internal.ui.launchConfigurations;


import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchDelegate;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.core.LaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.core.LaunchManager;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchConfigurationTab2;
import org.eclipse.debug.ui.ILaunchConfigurationTabGroup;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * A viewer that displays tabs for a launch configuration, with apply and revert
 * buttons.
 */
public class LaunchConfigurationTabGroupViewer {

	/**
	 * Containing launch dialog
	 */
	private ILaunchConfigurationDialog fDialog;

	/**
	 * The launch configuration (original) being edited
	 */
	private ILaunchConfiguration fOriginal;

	/**
	 * The working copy of the original
	 */
	private ILaunchConfigurationWorkingCopy fWorkingCopy;

	/**
	 * The old configuration
	 */
	private boolean fRefreshTabs = false;

	/**
	 * This view's control, which contains a composite area of controls
	 */
	private Composite fViewerControl;

	/**
	 * Name text widget
	 */
	private Text fNameWidget;

	/**
	 * Composite containing the launch config tab widgets
	 */
	private Composite fGroupComposite;

	/**
	 * Tab folder
	 */
	private CTabFolder fTabFolder;

	/**
	 * The current tab group being displayed
	 */
	private ILaunchConfigurationTabGroup fTabGroup;

	/**
	 * Index of the active tab
	 */
	private int fCurrentTabIndex = -1;

	/**
	 * Apply & Revert buttons
	 */
	private Button fApplyButton;
	private Button fRevertButton;

	Composite buttonComp;
	private Button fShowCommandLineButton;

	/**
	 * Whether tabs are currently being disposed or initialized
	 */
	private boolean fDisposingTabs = false;
	private boolean fInitializingTabs = false;

	/**
	 * The description of the currently selected launch configuration or
	 * launch configuration type or <code>null</code> if none.
	 */
	private String fDescription = null;

	/**
	 * A place holder for switching between the tabs for a config and the getting started tab
	 * @since 3.2
	 */
	private Composite fTabPlaceHolder = null;

	/**
	 * A link to allow users to select a valid set of launch options for the specified mode
	 * @since 3.3
	 */
	private Link fOptionsLink = null;

	/**
	 * A label to indicate that the user needs to select an a launcher.
	 * @since 3.5
	 */
	private Label fOptionsErrorLabel = null;

	/**
	 * A new composite replacing the perspectives tab
	 * @since 3.2
	 */
	private Composite fGettingStarted = null;

	private ViewForm fViewform;

	/**
	 * Job to update the dialog after a delay.
	 */
	private Job fRefreshJob;

	/**
	 * Constructs a viewer in the given composite, contained by the given
	 * launch configuration dialog.
	 *
	 * @param parent composite containing this viewer
	 * @param dialog containing launch configuration dialog
	 */
	public LaunchConfigurationTabGroupViewer(Composite parent, ILaunchConfigurationDialog dialog) {
		super();
		fDialog = dialog;
		createControl(parent);
	}

	/**
	 * Cleanup
	 */
	public void dispose() {
		disposeTabGroup();
	}

	/**
	 * Dispose the active tab group, if any.
	 */
	protected void disposeTabGroup() {
		if (fTabGroup != null) {
			fTabGroup.dispose();
			fTabGroup = null;
		}
	}

	/**
	 * Creates this viewer's control This area displays the name of the launch
	 * configuration currently being edited, as well as a tab folder of tabs
	 * that are applicable to the launch configuration.
	 * @param parent the parent {@link Composite}
	 *
	 */
	private void createControl(Composite parent) {
		fViewerControl = parent;
		fViewform = new ViewForm(parent, SWT.FLAT | SWT.BORDER);
		GridLayout layout = new GridLayout(1, false);
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		fViewform.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		fViewform.setLayoutData(gd);
		fViewform.setTopLeft(null);

		Composite mainComp = SWTFactory.createComposite(fViewform, fViewform.getFont(), 1, 1, 1, 0, 0);
		fViewform.setContent(mainComp);

		fTabPlaceHolder = SWTFactory.createComposite(mainComp, 1, 1, GridData.FILL_BOTH);
		fTabPlaceHolder.setLayout(new StackLayout());
		fGettingStarted = SWTFactory.createComposite(fTabPlaceHolder, 1, 1, GridData.FILL_BOTH);

		createGettingStarted(fGettingStarted);

		fGroupComposite = SWTFactory.createComposite(fTabPlaceHolder, fTabPlaceHolder.getFont(), 2, 2, GridData.FILL_BOTH, 5, 5);
		SWTFactory.createLabel(fGroupComposite, LaunchConfigurationsMessages.LaunchConfigurationDialog__Name__16, 1);

		fNameWidget = new Text(fGroupComposite, SWT.SINGLE | SWT.BORDER);
		fNameWidget.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fNameWidget.addModifyListener(e -> {
			if (!fInitializingTabs) {
				handleNameModified();
			}
		}
			);

		createTabFolder(fGroupComposite);

		Composite blComp = SWTFactory.createComposite(mainComp, mainComp.getFont(), 2, 1, GridData.FILL_HORIZONTAL);
		Composite linkComp = SWTFactory.createComposite(blComp, blComp.getFont(), 2, 1, GridData.FILL_HORIZONTAL);

	//a link for launch options
		fOptionsErrorLabel = new Label(linkComp, SWT.NONE);
		gd = new GridData();
		fOptionsErrorLabel.setLayoutData(gd);

		fOptionsLink = new Link(linkComp, SWT.WRAP);
		fOptionsLink.setFont(linkComp.getFont());
		gd = new GridData(SWT.LEFT);
		gd.grabExcessHorizontalSpace = true;
		fOptionsLink.setLayoutData(gd);
		fOptionsLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				//collect the options available
				try {
					if(!canLaunchWithModes()) {
						SelectLaunchModesDialog sld = new SelectLaunchModesDialog(getShell(),
								getLaunchConfigurationDialog().getMode(), getWorkingCopy());
						if(sld.open() == IDialogConstants.OK_ID) {
							//set the options to the config
							Object[] res = sld.getResult();
							if(res != null) {
								@SuppressWarnings("unchecked")
								Set<String> modes = (Set<String>) res[0];
								modes.remove(getLaunchConfigurationDialog().getMode());
								ILaunchConfigurationWorkingCopy wc = getWorkingCopy();
								wc.setModes(modes);
								refreshStatus();
							}
						}
					}
					else if(hasMultipleDelegates()) {
						SelectLaunchersDialog sldd = new SelectLaunchersDialog(getShell(),
								getWorkingCopy().getType().getDelegates(getCurrentModeSet()),
								getWorkingCopy(),
								getLaunchConfigurationDialog().getMode());
						if(sldd.open() == IDialogConstants.OK_ID) {
							displayInstanceTabs(true);
							refreshStatus();
						}
					}
				} catch (CoreException ex) {}
			}
		});
		fOptionsLink.setVisible(false);

		buttonComp = SWTFactory.createComposite(blComp, 3, 1, GridData.HORIZONTAL_ALIGN_END);

		fShowCommandLineButton = SWTFactory.createPushButton(buttonComp,
				LaunchConfigurationsMessages.LaunchConfigurationDialog_ShowCommandLine, null,
				GridData.HORIZONTAL_ALIGN_BEGINNING);
		fShowCommandLineButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				handleShowCommandLinePressed();
			}
		});

		fRevertButton = SWTFactory.createPushButton(buttonComp,
				LaunchConfigurationsMessages.LaunchConfigurationDialog_Revert_2, null, GridData.HORIZONTAL_ALIGN_END);
		fRevertButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				handleRevertPressed();
			}
		});

		fApplyButton = SWTFactory.createPushButton(buttonComp, LaunchConfigurationsMessages.LaunchConfigurationDialog__Apply_17, null,GridData.HORIZONTAL_ALIGN_END);
		fApplyButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				handleApplyPressed();
				ILaunchConfigurationTab[] tabs = getTabs();
				if (tabs != null) {
					for (ILaunchConfigurationTab tab : tabs) {
						tab.postApply();
					}
				}
			}
		});
		Dialog.applyDialogFont(parent);
	}

	/**
	 * Creates some help text for the tab group launch types
	 * @param parent the parent composite
	 * @since 3.2
	 */
	private void createGettingStarted(Composite parent) {
		Font font = parent.getFont();
		GridData gd = null;
		int width = parent.getBounds().width - 30;
		SWTFactory.createWrapLabel(parent, LaunchConfigurationsMessages.LaunchConfigurationTabGroupViewer_1, 1, width);
		SWTFactory.createWrapCLabel(parent, LaunchConfigurationsMessages.LaunchConfigurationTabGroupViewer_2, DebugUITools.getImage(IInternalDebugUIConstants.IMG_ELCL_NEW_CONFIG), 1, width);
		SWTFactory.createWrapCLabel(parent, LaunchConfigurationsMessages.LaunchConfigurationTabGroupViewer_9, DebugUITools.getImage(IInternalDebugUIConstants.IMG_ELCL_NEW_PROTO), 1, width);
		SWTFactory.createWrapCLabel(parent, LaunchConfigurationsMessages.LaunchConfigurationTabGroupViewer_7, DebugUITools.getImage(IInternalDebugUIConstants.IMG_ELCL_EXPORT_CONFIG), 1, width);
		SWTFactory.createWrapCLabel(parent, LaunchConfigurationsMessages.LaunchConfigurationTabGroupViewer_6, DebugUITools.getImage(IInternalDebugUIConstants.IMG_ELCL_DUPLICATE_CONFIG), 1, width);
		SWTFactory.createWrapCLabel(parent, LaunchConfigurationsMessages.LaunchConfigurationTabGroupViewer_4, DebugUITools.getImage(IInternalDebugUIConstants.IMG_ELCL_DELETE_CONFIG), 1, width);
		SWTFactory.createWrapCLabel(parent, LaunchConfigurationsMessages.LaunchConfigurationTabGroupViewer_8, DebugUITools.getImage(IInternalDebugUIConstants.IMG_ELCL_FILTER_CONFIGS), 1, width);
		SWTFactory.createWrapCLabel(parent, LaunchConfigurationsMessages.LaunchConfigurationTabGroupViewer_10, DebugUITools.getImage(IInternalDebugUIConstants.IMG_ELCL_LINK_PROTO), 1, width);
		SWTFactory.createWrapCLabel(parent, LaunchConfigurationsMessages.LaunchConfigurationTabGroupViewer_11, DebugUITools.getImage(IInternalDebugUIConstants.IMG_ELCL_UNLINK_PROTO), 1, width);
		SWTFactory.createWrapCLabel(parent, LaunchConfigurationsMessages.LaunchConfigurationTabGroupViewer_12, DebugUITools.getImage(IInternalDebugUIConstants.IMG_ELCL_RESET_PROTO), 1, width);
		SWTFactory.createHorizontalSpacer(parent, 2);
		SWTFactory.createLabel(parent, LaunchConfigurationsMessages.LaunchConfigurationTabGroupViewer_3, width);

		SWTFactory.createHorizontalSpacer(parent, 2);
		Link link = new Link(parent, SWT.LEFT | SWT.WRAP);
		link.setText(LaunchConfigurationsMessages.LaunchConfigurationTabGroupViewer_5);
		link.setFont(font);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = width;
		link.setLayoutData(gd);
		link.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				SWTFactory.showPreferencePage("org.eclipse.debug.ui.PerspectivePreferencePage"); //$NON-NLS-1$
			}
		});
	}

	/**
	 * Creates the tab folder for displaying config instances
	 * @param parent the parent {@link Composite}
	 */
	private void createTabFolder(Composite parent) {
		if (fTabFolder == null) {
			ColorRegistry reg = JFaceResources.getColorRegistry();
			Color c1 = reg.get("org.eclipse.ui.workbench.ACTIVE_TAB_BG_START"), //$NON-NLS-1$
				  c2 = reg.get("org.eclipse.ui.workbench.ACTIVE_TAB_BG_END"); //$NON-NLS-1$
			fTabFolder = new CTabFolder(parent, SWT.NO_REDRAW_RESIZE | SWT.FLAT);
			fTabFolder.setSelectionBackground(new Color[] {c1, c2},	new int[] {100}, true);
			fTabFolder.setSelectionForeground(reg.get("org.eclipse.ui.workbench.ACTIVE_TAB_TEXT_COLOR")); //$NON-NLS-1$
			fTabFolder.setBorderVisible(true);
			GridData gd = new GridData(GridData.FILL_BOTH);
			gd.horizontalSpan = 2;
			fTabFolder.setLayoutData(gd);
			fTabFolder.setFont(parent.getFont());
			fTabFolder.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent event) {
					int selectedIndex = fTabFolder.indexOf((CTabItem) event.item);
					if (!fInitializingTabs) {
						handleTabChange(selectedIndex);
					}

					Display display = fTabFolder.getDisplay();
					display.addListener(SWT.PostEvent, new Listener() {
						@Override
						public void handleEvent(Event newEvent) {
							if (newEvent.detail == SWT.Selection) { //
								display.removeListener(SWT.PostEvent, this);
								if (fRefreshTabs) {
									refreshTabs(true);
									fTabFolder.setSelection(selectedIndex);
									refreshStatus();
								}
							}
						}
					});

				}
			});
		}
	}

	private void handleTabChange(int newPageIndex) {
		if (fCurrentTabIndex == newPageIndex) {
			return;
		}
		if (isDirty() && !fInitializingTabs) {
			ILaunchConfigurationTab[] tabs = getTabs();
			if (fCurrentTabIndex != -1) {
				ILaunchConfigurationTab tab = tabs[fCurrentTabIndex];
				fTabFolder.setSelection(fCurrentTabIndex);
				if (tab.OkToLeaveTab()) {
					fTabFolder.setSelection(newPageIndex);
					handleTabSelected();
					refresh();
				}
			}
		} else {
			handleTabSelected();
			refresh();
		}

	}

	/**
	 * Returns the apply button
	 * @return the 'Apply' button
	 */
	protected Button getApplyButton() {
		return fApplyButton;
	}

	/**
	 * Returns the revert button
	 * @return the 'Revert' button
	 */
	protected Button getRevertButton() {
		return fRevertButton;
	}

	/**
	 * Sets the current name
	 * @param name the new name to set
	 */
	public void setName(String name) {
		if (getWorkingCopy() != null) {
			if (name == null) {
				fNameWidget.setText(IInternalDebugCoreConstants.EMPTY_STRING);
			}
			else {
				fNameWidget.setText(name.trim());
			}
			refreshStatus();
		}
	}

	/**
	 * @return the backing viewer control
	 */
	public Control getControl() {
		return fViewerControl;
	}

	/**
	 * Returns the shell this viewer is contained in.
	 * @return the current dialog shell
	 */
	private Shell getShell() {
		return getControl().getShell();
	}

	/**
	 * Returns the current input to the viewer. Input will
	 * be one of {@link ILaunchConfiguration} or {@link ILaunchConfigurationType}
	 *
	 * @return returns the current input
	 */
	public Object getInput() {
		return getConfiguration();
	}

	public void refresh() {
		if (fInitializingTabs) {
			return;
		}
		ILaunchConfigurationTab[] tabs = getTabs();
		if (tabs != null) {
			// update the working copy from the active tab
			boolean newwc = !getWorkingCopy().isDirty();
			ILaunchConfigurationTab tab = getActiveTab();
			if (tab != null) {
				tab.performApply(getWorkingCopy());
			}
			if((fOriginal instanceof ILaunchConfigurationWorkingCopy) && newwc) {
				try {
					ILaunchConfigurationWorkingCopy copy = getWorkingCopy();
					if(copy != null) {
						copy.doSave();
					}
				}
				catch (CoreException e) {DebugUIPlugin.log(e);}
			}
			updateButtons();
			// update error ticks
			CTabItem item = null;
			boolean error = false;
			Image image = null;
			for (int i = 0; i < tabs.length; i++) {
				item = fTabFolder.getItem(i);
				image = tabs[i].getImage();
				item.setImage(image);
				if(!tabs[i].isValid(getWorkingCopy())) {
					error = tabs[i].getErrorMessage() != null;
					if(error) {
						item.setImage(DebugUIPlugin.getDefault().getLaunchConfigurationManager().getErrorTabImage(tabs[i]));
					}
				}
			}
			showLink();
			getLaunchConfigurationDialog().updateMessage();
		}
	}

	/**
	 * Shows the link for either multiple launch delegates or bad launch mode combinations
	 *
	 * @since 3.3
	 */
	private void showLink() {
		String text = null;
		if(!canLaunchWithModes()) {
			text = LaunchConfigurationsMessages.LaunchConfigurationTabGroupViewer_13;
		}
		else if(hasMultipleDelegates()) {
			ILaunchDelegate delegate = getPreferredDelegate();
			if(delegate != null) {
				String name = delegate.getName();
				if(name == null) {
					text = LaunchConfigurationsMessages.LaunchConfigurationTabGroupViewer_15;
				}
				else {
					text = MessageFormat.format(LaunchConfigurationsMessages.LaunchConfigurationTabGroupViewer_16, new Object[] { name });
				}
			}
			else {
				text = LaunchConfigurationsMessages.LaunchConfigurationTabGroupViewer_17;
			}
		}
		if(text != null) {
			fOptionsLink.setText(text);
		}
		fOptionsLink.setVisible(!canLaunchWithModes() || hasMultipleDelegates());
		if (hasDuplicateDelegates()) {
			fOptionsErrorLabel.setImage(JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_ERROR));
		} else {
			fOptionsErrorLabel.setImage(null);
		}
		fOptionsLink.requestLayout();
		fOptionsErrorLabel.requestLayout();
	}

	/**
	 * Returns the preferred launch delegate for the current launch configuration and mode set
	 * @return the preferred launch delegate
	 *
	 * @since 3.3
	 */
	protected ILaunchDelegate getPreferredDelegate() {
		ILaunchDelegate preferred = null;
		ILaunchConfigurationWorkingCopy config = getWorkingCopy();
		if(config != null) {
			try {
				Set<String> modes = getCurrentModeSet();
				preferred = config.getPreferredDelegate(modes);
				if(preferred == null) {
					preferred = config.getType().getPreferredDelegate(modes);
				}
			}
			catch(CoreException ce) {DebugUIPlugin.log(ce);}
		}
		return preferred;
	}

	/**
	 * Returns the listing of modes for the current config
	 * @return the listing of modes for the current config
	 * @since 3.3
	 */
	private Set<String> getCurrentModeSet() {
		Set<String> set = new HashSet<>();
		ILaunchConfigurationWorkingCopy config = getWorkingCopy();
		if(config != null) {
			try {
				set.addAll(config.getModes());
				set.add(getLaunchConfigurationDialog().getMode());
			}
			catch(CoreException ce) {DebugUIPlugin.log(ce);}
		}
		return set;
	}

	/**
	 * @return returns the configuration input
	 *
	 * @since 3.6
	 */
	ILaunchConfiguration getConfiguration() {
		if(fOriginal == null) {
			return getWorkingCopy();
		}
		return fOriginal;
	}

	/**
	 * updates the button states
	 */
	private void updateButtons() {
		boolean isDirty = isDirty();
		fApplyButton.setEnabled(isDirty && canSave());
		fRevertButton.setEnabled(isDirty);
		fShowCommandLineButton.setEnabled(canLaunch());
	}

	public void setInput(final Object input) {
		if(DebugUIPlugin.getStandardDisplay().getThread().equals(Thread.currentThread())) {
			setInput0(input);
		}
		else {
			DebugUIPlugin.getStandardDisplay().syncExec(() -> setInput0(input));
		}

	}
	/**
	 * Sets the input to the tab group viewer
	 * @param input the new input
	 * @since 3.3
	 */
	private void setInput0(Object input) {
		if (input == null) {
			if (getConfiguration() == null) {
				return;
			}
			inputChanged(input);
		} else {
			if (!input.equals(getConfiguration())) {
				inputChanged(input);
			}
		}
	}
	/*
	 * Refresh tabs for same input
	 */

	public void refreshTabs(boolean refresh) {
		if (DebugUIPlugin.getStandardDisplay().getThread().equals(Thread.currentThread())) {
			refreshTabs0(refresh);
		} else {
			DebugUIPlugin.getStandardDisplay().syncExec(() -> refreshTabs0(refresh));
		}
	}

	/**
	 * The input has changed to the given object, possibly <code>null</code>.
	 *
	 * @param input the new input, possibly <code>null</code>
	 */
	protected void refreshTabs0(boolean refreshTabs) {
		final boolean refresh = refreshTabs;
		Runnable r = () -> {
			fViewform.setRedraw(false);
			fRefreshTabs = !refresh;
			displayInstanceTabs(refresh);
			refreshStatus();
			fViewform.setRedraw(true);
		};
		BusyIndicator.showWhile(getShell().getDisplay(), r);
	}

	/**
	 * The input has changed to the given object, possibly <code>null</code>.
	 *
	 * @param input the new input, possibly <code>null</code>
	 */
	protected void inputChanged(Object input) {
		final Object finput = input;
		Runnable r = () -> {
			try {
				fViewform.setRedraw(false);
				if (finput instanceof ILaunchConfiguration) {
					ILaunchConfiguration configuration = (ILaunchConfiguration) finput;
					boolean refreshTabs = true;
					if (fWorkingCopy != null
							&& fWorkingCopy.getOriginal().equals(configuration.getWorkingCopy().getOriginal())) {
						refreshTabs = false;
					}
					fOriginal = configuration;
					fWorkingCopy = configuration.getWorkingCopy();
					// Need to refresh all the time as tabs might have changed
					displayInstanceTabs(refreshTabs);
				} else if (finput instanceof ILaunchConfigurationType) {
					fDescription = getDescription((ILaunchConfigurationType) finput);
					setNoInput();
				} else {
					setNoInput();
				}
			} catch (CoreException ce) {
				errorDialog(ce);
				setNoInput();
			} finally {
				refreshStatus();
				fViewform.setRedraw(true);
			}
		};
		BusyIndicator.showWhile(getShell().getDisplay(), r);
	}

	/**
	 * Sets the tab group viewer to have no input, this is the case when null is passed as an input type
	 * Setting no input is equivalent to resetting all items, clearing any messages and showing the 'getting started' pane
	 * @since 3.2
	 */
	private void setNoInput() {
		fOriginal = null;
		fWorkingCopy = null;
		disposeExistingTabs();
		updateButtons();
		updateVisibleControls(false);
		updateShowCommandLineVisibility(false);
		ILaunchConfigurationDialog lcd = getLaunchConfigurationDialog();
		if(lcd instanceof LaunchConfigurationsDialog) {
			if(((LaunchConfigurationsDialog)lcd).isTreeSelectionEmpty()) {
				fDescription = IInternalDebugCoreConstants.EMPTY_STRING;
			}
		}
	}

	/**
	 * Returns if the two configurations are using the same <code>ILaunchDelegate</code> or not
	 * @param config1 the config to compare to
	 * @param config2 the config to compare
	 * @return true if the configurations are using the same <code>ILaunchDelegate</code> or false if they are not
	 * @since 3.3
	 */
	protected boolean delegatesEqual(ILaunchConfiguration config1, ILaunchConfiguration config2) {
		try {
			if(config1 == null || config2 == null) {
				return false;
			}
			if (config1.getType().equals(config2.getType())) {
				Set<String> modes = getCurrentModeSet();
				ILaunchDelegate d1 = config1.getPreferredDelegate(modes);
				if(d1 == null) {
					d1 = config1.getType().getPreferredDelegate(modes);
				}
				ILaunchDelegate d2 = config2.getPreferredDelegate(modes);
				if(d2 == null) {
					d2 = config2.getType().getPreferredDelegate(modes);
				}
				if (d1 == null) {
					return d2 == null;
				} else {
					return d1.equals(d2);
				}
			}
		}
		catch(CoreException ce) {DebugUIPlugin.log(ce);}
		return false;
	}

	/**
	 * Updates the visibility of controls based on the status provided
	 * @param visible the visibility status to be applied to the controls
	 */
	private void updateVisibleControls(boolean visible) {
		fApplyButton.setVisible(visible);
		fRevertButton.setVisible(visible);
		fOptionsLink.setVisible(visible);
		StackLayout layout = (StackLayout)fTabPlaceHolder.getLayout();
		if(visible) {
			layout.topControl = fGroupComposite;
		}
		else {
			layout.topControl = fGettingStarted;
		}
		layout.topControl.requestLayout();
	}

	private void updateShowCommandLineVisibility(boolean visible) {
		if (fShowCommandLineButton != null) {
			fShowCommandLineButton.setVisible(visible);
		}
	}
	/**
	 * sets the current widget focus to the 'Name' widget
	 */
	protected void setFocusOnName() {
		fNameWidget.setFocus();
	}

	/**
	 * Displays tabs for the current working copy
	 * @param redrawTabs if the tabs should be redrawn
	 */
	protected void displayInstanceTabs(boolean redrawTabs) {
		// Turn on initializing flag to ignore message updates
		fInitializingTabs = true;
		ILaunchConfigurationType type = null;
		try {
			type = getWorkingCopy().getType();
		}
		catch (CoreException e) {
			errorDialog(e);
			fInitializingTabs = false;
			return;
		}
		if(redrawTabs) {
			showInstanceTabsFor(type);
		}
		// show the name area
		updateVisibleControls(true);
		if (type.supportsCommandLine()) {
			updateShowCommandLineVisibility(true);
		}
		else {
			updateShowCommandLineVisibility(false);
		}

		// Retrieve the current tab group.  If there is none, clean up and leave
		ILaunchConfigurationTabGroup tabGroup = getTabGroup();
		if (tabGroup == null) {
			IStatus status = new Status(IStatus.ERROR, DebugUIPlugin.getUniqueIdentifier(), 0, MessageFormat.format(LaunchConfigurationsMessages.LaunchConfigurationTabGroupViewer_No_tabs_defined_for_launch_configuration_type__0__1, new Object[] { type.getName() }), null);
			CoreException e = new CoreException(status);
			errorDialog(e);
			fInitializingTabs = false;
			return;
		}

		// Update the tabs with the new working copy
		tabGroup.initializeFrom(getWorkingCopy());

		// Update the name field
		fNameWidget.setText(getWorkingCopy().getName());

		fCurrentTabIndex = fTabFolder.getSelectionIndex();

		// Turn off initializing flag to update message
		fInitializingTabs = false;

		if (!fViewform.isVisible()) {
			fViewform.setVisible(true);
		}
	}

	/**
	 * Populate the tabs in the configuration edit area to be appropriate to the current
	 * launch configuration type.
	 * @param configType the type to show tabs for
	 */
	private void showInstanceTabsFor(ILaunchConfigurationType configType) {
		// try to keep on same tab
		Class<? extends ILaunchConfigurationTab> tabKind = null;
		if (getActiveTab() != null) {
			tabKind = getActiveTab().getClass();
		}
		// Build the new tabs
		ILaunchConfigurationTabGroup group = null;
		try {
			group = createGroup();
		} catch (CoreException ce) {
			DebugUIPlugin.errorDialog(getShell(), LaunchConfigurationsMessages.LaunchConfigurationDialog_Error_19, LaunchConfigurationsMessages.LaunchConfigurationDialog_Exception_occurred_creating_launch_configuration_tabs_27,ce); //
			return;
		}
		disposeExistingTabs();
		fTabGroup = group;
		ILaunchConfigurationTab[] tabs = getTabs();
		CTabItem tab = null;
		String name = IInternalDebugCoreConstants.EMPTY_STRING;
		Control control = null;
		for (ILaunchConfigurationTab t : tabs) {
			tab = new CTabItem(fTabFolder, SWT.BORDER);
			name = t.getName();
			if (name == null) {
				name = LaunchConfigurationsMessages.LaunchConfigurationDialog_unspecified_28;
			}
			tab.setText(name);
			tab.setImage(t.getImage());
			ScrolledComposite sc = new ScrolledComposite(tab.getParent(), SWT.V_SCROLL | SWT.H_SCROLL);
			sc.setFont(tab.getParent().getFont());
			sc.setExpandHorizontal(true);
			sc.setExpandVertical(true);
			sc.setShowFocusedControl(true);
			t.createControl(sc);
			control = t.getControl();
			if (control != null) {
				sc.setContent(control);
				sc.setMinSize(control.computeSize(SWT.DEFAULT, SWT.DEFAULT));
				tab.setControl(control.getParent());
			}
		}
		//set the default tab as the first one
		if (tabs.length > 0) {
			setActiveTab(tabs[0]);
		}
		// select same tab as before, if possible
		for (ILaunchConfigurationTab t : tabs) {
			if (t.getClass().equals(tabKind)) {
				setActiveTab(t);
				break;
			}
		}
		fDescription = getDescription(configType);
	}

	/**
	 * Returns the description of the given configuration type
	 * in the current mode or <code>null</code> if none.
	 *
	 * @param configType the config type
	 * @return the description of the given configuration type or <code>null</code>
	 */
	private String getDescription(ILaunchConfigurationType configType) {
		String description = null;
		if(configType != null) {
			String mode = fDialog.getMode();
			description = LaunchConfigurationPresentationManager.getDefault().getDescription(configType, mode);
		}
		if (description == null) {
			description = IInternalDebugCoreConstants.EMPTY_STRING;
		}
		return description;
	}

	/**
	 * Returns tab group for the given type of launch configuration.
	 * Tabs are initialized to be contained in this dialog.
	 * @return the new {@link ILaunchConfigurationTabGroup}
	 *
	 * @exception CoreException if unable to instantiate a tab group
	 */
	protected ILaunchConfigurationTabGroup createGroup() throws CoreException {
		// Use a final Object array to store the tab group and any exception that
		// results from the Runnable
		final Object[] finalArray = new Object[2];
		Runnable runnable = () -> {
			ILaunchConfigurationTabGroup tabGroup = null;
			try {
				tabGroup = LaunchConfigurationPresentationManager.getDefault().getTabGroup(getWorkingCopy(),
						getLaunchConfigurationDialog().getMode());
				finalArray[0] = tabGroup;
			} catch (CoreException ce) {
				finalArray[1] = ce;
				return;
			}
			tabGroup.createTabs(getLaunchConfigurationDialog(), getLaunchConfigurationDialog().getMode());
			ILaunchConfigurationTab[] tabs = tabGroup.getTabs();
			for (ILaunchConfigurationTab tab : tabs) {
				tab.setLaunchConfigurationDialog(getLaunchConfigurationDialog());
			}
		};

		// Creating the tabs can result in plug-in loading, so we show the busy cursor
		BusyIndicator.showWhile(getControl().getDisplay(), runnable);

		// Re-throw any CoreException if there was one
		if (finalArray[1] != null) {
			throw (CoreException)finalArray[1];
		}

		// Otherwise return the tab group
		return (ILaunchConfigurationTabGroup)finalArray[0];
	}

	/**
	 * Returns the tabs currently being displayed, or
	 * <code>null</code> if none.
	 *
	 * @return currently displayed tabs, or <code>null</code>
	 */
	public ILaunchConfigurationTab[] getTabs() {
		if (getTabGroup() != null) {
			return getTabGroup().getTabs();
		}
		return null;
	}

	/**
	 * Returns the currently active <code>ILaunchConfigurationTab</code>
	 * being displayed, or <code>null</code> if there is none.
	 *
	 * @return currently active <code>ILaunchConfigurationTab</code>, or <code>null</code>.
	 */
	public ILaunchConfigurationTab getActiveTab() {
		ILaunchConfigurationTab[] tabs = getTabs();
		if (fTabFolder != null && tabs != null) {
			int pageIndex = fTabFolder.getSelectionIndex();
			if (pageIndex >= 0) {
				return tabs[pageIndex];
			}
		}
		return null;
	}

	/**
	 * Returns whether the launch configuration being edited is dirty (i.e.
	 * needs saving)
	 *
	 * @return whether the launch configuration being edited needs saving
	 */
	public boolean isDirty() {
		ILaunchConfigurationWorkingCopy workingCopy = getWorkingCopy();
		if (workingCopy == null) {
			return false;
		}
		if(workingCopy.getParent() != null) {
			return !workingCopy.getParent().contentsEqual(workingCopy);
		}
		// Working copy hasn't been saved
		if (workingCopy.getOriginal() == null) {
			return true;
		}
		return fOriginal != null && !fOriginal.contentsEqual(workingCopy);
	}

	/**
	 * Returns the job to update the launch configuration dialog.
	 *
	 * @return update job
	 */
	private Job getUpdateJob() {
		if (fRefreshJob == null) {
			fRefreshJob = createUpdateJob();
			fRefreshJob.setSystem(true);
		}
		return fRefreshJob;
	}

	/**
	 * Schedules the update job to run for this tab based on this tab's delay.
	 *
	 * @since 3.6
	 */
	protected void scheduleUpdateJob() {
		Job job = getUpdateJob();
		job.cancel(); // cancel existing job
		job.schedule(getUpdateJobDelay());
	}

	/**
	 * Return the time delay that should be used when scheduling the
	 * update job. Subclasses may override.
	 *
	 * @return a time delay in milliseconds before the job should run
	 * @since 3.6
	 */
	protected long getUpdateJobDelay() {
		return 200;
	}

	/**
	 * Creates and returns a job used to update the launch configuration dialog
	 * for this tab. Subclasses may override.
	 *
	 * @return job to update the launch dialog for this tab
	 * @since 3.6
	 */
	protected Job createUpdateJob() {
		return  new WorkbenchJob(getControl().getDisplay(), "Update LCD") { //$NON-NLS-1$
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				if (!getControl().isDisposed()) {
					refreshStatus();
				}
				return Status.OK_STATUS;
			}
			@Override
			public boolean shouldRun() {
				return !getControl().isDisposed();
			}
		};
	}

	/**
	 * Update apply & revert buttons, as well as buttons and message on the
	 * launch config dialog.
	 */
	protected void refreshStatus() {
		if (!fInitializingTabs) {
			LaunchConfigurationsDialog lcd = (LaunchConfigurationsDialog) getLaunchConfigurationDialog();
			lcd.refreshStatus();
		}
	}

	/**
	 * Returns the containing launch dialog
	 * @return the current {@link ILaunchConfigurationDialog}
	 */
	protected ILaunchConfigurationDialog getLaunchConfigurationDialog() {
		return fDialog;
	}

	/**
	 * Returns the original launch configuration being edited, possibly
	 * <code>null</code>.
	 *
	 * @return ILaunchConfiguration
	 */
	protected ILaunchConfiguration getOriginal() {
		return fOriginal;
	}

	/**
	 * Returns the working copy used to edit the original, possibly
	 * <code>null</code>.
	 * @return the backing {@link ILaunchConfigurationWorkingCopy}
	 */
	protected ILaunchConfigurationWorkingCopy getWorkingCopy() {
		return fWorkingCopy;
	}

	/**
	 * Return whether the current configuration can be saved.
	 * <p>
	 * Note this is NOT the same thing as the config simply being valid. It
	 * is possible to save a config that does not validate. This method
	 * determines whether the config can be saved without causing a serious
	 * error. For example, a shared config that has no specified location would
	 * cause this method to return <code>false</code>.
	 * </p>
	 * @return if the dialog can save in its current state
	 */
	public boolean canSave() {
		if (fInitializingTabs) {
			return false;
		}
		// First make sure that name doesn't prevent saving the config
		try {
			verifyName();
		} catch (CoreException ce) {
			return false;
		}

		// Next, make sure none of the tabs object to saving the config
		ILaunchConfigurationTab[] tabs = getTabs();
		if (tabs == null) {
			return false;
		}
		for (ILaunchConfigurationTab tab : tabs) {
			if (!tab.canSave()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @return if the dialog can launch in its current state
	 */
	public boolean canLaunch() {
		if(fInitializingTabs) {
			return false;
		}
		if (getWorkingCopy() == null) {
			return false;
		}
		try {
			verifyName();
		} catch (CoreException e) {
			return false;
		}

		ILaunchConfigurationTab[] tabs = getTabs();
		if (tabs == null) {
			return false;
		}
		for (ILaunchConfigurationTab tab : tabs) {
			if (!tab.isValid(getWorkingCopy())) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Determines if the tab groups that is currently visible can launch with the currently selected
	 * set of options.
	 *
	 * @return true if the dialog can launch with the given set of modes, false otherwise
	 *
	 * @since 3.3
	 */
	public boolean canLaunchWithModes() {
		if(fInitializingTabs) {
			return false;
		}
		//check if selected options exist and that the selected combination can be launched
		try {
			ILaunchConfigurationWorkingCopy wc = getWorkingCopy();
			if(wc != null) {
				return wc.getType().supportsModeCombination(getCurrentModeSet());
			}
		}  catch (CoreException e) {
		}
		return true;
	}

	/**
	 * Returns if the type currently showing in the tab group viewer has duplicate launch delegates for the given set of modes.
	 *
	 * The given set of modes comprises the current mode that the launch dialog was opened in as well as any modes that have been set on the launch
	 * configuration.
	 * @return the true if there are duplicates, false otherwise
	 *
	 * @since 3.3
	 */
	public boolean hasDuplicateDelegates() {
		if(fInitializingTabs) {
			return false;
		}
		ILaunchConfiguration config = getWorkingCopy();
		if(config != null) {
			if(hasMultipleDelegates()) {
				return getPreferredDelegate() == null;
			}
		}
		return false;
	}

	/**
	 * Determines if the currently showing launch configuration has multiple launch delegates for the same mode set, but does not care
	 * if there has been a default selected yet or not
	 * @return true if the current launch configuration has multiple launch delegates, false otherwise
	 */
	private boolean hasMultipleDelegates() {
		ILaunchConfiguration config = getWorkingCopy();
		if(config != null) {
			try {
				Set<String> modes = getCurrentModeSet();
				ILaunchDelegate[] delegates = LaunchConfigurationManager.filterLaunchDelegates(config.getType(), modes);
				return delegates.length > 1;
			}
			catch (CoreException ce) {DebugUIPlugin.log(ce);}
		}
		return false;
	}

	/**
	 * Returns the current error message or <code>null</code> if none.
	 * @return the error message for the tab
	 */
	public String getErrorMesssage() {
		if (fInitializingTabs) {
			return null;
		}

		if (getWorkingCopy() == null) {
			return null;
		}
		try {
			verifyName();
		} catch (CoreException ce) {
			return ce.getStatus().getMessage();
		}

		if(hasDuplicateDelegates()) {
			return LaunchConfigurationsMessages.LaunchConfigurationTabGroupViewer_18;
		}

		String message = null;
		ILaunchConfigurationTab activeTab = getActiveTab();
		if (activeTab == null) {
			return null;
		}
		message = activeTab.getErrorMessage();
		if (message != null) {
			return message;
		}

		ILaunchConfigurationTab[] allTabs = getTabs();
		for (ILaunchConfigurationTab tab : allTabs) {
			if (tab == activeTab) {
				continue;
			}
			message = tab.getErrorMessage();
			if (message != null) {
				StringBuilder temp= new StringBuilder();
				temp.append('[');
				temp.append(DebugUIPlugin.removeAccelerators(tab.getName()));
				temp.append("]: "); //$NON-NLS-1$
				temp.append(message);
				return temp.toString();
			}
		}
		if(!canLaunchWithModes()) {
			Set<String> modes = getCurrentModeSet();
			List<String> names = LaunchConfigurationPresentationManager.getDefault().getLaunchModeNames(modes);
			return MessageFormat.format(LaunchConfigurationsMessages.LaunchConfigurationTabGroupViewer_14, new Object[] { names.toString() });
		}
		return null;
	}

	/**
	 * Returns the current message or <code>null</code> if none.
	 * @return Returns an appropriate message for display to user. The message returned will be:
	 * The message defined by the visible tab,
	 * or The tab group description for the particular launch mode,
	 * or The generic tab group description,
	 * or <code>null</code> if no message is defined
	 */
	public String getMessage() {
		if (fInitializingTabs) {
			return null;
		}

		String message = fDescription;

		ILaunchConfigurationTab tab = getActiveTab();
		if (tab != null) {
			String tabMessage = tab.getMessage();
			if (tabMessage != null) {
				message = tabMessage;
			}
		}

		return message;
	}

	/**
	 * Returns the current warning message or <code>null</code> if none.
	 * @return Returns an appropriate warning message for display to user. The message returned will be:
	 * The warning message defined by the visible tab
	 * or <code>null</code> if no message is defined
	 */
	public String getWarningMessage() {
		if (fInitializingTabs) {
			return null;
		}

		String message = null;

		ILaunchConfigurationTab tab = getActiveTab();
		if (tab instanceof ILaunchConfigurationTab2) {
			String tabMessage = ((ILaunchConfigurationTab2)tab).getWarningMessage();
			if (tabMessage != null) {
				message = tabMessage;
			}
		}

		return message;
	}

	/**
	 * Verify that the launch configuration name is valid.
	 * @throws CoreException if a name conflict occurs
	 */
	protected void verifyName() throws CoreException {
		if (fNameWidget.isVisible()) {
			ILaunchManager mgr = DebugPlugin.getDefault().getLaunchManager();
			String currentName = fNameWidget.getText().trim();

			// If there is no name, complain
			if (currentName.length() < 1) {
				throw new CoreException(new Status(IStatus.ERROR,
													 DebugUIPlugin.getUniqueIdentifier(),
													 0,
													 LaunchConfigurationsMessages.LaunchConfigurationDialog_Name_required_for_launch_configuration_11,
													 null));
			}
			try {
				mgr.isValidLaunchConfigurationName(currentName);
			}
			catch(IllegalArgumentException iae) {
				throw new CoreException(new Status(IStatus.ERROR,
						 DebugUIPlugin.getUniqueIdentifier(),
						 0,
						 iae.getMessage(),
						 null));
			}
			// Otherwise, if there's already a config with the same name, complain
			if (fOriginal != null && !fOriginal.getName().equals(currentName)) {
				Set<String> reservednames = ((LaunchConfigurationsDialog) getLaunchConfigurationDialog()).getReservedNameSet();
				if (mgr.isExistingLaunchConfigurationName(currentName) || (reservednames != null ? reservednames.contains(currentName) : false)) {
					ILaunchConfiguration config = ((LaunchManager)mgr).findLaunchConfiguration(currentName);
					// config can be null if name matches to unsaved
					// configuration created in Properties dialog
					String configTypeName = config != null ? config.getType().getName() : fOriginal.getType().getName();
					throw new CoreException(new Status(IStatus.ERROR,
														 DebugUIPlugin.getUniqueIdentifier(),
														 0,
							NLS.bind(LaunchConfigurationsMessages.LaunchConfigurationDialog_Launch_configuration_already_exists_with_this_name_12, configTypeName),
														 null));
				}
			}
		}
	}

	/**
	 * Remove the existing tabs that are showing
	 */
	private void disposeExistingTabs() {
		fDisposingTabs = true;
		fTabFolder.dispose();
		fTabFolder = null;
		createTabFolder(fGroupComposite);
		disposeTabGroup();
		fDisposingTabs = false;
	}

	/**
	 * Returns the current tab group
	 *
	 * @return the current tab group, or <code>null</code> if none
	 */
	public ILaunchConfigurationTabGroup getTabGroup() {
		return fTabGroup;
	}

	/**
	 * Notification that a tab has been selected
	 *
	 * Disallow tab changing when the current tab is invalid.
	 * Update the config from the tab being left, and refresh
	 * the tab being entered.
	 */
	protected void handleTabSelected() {
		if (fDisposingTabs || fInitializingTabs) {
			return;
		}
		ILaunchConfigurationTab[] tabs = getTabs();
		if (fCurrentTabIndex == fTabFolder.getSelectionIndex() || tabs == null || tabs.length == 0 || fCurrentTabIndex > (tabs.length - 1)) {
			return;
		}
		if (fCurrentTabIndex != -1) {
			ILaunchConfigurationTab tab = tabs[fCurrentTabIndex];
			ILaunchConfigurationWorkingCopy wc = getWorkingCopy();
			if (wc != null) {
				tab.deactivated(wc);
				getActiveTab().activated(wc);
			}
		}
		fCurrentTabIndex = fTabFolder.getSelectionIndex();
	}

	/**
	 * Notification the name field has been modified
	 */
	protected void handleNameModified() {
		getWorkingCopy().rename(fNameWidget.getText().trim());
		scheduleUpdateJob();
	}

	/**
	 * Notification that the 'Apply' button has been pressed.
	 *
	 * @return the saved launch configuration or <code>null</code> if not saved
	 */
	public ILaunchConfiguration handleApplyPressed() {
		if(fOriginal != null && fOriginal.isReadOnly()) {
			IStatus status = ResourcesPlugin.getWorkspace().validateEdit(new IFile[] {fOriginal.getFile()}, fViewerControl.getShell());
			if(!status.isOK()) {
				return null;
			}
		}
		Exception exception = null;
		final ILaunchConfiguration[] saved = new ILaunchConfiguration[1];
		try {
			// update launch config
			fInitializingTabs = true;
			// trim name
			String trimmed = fNameWidget.getText().trim();
			fNameWidget.setText(trimmed);
			if(fWorkingCopy == null) {
				fWorkingCopy = fOriginal.getWorkingCopy();
			}
			fWorkingCopy.rename(trimmed);
			getTabGroup().performApply(fWorkingCopy);
			if (isDirty()) {
				if(!fWorkingCopy.isLocal()) {
					IRunnableWithProgress runnable = monitor -> {
						try {
							saved[0] = ((LaunchConfigurationWorkingCopy) fWorkingCopy).doSave(monitor);
						}
						catch (CoreException e) {
							DebugUIPlugin.log(e);
						}
					};
					getLaunchConfigurationDialog().run(true, false, runnable);
				}
				else {
					saved[0] = fWorkingCopy.doSave(ILaunchConfiguration.UPDATE_PROTOTYPE_CHILDREN);
				}
			}
		}
		catch (CoreException e) {exception = e;}
		catch (InvocationTargetException e) {exception = e;}
		catch (InterruptedException e) {exception = e;}
		finally { fInitializingTabs = false; }

		updateButtons();

		if(exception != null) {
			DebugUIPlugin.errorDialog(getShell(), LaunchConfigurationsMessages.LaunchConfigurationDialog_Launch_Configuration_Error_46, LaunchConfigurationsMessages.LaunchConfigurationDialog_Exception_occurred_while_saving_launch_configuration_47, exception); //
			return null;
		} else {
			return saved[0];
		}
	}

	/**
	 * Notification that the 'Revert' button has been pressed
	 */
	protected void handleRevertPressed() {
		try {
			if(fTabGroup != null) {
				fTabGroup.initializeFrom(fOriginal);
				fNameWidget.setText(fOriginal.getName());
				fWorkingCopy = fOriginal.getWorkingCopy();
				refreshStatus();
			}
		}
		catch (CoreException e) {DebugUIPlugin.log(e);}
	}

	/**
	 * Dialog to Show the Command line
	 */
	protected void handleShowCommandLinePressed() {
		boolean showCommandLineDialog = true;
		if (isDirty()) {
			int returnVal = showSaveChangesDialog();
			if (returnVal == IDialogConstants.YES_ID) {
				handleApplyPressed();
			} else if (returnVal == IDialogConstants.NO_ID) {
				handleRevertPressed();
			} else {
				showCommandLineDialog = false;
			}
		}
		if (showCommandLineDialog) {
			ShowCommandLineDialog dialog = new ShowCommandLineDialog(getShell(),
					getLaunchConfigurationDialog().getMode(),
					fWorkingCopy);
			dialog.open();
		}
	}

	private int showSaveChangesDialog() {
		String message = MessageFormat.format(
				LaunchConfigurationsMessages.LaunchConfigurationDialog_ShowCommandLine_Apply_Revert_Question,
				new Object[] { getWorkingCopy().getName() });
		MessageDialog dialog = new MessageDialog(getShell(),
				LaunchConfigurationsMessages.LaunchConfigurationDialog_ShowCommandLine_Apply_Revert_Title, null,
				message,
				MessageDialog.QUESTION,
				new String[] {
						LaunchConfigurationsMessages.LaunchConfigurationDialog_ShowCommandLine_Apply_Revert_Apply,
						LaunchConfigurationsMessages.LaunchConfigurationDialog_ShowCommandLine_Apply_Revert_Revert,
						LaunchConfigurationsMessages.LaunchConfigurationDialog_ShowCommandLine_Apply_Revert_Cancel },
				0);
		int ret = dialog.open();
		int val = IDialogConstants.CANCEL_ID;
		if (ret == 0 || ret == 1) {
			if (ret == 0) {
				val = IDialogConstants.YES_ID;
			} else {
				val = IDialogConstants.NO_ID;
			}
		}
		return val;
	}

	/**
	 * Show an error dialog on the given exception.
	 *
	 * @param exception the exception to display
	 */
	protected void errorDialog(CoreException exception) {
		ErrorDialog.openError(getShell(), null, null, exception.getStatus());
	}

	/**
	 * Sets the displayed tab to the given tab. Has no effect if the specified
	 * tab is not one of the tabs being displayed in the dialog currently.
	 *
	 * @param tab the tab to display/activate
	 */
	public void setActiveTab(ILaunchConfigurationTab tab) {
		ILaunchConfigurationTab[] tabs = getTabs();
		if(tabs != null) {
			for (int i = 0; i < tabs.length; i++) {
				if (tabs[i].getClass().equals(tab.getClass())) {
					setActiveTab(i);
					return;
				}
			}
		}
	}

	/**
	 * Sets the displayed tab to the tab with the given index. Has no effect if
	 * the specified index is not within the limits of the tabs returned by
	 * <code>getTabs()</code>.
	 *
	 * @param index the index of the tab to display
	 */
	public void setActiveTab(int index) {
		if (!fTabFolder.isDisposed()) {
			ILaunchConfigurationTab[] tabs = getTabs();
			if (index >= 0 && index < tabs.length) {
				fTabFolder.setSelection(index);
				handleTabSelected();
			}
		}
	}

}
