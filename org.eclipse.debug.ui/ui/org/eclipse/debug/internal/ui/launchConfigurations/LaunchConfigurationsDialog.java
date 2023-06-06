/*******************************************************************************
 *  Copyright (c) 2000, 2018 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sebastian Davids - Bug 137923
 *     Mohamed Hussein (Mentor Graphics) - Added s/getWarningMessage (Bug 386673)
 *     Axel Richard (Obeo) - Bug 41353 - Launch configurations prototypes
 *******************************************************************************/
package org.eclipse.debug.internal.ui.launchConfigurations;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.core.LaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.core.LaunchManager;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.IDebugView;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchConfigurationTabGroup;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.operation.ModalContext;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.ProgressMonitorPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.WorkbenchJob;
import org.osgi.framework.FrameworkUtil;

/**
 * The dialog used to edit and launch launch configurations.
 */
public class LaunchConfigurationsDialog extends TitleAreaDialog implements ILaunchConfigurationDialog, IPropertyChangeListener {

	/**
	 * Keep track of the currently visible dialog instance
	 */
	private static ILaunchConfigurationDialog fgCurrentlyVisibleLaunchConfigurationDialog;
	/**
	 * Id for 'Launch' button.
	 */
	protected static final int ID_LAUNCH_BUTTON = IDialogConstants.CLIENT_ID + 1;

	/**
	 * Id for 'Close' button.
	 */
	protected static final int ID_CLOSE_BUTTON = IDialogConstants.CLIENT_ID + 2;
	/**
	 * Id for 'Cancel' button.
	 */
	protected static final int ID_CANCEL_BUTTON = IDialogConstants.CLIENT_ID + 3;

	/**
	 * The id for the 'No' button on the discard changes message box
	 * @since 3.3
	 */
	protected static final int ID_DISCARD_BUTTON = IDialogConstants.CLIENT_ID + 4;

	/**
	 * Constant specifying how wide this dialog is allowed to get (as a percentage of
	 * total available screen width) as a result of tab labels in the edit area.
	 */
	protected static final float MAX_DIALOG_WIDTH_PERCENT = 0.75f;
	/**
	 * Constant specifying how tall this dialog is allowed to get (as a percentage of
	 * total available screen height) as a result of preferred tab size.
	 */
	protected static final float MAX_DIALOG_HEIGHT_PERCENT = 0.65f;
	/**
	 * Size of this dialog if there is no preference specifying a size.
	 */
	protected static final Point DEFAULT_INITIAL_DIALOG_SIZE = new Point(800, 640);
	/**
	 * defines some default sash weights when we have a new workspace
	 * @since 3.2
	 */
	private static final int[] DEFAULT_SASH_WEIGHTS = new int[] {190, 610};
	/**
	 * Constant specifying that this dialog should be opened with the last configuration launched
	 * in the workspace selected.
	 */
	public static final int LAUNCH_CONFIGURATION_DIALOG_OPEN_ON_LAST_LAUNCHED = 2;
	/**
	 * Constant specifying that this dialog should be opened with the value specified via
	 * <code>setInitialSelection()</code> selected.
	 */
	public static final int LAUNCH_CONFIGURATION_DIALOG_OPEN_ON_SELECTION = 3;
	/**
	 * Constant specifying that a new launch configuration dialog was not opened.  Instead
	 * an existing launch configuration dialog was used.
	 */
	public static final int LAUNCH_CONFIGURATION_DIALOG_REUSE_OPEN = 4;
	/**
	 * defines the delimiter used in the persistence of the expanded state
	 * @since 3.2
	 */
	private static final String DELIMITER = ", "; //$NON-NLS-1$
	/**
	 * Specifies how this dialog behaves when opened.  Value is one of the
	 * 'LAUNCH_CONFIGURATION_DIALOG' constants defined in this class.
	 */
	private int fOpenMode = LAUNCH_CONFIGURATION_DIALOG_OPEN_ON_LAST_LAUNCHED;

	private boolean fIsShift = false;

	/**
	 * dialog settings
	 */
	private static final String DIALOG_SASH_WEIGHTS_1 = IDebugUIConstants.PLUGIN_ID + ".DIALOG_SASH_WEIGHTS_1"; //$NON-NLS-1$
	private static final String DIALOG_SASH_WEIGHTS_2 = IDebugUIConstants.PLUGIN_ID + ".DIALOG_SASH_WEIGHTS_2"; //$NON-NLS-1$
	private static final String DIALOG_EXPANDED_NODES = IDebugUIConstants.PLUGIN_ID + ".EXPANDED_NODES"; //$NON-NLS-1$

	/**
	 * Returns the currently visible dialog
	 * @return the currently visible launch dialog
	 */
	public static ILaunchConfigurationDialog getCurrentlyVisibleLaunchConfigurationDialog() {
		return fgCurrentlyVisibleLaunchConfigurationDialog;
	}
	/**
	 * Sets which launch dialog is currently the visible one
	 * @param dialog the dialog to set as the visible one
	 */
	public static void setCurrentlyVisibleLaunchConfigurationDialog(ILaunchConfigurationDialog dialog) {
		fgCurrentlyVisibleLaunchConfigurationDialog = dialog;
	}

	/**
	 * widgets
	 */
	private Control fLastControl;
	private Composite fButtonComp;
	private SashForm fSashForm;
	private LaunchConfigurationView fLaunchConfigurationView;
	private LaunchConfigurationTabGroupViewer fTabViewer;
	private ProgressMonitorPart fProgressMonitorPart;
	private LaunchGroupExtension fGroup;
	private Image fBannerImage;

	/**
	 * When this dialog is opened in <code>LAUNCH_CONFIGURATION_DIALOG_OPEN_ON_SELECTION</code>
	 * mode, this specifies the selection that is initially shown in the dialog.
	 */
	private IStructuredSelection fInitialSelection;

	/**
	 * The status to open the dialog on, or <code>null</code> if none.
	 */
	private IStatus fInitialStatus;

	/**
	 * The number of 'long-running' operations currently taking place in this dialog
	 */
	private long fActiveRunningOperations = 0;

	/**
	 * Double-click action
	 */
	private IAction fDoubleClickAction;

	/**
	 * Filters for the LCD
	 * @since 3.2
	 */
	private ClosedProjectFilter fClosedProjectFilter;
	private DeletedProjectFilter fDeletedProjectFilter;
	private LaunchConfigurationTypeFilter fLCTFilter;
	private WorkingSetsFilter fWorkingSetsFilter;
	private UniqueLaunchConfigurationFileFilter fUniqueLaunchConfugurationFileFilter;

	/**
	 * set of reserved names that should not be considered when generating a new name for a launch configuration
	 */
	protected Set<String> fReservedNames = null;

	/**
	 * Whether to set default values when opened
	 * @since 3.6
	 */
	private boolean fSetDefaultOnOpen = false;

	/**
	 * Whether in the process of setting the input to the tab viewer
	 */
	private boolean fSettingInput = false;

	/**
	 * Constructs a new launch configuration dialog on the given
	 * parent shell.
	 *
	 * @param shell the parent shell
	 * @param group the group of launch configuration to display
	 */
	public LaunchConfigurationsDialog(Shell shell, LaunchGroupExtension group) {
		super(shell);
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
		setLaunchGroup(group);
	}

	/**
	 * Adds content to the dialog area
	 *
	 * @param dialogComp
	 */
	protected void addContent(Composite dialogComp) {
		GridData gd;
		Composite topComp = new Composite(dialogComp, SWT.NONE);
		gd = new GridData(GridData.FILL_BOTH);
		topComp.setLayoutData(gd);
		GridLayout topLayout = new GridLayout(2, false);
		topLayout.marginHeight = 5;
		topLayout.marginWidth = 5;
		topComp.setLayout(topLayout);

		// Set the things that TitleAreaDialog takes care of
		setTitle(LaunchConfigurationsMessages.LaunchConfigurationDialog_Create__manage__and_run_launch_configurations_8);
		setMessage(LaunchConfigurationsMessages.LaunchConfigurationDialog_Ready_to_launch_2);
		setModeLabelState();

		// Create the SashForm that contains the selection area on the left,
		// and the edit area on the right
		gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		SashForm sash = new SashForm(topComp, SWT.SMOOTH);
		sash.setOrientation(SWT.HORIZONTAL);
		sash.setLayoutData(gd);
		sash.setFont(dialogComp.getFont());
		sash.setVisible(true);
		fSashForm = sash;

		// Build the launch configuration selection area and put it into the composite.
		Control launchConfigSelectionArea = createLaunchConfigurationSelectionArea(fSashForm);
		gd = new GridData(GridData.FILL_VERTICAL);
		launchConfigSelectionArea.setLayoutData(gd);

		// Build the launch configuration edit area and put it into the composite.
		Composite editAreaComp = createLaunchConfigurationEditArea(fSashForm);
		gd = new GridData(GridData.FILL_BOTH);
		editAreaComp.setLayoutData(gd);

		dialogComp.layout(true);
		applyDialogFont(dialogComp);
	}

	/**
	 * Handle the 'close' & 'launch' buttons here, all others are handled
	 * in <code>Dialog</code>
	 *
	 * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
	 */
	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == ID_LAUNCH_BUTTON) {
			handleLaunchPressed();
		}
		else if (buttonId == ID_CLOSE_BUTTON) {
			handleClosePressed();
		}
		else {
			super.buttonPressed(buttonId);
		}
	}

	/**
	 * Return whether the current configuration should be saved or discarded.  This involves determining
	 * if it is dirty, and if it is, asking the user what to do.
	 *
	 * @return if we can discard the current config or not
	 */
	protected int shouldSaveCurrentConfig() {
		if (fTabViewer.isDirty()) {
			if (fTabViewer.canSave()) {
				return showSaveChangesDialog();
			}
			return showUnsavedChangesDialog();
		}
		return IDialogConstants.NO_ID;
	}

	@Override
	public boolean close() {
		if (!isSafeToClose()) {
			return false;
		}
		persistSashWeights();
		persistExpansion();
		setCurrentlyVisibleLaunchConfigurationDialog(null);
		fTabViewer.dispose();
		if (fLaunchConfigurationView != null) {
			fLaunchConfigurationView.dispose();
		}
		DebugUIPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(this);
		boolean result = super.close();
		getBannerImage().dispose();
		return result;
	}

	/**
	 * Sets the title for the dialog, and establishes the help context.
	 *
	 * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell);
	 */
	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(getShellTitle());
	}

	@Override
	public void create() {
		super.create();
		if (fTabViewer.getInput() == null) {
			fTabViewer.inputChanged(null);
		}
	}

	@Override
	protected Control createButtonBar(Composite parent) {
		Font font = parent.getFont();
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.marginLeft = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		composite.setFont(font);
		// create help control if needed
		if (isHelpAvailable()) {
			createHelpControl(composite);
		}
		Composite monitorComposite = new Composite(composite, SWT.NULL);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 2;
		monitorComposite.setLayout(layout);
		monitorComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		GridLayout pmLayout = new GridLayout();
		fProgressMonitorPart= new ProgressMonitorPart(monitorComposite, pmLayout, true);
		fProgressMonitorPart.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fProgressMonitorPart.setFont(font);
		monitorComposite.setVisible(false);

		/*
		 * Create the rest of the button bar, but tell it not to
		 * create a help button (we've already created it).
		 */
		boolean helpAvailable = isHelpAvailable();
		setHelpAvailable(false);
		fButtonComp = (Composite) super.createButtonBar(composite);
		setHelpAvailable(helpAvailable);
		return composite;
	}

	/**
	 *
	 * @param buttonId
	 */
	protected void launchButtonPressed(int buttonId) {
		if (buttonId == ID_LAUNCH_BUTTON) {
			handleLaunchPressed();
		} else if (buttonId == ID_CLOSE_BUTTON) {
			handleClosePressed();
		} else {
			super.buttonPressed(buttonId);
		}
	}


	/**
	 * A launch configuration dialog overrides this method
	 * to create a custom set of buttons in the button bar.
	 * This dialog has 'Launch' and 'Cancel'
	 * buttons.
	 *
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		Button button = createButton(parent, ID_LAUNCH_BUTTON, getLaunchButtonText(), true);
		button.setEnabled(false);
		Listener[] listeners = button.getListeners(SWT.Selection);
		for (Listener listener : listeners) {
			button.removeListener(SWT.Selection, listener);
		}
		listeners = button.getListeners(SWT.DefaultSelection);
		for (Listener listener : listeners) {
			button.removeListener(SWT.DefaultSelection, listener);
		}

		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				setShift(((event.stateMask & SWT.SHIFT) > 0) ? true : false);
				handleLaunchPressed();
			}
		});
		createButton(parent, ID_CLOSE_BUTTON, LaunchConfigurationsMessages.LaunchConfigurationDialog_Close_1, false);
	}

	@Override
	protected Control createContents(Composite parent) {
		Control contents = super.createContents(parent);
		initializeContent();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getShell(), getHelpContextId());
		return contents;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite dialogComp = (Composite)super.createDialogArea(parent);
		addContent(dialogComp);
		if(fLaunchConfigurationView != null) {
			fLaunchConfigurationView.updateFilterLabel();
		}
		return dialogComp;
	}

	/**
	 * Creates the launch configuration edit area of the dialog.
	 * This area displays the name of the launch configuration
	 * currently being edited, as well as a tab folder of tabs
	 * that are applicable to the launch configuration.
	 *
	 * @return the composite used for launch configuration editing
	 */
	protected Composite createLaunchConfigurationEditArea(Composite parent) {
		setTabViewer(new LaunchConfigurationTabGroupViewer(parent, this));
		return (Composite)fTabViewer.getControl();
	}

	/**
	 * Creates all of the actions for the toolbar
	 * @param toolbar
	 * @since 3.2
	 */
	protected void createToolbarActions(ToolBarManager tmanager) {
		tmanager.add(getNewAction());
		tmanager.add(getNewPrototypeAction());
		tmanager.add(getExportAction());
		tmanager.add(getDuplicateAction());
		tmanager.add(getDeleteAction());
		tmanager.add(new Separator());
		tmanager.add(getCollapseAllAction());
		tmanager.add(getFilterAction());
		tmanager.update(true);
		DebugUIPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(this);
	}

	protected void setShift(boolean isShift) {
		fIsShift = isShift;
	}
	/**
	 * Creates the launch configuration selection area of the dialog.
	 * This area displays a tree of launch configurations that the user
	 * may select, and allows users to create new configurations, and
	 * delete and duplicate existing configurations.
	 *
	 * @return the composite used for launch configuration selection area
	 */
	protected Control createLaunchConfigurationSelectionArea(Composite parent) {
		Composite comp = new Composite(parent, SWT.FLAT);
		GridLayout gridLayout = new GridLayout(1, false);
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		comp.setLayout(gridLayout);
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));

		ViewForm viewForm = new ViewForm(comp, SWT.FLAT | SWT.BORDER);
		ToolBarManager toolBarManager= new ToolBarManager(SWT.FLAT);
		ToolBar toolBar = toolBarManager.createControl(viewForm);
		toolBar.setBackground(parent.getBackground());
		viewForm.setTopLeft(toolBar);
		viewForm.setLayoutData(new GridData(GridData.FILL_BOTH));

		Composite viewFormContents = new Composite(viewForm, SWT.FLAT);
		gridLayout = new GridLayout();
		gridLayout.marginHeight = 5;
		gridLayout.marginWidth = 5;
		viewFormContents.setLayout(gridLayout);
		viewFormContents.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		fLaunchConfigurationView = new LaunchConfigurationView(getLaunchGroup(), createViewerFilters());
		fLaunchConfigurationView.createLaunchDialogControl(viewFormContents);
		Text filterText = fLaunchConfigurationView.getFilteringTextControl();
		if (filterText != null){
			filterText.setFocus();
		}

	//create toolbar actions, we reuse the actions from the view so we wait until after
	//the view is created to add them to the toolbar
		createToolbarActions(toolBarManager);
		fDoubleClickAction = new Action() {
			@Override
			public void run() {
				runInternal(false);
			}

			@Override
			public void runWithEvent(Event event) {
				runInternal(((event.stateMask & SWT.SHIFT) > 0) ? true : false);
			}

			void runInternal(boolean isShift) {
				IStructuredSelection selection = (IStructuredSelection)fLaunchConfigurationView.getViewer().getSelection();
				Object target = selection.getFirstElement();
				if (target instanceof ILaunchConfiguration) {
					if (fTabViewer.canLaunch() && fTabViewer.canLaunchWithModes()
							&& !fTabViewer.hasDuplicateDelegates()) {
						setShift(isShift);
						handleLaunchPressed();
					}
				} else {
					getNewAction().run();
				}
			}
		};
		fLaunchConfigurationView.setAction(IDebugView.DOUBLE_CLICK_ACTION, fDoubleClickAction);
		Viewer viewer = fLaunchConfigurationView.getViewer();

		Control control = viewer.getControl();
		GridData gd = new GridData(GridData.FILL_BOTH);
		control.setLayoutData(gd);
		viewForm.setContent(viewFormContents);
		AbstractLaunchConfigurationAction.IConfirmationRequestor requestor = () -> {
			int status = shouldSaveCurrentConfig();
			if(status == IDialogConstants.YES_ID) {
				fTabViewer.handleApplyPressed();
				return true;
			}
			else if(status == IDialogConstants.NO_ID) {
				fTabViewer.handleRevertPressed();
				return true;
			}
			return false;
		};
		getDuplicateAction().setConfirmationRequestor(requestor);
		getExportAction().setConfirmationRequestor(requestor);
		getNewAction().setConfirmationRequestor(requestor);
		getNewPrototypeAction().setConfirmationRequestor(requestor);
		getLinkPrototypeAction().setConfirmationRequestor(requestor);
		getUnlinkPrototypeAction().setConfirmationRequestor(requestor);
		getResetWithPrototypeValuesAction().setConfirmationRequestor(requestor);
		((StructuredViewer) viewer).addPostSelectionChangedListener(event -> {
			handleLaunchConfigurationSelectionChanged(event);
			getNewAction().setEnabled(getNewAction().isEnabled());
			getNewPrototypeAction().setEnabled(getNewPrototypeAction().isEnabled());
			getDeleteAction().setEnabled(getDeleteAction().isEnabled());
			getExportAction().setEnabled(getExportAction().isEnabled());
			getDuplicateAction().setEnabled(getDuplicateAction().isEnabled());
			getLinkPrototypeAction().setEnabled(getLinkPrototypeAction().isEnabled());
			getUnlinkPrototypeAction().setEnabled(getUnlinkPrototypeAction().isEnabled());
			getResetWithPrototypeValuesAction().setEnabled(getResetWithPrototypeValuesAction().isEnabled());
		});
		return comp;
	}

	/**
	 * Create the filters to be initially applied to the viewer.
	 * The initial filters are based on the persisted preferences
	 * @return the array of initial filters
	 * @since 3.2
	 */
	private ViewerFilter[] createViewerFilters() {
		ArrayList<ViewerFilter> filters = new ArrayList<>();
		fClosedProjectFilter = new ClosedProjectFilter();
		if(DebugUIPlugin.getDefault().getPreferenceStore().getBoolean(IInternalDebugUIConstants.PREF_FILTER_LAUNCH_CLOSED)) {
			filters.add(fClosedProjectFilter);
		}
		fDeletedProjectFilter = new DeletedProjectFilter();
		if(DebugUIPlugin.getDefault().getPreferenceStore().getBoolean(IInternalDebugUIConstants.PREF_FILTER_LAUNCH_DELETED)) {
			filters.add(fDeletedProjectFilter);
		}
		fLCTFilter = new LaunchConfigurationTypeFilter();
		if(DebugUIPlugin.getDefault().getPreferenceStore().getBoolean(IInternalDebugUIConstants.PREF_FILTER_LAUNCH_TYPES)) {
			filters.add(fLCTFilter);
		}
		fWorkingSetsFilter = new WorkingSetsFilter();
		if(DebugUIPlugin.getDefault().getPreferenceStore().getBoolean(IInternalDebugUIConstants.PREF_FILTER_WORKING_SETS)) {
			filters.add(fWorkingSetsFilter);
		}
		fUniqueLaunchConfugurationFileFilter = new UniqueLaunchConfigurationFileFilter();
		filters.add(fUniqueLaunchConfugurationFileFilter);
		return filters.toArray(new ViewerFilter[filters.size()]);
	}

	/**
	 * Set the initial selection in the tree.
	 */
	public void doInitialTreeSelection() {
		fLaunchConfigurationView.getViewer().setSelection(fInitialSelection);
	}

	@Override
	public String generateName(String name) {
		if (name == null) {
			return getLaunchManager().generateLaunchConfigurationName(IInternalDebugCoreConstants.EMPTY_STRING);
		}
		return getLaunchManager().generateLaunchConfigurationName(name);
	}

	/**
	 * Generates and returns a unique name using the specified name as a prefix in the event
	 * the specified name already exists or is contained in the set of reserved names.
	 * @param name the name to use as a prefix for generating a new name
	 * @param reservednames a listing of names that should be considered as 'taken' and cannot be generated
	 * by this method
	 * @return a new name based on the specified name.
	 *
	 * @since 3.3
	 */
	public String generateName(String name, Set<String> reservednames) {
		if(name == null) {
			return ((LaunchManager) getLaunchManager()).generateUniqueLaunchConfigurationNameFrom(IInternalDebugCoreConstants.EMPTY_STRING, reservednames);
		}
		return ((LaunchManager)getLaunchManager()).generateUniqueLaunchConfigurationNameFrom(name, reservednames);
	}

	@Override
	public ILaunchConfigurationTab getActiveTab() {
		return fTabViewer.getActiveTab();
	}

	/**
	 * Returns the banner image to display in the title area
	 */
	protected Image getBannerImage() {
		if (fBannerImage == null) {
			ImageDescriptor descriptor = getLaunchGroup().getBannerImageDescriptor();
			if (descriptor != null) {
				fBannerImage = descriptor.createImage();
			}
		}
		return fBannerImage;
	}

	/**
	 * Gets the delete menu action
	 *
	 * @return the delete menu action
	 */
	protected AbstractLaunchConfigurationAction getDeleteAction() {
		return (AbstractLaunchConfigurationAction)fLaunchConfigurationView.getAction(DeleteLaunchConfigurationAction.ID_DELETE_ACTION);
	}

	/**
	 * Gets the filter action
	 * @return the filter menu action
	 * @since 3.2
	 */
	protected IAction getFilterAction() {
		return fLaunchConfigurationView.getAction(FilterLaunchConfigurationAction.ID_FILTER_ACTION);
	}

	/**
	 * Gets the collapse all action
	 * @return the collapse all action
	 * @since 3.2
	 */
	protected IAction getCollapseAllAction() {
		return fLaunchConfigurationView.getAction(CollapseAllLaunchConfigurationAction.ID_COLLAPSEALL_ACTION);
	}

	/*
	 * @see org.eclipse.jface.dialogs.Dialog#getDialogBoundsSettings()
	 *
	 * @since 3.2
	 */
	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		return getDialogSettings();
	}

	@Override
	protected int getDialogBoundsStrategy() {
		return DIALOG_PERSISTSIZE;
	}

	/**
	 * Returns the dialog settings for this dialog. Subclasses should override
	 * <code>getDialogSettingsSectionName()</code>.
	 *
	 * @return IDialogSettings
	 */
	protected IDialogSettings getDialogSettings() {
		IDialogSettings settings = PlatformUI
				.getDialogSettingsProvider(FrameworkUtil.getBundle(LaunchConfigurationDialog.class))
				.getDialogSettings();
		IDialogSettings section = settings.getSection(getDialogSettingsSectionName());
		if (section == null) {
			section = settings.addNewSection(getDialogSettingsSectionName());
		}
		return section;
	}

	/**
	 * Returns the name of the section that this dialog stores its settings in
	 *
	 * @return String
	 */
	protected String getDialogSettingsSectionName() {
		return IDebugUIConstants.PLUGIN_ID + ".LAUNCH_CONFIGURATIONS_DIALOG_SECTION"; //$NON-NLS-1$
	}

	/**
	 * Gets the current display
	 *
	 * @return the display
	 */
	protected Display getDisplay() {
		Shell shell = getShell();
		if (shell != null) {
			return shell.getDisplay();
		}
		return DebugUIPlugin.getStandardDisplay();
	}

	/**
	 * Gets the duplicate menu action
	 *
	 * @return the duplicate menu action
	 */
	protected AbstractLaunchConfigurationAction getDuplicateAction() {
		return (AbstractLaunchConfigurationAction)fLaunchConfigurationView.getAction(DuplicateLaunchConfigurationAction.ID_DUPLICATE_ACTION);
	}

	/**
	 * Gets the export menu action
	 *
	 * @return the export menu action
	 */
	protected AbstractLaunchConfigurationAction getExportAction() {
		return (AbstractLaunchConfigurationAction) fLaunchConfigurationView.getAction(ExportLaunchConfigurationAction.ID_EXPORT_ACTION);
	}

	/**
	 * Gets the help context id
	 *
	 * @return the help context id
	 */
	protected String getHelpContextId() {
		return IDebugHelpContextIds.LAUNCH_CONFIGURATION_DIALOG;
	}

	/**
	 * Returns the status the dialog was opened on or <code>null</code> if none.
	 *
	 * @return IStatus
	 */
	protected IStatus getInitialStatus() {
		return fInitialStatus;
	}

	/**
	 * Return the last launched configuration in the workspace.
	 *
	 * @return the last launched configuration
	 */
	protected ILaunchConfiguration getLastLaunchedWorkbenchConfiguration() {
		return DebugUIPlugin.getDefault().getLaunchConfigurationManager().getLastLaunch(getLaunchGroup().getIdentifier());
	}

	/**
	 * Returns the appropriate text for the launch button - run or debug.
	 *
	 * @return the launch button text
	 */
	protected String getLaunchButtonText() {
		return DebugPlugin.getDefault().getLaunchManager().getLaunchMode(getMode()).getLabel();
	}

	/**
	 * Returns the launch group being displayed.
	 *
	 * @return launch group
	 */
	public LaunchGroupExtension getLaunchGroup() {
		return fGroup;
	}

	@Override
	public String getMode() {
		return getLaunchGroup().getMode();
	}

	/**
	 * Gets the new menu action
	 *
	 * @return the new menu action
	 */
	protected AbstractLaunchConfigurationAction getNewAction() {
		return (AbstractLaunchConfigurationAction)fLaunchConfigurationView.getAction(CreateLaunchConfigurationAction.ID_CREATE_ACTION);
	}

	/**
	 * Gets the new prototype menu action
	 *
	 * @return the new prototype menu action
	 *
	 * @since 3.13
	 */
	protected AbstractLaunchConfigurationAction getNewPrototypeAction() {
		return (AbstractLaunchConfigurationAction) fLaunchConfigurationView.getAction(CreateLaunchConfigurationPrototypeAction.ID_CREATE_PROTOTYPE_ACTION);
	}

	/**
	 * Gets the link prototype menu action
	 *
	 * @return the link prototype menu action
	 *
	 * @since 3.13
	 */
	protected AbstractLaunchConfigurationAction getLinkPrototypeAction() {
		return (AbstractLaunchConfigurationAction) fLaunchConfigurationView.getAction(LinkPrototypeAction.ID_LINK_PROTOTYPE_ACTION);
	}

	/**
	 * Gets the unlink prototype menu action
	 *
	 * @return the unlink prototype menu action
	 *
	 * @since 3.13
	 */
	protected AbstractLaunchConfigurationAction getUnlinkPrototypeAction() {
		return (AbstractLaunchConfigurationAction) fLaunchConfigurationView.getAction(UnlinkPrototypeAction.ID_UNLINK_PROTOTYPE_ACTION);
	}

	/**
	 * Gets the reset with prototype values menu action
	 *
	 * @return the reset with prototype values menu action
	 *
	 * @since 3.13
	 */
	protected AbstractLaunchConfigurationAction getResetWithPrototypeValuesAction() {
		return (AbstractLaunchConfigurationAction) fLaunchConfigurationView.getAction(ResetWithPrototypeValuesAction.ID_RESET_WITH_PROTOTYPE_VALUES_ACTION);
	}

	/**
	 * Returns the reserved name set (if there is one), <code>null</code> otherwise
	 * @return the reserved name set or <code>null</code>
	 * @since 3.3
	 *
	 */
	public Set<String> getReservedNameSet() {
		return fReservedNames;
	}

	/**
	 * returns the open mode
	 *
	 * @return the open mode
	 */
	protected int getOpenMode() {
		return fOpenMode;
	}

	public Object getSelectedPage() {
		return getActiveTab();
	}

	/**
	 * Returns the title of the shell
	 * @return the shell title
	 */
	protected String getShellTitle() {
		String title = null;
		if(getLaunchGroup() != null) {
			title = MessageFormat.format(LaunchConfigurationsMessages.LaunchConfigurationsDialog_configurations, new Object[] { DebugUIPlugin.removeAccelerators(getLaunchGroup().getLabel()) });
		}
		if (title == null) {
			title = LaunchConfigurationsMessages.LaunchConfigurationDialog_Launch_Configurations_18;
		}
		return title;
	}

	/**
	 * Returns the current tab group
	 *
	 * @return the current tab group, or <code>null</code> if none
	 */
	public ILaunchConfigurationTabGroup getTabGroup() {
		if (fTabViewer != null) {
			return fTabViewer.getTabGroup();
		}
		return null;
	}

	@Override
	public ILaunchConfigurationTab[] getTabs() {
		if (getTabGroup() == null) {
			return null;
		}
		return getTabGroup().getTabs();
	}

	/**
	 * Returns the viewer used to display the tabs for a launch configuration.
	 *
	 * @return LaunchConfigurationTabGroupViewer
	 */
	public LaunchConfigurationTabGroupViewer getTabViewer() {
		return fTabViewer;
	}

	/**
	 * Notification the 'Close' button has been pressed.
	 */
	protected void handleClosePressed() {
		if(fTabViewer.canSave()) {
			int status = shouldSaveCurrentConfig();
			if(status != IDialogConstants.CANCEL_ID) {
				if(status != ID_DISCARD_BUTTON) {
					if(status == IDialogConstants.YES_ID) {
						fTabViewer.handleApplyPressed();
					}
					cancelPressed();
				}
			}
		}
		else {
			cancelPressed();
		}
	}

	/**
	 * Notification that selection has changed in the launch configuration tree.
	 * <p>
	 * If the currently displayed configuration is not saved,
	 * prompt for saving before moving on to the new selection.
	 * </p>
	 *
	 * @param event selection changed event
	 */
	protected void handleLaunchConfigurationSelectionChanged(SelectionChangedEvent event) {
		Object input = fTabViewer.getInput();
		Object newInput = null;
		IStructuredSelection selection = event.getStructuredSelection();
		if (selection.size() == 1) {
			newInput = selection.getFirstElement();
		}
		if (!isEqual(input, newInput)) {
			ILaunchConfiguration original = fTabViewer.getOriginal();
			if (original != null && newInput == null && getLaunchManager().getMovedTo(original) != null) {
				return;
			}
			boolean deleted = false;
			if (original != null) {
				deleted = !original.exists();
			}
			boolean renamed = false;
			if (newInput instanceof ILaunchConfiguration) {
				renamed = getLaunchManager().getMovedFrom((ILaunchConfiguration)newInput) != null;
			}
			try {
				fSettingInput = true;
				if (fTabViewer.canSave() && fTabViewer.isDirty() && !deleted && !renamed) {
					if(fLaunchConfigurationView != null) {
						fLaunchConfigurationView.setAutoSelect(false);
					}
					int ret = showUnsavedChangesDialog();
					if(ret == IDialogConstants.YES_ID) {
						fTabViewer.handleApplyPressed();
						ILaunchConfigurationTab[] tabs = getTabs();
						if (tabs != null) {
							for (ILaunchConfigurationTab tab : tabs) {
								tab.postApply();
							}
						}
						fTabViewer.setInput(newInput);
					}
					else if(ret == IDialogConstants.NO_ID) {
						fTabViewer.handleRevertPressed();
						fTabViewer.setInput(newInput);
					}
					else {
						fLaunchConfigurationView.getViewer().setSelection(new StructuredSelection(input));
					}
					fLaunchConfigurationView.setAutoSelect(true);
				}
				else {
					fTabViewer.setInput(newInput);
					if(fTabViewer.isDirty()) {
						fTabViewer.handleApplyPressed();
					}
				}
			} finally {
				fSettingInput = false;
				updateButtons();
				updateMessage();
			}
			if(getShell() != null && getShell().isVisible()) {
				resize();
			}
		}
		}

	/**
	 * Notification the 'launch' button has been pressed. Save and launch.
	 *
	 */
	protected void handleLaunchPressed() {
		ILaunchConfiguration config = fTabViewer.getOriginal();
		if (fTabViewer.isDirty() && fTabViewer.canSave()) {
			config = fTabViewer.handleApplyPressed();
		}
		if(config != null) {
			close();
			DebugUITools.launch(config, getMode(), fIsShift);
		}
		setShift(false);
	}

	/**
	 * Consult a status handler for the given status, if any. The status handler
	 * is passed this launch config dialog as an argument.
	 *
	 * @param status the status to be handled
	 */
	public void handleStatus(IStatus status) {
		IStatusHandler handler = DebugPlugin.getDefault().getStatusHandler(status);
		if (handler != null) {
			try {
				handler.handleStatus(status, this);
				return;
			}
			catch (CoreException e) {status = e.getStatus();}
		}
		// if no handler, or handler failed, display error/warning dialog
		String title = null;
		switch (status.getSeverity()) {
			case IStatus.ERROR:
				title = LaunchConfigurationsMessages.LaunchConfigurationsDialog_Error_1;
				break;
			case IStatus.WARNING:
				title = LaunchConfigurationsMessages.LaunchConfigurationsDialog_Warning_2;
				break;
			default:
				title = LaunchConfigurationsMessages.LaunchConfigurationsDialog_Information_3;
				break;
		}
		ErrorDialog.openError(getShell(), title, null, status);
	}

	@Override
	protected void initializeBounds() {
		IDialogSettings settings = getDialogSettings();
		if (fSashForm != null) {
			int w1, w2;
			try {
				w1 = settings.getInt(DIALOG_SASH_WEIGHTS_1);
				if(w1 < 10) {
					w1 = DEFAULT_SASH_WEIGHTS[0];
				}
				w2 = settings.getInt(DIALOG_SASH_WEIGHTS_2);
				if(w2 < 10) {
					w2 = DEFAULT_SASH_WEIGHTS[1];
				}
			}
			catch(NumberFormatException nfe) {
				w1 = DEFAULT_SASH_WEIGHTS[0];
				w2 = DEFAULT_SASH_WEIGHTS[1];
			}
			fSashForm.setWeights(new int[] {w1, w2});
		}
		super.initializeBounds();
	}

	@Override
	protected Point getInitialSize() {
		try {
			getDialogSettings().getInt("DIALOG_HEIGHT"); //$NON-NLS-1$
			return super.getInitialSize();
		}
		catch(NumberFormatException nfe) {
			return DEFAULT_INITIAL_DIALOG_SIZE;
		}
	}

	/**
	 * Sets the default values for the given {@link LaunchConfigurationWorkingCopy}
	 * @param wc
	 * @since 3.6
	 */
	protected void doSetDefaults(ILaunchConfigurationWorkingCopy wc) {
		try {
			ILaunchConfigurationTabGroup tabGroup = LaunchConfigurationPresentationManager.getDefault().getTabGroup(wc, getMode());
			// this only works because this action is only present when the dialog is open
			ILaunchConfigurationDialog dialog = LaunchConfigurationsDialog.getCurrentlyVisibleLaunchConfigurationDialog();
			tabGroup.createTabs(dialog, dialog.getMode());
			ILaunchConfigurationTab[] tabs = tabGroup.getTabs();
			for (ILaunchConfigurationTab tab : tabs) {
				tab.setLaunchConfigurationDialog(dialog);
			}
			tabGroup.setDefaults(wc);
			tabGroup.dispose();
		} catch (CoreException e) {
			DebugUIPlugin.log(e.getStatus());
		}
	}

	/**
	 * Performs initialization of the content by setting the initial tree selection
	 */
	protected void initializeContent() {
		if(fSetDefaultOnOpen) {
			try {
				Object o = fInitialSelection.getFirstElement();
				if(o instanceof ILaunchConfigurationWorkingCopy) {
					ILaunchConfigurationWorkingCopy wc = (ILaunchConfigurationWorkingCopy) o;
					doSetDefaults(wc);
					setInitialSelection(new StructuredSelection(wc.doSave()));
				}
			} catch (CoreException e) {
				DebugUIPlugin.log(e);
			}
		}
		doInitialTreeSelection();

		IStatus status = getInitialStatus();
		if (status != null) {
			handleStatus(status);
		}
		restoreExpansion();
	}

	/**
	 * Compares two objects to determine their equality
	 *
	 * @param o1 the first object
	 * @param o2 the object to compare to object one
	 * @return true if they are equal, false if object 1 is null, the result of o1.equals(o2) otherwise
	 */
	protected boolean isEqual(Object o1, Object o2) {
		if (o1 == o2) {
			return true;
		} else if (o1 == null) {
			return false;
		} else {
			return o1.equals(o2);
		}
	}

	/**
	 * Returns whether the dialog can be closed
	 *
	 * @return whether the dialog can be closed
	 */
	protected boolean isSafeToClose() {
		return fActiveRunningOperations == 0;
	}

	/**
	 * Determine the initial configuration for this dialog.
	 * Open the dialog in the mode set using #setOpenMode(int) and return one of
	 * <code>Window. OK</code> or <code>Window.CANCEL</code>.
	 *
	 * @see org.eclipse.jface.window.Window#open()
	 * @return the int status of opening the dialog
	 */
	@Override
	public int open() {
		int mode = getOpenMode();
		setCurrentlyVisibleLaunchConfigurationDialog(this);
		if (mode == LAUNCH_CONFIGURATION_DIALOG_OPEN_ON_LAST_LAUNCHED) {
			ILaunchConfiguration lastLaunchedConfig = getLastLaunchedWorkbenchConfiguration();
			if (lastLaunchedConfig != null) {
				setInitialSelection(new StructuredSelection(lastLaunchedConfig));
			}
		}
		return super.open();
	}

	/**
	 * saves which of the nodes are expanded at the time the dialog is closed
	 * @since 3.2
	 */
	protected void persistExpansion() {
		if(fLaunchConfigurationView != null) {
			IDialogSettings settings = getDialogSettings();
			TreeItem[] items = fLaunchConfigurationView.getTreeViewer().getTree().getItems();
			String value = settings.get(DIALOG_EXPANDED_NODES);
			if(value == null) {
				value = IInternalDebugCoreConstants.EMPTY_STRING;
			}
			ArrayList<String> list = new ArrayList<>();
			String[] persisted = value.split(DELIMITER);
			Collections.addAll(list, persisted);
			String type = IInternalDebugCoreConstants.EMPTY_STRING;
			//if the item is not in the list and is expanded add it, otherwise if it
			//is not expanded do a remove...either way for the else we query the list
			for (TreeItem item : items) {
				type = ((ILaunchConfigurationType) item.getData()).getIdentifier();
				if (!list.contains(type) && item.getExpanded()) {
					list.add(type);
				} else if (!item.getExpanded()) {
					list.remove(type);
				}
			}
			value = IInternalDebugCoreConstants.EMPTY_STRING;
			//build the preference string
			for (String segment : list) {
				value += segment + DELIMITER;
			}
			settings.put(DIALOG_EXPANDED_NODES, value);
		}
	}

	/**
	 * Restore the original expansion state of the nodes in the viewer
	 * @since 3.2
	 */
	protected void restoreExpansion() {
		if(fLaunchConfigurationView != null) {
			IDialogSettings settings = getDialogSettings();
			String value = settings.get(DIALOG_EXPANDED_NODES);
			if(value != null) {
				String[] nodes = value.split(DELIMITER);
				TreeItem[] items = fLaunchConfigurationView.getTreeViewer().getTree().getItems();
				HashSet<Object> toexpand = new HashSet<>();
				// if we have a selection make sure it is expanded
				if(fInitialSelection != null && !fInitialSelection.isEmpty()) {
					Object obj = fInitialSelection.getFirstElement();
					if(obj instanceof ILaunchConfigurationType) {
						toexpand.add(obj);
					}
					else if(obj instanceof ILaunchConfiguration) {
						try {
							toexpand.add(((ILaunchConfiguration) obj).getType());
						}
						catch (CoreException e) {DebugUIPlugin.log(e);}
					}
				}
				for (String node : nodes) {
					for (TreeItem item : items) {
						ILaunchConfigurationType type = (ILaunchConfigurationType) item.getData();
						if (type.getIdentifier().equals(node)) {
							toexpand.add(type);
						}
					}
				}
				fLaunchConfigurationView.getTreeViewer().setExpandedElements(toexpand.toArray());
			}
		}
	}

	/**
	 * Save the current sash weights
	 */
	protected void persistSashWeights() {
		IDialogSettings settings = getDialogSettings();
		if (fSashForm != null) {
			int[] sashWeights = fSashForm.getWeights();
			settings.put(DIALOG_SASH_WEIGHTS_1, (sashWeights[0] < 10 ? DEFAULT_SASH_WEIGHTS[0] : sashWeights[0]));
			settings.put(DIALOG_SASH_WEIGHTS_2, (sashWeights[1] < 10 ? DEFAULT_SASH_WEIGHTS[1] : sashWeights[1]));
		}
	}

	/**
	 * Update buttons and message.
	 */
	protected void refreshStatus() {
		updateMessage();
		updateButtons();
	}

	/**
	 * Refresh the launch configurations' tree viewer
	 *
	 * @since 3.13
	 */
	public void refreshLaunchConfigurationView() {
		LaunchConfigurationView view = fLaunchConfigurationView;
		if (view != null) {
			view.getTreeViewer().refresh();
		}
	}

	/**
	 * resize the dialog to show all relevant content
	 */
	protected void resize() {
		if(getTabGroup() != null) {
			Point shell = getShell().getSize();
			int maxx = (int)(getDisplay().getBounds().width * MAX_DIALOG_WIDTH_PERCENT),
				maxy = (int) (getDisplay().getBounds().height * MAX_DIALOG_HEIGHT_PERCENT);
			maxx = (maxx < DEFAULT_INITIAL_DIALOG_SIZE.x ? DEFAULT_INITIAL_DIALOG_SIZE.x : maxx);
			maxy = (maxy < DEFAULT_INITIAL_DIALOG_SIZE.y ? DEFAULT_INITIAL_DIALOG_SIZE.y : maxy);
			Point psize = getShell().computeSize(SWT.DEFAULT, maxy);
			if((psize.x > maxx ? maxx : psize.x) > shell.x || (psize.y > maxy ? maxy : psize.y) > shell.y) {
				setShellSize(Math.min(psize.x, maxx), Math.min(psize.y, maxy));
				constrainShellSize();
			}
		}
	}

	@Override
	public void run(boolean fork, boolean cancelable, IRunnableWithProgress runnable) throws InvocationTargetException, InterruptedException {
		if (getShell() != null && getShell().isVisible()) {
			// Save focus control
			fLastControl = getShell().getDisplay().getFocusControl();
			if (fLastControl != null && fLastControl.getShell() != getShell()) {
				fLastControl = null;
			}
			// Attach the progress monitor part to the cancel button
			fProgressMonitorPart.attachToCancelComponent(null);
			fProgressMonitorPart.getParent().setVisible(true);
			fActiveRunningOperations++;

		//do work here collecting enabled states, otherwise to get these states we would need to
		//perform the validation of the dialog again, which is expensive and would cause flashing of widgets.
			Control[] children = ((Composite)fButtonComp.getChildren()[0]).getChildren();
			boolean[] prev = new boolean[children.length+2];
			prev[0] = fTabViewer.getApplyButton().isEnabled();
			prev[1] = fTabViewer.getRevertButton().isEnabled();
			for(int i = 0; i < children.length; i++) {
				prev[i+2] = children[i].isEnabled();
			}
			try {
				updateRunnnableControls(false, prev);
				ModalContext.run(runnable, fork, fProgressMonitorPart, getShell().getDisplay());
			}
			finally {
				fActiveRunningOperations--;
				updateRunnnableControls(true, prev);
				if (getShell() != null) {
					fProgressMonitorPart.getParent().setVisible(false);
					fProgressMonitorPart.removeFromCancelComponent(null);
					if (fLastControl != null && !fLastControl.isDisposed()) {
						fLastControl.setFocus();
					}
				}
			}
		}
		else {
			PlatformUI.getWorkbench().getProgressService().run(fork, cancelable, runnable);
		}
	}

	/**
	 * Updates the enablement of the runnable controls to appear disabled as a job is running
	 * @param enabled the desired enable status of the dialog area, revert//apply buttons, and
	 * @param prev the previous settings for the apply and revert buttons to be reset to, only takes effect if enable is set to true
	 * any children of the button bar
	 * @since 3.3.0
	 */
	private void updateRunnnableControls(boolean enabled, boolean[] prev) {
		fTabViewer.getApplyButton().setEnabled(enabled ? prev[0] : enabled);
		fTabViewer.getRevertButton().setEnabled(enabled ? prev[1] : enabled);
		//the arrangement never differs: button comp has one child that holds all the buttons
		Control[] children = ((Composite)fButtonComp.getChildren()[0]).getChildren();
		for(int i = 0; i < children.length; i++) {
			children[i].setEnabled(enabled ? prev[i+2] : enabled);
		}
		getDialogArea().setEnabled(enabled);
	}

	@Override
	public void setActiveTab(ILaunchConfigurationTab tab) {
		fTabViewer.setActiveTab(tab);
	}

	@Override
	public void setActiveTab(int index) {
		fTabViewer.setActiveTab(index);
	}

	/**
	 * Sets the initial selection for the dialog when opened in
	 * <code>LAUNCH_CONFIGURATION_DIALOG_OPEN_ON_SELECTION</code> mode.
	 */
	public void setInitialSelection(IStructuredSelection selection) {
		fInitialSelection = selection;
	}

	/**
	 * Sets the status to open the dialog on.
	 *
	 * @param status the initial status for the dialog
	 */
	public void setInitialStatus(IStatus status) {
		fInitialStatus = status;
	}

	/**
	 * Sets whether the tab group should set default values in the launch configuration
	 * when the dialog is opened. If this method is not called, default values are not
	 * set.
	 *
	 * @param setDefaults whether to set default values
	 * @since 3.6
	 */
	public void setDefaultsOnOpen(boolean setDefaults) {
		fSetDefaultOnOpen = setDefaults;
	}

	/**
	 * Returns if the dialog is supposed to be setting the default values for
	 * the initial configuration when it opens
	 *
	 * @return <code>true</code> if the defaults should be set on open, <code>false</code> otherwise
	 * @since 3.6
	 */
	public boolean shouldSetDefaultsOnOpen() {
		return fSetDefaultOnOpen;
	}

	/**
	 * Sets the launch group to display.
	 *
	 * @param group launch group
	 */
	protected void setLaunchGroup(LaunchGroupExtension group) {
		fGroup = group;
	}

	/**
	 * Set the title area image based on the mode this dialog was initialized with
	 */
	protected void setModeLabelState() {
		setTitleImage(getBannerImage());
	}

	@Override
	public void setName(String name) {
		fTabViewer.setName(name);
	}

	/**
	 * Returns the current launch manager
	 * @return the current launch manager
	 */
	protected ILaunchManager getLaunchManager() {
		return DebugPlugin.getDefault().getLaunchManager();
	}

	/**
	 * Set the flag indicating how this dialog behaves when the <code>open()</code> method is called.
	 * Valid values are defined by the LAUNCH_CONFIGURATION_DIALOG... constants in this class.
	 */
	public void setOpenMode(int mode) {
		fOpenMode = mode;
	}

	/**
	 * Increase the size of this dialog's <code>Shell</code> by the specified amounts.
	 * Do not increase the size of the Shell beyond the bounds of the Display.
	 */
	protected void setShellSize(int width, int height) {
		Rectangle bounds = getShell().getMonitor().getBounds();
		getShell().setSize(Math.min(width, bounds.width), Math.min(height, bounds.height));
	}

	/**
	 * Sets the viewer used to display the tabs for a launch configuration.
	 *
	 * @param viewer the new view to set
	 */
	protected void setTabViewer(LaunchConfigurationTabGroupViewer viewer) {
		fTabViewer = viewer;
	}

	/**
	 * Create and return a dialog that asks the user whether they want to discard
	 * unsaved changes.
	 *
	 * @return the return code based on the button selected.
	 * The value will be one of <code>YES_ID</code> or <code>NO_ID</code> from
	 * <code>IDialogConstants</code>.
	 */
	private int showDiscardChangesDialog() {
		StringBuilder buffer = new StringBuilder(MessageFormat.format(LaunchConfigurationsMessages.LaunchConfigurationDialog_The_configuration___35, new Object[] { fTabViewer.getWorkingCopy().getName() }));
		buffer.append(fTabViewer.getErrorMesssage());
		buffer.append(LaunchConfigurationsMessages.LaunchConfigurationDialog_Do_you_wish_to_discard_changes_37);
		MessageDialog dialog = new MessageDialog(getShell(),
												 LaunchConfigurationsMessages.LaunchConfigurationDialog_Discard_changes__38,
												 null,
												 buffer.toString(),
												 MessageDialog.QUESTION,
				new String[] {
						LaunchConfigurationsMessages.LaunchConfigurationDialog_Discard_Button,
						LaunchConfigurationsMessages.LaunchConfigurationDialog_Dont_Discard_Button },
												 1);
		int val = IDialogConstants.NO_ID;
		if (dialog.open() == 0) {
			if (fLaunchConfigurationView != null) {
				fLaunchConfigurationView.setAutoSelect(false);
			}
			fTabViewer.handleRevertPressed();
			val = IDialogConstants.YES_ID;
			if (fLaunchConfigurationView != null) {
				fLaunchConfigurationView.setAutoSelect(true);
			}
		}
		if(val == IDialogConstants.NO_ID) {
			val = ID_DISCARD_BUTTON;
		}
		return val;
	}

	/**
	 * Create and return a dialog that asks the user whether they want to save
	 * unsaved changes.
	 *
	 * @return the return code based on the button selected.
	 * The value will be one of <code>YES_ID</code>, <code>NO_ID</code>, or <code>CANCEL_ID</code>, from
	 * <code>IDialogConstants</code>.
	 */
	private int showSaveChangesDialog() {
		String message = MessageFormat.format(LaunchConfigurationsMessages.LaunchConfigurationDialog_The_configuration___29, new Object[] { fTabViewer.getWorkingCopy().getName() });
		MessageDialog dialog = new MessageDialog(getShell(),
												 LaunchConfigurationsMessages.LaunchConfigurationFilteredTree_save_changes,
												 null,
												 message,
												 MessageDialog.QUESTION,
				new String[] {
						LaunchConfigurationsMessages.LaunchConfigurationDialog_Save_Button,
						LaunchConfigurationsMessages.LaunchConfigurationDialog_Dont_Save_Button,
						LaunchConfigurationsMessages.LaunchConfigurationsDialog_c_ancel },
												 0);
		int ret = dialog.open();
		int val = IDialogConstants.CANCEL_ID;
		if (ret == 0 || ret == 1) {
			if (ret == 0) {
				val = IDialogConstants.YES_ID;
			}
			else {
				val = IDialogConstants.NO_ID;
			}
		}
		return val;
	}

	/**
	 * Show the user a dialog appropriate to whether the unsaved changes in the current config
	 * can be saved or not.  Return <code>true</code> if the user indicated that they wish to replace
	 * the current config, either by saving changes or by discarding the, return <code>false</code>
	 * otherwise.
	 *
	 * @return returns the <code>showSaveChangesDialog</code> return value
	 */
	private int showUnsavedChangesDialog() {
		if (fTabViewer.canSave()) {
			return showSaveChangesDialog();
		}
		return showDiscardChangesDialog();
	}

	@Override
	public void updateButtons() {
		if (!fSettingInput) {
			// New, Delete, & Duplicate toolbar actions
	 		getNewAction().setEnabled(getNewAction().isEnabled());
			getExportAction().setEnabled(getExportAction().isEnabled());
			getDeleteAction().setEnabled(getDeleteAction().isEnabled());
			getDuplicateAction().setEnabled(getDuplicateAction().isEnabled());
			getLinkPrototypeAction().setEnabled(getLinkPrototypeAction().isEnabled());
			getUnlinkPrototypeAction().setEnabled(getUnlinkPrototypeAction().isEnabled());
			getResetWithPrototypeValuesAction().setEnabled(getResetWithPrototypeValuesAction().isEnabled());
			fTabViewer.refresh();
			getButton(ID_LAUNCH_BUTTON).setEnabled(
					fTabViewer.canLaunch() && fTabViewer.canLaunchWithModes() && !fTabViewer.hasDuplicateDelegates());
		} else {
			fTabViewer.refresh();
		}
	}

	@Override
	public void updateMessage() {
		if (!fSettingInput) {
			setErrorMessage(fTabViewer.getErrorMesssage());
			if (fTabViewer.getWarningMessage() != null) {
				setMessage(fTabViewer.getWarningMessage(), IMessageProvider.WARNING);
			} else {
				setMessage(fTabViewer.getMessage());
			}
		}
	}

	/**
	 * Returns if there is a selection in the tree viewer or not
	 * @return true if something in the tree is selected, false otherwise
	 * @since 3.2
	 */
	public boolean isTreeSelectionEmpty() {
		return fLaunchConfigurationView.getTreeViewer().getSelection().isEmpty();
	}

	@Override
	public void propertyChange(final PropertyChangeEvent event) {
		WorkbenchJob job = new WorkbenchJob(IInternalDebugCoreConstants.EMPTY_STRING) {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				TreeViewer viewer = fLaunchConfigurationView.getTreeViewer();
				boolean newvalue = Boolean.parseBoolean(event.getNewValue().toString());
				if(event.getProperty().equals(IInternalDebugUIConstants.PREF_FILTER_LAUNCH_CLOSED)) {
					updateFilter(newvalue, fClosedProjectFilter);
				}
				else if(event.getProperty().equals(IInternalDebugUIConstants.PREF_FILTER_LAUNCH_DELETED)) {
					updateFilter(newvalue, fDeletedProjectFilter);
				}
				else if(event.getProperty().equals(IInternalDebugUIConstants.PREF_FILTER_LAUNCH_TYPES)) {
					updateFilter(newvalue, fLCTFilter);
				}
				else if(event.getProperty().equals(IInternalDebugUIConstants.PREF_FILTER_WORKING_SETS)) {
					updateFilter(newvalue, fWorkingSetsFilter);
				}
				else if(event.getProperty().equals(IInternalDebugUIConstants.PREF_FILTER_TYPE_LIST)) {
					if(DebugUIPlugin.getDefault().getPreferenceStore().getBoolean(IInternalDebugUIConstants.PREF_FILTER_LAUNCH_TYPES)) {
						viewer.refresh();
						fLaunchConfigurationView.updateFilterLabel();
					}
				}

				return Status.OK_STATUS;
			}
		};
		job.runInUIThread(new NullProgressMonitor());
	}

	/**
	 * Updates the state of a filter based on the state variable
	 * @param state if the filter needs to be added or removed, true indicates add, false indicates remove
	 * @param filter the filter to update
	 */
	private void updateFilter(boolean state, ViewerFilter filter) {
		TreeViewer viewer = (TreeViewer)fLaunchConfigurationView.getViewer();
		if(state) {
			viewer.addFilter(filter);
		}
		else {
			viewer.removeFilter(filter);
		}
		fLaunchConfigurationView.updateFilterLabel();
	}
}
