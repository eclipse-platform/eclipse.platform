/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.core;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.*;
import org.eclipse.team.core.variants.IResourceVariantTree;
import org.eclipse.team.core.variants.SessionResourceVariantByteStore;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.CVSResourceVariantTree;
import org.eclipse.team.internal.ccvs.core.syncinfo.MultiTagResourceVariantTree;

/**
 * This subscriber is used when comparing the local workspace with its
 * corresponding remote.
 */
public class CVSCompareSubscriber extends CVSSyncTreeSubscriber implements ISubscriberChangeListener {

	public static final String ID = "org.eclipse.team.cvs.ui.compare-participant"; //$NON-NLS-1$
	public static final String ID_MODAL = "org.eclipse.team.cvs.ui.compare-participant-modal"; //$NON-NLS-1$
	
	public static final String QUALIFIED_NAME = CVSProviderPlugin.ID + ".compare"; //$NON-NLS-1$
	private static final String UNIQUE_ID_PREFIX = "compare-"; //$NON-NLS-1$
	
	private IResource[] resources;
	private CVSResourceVariantTree tree;
	
	public CVSCompareSubscriber(IResource[] resources, CVSTag tag) {
		super(getUniqueId(), NLS.bind(CVSMessages.CVSCompareSubscriber_2, new String[] { tag.getName() })); // 
		this.resources = resources;
		tree = new CVSResourceVariantTree(new SessionResourceVariantByteStore(), tag, getCacheFileContentsHint());
		initialize();
	}

	public CVSCompareSubscriber(IResource[] resources, CVSTag[] tags, String name) {
		super(getUniqueId(), NLS.bind(CVSMessages.CVSCompareSubscriber_2, new String[] { name })); // 
		resetRoots(resources, tags);
		initialize();
	}

	/**
	 * @param resources
	 * @param tags
	 */
	public void resetRoots(IResource[] resources, CVSTag[] tags) {
		if (this.resources != null) {
			List<ISubscriberChangeEvent> removed = new ArrayList<>();
			for (IResource resource : this.resources) {
				removed.add(new SubscriberChangeEvent(this, ISubscriberChangeEvent.ROOT_REMOVED, resource));
			}
			this.resources = new IResource[0];
			fireTeamResourceChange(removed.toArray(new ISubscriberChangeEvent[removed.size()]));
			if (tree != null) {
				tree.dispose();
				tree = null;
			}
		}
		this.resources = resources;
		MultiTagResourceVariantTree multiTree = new MultiTagResourceVariantTree(new SessionResourceVariantByteStore(), getCacheFileContentsHint());
		for (int i = 0; i < tags.length; i++) {
			multiTree.addResource(resources[i], tags[i]);
		}
		tree = multiTree;
	}

	private void initialize() {
		CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber().addListener(this);
	}

	public void dispose() {	
		CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber().removeListener(this);	
		tree.dispose();	
	}
	
	private static QualifiedName getUniqueId() {
		String uniqueId = Long.toString(System.currentTimeMillis());
		return new QualifiedName(QUALIFIED_NAME, UNIQUE_ID_PREFIX + uniqueId); 
	}

	@Override
	protected IResourceVariantTree getBaseTree() {
		// No base cache needed since it's a two way compare
		return null;
	}

	@Override
	protected IResourceVariantTree getRemoteTree() {
		return tree;
	}
	
	@Override
	public boolean isThreeWay() {
		return false;
	}

	@Override
	public IResource[] roots() {
		return resources;
	}

	@Override
	public void subscriberResourceChanged(ISubscriberChangeEvent[] deltas) {
		List<ISubscriberChangeEvent> outgoingDeltas = new ArrayList<>(deltas.length);
		for (ISubscriberChangeEvent delta : deltas) {
			if ((delta.getFlags() & ISubscriberChangeEvent.ROOT_REMOVED) != 0) {
				IResource resource = delta.getResource();
				outgoingDeltas.addAll(Arrays.asList(handleRemovedRoot(resource)));
			} else if ((delta.getFlags() & ISubscriberChangeEvent.SYNC_CHANGED) != 0) {
				IResource resource = delta.getResource();
				try {
					if (isSupervised(resource)) {
						outgoingDeltas.add(new SubscriberChangeEvent(this, delta.getFlags(), resource));
					}
				} catch (TeamException e) {
					// Log and ignore
					CVSProviderPlugin.log(e);
				}
			}
		}
		
		fireTeamResourceChange(outgoingDeltas.toArray(new SubscriberChangeEvent[outgoingDeltas.size()]));
	}

	private SubscriberChangeEvent[] handleRemovedRoot(IResource removedRoot) {
		// Determine if any of the roots of the compare are affected
		List<IResource> removals = new ArrayList<>(resources.length);
		for (IResource root : resources) {
			if (removedRoot.getFullPath().isPrefixOf(root.getFullPath())) {
				// The root is no longer managed by CVS
				removals.add(root);
				try {
					tree.flushVariants(root, IResource.DEPTH_INFINITE);
				} catch (TeamException e) {
					CVSProviderPlugin.log(e);
				}
			}
		}
		if (removals.isEmpty()) {
			return new SubscriberChangeEvent[0];
		}
		
		// Adjust the roots of the subscriber
		List<IResource> newRoots = new ArrayList<>(resources.length);
		newRoots.addAll(Arrays.asList(resources));
		newRoots.removeAll(removals);
		resources = newRoots.toArray(new IResource[newRoots.size()]);
		
		// Create the deltas for the removals
		SubscriberChangeEvent[] deltas = new SubscriberChangeEvent[removals.size()];
		for (int i = 0; i < deltas.length; i++) {
			deltas[i] = new SubscriberChangeEvent(this, ISubscriberChangeEvent.ROOT_REMOVED, removals.get(i));
		}
		return deltas;
	}
	
	@Override
	public boolean isSupervised(IResource resource) throws TeamException {
		if (super.isSupervised(resource)) {
			if (!resource.exists() && !getRemoteTree().hasResourceVariant(resource)) {
				// Exclude conflicting deletions
				return false;
			}
			if (this.resources != null) {
				for (IResource root : resources) {
					if (root.getFullPath().isPrefixOf(resource.getFullPath())) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	@Override
	protected boolean getCacheFileContentsHint() {
		return true;
	}
	
	public CVSTag getTag() {
		return tree.getTag(ResourcesPlugin.getWorkspace().getRoot());
	}
		
	@Override
	public boolean equals(Object other) {
		if(this == other) return true;
		if(! (other instanceof CVSCompareSubscriber)) return false;
		CVSCompareSubscriber s = (CVSCompareSubscriber)other;
		CVSResourceVariantTree tree1 = (CVSResourceVariantTree)getRemoteTree();
		CVSResourceVariantTree tree2 = (CVSResourceVariantTree)s.getRemoteTree();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		CVSTag tag1 = tree1.getTag(root);
		CVSTag tag2 = tree2.getTag(root);
		if (tag1 == null || tag2 == null) return false;
		return tag1.equals(tag2) && rootsEqual(s);		
	}
	
	/**
	 * Prime the remote tree with the sync info from the local workspace.
	 * This is done to ensure that we don't get a huge number of outgoing
	 * changes before the first refresh.
	 *
	 */
	public void primeRemoteTree() throws CVSException {
		for (IResource resource : resources) {
			ICVSResource cvsResource = CVSWorkspaceRoot.getCVSResourceFor(resource);
			cvsResource.accept(new ICVSResourceVisitor() {
				public void visitFile(ICVSFile file) throws CVSException {
					byte[] bytes = file.getSyncBytes();
					if (bytes != null) {
						try {
							tree.getByteStore().setBytes(file.getIResource(), bytes);
						} catch (TeamException e) {
							throw CVSException.wrapException(e);
						}
					}
				}
				public void visitFolder(ICVSFolder folder) throws CVSException {
					// No need to copy sync info for folders since
					// CVS resource variant tree will get missing
					// folder info from the local resources
					folder.acceptChildren(this);
				}
			});
		}
	}

	/**
	 * Return the tag associated with the given root resource
	 * or <code>null</code> if there is only a single tag
	 * for the subscriber.
	 * @param root the root resource
	 * @return the tag associated with the given root resource
	 */
	public CVSTag getTag(IResource root) {
		return tree.getTag(root);
	}

	/**
	 * Return <code>true</code> if the tag against which each
	 * root is compared may differ. 
	 * @return whether the tag on each root may differ.
	 */
	public boolean isMultipleTagComparison() {
		return getTag() == null;
	}

}
