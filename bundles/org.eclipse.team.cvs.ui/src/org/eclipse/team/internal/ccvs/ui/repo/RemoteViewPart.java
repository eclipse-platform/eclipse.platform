/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.repo;

import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFile;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.actions.OpenRemoteFileAction;
import org.eclipse.team.internal.ccvs.ui.model.RemoteContentProvider;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.WorkingSetFilterActionGroup;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;

/**
 * This class acts as a superclass to all remote CVS tree views.
 */
public abstract class RemoteViewPart extends ViewPart implements ISelectionListener {
	
	// The tree viewer
	protected TreeViewer viewer;

	// Drill down adapter
	private DrillDownAdapter drillPart;
	
	private Action refreshAction;
	private Action collapseAllAction;
	
	private OpenRemoteFileAction openAction;
	
	private WorkingSetFilterActionGroup workingSetActionGroup;
	private RemoteContentProvider contentProvider;
	private IDialogSettings settings;

	private static final String SELECTED_WORKING_SET = "SelectedWorkingSet"; //$NON-NLS-1$

	/* package */ class DecoratingRepoLabelProvider extends WorkbenchLabelProvider {
		@Override
		protected String decorateText(String input, Object element) {
			//Used to process RTL locales only
			return TextProcessor.process(input, ":@/"); //$NON-NLS-1$
		}
	}
	
	protected RemoteViewPart(String partName) {
		IDialogSettings workbenchSettings = CVSUIPlugin.getPlugin().getDialogSettings();
		settings = workbenchSettings.getSection(partName);
		if (settings == null) {
			settings = workbenchSettings.addNewSection(partName);
		}
		String name = settings.get(SELECTED_WORKING_SET);
		IWorkingSet set = null;
		if (name != null)
			set = PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(name);
		setWorkingSet(set, false);
	}
	
	@Override
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(getContentProvider());
		viewer.setLabelProvider(new DecoratingRepoLabelProvider()/*WorkbenchLabelProvider()*/);
		getSite().setSelectionProvider(viewer);
		viewer.setInput(getTreeInput());
		viewer.setComparator(new RepositoryComparator());
		viewer.getControl().addKeyListener(getKeyListener());
		drillPart = new DrillDownAdapter(viewer);
		
		contributeActions();
		initializeListeners();
		
		getWorkingSetActionGroup().fillActionBars(getViewSite().getActionBars());

		// F1 Help
		String helpID = getHelpContextId();
		if (helpID != null)
			PlatformUI.getWorkbench().getHelpSystem().setHelp(viewer.getControl(), helpID);
	}
	
	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}
	
	/**
	 * Method getHelpContextId should be overridden by subclasses to provide the
	 * appropriate help id.
	 * 
	 * @return String
	 */
	protected String getHelpContextId() {
		return null;
	}
	
	/**
	 * Method getTreeInput.
	 * @return Object
	 */
	protected abstract Object getTreeInput();
	
	/**
	 * @see org.eclipse.team.internal.ccvs.ui.repo.RemoteViewPart#getContentProvider()
	 */
	public RemoteContentProvider getContentProvider() {
		if (contentProvider == null) {
			contentProvider = new RemoteContentProvider();
		}
		return contentProvider;
	}

	protected KeyAdapter getKeyListener() {
		return new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent event) {
				if (event.keyCode == SWT.F5) {
					refreshAll();
				}
			}
		};
	}

	protected void initializeListeners() {
		// listen for selection changes in the repo view
		getSite().getWorkbenchWindow().getSelectionService().addPostSelectionListener(this);
	}
	
	protected IWorkingSet getWorkingSet() {
		return getContentProvider().getWorkingSet();
	}
	
	protected void setWorkingSet(IWorkingSet workingSet, boolean refreshViewer) {
		if (settings != null) {
			String name = null;
			if (workingSet != null)
				name = workingSet.getName();
			settings.put(SELECTED_WORKING_SET, name);
		}
		getContentProvider().setWorkingSet(workingSet);
		String toolTip;
		if (workingSet == null) {
			toolTip = null;
		} else {
			toolTip = NLS.bind(CVSUIMessages.RemoteViewPart_workingSetToolTip, new String[] { workingSet.getName() }); 
		}
		setTitleToolTip(toolTip);
		if (refreshViewer) refreshViewer();
	}
	
	/**
	 * Contribute actions to the view
	 */
	protected void contributeActions() {
		
		final Shell shell = getShell();
		
		// Refresh (toolbar)
		CVSUIPlugin plugin = CVSUIPlugin.getPlugin();
		refreshAction = new Action(CVSUIMessages.RepositoriesView_refresh, CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_REFRESH_ENABLED)) { 
			@Override
			public void run() {
				refreshAll();
			}
		};
		refreshAction.setToolTipText(CVSUIMessages.RepositoriesView_refreshTooltip); 
		refreshAction.setDisabledImageDescriptor(plugin.getImageDescriptor(ICVSUIConstants.IMG_REFRESH_DISABLED));
		refreshAction.setHoverImageDescriptor(plugin.getImageDescriptor(ICVSUIConstants.IMG_REFRESH));
		getViewSite().getActionBars().setGlobalActionHandler(ActionFactory.REFRESH.getId(), refreshAction);

		collapseAllAction = new Action(CVSUIMessages.RepositoriesView_collapseAll, CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_COLLAPSE_ALL_ENABLED)) { 
			@Override
			public void run() {
				collapseAll();
			}
		};
		collapseAllAction.setToolTipText(CVSUIMessages.RepositoriesView_collapseAllTooltip); 
		collapseAllAction.setHoverImageDescriptor(plugin.getImageDescriptor(ICVSUIConstants.IMG_COLLAPSE_ALL));
		
		// Working Set action group
		IPropertyChangeListener workingSetUpdater = event -> {
			String property = event.getProperty();
			if (WorkingSetFilterActionGroup.CHANGE_WORKING_SET.equals(property)) {
				Object newValue = event.getNewValue();
				setWorkingSet((IWorkingSet) newValue, true);
			}
		};
		setActionGroup(new WorkingSetFilterActionGroup(shell, workingSetUpdater));
		getWorkingSetActionGroup().setWorkingSet(getContentProvider().getWorkingSet());
		
		// Create the popup menu
		MenuManager menuMgr = new MenuManager();
		Tree tree = viewer.getTree();
		Menu menu = menuMgr.createContextMenu(tree);
		menuMgr.addMenuListener(manager -> addWorkbenchActions(manager));
		menuMgr.setRemoveAllWhenShown(true);
		tree.setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);

		// Create the local tool bar
		IActionBars bars = getViewSite().getActionBars();
		IToolBarManager tbm = bars.getToolBarManager();
		drillPart.addNavigationActions(tbm);
		tbm.add(refreshAction);
		tbm.add(new Separator());
		tbm.add(collapseAllAction);
		tbm.update(false);

		// Register the open handler
		openAction = new OpenRemoteFileAction();
		viewer.addOpenListener(event -> handleOpen(event));

		bars.updateActionBars();
	}
	
	/**
	 * Returns the action group.
	 * 
	 * @return the action group
	 */
	private WorkingSetFilterActionGroup getWorkingSetActionGroup() {
		return workingSetActionGroup;
	}

	/**
	 * Sets the action group.
	 * 
	 * @param actionGroup the action group
	 */
	private void setActionGroup(WorkingSetFilterActionGroup actionGroup) {
		this.workingSetActionGroup = actionGroup;
	}
	
	/**
	 * Add the menu actions that were contributed in plugin.xml
	 * 
	 * @param manager
	 */
	protected void addWorkbenchActions(IMenuManager manager) {
		// File actions go first (view file)
		manager.add(new Separator(IWorkbenchActionConstants.GROUP_FILE));
		// Misc additions
		manager.add(new Separator("checkoutGroup")); //$NON-NLS-1$
		manager.add(new Separator("tagGroup")); //$NON-NLS-1$
		manager.add(new Separator("miscGroup")); //$NON-NLS-1$
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

		manager.add(refreshAction);
	}
	
	/**
	 * Method getShell.
	 * @return Shell
	 */
	protected Shell getShell() {
		return viewer.getTree().getShell();
	}
	
	protected void refreshAll() {
		//CVSUIPlugin.getPlugin().getRepositoryManager().clearCaches();
		refreshViewer();
	}

	protected void refreshViewer() {
		if (viewer == null) return;
		((RemoteContentProvider)viewer.getContentProvider()).cancelJobs(CVSUIPlugin.getPlugin().getRepositoryManager().getKnownRepositoryRoots());
		((RemoteContentProvider)viewer.getContentProvider()).purgeCache();
		CVSUIPlugin.getPlugin().getRepositoryManager().purgeCache();
		viewer.getControl().setRedraw(false);
		viewer.refresh();
		viewer.getControl().setRedraw(true);
	}
	
	public void collapseAll() {
		if (viewer == null) return;
		viewer.getControl().setRedraw(false);
		viewer.collapseToLevel(viewer.getInput(), AbstractTreeViewer.ALL_LEVELS);
		viewer.getControl().setRedraw(true);
	}
	/**
	 * Handle open request.
	 */
	/* package */ void handleOpen(OpenEvent e) {
		// Only act on single selection
		ISelection selection = e.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structured = (IStructuredSelection)selection;
			if (structured.size() == 1) {
				Object first = structured.getFirstElement();
				if (first instanceof ICVSRemoteFile) {
					// It's a file, open it.
					openAction.selectionChanged(null, selection);
					openAction.run(null);
				} else {
					// Try to expand/contract
					viewer.setExpandedState(first, !viewer.getExpandedState(first));
				}
			}
		}
	}
	
	/**
	 * Returns the viewer.
	 * @return TreeViewer
	 */
	public TreeViewer getViewer() {
		return viewer;
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
	}

	@Override
	public void dispose() {
		getSite().getWorkbenchWindow().getSelectionService().removePostSelectionListener(this);
		if (getWorkingSetActionGroup() != null) {
			getWorkingSetActionGroup().dispose();
		}
		super.dispose();
		viewer = null;
	}
	
}
