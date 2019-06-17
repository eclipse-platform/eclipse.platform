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
package org.eclipse.team.tests.ccvs.core.subscriber;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import junit.framework.AssertionFailedError;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.*;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.tests.ccvs.core.EclipseTest;
import org.eclipse.team.tests.ccvs.ui.ModelParticipantSyncInfoSource;

/**
 * Provides test methods common to CVS sync subscribers
 */
public abstract class CVSSyncSubscriberTest extends EclipseTest {

	private ISubscriberChangeListener listener;
	private List<ISubscriberChangeEvent> accumulatedTeamDeltas = new ArrayList<>();
	private static SyncInfoSource source = new ModelParticipantSyncInfoSource();

	public CVSSyncSubscriberTest() {
		super();
	}
	
	public CVSSyncSubscriberTest(String name) {
		super(name);
	}

	public static void setSyncSource(SyncInfoSource newSource) {
		source = newSource;
	}
	
	protected Subscriber getWorkspaceSubscriber() throws TeamException {
		return getSyncInfoSource().createWorkspaceSubscriber();
	}
	
	public SyncInfoSource getSyncInfoSource() {
		assertNotNull(source);
		return source;
	}
	
	protected void refresh(Subscriber subscriber, IResource resource) throws TeamException {
		getSyncInfoSource().refresh(subscriber, resource);
	}
	
	/*
	 * Assert that the specified resources in the subscriber have the specified sync kind
	 * Ignore conflict types if they are not specified in the assert statement
	 */
	protected void assertSyncEquals(String message, Subscriber subscriber, IContainer root, String[] resourcePaths, boolean refresh, int[] syncKinds) throws CoreException, TeamException {
		assertTrue(resourcePaths.length == syncKinds.length);
		if (refresh) refresh(subscriber, root);
		IResource[] resources = getResources(root, resourcePaths);
		for (int i=0;i<resources.length;i++) {
			assertSyncEquals(message, subscriber, resources[i], syncKinds[i]);
		}
		
	}
	
	protected void assertSyncEquals(String message, Subscriber subscriber, IResource resource, int syncKind) throws CoreException {
		getSyncInfoSource().assertSyncEquals(message, subscriber, resource, syncKind);
	}
	
	protected void assertDiffKindEquals(String message, Subscriber subscriber, IContainer root, String[] resourcePaths, boolean refresh, int[] diffKinds) throws CoreException, TeamException {
		assertTrue(resourcePaths.length == diffKinds.length);
		if (refresh) refresh(subscriber, root);
		IResource[] resources = getResources(root, resourcePaths);
		for (int i=0;i<resources.length;i++) {
			getSyncInfoSource().assertDiffKindEquals(message, subscriber, resources[i], diffKinds[i]);
		}
	}

	protected void assertSyncChangesMatch(ISubscriberChangeEvent[] changes, IResource[] resources) {
		// First, ensure that all the resources appear in the delta
		for (IResource resource : resources) {
			boolean found = false;
			for (ISubscriberChangeEvent delta : changes) {
				if (delta.getResource().equals(resource)) {
					found = true;
					break;
				}
			}
			assertTrue("No change reported for " + resource, found);
		}
		// TODO: We'll worry about extra deltas later
//		// Next, ensure there are no extra deltas
//		List changedResources = new ArrayList(resources.length);
//		changedResources.addAll(Arrays.asList(resources));
//		for (int i = 0; i < changes.length; i++) {
//			TeamDelta change = changes[i];
//			IResource resource = change.getResource();
//			assertTrue("Unanticipated change reported for " + resource, changedResources.contains(resource));
//		}
	}
	
	/* 
	 * Assert that the named resources have no local resource or sync info
	 */
	protected void assertDeleted(String message, IContainer root, String[] resourcePaths) {
		IResource[] resources = getResources(root, resourcePaths);
		for (IResource resource : resources) {
			try {
				if (!resource.exists()) {
					break;
				}
			} catch (AssertionFailedError e) {
				break;
			}
			assertTrue(message + ": resource " + resource + " still exists in some form", false);
		}
	}
	
	public static class ResourceCondition {
		@SuppressWarnings("unused")
		public boolean matches(IResource resource) throws CoreException, TeamException {
			return true;
		}
	}
	
	protected IResource[] collect(IResource[] resources, final ResourceCondition condition, int depth) throws CoreException, TeamException {
		final Set<IResource> affected = new HashSet<>();
		for (IResource resource : resources) {
			if (resource.exists() || resource.isPhantom()) {
				resource.accept(new IResourceVisitor() {
					@Override
					public boolean visit(IResource r) throws CoreException {
						try {
							if (condition.matches(r)) {
								affected.add(r);
							}
						} catch (TeamException e) {
							throw new CoreException(e.getStatus());
						}
						return true;
					}
				}, depth, true /* include phantoms */);
			} else {
				if (condition.matches(resource)) {
					affected.add(resource);
				}
			}
		}
		return affected.toArray(new IResource[affected.size()]);
	}
	
	protected IResource[] collectAncestors(IResource[] resources, ResourceCondition condition) throws CoreException, TeamException {
		Set<IResource> affected = new HashSet<>();
		for (IResource resource : resources) {
			while (resource.getType() != IResource.ROOT) {
				if (condition.matches(resource)) {
					affected.add(resource);
				} else {
					break;
				}
				resource = resource.getParent();
			}
		}
		return affected.toArray(new IResource[affected.size()]);
	}
	
	protected ISubscriberChangeEvent[] deregisterSubscriberListener(Subscriber subscriber) {
		subscriber.removeListener(listener);
		return accumulatedTeamDeltas.toArray(new SubscriberChangeEvent[accumulatedTeamDeltas.size()]);
	}

	protected ISubscriberChangeListener registerSubscriberListener(Subscriber subscriber) {
		listener = new ISubscriberChangeListener() {
			@Override
			public void subscriberResourceChanged(ISubscriberChangeEvent[] deltas) {
				accumulatedTeamDeltas.addAll(Arrays.asList(deltas));
			}
		};
		accumulatedTeamDeltas.clear();
		subscriber.addListener(listener);
		return listener;
	}

	protected void assertProjectRemoved(Subscriber subscriber, IProject project) {
		getSyncInfoSource().assertProjectRemoved(subscriber, project);
	}
	
	protected void markAsMerged(CVSSyncTreeSubscriber subscriber, IProject project, String[] resourcePaths) throws CoreException, TeamException, InvocationTargetException, InterruptedException {
		IResource[] resources = getResources(project, resourcePaths);
		getSyncInfoSource().markAsMerged(subscriber, resources);
	}
	
	protected void assertIsBinary(IFile local) throws CVSException {
		ICVSFile file = CVSWorkspaceRoot.getCVSFileFor(local);
		byte[] syncBytes = file.getSyncBytes();
		if (syncBytes != null) {
			assertTrue(ResourceSyncInfo.isBinary(syncBytes));
		}
	}
}
