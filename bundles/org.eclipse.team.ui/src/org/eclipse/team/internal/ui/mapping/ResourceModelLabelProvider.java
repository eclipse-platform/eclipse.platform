/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.mapping;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.team.core.diff.*;
import org.eclipse.team.core.mapping.IResourceDiffTree;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.internal.ui.synchronize.ImageManager;
import org.eclipse.team.ui.mapping.ITeamContentProviderManager;
import org.eclipse.team.ui.mapping.SynchronizationLabelProvider;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;

/**
 * Resource label provider that can decorate using sync state.
 */
public class ResourceModelLabelProvider extends
		SynchronizationLabelProvider implements IFontProvider, IResourceChangeListener, ITreePathLabelProvider {

	public static final FastDiffFilter CONFLICT_FILTER = new FastDiffFilter() {
		@Override
		public boolean select(IDiff diff) {
			if (diff instanceof IThreeWayDiff) {
				IThreeWayDiff twd = (IThreeWayDiff) diff;
				return twd.getDirection() == IThreeWayDiff.CONFLICTING;
			}
			return false;
		}
	};

	private ILabelProvider provider;
	private ResourceModelContentProvider contentProvider;
	private ImageManager localImageManager;

	@Override
	public void init(ICommonContentExtensionSite site) {
		ITreeContentProvider aContentProvider = site.getExtension().getContentProvider();
		if (aContentProvider instanceof ResourceModelContentProvider) {
			contentProvider = (ResourceModelContentProvider) aContentProvider;
			ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
		}
		super.init(site);
	}

	@Override
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		if (localImageManager != null)
			localImageManager.dispose();
		if (provider != null)
			provider.dispose();
		super.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.mapping.SynchronizationOperationLabelProvider#getBaseLabelProvider()
	 */
	@Override
	protected ILabelProvider getDelegateLabelProvider() {
		if (provider == null)
			provider = new WorkbenchLabelProvider();
		return provider ;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.AbstractSynchronizationLabelProvider#getSyncDelta(java.lang.Object)
	 */
	@Override
	protected IDiff getDiff(Object elementOrPath) {
		IResource resource = getResource(elementOrPath);
		IResourceDiffTree tree = getDiffTree(elementOrPath);
		if (tree != null && resource != null) {
			IDiff delta = tree.getDiff(resource.getFullPath());
			return delta;
		}
		return null;
	}

	private IResource getResource(Object elementOrPath) {
		Object element = internalGetElement(elementOrPath);
		if (element instanceof IResource) {
			return (IResource) element;
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.AbstractSynchronizeLabelProvider#isIncludeOverlays()
	 */
	@Override
	protected boolean isIncludeOverlays() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.AbstractSynchronizeLabelProvider#isBusy(java.lang.Object)
	 */
	@Override
	protected boolean isBusy(Object elementOrPath) {
		IResource resource = getResource(elementOrPath);
		IResourceDiffTree tree = getDiffTree(elementOrPath);
		if (tree != null && resource != null) {
			return tree.getProperty(resource.getFullPath(), IDiffTree.P_BUSY_HINT);
		}
		return super.isBusy(elementOrPath);
	}

	private TreePath internalGetPath(Object elementOrPath) {
		if (elementOrPath instanceof TreePath) {
			return (TreePath) elementOrPath;
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.AbstractSynchronizeLabelProvider#hasDecendantConflicts(java.lang.Object)
	 */
	@Override
	protected boolean hasDecendantConflicts(Object elementOrPath) {
		IResource resource = getResource(elementOrPath);
		IResourceDiffTree tree = getDiffTree(elementOrPath);
		if (tree != null && resource != null) {
			int depth = getTraversalCalculator().getLayoutDepth(resource, internalGetPath(elementOrPath));
			if (depth == IResource.DEPTH_INFINITE || resource.getType() == IResource.FILE)
				return tree.getProperty(resource.getFullPath(), IDiffTree.P_HAS_DESCENDANT_CONFLICTS);
			return tree.hasMatchingDiffs(getTraversalCalculator().getTraversals(resource, internalGetPath(elementOrPath)), CONFLICT_FILTER);
		}
		return super.hasDecendantConflicts(elementOrPath);
	}

	protected IResourceDiffTree getDiffTree(Object elementOrPath) {
		ISynchronizationContext context = getContext();
		if (context != null)
			return context.getDiffTree();
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
	 */
	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		String[] markerTypes = new String[] {IMarker.PROBLEM};
		final Set handledResources = new HashSet();

		// Accumulate all distinct resources that have had problem marker
		// changes
		for (int idx = 0; idx < markerTypes.length; idx++) {
			IMarkerDelta[] markerDeltas = event.findMarkerDeltas(markerTypes[idx], true);
				for (int i = 0; i < markerDeltas.length; i++) {
					IMarkerDelta delta = markerDeltas[i];
					IResource resource = delta.getResource();
					while (resource != null && resource.getType() != IResource.ROOT && !handledResources.contains(resource)) {
						handledResources.add(resource);
						resource = resource.getParent();
					}
				}
			}

		if (!handledResources.isEmpty()) {
			final IResource[] resources = (IResource[]) handledResources.toArray(new IResource[handledResources.size()]);
		    updateLabels(resources);
		}
	}

	protected void updateLabels(final Object[] resources) {
		Utils.asyncExec(new Runnable() {
			@Override
			public void run() {
				contentProvider.getStructuredViewer().update(
						resources, null);
			}
		}, contentProvider.getStructuredViewer());
	}

	@Override
	protected String getDelegateText(Object elementOrPath) {
		if (getConfiguration() != null) {
			String label = getTraversalCalculator().getLabel(elementOrPath);
			if (label != null)
				return label;
		}
		return super.getDelegateText(internalGetElement(elementOrPath));
	}

	@Override
	protected Image getDelegateImage(Object elementOrPath) {
		if (getConfiguration() != null && getTraversalCalculator().isCompressedFolder(elementOrPath)) {
			return getImageManager().getImage(TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_COMPRESSED_FOLDER));
		}
		return super.getDelegateImage(internalGetElement(elementOrPath));
	}

	private Object internalGetElement(Object elementOrPath) {
		if (elementOrPath instanceof TreePath) {
			TreePath tp = (TreePath) elementOrPath;
			return tp.getLastSegment();
		}
		return elementOrPath;
	}

	protected ResourceModelTraversalCalculator getTraversalCalculator() {
		return ResourceModelTraversalCalculator.getTraversalCalculator(getConfiguration());
	}

	private ISynchronizePageConfiguration getConfiguration() {
		return (ISynchronizePageConfiguration)getExtensionSite().getExtensionStateModel().getProperty(ITeamContentProviderManager.P_SYNCHRONIZATION_PAGE_CONFIGURATION);
	}

	@Override
	public void updateLabel(ViewerLabel label, TreePath elementPath) {
		label.setImage(getImage(elementPath));
		label.setText(getText(elementPath));
		Font f = getFont(elementPath);
		if (f != null)
			label.setFont(f);
	}

	protected ImageManager getImageManager() {
		ISynchronizationContext context = getContext();
		if (context != null) {
			return ImageManager.getImageManager(context, getConfiguration());
		}
		if (localImageManager == null) {
			localImageManager = new ImageManager();
		}
		return localImageManager;
	}

}
