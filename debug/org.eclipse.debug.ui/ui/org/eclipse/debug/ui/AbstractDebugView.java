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
 *******************************************************************************/
package org.eclipse.debug.ui;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.DelegatingModelPresentation;
import org.eclipse.debug.internal.ui.LazyModelPresentation;
import org.eclipse.debug.internal.ui.actions.breakpoints.SkipAllBreakpointsAction;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.IPage;
import org.eclipse.ui.part.MessagePage;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.PageBookView;
import org.eclipse.ui.texteditor.IUpdate;

/**
 * Common function for views related to debugging. Clients implementing
 * views for a debugger should subclass this class. Common function
 * includes:
 * <ul>
 * <li>Debug view adapter implementation - <code>IDebugView</code></li>
 * <li>Action registry - actions can be stored in this view
 * 		with a key. Actions that implement <code>IUpdate</code>
 *      are updated when <code>updateActions()</code> is
 *		called.</li>
 * <li>Hooks the context menu associated with this view's
 * 		underlying viewer and registers the menu with this
 * 		view's site, such that other plug-ins may contribute.</li>
 * <li>Hooks a key press listener, and invokes the
 * 		<code>REMOVE_ACTION</code> when the delete key
 * 		is pressed.</li>
 * <li>Hooks a double-click listener, and invokes the
 * 		<code>DOUBLE_CLICK_ACTION</code> when the mouse
 * 		is double-clicked.</li>
 * <li>Provides a mechanism for displaying an error message
 * 		in the view, via the <code>PageBookView</code> mechanism.
 * 		By default, a page book is created with a page showing
 * 		this view's viewer. A message page is also created
 * 		and shown when <code>showMessage(String)</code> is
 * 		called.</li>
 * <li>Notification when this view becomes visible and becomes
 * 		hidden via <code>becomesVisible()</code> and <code>becomesHidden()</code>.</li>
 * <li>Linking of a help context id via <code>getHelpContextId().</code></li>
 * </ul>
 * <p>
 * This class may be sub-classed.
 * </p>
 * @since 2.0
 */

public abstract class AbstractDebugView extends PageBookView implements IDebugView, IDoubleClickListener {

	/**
	 * Underlying viewer that displays the contents of
	 * this view.
	 */
	private Viewer fViewer = null;

	/**
	 * This view's message page.
	 */
	private MessagePage fMessagePage = null;

	/**
	 * Map of actions. Keys are strings, values
	 * are <code>IAction</code>.
	 */
	private Map<String, IAction> fActionMap = null;

	/**
	 * Map of actions. Keys are strings, values
	 * are <code>IAction</code>.
	 */
	private List<IUpdate> fUpdateables = null;

	/**
	 * The collection of menu managers that are
	 * relevant for this view.
	 */
	private List<IMenuManager> fContextMenuManagers;

	/**
	 * The memento that was used to persist the state of this view.
	 * May be <code>null</code>.
	 */
	private IMemento fMemento;

	/**
	 * Whether this view is currently visible.
	 */
	private boolean fIsVisible = false;

	/**
	 * The part listener for this view, used to notify this view when it
	 * becomes visible and hidden. Set to <code>null</code> when this view isn't
	 * currently listening to part changes.
	 */
	private DebugViewPartListener fPartListener= null;

	/**
	 * A message was requested to be displayed before the view was fully
	 * created. The message is cached until it can be properly displayed.
	 */
	private String fEarlyMessage= null;

	private static Set<String> fgGlobalActionIds;
	static {
		fgGlobalActionIds = new HashSet<>();
		fgGlobalActionIds.add(SELECT_ALL_ACTION);
		fgGlobalActionIds.add(COPY_ACTION);
		fgGlobalActionIds.add(CUT_ACTION);
		fgGlobalActionIds.add(PASTE_ACTION);
		fgGlobalActionIds.add(FIND_ACTION);
		fgGlobalActionIds.add(ActionFactory.UNDO.getId());
		fgGlobalActionIds.add(ActionFactory.REDO.getId());
		fgGlobalActionIds.add(ActionFactory.RENAME.getId());
	}

	/**
	 * Part listener that disables updating when the view is not visible and
	 * re-enables updating when the view appears.
	 */
	private class DebugViewPartListener implements IPartListener2 {
		/**
		 *
		 * @see org.eclipse.ui.IPartListener2#partVisible(IWorkbenchPartReference)
		 */
		@Override
		public void partVisible(IWorkbenchPartReference ref) {
			IWorkbenchPart part= ref.getPart(false);
			if (part == AbstractDebugView.this) {
				fIsVisible = true;
				becomesVisible();
			}
		}
		/**
		 * @see org.eclipse.ui.IPartListener2#partHidden(IWorkbenchPartReference)
		 */
		@Override
		public void partHidden(IWorkbenchPartReference ref) {
			IWorkbenchPart part= ref.getPart(false);
			if (part == AbstractDebugView.this) {
				fIsVisible = false;
				becomesHidden();
			}
		}
		/**
		 * @see org.eclipse.ui.IPartListener2#partActivated(IWorkbenchPartReference)
		 */
		@Override
		public void partActivated(IWorkbenchPartReference ref) {
		}

		/**
		 * @see org.eclipse.ui.IPartListener2#partBroughtToTop(IWorkbenchPartReference)
		 */
		@Override
		public void partBroughtToTop(IWorkbenchPartReference ref) {
		}

		/**
		 * @see org.eclipse.ui.IPartListener2#partClosed(IWorkbenchPartReference)
		 */
		@Override
		public void partClosed(IWorkbenchPartReference ref) {
		}

		/**
		 * @see org.eclipse.ui.IPartListener2#partDeactivated(IWorkbenchPartReference)
		 */
		@Override
		public void partDeactivated(IWorkbenchPartReference ref) {
		}

		/**
		 * @see org.eclipse.ui.IPartListener2#partOpened(IWorkbenchPartReference)
		 */
		@Override
		public void partOpened(IWorkbenchPartReference ref) {
		}

		/**
		 * @see org.eclipse.ui.IPartListener2#partInputChanged(IWorkbenchPartReference)
		 */
		@Override
		public void partInputChanged(IWorkbenchPartReference ref){
		}

	}

	/**
	 * Constructs a new debug view.
	 */
	public AbstractDebugView() {
		fActionMap = new HashMap<>(5);
		fUpdateables = new ArrayList<>(3);
	}

	/**
	 * Debug views implement the debug view adapter which
	 * provides access to a view's underlying viewer and
	 * debug model presentation for a specific debug model.
	 *
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 * @see IDebugView
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IDebugView.class) {
			return (T) this;
		}
		if (adapter == IDebugModelPresentation.class) {
			StructuredViewer viewer = getStructuredViewer();
			if (viewer != null) {
				IBaseLabelProvider labelProvider = viewer.getLabelProvider();
				if (labelProvider instanceof IDebugModelPresentation) {
					return (T) labelProvider;
				}
			}
		}
		return super.getAdapter(adapter);
	}

	/**
	 * A page in this view's page book that contains this
	 * view's viewer.
	 */
	class ViewerPage extends Page {
		/**
		 * @see IPage#createControl(Composite)
		 */
		@Override
		public void createControl(Composite parent) {
			Viewer viewer = createViewer(parent);
			setViewer(viewer);
		}

		/**
		 * @see IPage#getControl()
		 */
		@Override
		public Control getControl() {
			return getDefaultControl();
		}

		/**
		 * @see IPage#setFocus()
		 */
		@Override
		public void setFocus() {
			Viewer viewer= getViewer();
			if (viewer != null) {
				Control c = viewer.getControl();
				if (!c.isFocusControl()) {
					c.setFocus();
				}
			}
		}

}

	/**
	 * Creates this view's underlying viewer and actions.
	 * Hooks a pop-up menu to the underlying viewer's control,
	 * as well as a key listener. When the delete key is pressed,
	 * the <code>REMOVE_ACTION</code> is invoked. Hooks help to
	 * this view. Subclasses must implement the following methods
	 * which are called in the following order when a view is
	 * created:<ul>
	 * <li><code>createViewer(Composite)</code> - the context
	 *   menu is hooked to the viewer's control.</li>
	 * <li><code>createActions()</code></li>
	 * <li><code>configureToolBar(IToolBarManager)</code></li>
	 * <li><code>getHelpContextId()</code></li>
	 * </ul>
	 * @see IWorkbenchPart#createPartControl(Composite)
	 * @see AbstractDebugView#createPartControl(Composite)
	 * @see AbstractDebugView#createActions()
	 * @see AbstractDebugView#configureToolBar(IToolBarManager)
	 * @see AbstractDebugView#getHelpContextId()
	 * @see AbstractDebugView#fillContextMenu(IMenuManager)
	 */
	@Override
	public void createPartControl(Composite parent) {
		registerPartListener();
		super.createPartControl(parent);
		createActions();
		initializeToolBar();
		Viewer viewer = getViewer();
		if (viewer != null) {
			createContextMenu(viewer.getControl());
		}
		String helpId = getHelpContextId();
		if (helpId != null) {
			PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, helpId);
		}
		if (viewer != null) {
			getViewer().getControl().addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e) {
					handleKeyPressed(e);
				}
			});
			if (getViewer() instanceof StructuredViewer) {
				((StructuredViewer)getViewer()).addDoubleClickListener(this);
			}
		}
		// create the message page
		setMessagePage(new MessagePage());
		getMessagePage().createControl(getPageBook());
		initPage(getMessagePage());

		if (fEarlyMessage != null) { //bug 28127
			showMessage(fEarlyMessage);
			fEarlyMessage= null;
		}
	}

	/**
	 * The default page for a debug view is its viewer.
	 *
	 * @see PageBookView#createDefaultPage(PageBook)
	 */
	@Override
	protected IPage createDefaultPage(PageBook book) {
		ViewerPage page = new ViewerPage();
		page.createControl(book);
		initPage(page);
		return page;
	}

	/**
	 * Creates and returns this view's underlying viewer.
	 * The viewer's control will automatically be hooked
	 * to display a pop-up menu that other plug-ins may
	 * contribute to. Subclasses must override this method.
	 *
	 * @param parent the parent control
	 * @return the new {@link Viewer}
	 */
	protected abstract Viewer createViewer(Composite parent);

	/**
	 * Creates this view's actions. Subclasses must
	 * override this method, which is called after
	 * <code>createViewer(Composite)</code>
	 */
	protected abstract void createActions();

	/**
	 * Returns this view's help context id, which is hooked
	 * to this view on creation.
	 *
	 * @return help context id
	 */
	protected abstract String getHelpContextId();

	/**
	 * @see IWorkbenchPart#dispose()
	 */
	@Override
	public void dispose() {
		saveAllCheckedActionStates();
		deregisterPartListener();
		if (getViewer() instanceof StructuredViewer) {
			((StructuredViewer)getViewer()).removeDoubleClickListener(this);
		}
		setViewer(null);
		fActionMap.clear();
		if (fMessagePage != null) {
			fMessagePage.dispose();
			fMessagePage = null;
		}
		super.dispose();
	}

	/**
	 * Saves the checked state for all actions contributed to the toolbar
	 * manager that function as a toggle action.  The states are saved in
	 * the Debug UI plugin's preference store.
	 *
	 * @since 2.1
	 */
	protected void saveAllCheckedActionStates() {
		IToolBarManager tbm= getViewSite().getActionBars().getToolBarManager();
		IContributionItem[] items= tbm.getItems();
		for (IContributionItem contitem : items) {
			if (contitem instanceof ActionContributionItem) {
				ActionContributionItem item= (ActionContributionItem)contitem;
				IAction action= item.getAction();
				if (action.getStyle() == IAction.AS_CHECK_BOX && action.isEnabled()) {
					saveCheckedActionState(action);
				}
			}
		}
	}

	/**
	 * Save the checked state of the specified action in the Debug UI plugin's
	 * preference store.  The specified action is expected to be enabled and
	 * support the style <code>IAction.AS_CHECK_BOX</code>.
	 *
	 * @param action the enabled, toggle action whose checked state will be
	 * saved in preferences
	 * @since 2.1
	 */
	protected void saveCheckedActionState(IAction action) {
		String prefKey = generatePreferenceKey(action);
		IPreferenceStore prefStore = getPreferenceStore();
		prefStore.setValue(prefKey, action.isChecked());
	}

	/**
	 * Generate a String that can be used as a key into a preference store based
	 * on the specified action.  The resulting String will be unique across
	 * views.
	 * @param action the action to generate a key for
	 * @return a String suitable for use as a preference store key for the given
	 * action
	 * @since 2.1
	 */
	protected String generatePreferenceKey(IAction action) {
		return getViewSite().getId() + '+' + action.getId();
	}

	/**
	 * Convenience method to return the preference store for the Debug UI
	 * plug-in.
	 *
	 * @return the preference store for the Debug UI plug-in
	 * @since 2.1
	 */
	protected IPreferenceStore getPreferenceStore() {
		return DebugUIPlugin.getDefault().getPreferenceStore();
	}

	/**
	 * @see IDebugView#getViewer()
	 */
	@Override
	public Viewer getViewer() {
		return fViewer;
	}

	/**
	 * Returns this view's viewer as a structured viewer,
	 * or <code>null</code> if none.
	 *
	 * @return this view's viewer as a structured viewer
	 * 	or <code>null</code>
	 */
	protected StructuredViewer getStructuredViewer() {
		if (getViewer() instanceof StructuredViewer) {
			return (StructuredViewer)getViewer();
		}
		return null;
	}

	/**
	 * Returns this view's viewer as a text viewer,
	 * or <code>null</code> if none.
	 *
	 * @return this view's viewer as a text viewer
	 * 	or <code>null</code>
	 */
	protected TextViewer getTextViewer() {
		if (getViewer() instanceof TextViewer) {
			return (TextViewer)getViewer();
		}
		return null;
	}

	/**
	 * @see IDebugView#getPresentation(String)
	 */
	@Override
	public IDebugModelPresentation getPresentation(String id) {
		if (getViewer() instanceof StructuredViewer) {
			IBaseLabelProvider lp = ((StructuredViewer)getViewer()).getLabelProvider();
			if (lp instanceof DelegatingModelPresentation) {
				return ((DelegatingModelPresentation)lp).getPresentation(id);
			}
			if (lp instanceof LazyModelPresentation) {
				if (((LazyModelPresentation)lp).getDebugModelIdentifier().equals(id)) {
					return (IDebugModelPresentation)lp;
				}
			}
		}
		return null;
	}

	/**
	 * Creates a pop-up menu on the given control. The menu
	 * is registered with this view's site, such that other
	 * plug-ins may contribute to the menu. Subclasses should
	 * call this method, specifying the menu control as the
	 * control used in their viewer (for example, tree viewer).
	 * Subclasses must implement the method
	 * <code>#fillContextMenu(IMenuManager)</code> which will
	 * be called each time the context menu is realized.
	 *
	 * @param menuControl the control with which the pop-up
	 *  menu will be associated with.
	 */
	protected void createContextMenu(Control menuControl) {
		MenuManager menuMgr= new MenuManager("#PopUp"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(this::fillContextMenu);
		Menu menu= menuMgr.createContextMenu(menuControl);
		menuControl.setMenu(menu);

		// register the context menu such that other plug-ins may contribute to it
		if (getSite() != null) {
			getSite().registerContextMenu(menuMgr, getViewer());
		}
		addContextMenuManager(menuMgr);
	}

	/**
	 * @see IDebugView#getContextMenuManager()
	 * @see AbstractDebugView#getContextMenuManagers()
	 *
	 * @deprecated
	 */
	@Deprecated
	@Override
	public IMenuManager getContextMenuManager() {
		if (fContextMenuManagers != null) {
			fContextMenuManagers.get(fContextMenuManagers.size() - 1);
		}
		return null;
	}

	/**
	 * Returns the context menu managers relevant to this view.
	 *
	 * @return the context menu managers relevant to this view
	 * @since 2.1
	 */
	public List<IMenuManager> getContextMenuManagers() {
		return fContextMenuManagers;
	}

	/**
	 * Subclasses must override this method to fill the context
	 * menu each time it is realized.
	 *
	 * @param menu the context menu
	 */
	protected abstract void fillContextMenu(IMenuManager menu);

	/**
	 * Configures this view's toolbar. Subclasses implement
	 * <code>#configureToolBar(IToolBarManager)</code> to
	 * contribute actions to the toolbar.
	 * <p>
	 * To properly initialize toggle actions that are contributed
	 * to this view, state is restored for toggle actions that have
	 * a persisted state in the Debug UI plugin's preferences.  As well, any
	 * toggle actions that have an initial state of 'checked' are invoked. The
	 * actions' states are restored and the actions are invoked in a runnable,
	 * after the view is created.
	 * </p>
	 */
	protected void initializeToolBar() {
		final IToolBarManager tbm= getViewSite().getActionBars().getToolBarManager();
		configureToolBar(tbm);
		getViewSite().getActionBars().updateActionBars();

		// This is done in a runnable to be run after this view's pane
		// is created
		Runnable r = () -> {
			if (!isAvailable()) {
				return;
			}
			IContributionItem[] items = tbm.getItems();
			if (items != null) {
				for (IContributionItem item : items) {
					if (item instanceof ActionContributionItem) {
						IAction action = ((ActionContributionItem) item).getAction();
						if (!SkipAllBreakpointsAction.ACTION_ID.equals(action.getId())) {
							if (action.getStyle() == IAction.AS_CHECK_BOX) {
								initActionState(action);
								if (action.isChecked()) {
									action.run();
								}
							}
						}
					}
				}
				setMemento(null);
			}
			updateObjects();
		};
		asyncExec(r);
	}

	/**
	 * Restores the persisted checked state of the specified action that was
	 * stored in preferences. If the action is disabled, its persisted state
	 * is not restored (because a disabled action cannot be run).
	 *
	 * @param action the action whose checked state will be restored
	 * @since 2.1
	 */
	protected void initActionState(IAction action) {
		String id = action.getId();
		if (id != null && action.isEnabled()) {
			String prefKey = generatePreferenceKey(action);
			boolean checked = getPreferenceStore().getBoolean(prefKey);
			action.setChecked(checked);
		}
	}

	/**
	 * @see IViewPart#init(IViewSite, IMemento)
	 */
	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		//store the memento to be used when this view is created.
		setMemento(memento);
	}

	/**
	 * Sets the viewer for this view.
	 *
	 * @param viewer viewer
	 * @since 3.1
	 */
	protected void setViewer(Viewer viewer) {
		fViewer = viewer;
	}

	/**
	 * Subclasses implement this menu to contribute actions
	 * to the toolbar. This method is called after
	 * <code>createActions()</code>.
	 *
	 * @param tbm the tool bar manager for this view's site
	 * @see #createViewer(Composite)
	 */
	protected abstract void configureToolBar(IToolBarManager tbm);

	/**
	 * @see IDebugView#setAction(String, IAction)
	 */
	@Override
	public void setAction(String actionID, IAction action) {
		if (action == null) {
			Object removedAction= fActionMap.remove(actionID);
			fUpdateables.remove(removedAction);
		} else {
			fActionMap.put(actionID, action);
			if (action instanceof IUpdate) {
				add((IUpdate) action);
			}
		}
		if (fgGlobalActionIds.contains(actionID)) {
			IActionBars actionBars = getViewSite().getActionBars();
			actionBars.setGlobalActionHandler(actionID, action);
		}
	}

	/**
	 * @see IDebugView#getAction(String)
	 */
	@Override
	public IAction getAction(String actionID) {
		return fActionMap.get(actionID);
	}

	/**
	 * Updates all the registered updatables.
	 */
	public void updateObjects() {
		for (IUpdate update : fUpdateables) {
			update.update();
		}
	}

	/**
	 * Handles key events in viewer. Invokes
	 * <ol>
	 * <li><code>REMOVE_ACTION</code> when the delete key is pressed</li>
	 * </ol>
	 *
	 * @param event the {@link KeyEvent}
	 */
	protected void handleKeyPressed(KeyEvent event) {
		if (event.character == SWT.DEL && event.stateMask == 0) {
			IAction action = getAction(REMOVE_ACTION);
			if (action != null && action.isEnabled()) {
				action.run();
			}
		}
	}

	/**
	 * Delegate to the <code>DOUBLE_CLICK_ACTION</code>,
	 * if any.
	 *
	 * @see IDoubleClickListener#doubleClick(DoubleClickEvent)
	 */
	@Override
	public void doubleClick(DoubleClickEvent event) {
		IAction action = getAction(DOUBLE_CLICK_ACTION);
		if (action != null && !event.getSelection().isEmpty() && action.isEnabled()) {
			action.run();
		}
	}

	/**
	 * Registers the given runnable with the display
	 * associated with this view's control, if any.
	 * @param r the {@link Runnable} to run
	 *
	 * @see org.eclipse.swt.widgets.Display#asyncExec(java.lang.Runnable)
	 */
	public void asyncExec(Runnable r) {
		if (isAvailable()) {
			getControl().getDisplay().asyncExec(r);
		}
	}

	/**
	 * Returns the control for this view, or <code>null</code> if none.
	 *
	 * @return the control for this view, or <code>null</code> if none
	 * @since 3.0
	 */
	protected Control getControl() {
		return getViewer().getControl();
	}

	/**
	 * Registers the given runnable with the display
	 * associated with this view's control, if any.
	 * @param r the {@link Runnable} to run
	 *
	 * @see org.eclipse.swt.widgets.Display#syncExec(java.lang.Runnable)
	 */
	public void syncExec(Runnable r) {
		if (isAvailable()) {
			getControl().getDisplay().syncExec(r);
		}
	}

	/**
	 * Returns the memento that contains the persisted state of
	 * the view.  May be <code>null</code>.
	 * @return the current {@link IMemento}
	 */
	protected IMemento getMemento() {
		return fMemento;
	}

	/**
	 * Sets the memento that contains the persisted state of the
	 * view.
	 * @param memento the new {@link IMemento}
	 */
	protected void setMemento(IMemento memento) {
		fMemento = memento;
	}

	/**
	 * Returns the specified view in this view's page
	 * or <code>null</code> if none.
	 *
	 * @param id view identifier
	 * @return view part
	 */
	protected IViewPart findView(String id) {
		IWorkbenchPage page = getSite().getPage();
		IViewPart view = null;
		if (page != null) {
			view = page.findView(id);
		}
		return view;
	}

	/**
	 * @see PageBookView#isImportant(IWorkbenchPart)
	 */
	@Override
	protected boolean isImportant(IWorkbenchPart part) {
		return false;
	}

	/**
	 * @see PageBookView#doCreatePage(IWorkbenchPart)
	 */
	@Override
	protected PageRec doCreatePage(IWorkbenchPart part) {
		return null;
	}

	/**
	 * @see PageBookView#doDestroyPage(org.eclipse.ui.IWorkbenchPart, org.eclipse.ui.part.PageBookView.PageRec)
	 */
	@Override
	protected void doDestroyPage(IWorkbenchPart part, PageRec pageRecord) {
	}

	/**
	 * @see PageBookView#getBootstrapPart()
	 */
	@Override
	protected IWorkbenchPart getBootstrapPart() {
		return null;
	}

	/**
	 * Returns the default control for this view. By default,
	 * this view's viewer's control is returned. Subclasses
	 * should override if required - for example, if this
	 * view has its viewer nested inside other controls.
	 *
	 * @return this view's default control.
	 */
	protected Control getDefaultControl() {
		Viewer viewer = getViewer();
		if (viewer != null) {
			return viewer.getControl();
		}
		return null;
	}

	/**
	 * Sets this view's message page
	 *
	 * @param page message page
	 */
	private void setMessagePage(MessagePage page) {
		fMessagePage = page;
	}

	/**
	 * Returns this view's message page
	 *
	 * @return message page
	 */
	protected MessagePage getMessagePage() {
		return fMessagePage;
	}

	/**
	 * Shows the given message in this view's message'
	 * page. Makes the message page the visible page.
	 *
	 * @param message the message to display
	 */
	public void showMessage(String message) {
		if (getPageBook().isDisposed()) {
			return;
		}
		if (getMessagePage() == null) {
			//not fully created yet
			fEarlyMessage= message;
			return;
		}
		getMessagePage().setMessage(message);
		getPageBook().showPage(getMessagePage().getControl());
	}

	/**
	 * Shows this view's viewer page.
	 */
	public void showViewer() {
		if (getPageBook().isDisposed()) {
			return;
		}
		getPageBook().showPage(getDefaultPage().getControl());
	}

	/**
	 * Returns whether this view's viewer is
	 * currently available.
	 *
	 * @return whether this view's viewer is
	 * currently available
	 */
	public boolean isAvailable() {
		return !(getViewer() == null || getViewer().getControl() == null || getViewer().getControl().isDisposed());
	}
	/**
	 * @see IDebugView#add(IUpdate)
	 */
	@Override
	public void add(IUpdate updatable) {
		if (!fUpdateables.contains(updatable)) {
			fUpdateables.add(updatable);
		}
	}

	/**
	 * @see IDebugView#remove(IUpdate)
	 */
	@Override
	public void remove(IUpdate updatable) {
		fUpdateables.remove(updatable);
	}

	/**
	 * Adds a context menu manager that is relevant to this view.
	 * @param contextMenuManager The contextMenuManager to add
	 *
	 * @since 2.1
	 */
	public void addContextMenuManager(IMenuManager contextMenuManager) {
		if (fContextMenuManagers == null) {
			fContextMenuManagers = new ArrayList<>();
		}
		fContextMenuManagers.add(contextMenuManager);
	}

	/**
	 * Notification this view is now visible.
	 *
	 * @since 2.1
	 */
	protected void becomesVisible() {
	}

	/**
	 * Notification this view is now hidden.
	 *
	 * @since 2.1
	 */
	protected void becomesHidden() {
	}

	/**
	 * Returns whether this view is currently visible.
	 *
	 * @return whether this view is currently visible
	 * @since 2.1
	 */
	public boolean isVisible() {
		return fIsVisible;
	}

	/**
	 * Creates and registers a part listener with this event handler's page,
	 * if one does not already exist.
	 *
	 * @since 2.1
	 */
	protected void registerPartListener() {
		if (fPartListener == null) {
			fPartListener= new DebugViewPartListener();
			getSite().getPage().addPartListener(fPartListener);
		}
	}

	/**
	 * Unregisters and disposes this event handler's part listener.
	 *
	 * @since 2.1
	 */
	protected void deregisterPartListener() {
		if (fPartListener != null) {
			getSite().getPage().removePartListener(fPartListener);
			fPartListener = null;
		}
	}

	/**
	 * Returns a map of the current attribute settings in the model
	 * presentation in this view associated with the given debug model.
	 * @param modelId the debug model identifier
	 * @return a map of the current attribute settings in the model
	 * presentation in this view associated with the given debug model
	 * @since 3.0
	 */
	public Map<String, Object> getPresentationAttributes(String modelId) {
		IDebugModelPresentation presentation = getPresentation(modelId);
		if (presentation instanceof DelegatingModelPresentation) {
			return ((DelegatingModelPresentation)presentation).getAttributeMap();
		} else if (presentation instanceof LazyModelPresentation) {
			return ((LazyModelPresentation)presentation).getAttributeMap();
		}
		return Collections.EMPTY_MAP;
	}
}


