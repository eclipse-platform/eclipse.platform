/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize.patch;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.internal.patch.HunkDiffNode;
import org.eclipse.compare.internal.patch.PatchDiffNode;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.mapping.SynchronizationResourceMappingContext;
import org.eclipse.team.ui.mapping.SynchronizationContentProvider;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;

public class PatchSyncContentProvider extends SynchronizationContentProvider {

	private BaseWorkbenchContentProvider delegate;

	@Override
	public void init(ICommonContentExtensionSite site) {
		super.init(site);
		delegate = new BaseWorkbenchContentProvider();
	}

	@Override
	public void dispose() {
		super.dispose();
		if (delegate != null)
			delegate.dispose();
	}

	@Override
	protected ITreeContentProvider getDelegateContentProvider() {
		return delegate;
	}

	@Override
	protected String getModelProviderId() {
		return PatchModelProvider.ID;
	}

	@Override
	protected Object getModelRoot() {
		if (getContext() instanceof ApplyPatchSubscriberMergeContext) {
			ApplyPatchSubscriberMergeContext context = (ApplyPatchSubscriberMergeContext) getContext();
			return PatchModelProvider.getPatchWorkspace(context.getSubscriber());
		}
		// TODO: assertion?
		return null;
	}

	/*
	 * Copied from
	 * org.eclipse.team.examples.model.ui.mapping.ModelSyncContentProvider
	 * .getTraversals(ISynchronizationContext, Object)
	 */
	@Override
	protected ResourceTraversal[] getTraversals(
			ISynchronizationContext context, Object object) {
		if (object instanceof IDiffElement) {
			ResourceMapping mapping = PatchModelProvider.getResourceMapping((IDiffElement) object);
			ResourceMappingContext rmc = new SynchronizationResourceMappingContext(
					context);
			try {
				return mapping.getTraversals(rmc, new NullProgressMonitor());
			} catch (CoreException e) {
				TeamUIPlugin.log(e);
			}
		}
		return new ResourceTraversal[0];
	}

	@Override
	protected boolean isInScope(ISynchronizationScope scope, Object parent,
			Object element) {
		if (element instanceof PatchDiffNode) {
			final IResource resource = PatchModelProvider
					.getResource((PatchDiffNode) element);
			if (resource == null)
				return false;
			if (scope.contains(resource))
				return true;
		}
		return false;
	}

	@Override
	public boolean hasChildren(final Object element) {
		if (element instanceof HunkDiffNode)
			return false;
		return super.hasChildren(element);
	}

	@Override
	public Object[] getChildren(Object parent) {
		Object[] children = super.getChildren(parent);
		List result = new ArrayList();
		for (int i = 0; i < children.length; i++) {
			if (children[i] instanceof PatchDiffNode) {
				PatchDiffNode node = (PatchDiffNode) children[i];
				if (node.isEnabled())
					result.add(node); // hide disabled nodes
			}
		}
		return result.toArray();
	}

}
