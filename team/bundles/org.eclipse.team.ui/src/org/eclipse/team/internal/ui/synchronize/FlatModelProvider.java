/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.team.core.synchronize.ISyncInfoTreeChangeEvent;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.internal.ui.ITeamUIImages;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.TeamImages;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizePageActionGroup;

/**
 * Provides a flat layout
 */
public class FlatModelProvider extends SynchronizeModelProvider {

	public static class FlatModelProviderDescriptor implements ISynchronizeModelProviderDescriptor {
		public static final String ID = TeamUIPlugin.ID + ".modelprovider_flat"; //$NON-NLS-1$
		@Override
		public String getId() {
			return ID;
		}
		@Override
		public String getName() {
			return TeamUIMessages.FlatModelProvider_0;
		}
		@Override
		public ImageDescriptor getImageDescriptor() {
			return TeamImages.getImageDescriptor(ITeamUIImages.IMG_FLAT);
		}
	}
	private static final FlatModelProviderDescriptor flatDescriptor = new FlatModelProviderDescriptor();

	private static final String P_LAST_RESOURCESORT = TeamUIPlugin.ID + ".P_LAST_RESOURCE_SORT"; //$NON-NLS-1$

	private int sortCriteria = FlatSorter.PATH;

	/* *****************************************************************************
	 * Model element for the resources in this layout. They are displayed with filename and path
	 * onto the same line.
	 */
	public static class FullPathSyncInfoElement extends SyncInfoModelElement {
		public FullPathSyncInfoElement(IDiffContainer parent, SyncInfo info) {
			super(parent, info);
		}
		@Override
		public String getName() {
			IResource resource = getResource();
			return resource.getName() + " - " + resource.getFullPath().toString(); //$NON-NLS-1$
		}
	}

	/**
	 * Sorter that sorts flat path elements using the criteria specified
	 * when the sorter is created
	 */
	public class FlatSorter extends ViewerSorter {

		private int resourceCriteria;

		// Resource sorting options
		public final static int NAME = 1;
		public final static int PATH = 2;
		public final static int PARENT_NAME = 3;

		public FlatSorter(int resourceCriteria) {
			this.resourceCriteria = resourceCriteria;
		}

		protected int classComparison(Object element) {
			if (element instanceof FullPathSyncInfoElement) {
				return 0;
			}
			return 1;
		}

		protected int compareClass(Object element1, Object element2) {
			return classComparison(element1) - classComparison(element2);
		}

		protected int compareNames(String s1, String s2) {
			return collator.compare(s1, s2);
		}

		@Override
		public int compare(Viewer viewer, Object o1, Object o2) {

			if (o1 instanceof FullPathSyncInfoElement && o2 instanceof FullPathSyncInfoElement) {
				IResource r1 = ((FullPathSyncInfoElement)o1).getResource();
				IResource r2 = ((FullPathSyncInfoElement)o2).getResource();
				if(resourceCriteria == NAME)
					return compareNames(r1.getName(), r2.getName());
				else if(resourceCriteria == PATH)
					return compareNames(r1.getFullPath().toString(), r2.getFullPath().toString());
				else if(resourceCriteria == PARENT_NAME)
					return compareNames(r1.getParent().getName(), r2.getParent().getName());
				else return 0;
			} else if (o1 instanceof ISynchronizeModelElement)
				return 1;
			else if (o2 instanceof ISynchronizeModelElement)
				return -1;

			return 0;
		}

		public int getResourceCriteria() {
			return resourceCriteria;
		}
	}

	/* *****************************************************************************
	 * Action that allows changing the model providers sort order.
	 */
	private class ToggleSortOrderAction extends Action {
		private int criteria;
		protected ToggleSortOrderAction(String name, int criteria) {
			super(name, IAction.AS_RADIO_BUTTON);
			this.criteria = criteria;
			update();
		}

		@Override
		public void run() {
			if (isChecked() && sortCriteria != criteria) {
				sortCriteria = criteria;
				String key = getSettingsKey();
				IDialogSettings pageSettings = getConfiguration().getSite().getPageSettings();
				if(pageSettings != null) {
					pageSettings.put(key, criteria);
				}
				update();
				FlatModelProvider.this.firePropertyChange(P_VIEWER_SORTER, null, null);
			}
		}

		public void update() {
			setChecked(sortCriteria == criteria);
		}

		protected String getSettingsKey() {
			return P_LAST_RESOURCESORT;
		}
	}

	/* *****************************************************************************
	 * Action group for this layout. It is added and removed for this layout only.
	 */
	public class FlatActionGroup extends SynchronizePageActionGroup {
		private MenuManager sortByResource;
		@Override
		public void initialize(ISynchronizePageConfiguration configuration) {
			super.initialize(configuration);
			sortByResource = new MenuManager(TeamUIMessages.FlatModelProvider_6);

			appendToGroup(
					ISynchronizePageConfiguration.P_CONTEXT_MENU,
					ISynchronizePageConfiguration.SORT_GROUP,
					sortByResource);

			// Ensure that the sort criteria of the provider is properly initialized
			FlatModelProvider.this.initialize(configuration);

			sortByResource.add( new ToggleSortOrderAction(TeamUIMessages.FlatModelProvider_8, FlatSorter.PATH));
			sortByResource.add(new ToggleSortOrderAction(TeamUIMessages.FlatModelProvider_7, FlatSorter.NAME));
			sortByResource.add(new ToggleSortOrderAction(TeamUIMessages.FlatModelProvider_9, FlatSorter.PARENT_NAME));
		}

		@Override
		public void dispose() {
			sortByResource.dispose();
			sortByResource.removeAll();
			super.dispose();
		}
	}

	public FlatModelProvider(ISynchronizePageConfiguration configuration, SyncInfoSet set) {
		super(configuration, set);
		initialize(configuration);
	}

	public FlatModelProvider(AbstractSynchronizeModelProvider parentProvider, ISynchronizeModelElement modelRoot, ISynchronizePageConfiguration configuration, SyncInfoSet set) {
		super(parentProvider, modelRoot, configuration, set);
		initialize(configuration);
	}

	private void initialize(ISynchronizePageConfiguration configuration) {
		try {
			IDialogSettings pageSettings = getConfiguration().getSite().getPageSettings();
			if(pageSettings != null) {
				sortCriteria = pageSettings.getInt(P_LAST_RESOURCESORT);
			}
		} catch(NumberFormatException e) {
			// ignore and use the defaults.
		}
		switch (sortCriteria) {
		case FlatSorter.PATH:
		case FlatSorter.NAME:
		case FlatSorter.PARENT_NAME:
			break;
		default:
			sortCriteria = FlatSorter.PATH;
			break;
		}
	}

	@Override
	protected SynchronizePageActionGroup createActionGroup() {
		return new FlatActionGroup();
	}

	@Override
	public ViewerSorter getViewerSorter() {
		return new FlatSorter(sortCriteria);
	}

	@Override
	protected IDiffElement[] buildModelObjects(ISynchronizeModelElement node) {
		if (node == getModelRoot());
		SyncInfo[] infos = getSyncInfoSet().getSyncInfos();
		List<IDiffElement> result = new ArrayList<>();
		for (SyncInfo info : infos) {
			result.add(createModelObject(node, info));
		}
		return result.toArray(new IDiffElement[result.size()]);
	}

	@Override
	protected void handleResourceAdditions(ISyncInfoTreeChangeEvent event) {
		addResources(event.getAddedResources());
	}

	@Override
	protected void handleResourceRemovals(ISyncInfoTreeChangeEvent event) {
		IResource[] resources = event.getRemovedResources();
		removeFromViewer(resources);
	}

	@Override
	public ISynchronizeModelProviderDescriptor getDescriptor() {
		return flatDescriptor;
	}

	@Override
	protected void addResource(SyncInfo info) {
		// Add the node to the root
		ISynchronizeModelElement node = getModelObject(info.getLocal());
		if (node != null) {
			// Somehow the node exists. Remove it and read it to ensure
			// what is shown matches the contents of the sync set
			removeFromViewer(info.getLocal());
		}
		createModelObject(getModelRoot(), info);
	}

	@Override
	protected ISynchronizeModelElement createModelObject(ISynchronizeModelElement parent, SyncInfo info) {
		SynchronizeModelElement newNode = new FullPathSyncInfoElement(parent, info);
		addToViewer(newNode);
		return newNode;
	}
}
