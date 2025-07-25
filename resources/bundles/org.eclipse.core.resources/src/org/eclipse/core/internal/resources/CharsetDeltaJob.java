/*******************************************************************************
 * Copyright (c) 2004, 2015 IBM Corporation and others.
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
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 473427
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import java.util.LinkedList;
import java.util.Queue;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.internal.utils.Policy;
import org.eclipse.core.internal.utils.WrappedRuntimeException;
import org.eclipse.core.internal.watson.ElementTreeIterator;
import org.eclipse.core.internal.watson.IElementContentVisitor;
import org.eclipse.core.internal.watson.IPathRequestor;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.core.runtime.content.IContentTypeManager.ContentTypeChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.framework.Bundle;

/**
 * Detects changes to content types/project preferences and
 * broadcasts any corresponding encoding changes as resource deltas.
 */

public class CharsetDeltaJob extends Job implements IContentTypeManager.IContentTypeChangeListener {

	// this is copied in the runtime tests - if changed here, has to be changed there too
	public final static String FAMILY_CHARSET_DELTA = ResourcesPlugin.PI_RESOURCES + "charsetJobFamily"; //$NON-NLS-1$

	interface ICharsetListenerFilter {

		/**
		 * Returns the path for the node in the tree we are interested in. Returns <code>null</code>
		 * if the visitor no longer wants to visit anything.
		 */
		IPath getRoot();

		IProject getProject();

		/**
		 * Returns whether the corresponding resource is affected by this change.
		 */
		boolean isAffected(ResourceInfo info, IPathRequestor requestor);
	}

	private final ThreadLocal<Boolean> disabled = new ThreadLocal<>();

	private final Bundle systemBundle = Platform.getBundle("org.eclipse.osgi"); //$NON-NLS-1$
	private final Queue<ICharsetListenerFilter> work = new LinkedList<>();

	Workspace workspace;

	private static final int CHARSET_DELTA_DELAY = 500;

	public CharsetDeltaJob(Workspace workspace) {
		super(Messages.resources_charsetBroadcasting);
		this.workspace = workspace;
		setRule(workspace.getRoot()); // make sure workspace.prepareOperation() does not block
	}

	private void addToQueue(ICharsetListenerFilter filter) {
		synchronized (work) {
			work.add(filter);
		}
		schedule(CHARSET_DELTA_DELAY);
	}

	@Override
	public boolean belongsTo(Object family) {
		return FAMILY_CHARSET_DELTA.equals(family);
	}

	public void charsetPreferencesChanged(final IProject project) {
		// avoid reacting to changes made by ourselves
		if (isDisabled()) {
			return;
		}
		ResourceInfo projectInfo = ((Project) project).getResourceInfo(false, false);
		//nothing to do if project has already been deleted
		if (projectInfo == null) {
			return;
		}
		final long projectId = projectInfo.getNodeId();
		// ensure all resources under the affected project are
		// reported as having encoding changes
		ICharsetListenerFilter filter = new ICharsetListenerFilter() {
			@Override
			public IPath getRoot() {
				//make sure it is still the same project - it could have been deleted and recreated
				ResourceInfo currentInfo = ((Project) project).getResourceInfo(false, false);
				if (currentInfo == null) {
					return null;
				}
				long currentId = currentInfo.getNodeId();
				if (currentId != projectId) {
					return null;
				}
				// visit the project subtree
				return project.getFullPath();
			}

			@Override
			public boolean isAffected(ResourceInfo info, IPathRequestor requestor) {
				// for now, mark all resources in the project as potential encoding resource changes
				return true;
			}

			@Override
			public IProject getProject() {
				return project;
			}

		};
		addToQueue(filter);
	}

	@Override
	public void contentTypeChanged(final ContentTypeChangeEvent event) {
		// check all files that may be affected by this change (taking
		// only the current content type state into account
		// dispatch a job to generate the deltas
		ICharsetListenerFilter filter = new ICharsetListenerFilter() {

			@Override
			public IPath getRoot() {
				// visit all resources in the workspace
				return IPath.ROOT;
			}

			@Override
			public boolean isAffected(ResourceInfo info, IPathRequestor requestor) {
				if (info.getType() != IResource.FILE) {
					return false;
				}
				return event.getContentType().isAssociatedWith(requestor.requestName());
			}

			@Override
			public IProject getProject() {
				return null;
			}
		};
		addToQueue(filter);
	}

	private boolean isDisabled() {
		return disabled.get() != null;
	}

	private void processNextEvent(final ICharsetListenerFilter filter, IProgressMonitor monitor) throws CoreException {
		IElementContentVisitor visitor = (tree, requestor, elementContents) -> {
			ResourceInfo info = (ResourceInfo) elementContents;
			if (!filter.isAffected(info, requestor)) {
				return true;
			}
			info = workspace.getResourceInfo(requestor.requestPath(), false, true);
			if (info == null) {
				return false;
			}
			info.incrementCharsetGenerationCount();
			return true;
		};
		try {
			IPath root = filter.getRoot();
			if (root != null) {
				new ElementTreeIterator(workspace.getElementTree(), root).iterate(visitor);
			}
			IProject project = filter.getProject();
			if (project != null) {
				ValidateProjectEncoding.updateMissingEncodingMarker(project);
			}
		} catch (WrappedRuntimeException e) {
			throw (CoreException) e.getTargetException();
		}
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
	}

	private ICharsetListenerFilter removeFromQueue() {
		synchronized (work) {
			return work.poll();
		}
	}

	@Override
	public IStatus run(IProgressMonitor monitor) {
		monitor = Policy.monitorFor(monitor);
		try {
			String message = Messages.resources_charsetBroadcasting;
			monitor.beginTask(message, Policy.totalWork);
			try {
				workspace.prepareOperation(null, monitor);
				workspace.beginOperation(true);
				ICharsetListenerFilter next;
				//if the system is shutting down, don't broadcast
				while (systemBundle.getState() != Bundle.STOPPING && (next = removeFromQueue()) != null) {
					processNextEvent(next, monitor);
				}
			} catch (OperationCanceledException e) {
				workspace.getWorkManager().operationCanceled();
				return Status.CANCEL_STATUS;
			} finally {
				workspace.endOperation(null, true);
			}
			monitor.worked(Policy.opWork);
		} catch (CoreException sig) {
			return sig.getStatus();
		} finally {
			monitor.done();
		}
		return Status.OK_STATUS;
	}

	/**
	 * Turns off reaction to changes in the preference file.
	 */
	public void setDisabled(boolean disabled) {
		// using a thread local because this can be called by multiple threads concurrently
		if (disabled) {
			this.disabled.set(Boolean.TRUE);
		} else {
			this.disabled.remove();
		}
	}

	public void shutdown() {
		try {
			// try to prevent execution of this job to avoid "already shutdown.":
			cancel();
			wakeUp();
			// if job is already running wait for it to finish:
			join(3000, null);
		} catch (InterruptedException e) {
			// ignore
		}
		IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
		//if the service is already gone there is nothing to do
		if (contentTypeManager != null) {
			contentTypeManager.removeContentTypeChangeListener(this);
		}
	}

	public void startup() {
		Platform.getContentTypeManager().addContentTypeChangeListener(this);
	}
}
