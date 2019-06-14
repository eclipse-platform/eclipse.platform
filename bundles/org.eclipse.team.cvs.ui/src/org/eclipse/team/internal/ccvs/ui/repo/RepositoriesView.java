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
package org.eclipse.team.internal.ccvs.ui.repo;


import java.util.Properties;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.util.KnownRepositories;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.model.AllRootsElement;
import org.eclipse.team.internal.ccvs.ui.wizards.NewLocationWizard;
import org.eclipse.team.internal.core.TeamPlugin;
import org.eclipse.team.internal.ui.actions.TeamAction;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.ui.part.PluginTransfer;
import org.eclipse.ui.part.PluginTransferData;

/**
 * RepositoriesView is a view on a set of known CVS repositories
 * which allows navigation of the structure of the repository and
 * the performing of CVS-specific operations on the repository contents.
 */
public class RepositoriesView extends RemoteViewPart {
	public static final String VIEW_ID = "org.eclipse.team.ccvs.ui.RepositoriesView"; //$NON-NLS-1$
	
	// The root
	private AllRootsElement root;
	
	// Actions
	private IAction removeAction;
	private Action newAction;
	private Action newAnonAction;
	private PropertyDialogAction propertiesAction;
	private RemoveRootAction removeRootAction;
	private RemoveDateTagAction removeDateTagAction;
	private RepositoriesFilterAction repositoriesFilterAction;
	private IAction toggleFilterAction;
	
	private RepositoriesSortingActionGroup repositoriesSortingActionGroup;
	private IDialogSettings dialogSettings;
	private static final String SELECTED_ORDER_BY = "selectedOrderBy"; //$NON-NLS-1$
	private static final String SELECTED_SORTING_ORDER = "selectedSortingOrder"; //$NON-NLS-1$
	private RepositoryComparator savedComparator;
	private RepositoriesFilter repositoriesFilter;
	private static final String FILTER_SHOW_MODULES = "filterShowModules"; //$NON-NLS-1$

	IRepositoryListener listener = new IRepositoryListener() {
		@Override
		public void repositoryAdded(final ICVSRepositoryLocation root) {
			getViewer().getControl().getDisplay().asyncExec(() -> {
				refreshViewer();
				getViewer().setSelection(new StructuredSelection(root));
			});
		}
		@Override
		public void repositoriesChanged(ICVSRepositoryLocation[] roots) {
			refresh();
		}
		private void refresh() {
			Display display = getViewer().getControl().getDisplay();
			display.asyncExec(() -> RepositoriesView.this.refreshViewer());
		}
	};
	
	private static final class RepositoryDragSourceListener implements DragSourceListener {
		private IStructuredSelection selection;

		@Override
		public void dragStart(DragSourceEvent event) {
			if(selection!=null) {            
				final Object[] array = selection.toArray();
				// event.doit = Utils.getResources(array).length > 0;
				for (Object a : array) {
					if (a instanceof ICVSRemoteFile) {
						event.doit = true;
						return;
					}
				}
				event.doit = false;
			}
		}

		@Override
		public void dragSetData(DragSourceEvent event) {
			if (selection!=null && CVSResourceTransfer.getInstance().isSupportedType(event.dataType)) {
				final Object[] array = selection.toArray();
				for (Object a : array) {
					if (a instanceof ICVSRemoteFile) {
						event.data = a;
						return;
					}
				}
			} else if (PluginTransfer.getInstance().isSupportedType(event.dataType)) {
				final Object[] array = selection.toArray();
				for (Object a : array) {
					if (a instanceof ICVSRemoteFile) {
						event.data = new PluginTransferData("org.eclipse.team.cvs.ui.cvsRemoteDrop", CVSResourceTransfer.getInstance().toByteArray((ICVSRemoteFile) a)); //$NON-NLS-1$
						return;
					}
				}
				
			}
		}
		
		@Override
		public void dragFinished( DragSourceEvent event) {
		}

		public void updateSelection( IStructuredSelection selection) {
			this.selection = selection;
		}
	}
	
	RepositoryDragSourceListener repositoryDragSourceListener;

	/**
	 * Constructor for RepositoriesView.
	 */
	public RepositoriesView() {
		super(VIEW_ID);		
		IDialogSettings workbenchSettings = CVSUIPlugin.getPlugin().getDialogSettings();
		dialogSettings = workbenchSettings.getSection(VIEW_ID);
		if (dialogSettings == null) {
			dialogSettings = workbenchSettings.addNewSection(VIEW_ID);
		}
		
		try {
			// parse the values
			String selectedOrderBy = dialogSettings.get(SELECTED_ORDER_BY);
			String selectedSortingOrder = dialogSettings.get(SELECTED_SORTING_ORDER);
			
			int orderBy = Integer.parseInt(selectedOrderBy);
			boolean ascending = Boolean.valueOf(selectedSortingOrder).booleanValue();
			
			savedComparator = new RepositoryComparator(orderBy, ascending);
		} catch (NumberFormatException e) {
			// use default comparator
			savedComparator = new RepositoryComparator(
					RepositoryComparator.ORDER_BY_LABEL, true);
		}
		
		if (dialogSettings.get(FILTER_SHOW_MODULES) != null && !dialogSettings.get(FILTER_SHOW_MODULES).equals("")) //$NON-NLS-1$
			repositoriesFilter = new RepositoriesFilter(dialogSettings.getBoolean(FILTER_SHOW_MODULES));
	}

	/**
	 * Contribute actions to the view
	 */
	@Override
	protected void contributeActions() {
		
		final Shell shell = getShell();
		
		// Create actions

		// New Repository (popup)
		newAction = new Action(CVSUIMessages.RepositoriesView_new, CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_NEWLOCATION)) { 
			@Override
			public void run() {
				NewLocationWizard wizard = new NewLocationWizard();
				wizard.setSwitchPerspectives(false);
				WizardDialog dialog = new WizardDialog(shell, wizard);
				dialog.open();
			}
		};
		PlatformUI.getWorkbench().getHelpSystem().setHelp(newAction, IHelpContextIds.NEW_REPOSITORY_LOCATION_ACTION);
		
		if (includeAnonConnection()) {
			newAnonAction = new Action(CVSUIMessages.RepositoriesView_newAnonCVS, CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_NEWLOCATION)) { 
				@Override
				public void run() {
					Properties p = new Properties();
					p.setProperty("connection", "pserver"); //$NON-NLS-1$ //$NON-NLS-2$
					p.setProperty("user", "anonymous"); //$NON-NLS-1$ //$NON-NLS-2$
					p.setProperty("host", "dev.eclipse.org"); //$NON-NLS-1$ //$NON-NLS-2$
					p.setProperty("root", "/cvsroot/eclipse"); //$NON-NLS-1$ //$NON-NLS-2$
					NewLocationWizard wizard = new NewLocationWizard(p);
					wizard.setSwitchPerspectives(false);
					WizardDialog dialog = new WizardDialog(shell, wizard);
					dialog.open();
				}
			};
			PlatformUI.getWorkbench().getHelpSystem().setHelp(newAnonAction, IHelpContextIds.NEW_DEV_ECLIPSE_REPOSITORY_LOCATION_ACTION);
		}
		
		// Properties
		propertiesAction = new PropertyDialogAction(shell, getViewer());
		getViewSite().getActionBars().setGlobalActionHandler(ActionFactory.PROPERTIES.getId(), propertiesAction);		
		IStructuredSelection selection = getViewer().getStructuredSelection();
		if (selection.size() == 1 && selection.getFirstElement() instanceof RepositoryRoot) {
			propertiesAction.setEnabled(true);
		} else {
			propertiesAction.setEnabled(false);
		}
		getViewer().addSelectionChangedListener(event -> {
			IStructuredSelection ss = event.getStructuredSelection();
			boolean enabled = ss.size() == 1 && ss.getFirstElement() instanceof RepositoryRoot;
			propertiesAction.setEnabled(enabled);
		});
		removeRootAction = new RemoveRootAction(viewer.getControl().getShell(), this);
		removeRootAction.selectionChanged((IStructuredSelection)null);
		removeDateTagAction = new RemoveDateTagAction();
		removeDateTagAction.selectionChanged( (IStructuredSelection)null);
		removeAction = new Action(){
			@Override
			public void run(){
				if(removeRootAction.isEnabled()){
					removeRootAction.run();
				}
				if(removeDateTagAction.isEnabled()){
					removeDateTagAction.run();
				}
			}
		};
		PlatformUI.getWorkbench().getHelpSystem().setHelp(removeRootAction, IHelpContextIds.REMOVE_REPOSITORY_LOCATION_ACTION);
		IActionBars bars = getViewSite().getActionBars();
		bars.setGlobalActionHandler(ActionFactory.DELETE.getId(), removeAction);
		
		// Sort By action group
		IPropertyChangeListener comparatorUpdater = event -> {
			String property = event.getProperty();
			if (RepositoriesSortingActionGroup.CHANGE_COMPARATOR
					.equals(property)) {
				Object newValue = event.getNewValue();
				getViewer().refresh();
				saveSelectedComparator((RepositoryComparator) newValue);
			}
		};
		setActionGroup(new RepositoriesSortingActionGroup(shell,
				comparatorUpdater));
		// restore comparator selection
		getRepositoriesSortingActionGroup().setSelectedComparator(
				savedComparator);
		
		super.contributeActions();
		
		toggleFilterAction = new Action(CVSUIMessages.RepositoriesView_NoFilter){
			@Override
			public void run(){
				if (repositoriesFilter != null)
					getViewer().removeFilter(repositoriesFilter);
					repositoriesFilter = null;
					toggleFilterAction.setEnabled(false);
					repositoriesFilterAction.setFilter(repositoriesFilter);
			}
		};
		toggleFilterAction.setEnabled(repositoriesFilter != null);
		
		//Create the filter action
		repositoriesFilterAction = new RepositoriesFilterAction(this);
		repositoriesFilterAction.setText(CVSUIMessages.RepositoriesView_FilterOn);
		repositoriesFilterAction.init(getViewer());
		repositoriesFilterAction.setFilter(repositoriesFilter);
		repositoriesFilterAction.setToolTipText(CVSUIMessages.RepositoriesView_FilterRepositoriesTooltip);
		repositoriesFilterAction.setImageDescriptor(CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_FILTER_HISTORY));
		
		IMenuManager actionBarsMenu = bars.getMenuManager();
		if (actionBarsMenu != null){
			actionBarsMenu.add(new Separator());
			actionBarsMenu.add(repositoriesFilterAction);
			actionBarsMenu.add(toggleFilterAction);
		}
	}
	
	private void saveSelectedComparator(RepositoryComparator selectedComparator) {
		if (dialogSettings != null) {
			dialogSettings.put(SELECTED_ORDER_BY, selectedComparator.getOrderBy());
			dialogSettings.put(SELECTED_SORTING_ORDER, selectedComparator.isAscending());
		}
	}
	
	/**
	 * Returns the action group.
	 * 
	 * @return the action group
	 */
	private RepositoriesSortingActionGroup getRepositoriesSortingActionGroup() {
		return repositoriesSortingActionGroup;
	}

	/**
	 * Sets the action group.
	 * 
	 * @param actionGroup the action group
	 */
	private void setActionGroup(RepositoriesSortingActionGroup actionGroup) {
		this.repositoriesSortingActionGroup = actionGroup;
	}

	/**
	 * Method includeEclipseConnection.
	 * @return boolean
	 */
	private boolean includeAnonConnection() {
		return System.getProperty("eclipse.cvs.anon") != null; //$NON-NLS-1$
	}

	@Override
	protected void addWorkbenchActions(IMenuManager manager) {
		// New actions go next
		MenuManager sub = new MenuManager(CVSUIMessages.RepositoriesView_newSubmenu, IWorkbenchActionConstants.GROUP_ADD); 
		manager.add(sub);
		super.addWorkbenchActions(manager);
		IStructuredSelection selection = getViewer().getStructuredSelection();

		removeRootAction.selectionChanged(selection);
		removeDateTagAction.selectionChanged(selection);
		if(removeRootAction.isEnabled()) {
			manager.add(removeRootAction);
		}		
		if(removeDateTagAction.isEnabled()){
			manager.add(removeDateTagAction);
		}
		if (selection.size() == 1 && selection.getFirstElement() instanceof RepositoryRoot) {
			manager.add(new Separator());
			manager.add(propertiesAction);
		}
		sub.add(newAction);
		if (newAnonAction != null)
			sub.add(newAnonAction);
		sub.add(new Separator("group1")); //$NON-NLS-1$
	}
	
	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		CVSUIPlugin.getPlugin().getRepositoryManager().addRepositoryListener(listener);
		// We need to set comparator on the viewer, in order to modify it by
		// repositoriesSortingActionGroup in the future. It's the same object
		getViewer().setComparator(savedComparator);
		if (repositoriesFilter != null)
			getViewer().addFilter(repositoriesFilter);
		getRepositoriesSortingActionGroup().fillActionBars(getViewSite().getActionBars());
	}
	
	@Override
	public void dispose() {
		if (repositoriesFilter != null)
			dialogSettings.put(FILTER_SHOW_MODULES, repositoriesFilter.isShowModules());
		else
			dialogSettings.put(FILTER_SHOW_MODULES, (String) null);
		CVSUIPlugin.getPlugin().getRepositoryManager().removeRepositoryListener(listener);
		if (getRepositoriesSortingActionGroup() != null) {
			getRepositoriesSortingActionGroup().dispose();
		}
		super.dispose();
	}
	
	/**
	 * Initialize the repositories and actions
	 */
	private void initialize() {
		root = new AllRootsElement();
	}

	@Override
	protected void initializeListeners() {
		super.initializeListeners();
		viewer.addSelectionChangedListener(removeRootAction);
		viewer.addSelectionChangedListener(removeDateTagAction);
		viewer.addSelectionChangedListener(event -> {
			IStructuredSelection selection = (IStructuredSelection)event.getSelection();
			handleChange(selection);	
		});
		
		repositoryDragSourceListener = new RepositoryDragSourceListener();
		viewer.addDragSupport( DND.DROP_LINK | DND.DROP_DEFAULT, 
				new Transfer[] { CVSResourceTransfer.getInstance(),PluginTransfer.getInstance()}, 
				repositoryDragSourceListener);
	}
	
	@Override
	protected Object getTreeInput() {
		initialize();
		return root;
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		String msg = getStatusLineMessage(selection);
		getViewSite().getActionBars().getStatusLineManager().setMessage(msg);
	}

	private String getStatusLineMessage(ISelection selection) {
		if (selection==null || selection.isEmpty()) return ""; //$NON-NLS-1$
		if (!(selection instanceof IStructuredSelection)) return ""; //$NON-NLS-1$
		IStructuredSelection s = (IStructuredSelection)selection;
		
		if (s.size() > 1)
			return NLS.bind(CVSUIMessages.RepositoriesView_NItemsSelected, new String[] { String.valueOf(s.size()) }); 
		Object element = TeamAction.getAdapter(s.getFirstElement(), ICVSResource.class);
		if (element instanceof ICVSRemoteResource) {
			ICVSRemoteResource res = (ICVSRemoteResource)element;
			String name;
			if (res.isContainer()) {
				name = res.getRepositoryRelativePath();
			} else { 
				try {
					name = res.getRepositoryRelativePath() + " " + ((ICVSRemoteFile)res).getRevision(); //$NON-NLS-1$
				} catch (TeamException e) {
					TeamPlugin.log(IStatus.ERROR, CVSUIMessages.RepositoriesView_CannotGetRevision, e); 
					name = res.getRepositoryRelativePath();
				} 
			}
			return NLS.bind(CVSUIMessages.RepositoriesView_ResourceInRepository, new String[] { name, res.getRepository().getLocation(true) }); 
		}
		return CVSUIMessages.RepositoriesView_OneItemSelected; 
	}
	
	@Override
	protected String getHelpContextId() {
		return IHelpContextIds.REPOSITORIES_VIEW;
	}

	@Override
	protected KeyAdapter getKeyListener() {
		return new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent event) {
				if (event.keyCode == SWT.F5) {
					if (WorkbenchUserAuthenticator.USE_ALTERNATE_PROMPTER) {
						ICVSRepositoryLocation[] locations = KnownRepositories.getInstance().getRepositories();
						for (ICVSRepositoryLocation location : locations) {
							location.flushUserInfo();
						}
					} else {
						refreshAll();
					}
				} else if (event.keyCode == SWT.F9 && WorkbenchUserAuthenticator.USE_ALTERNATE_PROMPTER) {
					refreshAll();
				}
			}
		};
	}
		
	private void handleChange(IStructuredSelection selection){
		removeRootAction.updateSelection(selection);
		removeDateTagAction.updateSelection(selection);
		removeAction.setEnabled(removeRootAction.isEnabled() || removeDateTagAction.isEnabled());
		
		repositoryDragSourceListener.updateSelection(selection);
	}
	
	public void showFilter(RepositoriesFilter filter) {
		if (repositoriesFilter != null)
			getViewer().removeFilter(repositoriesFilter);
		repositoriesFilter = filter;
		getViewer().addFilter(filter);
		toggleFilterAction.setEnabled(true);
	}
	
}
