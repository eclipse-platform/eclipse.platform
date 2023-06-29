/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
 *     Wind River - Pawel Piech - Busy status while updates in progress (Bug 206822)
 *     Pawel Piech (Wind River) - added a breadcrumb mode to Debug view (Bug 252677)
 *     Andrey Loskutov <loskutov@gmx.de> - fixed memory leak due the breadcrumb (Bug 466789)
 *******************************************************************************/
package org.eclipse.debug.internal.ui.views.launch;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.commands.IHandler2;
import org.eclipse.core.commands.contexts.ContextManagerEvent;
import org.eclipse.core.commands.contexts.IContextManagerListener;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.commands.IRestartHandler;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.DelegatingModelPresentation;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.actions.AddToFavoritesAction;
import org.eclipse.debug.internal.ui.actions.EditLaunchConfigurationAction;
import org.eclipse.debug.internal.ui.commands.actions.DisconnectCommandAction;
import org.eclipse.debug.internal.ui.commands.actions.DropToFrameCommandAction;
import org.eclipse.debug.internal.ui.commands.actions.RestartCommandAction;
import org.eclipse.debug.internal.ui.commands.actions.ResumeCommandAction;
import org.eclipse.debug.internal.ui.commands.actions.StepIntoCommandAction;
import org.eclipse.debug.internal.ui.commands.actions.StepOverCommandAction;
import org.eclipse.debug.internal.ui.commands.actions.StepReturnCommandAction;
import org.eclipse.debug.internal.ui.commands.actions.SuspendCommandAction;
import org.eclipse.debug.internal.ui.commands.actions.TerminateAllAction;
import org.eclipse.debug.internal.ui.commands.actions.TerminateAndRelaunchAction;
import org.eclipse.debug.internal.ui.commands.actions.TerminateAndRemoveAction;
import org.eclipse.debug.internal.ui.commands.actions.TerminateCommandAction;
import org.eclipse.debug.internal.ui.commands.actions.ToggleStepFiltersAction;
import org.eclipse.debug.internal.ui.preferences.IDebugPreferenceConstants;
import org.eclipse.debug.internal.ui.sourcelookup.EditSourceLookupPathAction;
import org.eclipse.debug.internal.ui.sourcelookup.LookupSourceAction;
import org.eclipse.debug.internal.ui.viewers.model.InternalTreeModelViewer;
import org.eclipse.debug.internal.ui.viewers.model.VirtualFindAction;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelChangedListener;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDeltaVisitor;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelProxy;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdateListener;
import org.eclipse.debug.internal.ui.viewers.model.provisional.TreeModelViewer;
import org.eclipse.debug.internal.ui.views.DebugModelPresentationContext;
import org.eclipse.debug.internal.ui.views.ViewContextService;
import org.eclipse.debug.ui.AbstractDebugView;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.actions.DebugCommandAction;
import org.eclipse.debug.ui.contexts.AbstractDebugContextProvider;
import org.eclipse.debug.ui.contexts.DebugContextEvent;
import org.eclipse.debug.ui.contexts.IDebugContextListener;
import org.eclipse.debug.ui.contexts.IDebugContextProvider;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveListener2;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.actions.SelectionListenerAction;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTarget;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.texteditor.IUpdate;

public class LaunchView extends AbstractDebugView
	implements ISelectionChangedListener, IPerspectiveListener2, IPageListener, IShowInTarget, IShowInSource,
	IShowInTargetList, IPartListener2, IViewerUpdateListener, IContextManagerListener, IModelChangedListener
{

	public static final String ID_CONTEXT_ACTIVITY_BINDINGS = "contextActivityBindings"; //$NON-NLS-1$

	private static final String TERMINATE = "terminate"; //$NON-NLS-1$

	private static final String DISCONNECT = "disconnect"; //$NON-NLS-1$

	private static final String SUSPEND = "suspend"; //$NON-NLS-1$

	private static final String RESUME = "resume"; //$NON-NLS-1$

	private static final String STEP_RETURN = "step_return"; //$NON-NLS-1$

	private static final String STEP_OVER = "step_over"; //$NON-NLS-1$

	private static final String DROP_TO_FRAME = "drop_to_frame"; //$NON-NLS-1$

	private static final String STEP_INTO = "step_into"; //$NON-NLS-1$

	public static final String TERMINATE_AND_REMOVE = "terminate_and_remove"; //$NON-NLS-1$

	public static final String TERMINATE_ALL = "terminate_all"; //$NON-NLS-1$

	public static final String TERMINATE_AND_RELAUNCH = "terminate_relaunch"; //$NON-NLS-1$

	private static final String TOGGLE_STEP_FILTERS = "toggle_step_filters"; //$NON-NLS-1$

	private static final String RESTART = "restart"; //$NON-NLS-1$

	private static final int BREADCRUMB_TRIGGER_HEIGHT_DEFAULT = 30; // pixels

	private static final int BREADCRUMB_TRIGGER_RANGE = 5; // pixels

	private static final int BREADCRUMB_STICKY_RANGE = 20; // pixels

	/**
	 * Whether this view is in the active page of a perspective.
	 */
	private boolean fIsActive = true;

	/**
	 * Model presentation or <code>null</code> if none
	 */
	private IDebugModelPresentation fPresentation;

	private IPresentationContext fPresentationContext;

	private EditLaunchConfigurationAction fEditConfigAction;
	private AddToFavoritesAction fAddToFavoritesAction;
	private EditSourceLookupPathAction fEditSourceAction;
	private LookupSourceAction fLookupAction;

	/**
	 * Current view mode (auto vs. breadcrumb, vs. tree view).
	 *
	 * @since 3.5
	 */
	private String fCurrentViewMode = IDebugPreferenceConstants.DEBUG_VIEW_MODE_AUTO;

	/**
	 * Actions for selecting the view mode (auto vs. breadcrumb, vs. tree view).
	 *
	 * @since 3.5
	 */
	private DebugViewModeAction[] fDebugViewModeActions;

	/**
	 * Action which shows or hides the Debug view toolbar.
	 */
	private DebugToolBarAction fDebugToolBarAction;

	/**
	 * Action that controls the breadcrumb drop-down auto-expand behavior.
	 *
	 * @since 3.5
	 */
	private BreadcrumbDropDownAutoExpandAction fBreadcrumbDropDownAutoExpandAction;

	/**
	 * Context service for this view.  Used to track whether debug toolbar
	 * action set is active.
	 *
	 * @since 3.8
	 */
	private IContextService fContextService;

	/**
	 * Preference name for the view's memento.
	 *
	 * @since 3.5
	 */
	private String PREF_STATE_MEMENTO = "pref_state_memento."; //$NON-NLS-1$

	/**
	 * Key for a view preference for whether the elements in breadcrumb's
	 * drop-down viewer should be automatically expanded.
	 *
	 * @since 3.5
	 */
	private static final String BREADCRUMB_DROPDOWN_AUTO_EXPAND = DebugUIPlugin.getUniqueIdentifier() + ".BREADCRUMB_DROPDOWN_AUTO_EXPAND"; //$NON-NLS-1$

	/**
	 * Preference for whether the elements in breadcrumb's
	 * drop-down viewer should be automatically expanded.
	 *
	 * @since 3.5
	 */
	private boolean fBreadcrumbDropDownAutoExpand;

	/**
	 * Action handlers. Maps action identifiers to IHandler's.
	 *
	 * @since 3.6
	 */
	private Map<String, IHandler2> fHandlers = new HashMap<>();

	private boolean fDebugToolbarInView = true;

	private Set<String> fDebugToolbarPerspectives = new TreeSet<>();

	/**
	 * Page-book page for the breadcrumb viewer.  This page is activated in
	 * Debug view when the height of the view is reduced to just one line.
	 *
	 * @since 3.5
	 */
	private class BreadcrumbPage extends Page {

		LaunchViewBreadcrumb fCrumb;
		Control fControl;

		@Override
		public void createControl(Composite parent) {
			fCrumb = new LaunchViewBreadcrumb(LaunchView.this, (TreeModelViewer)getViewer(), fTreeViewerDebugContextProvider);
			fControl = fCrumb.createContent(parent);
		}

		@Override
		public void init(IPageSite pageSite) {
			super.init(pageSite);
			pageSite.setSelectionProvider(fCrumb.getSelectionProvider());
		}

		@Override
		public Control getControl() {
			return fControl;
		}

		@Override
		public void setFocus() {
			fCrumb.activate();
		}

		IDebugContextProvider getContextProvider() {
			return fCrumb.getContextProvider();
		}

		int getHeight() {
			return fCrumb.getHeight();
		}

		@Override
		public void dispose() {
			fCrumb.dispose();
		}
	}

	private BreadcrumbPage fBreadcrumbPage;

	class TreeViewerContextProvider extends AbstractDebugContextProvider implements IModelChangedListener {

		private ISelection fContext = null;
		private TreeModelViewer fViewer = null;
		private Visitor fVisitor = new Visitor();

		class Visitor implements IModelDeltaVisitor {
			@Override
			public boolean visit(IModelDelta delta, int depth) {
				if ((delta.getFlags() & (IModelDelta.STATE | IModelDelta.CONTENT)) > 0) {
					// state and/or content change
					if ((delta.getFlags() & IModelDelta.SELECT) == 0) {
						// no select flag
						if ((delta.getFlags() & IModelDelta.CONTENT) > 0) {
							// content has changed without select >> possible re-activation
							possibleChange(getViewerTreePath(delta), DebugContextEvent.ACTIVATED);
						} else if ((delta.getFlags() & IModelDelta.STATE) > 0) {
							// state has changed without select >> possible state change of active context
							possibleChange(getViewerTreePath(delta), DebugContextEvent.STATE);
						}
					}
				}
				return true;
			}
		}

		/**
		 * Returns a tree path for the node, *not* including the root element.
		 *
		 * @param node
		 *            model delta
		 * @return corresponding tree path
		 */
		private TreePath getViewerTreePath(IModelDelta node) {
			ArrayList<Object> list = new ArrayList<>();
			IModelDelta parentDelta = node.getParentDelta();
			while (parentDelta != null) {
				list.add(0, node.getElement());
				node = parentDelta;
				parentDelta = node.getParentDelta();
			}
			return new TreePath(list.toArray());
		}

		public TreeViewerContextProvider(TreeModelViewer viewer) {
			super(LaunchView.this);
			fViewer = viewer;
			fViewer.addModelChangedListener(this);
		}

		protected void dispose() {
			fContext = null;
			fViewer.removeModelChangedListener(this);
		}

		@Override
		public synchronized ISelection getActiveContext() {
			return fContext;
		}

		protected void activate(ISelection selection) {
			synchronized (this) {
				fContext = selection;
			}
			fire(new DebugContextEvent(this, selection, DebugContextEvent.ACTIVATED));
		}

		protected void possibleChange(TreePath element, int type) {
			DebugContextEvent event = null;
			synchronized (this) {
				if (fContext instanceof ITreeSelection) {
					ITreeSelection ss = (ITreeSelection) fContext;
					for (TreePath path : ss.getPaths()) {
						if (path.startsWith(element, null)) {
							if (path.getSegmentCount() == element.getSegmentCount()) {
								event = new DebugContextEvent(this, fContext, type);
							} else {
								// if parent of the currently selected element
								// changes, issue event to update STATE only
								event = new DebugContextEvent(this, fContext, DebugContextEvent.STATE);
							}
						}
					}
				}
			}
			if (event == null) {
				return;
			}
			if (getControl().getDisplay().getThread() == Thread.currentThread()) {
				fire(event);
			} else {
				final DebugContextEvent finalEvent = event;
				Job job = new UIJob("context change") { //$NON-NLS-1$
					@Override
					public IStatus runInUIThread(IProgressMonitor monitor) {
						// verify selection is still the same context since job was scheduled
						synchronized (TreeViewerContextProvider.this) {
							if (fContext instanceof IStructuredSelection) {
								IStructuredSelection ss = (IStructuredSelection) fContext;
								Object changed = ((IStructuredSelection)finalEvent.getContext()).getFirstElement();
								if (!(ss.size() == 1 && ss.getFirstElement().equals(changed))) {
									return Status.OK_STATUS;
								}
							}
						}
						fire(finalEvent);
						return Status.OK_STATUS;
					}
				};
				job.setSystem(true);
				job.schedule();
			}
		}

		@Override
		public void modelChanged(IModelDelta delta, IModelProxy proxy) {
			delta.accept(fVisitor);
		}

	}

	/**
	 * Context provider
	 */
	private TreeViewerContextProvider fTreeViewerDebugContextProvider;

	/**
	 * The PageBookView, which is a base class of this class does not make it
	 * easy to control which page is currently active.  It is intended that the
	 * page book pages are associated with workbench parts, and the parts are
	 * in turn associated with PageRec records.
	 * <p>
	 * PageRec is needed in order to properly active a page book page, by
	 * calling showPageRec(), so in this class we need to add some hooks in
	 * order to obtain the page record for the tree viewer page and the
	 * breadcrumb page.</p><p>
	 * For the default page, we override the showPageRec()
	 * to determine if the default page is being shown and if it is, we save
	 * its record for later use.  showPageRec() is always called for the default
	 * page after it is created.  For the breadcrumb page, we use the page book
	 * view mechanism to create the page based on a workbench part, but we have
	 * to create a dummy part in order for this to work.
	 * </p>
	 * <p>
	 * See bug 262845 and 262870.
	 * </p>
	 *
	 * @see #createPartControl(Composite)
	 * @see BreadcrumbWorkbenchPart
	 * @eee #doCreatePage(IWorkbenchPart)
	 * @see #isImportant(IWorkbenchPart)
	 * @see #autoSelectViewPage(Composite)
	 * @see #showTreeViewerPage()
	 * @see #showBreadcrumbPage()
	 */
	private PageRec fDefaultPageRec = null;

	private ISelectionChangedListener fTreeViewerSelectionChangedListener = event -> fTreeViewerDebugContextProvider.activate(event.getSelection());

	private class ContextProviderProxy extends AbstractDebugContextProvider implements IDebugContextListener {
		private IDebugContextProvider fActiveProvider;
		private IDebugContextProvider[] fProviders;

		ContextProviderProxy(IDebugContextProvider[] providers) {
			super(LaunchView.this);
			fProviders = providers;
			fActiveProvider = providers[0];
			for (IDebugContextProvider provider : fProviders) {
				provider.addDebugContextListener(this);
			}
		}

		void setActiveProvider(IDebugContextProvider provider) {
			if (!provider.equals(fActiveProvider)) {
				ISelection activeContext = getActiveContext();
				fActiveProvider = provider;
				ISelection newActiveContext = getActiveContext();
				if (!activeContext.equals(newActiveContext)) {
					fire(new DebugContextEvent(this, getActiveContext(), DebugContextEvent.ACTIVATED));
				}
			}
		}

		@Override
		public ISelection getActiveContext() {
			ISelection activeContext = fActiveProvider.getActiveContext();
			if (activeContext != null) {
				return activeContext;
			}
			return TreeSelection.EMPTY;
		}

		@Override
		public void debugContextChanged(DebugContextEvent event) {
			if (event.getSource().equals(fActiveProvider)) {
				fire(new DebugContextEvent(this, event.getContext(), event.getFlags()));
			}
		}

		void dispose() {
			for (IDebugContextProvider provider : fProviders) {
				provider.removeDebugContextListener(this);
			}
			fProviders = null;
			fActiveProvider = null;
		}
	}

	private ContextProviderProxy fContextProviderProxy;

	@Override
	protected String getHelpContextId() {
		return IDebugHelpContextIds.DEBUG_VIEW;
	}

	@Override
	protected void createActions() {
		setAction("Properties", new PropertyDialogAction(getSite(), getSite().getSelectionProvider())); //$NON-NLS-1$
		fEditConfigAction = new EditLaunchConfigurationAction();
		fAddToFavoritesAction = new AddToFavoritesAction();
		fEditSourceAction = new EditSourceLookupPathAction(this);
		fLookupAction = new LookupSourceAction(this);
		setAction(FIND_ACTION, new VirtualFindAction((TreeModelViewer) getViewer()));

		addCapabilityAction(new TerminateCommandAction(), TERMINATE);
		addCapabilityAction(new DisconnectCommandAction(), DISCONNECT);
		addCapabilityAction(new SuspendCommandAction(), SUSPEND);
		addCapabilityAction(new ResumeCommandAction(), RESUME);
		addCapabilityAction(new StepReturnCommandAction(), STEP_RETURN);
		addCapabilityAction(new StepOverCommandAction(), STEP_OVER);
		addCapabilityAction(new StepIntoCommandAction(), STEP_INTO);
		addCapabilityAction(new DropToFrameCommandAction(), DROP_TO_FRAME);
		DebugCommandAction action = new TerminateAndRemoveAction();
		addCapabilityAction(action, TERMINATE_AND_REMOVE);
		setHandler(TERMINATE_AND_REMOVE, new ActionHandler(action));
		action = new TerminateAndRelaunchAction();
		addCapabilityAction(action, TERMINATE_AND_RELAUNCH);
		setHandler(TERMINATE_AND_RELAUNCH, new ActionHandler(action));
		addCapabilityAction(new RestartCommandAction(), RESTART);
		action = new TerminateAllAction();
		addCapabilityAction(action, TERMINATE_ALL);
		setHandler(TERMINATE_ALL, new ActionHandler(action));
		addCapabilityAction(new ToggleStepFiltersAction(), TOGGLE_STEP_FILTERS);
	}

	/**
	 * Sets the handler associated with the given action identifier.
	 *
	 * @param id action identifier
	 * @param handler handler
	 */
	private void setHandler(String id, IHandler2 handler) {
		fHandlers.put(id, handler);
	}

	/**
	 * Returns the handler associated with the given action identifier or <code>null</code>.
	 *
	 * @param id action identifier
	 * @return handler or <code>null</code>
	 */
	public IHandler2 getHandler(String id) {
		return fHandlers.get(id);
	}

	/**
	 * Initializes the action and associates it with the given id.
	 *
	 * @param capability
	 * @param actionID
	 */
	private void addCapabilityAction(DebugCommandAction capability, String actionID) {
		capability.init(this);
		setAction(actionID, capability);
	}

	/**
	 * Disposes the given action.
	 *
	 * @param actionID
	 */
	private void disposeCommandAction(String actionID) {
		DebugCommandAction action = (DebugCommandAction) getAction(actionID);
		action.dispose();
	}

	/**
	 * Override the default implementation to create the breadcrumb page.
	 *
	 * @since 3.5
	 * @see #fDefaultPageRec
	 */
	@Override
	public void createPartControl(final Composite parent) {
		super.createPartControl(parent);

		// Copy the global action handlers to the default page.
		setGlobalActionBarsToPage((IPageBookViewPage)getDefaultPage());

		// Add view as a selection listener to the site.
		getSite().getSelectionProvider().addSelectionChangedListener(this);

		// Set the tree viewer as the selection provider to the default page.
		// The page book view handles switching the between page selection
		// providers as needed.
		((IPageBookViewPage)getDefaultPage()).getSite().setSelectionProvider(getViewer());

		// Call the PageBookView part listener to indirectly create the breadcrumb page.
		// This call eventually calls doCreatePage() implemented below.
		partActivated(new BreadcrumbWorkbenchPart(getSite()));

		fContextProviderProxy = new ContextProviderProxy(
			new IDebugContextProvider[] {fTreeViewerDebugContextProvider, fBreadcrumbPage.getContextProvider()});
		DebugUITools.getDebugContextManager().getContextService(getSite().getWorkbenchWindow()).addDebugContextProvider(fContextProviderProxy);

		// Create and configure actions for selecting view mode.
		createViewModeActions(parent);
		IPreferenceStore prefStore = DebugUIPlugin.getDefault().getPreferenceStore();
		String mode = prefStore.getString(IDebugPreferenceConstants.DEBUG_VIEW_MODE);
		setViewMode(mode, parent);
		for (DebugViewModeAction action : fDebugViewModeActions) {
			action.setChecked(action.getMode().equals(mode));
		}

		createDebugToolBarInViewActions(parent);

		// Add a resize listener for the view to activate breadcrumb as needed.
		parent.addControlListener(new ControlListener() {
			@Override
			public void controlMoved(ControlEvent e) {
			}
			@Override
			public void controlResized(ControlEvent e) {
				if (parent.isDisposed()) {
					return;
				}
				if (IDebugPreferenceConstants.DEBUG_VIEW_MODE_AUTO.equals(fCurrentViewMode)) {
					autoSelectViewPage(parent);
				}
			}
		});

		fContextService.addContextManagerListener(this);
	}

	/**
	 * Copies the view's global action handlers created by createActions(),
	 * into the page site's action bars.  This is necessary because the page
	 * book view resets the view site's global actions after each page switch
	 * (see bug 264618).
	 *
	 * @param page Page to copy the global actions into.
	 *
	 * @since 3.5
	 */
	private void setGlobalActionBarsToPage(IPageBookViewPage page) {
		IActionBars pageActionBars = page.getSite().getActionBars();
		// Set the view site action bars created by createActions() to the
		// default page site.
		IActionBars bars = getViewSite().getActionBars();
		pageActionBars.setGlobalActionHandler(FIND_ACTION, bars.getGlobalActionHandler(FIND_ACTION));
		pageActionBars.setGlobalActionHandler(COPY_ACTION, bars.getGlobalActionHandler(COPY_ACTION));
	}

	/**
	 * Override the default implementation to create the breadcrumb page.
	 *
	 * @since 3.5
	 * @see #fDefaultPageRec
	 */
	@Override
	protected PageRec doCreatePage(IWorkbenchPart part) {
		if (part instanceof BreadcrumbWorkbenchPart) {
			fBreadcrumbPage = new BreadcrumbPage();
			fBreadcrumbPage.createControl(getPageBook());
			initPage(fBreadcrumbPage);
			setGlobalActionBarsToPage(fBreadcrumbPage);
			return new PageRec(part, fBreadcrumbPage);
		}
		return null;
	}

	/**
	 * Override the default implementation to create the breadcrumb page.
	 *
	 * @since 3.5
	 * @see #fDefaultPageRec
	 */
	@Override
	protected boolean isImportant(IWorkbenchPart part) {
		return part instanceof BreadcrumbWorkbenchPart;
	}

	/**
	 * Override the default implementation to gain access at the default
	 * page record.
	 *
	 * @since 3.5
	 * @see #fDefaultPageRec
	 */
	@Override
	protected void showPageRec(PageRec pageRec) {
		if (pageRec.page == getDefaultPage()) {
			fDefaultPageRec = pageRec;
		}

		super.showPageRec(pageRec);
	}

	/**
	 * Creates actions for controlling view mode.
	 *
	 * @param parent The view's parent control used to calculate view size
	 * in auto mode.
	 */
	private void createViewModeActions(final Composite parent) {
		IActionBars actionBars = getViewSite().getActionBars();
		IMenuManager viewMenu = actionBars.getMenuManager();

		fDebugViewModeActions = new DebugViewModeAction[3];
		fDebugViewModeActions[0] = new DebugViewModeAction(this, IDebugPreferenceConstants.DEBUG_VIEW_MODE_AUTO, parent);
		fDebugViewModeActions[1] = new DebugViewModeAction(this, IDebugPreferenceConstants.DEBUG_VIEW_MODE_FULL, parent);
		fDebugViewModeActions[2] = new DebugViewModeAction(this, IDebugPreferenceConstants.DEBUG_VIEW_MODE_COMPACT, parent);
		fBreadcrumbDropDownAutoExpandAction = new BreadcrumbDropDownAutoExpandAction(this);
		viewMenu.add(new Separator());

		final MenuManager modeSubmenu = new MenuManager(LaunchViewMessages.LaunchView_ViewModeMenu_label);
		modeSubmenu.setRemoveAllWhenShown(true);
		modeSubmenu.add(fDebugViewModeActions[0]);
		modeSubmenu.add(fDebugViewModeActions[1]);
		modeSubmenu.add(fDebugViewModeActions[2]);
		modeSubmenu.add(new Separator());
		modeSubmenu.add(fBreadcrumbDropDownAutoExpandAction);
		viewMenu.add(modeSubmenu);

		modeSubmenu.addMenuListener(manager -> {
			modeSubmenu.add(fDebugViewModeActions[0]);
			modeSubmenu.add(fDebugViewModeActions[1]);
			modeSubmenu.add(fDebugViewModeActions[2]);
			modeSubmenu.add(new Separator());
			modeSubmenu.add(fBreadcrumbDropDownAutoExpandAction);
		});
	}

	/**
	 * Creates actions for controlling view mode.
	 *
	 * @param parent The view's parent control used to calculate view size
	 * in auto mode.
	 */
	private void createDebugToolBarInViewActions(final Composite parent) {
		IActionBars actionBars = getViewSite().getActionBars();
		IMenuManager viewMenu = actionBars.getMenuManager();

		fDebugToolBarAction = new DebugToolBarAction(this);
		viewMenu.add(fDebugToolBarAction);
		updateCheckedDebugToolBarAction();
	}


	/**
	 * Sets the current view mode.  If needed, the active view page is changed.
	 *
	 * @param mode New mode to set.
	 * @param parent The view's parent control used to calculate view size
	 * in auto mode.
	 *
	 * @since 3.5
	 */
	void setViewMode(String mode, Composite parent) {
		if (fCurrentViewMode.equals(mode)) {
			return;
		}

		fCurrentViewMode = mode;
		if (IDebugPreferenceConstants.DEBUG_VIEW_MODE_COMPACT.equals(mode)) {
			showBreadcrumbPage();
		} else if (IDebugPreferenceConstants.DEBUG_VIEW_MODE_FULL.equals(mode)) {
			showTreeViewerPage();
		} else {
			autoSelectViewPage(parent);
		}
		DebugUIPlugin.getDefault().getPreferenceStore().setValue(IDebugPreferenceConstants.DEBUG_VIEW_MODE, mode);
	}

	/**
	* Based on the current view size, select the active view page
	* (tree viewer vs. breadcrumb).
	*
	* @param parent View pane control.
	*
	* @since 3.5
	*/
	private void autoSelectViewPage(Composite parent) {
		int breadcrumbHeight = fBreadcrumbPage.getHeight();
		// Breadcrumb may report size 0 if it hasn't been shown yet.
		// Bug 335536.
		if (breadcrumbHeight == 0) {
			breadcrumbHeight = BREADCRUMB_TRIGGER_HEIGHT_DEFAULT;
		}
		if (parent.getClientArea().height < breadcrumbHeight + BREADCRUMB_TRIGGER_RANGE) {
			showBreadcrumbPage();
		}
		else if (parent.getClientArea().height > breadcrumbHeight + BREADCRUMB_STICKY_RANGE)
		{
			showTreeViewerPage();
		}
	}


	/**
	 * Shows the tree viewer in the Debug view.
	 *
	 * @since 3.5
	 */
	void showTreeViewerPage() {
		if (fDefaultPageRec != null && !getDefaultPage().equals(getCurrentPage())) {
			showPageRec(fDefaultPageRec);
			fContextProviderProxy.setActiveProvider(fTreeViewerDebugContextProvider);
			// Clear the selection in the breadcrumb to avoid having it re-selected
			// when the breadcrumb is activated again (bug 268124).
			fBreadcrumbPage.fCrumb.clearSelection();
		}
	}

	/**
	 * Shows the breadcrumb in the Debug view.
	 *
	 * @since 3.5
	 */
	void showBreadcrumbPage() {
		PageRec rec = getPageRec(fBreadcrumbPage);
		if (rec != null && !fBreadcrumbPage.equals(getCurrentPage())) {
			showPageRec(rec);
			// Ask the breadcrumb to take focus if we're the active part.
			if (getSite().getPage().getActivePart() == this) {
				setFocus();
			}
			ISelection activeContext = fTreeViewerDebugContextProvider.getActiveContext();
			if (activeContext == null) {
				activeContext = StructuredSelection.EMPTY;
			}
			fBreadcrumbPage.fCrumb.debugContextChanged(new DebugContextEvent(
				fTreeViewerDebugContextProvider,
				activeContext,
				DebugContextEvent.ACTIVATED));
			fContextProviderProxy.setActiveProvider(fBreadcrumbPage.getContextProvider());
		}
	}

	@Override
	protected Viewer createViewer(Composite parent) {
		fPresentation = new DelegatingModelPresentation();
		fPresentationContext = new DebugModelPresentationContext(IDebugUIConstants.ID_DEBUG_VIEW, this, fPresentation);
		TreeModelViewer viewer = new TreeModelViewer(parent,
				SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.VIRTUAL,
				fPresentationContext);

		viewer.addSelectionChangedListener(fTreeViewerSelectionChangedListener);
		viewer.addViewerUpdateListener(this);
		viewer.addModelChangedListener(this);

		viewer.setInput(DebugPlugin.getDefault().getLaunchManager());
		//setEventHandler(new LaunchViewEventHandler(this));
		fTreeViewerDebugContextProvider = new TreeViewerContextProvider(viewer);

		return viewer;
	}

	private void commonInit(IViewSite site) {
		site.getPage().addPartListener((IPartListener2) this);
		site.getWorkbenchWindow().addPageListener(this);
		site.getWorkbenchWindow().addPerspectiveListener(this);
	}

	private void preferenceInit(IViewSite site) {
		PREF_STATE_MEMENTO = PREF_STATE_MEMENTO + site.getId();
		IPreferenceStore store = DebugUIPlugin.getDefault().getPreferenceStore();
		String string = store.getString(PREF_STATE_MEMENTO);
		if(string.length() > 0) {
			try (ByteArrayInputStream bin = new ByteArrayInputStream(string.getBytes()); InputStreamReader reader = new InputStreamReader(bin)) {
				XMLMemento stateMemento = XMLMemento.createReadRoot(reader);
				setMemento(stateMemento);
			} catch (WorkbenchException e) {
			} catch (IOException e1) {
			}
		}
		IMemento mem = getMemento();

		if (mem != null) {
			Boolean auto = mem.getBoolean(BREADCRUMB_DROPDOWN_AUTO_EXPAND);
			if(auto != null) {
				setBreadcrumbDropDownAutoExpand(auto.booleanValue());
			}
		}

		String preference = DebugUIPlugin.getDefault().getPreferenceStore().getString(
			IDebugPreferenceConstants.DEBUG_VIEW_TOOLBAR_HIDDEN_PERSPECTIVES);
		if (preference != null) {
			fDebugToolbarPerspectives = ViewContextService.parseList(preference);
		}
		IPerspectiveDescriptor perspective = getSite().getPage().getPerspective();
		fDebugToolbarInView = isDebugToolbarShownInPerspective(perspective);
	}

	@Override
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		commonInit(site);
		preferenceInit(site);
		fContextService = site.getService(IContextService.class);
	}

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		commonInit(site);
		preferenceInit(site);
		fContextService = site.getService(IContextService.class);
	}

	@Override
	public void partDeactivated(IWorkbenchPart part) {
		String id = part.getSite().getId();
		if (id.equals(getSite().getId())) {
			try (ByteArrayOutputStream bout = new ByteArrayOutputStream(); OutputStreamWriter writer = new OutputStreamWriter(bout)) {
				XMLMemento memento = XMLMemento.createWriteRoot("DebugViewMemento"); //$NON-NLS-1$
				saveViewerState(memento);
				memento.save(writer);

				IPreferenceStore store = DebugUIPlugin.getDefault().getPreferenceStore();
				String xmlString = bout.toString();
				store.putValue(PREF_STATE_MEMENTO, xmlString);
			} catch (IOException e) {
			}
		}

		StringBuilder buffer= new StringBuilder();
		for (String perspectiveId : fDebugToolbarPerspectives) {
			buffer.append(perspectiveId).append(',');
		}
		getPreferenceStore().setValue(IDebugPreferenceConstants.DEBUG_VIEW_TOOLBAR_HIDDEN_PERSPECTIVES, buffer.toString());

		super.partDeactivated(part);
	}

	/**
	 * Saves the current state of the viewer
	 * @param memento the memento to write the viewer state into
	 */
	public void saveViewerState(IMemento memento) {
		memento.putBoolean(BREADCRUMB_DROPDOWN_AUTO_EXPAND, getBreadcrumbDropDownAutoExpand());
	}

	@Override
	protected void configureToolBar(IToolBarManager tbm) {
		tbm.add(new Separator(IDebugUIConstants.THREAD_GROUP));
		tbm.add(new Separator(IDebugUIConstants.STEP_GROUP));
		tbm.add(new GroupMarker(IDebugUIConstants.STEP_INTO_GROUP));
		tbm.add(new GroupMarker(IDebugUIConstants.STEP_OVER_GROUP));
		tbm.add(new GroupMarker(IDebugUIConstants.STEP_RETURN_GROUP));
		tbm.add(new GroupMarker(IDebugUIConstants.EMPTY_STEP_GROUP));
		tbm.add(new Separator(IDebugUIConstants.RENDER_GROUP));

		if (fDebugToolbarInView) {
			addDebugToolbarActions(tbm);
		}
	}

	protected void addDebugToolbarActions(IToolBarManager tbm) {
		tbm.appendToGroup(IDebugUIConstants.THREAD_GROUP, getAction(RESUME));
		tbm.appendToGroup(IDebugUIConstants.THREAD_GROUP, getAction(SUSPEND));
		tbm.appendToGroup(IDebugUIConstants.THREAD_GROUP, getAction(TERMINATE));
		tbm.appendToGroup(IDebugUIConstants.THREAD_GROUP, getAction(DISCONNECT));

		tbm.appendToGroup(IDebugUIConstants.STEP_INTO_GROUP, getAction(STEP_INTO));
		tbm.appendToGroup(IDebugUIConstants.STEP_OVER_GROUP, getAction(STEP_OVER));
		tbm.appendToGroup(IDebugUIConstants.STEP_RETURN_GROUP, getAction(STEP_RETURN));

		tbm.appendToGroup(IDebugUIConstants.EMPTY_STEP_GROUP, getAction(DROP_TO_FRAME));

		tbm.appendToGroup(IDebugUIConstants.RENDER_GROUP, getAction(TOGGLE_STEP_FILTERS));
	}

	/**
	* Removes the toolbar actions contributed by this view from the toolbar
	* manager.
	* @param tbm
	*/
	protected void removeDebugToolbarActions(IToolBarManager tbm) {
		tbm.remove(new ActionContributionItem(getAction(RESUME)));
		tbm.remove(new ActionContributionItem(getAction(SUSPEND)));
		tbm.remove(new ActionContributionItem(getAction(TERMINATE)));
		tbm.remove(new ActionContributionItem(getAction(DISCONNECT)));

		tbm.remove(new ActionContributionItem(getAction(STEP_INTO)));
		tbm.remove(new ActionContributionItem(getAction(STEP_OVER)));
		tbm.remove(new ActionContributionItem(getAction(STEP_RETURN)));

		tbm.remove(new ActionContributionItem(getAction(DROP_TO_FRAME)));

		tbm.remove(new ActionContributionItem(getAction(TOGGLE_STEP_FILTERS)));
	}

	public boolean isDebugToolbarInView() {
		return fDebugToolbarInView;
	}

	public boolean isDebugToolbarShownInPerspective(IPerspectiveDescriptor perspective) {
		return perspective == null || fDebugToolbarPerspectives.contains(perspective.getId());
	}

	public void setDebugToolbarInView(boolean show) {
		if (show == isDebugToolbarInView()) {
			return;
		}
		fDebugToolbarInView = show;

		// Update the perspectives set.
		IPerspectiveDescriptor perspective = getSite().getPage().getPerspective();
		if (perspective != null) {
			if (show) {
				fDebugToolbarPerspectives.add(perspective.getId());
			} else {
				fDebugToolbarPerspectives.remove(perspective.getId());
			}
		}

		// Update the toolbar manager.
		IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
		if (show) {
			addDebugToolbarActions(tbm);
		} else {
			removeDebugToolbarActions(tbm);
		}
		getViewSite().getActionBars().updateActionBars();

		// Update system property used by contributed actions.
		if (!Boolean.toString(show).equals(System.getProperty(IDebugUIConstants.DEBUG_VIEW_TOOBAR_VISIBLE))) {
			try {
				System.setProperty(IDebugUIConstants.DEBUG_VIEW_TOOBAR_VISIBLE, Boolean.toString(show));
			} catch (SecurityException ex) {
				// ignore
			}
		}
	}

	@Override
	public void dispose() {
		fContextService.removeContextManagerListener(this);
		getSite().getSelectionProvider().removeSelectionChangedListener(this);
		DebugUITools.getDebugContextManager().getContextService(getSite().getWorkbenchWindow()).removeDebugContextProvider(fContextProviderProxy);
		fContextProviderProxy.dispose();
		fTreeViewerDebugContextProvider.dispose();
		disposeActions();
		Viewer viewer = getViewer();
		if (viewer != null) {
			viewer.removeSelectionChangedListener(fTreeViewerSelectionChangedListener);
			((TreeModelViewer)viewer).removeViewerUpdateListener(this);
			((TreeModelViewer)viewer).removeModelChangedListener(this);
		}
		if (fPresentationContext != null) {
			fPresentationContext.dispose();
		}
		IWorkbenchPage page = getSite().getPage();
		page.removePartListener((IPartListener2) this);
		IWorkbenchWindow window = getSite().getWorkbenchWindow();
		window.removePerspectiveListener(this);
		window.removePageListener(this);
		for (IHandler2 handler : fHandlers.values()) {
			handler.dispose();
		}
		fHandlers.clear();
		if (fBreadcrumbPage != null) {
			fBreadcrumbPage.dispose();
			fBreadcrumbPage = null;
		}
		super.dispose();
	}

	private void disposeActions() {
		PropertyDialogAction properties = (PropertyDialogAction) getAction("Properties"); //$NON-NLS-1$
		properties.dispose();

		disposeCommandAction(TERMINATE);
		disposeCommandAction(DISCONNECT);
		disposeCommandAction(SUSPEND);
		disposeCommandAction(RESUME);
		disposeCommandAction(STEP_RETURN);
		disposeCommandAction(STEP_OVER);
		disposeCommandAction(STEP_INTO);
		disposeCommandAction(DROP_TO_FRAME);
		disposeCommandAction(TERMINATE_AND_REMOVE);
		disposeCommandAction(TERMINATE_AND_RELAUNCH);
		disposeCommandAction(RESTART);
		disposeCommandAction(TERMINATE_ALL);
		disposeCommandAction(TOGGLE_STEP_FILTERS);
	}

	/**
	 * The selection has changed in the viewer. Show the
	 * associated source code if it is a stack frame.
	 *
	 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
	 */
	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		updateObjects();
	}

	@Override
	public void doubleClick(DoubleClickEvent event) {
		ISelection selection= event.getSelection();
		if (!(selection instanceof IStructuredSelection)) {
			return;
		}
		IStructuredSelection ss= (IStructuredSelection)selection;
		Object o= ss.getFirstElement();
		if (o == null || o instanceof IStackFrame) {
			return;
		}
		StructuredViewer viewer = (StructuredViewer) getViewer();
		viewer.refresh(o);
	}

	@Override
	public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
		setActive(page.findView(getSite().getId()) != null);
		updateObjects();
		setDebugToolbarInView( isDebugToolbarShownInPerspective(getSite().getPage().getPerspective()) );
		updateCheckedDebugToolBarAction();
	}

	@Override
	public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective, String changeId) {
		setActive(page.findView(getSite().getId()) != null);
	}

	@Override
	public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective, IWorkbenchPartReference partRef, String changeId) {
	}

	@Override
	public void pageActivated(IWorkbenchPage page) {
		if (getSite().getPage().equals(page)) {
			setActive(true);
			updateObjects();
		}
	}

	@Override
	public void pageClosed(IWorkbenchPage page) {
	}

	@Override
	public void pageOpened(IWorkbenchPage page) {
	}

	@Override
	public IDebugModelPresentation getPresentation(String id) {
		return ((DelegatingModelPresentation)fPresentation).getPresentation(id);
	}

	@Override
	protected void fillContextMenu(IMenuManager menu) {
		TreeSelection sel = (TreeSelection) fTreeViewerDebugContextProvider.getActiveContext();
		Object element = sel != null && sel.size() > 0 ? sel.getFirstElement() : null;

		menu.add(new Separator(IDebugUIConstants.EMPTY_EDIT_GROUP));
		menu.add(new Separator(IDebugUIConstants.EDIT_GROUP));
		menu.add(getAction(FIND_ACTION));
		menu.add(new Separator(IDebugUIConstants.EMPTY_STEP_GROUP));
		menu.add(new Separator(IDebugUIConstants.STEP_GROUP));
		menu.add(new GroupMarker(IDebugUIConstants.STEP_INTO_GROUP));
		menu.add(new GroupMarker(IDebugUIConstants.STEP_OVER_GROUP));
		menu.add(new GroupMarker(IDebugUIConstants.STEP_RETURN_GROUP));
		menu.add(new Separator(IDebugUIConstants.RENDER_GROUP));
		menu.add(new Separator(IDebugUIConstants.EMPTY_THREAD_GROUP));
		menu.add(new Separator(IDebugUIConstants.THREAD_GROUP));
		menu.add(new Separator(IDebugUIConstants.EMPTY_LAUNCH_GROUP));
		menu.add(new Separator(IDebugUIConstants.LAUNCH_GROUP));
		IStructuredSelection selection = (IStructuredSelection) getSite().getSelectionProvider().getSelection();
		updateAndAdd(menu, fEditConfigAction, selection);
		updateAndAdd(menu, fAddToFavoritesAction, selection);
		updateAndAdd(menu, fEditSourceAction, selection);
		updateAndAdd(menu, fLookupAction, selection);
		menu.add(new Separator(IDebugUIConstants.EMPTY_RENDER_GROUP));
		menu.add(new Separator(IDebugUIConstants.RENDER_GROUP));
		menu.add(new Separator(IDebugUIConstants.PROPERTY_GROUP));
		PropertyDialogAction action = (PropertyDialogAction)getAction("Properties"); //$NON-NLS-1$
		/**
		 * TODO hack to get around bug 148424, remove if UI ever fixes the PropertyDialogAction to respect enablesWhen conditions
		 */
		action.setEnabled(action.isApplicableForSelection() && !(element instanceof ILaunch));
		menu.add(action);
		menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

		menu.appendToGroup(IDebugUIConstants.LAUNCH_GROUP, getAction(TERMINATE_AND_REMOVE));
		menu.appendToGroup(IDebugUIConstants.LAUNCH_GROUP, getAction(TERMINATE_ALL));

		menu.appendToGroup(IDebugUIConstants.THREAD_GROUP, getAction(RESUME));
		menu.appendToGroup(IDebugUIConstants.THREAD_GROUP, getAction(SUSPEND));
		menu.appendToGroup(IDebugUIConstants.THREAD_GROUP, getAction(TERMINATE));
		menu.appendToGroup(IDebugUIConstants.THREAD_GROUP, getAction(TERMINATE_AND_RELAUNCH));
		if (element instanceof IAdaptable && ((IAdaptable)element).getAdapter(IRestartHandler.class) != null) {
			menu.appendToGroup(IDebugUIConstants.THREAD_GROUP, getAction(RESTART));
		}
		menu.appendToGroup(IDebugUIConstants.THREAD_GROUP, getAction(DISCONNECT));

		menu.appendToGroup(IDebugUIConstants.STEP_INTO_GROUP, getAction(STEP_INTO));
		menu.appendToGroup(IDebugUIConstants.STEP_OVER_GROUP, getAction(STEP_OVER));
		menu.appendToGroup(IDebugUIConstants.STEP_RETURN_GROUP, getAction(STEP_RETURN));

		menu.appendToGroup(IDebugUIConstants.EMPTY_STEP_GROUP, getAction(DROP_TO_FRAME));

		menu.appendToGroup(IDebugUIConstants.RENDER_GROUP, getAction(TOGGLE_STEP_FILTERS));
	}

	@Override
	public void contextManagerChanged(ContextManagerEvent event) {
		if (event.isActiveContextsChanged()) {
			Set<?> oldContexts = event.getPreviouslyActiveContextIds();
			Set<?> newContexts = event.getContextManager().getActiveContextIds();
			if (oldContexts.contains(IDebugUIConstants.DEBUG_TOOLBAR_ACTION_SET) !=
				newContexts.contains(IDebugUIConstants.DEBUG_TOOLBAR_ACTION_SET))
			{
				updateCheckedDebugToolBarAction();
			}
		}
	}

	private void updateCheckedDebugToolBarAction() {
		fDebugToolBarAction.setChecked(isDebugToolbarInView());
	}

	/**
	 * Updates the enabled state of the given action based on the selection
	 * and adds to the menu if enabled.
	 *
	 * @param menu menu to add the action to
	 * @param action action to add if enabled
	 * @param selection selection to update enabled state for
	 */
	private void updateAndAdd(IMenuManager menu, SelectionListenerAction action, IStructuredSelection selection) {
		action.selectionChanged(selection);
		if (action.isEnabled()) {
			menu.add(action);
		}
	}

	/**
	 * Sets whether this view is in the active page of a
	 * perspective. Since a page can have more than one
	 * perspective, this view only show's source when in
	 * the active perspective/page.
	 *
	 * @param active whether this view is in the active page of a
	 * perspective
	 */
	protected void setActive(boolean active) {
		fIsActive = active;
	}

	/**
	 * Returns whether this view is in the active page of
	 * the active perspective and has been fully created.
	 *
	 * @return whether this view is in the active page of
	 * the active perspective and has been fully created.
	 */
	protected boolean isActive() {
		return fIsActive && getViewer() != null;
	}

	@Override
	public boolean show(ShowInContext context) {
		ISelection selection = context.getSelection();
		if (selection != null) {
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection ss = (IStructuredSelection)selection;
				if (ss.size() == 1) {
					Object obj = ss.getFirstElement();
					if (obj instanceof IDebugTarget || obj instanceof IProcess) {
						Viewer viewer = getViewer();
						if (viewer instanceof InternalTreeModelViewer) {
							InternalTreeModelViewer tv = (InternalTreeModelViewer) viewer;
							tv.setSelection(selection, true, true);
						} else {
							viewer.setSelection(selection, true);
						}
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public ShowInContext getShowInContext() {
		if (isActive()) {
			IStructuredSelection selection = (IStructuredSelection)getViewer().getSelection();
			if (selection.size() == 1) {
				Object object = selection.getFirstElement();
				if (object instanceof IAdaptable) {
					IAdaptable adaptable = (IAdaptable) object;
					IShowInSource show = adaptable.getAdapter(IShowInSource.class);
					if (show != null) {
						return show.getShowInContext();
					}
				}
			}
		}
		return null;
	}

	@Override
	public String[] getShowInTargetIds() {
		if (isActive()) {
			IStructuredSelection selection = (IStructuredSelection)getViewer().getSelection();
			if (selection.size() == 1) {
				Object object = selection.getFirstElement();
				if (object instanceof IAdaptable) {
					IAdaptable adaptable = (IAdaptable) object;
					IShowInTargetList show = adaptable.getAdapter(IShowInTargetList.class);
					if (show != null) {
						return show.getShowInTargetIds();
					}
				}
			}
		}
		return new String[0];
	}

	@Override
	public void partClosed(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partVisible(IWorkbenchPartReference partRef) {
		IWorkbenchPart part = partRef.getPart(false);
		if (part == this) {
			setActive(true);
// TODO: Workaround for Bug #63332. Reexamine after M9.
//			updateContextListener();
			// When the launch view becomes visible, turn on the
			// debug action set. Note that the workbench will handle the
			// case where the user really doesn't want the action set
			// enabled - showActionSet(String) will do nothing for an
			// action set that's been manually disabled.
			getSite().getPage().showActionSet(IDebugUIConstants.DEBUG_ACTION_SET);
		}
	}

	@Override
	public void partOpened(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partActivated(IWorkbenchPartReference partRef) {
		// Ensure that the system property matches the debug toolbar state.
		// Bug 385400
		String debugToolBarShown = Boolean
				.toString(isDebugToolbarShownInPerspective(getSite().getPage().getPerspective()));
		if (!(debugToolBarShown.equals(System.getProperty(IDebugUIConstants.DEBUG_VIEW_TOOBAR_VISIBLE)))) {
			try {
				System.setProperty(IDebugUIConstants.DEBUG_VIEW_TOOBAR_VISIBLE, debugToolBarShown);
			} catch (SecurityException ex) {
				// ignore
			}
		}
	}

	@Override
	public void partBroughtToTop(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partDeactivated(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partHidden(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partInputChanged(IWorkbenchPartReference partRef) {
	}

	@Override
	protected void becomesVisible() {
		super.becomesVisible();
		getViewer().refresh();
	}

	@Override
	public void updateComplete(IViewerUpdate update) {
		if (!update.isCanceled()) {
			if (TreePath.EMPTY.equals(update.getElementPath())) {
				updateFindAction();
			}
		}
	}

	@Override
	public void updateStarted(IViewerUpdate update) {
	}

	@Override
	public synchronized void viewerUpdatesBegin() {
		IWorkbenchSiteProgressService progressService =
			getSite().getAdapter(IWorkbenchSiteProgressService.class);
		if (progressService != null) {
			progressService.incrementBusy();
		}
	}

	@Override
	public synchronized void viewerUpdatesComplete() {
		IWorkbenchSiteProgressService progressService =
			getSite().getAdapter(IWorkbenchSiteProgressService.class);
		if (progressService != null) {
			progressService.decrementBusy();
		}
	}

	@Override
	public void modelChanged(IModelDelta delta, IModelProxy proxy) {
		updateFindAction();
	}

	private void updateFindAction() {
		IAction action= getAction(FIND_ACTION);
		if (action instanceof IUpdate) {
			((IUpdate) action).update();
		}
	}

	/**
	 * Returns whether the breadcrumb viewer is currently visible in the view.
	 *
	 * @since 3.5
	 */
	boolean isBreadcrumbVisible() {
		return fBreadcrumbPage.equals(getCurrentPage());
	}

	/**
	 * Returns whether the elements in breadcrumb's drop-down viewer should be
	 * automatically expanded.
	 *
	 * @since 3.5
	 */
	boolean getBreadcrumbDropDownAutoExpand() {
		return fBreadcrumbDropDownAutoExpand;
	}

	/**
	 * Sets whether the elements in breadcrumb's drop-down viewer should be
	 * automatically expanded.
	 *
	 * @since 3.5
	 */
	void setBreadcrumbDropDownAutoExpand(boolean expand) {
		fBreadcrumbDropDownAutoExpand = expand;
	}

}
