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
package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffTreeViewer;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.core.resources.IEncodedStorage;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.TeamUIPlugin;

/**
 * A diff node used to display the synchronization state for resources described by
 * existing {@link SyncInfo} objects. The synchronization state for a node can
 * change after it has been created. Since it implements the <code>ITypedElement</code>
 * and <code>ICompareInput</code> interfaces it can be used directly to
 * display the compare result in a <code>DiffTreeViewer</code> and as the
 * input to any other compare/merge viewer.
 * <p>
 * Clients typically use this class as is, but may subclass if required.
 * </p>
 * @see DiffTreeViewer
 * @see Differencer
 */
public class SyncInfoModelElement extends SynchronizeModelElement {

	private ITypedElement ancestor;
	private SyncInfo info;

	/**
	 * Construct a <code>SyncInfoModelElement</code> for the given resource.
	 *
	 * @param parent
	 * @param info
	 */
	public SyncInfoModelElement(IDiffContainer parent, SyncInfo info) {
		super(parent);

		Assert.isNotNull(info);
		this.info = info;
		// update state
		setKind(info.getKind());
		// local
		setLeft(createLocalTypeElement(info));
		// remote
		setRight(createRemoteTypeElement(info));
		// base
		setAncestor(createBaseTypeElement(info));

		fireChange();
	}

	/**
	 * Update this element with a changed sync info. The remote and base handles have to be updated
	 * with the new handles in the sync info.
	 *
	 * @param info the new sync info
	 */
	public void update(SyncInfo info) {
		this.info = info;
		// update state
		setKind(info.getKind());

		// Remote
		RemoteResourceTypedElement rightEl = (RemoteResourceTypedElement)getRight();
		IResourceVariant remote = info.getRemote();
		if(rightEl == null && remote != null) {
			setRight(createRemoteTypeElement(info));
		} else if(rightEl != null) {
			if(remote == null) {
				setRight(null);
			} else {
				setRight(createRemoteTypeElement(info));
			}
		}
		// Base
		RemoteResourceTypedElement ancestorEl = (RemoteResourceTypedElement)getAncestor();
		IResourceVariant base = info.getBase();
		if(ancestorEl == null && base != null) {
			setAncestor(createBaseTypeElement(info));
		} else if(ancestorEl != null) {
			if(base == null) {
				setAncestor(null);
			} else {
				setAncestor(createBaseTypeElement(info));
			}
		}

		fireChange();
	}

	@Override
	public int getKind() {
		SyncInfo info = getSyncInfo();
		if (info != null) {
			return info.getKind();
		} else {
			return SyncInfo.IN_SYNC;
		}
	}

	/**
	 * We have to track the base because <code>DiffNode</code> doesn't provide a
	 * setter. See:
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=52261
	 */
	@Override
	public void setAncestor(ITypedElement ancestor) {
		this.ancestor = ancestor;
	}

	@Override
	public ITypedElement getAncestor() {
		return this.ancestor;
	}

	@Override
	public String getName() {
		IResource resource = getResource();
		if(resource != null) {
			return resource.getName();
		} else {
			return super.getName();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if(adapter == SyncInfo.class) {
			return (T) getSyncInfo();
		}
		return super.getAdapter(adapter);
	}

	/**
	 * Helper method that returns the resource associated with this node. A node is not
	 * required to have an associated local resource.
	 * @return the resource associated with this node or <code>null</code> if the local
	 * contributor is not a resource.
	 */
	@Override
	public IResource getResource() {
		return info.getLocal();
	}

	@Override
	public String toString() {
		return getResource().getFullPath().toString();
	}

	/**
	 * Cache the contents for the base and remote.
	 * @param monitor
	 */
	public void cacheContents(IProgressMonitor monitor) throws TeamException {
		ITypedElement base = getAncestor();
		ITypedElement remote = getRight();
		int work = Math.min((remote== null ? 0 : 50) + (base == null ? 0 : 50), 10);
		monitor.beginTask(null, work);
		try {
			if (base != null && base instanceof RemoteResourceTypedElement) {
				((RemoteResourceTypedElement)base).cacheContents(Policy.subMonitorFor(monitor, 50));
			}
			if (remote != null && remote instanceof RemoteResourceTypedElement) {
				((RemoteResourceTypedElement)remote).cacheContents(Policy.subMonitorFor(monitor, 50));
			}
		} catch (CoreException e) {
			throw TeamException.asTeamException(e);
		} finally {
			monitor.done();
		}
	}

	public SyncInfo getSyncInfo() {
		return info;
	}

	/**
	 * Create an ITypedElement for the given local resource. The returned ITypedElement
	 * will prevent editing of outgoing deletions.
	 */
	private static ITypedElement createTypeElement(final IResource resource, final int kind) {
		if(resource != null) {
			return new LocalResourceTypedElement(resource);
		}
		return null;
	}

	/**
	 * Create an ITypedElement for the given remote resource. The contents for the remote resource
	 * will be retrieved from the given IStorage which is a local cache used to buffer the remote contents
	 */
	protected static ITypedElement createTypeElement(IResourceVariant remoteResource, String encoding) {
		return new RemoteResourceTypedElement(remoteResource,encoding);
	}

	protected static ITypedElement createRemoteTypeElement(SyncInfo info) {
		if(info != null && info.getRemote() != null) {
			return createTypeElement(info.getRemote(), getEncoding(info.getLocal()));
		}
		return null;
	}

	private static String getEncoding(IResource local) {
		if (local instanceof IEncodedStorage) {
			IEncodedStorage es = (IEncodedStorage) local;
			try {
				return es.getCharset();
			} catch (CoreException e) {
				TeamUIPlugin.log(e);
			}
		}
		return null;
	}

	protected static ITypedElement createLocalTypeElement(SyncInfo info) {
		if(info != null && info.getLocal() != null) {
			return createTypeElement(info.getLocal(), info.getKind());
		}
		return null;
	}

	protected static ITypedElement createBaseTypeElement(SyncInfo info) {
		if(info != null && info.getBase() != null) {
			return createTypeElement(info.getBase(), getEncoding(info.getLocal()));
		}
		return null;
	}
}
