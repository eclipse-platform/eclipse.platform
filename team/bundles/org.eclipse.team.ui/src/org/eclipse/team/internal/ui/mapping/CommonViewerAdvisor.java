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
 *******************************************************************************/
package org.eclipse.team.internal.ui.mapping;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.mapping.IModelProviderDescriptor;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.DecoratingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DecorationContext;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.AbstractTreeViewerAdvisor;
import org.eclipse.team.internal.ui.synchronize.SynchronizePageConfiguration;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.mapping.ITeamContentProviderDescriptor;
import org.eclipse.team.ui.mapping.ITeamContentProviderManager;
import org.eclipse.team.ui.mapping.SynchronizationStateTester;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipant;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.SubActionBars;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.navigator.CommonViewerSiteFactory;
import org.eclipse.ui.navigator.CommonViewerSorter;
import org.eclipse.ui.navigator.ICommonViewerSite;
import org.eclipse.ui.navigator.INavigatorContentExtension;
import org.eclipse.ui.navigator.INavigatorContentServiceListener;
import org.eclipse.ui.navigator.NavigatorActionService;
import org.eclipse.ui.part.IPageSite;

/**
 * Provides a Common Navigator based viewer for use by a {@link ModelSynchronizePage}.
 */
public class CommonViewerAdvisor extends AbstractTreeViewerAdvisor implements INavigatorContentServiceListener, IEmptyTreeListener, IPropertyChangeListener {

	public static final class NavigableCommonViewer extends CommonViewer implements ITreeViewerAccessor {
		private final IEmptyTreeListener listener;
		private boolean empty;
		private NavigableCommonViewer(String id, Composite parent, int style, IEmptyTreeListener listener) {
			super(id, parent, style);
			this.listener = listener;
		}
		@Override
		public void createChildren(TreeItem item) {
			super.createChildren(item);
		}
		@Override
		public void openSelection() {
			fireOpen(new OpenEvent(this, getSelection()));
		}
		@Override
		protected void internalRefresh(Object element, boolean updateLabels) {
			TreePath[] expanded = getVisibleExpandedPaths();
			super.internalRefresh(element, updateLabels);
			setExpandedTreePaths(expanded);
			checkForEmptyViewer();
		}
		@Override
		protected void internalRemove(Object parent, Object[] elements) {
			super.internalRemove(parent, elements);
			if (parent == getInput())
				checkForEmptyViewer();
		}
		@Override
		protected void internalRemove(Object[] elements) {
			super.internalRemove(elements);
			checkForEmptyViewer();
		}
		@Override
		protected void internalAdd(Widget widget, Object parentElement, Object[] childElements) {
			super.internalAdd(widget, parentElement, childElements);
			if (empty) {
				empty = false;
				listener.notEmpty(this);
			}

		}
		@Override
		protected void inputChanged(Object input, Object oldInput) {
			super.inputChanged(input, oldInput);
			checkForEmptyViewer();
		}
		private void checkForEmptyViewer() {
			Object input = getInput();
			if (input != null) {
				Widget w = findItem(input);
				Item[] children = getChildren(w);
				if (children.length == 0) {
					if (!empty) {
						empty = true;
						listener.treeEmpty(this);
					}
					return;
				}
			}
			empty = false;
			if (listener != null)
				listener.notEmpty(this);
		}
		public boolean isEmpty() {
			return empty;
		}

		@Override
		protected void initDragAndDrop() {
			getNavigatorContentService().getDnDService().bindDragAssistant(new ResourceDragAdapterAssistant());
			super.initDragAndDrop();
		}
		/**
		 * Gets the expanded elements that are visible to the user. An expanded
		 * element is only visible if the parent is expanded.
		 *
		 * @return the visible expanded elements
		 * @since 2.0
		 */
		public TreePath[] getVisibleExpandedPaths() {
			ArrayList<TreePath> v = new ArrayList<>();
			internalCollectVisibleExpanded(v, getControl());
			return v.toArray(new TreePath[v.size()]);
		}

		private void internalCollectVisibleExpanded(ArrayList<TreePath> result, Widget widget) {
			Item[] items = getChildren(widget);
			for (Item item : items) {
				if (getExpanded(item)) {
					TreePath path = getTreePathFromItem(item);
					if (path != null) {
						result.add(path);
					}
					//Only recurse if it is expanded - if
					//not then the children aren't visible
					internalCollectVisibleExpanded(result, item);
				}
			}
		}
	}

	/**
	 * Subclass of SubActionBars that manages the contributions from the common action service
	 */
	private class CommonSubActionBars extends SubActionBars {

		public CommonSubActionBars(IActionBars parent) {
			super(parent);
		}

		@Override
		public void setGlobalActionHandler(String actionID, IAction handler) {
			if (handler == null) {
				// Only remove the handler if it was set
				if (getGlobalActionHandler(actionID) != null) {
					getParent().setGlobalActionHandler(actionID, null);
					super.setGlobalActionHandler(actionID, null);
				}
			} else {
				// Only set the action handler if the parent doesn't
				if (getParent().getGlobalActionHandler(actionID) != null) {
					TeamUIPlugin.log(new TeamException(NLS.bind("Conflicting attempt to set action id {0} detected", actionID))); //$NON-NLS-1$
					return;
				}
				super.setGlobalActionHandler(actionID, handler);
			}
		}

		@Override
		public void clearGlobalActionHandlers() {
			// When cleared, also remove the ids from the parent
			Map handlers = getGlobalActionHandlers();
			if (handlers != null) {
				Set keys = handlers.keySet();
				Iterator iter = keys.iterator();
				while (iter.hasNext()) {
					String actionId = (String) iter.next();
					getParent().setGlobalActionHandler(actionId,
							null);
				}
			}
			super.clearGlobalActionHandlers();
		}

		@Override
		public void updateActionBars() {
			// On update, push all or action handlers into our parent
			Map newActionHandlers = getGlobalActionHandlers();
			if (newActionHandlers != null) {
				Set keys = newActionHandlers.entrySet();
				Iterator iter = keys.iterator();
				while (iter.hasNext()) {
					Map.Entry entry = (Map.Entry) iter.next();
					getParent().setGlobalActionHandler((String) entry.getKey(),
							(IAction) entry.getValue());
				}
			}
			super.updateActionBars();
		}

	}

	public static final String TEAM_NAVIGATOR_CONTENT = "org.eclipse.team.ui.navigatorViewer"; //$NON-NLS-1$

	private static final String PROP_ACTION_SERVICE_ACTION_BARS = "org.eclipse.team.ui.actionServiceActionBars"; //$NON-NLS-1$

	private Set<INavigatorContentExtension> extensions = new HashSet<>();

	private NavigatorActionService actionService;

	private IEmptyTreeListener emptyTreeListener;

	/**
	 * Create a common viewer
	 * @param parent the parent composite of the common viewer
	 * @param configuration the configuration for the viewer
	 * @return a newly created common viewer
	 */
	private static CommonViewer createViewer(Composite parent, final ISynchronizePageConfiguration configuration, IEmptyTreeListener listener) {
		final CommonViewer v = new NavigableCommonViewer(configuration.getViewerId(), parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL, listener);
		v.setSorter(new CommonViewerSorter());
		v.setSorter(new TeamViewerSorter((CommonViewerSorter)v.getSorter()));
		ISynchronizationScope scope = getScope(configuration);
		bindTeamContentProviders(v);
		scope.addScopeChangeListener((scope1, newMappings, newTraversals) -> {
			enableContentProviders(v, configuration);
			Utils.asyncExec((Runnable) () -> v.refresh(), v);
		});
		enableContentProviders(v, configuration);
		configuration.getSite().setSelectionProvider(v);
		return v;
	}

	private static void enableContentProviders(CommonViewer v, ISynchronizePageConfiguration configuration) {
		v.getNavigatorContentService().getActivationService().activateExtensions(getEnabledContentProviders(configuration), true);
	}

	private static String[] getEnabledContentProviders(ISynchronizePageConfiguration configuration) {
		String visibleModel = (String)configuration.getProperty(ModelSynchronizeParticipant.P_VISIBLE_MODEL_PROVIDER);
		if (visibleModel != null && !visibleModel.equals(ModelSynchronizeParticipant.ALL_MODEL_PROVIDERS_VISIBLE)) {
			ITeamContentProviderDescriptor desc = TeamUI.getTeamContentProviderManager().getDescriptor(visibleModel);
			if (desc != null && desc.isEnabled())
				return new String[] { desc.getContentExtensionId() };
		}
		configuration.setProperty(ModelSynchronizeParticipant.P_VISIBLE_MODEL_PROVIDER, ModelSynchronizeParticipant.ALL_MODEL_PROVIDERS_VISIBLE);
		ModelSynchronizeParticipant participant = (ModelSynchronizeParticipant)configuration.getParticipant();
		ModelProvider[] providers = participant.getEnabledModelProviders();
		Set<String> result = new HashSet<>();
		Object property = configuration.getProperty(ITeamContentProviderManager.PROP_PAGE_LAYOUT);
		boolean isFlatLayout = property != null && property.equals(ITeamContentProviderManager.FLAT_LAYOUT);
		for (ModelProvider provider : providers) {
			ITeamContentProviderDescriptor desc = TeamUI.getTeamContentProviderManager().getDescriptor(provider.getId());
			if (desc != null && desc.isEnabled() && (!isFlatLayout || desc.isFlatLayoutSupported()))
				result.add(desc.getContentExtensionId());
		}
		return result.toArray(new String[result.size()]);
	}

	private static void bindTeamContentProviders(CommonViewer v) {
		ITeamContentProviderManager teamContentProviderManager = TeamUI.getTeamContentProviderManager();
		ITeamContentProviderDescriptor[] descriptors = teamContentProviderManager.getDescriptors();
		Set<String> toBind = new HashSet<>();
		for (ITeamContentProviderDescriptor descriptor : descriptors) {
			toBind.add(descriptor.getContentExtensionId());
		}
		v.getNavigatorContentService().bindExtensions(toBind.toArray(new String[toBind.size()]), true);
	}

	private static ISynchronizationScope getScope(ISynchronizePageConfiguration configuration) {
		return (ISynchronizationScope)configuration.getProperty(ITeamContentProviderManager.P_SYNCHRONIZATION_SCOPE);
	}

	/**
	 * Create the advisor using the given configuration
	 * @param parent the parent
	 * @param configuration the configuration
	 */
	public CommonViewerAdvisor(Composite parent, ISynchronizePageConfiguration configuration) {
		super(configuration);
		final CommonViewer viewer = CommonViewerAdvisor.createViewer(parent, configuration, this);
		TeamUI.getTeamContentProviderManager().addPropertyChangeListener(this);
		configuration.addPropertyChangeListener(this);
		GridData data = new GridData(GridData.FILL_BOTH);
		viewer.getControl().setLayoutData(data);
		viewer.getNavigatorContentService().addListener(this);
		initializeViewer(viewer);
		IBaseLabelProvider provider = viewer.getLabelProvider();
		if (provider instanceof DecoratingLabelProvider) {
			DecoratingLabelProvider dlp = (DecoratingLabelProvider) provider;
			ILabelDecorator decorator = ((SynchronizePageConfiguration)configuration).getLabelDecorator();
			if (decorator != null) {
				ILabelProvider lp = dlp.getLabelProvider();
				dlp = new DecoratingLabelProvider(
						new DecoratingLabelProvider(lp, decorator),
						PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator());
				viewer.setLabelProvider(dlp);
			}
			DecorationContext decorationContext = new DecorationContext();
			decorationContext.putProperty(SynchronizationStateTester.PROP_TESTER, new SynchronizationStateTester() {
				@Override
				public boolean isStateDecorationEnabled() {
					return false;
				}
			});
			dlp.setDecorationContext(decorationContext);
		} else if (provider instanceof DecoratingStyledCellLabelProvider) {
			DecoratingStyledCellLabelProvider dsclp = (DecoratingStyledCellLabelProvider) provider;
			ILabelDecorator decorator = ((SynchronizePageConfiguration) configuration)
					.getLabelDecorator();
			if (decorator != null) {
				IStyledLabelProvider slp = dsclp.getStyledStringProvider();
				dsclp = new DecoratingStyledCellLabelProvider(
						new MyDecoratingStyledCellLabelProvider(slp, decorator),
						PlatformUI.getWorkbench().getDecoratorManager()
								.getLabelDecorator(), null);
				viewer.setLabelProvider(dsclp);
			}
			DecorationContext decorationContext = new DecorationContext();
			decorationContext.putProperty(SynchronizationStateTester.PROP_TESTER, new SynchronizationStateTester() {
				@Override
				public boolean isStateDecorationEnabled() {
					return false;
				}
			});
			dsclp.setDecorationContext(decorationContext);
		}
	}

	private class MyDecoratingStyledCellLabelProvider extends
			DecoratingStyledCellLabelProvider implements IStyledLabelProvider,
			IFontProvider {

		private IStyledLabelProvider slp;

		public MyDecoratingStyledCellLabelProvider(IStyledLabelProvider slp,
				ILabelDecorator decorator) {
			super(slp, decorator, null);
			this.slp = slp;
		}

		@Override
		public StyledString getStyledText(Object element) {
			return slp.getStyledText(element);
		}

		@Override
		public Font getFont(Object element) {
			// DelegatingStyledCellLabelProvider does not implement
			// IFontProvider
			return super.getFont(element);
		}
	}

	@Override
	public void setInitialInput() {
		CommonViewer viewer = (CommonViewer)getViewer();
		viewer.setInput(getInitialInput());
		viewer.expandToLevel(2);
	}

	@Override
	public void initializeViewer(StructuredViewer viewer) {
		createActionService((CommonViewer)viewer, getConfiguration());
		super.initializeViewer(viewer);
	}

	private void createActionService(CommonViewer viewer, ISynchronizePageConfiguration configuration) {
		ICommonViewerSite commonSite = createCommonViewerSite(viewer, configuration);
		actionService = new NavigatorActionService(commonSite, viewer, viewer.getNavigatorContentService());
	}

	private ICommonViewerSite createCommonViewerSite(CommonViewer viewer, ISynchronizePageConfiguration configuration) {
		IWorkbenchSite site = configuration.getSite().getWorkbenchSite();
		if (site instanceof IEditorSite) {
			IEditorSite es = (IEditorSite) site;
			return CommonViewerSiteFactory.createCommonViewerSite(es);
		}
		if (site instanceof IViewSite) {
			IViewSite vs = (IViewSite) site;
			return CommonViewerSiteFactory.createCommonViewerSite(vs);
		}
		if (site instanceof IPageSite) {
			IPageSite ps = (IPageSite) site;
			return CommonViewerSiteFactory.createCommonViewerSite(configuration.getViewerId(), ps);
		}
		return CommonViewerSiteFactory.createCommonViewerSite(configuration.getViewerId(), viewer, configuration.getSite().getShell());
	}

	private Object getInitialInput() {
		String visible = (String)getConfiguration().getProperty(ModelSynchronizeParticipant.P_VISIBLE_MODEL_PROVIDER);
		if (visible != null && !visible.equals(ModelSynchronizeParticipant.ALL_MODEL_PROVIDERS_VISIBLE)) {
			try {
				IModelProviderDescriptor desc = ModelProvider.getModelProviderDescriptor(visible);
				if (desc != null)
					return desc.getModelProvider();
			} catch (CoreException e) {
				TeamUIPlugin.log(e);
			}
		}
		return getConfiguration().getProperty(ITeamContentProviderManager.P_SYNCHRONIZATION_CONTEXT);
	}

	@Override
	public void onLoad(INavigatorContentExtension anExtension) {
		extensions.add(anExtension);
		ISynchronizationContext context = getParticipant().getContext();
		anExtension.getStateModel().setProperty(ITeamContentProviderManager.P_SYNCHRONIZATION_SCOPE, context.getScope());
		anExtension.getStateModel().setProperty(ITeamContentProviderManager.P_SYNCHRONIZATION_PAGE_CONFIGURATION, getConfiguration());
		anExtension.getStateModel().setProperty(ITeamContentProviderManager.P_SYNCHRONIZATION_CONTEXT, context);
	}

	private ModelSynchronizeParticipant getParticipant() {
		return (ModelSynchronizeParticipant)getConfiguration().getParticipant();
	}

	@Override
	protected String getContextMenuId(StructuredViewer viewer) {
		return ((CommonViewer)viewer).getNavigatorContentService().getViewerDescriptor().getPopupMenuId();
	}

	@Override
	protected void registerContextMenu(StructuredViewer viewer, MenuManager menuMgr) {
		actionService.prepareMenuForPlatformContributions(menuMgr,
				viewer, false);
	}

	@Override
	protected void fillContextMenu(StructuredViewer viewer, IMenuManager manager) {
		// Clear any handlers from the menu
		if (manager instanceof CommonMenuManager) {
			CommonMenuManager cmm = (CommonMenuManager) manager;
			cmm.clearHandlers();
		}

		// Add the actions from the service (which willal so add the groups)
		ISelection selection = getViewer().getSelection();
		actionService.setContext(new ActionContext(selection));
		actionService.fillContextMenu(manager);

		// Add any programmatic menu items
		super.fillContextMenu(viewer, manager);
	}

	@Override
	public void dispose() {
		TeamUI.getTeamContentProviderManager().removePropertyChangeListener(this);
		getConfiguration().removePropertyChangeListener(this);
		actionService.dispose();
		super.dispose();
	}

	@Override
	protected void updateActionBars(IStructuredSelection selection) {
		super.updateActionBars(selection);
		if (!getConfiguration().getSite().isModal()) {
			actionService.setContext(new ActionContext(selection));
			// This is non-standard behavior that is required by the common navigator framework (see bug 122808)
			SubActionBars subActionBars = (SubActionBars)getConfiguration().getProperty(PROP_ACTION_SERVICE_ACTION_BARS);
			if (subActionBars == null) {
				subActionBars = new CommonSubActionBars(getConfiguration().getSite().getActionBars());
				getConfiguration().setProperty(PROP_ACTION_SERVICE_ACTION_BARS, subActionBars);
			}
			actionService.fillActionBars(subActionBars);
		}
	}

	@Override
	protected MenuManager createContextMenuManager(String targetID) {
		return new CommonMenuManager(targetID);
	}

	@Override
	protected void addContextMenuGroups(IMenuManager manager) {
		// Don't do anything. The groups will be added by the action service
	}

	public void addEmptyTreeListener(IEmptyTreeListener emptyTreeListener) {
		this.emptyTreeListener = emptyTreeListener;
	}

	@Override
	public void treeEmpty(TreeViewer viewer) {
		if (emptyTreeListener != null)
			emptyTreeListener.treeEmpty(viewer);
	}

	@Override
	public void notEmpty(TreeViewer viewer) {
		if (emptyTreeListener != null)
			emptyTreeListener.notEmpty(viewer);
	}

	@Override
	public void propertyChange(final PropertyChangeEvent event) {
		if (event.getProperty().equals(ITeamContentProviderManager.PROP_ENABLED_MODEL_PROVIDERS)) {
			enableContentProviders((CommonViewer)getViewer(), getConfiguration());
		} else if (event.getProperty().equals(ModelSynchronizeParticipant.P_VISIBLE_MODEL_PROVIDER)) {
			enableContentProviders((CommonViewer)getViewer(), getConfiguration());
			final Viewer viewer = getViewer();
			Utils.syncExec((Runnable) () -> {
				Object viewerInput = ModelSynchronizePage.getViewerInput(getConfiguration(), (String)event.getNewValue());
				if (viewer != null && viewerInput != null) {
					viewer.setInput(viewerInput);
				}
			}, (StructuredViewer)viewer);
		} else if (event.getProperty().equals(ITeamContentProviderManager.PROP_PAGE_LAYOUT)) {
			// TODO
			enableContentProviders((CommonViewer)getViewer(), getConfiguration());
		}
	}

	@Override
	protected boolean handleDoubleClick(StructuredViewer viewer, DoubleClickEvent event) {
		if (isOpenable(event.getSelection())) {
			return true;
		}
		return super.handleDoubleClick(viewer, event);
	}

	private boolean isOpenable(ISelection selection) {
		IStructuredSelection ss = (IStructuredSelection) selection;
		Object object = ss.getFirstElement();
		if (object == null)
			return false;
		return getParticipant().hasCompareInputFor(object);
	}

	@Override
	protected void expandToNextDiff(Object element) {
		((TreeViewer)getViewer()).expandToLevel(element, AbstractTreeViewer.ALL_LEVELS);
	}

}
