/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.actions.StatusLineContributionGroup;
import org.eclipse.team.internal.ui.synchronize.actions.SyncInfoSetStatusLineContributionGroup;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.part.ResourceTransfer;

/**
 * A <code>TreeViewerAdvisor</code> that works with TreeViewers. Two default
 * tree viewers are provided that support navigation: <code>NavigableTreeViewer</code>
 * and <code>NavigableCheckboxTreeViewer</code>.
 * <p>
 * Note that this advisor can be used with any tree viewer. By default it provides an
 * expand all action, double click behavior on containers, and navigation support for
 * tree viewers.
 * </p><p>
 * By default this advisor supports hierarchical models and honour the compressed
 * folder Team preference for showing the sync set as compressed folders. Subclasses
 * can provide their own presentation models.
 * <p>
 * @since 3.0
 */
public class TreeViewerAdvisor extends AbstractTreeViewerAdvisor {

	// Special actions that could not be contributed using an ActionGroup
	private StatusLineContributionGroup statusLine;

	/**
	 * Style bit that indicates that a checkbox viewer is desired.
	 */
	public static final int CHECKBOX = 1;

	private SynchronizeModelManager modelManager;

	/**
	 * A navigable checkbox tree viewer that will work with the <code>navigate</code> method of
	 * this advisor.
	 */
	public static class NavigableCheckboxTreeViewer extends ContainerCheckedTreeViewer implements ITreeViewerAccessor {
		public NavigableCheckboxTreeViewer(Composite parent, int style) {
			super(parent, style);
			setUseHashlookup(true);
		}

		@Override
		public void createChildren(TreeItem item) {
			super.createChildren(item);
		}

		@Override
		public void openSelection() {
			fireOpen(new OpenEvent(this, getSelection()));
		}
	}

	/**
	 * A navigable tree viewer that will work with the <code>navigate</code> method of
	 * this advisor.
	 */
	public static class NavigableTreeViewer extends TreeViewer implements ITreeViewerAccessor {
		public NavigableTreeViewer(Composite parent, int style) {
			super(parent, style);
			setUseHashlookup(true);
		}

		@Override
		public void createChildren(TreeItem item) {
			super.createChildren(item);
		}

		@Override
		public void openSelection() {
			fireOpen(new OpenEvent(this, getSelection()));
		}
	}

	public static StructuredViewer createViewer(Composite parent, ISynchronizePageConfiguration configuration) {
		int style = ((SynchronizePageConfiguration)configuration).getViewerStyle();
		if ((style & CHECKBOX) > 0) {
			NavigableCheckboxTreeViewer v = new TreeViewerAdvisor.NavigableCheckboxTreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
			configuration.getSite().setSelectionProvider(v);
			return v;
		} else {
			NavigableTreeViewer v = new TreeViewerAdvisor.NavigableTreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
			configuration.getSite().setSelectionProvider(v);
			return v;
		}
	}

	/**
	 * Create an advisor that will allow viewer contributions with the given <code>targetID</code>. This
	 * advisor will provide a presentation model based on the given sync info set. Note that it's important
	 * to call {@link #dispose()} when finished with an advisor.
	 *
	 * @param parent
	 * @param configuration
	 */
	public TreeViewerAdvisor(Composite parent, ISynchronizePageConfiguration configuration) {
		super(configuration);

		// Allow the configuration to provide it's own model manager but if one isn't initialized, then
		// simply use the default provided by the advisor.
		modelManager = (SynchronizeModelManager)configuration.getProperty(SynchronizePageConfiguration.P_MODEL_MANAGER);
		if(modelManager == null) {
			modelManager = createModelManager(configuration);
			configuration.setProperty(SynchronizePageConfiguration.P_MODEL_MANAGER, modelManager);
		}
		Assert.isNotNull(modelManager, "model manager must be set"); //$NON-NLS-1$
		modelManager.setViewerAdvisor(this);

		StructuredViewer viewer = TreeViewerAdvisor.createViewer(parent, configuration);
		GridData data = new GridData(GridData.FILL_BOTH);
		viewer.getControl().setLayoutData(data);
		initializeViewer(viewer);
	}

	@Override
	public void setInitialInput() {
		// The input will be set later
	}

	/**
	 * Create the model manager to be used by this advisor
	 * @param configuration
	 */
	protected SynchronizeModelManager createModelManager(ISynchronizePageConfiguration configuration) {
		ISynchronizeParticipant participant = configuration.getParticipant();
		if (participant instanceof IChangeSetProvider) {
			IChangeSetProvider provider = (IChangeSetProvider) participant;
			ChangeSetCapability changeSetCapability = provider.getChangeSetCapability();
			if (changeSetCapability != null) {
				if (changeSetCapability.supportsActiveChangeSets() || changeSetCapability.supportsCheckedInChangeSets()) {
					return new ChangeSetModelManager(configuration);
				}
			}
		}
		return new HierarchicalModelManager(configuration);
	}

	/*
	 * For use by test cases only
	 * @return Returns the modelManager.
	 */
	public SynchronizeModelManager getModelManager() {
		return modelManager;
	}

	@Override
	public boolean validateViewer(StructuredViewer viewer) {
		return viewer instanceof AbstractTreeViewer;
	}

	@Override
	protected void initializeListeners(final StructuredViewer viewer) {
		super.initializeListeners(viewer);
		viewer.addSelectionChangedListener(event -> updateStatusLine(event.getStructuredSelection()));
	}

	/* private */ void updateStatusLine(IStructuredSelection selection) {
		IWorkbenchSite ws = getConfiguration().getSite().getWorkbenchSite();
		if (ws != null && ws instanceof IViewSite) {
			String msg = getStatusLineMessage(selection);
			((IViewSite)ws).getActionBars().getStatusLineManager().setMessage(msg);
		}
	}

	private String getStatusLineMessage(IStructuredSelection selection) {
		if (selection.size() == 1) {
			Object first = selection.getFirstElement();
			if (first instanceof SyncInfoModelElement) {
				SyncInfoModelElement node = (SyncInfoModelElement) first;
				IResource resource = node.getResource();
				if (resource == null) {
					return node.getName();
				} else {
					return resource.getFullPath().makeRelative().toString();
				}
			}
		}
		if (selection.size() > 1) {
			return selection.size() + TeamUIMessages.SynchronizeView_13;
		}
		return ""; //$NON-NLS-1$
	}

	/**
	 * Called to set the input to a viewer. The input to a viewer is always the model created
	 * by the model provider.
	 *
	 * @param modelProvider
	 */
	public final void setInput(final ISynchronizeModelProvider modelProvider) {
		final ISynchronizeModelElement modelRoot = modelProvider.getModelRoot();
		getActionGroup().modelChanged(modelRoot);
		modelRoot.addCompareInputChangeListener(source -> getActionGroup().modelChanged(modelRoot));
		final StructuredViewer viewer = getViewer();
		if (viewer != null) {
			viewer.setSorter(modelProvider.getViewerSorter());
			viewer.setInput(modelRoot);
			modelProvider.addPropertyChangeListener(event -> {
				if (event.getProperty() == ISynchronizeModelProvider.P_VIEWER_SORTER) {
					if (viewer != null && !viewer.getControl().isDisposed()) {
						viewer.getControl().getDisplay().syncExec(() -> {
							if (viewer != null && !viewer.getControl().isDisposed()) {
								ViewerSorter newSorter = modelProvider.getViewerSorter();
								ViewerSorter oldSorter = viewer.getSorter();
								if (newSorter == oldSorter) {
									viewer.refresh();
								} else {
									viewer.setSorter(newSorter);
								}
							}
						});
					}
				}
			});
		}
	}

	/**
	 * Install a viewer to be configured with this advisor. An advisor can only be installed with
	 * one viewer at a time. When this method completes the viewer is considered initialized and
	 * can be shown to the user.
	 * @param viewer the viewer being installed
	 */
	@Override
	public final void initializeViewer(final StructuredViewer viewer) {
		super.initializeViewer(viewer);

		final DragSourceListener listener = new DragSourceListener() {

			@Override
			public void dragStart(DragSourceEvent event) {
				final IStructuredSelection selection = viewer.getStructuredSelection();
				final Object [] array= selection.toArray();
				event.doit= Utils.getResources(array).length > 0;
			}

			@Override
			public void dragSetData(DragSourceEvent event) {

				if (ResourceTransfer.getInstance().isSupportedType(event.dataType)) {
					final IStructuredSelection selection= viewer.getStructuredSelection();
					final Object [] array= selection.toArray();
					event.data= Utils.getResources(array);
				}
			}

			@Override
			public void dragFinished(DragSourceEvent event) {}
		};

		final int ops = DND.DROP_COPY | DND.DROP_LINK;
		viewer.addDragSupport(ops, new Transfer[] { ResourceTransfer.getInstance() }, listener);

		viewer.setLabelProvider(getLabelProvider());
		viewer.setContentProvider(getContentProvider());
	}

	/**
	 * Returns the content provider for the viewer.
	 *
	 * @return the content provider for the viewer.
	 */
	protected IStructuredContentProvider getContentProvider() {
		return new BaseWorkbenchContentProvider();
	}

	/**
	 * Get the label provider that will be assigned to the viewer initialized
	 * by this configuration. Subclass may override but should either wrap the
	 * default one provided by this method or subclass <code>TeamSubscriberParticipantLabelProvider</code>.
	 * In the later case, the logical label provider should still be assigned
	 * to the subclass of <code>TeamSubscriberParticipantLabelProvider</code>.
	 * @return a label provider
	 * @see SynchronizeModelElementLabelProvider
	 */
	protected ILabelProvider getLabelProvider() {
		ILabelProvider provider = new SynchronizeModelElementLabelProvider();
		ILabelDecorator[] decorators = (ILabelDecorator[])getConfiguration().getProperty(ISynchronizePageConfiguration.P_LABEL_DECORATORS);
		if (decorators == null) {
			return provider;
		}
		return new DecoratingColorLabelProvider(provider, decorators);
	}

	@Override
	public void dispose() {
		if (statusLine != null) {
			statusLine.dispose();
		}
		super.dispose();
	}

	@Override
	protected void initializeStatusLine(IActionBars actionBars) {
		statusLine = new SyncInfoSetStatusLineContributionGroup(
				getConfiguration().getSite().getShell(),
				getConfiguration());
		IStatusLineManager statusLineMgr = actionBars.getStatusLineManager();
		if (statusLineMgr != null && statusLine != null) {
			statusLine.fillActionBars(actionBars);
		}
	}
}
