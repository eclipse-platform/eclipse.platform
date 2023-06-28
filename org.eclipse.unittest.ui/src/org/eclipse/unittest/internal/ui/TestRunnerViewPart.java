/*******************************************************************************
L * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.unittest.internal.ui;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.unittest.internal.UnitTestPlugin;
import org.eclipse.unittest.internal.UnitTestPreferencesConstants;
import org.eclipse.unittest.internal.model.ITestRunSessionListener;
import org.eclipse.unittest.internal.model.ITestSessionListener;
import org.eclipse.unittest.internal.model.ProgressState;
import org.eclipse.unittest.internal.model.TestCaseElement;
import org.eclipse.unittest.internal.model.TestElement;
import org.eclipse.unittest.internal.model.TestRunSession;
import org.eclipse.unittest.internal.model.UnitTestModel;
import org.eclipse.unittest.internal.ui.history.History;
import org.eclipse.unittest.internal.ui.history.HistoryHandler;
import org.eclipse.unittest.model.ITestCaseElement;
import org.eclipse.unittest.model.ITestElement;
import org.eclipse.unittest.model.ITestElement.FailureTrace;
import org.eclipse.unittest.model.ITestElement.Result;
import org.eclipse.unittest.model.ITestRunSession;
import org.eclipse.unittest.model.ITestSuiteElement;
import org.eclipse.unittest.ui.ITestViewSupport;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.URLTransfer;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ILock;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.statushandlers.StatusManager;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.actions.EditLaunchConfigurationAction;

import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;

/**
 * A ViewPart that shows the results of a test run.
 */
@SuppressWarnings("restriction")
public class TestRunnerViewPart extends ViewPart {

	/**
	 * An identifier of Test Runner View Part
	 */
	public static final String NAME = UnitTestPlugin.PLUGIN_ID + ".ResultView"; //$NON-NLS-1$

	private static final String RERUN_LAST_COMMAND = UnitTestPlugin.PLUGIN_ID + ".UnitTestShortcut.rerunLast"; //$NON-NLS-1$
	private static final String RERUN_FAILED_CASES_COMMAND = UnitTestPlugin.PLUGIN_ID
			+ ".UnitTestShortcut.rerunFailedCases"; //$NON-NLS-1$

	static final int REFRESH_INTERVAL = 200;

	/**
	 * A Test Result layout
	 */
	public enum TestResultsLayout {
		FLAT, HIERARCHICAL
	}

	/**
	 * Whether the output scrolls and reveals tests as they are executed.
	 */
	protected boolean fAutoScroll = true;
	/**
	 * The current orientation; either <code>VIEW_ORIENTATION_HORIZONTAL</code>
	 * <code>VIEW_ORIENTATION_VERTICAL</code>, or
	 * <code>VIEW_ORIENTATION_AUTOMATIC</code>.
	 */
	private int fOrientation = VIEW_ORIENTATION_AUTOMATIC;
	/**
	 * The current orientation; either <code>VIEW_ORIENTATION_HORIZONTAL</code>
	 * <code>VIEW_ORIENTATION_VERTICAL</code>.
	 */
	private int fCurrentOrientation;
	/**
	 * The current layout mode (LAYOUT_FLAT or LAYOUT_HIERARCHICAL).
	 */
	private TestResultsLayout fLayout = TestResultsLayout.HIERARCHICAL;

	private UnitTestProgressBar fProgressBar;
	private ProgressIcons fProgressImages;
	protected Image fViewImage;
	private CounterPanel fCounterPanel;
	protected boolean fShowOnErrorOnly = false;
	protected Clipboard fClipboard;
	protected volatile String fInfoMessage;

	private FailureTraceUIBlock fFailureTrace;

	private TestViewer fTestViewer;
	/**
	 * Is the UI disposed?
	 */
	private boolean fIsDisposed = false;

	/**
	 * Actions
	 */
	private Action fNextAction;
	private Action fPreviousAction;

	private StopAction fStopAction;
	private UnitTestCopyAction fCopyAction;
	private Action fPasteAction;

	private Action fRerunLastTestAction;
	private IHandlerActivation fRerunLastActivation;
	private Action fRerunFailedCasesAction;
	private IHandlerActivation fRerunFailedFirstActivation;
	private EditLaunchConfigurationAction fEditLaunchConfigAction;

	private Action fFailuresOnlyFilterAction;
	private Action fIgnoredOnlyFilterAction;
	private ScrollLockAction fScrollLockAction;
	private ToggleOrientationAction[] fToggleOrientationActions;
	private ShowTestHierarchyAction fShowTestHierarchyAction;
	private ShowTimeAction fShowTimeAction;
	private ActivateOnErrorAction fActivateOnErrorAction;
	private IMenuListener fViewMenuListener;

	private TestRunSession fTestRunSession;
	private TestSessionListener fTestSessionListener;

//	private RunnerViewHistory fViewHistory;
	private TestRunSessionListener fTestRunSessionListener;

	final Image fStackViewIcon;
	final Image fTestRunOKIcon;
	final Image fTestRunFailIcon;
	final Image fTestRunOKDirtyIcon;
	final Image fTestRunFailDirtyIcon;

	final Image fTestIcon;
	final Image fTestOkIcon;
	final Image fTestErrorIcon;
	final Image fTestFailIcon;
	final Image fTestAssumptionFailureIcon;
	final Image fTestRunningIcon;
	final Image fTestIgnoredIcon;

	final ImageDescriptor fSuiteIconDescriptor = Images.getImageDescriptor("obj16/tsuite.png"); //$NON-NLS-1$
	final ImageDescriptor fSuiteOkIconDescriptor = Images.getImageDescriptor("obj16/tsuiteok.png"); //$NON-NLS-1$
	final ImageDescriptor fSuiteErrorIconDescriptor = Images.getImageDescriptor("obj16/tsuiteerror.png"); //$NON-NLS-1$
	final ImageDescriptor fSuiteFailIconDescriptor = Images.getImageDescriptor("obj16/tsuitefail.png"); //$NON-NLS-1$
	final ImageDescriptor fSuiteRunningIconDescriptor = Images.getImageDescriptor("obj16/tsuiterun.png"); //$NON-NLS-1$

	final Image fSuiteIcon;
	final Image fSuiteOkIcon;
	final Image fSuiteErrorIcon;
	final Image fSuiteFailIcon;
	final Image fSuiteRunningIcon;

	final List<Image> fImagesToDispose;

	// Persistence tags.
	static final String TAG_PAGE = "page"; //$NON-NLS-1$
	static final String TAG_RATIO = "ratio"; //$NON-NLS-1$
	static final String TAG_TRACEFILTER = "tracefilter"; //$NON-NLS-1$
	static final String TAG_ORIENTATION = "orientation"; //$NON-NLS-1$
	static final String TAG_SCROLL = "scroll"; //$NON-NLS-1$
	/**
	 */
	static final String TAG_LAYOUT = "layout"; //$NON-NLS-1$
	/**
	 */
	static final String TAG_FAILURES_ONLY = "failuresOnly"; //$NON-NLS-1$

	/**
	 */
	static final String TAG_IGNORED_ONLY = "ignoredOnly"; //$NON-NLS-1$
	/**
	 */
	static final String TAG_SHOW_TIME = "time"; //$NON-NLS-1$

	/**
	 */
	static final String PREF_LAST_PATH = "lastImportExportPath"; //$NON-NLS-1$

	/**
	 */
	static final String PREF_LAST_URL = "lastImportURL"; //$NON-NLS-1$

	// orientations
	static final int VIEW_ORIENTATION_VERTICAL = 0;
	static final int VIEW_ORIENTATION_HORIZONTAL = 1;
	static final int VIEW_ORIENTATION_AUTOMATIC = 2;

	private IMemento fMemento;

	Image fOriginalViewImage;

	private SashForm fSashForm;

	private Composite fCounterComposite;
	private Composite fParent;

	/**
	 * A Job that periodically updates view description, counters, and progress bar.
	 */
	private UpdateUIJob fUpdateJob;

	/**
	 * A Job that runs as long as a test run is running. It is used to show busyness
	 * for running jobs in the view (title in italics).
	 */
	private UnitTestIsRunningJob fUnitTestIsRunningJob;
	private ILock fUnitTestIsRunningLock;
	public static final Object FAMILY_UNITTEST_RUN = new Object();

	private IPartListener2 fPartListener = new IPartListener2() {
		@Override
		public void partActivated(IWorkbenchPartReference ref) {
		}

		@Override
		public void partBroughtToTop(IWorkbenchPartReference ref) {
		}

		@Override
		public void partInputChanged(IWorkbenchPartReference ref) {
		}

		@Override
		public void partClosed(IWorkbenchPartReference ref) {
		}

		@Override
		public void partDeactivated(IWorkbenchPartReference ref) {
		}

		@Override
		public void partOpened(IWorkbenchPartReference ref) {
		}

		@Override
		public void partVisible(IWorkbenchPartReference ref) {
			if (getSite().getId().equals(ref.getId())) {
				fPartIsVisible = true;
			}
		}

		@Override
		public void partHidden(IWorkbenchPartReference ref) {
			if (getSite().getId().equals(ref.getId())) {
				fPartIsVisible = false;
			}
		}
	};

	protected boolean fPartIsVisible = false;

	private static class UnitTesttPasteAction extends Action {
		private final Shell fShell;
		private Clipboard fClipboard;

		public UnitTesttPasteAction(Shell shell, Clipboard clipboard) {
			super(Messages.TestRunnerViewPart_PasteAction_label);
			Assert.isNotNull(clipboard);
			fShell = shell;
			fClipboard = clipboard;
		}

		@Override
		public void run() {
			String urlData = (String) fClipboard.getContents(URLTransfer.getInstance());
			if (urlData == null) {
				urlData = (String) fClipboard.getContents(TextTransfer.getInstance());
			}
			if (urlData != null && urlData.length() > 0) {
				if (isValidUrl(urlData)) {
					importTestRunSession(urlData);
					return;
				}
			}
			MessageDialog.openInformation(fShell, Messages.TestRunnerViewPart_PasteAction_cannotpaste_title,
					Messages.TestRunnerViewPart_PasteAction_cannotpaste_message);
		}

		private boolean isValidUrl(String urlData) {
			try {
				@SuppressWarnings("unused")
				URL url = new URL(urlData);
			} catch (MalformedURLException e) {
				return false;
			}
			return true;
		}
	}

	private class TestRunSessionListener implements ITestRunSessionListener {
		@Override
		public void sessionAdded(final ITestRunSession testRunSession) {
			getDisplay().asyncExec(() -> {
				if (UnitTestUIPreferencesConstants.getShowInAllViews()
						|| getSite().getWorkbenchWindow() == PlatformUI.getWorkbench().getActiveWorkbenchWindow()) {
					if (fInfoMessage == null) {
						String testRunLabel = BasicElementLabels
								.getJavaElementName(((TestRunSession) testRunSession).getTestRunName());
						String msg;
						if (testRunSession.getLaunch() != null) {
							msg = MessageFormat.format(Messages.TestRunnerViewPart_Launching, testRunLabel);
						} else {
							msg = testRunLabel;
						}
						registerInfoMessage(msg);
					}

					setActiveTestRunSession((TestRunSession) testRunSession);
				}
			});
		}

		@Override
		public void sessionRemoved(final ITestRunSession testRunSession) {
			getDisplay().asyncExec(() -> {
				if (testRunSession.equals(fTestRunSession)) {
					List<TestRunSession> testRunSessions = UnitTestModel.getInstance().getTestRunSessions();
					if (!testRunSessions.isEmpty()) {
						setActiveTestRunSession(testRunSessions.get(0));
					} else {
						setActiveTestRunSession(null);
					}
				}
			});
		}
	}

	private class TestSessionListener implements ITestSessionListener {
		@Override
		public void sessionStarted() {
			fTestViewer.registerViewersRefresh();
			fShowOnErrorOnly = getShowOnErrorOnly();

			startUpdateJobs();

			fStopAction.setEnabled(true);
			fRerunLastTestAction.setEnabled(true);
			fEditLaunchConfigAction.setEnabled(fTestRunSession.getLaunch() != null);
		}

		@Override
		public void sessionCompleted(Duration duration) {
			deregisterTestSessionListener();

			fTestViewer.registerAutoScrollTarget(null);

			final String msg = MessageFormat.format(Messages.TestRunnerViewPart_message_finish,
					Double.valueOf(duration != null ? duration.toNanos() / 1.0e9 : 0));
			getDisplay().asyncExec(() -> registerInfoMessage(msg));

			postSyncRunnable(() -> {
				if (isDisposed())
					return;
				fStopAction.setEnabled(lastLaunchStillRunning());
				updateRerunFailedFirstAction();
				processChangesInUI();
				if (hasErrorsOrFailures()) {
					selectFirstFailure();
				}
				/*
				 * if (fDirtyListener == null) { fDirtyListener= new DirtyListener();
				 * JavaCore.addElementChangedListener(fDirtyListener); }
				 */
				warnOfContentChange();
			});
			stopUpdateJobs();
			showMessageIfNoTests();
		}

		@Override
		public void sessionAborted(Duration duration) {
			deregisterTestSessionListener();

			fTestViewer.registerAutoScrollTarget(null);

			getDisplay().asyncExec(() -> registerInfoMessage(Messages.TestRunnerViewPart_message_stopped));
			handleStopped();
		}

		@Override
		public void runningBegins() {
			if (!fShowOnErrorOnly)
				postShowTestResultsView();
		}

		@Override
		public void testStarted(ITestCaseElement testCaseElement) {
			fTestViewer.registerAutoScrollTarget(testCaseElement);
			fTestViewer.registerViewerUpdate(testCaseElement);
			registerInfoMessage(testCaseElement.getDisplayName());
		}

		@Override
		public void testFailed(ITestElement testElement, ITestElement.Result status, FailureTrace trace) {
			if (isAutoScroll()) {
				fTestViewer.registerFailedForAutoScroll(testElement);
			}
			fTestViewer.registerViewerUpdate(testElement);

			// show the view on the first error only
			if (fShowOnErrorOnly && (getErrorsPlusFailures() == 1))
				postShowTestResultsView();
		}

		@Override
		public void testEnded(ITestCaseElement testCaseElement) {
			fTestViewer.registerViewerUpdate(testCaseElement);
		}

		@Override
		public void testAdded(ITestElement testElement) {
			fTestViewer.registerTestAdded(testElement);
		}
	}

	private class UpdateUIJob extends UIJob {
		private boolean fRunning = true;

		public UpdateUIJob(String name) {
			super(name);
			setSystem(true);
		}

		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			if (!isDisposed()) {
				processChangesInUI();
			}
			schedule(REFRESH_INTERVAL);
			return Status.OK_STATUS;
		}

		public void stop() {
			fRunning = false;
		}

		@Override
		public boolean shouldSchedule() {
			return fRunning;
		}
	}

	private class UnitTestIsRunningJob extends Job {
		public UnitTestIsRunningJob(String name) {
			super(name);
			setSystem(true);
		}

		@Override
		public IStatus run(IProgressMonitor monitor) {
			// wait until the test run terminates
			fUnitTestIsRunningLock.acquire();
			return Status.OK_STATUS;
		}

		@Override
		public boolean belongsTo(Object family) {
			return family == TestRunnerViewPart.FAMILY_UNITTEST_RUN;
		}
	}

	private class StopAction extends Action {
		public StopAction() {
			setText(Messages.TestRunnerViewPart_stopaction_text);
			setToolTipText(Messages.TestRunnerViewPart_stopaction_tooltip);
			Images.setLocalImageDescriptors(this, "stop.png"); //$NON-NLS-1$
		}

		@Override
		public void run() {
			stopTest();
			setEnabled(false);
		}
	}

	private class RerunLastAction extends Action {
		public RerunLastAction() {
			setText(Messages.TestRunnerViewPart_rerunaction_label);
			setToolTipText(Messages.TestRunnerViewPart_rerunaction_tooltip);
			Images.setLocalImageDescriptors(this, "relaunch.png"); //$NON-NLS-1$
			setEnabled(false);
			setActionDefinitionId(RERUN_LAST_COMMAND);
		}

		@Override
		public void run() {
			DebugUITools.launch(fTestRunSession.getLaunch().getLaunchConfiguration(), rerunLaunchMode());
		}
	}

	private class RerunFailedCasesAction extends Action {
		public RerunFailedCasesAction() {
			setText(Messages.TestRunnerViewPart_rerunfailuresaction_label);
			setToolTipText(Messages.TestRunnerViewPart_rerunfailuresaction_tooltip);
			Images.setLocalImageDescriptors(this, "relaunchf.png"); //$NON-NLS-1$
			setEnabled(false);
			setActionDefinitionId(RERUN_FAILED_CASES_COMMAND);
		}

		@Override
		public void run() {
			rerunFailedTestCases();
		}
	}

	private class ToggleOrientationAction extends Action {
		private final int fActionOrientation;

		public ToggleOrientationAction(int orientation) {
			super("", AS_RADIO_BUTTON); //$NON-NLS-1$
			switch (orientation) {
			case TestRunnerViewPart.VIEW_ORIENTATION_HORIZONTAL:
				setText(Messages.TestRunnerViewPart_toggle_horizontal_label);
				setImageDescriptor(Images.getImageDescriptor("elcl16/th_horizontal.png")); //$NON-NLS-1$
				break;
			case TestRunnerViewPart.VIEW_ORIENTATION_VERTICAL:
				setText(Messages.TestRunnerViewPart_toggle_vertical_label);
				setImageDescriptor(Images.getImageDescriptor("elcl16/th_vertical.png")); //$NON-NLS-1$
				break;
			case TestRunnerViewPart.VIEW_ORIENTATION_AUTOMATIC:
				setText(Messages.TestRunnerViewPart_toggle_automatic_label);
				setImageDescriptor(Images.getImageDescriptor("elcl16/th_automatic.png")); //$NON-NLS-1$
				break;
			default:
				break;
			}
			fActionOrientation = orientation;
			PlatformUI.getWorkbench().getHelpSystem().setHelp(this,
					IUnitTestHelpContextIds.RESULTS_VIEW_TOGGLE_ORIENTATION_ACTION);
		}

		public int getOrientation() {
			return fActionOrientation;
		}

		@Override
		public void run() {
			if (isChecked()) {
				fOrientation = fActionOrientation;
				computeOrientation();
			}
		}
	}

	private class SortAction extends Action {
		private final boolean enableAlphabeticalSort;

		public SortAction(boolean enableAlphabeticalSort) {
			super(enableAlphabeticalSort ? Messages.TestRunnerViewPart_sortAlphabetical
					: Messages.TestRunnerViewPart_sortRunner, AS_RADIO_BUTTON);
			this.enableAlphabeticalSort = enableAlphabeticalSort;
		}

		@Override
		public void run() {
			fTestViewer.setAlphabeticalSort(enableAlphabeticalSort);
		}

		@Override
		public boolean isChecked() {
			return fTestViewer.isAlphabeticalSort() == enableAlphabeticalSort;
		}
	}

	private class FailuresOnlyFilterAction extends Action {
		public FailuresOnlyFilterAction() {
			super(Messages.TestRunnerViewPart_show_failures_only, AS_CHECK_BOX);
			setToolTipText(Messages.TestRunnerViewPart_show_failures_only);
			setImageDescriptor(Images.getImageDescriptor("obj16/failures.png")); //$NON-NLS-1$
		}

		@Override
		public void run() {
			setShowFailuresOnly(isChecked());
		}
	}

	private class IgnoredOnlyFilterAction extends Action {
		public IgnoredOnlyFilterAction() {
			super(Messages.TestRunnerViewPart_show_ignored_only, AS_CHECK_BOX);
			setToolTipText(Messages.TestRunnerViewPart_show_ignored_only);
			setImageDescriptor(Images.getImageDescriptor("obj16/testignored.png")); //$NON-NLS-1$
		}

		@Override
		public void run() {
			setShowIgnoredOnly(isChecked());
		}
	}

	private class ShowTimeAction extends Action {
		public ShowTimeAction() {
			super(Messages.TestRunnerViewPart_show_execution_time, IAction.AS_CHECK_BOX);
		}

		@Override
		public void run() {
			setShowExecutionTime(isChecked());
		}
	}

	private class ShowTestHierarchyAction extends Action {
		public ShowTestHierarchyAction() {
			super(Messages.TestRunnerViewPart_hierarchical_layout, IAction.AS_CHECK_BOX);
			setImageDescriptor(Images.getImageDescriptor("elcl16/hierarchicalLayout.png")); //$NON-NLS-1$
		}

		@Override
		public void run() {
			setFilterAndLayout(fFailuresOnlyFilterAction.isChecked(), fIgnoredOnlyFilterAction.isChecked(),
					isChecked() ? TestResultsLayout.HIERARCHICAL : TestResultsLayout.FLAT);
		}
	}

	private class ActivateOnErrorAction extends Action {
		public ActivateOnErrorAction() {
			super(Messages.TestRunnerViewPart_activate_on_failure_only, IAction.AS_CHECK_BOX);
			// setImageDescriptor(UnitTestPlugin.getImageDescriptor("obj16/failures.png"));
			// //$NON-NLS-1$
			update();
		}

		public void update() {
			setChecked(getShowOnErrorOnly());
		}

		@Override
		public void run() {
			boolean checked = isChecked();
			fShowOnErrorOnly = checked;
			InstanceScope.INSTANCE.getNode(UnitTestPlugin.PLUGIN_ID)
					.putBoolean(UnitTestPreferencesConstants.SHOW_ON_ERROR_ONLY, checked);
		}
	}

	/**
	 * Constructs Test Runner View part object
	 */
	public TestRunnerViewPart() {
		fImagesToDispose = new ArrayList<>();

		fStackViewIcon = createManagedImage("eview16/stackframe.png");//$NON-NLS-1$
		fTestRunOKIcon = createManagedImage("eview16/unitsucc.png"); //$NON-NLS-1$
		fTestRunFailIcon = createManagedImage("eview16/uniterr.png"); //$NON-NLS-1$
		fTestRunOKDirtyIcon = createManagedImage("eview16/unitsuccq.png"); //$NON-NLS-1$
		fTestRunFailDirtyIcon = createManagedImage("eview16/uniterrq.png"); //$NON-NLS-1$

		fTestIcon = createManagedImage("obj16/test.png"); //$NON-NLS-1$
		fTestOkIcon = createManagedImage("obj16/testok.png"); //$NON-NLS-1$
		fTestErrorIcon = createManagedImage("obj16/testerr.png"); //$NON-NLS-1$
		fTestFailIcon = createManagedImage("obj16/testfail.png"); //$NON-NLS-1$
		fTestRunningIcon = createManagedImage("obj16/testrun.png"); //$NON-NLS-1$
		fTestIgnoredIcon = createManagedImage("obj16/testignored.png"); //$NON-NLS-1$
		fTestAssumptionFailureIcon = createManagedImage("obj16/testassumptionfailed.png"); //$NON-NLS-1$

		fSuiteIcon = createManagedImage(fSuiteIconDescriptor);
		fSuiteOkIcon = createManagedImage(fSuiteOkIconDescriptor);
		fSuiteErrorIcon = createManagedImage(fSuiteErrorIconDescriptor);
		fSuiteFailIcon = createManagedImage(fSuiteFailIconDescriptor);
		fSuiteRunningIcon = createManagedImage(fSuiteRunningIconDescriptor);
	}

	private Image createManagedImage(String path) {
		return createManagedImage(Images.getImageDescriptor(path));
	}

	private Image createManagedImage(ImageDescriptor descriptor) {
		Image image = descriptor.createImage();
		if (image == null) {
			image = ImageDescriptor.getMissingImageDescriptor().createImage();
		}
		fImagesToDispose.add(image);
		return image;
	}

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		fMemento = memento;
		IWorkbenchSiteProgressService progressService = getProgressService();
		if (progressService != null)
			progressService.showBusyForFamily(TestRunnerViewPart.FAMILY_UNITTEST_RUN);
	}

	private IWorkbenchSiteProgressService getProgressService() {
		Object siteService = getSite().getAdapter(IWorkbenchSiteProgressService.class);
		if (siteService != null)
			return (IWorkbenchSiteProgressService) siteService;
		return null;
	}

	@Override
	public void saveState(IMemento memento) {
		if (fSashForm == null) {
			// part has not been created
			if (fMemento != null) // Keep the old state;
				memento.putMemento(fMemento);
			return;
		}

//		int activePage= fTabFolder.getSelectionIndex();
//		memento.putInteger(TAG_PAGE, activePage);
		memento.putString(TAG_SCROLL, fScrollLockAction.isChecked() ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
		int weigths[] = fSashForm.getWeights();
		int ratio = (weigths[0] * 1000) / (weigths[0] + weigths[1]);
		memento.putInteger(TAG_RATIO, ratio);
		memento.putInteger(TAG_ORIENTATION, fOrientation);

		memento.putString(TAG_FAILURES_ONLY, fFailuresOnlyFilterAction.isChecked() ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
		memento.putString(TAG_IGNORED_ONLY, fIgnoredOnlyFilterAction.isChecked() ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
		memento.putString(TAG_LAYOUT, fLayout.name());
		memento.putString(TAG_SHOW_TIME, fShowTimeAction.isChecked() ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void restoreLayoutState(IMemento memento) {
//		Integer page= memento.getInteger(TAG_PAGE);
//		if (page != null) {
//			int p= page.intValue();
//			if (p < fTestRunTabs.size()) { // tab count can decrease if a contributing plug-in is removed
//				fTabFolder.setSelection(p);
//				fActiveRunTab= (TestRunTab)fTestRunTabs.get(p);
//			}
//		}
		Integer ratio = memento.getInteger(TAG_RATIO);
		if (ratio != null)
			fSashForm.setWeights(ratio.intValue(), 1000 - ratio.intValue());
		Integer orientation = memento.getInteger(TAG_ORIENTATION);
		if (orientation != null)
			fOrientation = orientation.intValue();
		computeOrientation();
		String scrollLock = memento.getString(TAG_SCROLL);
		if (scrollLock != null) {
			fScrollLockAction.setChecked(scrollLock.equals("true")); //$NON-NLS-1$
			setAutoScroll(!fScrollLockAction.isChecked());
		}

		String layoutString = memento.getString(TAG_LAYOUT);
		TestResultsLayout layout = layoutString == null ? TestResultsLayout.HIERARCHICAL
				: TestResultsLayout.valueOf(layoutString);

		String failuresOnly = memento.getString(TAG_FAILURES_ONLY);
		boolean showFailuresOnly = false;
		if (failuresOnly != null)
			showFailuresOnly = failuresOnly.equals("true"); //$NON-NLS-1$

		String ignoredOnly = memento.getString(TAG_IGNORED_ONLY);
		boolean showIgnoredOnly = false;
		if (ignoredOnly != null)
			showIgnoredOnly = ignoredOnly.equals("true"); //$NON-NLS-1$

		String time = memento.getString(TAG_SHOW_TIME);
		boolean showTime = true;
		if (time != null)
			showTime = time.equals("true"); //$NON-NLS-1$

		setFilterAndLayout(showFailuresOnly, showIgnoredOnly, layout);
		setShowExecutionTime(showTime);
	}

	/**
	 * Stops the currently running test and shuts down the RemoteTestRunner
	 */
	public void stopTest() {
		if (fTestRunSession != null) {
			if (fTestRunSession.isRunning()) {
				setContentDescription(Messages.TestRunnerViewPart_message_stopping);
			}
			fTestRunSession.abortTestRun();
		}
	}

	private void startUpdateJobs() {
		postSyncProcessChanges();

		if (fUpdateJob != null) {
			return;
		}
		fUnitTestIsRunningJob = new UnitTestIsRunningJob(Messages.TestRunnerViewPart_wrapperJobName);
		fUnitTestIsRunningLock = Job.getJobManager().newLock();
		// acquire lock while a test run is running
		// the lock is released when the test run terminates
		// the wrapper job will wait on this lock.
		fUnitTestIsRunningLock.acquire();
		getProgressService().schedule(fUnitTestIsRunningJob);

		fUpdateJob = new UpdateUIJob(Messages.TestRunnerViewPart_jobName);
		fUpdateJob.schedule(REFRESH_INTERVAL);
	}

	private void stopUpdateJobs() {
		if (fUpdateJob != null) {
			fUpdateJob.stop();
			fUpdateJob = null;
		}
		if (fUnitTestIsRunningJob != null && fUnitTestIsRunningLock != null) {
			fUnitTestIsRunningLock.release();
			fUnitTestIsRunningJob = null;
		}
		postSyncProcessChanges();
	}

	private void processChangesInUI() {
		if (fSashForm.isDisposed())
			return;

		doShowInfoMessage();
		refreshCounters();

		if (!fPartIsVisible)
			updateViewTitleProgress();
		else {
			updateViewIcon();
		}
		updateNextPreviousActions();

		fTestViewer.processChangesInUI();
	}

	private void updateNextPreviousActions() {
		boolean hasErrorsOrFailures = !fIgnoredOnlyFilterAction.isChecked() && hasErrorsOrFailures();
		fNextAction.setEnabled(hasErrorsOrFailures);
		fPreviousAction.setEnabled(hasErrorsOrFailures);
	}

	private String rerunLaunchMode() {
		return fTestRunSession != null && fTestRunSession.getLaunch() != null
				? fTestRunSession.getLaunch().getLaunchMode()
				: ILaunchManager.RUN_MODE;
	}

	/**
	 * Re-runs the tests executing the failed tests first
	 */
	private void rerunFailedTestCases() {
		if (lastLaunchStillRunning()) {
			// prompt for terminating the existing run
			if (MessageDialog.openQuestion(getSite().getShell(), Messages.TestRunnerViewPart_terminate_title,
					Messages.TestRunnerViewPart_terminate_message) && fTestRunSession != null) {
				fTestRunSession.abortTestRun();
			}
		}
		List<ITestElement> allFailedTestCases = new ArrayList<>();
		collectFailedTestCases(fTestRunSession, allFailedTestCases);
		ILaunchConfiguration tmp = fTestRunSession.getTestViewSupport().getRerunLaunchConfiguration(allFailedTestCases);
		if (tmp != null) {
			DebugUITools.launch(tmp, rerunLaunchMode());
		}
	}

	private void collectFailedTestCases(TestElement testElement, List<ITestElement> allFailedTestCases) {
		if (testElement == null) {
			return;
		}
		Result result = testElement.getTestResult(true);
		if (result != Result.ERROR && result != Result.FAILURE) {
			return;
		}
		if (testElement instanceof TestCaseElement) {
			allFailedTestCases.add(testElement);
		} else if (testElement instanceof ITestSuiteElement) {
			((ITestSuiteElement) testElement).getChildren()
					.forEach(child -> collectFailedTestCases((TestElement) child, allFailedTestCases));
		}

	}

	/**
	 * Sets auto-scroll enabled value
	 *
	 * @param scroll <code>true</code> in case of auto-scroll enabled, otherwise -
	 *               <code>false</code>
	 */
	public void setAutoScroll(boolean scroll) {
		fAutoScroll = scroll;
	}

	/**
	 * Indicates if autoscroll is enabled
	 *
	 * @return <code>true</code> if the output scroll and reveal is needed for the
	 *         tests as they are executed, otherwise returns <code>false</code>
	 */
	public boolean isAutoScroll() {
		return fAutoScroll;
	}

	/**
	 * Selects the next failure in the tests tree
	 */
	public void selectNextFailure() {
		fTestViewer.selectFailure(true);
	}

	/**
	 * Selects the previous failure in the tests tree
	 */
	public void selectPreviousFailure() {
		fTestViewer.selectFailure(false);
	}

	/**
	 * Selects the first failure in the tests tree
	 */
	protected void selectFirstFailure() {
		fTestViewer.selectFirstFailure();
	}

	private boolean hasErrorsOrFailures() {
		return getErrorsPlusFailures() > 0;
	}

	private int getErrorsPlusFailures() {
		if (fTestRunSession == null)
			return 0;
		else
			return fTestRunSession.getCurrentErrorCount() + fTestRunSession.getCurrentFailureCount();
	}

	private void handleStopped() {
		postSyncRunnable(() -> {
			if (isDisposed())
				return;
			resetViewIcon();
			fStopAction.setEnabled(false);
			updateRerunFailedFirstAction();
		});
		stopUpdateJobs();
		showMessageIfNoTests();
	}

	private void showMessageIfNoTests() {
		if (fTestRunSession != null && fTestRunSession.getFinalTestCaseCount() != null
				&& fTestRunSession.getFinalTestCaseCount().intValue() == 0) {
			Display.getDefault().asyncExec(() -> {
				String msg = MessageFormat.format(Messages.TestRunnerViewPart_error_no_tests_found, getDisplayName());
				MessageDialog.openInformation(getSite().getShell(), Messages.TestRunnerViewPart__error_cannotrun, msg);
			});
		}
	}

	private void resetViewIcon() {
		fViewImage = fOriginalViewImage;
		firePropertyChange(IWorkbenchPart.PROP_TITLE);
	}

	private void updateViewIcon() {
		if (fTestRunSession == null || fTestRunSession.isStopped() || fTestRunSession.isRunning()
				|| fTestRunSession.countStartedTestCases() == 0)
			fViewImage = fOriginalViewImage;
		else if (hasErrorsOrFailures())
			fViewImage = fTestRunFailIcon;
		else
			fViewImage = fTestRunOKIcon;
		firePropertyChange(IWorkbenchPart.PROP_TITLE);
	}

	private void updateViewTitleProgress() {
		if (fTestRunSession != null) {
			if (fTestRunSession.isRunning()) {
				Image progress = fProgressImages.getImage(fTestRunSession.countStartedTestCases(),
						fTestRunSession.getFinalTestCaseCount(),
						fTestRunSession.getCurrentErrorCount() > 0 || fTestRunSession.getCurrentFailureCount() > 0);
				if (progress != fViewImage) {
					fViewImage = progress;
					firePropertyChange(IWorkbenchPart.PROP_TITLE);
				}
			} else {
				updateViewIcon();
			}
		} else {
			resetViewIcon();
		}
	}

	/**
	 * Sets an active test run session
	 *
	 * @param testRunSession new active test run session
	 * @return deactivated session, or <code>null</code> iff no session got
	 *         deactivated
	 */
	public TestRunSession setActiveTestRunSession(TestRunSession testRunSession) {
		/*
		 * - State: fTestRunSession fTestSessionListener Jobs
		 * fTestViewer.processChangesInUI(); - UI: fCounterPanel fProgressBar
		 * setContentDescription / fInfoMessage setTitleToolTip view icons statusLine
		 * fFailureTrace
		 *
		 * action enablement
		 */
		if (fTestRunSession == testRunSession)
			return null;

		deregisterTestSessionListener();

		TestRunSession deactivatedSession = fTestRunSession;

		fTestRunSession = testRunSession;
		fTestViewer.registerActiveSession(testRunSession);
		History.INSTANCE.watch(testRunSession);

		if (fSashForm.isDisposed()) {
			stopUpdateJobs();
			return deactivatedSession;
		}

		if (testRunSession == null) {
			setTitleToolTip(null);
			resetViewIcon();
			clearStatus();
			fFailureTrace.clear();

			registerInfoMessage(" "); //$NON-NLS-1$
			stopUpdateJobs();

			fStopAction.setEnabled(false);
			fRerunFailedCasesAction.setEnabled(false);
			fRerunLastTestAction.setEnabled(false);

		} else {
			if (fTestRunSession.isStarting() || fTestRunSession.isRunning()) {
				fTestSessionListener = new TestSessionListener();
				fTestRunSession.addTestSessionListener(fTestSessionListener);
			}
			if (!fTestRunSession.isStarting() && !fShowOnErrorOnly) {
				showTestResultsView();
			}

			setTitleToolTip();

			clearStatus();
			fFailureTrace.clear();
			registerInfoMessage(BasicElementLabels.getJavaElementName(fTestRunSession.getTestRunName()));

			updateRerunFailedFirstAction();
			fRerunLastTestAction.setEnabled(fTestRunSession.getLaunch() != null);
			fEditLaunchConfigAction.setEnabled(fTestRunSession.getLaunch() != null);

			fStopAction.setEnabled(fTestRunSession.isRunning());
			if (fTestRunSession.isRunning()) {
				startUpdateJobs();
			} else /* old or fresh session: don't want jobs at this stage */ {
				stopUpdateJobs();
			}
		}
		getSite().getShell().getDisplay().asyncExec(this::processChangesInUI);
		return deactivatedSession;
	}

	private void deregisterTestSessionListener() {
		if (fTestRunSession != null && fTestSessionListener != null) {
			fTestRunSession.removeTestSessionListener(fTestSessionListener);
			fTestSessionListener = null;
		}
	}

	private void updateRerunFailedFirstAction() {
		boolean state = hasErrorsOrFailures() && fTestRunSession.getLaunch().getLaunchConfiguration() != null;
		fRerunFailedCasesAction.setEnabled(state);
	}

	/**
	 * Returns the display name of the current test run session
	 *
	 * @return the display name of the current test run session, or
	 *         <code>null</code>
	 */
	public String getDisplayName() {
		ITestViewSupport testViewSupport = fTestRunSession.getTestViewSupport();
		return testViewSupport != null ? testViewSupport.getDisplayName() : null;
	}

	private void setTitleToolTip() {
		String displayStr = getDisplayName();

		String testRunLabel = BasicElementLabels.getJavaElementName(fTestRunSession.getTestRunName());
		if (displayStr != null)
			setTitleToolTip(MessageFormat.format(Messages.TestRunnerViewPart_titleToolTip, testRunLabel, displayStr));
		else
			setTitleToolTip(testRunLabel);
	}

	@Override
	public synchronized void dispose() {
		fIsDisposed = true;
		if (fTestRunSessionListener != null)
			UnitTestModel.getInstance().removeTestRunSessionListener(fTestRunSessionListener);

		IHandlerService handlerService = getSite().getWorkbenchWindow().getService(IHandlerService.class);
		handlerService.deactivateHandler(fRerunLastActivation);
		handlerService.deactivateHandler(fRerunFailedFirstActivation);
		setActiveTestRunSession(null);

		if (fProgressImages != null)
			fProgressImages.dispose();
		getViewSite().getPage().removePartListener(fPartListener);

		disposeImages();
		if (fClipboard != null)
			fClipboard.dispose();
		if (fViewMenuListener != null) {
			getViewSite().getActionBars().getMenuManager().removeMenuListener(fViewMenuListener);
		}
		/*
		 * if (fDirtyListener != null) {
		 * JavaCore.removeElementChangedListener(fDirtyListener); fDirtyListener= null;
		 * }
		 */
		if (fFailureTrace != null) {
			fFailureTrace.dispose();
		}
	}

	private void disposeImages() {
		for (Image imageToDispose : fImagesToDispose) {
			imageToDispose.dispose();
		}
	}

	private void postSyncRunnable(Runnable r) {
		if (!isDisposed())
			getDisplay().syncExec(r);
	}

	private void refreshCounters() {
		// TODO: Inefficient. Either
		// - keep a boolean fHasTestRun and update only on changes, or
		// - improve components to only redraw on changes (once!).

		int startedCount;
		int ignoredCount;
		Integer totalCount;
		int errorCount;
		int failureCount;
		int assumptionFailureCount;
		boolean hasErrorsOrFailures;
		boolean stopped;

		if (fTestRunSession != null) {
			startedCount = fTestRunSession.countStartedTestCases();
			ignoredCount = fTestRunSession.getCurrentIgnoredCount();
			totalCount = fTestRunSession.getFinalTestCaseCount();
			errorCount = fTestRunSession.getCurrentErrorCount();
			failureCount = fTestRunSession.getCurrentFailureCount();
			assumptionFailureCount = fTestRunSession.getCurrentAssumptionFailureCount();
			hasErrorsOrFailures = errorCount + failureCount > 0;
			stopped = fTestRunSession.isStopped();
		} else {
			startedCount = 0;
			ignoredCount = 0;
			totalCount = null;
			errorCount = 0;
			failureCount = 0;
			assumptionFailureCount = 0;
			hasErrorsOrFailures = false;
			stopped = false;
		}

		fCounterPanel.setTotal(totalCount);
		fCounterPanel.setRunValue(startedCount, ignoredCount, assumptionFailureCount);
		fCounterPanel.setErrorValue(errorCount);
		fCounterPanel.setFailureValue(failureCount);

		int ticksDone;
		if (startedCount == 0) {
			ticksDone = 0;
		} else if (totalCount != null && startedCount == totalCount.intValue()
				&& fTestRunSession.getProgressState() == ProgressState.COMPLETED) {
			ticksDone = totalCount.intValue();
		} else {
			ticksDone = startedCount - 1;
		}

		fProgressBar.reset(hasErrorsOrFailures, stopped, ticksDone,
				totalCount != null ? totalCount.intValue() : ticksDone + 1);
	}

	/**
	 * Queues a runnable that shows a test results view
	 */
	protected void postShowTestResultsView() {
		postSyncRunnable(() -> {
			if (isDisposed())
				return;
			showTestResultsView();
		});
	}

	/**
	 * Makes the test results view visible
	 */
	public void showTestResultsView() {
		IWorkbenchWindow window = getSite().getWorkbenchWindow();
		IWorkbenchPage page = window.getActivePage();
		TestRunnerViewPart testRunner = null;

		if (page != null) {
			try { // show the result view
				testRunner = (TestRunnerViewPart) page.findView(TestRunnerViewPart.NAME);
				if (testRunner == null) {
					IWorkbenchPart activePart = page.getActivePart();
					testRunner = (TestRunnerViewPart) page.showView(TestRunnerViewPart.NAME, null,
							IWorkbenchPage.VIEW_VISIBLE);
					// restore focus
					page.activate(activePart);
				} else {
					page.bringToTop(testRunner);
				}
			} catch (PartInitException pie) {
				UnitTestPlugin.log(pie);
			}
		}
	}

	/**
	 * Shows an info message
	 */
	protected void doShowInfoMessage() {
		if (fInfoMessage != null) {
			setContentDescription(fInfoMessage);
			fInfoMessage = null;
		}
	}

	/**
	 * Registers a test information message
	 *
	 * @param message an information message to register
	 */
	public void registerInfoMessage(String message) {
		fInfoMessage = message;
	}

	private SashForm createSashForm(Composite parent) {
		fSashForm = new SashForm(parent, SWT.VERTICAL);

		ViewForm top = new ViewForm(fSashForm, SWT.NONE);

		Composite empty = new Composite(top, SWT.NONE);
		empty.setLayout(new Layout() {
			@Override
			protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
				return new Point(1, 1); // (0, 0) does not work with super-intelligent ViewForm
			}

			@Override
			protected void layout(Composite composite, boolean flushCache) {
			}
		});
		top.setTopLeft(empty); // makes ViewForm draw the horizontal separator line ...
		fTestViewer = new TestViewer(top, fClipboard, this);
		top.setContent(fTestViewer.getTestViewerControl());

		ViewForm bottom = new ViewForm(fSashForm, SWT.NONE);

		CLabel label = new CLabel(bottom, SWT.NONE);
		label.setText(Messages.TestRunnerViewPart_label_failure);
		label.setImage(fStackViewIcon);
		bottom.setTopLeft(label);
		ToolBar failureToolBar = new ToolBar(bottom, SWT.FLAT | SWT.WRAP);
		bottom.setTopCenter(failureToolBar);
		fFailureTrace = new FailureTraceUIBlock(bottom, fClipboard, this, failureToolBar);
		bottom.setContent(fFailureTrace.getComposite());

		fSashForm.setWeights(50, 50);
		return fSashForm;
	}

	private void clearStatus() {
		getStatusLine().setMessage(null);
		getStatusLine().setErrorMessage(null);
	}

	@Override
	public void setFocus() {
		if (fTestViewer != null)
			fTestViewer.getTestViewerControl().setFocus();
	}

	@Override
	public void createPartControl(Composite parent) {
		fParent = parent;
		addResizeListener(parent);
		fClipboard = new Clipboard(parent.getDisplay());

		GridLayout gridLayout = new GridLayout();
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		parent.setLayout(gridLayout);

//		fViewHistory= new RunnerViewHistory();
		configureToolBar();

		fCounterComposite = createProgressCountPanel(parent);
		fCounterComposite.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		SashForm sashForm = createSashForm(parent);
		sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));

		IActionBars actionBars = getViewSite().getActionBars();

		fCopyAction = new UnitTestCopyAction(fFailureTrace, fClipboard);
		fCopyAction.setActionDefinitionId(ActionFactory.COPY.getCommandId());
		actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(), fCopyAction);

		fPasteAction = new UnitTesttPasteAction(parent.getShell(), fClipboard);
		fPasteAction.setActionDefinitionId(ActionFactory.PASTE.getCommandId());
		actionBars.setGlobalActionHandler(ActionFactory.PASTE.getId(), fPasteAction);

		initPageSwitcher();
		addDropAdapter(parent);

		fOriginalViewImage = getTitleImage();
		fProgressImages = new ProgressIcons(fOriginalViewImage);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IUnitTestHelpContextIds.RESULTS_VIEW);

		getViewSite().getPage().addPartListener(fPartListener);

		setFilterAndLayout(false, false, TestResultsLayout.HIERARCHICAL);
		setShowExecutionTime(true);
		if (fMemento != null) {
			restoreLayoutState(fMemento);
		}
		fMemento = null;

		fTestRunSessionListener = new TestRunSessionListener();
		UnitTestModel.getInstance().addTestRunSessionListener(fTestRunSessionListener);
		UnitTestModel.getInstance().addTestRunSessionListener(History.INSTANCE);

		// always show youngest test run in view. simulate "sessionAdded" event to do
		// that
		List<TestRunSession> testRunSessions = UnitTestModel.getInstance().getTestRunSessions();
		if (!testRunSessions.isEmpty()) {
			fTestRunSessionListener.sessionAdded(testRunSessions.get(0));
		}
	}

	private void addDropAdapter(Composite parent) {
		DropTarget dropTarget = new DropTarget(parent,
				DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK | DND.DROP_DEFAULT);
		dropTarget.setTransfer(TextTransfer.getInstance());
		class DropAdapter extends DropTargetAdapter {
			@Override
			public void dragEnter(DropTargetEvent event) {
				event.detail = DND.DROP_COPY;
				event.feedback = DND.FEEDBACK_NONE;
			}

			@Override
			public void dragOver(DropTargetEvent event) {
				event.detail = DND.DROP_COPY;
				event.feedback = DND.FEEDBACK_NONE;
			}

			@Override
			public void dragOperationChanged(DropTargetEvent event) {
				event.detail = DND.DROP_COPY;
				event.feedback = DND.FEEDBACK_NONE;
			}

			@Override
			public void drop(final DropTargetEvent event) {
				if (TextTransfer.getInstance().isSupportedType(event.currentDataType)) {
					String url = (String) event.data;
					importTestRunSession(url);
				}
			}
		}
		dropTarget.addDropListener(new DropAdapter());
	}

	private void initPageSwitcher() {
		/*
		 * @SuppressWarnings("unused") PageSwitcher pageSwitcher= new PageSwitcher(this)
		 * {
		 *
		 * @Override public Object[] getPages() { return
		 * fViewHistory.getHistoryEntries().toArray(); }
		 *
		 * @Override public String getName(Object page) { return
		 * fViewHistory.getText((TestRunSession) page); }
		 *
		 * @Override public ImageDescriptor getImageDescriptor(Object page) { return
		 * fViewHistory.getImageDescriptor(page); }
		 *
		 * @Override public void activatePage(Object page) {
		 * fViewHistory.setActiveEntry((TestRunSession) page); }
		 *
		 * @Override public int getCurrentPageIndex() { return
		 * fViewHistory.getHistoryEntries().indexOf(fViewHistory.getCurrentEntry()); }
		 * };
		 */
	}

	private void addResizeListener(Composite parent) {
		parent.addControlListener(ControlListener.controlResizedAdapter(e -> {
			computeOrientation();
		}));
	}

	void computeOrientation() {
		if (fOrientation != VIEW_ORIENTATION_AUTOMATIC) {
			fCurrentOrientation = fOrientation;
			setOrientation(fCurrentOrientation);
		} else {
			Point size = fParent.getSize();
			if (size.x != 0 && size.y != 0) {
				if (size.x > size.y)
					setOrientation(VIEW_ORIENTATION_HORIZONTAL);
				else
					setOrientation(VIEW_ORIENTATION_VERTICAL);
			}
		}
	}

	private void configureToolBar() {
		IActionBars actionBars = getViewSite().getActionBars();
		IToolBarManager toolBar = actionBars.getToolBarManager();
		IMenuManager viewMenu = actionBars.getMenuManager();
		fNextAction = new ShowNextFailureAction(this);
		fNextAction.setEnabled(false);
		actionBars.setGlobalActionHandler(ActionFactory.NEXT.getId(), fNextAction);

		fPreviousAction = new ShowPreviousFailureAction(this);
		fPreviousAction.setEnabled(false);
		actionBars.setGlobalActionHandler(ActionFactory.PREVIOUS.getId(), fPreviousAction);
		fStopAction = new StopAction();
		fStopAction.setEnabled(false);

		fRerunLastTestAction = new RerunLastAction();
		IHandlerService handlerService = getSite().getWorkbenchWindow().getService(IHandlerService.class);
		IHandler handler = new AbstractHandler() {
			@Override
			public Object execute(ExecutionEvent event) throws ExecutionException {
				fRerunLastTestAction.run();
				return null;
			}

			@Override
			public boolean isEnabled() {
				return fRerunLastTestAction.isEnabled();
			}
		};
		fRerunLastActivation = handlerService.activateHandler(RERUN_LAST_COMMAND, handler);

		fRerunFailedCasesAction = new RerunFailedCasesAction();
		handler = new AbstractHandler() {
			@Override
			public Object execute(ExecutionEvent event) throws ExecutionException {
				fRerunFailedCasesAction.run();
				return null;
			}

			@Override
			public boolean isEnabled() {
				return fRerunFailedCasesAction.isEnabled();
			}
		};
		fRerunFailedFirstActivation = handlerService.activateHandler(RERUN_FAILED_CASES_COMMAND, handler);
		fEditLaunchConfigAction = new EditLaunchConfigurationAction() {
			@Override
			protected ILaunchConfiguration getLaunchConfiguration() {
				return fTestRunSession != null ? fTestRunSession.getLaunch().getLaunchConfiguration() : null;
			}

			@Override
			protected String getMode() {
				return rerunLaunchMode();
			}

			@Override
			protected boolean isTerminated() {
				return true; // always allow to re-run
			}
		};
		fEditLaunchConfigAction.setToolTipText(Messages.TestRunnerViewPart_editLaunchConfiguration);
		fEditLaunchConfigAction.setImageDescriptor(
				DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_OBJS_MODIFICATION_WATCHPOINT));
		fEditLaunchConfigAction.setDisabledImageDescriptor(
				DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_OBJS_MODIFICATION_WATCHPOINT_DISABLED));

		fFailuresOnlyFilterAction = new FailuresOnlyFilterAction();
		fIgnoredOnlyFilterAction = new IgnoredOnlyFilterAction();

		fScrollLockAction = new ScrollLockAction(this);
		fScrollLockAction.setChecked(!fAutoScroll);

		fToggleOrientationActions = new ToggleOrientationAction[] {
				new ToggleOrientationAction(VIEW_ORIENTATION_VERTICAL),
				new ToggleOrientationAction(VIEW_ORIENTATION_HORIZONTAL),
				new ToggleOrientationAction(VIEW_ORIENTATION_AUTOMATIC) };

		toolBar.add(fNextAction);
		toolBar.add(fPreviousAction);
		toolBar.add(fFailuresOnlyFilterAction);
		toolBar.add(fIgnoredOnlyFilterAction);
		toolBar.add(fScrollLockAction);
		toolBar.add(new Separator());
		toolBar.add(fRerunLastTestAction);
		toolBar.add(fRerunFailedCasesAction);
		toolBar.add(fStopAction);
		toolBar.add(fEditLaunchConfigAction);
		toolBar.add(new Separator());
		IContributionItem historyIte = new CommandContributionItem(new CommandContributionItemParameter(getSite(),
				HistoryHandler.COMMAND_ID, HistoryHandler.COMMAND_ID, SWT.PUSH));
		toolBar.add(historyIte);

		fShowTestHierarchyAction = new ShowTestHierarchyAction();
		fShowTimeAction = new ShowTimeAction();
		viewMenu.add(fShowTestHierarchyAction);
		viewMenu.add(fShowTimeAction);
		viewMenu.add(new Separator());

		MenuManager layoutSubMenu = new MenuManager(Messages.TestRunnerViewPart_layout_menu);
		for (ToggleOrientationAction toggleOrientationAction : fToggleOrientationActions) {
			layoutSubMenu.add(toggleOrientationAction);
		}
		viewMenu.add(layoutSubMenu);
		MenuManager sortSubmenu = new MenuManager(Messages.TestRunnerViewPart_sort);
		sortSubmenu.add(new SortAction(true));
		sortSubmenu.add(new SortAction(false));
		viewMenu.add(sortSubmenu);
		viewMenu.add(new Separator());

		viewMenu.add(fFailuresOnlyFilterAction);
		viewMenu.add(fIgnoredOnlyFilterAction);

		fActivateOnErrorAction = new ActivateOnErrorAction();
		viewMenu.add(fActivateOnErrorAction);
		fViewMenuListener = manager -> fActivateOnErrorAction.update();

		viewMenu.addMenuListener(fViewMenuListener);

		actionBars.updateActionBars();
	}

	private IStatusLineManager getStatusLine() {
		// we want to show messages globally hence we
		// have to go through the active part
		IViewSite site = getViewSite();
		IWorkbenchPage page = site.getPage();
		IWorkbenchPart activePart = page.getActivePart();

		if (activePart instanceof IViewPart) {
			IViewPart activeViewPart = (IViewPart) activePart;
			IViewSite activeViewSite = activeViewPart.getViewSite();
			return activeViewSite.getActionBars().getStatusLineManager();
		}

		if (activePart instanceof IEditorPart) {
			IEditorPart activeEditorPart = (IEditorPart) activePart;
			IEditorActionBarContributor contributor = activeEditorPart.getEditorSite().getActionBarContributor();
			if (contributor instanceof EditorActionBarContributor)
				return ((EditorActionBarContributor) contributor).getActionBars().getStatusLineManager();
		}
		// no active part
		return getViewSite().getActionBars().getStatusLineManager();
	}

	/**
	 * Creates a progress count panel
	 *
	 * @param parent a parent composite
	 * @return a progress count ppanel composite object
	 */
	protected Composite createProgressCountPanel(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		setCounterColumns(layout);

		fCounterPanel = new CounterPanel(composite);
		fCounterPanel.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		fProgressBar = new UnitTestProgressBar(composite);
		fProgressBar.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		return composite;
	}

	/**
	 * Handles the test element selection
	 *
	 * @param test a selected test element
	 */
	public void handleTestSelected(TestElement test) {
		showFailure(test);
		fCopyAction.handleTestSelected(test);
	}

	private void showFailure(final TestElement test) {
		postSyncRunnable(() -> {
			if (!isDisposed()) {
				fFailureTrace.showFailure(test);
			}
		});
	}

	/**
	 * Returns a current test run session object
	 *
	 * @return the current test run session, or <code>null</code>
	 */
	public TestRunSession getCurrentTestRunSession() {
		return fTestRunSession;
	}

	private boolean isDisposed() {
		return fIsDisposed || fCounterPanel.isDisposed();
	}

	private Display getDisplay() {
		return getViewSite().getShell().getDisplay();
	}

	@Override
	public Image getTitleImage() {
		if (fOriginalViewImage == null) {
			fOriginalViewImage = super.getTitleImage();
		}

		if (fViewImage == null) {
			return super.getTitleImage();
		}
		return fViewImage;
	}

	void codeHasChanged() {
		/*
		 * if (fDirtyListener != null) {
		 * JavaCore.removeElementChangedListener(fDirtyListener); fDirtyListener= null;
		 * }
		 */
		if (fViewImage == fTestRunOKIcon)
			fViewImage = fTestRunOKDirtyIcon;
		else if (fViewImage == fTestRunFailIcon)
			fViewImage = fTestRunFailDirtyIcon;

		Runnable r = () -> {
			if (isDisposed())
				return;
			firePropertyChange(IWorkbenchPart.PROP_TITLE);
		};
		if (!isDisposed())
			getDisplay().asyncExec(r);
	}

	private void postSyncProcessChanges() {
		postSyncRunnable(this::processChangesInUI);
	}

	/**
	 * Warns on content change
	 */
	public void warnOfContentChange() {
		IWorkbenchSiteProgressService service = getProgressService();
		if (service != null)
			service.warnOfContentChange();
	}

	/**
	 * Indicates if the last test launch is kept alive
	 *
	 * @return <code>true</code> in case of the last test launch is kept alive,
	 *         otherwise returns <code>false</code>
	 */
	public boolean lastLaunchStillRunning() {
		return fTestRunSession != null && !fTestRunSession.getLaunch().isTerminated();
	}

	private void setOrientation(int orientation) {
		if ((fSashForm == null) || fSashForm.isDisposed())
			return;
		boolean horizontal = orientation == VIEW_ORIENTATION_HORIZONTAL;
		fSashForm.setOrientation(horizontal ? SWT.HORIZONTAL : SWT.VERTICAL);
		for (ToggleOrientationAction toggleOrientationAction : fToggleOrientationActions)
			toggleOrientationAction.setChecked(fOrientation == toggleOrientationAction.getOrientation());
		fCurrentOrientation = orientation;
		GridLayout layout = (GridLayout) fCounterComposite.getLayout();
		setCounterColumns(layout);
		fParent.layout();
	}

	private void setCounterColumns(GridLayout layout) {
		if (fCurrentOrientation == VIEW_ORIENTATION_HORIZONTAL)
			layout.numColumns = 2;
		else
			layout.numColumns = 1;
	}

	static boolean getShowOnErrorOnly() {
		return Platform.getPreferencesService().getBoolean(UnitTestPlugin.PLUGIN_ID,
				UnitTestPreferencesConstants.SHOW_ON_ERROR_ONLY, false, null);
	}

	static void importTestRunSession(final String url) {
		try {
			PlatformUI.getWorkbench().getProgressService()
					.busyCursorWhile(monitor -> UnitTestModel.getInstance().importTestRunSession(url, monitor));
		} catch (InterruptedException e) {
			// cancelled
		} catch (InvocationTargetException e) {
			CoreException ce = (CoreException) e.getCause();
			StatusManager.getManager().handle(ce.getStatus(), StatusManager.SHOW | StatusManager.LOG);
		}
	}

	/**
	 * Returns the Failure Trace UI Block
	 *
	 * @return the current failure trace OI block
	 */
	public FailureTraceUIBlock getFailureTrace() {
		return fFailureTrace;
	}

	void setShowFailuresOnly(boolean failuresOnly) {
		setFilterAndLayout(failuresOnly, false /* ignoredOnly must be off */, fLayout);
	}

	void setShowIgnoredOnly(boolean ignoredOnly) {
		setFilterAndLayout(false /* failuresOnly must be off */, ignoredOnly, fLayout);
	}

	private void setFilterAndLayout(boolean failuresOnly, boolean ignoredOnly, TestResultsLayout layoutMode) {
		fShowTestHierarchyAction.setChecked(layoutMode == TestResultsLayout.HIERARCHICAL);
		fLayout = layoutMode;
		fFailuresOnlyFilterAction.setChecked(failuresOnly);
		fIgnoredOnlyFilterAction.setChecked(ignoredOnly);
		fTestViewer.setShowFailuresOrIgnoredOnly(failuresOnly, ignoredOnly, layoutMode);
		updateNextPreviousActions();
	}

	private void setShowExecutionTime(boolean showTime) {
		fTestViewer.setShowTime(showTime);
		fShowTimeAction.setChecked(showTime);
	}
}
