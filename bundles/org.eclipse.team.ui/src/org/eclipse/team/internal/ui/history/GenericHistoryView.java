/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
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
 *     Sergey Prigogin (Google) - [490835] Local history page is sticky
 *******************************************************************************/
package org.eclipse.team.internal.ui.history;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.history.IFileHistoryProvider;
import org.eclipse.team.internal.ui.ITeamUIImages;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.SyncInfoModelElement;
import org.eclipse.team.ui.history.ElementLocalHistoryPageSource;
import org.eclipse.team.ui.history.HistoryPage;
import org.eclipse.team.ui.history.IHistoryPage;
import org.eclipse.team.ui.history.IHistoryPageSource;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.part.IPage;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.part.IShowInTarget;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.PageBookView;
import org.eclipse.ui.part.PluginTransfer;
import org.eclipse.ui.part.ResourceTransfer;
import org.eclipse.ui.part.ShowInContext;

public class GenericHistoryView extends PageBookView implements IHistoryView, IPropertyChangeListener, IShowInTarget {
	private static final String HISTORY_VIEW_GROUP = "org.eclipse.team.ui.historyView"; //$NON-NLS-1$
	private static final String NAVIGATION_GROUP = "org.eclipse.team.ui.navigation"; //$NON-NLS-1$
	private static final int MAX_NAVIGATION_HISTORY_ENTRIES = 15;

	static boolean sameSource(IHistoryPageSource source1, IHistoryPageSource source2) {
		return Objects.equals(source1, source2);
	}

	private boolean matches(IPage page, Object object, IHistoryPageSource pageSource) {
		if (page instanceof IHistoryPage) {
			Object input = ((IHistoryPage)page).getInput();
			if (input != null)
				return input.equals(object) && sameSource(getPageSourceFor(object, pageSource), getCurrentPageSource());
		}
		return false;
	}

	private IHistoryPageSource getCurrentPageSource() {
		IWorkbenchPart part = getCurrentContributingPart();
		if (part instanceof HistoryPageSourceWorkbenchPart) {
			return ((HistoryPageSourceWorkbenchPart) part).getSource();
		}
		return null;
	}

	/*
	 * The navigation history for this view.
	 * The history adds the MRU to the end so basic navigation goes backwards.
	 */
	class NavigationHistory {
		List<NavigationHistoryEntry> history = new ArrayList<>();
		int position;
		private boolean navigating;

		public int size() {
			return history.size();
		}

		public void gotoPreviousEntry() {
			if (position > 0) {
				position--;
				gotoEntry();
			} else {
				position = history.size() - 1;
				gotoEntry();
			}
		}

		private void gotoEntry() {
			try {
				navigating = true;
				NavigationHistoryEntry currentEntry = getCurrentEntry();
				showHistoryPageFor(currentEntry.object, true, true, currentEntry.source);
			} finally {
				navigating = false;
			}
		}

		private NavigationHistoryEntry getCurrentEntry() {
			return history.get(position);
		}

		public void addEntry(Object object, String name, IHistoryPageSource source) {
			if (!navigating) {
				NavigationHistoryEntry navigationHistoryEntry = new NavigationHistoryEntry(object, name, source);
				if (history.contains(navigationHistoryEntry)) {
					history.remove(navigationHistoryEntry);
				}
				history.add(navigationHistoryEntry);
				if (history.size() > MAX_NAVIGATION_HISTORY_ENTRIES) {
					history.remove(0);
				}
				position = history.size() - 1;
			}
			navigateAction.update();
		}

		public NavigationHistoryEntry[] getEntries() {
			return history.toArray(new NavigationHistoryEntry[history.size()]);
		}

		private NavigationHistoryEntry getEntry(int i) {
			return history.get(i);
		}

		public void gotoEntry(NavigationHistoryEntry navigationHistoryEntry) {
			position = history.indexOf(navigationHistoryEntry);
			gotoEntry();
		}

		public NavigationHistoryEntry getPreviousEntry() {
			int next = position - 1;
			if (next < 0)
				next = size() - 1;
			return getEntry(next);
		}

		public void updateName(IHistoryPage historyPage,
				IHistoryPageSource pageSource) {
			NavigationHistoryEntry[] historyEntries = getEntries();
			for (NavigationHistoryEntry historyEntry : historyEntries) {
				if (historyEntry.matches(historyPage, pageSource))
					historyEntry.name = historyPage.getName();
			}
			navigateAction.update();
		}
	}

	static class NavigationHistoryEntry {
		Object object;
		String name;
		IHistoryPageSource source;

		public NavigationHistoryEntry(Object object, String name, IHistoryPageSource source) {
			this.object = object;
			this.name = name;
			this.source = source;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof NavigationHistoryEntry) {
				NavigationHistoryEntry other = (NavigationHistoryEntry) obj;
				return other.object.equals(this.object) && sameSource(source, other.source);
			}
			return false;
		}

		public boolean matches(IHistoryPage historyPage,
				IHistoryPageSource pageSource) {
			return object.equals(historyPage.getInput())
					&& sameSource(source, pageSource);
		}

		@Override
		public int hashCode() {
			return object.hashCode();
		}
	}

	abstract static class MenuCreator implements IMenuCreator {
		private MenuManager menuManager;

		@Override
		public void dispose() {
			if(menuManager != null) {
				menuManager.dispose();
				menuManager = null;
			}
		}

		@Override
		public Menu getMenu(Control parent) {
			Menu fMenu = null;
			if (menuManager == null) {
				menuManager = new MenuManager();
				fMenu = menuManager.createContextMenu(parent);
				IAction[] actions = getDropDownActions();
				for (int i = actions.length - 1; i >= 0 ; i--) {
					IAction action = actions[i];
					menuManager.add(action);
				}
				updateMenuState();
			} else {
				fMenu = menuManager.getMenu();
			}
			return fMenu;
		}

		protected void updateMenuState() {
			if (menuManager != null)
				menuManager.update(true);
		}

		protected abstract IAction[] getDropDownActions();

		@Override
		public Menu getMenu(Menu parent) {
			return null;
		}

		public void rebuildMenu() {
			if(menuManager != null) {
				menuManager.dispose();
				menuManager = null;
			}
		}
	}

	class NavigationHistoryAction extends Action {
		private MenuCreator menuCreator;
		private IAction[] actions;

		public NavigationHistoryAction() {
			menuCreator = new MenuCreator() {
				@Override
				protected IAction[] getDropDownActions() {
					return getActions();
				}
			};
			setMenuCreator(menuCreator);
			update();
		}

		private IAction[] createActions() {
			NavigationHistoryEntry[] entries = getDropDownEntries();
			List<NavigationHistoryEntryAction> actions = new ArrayList<>();
			for (NavigationHistoryEntry navigationHistoryEntry : entries) {
				actions.add(new NavigationHistoryEntryAction(navigationHistoryEntry));
			}
			return actions.toArray(new IAction[actions.size()]);
		}

		protected NavigationHistoryEntry[] getDropDownEntries() {
			return navigationHistory.getEntries();
		}

		@Override
		public void run() {
			navigationHistory.gotoPreviousEntry();
			updateCheckState();
		}

		public void update() {
			setEnabled(navigationHistory.size() > 1);
			if (isEnabled()) {
				setToolTipText(NLS.bind(TeamUIMessages.GenericHistoryView_1, navigationHistory.getPreviousEntry().name));
			} else {
				setToolTipText(TeamUIMessages.GenericHistoryView_2);
			}
			actions = null;
			menuCreator.rebuildMenu();
			updateCheckState();
		}

		private void updateCheckState() {
			IAction[] actions = getActions();
			for (IAction action : actions) {
				if (action instanceof NavigationHistoryEntryAction) {
					NavigationHistoryEntryAction a = (NavigationHistoryEntryAction) action;
					a.update();
				}
			}
			menuCreator.updateMenuState();
		}

		public void dispose() {
			menuCreator.dispose();
		}

		private IAction[] getActions() {
			if (actions == null)
				actions = createActions();
			return actions;
		}
	}

	class NavigationHistoryEntryAction extends Action {
		private final NavigationHistoryEntry navigationHistoryEntry;

		public NavigationHistoryEntryAction(NavigationHistoryEntry navigationHistoryEntry) {
			super(navigationHistoryEntry.name);
			this.navigationHistoryEntry = navigationHistoryEntry;
		}

		@Override
		public void run() {
			navigationHistory.gotoEntry(navigationHistoryEntry);
			navigateAction.updateCheckState();
		}

		public void update() {
			setChecked(navigationHistory.getCurrentEntry() == navigationHistoryEntry);
		}
	}

	/**
	 * View actions
	 */
	private Action refreshAction;
	private Action linkWithEditorAction;
	private Action pinAction;
	private NavigationHistoryAction navigateAction;

	/**
	 * The drop target + drop target listener
	 */
	DropTarget dropTarget;
	GenericHistoryDropAdapter dropAdapter;

	NavigationHistory navigationHistory = new NavigationHistory();

	/**
	 * Keeps track of the last selected element (either by selecting or opening an editor)
	 */
	private Object lastSelectedElement;

	@Override
	public void partActivated(IWorkbenchPart part) {
		// don't call super.partActivated(IWorkbenchPart), it will be done in #showHistoryPageFor(...)
		if (part instanceof IEditorPart)
			editorActivated((IEditorPart) part);
	}

	private ISelectionListener selectionListener = new ISelectionListener() {
		private boolean isUpdatingSelection = false;

		@Override
		public void selectionChanged(IWorkbenchPart part, ISelection selection) {
			if (isUpdatingSelection)
				return;

			try {
				isUpdatingSelection = true;
				if (GenericHistoryView.this == part)
					return;

				if (selection instanceof IStructuredSelection) {
					IStructuredSelection structSelection = (IStructuredSelection) selection;
					// Always take the first element - this is not intended to work with multiple selection
					// Also, hang on to this selection for future use in case the history view is not visible
					lastSelectedElement = structSelection.getFirstElement();

					if (!isLinkingEnabled() || !checkIfPageIsVisible()) {
						return;
					}

					showLastSelectedElement();
				}
			} finally {
				isUpdatingSelection = false;
			}
		}
	};

	private boolean linkingEnabled;
	private boolean viewPinned;

	@Override
	public void init(IViewSite site) throws PartInitException {
		super.init(site);

		ISelection selection= getSite().getPage().getSelection();
		if (selection instanceof IStructuredSelection) {
			//Always take the first element - this is not intended to work with multiple selection
			lastSelectedElement= ((IStructuredSelection)selection).getFirstElement();
		}

		// Use active editor as fallback
		if (lastSelectedElement == null)
			lastSelectedElement= getSite().getPage().getActiveEditor();
	}

	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);

		this.linkingEnabled = TeamUIPlugin.getPlugin().getPreferenceStore().getBoolean(IFileHistoryConstants.PREF_GENERIC_HISTORYVIEW_EDITOR_LINKING);

		// Contribute toolbars
		configureToolbars(getViewSite().getActionBars());

		// add global action handler
		getViewSite().getActionBars().setGlobalActionHandler(ActionFactory.REFRESH.getId(), refreshAction);

		// initialize the drag and drop
		initDragAndDrop();

		// add listener for selections
		getSite().getPage().addPostSelectionListener(selectionListener);
	}

	private void configureToolbars(IActionBars actionBars) {
		pinAction = new Action(TeamUIMessages.GenericHistoryView_PinCurrentHistory, TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_PINNED)) {
			@Override
			public void run() {
				if (isChecked()) {
					// uncheck editor linking
					linkWithEditorAction.setChecked(false);
					setLinkingEnabled(false);
				}
				setViewPinned(isChecked());
			}
		};
		pinAction.setChecked(isViewPinned());
		pinAction.setToolTipText(TeamUIMessages.GenericHistoryView_0);

		refreshAction = new Action(TeamUIMessages.GenericHistoryView_Refresh, TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_REFRESH)) {
			@Override
			public void run() {
				getHistoryPage().refresh();
			}
		};
		refreshAction.setToolTipText(TeamUIMessages.GenericHistoryView_RefreshTooltip);
		refreshAction.setEnabled(true);

		linkWithEditorAction = new Action(TeamUIMessages.GenericHistoryView_LinkWithEditor, TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_LINK_WITH)) {
			@Override
			public void run() {
				if (isChecked()) {
					// uncheck pinned
					pinAction.setChecked(false);
					setViewPinned(false);
				}
				setLinkingEnabled(isViewPinned() ? false : isChecked());
			}
		};
		linkWithEditorAction.setChecked(isLinkingEnabled());
		linkWithEditorAction.setToolTipText(TeamUIMessages.GenericHistoryView_LinkWithTooltip);

		navigateAction = new NavigationHistoryAction();
		Utils.initAction(navigateAction, "action.previousHistory."); //$NON-NLS-1$

		// Create the local tool bar
		IToolBarManager tbm = actionBars.getToolBarManager();
		tbm.add(new Separator(HISTORY_VIEW_GROUP));
		tbm.appendToGroup(HISTORY_VIEW_GROUP, refreshAction);
		tbm.appendToGroup(HISTORY_VIEW_GROUP, linkWithEditorAction);
		tbm.appendToGroup(HISTORY_VIEW_GROUP, pinAction);
		tbm.add(new Separator(NAVIGATION_GROUP));
		tbm.appendToGroup(NAVIGATION_GROUP, navigateAction);
		tbm.update(false);
	}

	boolean isLinkingEnabled() {
		return linkingEnabled;
	}

	/**
	 * Enabled linking to the active editor
	 * @param enabled flag indicating whether linking is enabled
	 */
	public void setLinkingEnabled(boolean enabled) {
		this.linkingEnabled = enabled;

		// remember the last setting in the dialog settings
		TeamUIPlugin.getPlugin().getPreferenceStore().setValue(IFileHistoryConstants.PREF_GENERIC_HISTORYVIEW_EDITOR_LINKING, enabled);

		// if turning linking on, update the selection to correspond to the active editor
		if (enabled) {
			showLastSelectedElement();
		}
	}

	/**
	 * Sets the current view pinned
	 * @param b
	 */
	void setViewPinned(boolean pinned) {
		this.viewPinned = pinned;
	}

	/**
	 * Adds drag and drop support to the history view.
	 */
	void initDragAndDrop() {
		int ops = DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK;
		Transfer[] transfers = new Transfer[] {ResourceTransfer.getInstance(), PluginTransfer.getInstance()};

		dropTarget = new DropTarget(getPageBook(), ops);
		dropTarget.setTransfer(transfers);
		dropAdapter = new GenericHistoryDropAdapter(this);
		dropTarget.addDropListener(dropAdapter);
	}

	@Override
	public void setFocus() {
		if (isLinkingEnabled() && lastSelectedElement != null) {
			showLastSelectedElement();
		}
		getCurrentPage().setFocus();
	}

	private void showLastSelectedElement() {
		if (lastSelectedElement != null) {
			if (lastSelectedElement instanceof IEditorPart)
				editorActivated((IEditorPart)lastSelectedElement);
			else {
				Object resource;
				if (lastSelectedElement instanceof SyncInfoModelElement) {
					SyncInfoModelElement syncInfoModelElement = (SyncInfoModelElement) lastSelectedElement;
					resource = syncInfoModelElement.getSyncInfo().getLocal();
				} else {
					resource= Adapters.adapt(lastSelectedElement, IResource.class);
				}
				if (resource != null)
					showHistoryPageFor(resource, false, false, null);
				else
					showHistoryPageFor(lastSelectedElement, false, false, null);
			}

			// reset lastSelectedElement to null to prevent updating history view if it just gets focus
			lastSelectedElement= null;
		}
	}

	@Override
	protected void showPageRec(PageRec pageRec) {
		super.showPageRec(pageRec);
		addNavigationHistoryEntry();
	}

	private void addNavigationHistoryEntry() {
		if (getCurrentPage() != null) {
			Object input = getHistoryPage().getInput();
			if (input != null)
				navigationHistory.addEntry(input, getHistoryPage().getName(), getPageSourceFor(input, null));
		}
	}

	@Override
	public IHistoryPage showHistoryFor(Object object, boolean force) {
		return showHistoryPageFor(object, true, force, null);
	}

	public IHistoryPage showHistoryPageFor(Object object, boolean refresh, boolean force, IHistoryPageSource pageSource) {
		lastSelectedElement= null;
		if (Policy.DEBUG_HISTORY) {
			String time = new SimpleDateFormat("m:ss.SSS").format(new Date(System.currentTimeMillis())); //$NON-NLS-1$
			System.out.println(time + ": GenericHistoryView#showHistoryPageFor, the object to show is: " + object); //$NON-NLS-1$
		}

		// Check to see if history view is visible - if it's not, don't bother
		// going to the trouble of fetching the history
		if (!checkIfPageIsVisible())
			return null;

		// Ensure that there is a page source available
		pageSource = getPageSourceFor(object, pageSource);
		if (pageSource == null || !pageSource.canShowHistoryFor(object))
			return null;

		// Check to see if the object is already being displayed in another page
		IHistoryPage existingPage = checkForExistingPage(object, refresh, force, pageSource);
		if (existingPage != null){
			return existingPage;
		}

		// Now check to see if this view is pinned
		if (isViewPinned() && !force) {
			return handlePinnedView(object, refresh, pageSource);
		}

		HistoryPageSourceWorkbenchPart part = new HistoryPageSourceWorkbenchPart(
				object, pageSource, getViewSite());

		// If a page for the part exists, open it; otherwise, create a new page
		super.partActivated(part);

		if (Policy.DEBUG_HISTORY) {
			String time = new SimpleDateFormat("m:ss.SSS").format(new Date(System.currentTimeMillis())); //$NON-NLS-1$
			System.out.println(time + ": GenericHistoryView#showHistoryPageFor, the page showing the history is: " + getHistoryPage()); //$NON-NLS-1$
		}

		return getHistoryPage();
	}

	@Override
	protected PageRec getPageRec(IWorkbenchPart part) {
		PageRec rec = super.getPageRec(part);
		if (rec != null) {
			if (part instanceof HistoryPageSourceWorkbenchPart) {
				HistoryPageSourceWorkbenchPart p = (HistoryPageSourceWorkbenchPart)part;
				IHistoryPage historyPage = (IHistoryPage) rec.page;
				Object newInput= p.getObject();
				if (!historyPage.isValidInput(newInput)) {
					if (historyPage instanceof EditionHistoryPage)
						((EditionHistoryPage)historyPage).setInput(((ElementLocalHistoryPageSource)p.getSource()).internalGetFile(newInput), newInput);
					else
						return null; // Create a new page
				} else
					historyPage.setInput(newInput);

				((HistoryPage)historyPage).setHistoryView(this);
				setContentDescription(historyPage.getName());
			}
		}
		return rec;
	}

	private IHistoryPageSource getPageSourceFor(Object object, IHistoryPageSource pageSource) {
		if (object == null || pageSource != null)
			return pageSource;
		IResource resource = Utils.getResource(object);
		if (resource == null) {
			return Adapters.adapt(object, IHistoryPageSource.class);
		} else {
			if (resource.getProject() == null)
				return null;
			// check to see if resource is managed
			RepositoryProvider teamProvider = RepositoryProvider.getProvider(resource.getProject());
			if (teamProvider == null){
				// couldn't find a repository provider; try showing it in a local page
				return LocalHistoryPageSource.getInstance();
			} else {
				IFileHistoryProvider fileHistory = teamProvider.getFileHistoryProvider();
				if (fileHistory != null) {
					IHistoryPageSource source = Adapters.adapt(fileHistory, IHistoryPageSource.class);
					if (source != null)
						return source;
				}
				return Adapters.adapt(teamProvider, IHistoryPageSource.class);
			}
		}
	}

	private IHistoryPage handlePinnedView(Object object, boolean refresh, IHistoryPageSource source) {
		try {
			// Check to see if an unpinned version of the history view exists
			GenericHistoryView historyView = findUnpinnedHistoryView();
			if (historyView != null){
				getSite().getPage().activate(historyView);
				return historyView.showHistoryPageFor(object, refresh, true, source);
			}
			// Otherwise, open another instance of the view
			String id = VIEW_ID + System.currentTimeMillis();
			IViewPart view = getSite().getPage().showView(VIEW_ID, id, IWorkbenchPage.VIEW_CREATE);
			getSite().getPage().activate(view);
			if (view instanceof GenericHistoryView)
				return ((GenericHistoryView) view).showHistoryPageFor(object, refresh, true, source);

		} catch (PartInitException e) {
		}
		return null;
	}

	private IHistoryPage checkForExistingPage(Object object, boolean refresh, boolean thisViewOnly, IHistoryPageSource pageSource) {
		// first check to see if the main history view contains the current resource
		IHistoryPage tempPage = checkForExistingPage(object, refresh, pageSource);
		if (tempPage != null || thisViewOnly)
			return tempPage;
		tempPage = searchHistoryViewsForObject(object, refresh, pageSource);
		if (tempPage != null)
			getSite().getPage().bringToTop((IWorkbenchPart)tempPage.getHistoryView());
		return tempPage;
	}

	private IHistoryPage checkForExistingPage(Object object, boolean refresh, IHistoryPageSource pageSource) {
		// first check to see if the main history view contains the current resource
		if (getCurrentPage() != null) {
			if (matches(getCurrentPage(), object, pageSource)) {
				//current page contains object, so just refresh it
				IHistoryPage tempPage = (IHistoryPage) getCurrentPage();
				if (refresh)
					tempPage.refresh();
				return tempPage;
			}
		}
		return null;
	}

	private IHistoryPage searchHistoryViewsForObject(Object object, boolean  refresh, IHistoryPageSource pageSource) {
		IWorkbenchPage page = getSite().getPage();
		IViewReference[] historyViews = page.getViewReferences();
		for (IViewReference h : historyViews) {
			if (h.getId().equals(VIEW_ID)) {
				IViewPart historyView = h.getView(true);
				if (historyView instanceof GenericHistoryView) {
					GenericHistoryView ghv = (GenericHistoryView)historyView;
					IHistoryPage historyPage = ghv.checkForExistingPage(object, refresh, pageSource);
					if (historyPage != null) {
						return historyPage;
					}
				}
			}
		}
		return null;
	}

	public GenericHistoryView findUnpinnedHistoryView(){
		IWorkbenchPage page = getSite().getPage();
		IViewReference[] historyViews = page.getViewReferences();
		for (IViewReference h : historyViews) {
			if (h.getId().equals(VIEW_ID)) {
				IViewPart historyView = h.getView(false);
				if (!((GenericHistoryView)historyView).isViewPinned())
					return (GenericHistoryView) historyView;
			}
		}
		return null;
	}

	boolean isViewPinned() {
		return viewPinned;
	}

	@Override
	protected PageRec doCreatePage(IWorkbenchPart part) {
		HistoryPageSourceWorkbenchPart p = (HistoryPageSourceWorkbenchPart) part;
		IHistoryPageSource source = p.getSource();
		IPageBookViewPage page = source.createPage(p.getObject());
		if (page != null) {
			initPage(page);
			IHistoryPage historyPage = (IHistoryPage) page;
			historyPage.addPropertyChangeListener(this);
			historyPage.setSite(new WorkbenchHistoryPageSite(this, page.getSite()));
			page.createControl(getPageBook());
			historyPage.setInput(p.getObject());
			((HistoryPage)page).setHistoryView(this);
			setContentDescription(historyPage.getName());
			return new PageRec(part, page);
		}
		return null;
	}

	@Override
	protected void doDestroyPage(IWorkbenchPart part, PageRec pageRecord) {
		IPage page = pageRecord.page;
		if (page instanceof IHistoryPage)
			((IHistoryPage)page).removePropertyChangeListener(this);
		page.dispose();
		pageRecord.dispose();
	}

	@Override
	protected IWorkbenchPart getBootstrapPart() {
		return null;
	}

	@Override
	protected boolean isImportant(IWorkbenchPart part) {
		if (part instanceof HistoryPageSourceWorkbenchPart) {
			HistoryPageSourceWorkbenchPart p = (HistoryPageSourceWorkbenchPart)part;
			Object object = p.getObject();
			return p.getSource().canShowHistoryFor(object);
		}
		return false;
	}

	@Override
	protected IPage createDefaultPage(PageBook book) {
		GenericHistoryViewDefaultPage page = new GenericHistoryViewDefaultPage();
		initPage(page);
		page.createControl(book);
		return page;
	}

	/**
	 * An editor has been activated.  Fetch the history if the file is shared and the history view
	 * is visible in the current page.
	 *
	 * @param editor the active editor
	 */
	protected void editorActivated(IEditorPart editor) {
		lastSelectedElement= editor;

		// Only fetch contents if the view is shown in the current page.
		if (editor == null || !isLinkingEnabled() || !checkIfPageIsVisible() || isViewPinned()) {
			return;
		}
		IEditorInput input = editor.getEditorInput();

		IFile file = ResourceUtil.getFile(input);
		if (file != null) {
			showHistory(file); /* don't fetch if already cached */
		} else {
			// see if it adapts to an IHistoryPageSource
			Object pageSource = Adapters.adapt(input, IHistoryPageSource.class);
			if (pageSource != null)
				showHistory(input);
		}
	}

	private boolean checkIfPageIsVisible() {
		return getViewSite().getPage().isPartVisible(this);
	}

	@Override
	public void dispose() {
		//Remove the drop listener
		if (dropTarget != null && !dropTarget.isDisposed())
			dropTarget.removeDropListener(dropAdapter);

		//Remove the selection listener
		getSite().getPage().removePostSelectionListener(selectionListener);
		navigateAction.dispose();
		super.dispose();
	}

	@Override
	public IHistoryPage showHistoryFor(Object object) {
		return showHistoryFor(object, false);
	}

	@Override
	public IHistoryPage getHistoryPage() {
		return (IHistoryPage) getCurrentPage();
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getSource() == getCurrentPage()) {
			if (event.getProperty().equals(IHistoryPage.P_NAME)) {
				Display.getDefault().asyncExec(() -> {
					IHistoryPage historyPage = getHistoryPage();
					setContentDescription(historyPage.getName());
					navigationHistory.updateName(historyPage, getPageSourceFor(historyPage.getInput(), null));
				});
			} else if (event.getProperty().equals(IHistoryPage.P_DESCRIPTION)) {
				// We don't show the description
			}
		}
	}

	public IHistoryView findAppropriateHistoryViewFor(Object input,
			IHistoryPageSource pageSource) {
		// First, check to see if the input and pageSource of this view match the input
		IHistoryPage page = searchHistoryViewsForObject(input, false, pageSource);
		if (page != null) {
			return page.getHistoryView();
		}
		return findUnpinnedHistoryView();
	}

	private void showHistory(Object object) {
		// Only show the history if the input differs
		// (i.e. don't do the change if the input is the same but the page source differs; bug 167648)
		if (getHistoryPage().getInput() != object)
			showHistoryPageFor(object, false, false, null);
	}

	@Override
	public boolean show(ShowInContext context) {
		ISelection selection = context.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection) selection;
			if (ss.size() == 1) {
				// If we can show the selection, return.
				// Otherwise, fall through and attempt to show the input
				if ((showHistoryFor(ss.getFirstElement()) != null))
					return true;
			}
		}
		if (context.getInput() != null) {
			return (showHistoryFor(context.getInput()) != null);
		}
		return false;
	}
}
