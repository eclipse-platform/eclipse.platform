/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.core.subscribers;

import java.util.*;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.core.*;

/**
 * Provides a per-thread nested locking mechanism. A thread can acquire a
 * lock on a specific resource by calling acquire(). Subsequently, acquire() can be called
 * multiple times on the resource or any of its children from within the same thread
 * without blocking. Other threads that try
 * and acquire the lock on those same resources will be blocked until the first 
 * thread releases all it's nested locks.
 * <p>
 * The locking is managed by the platform via scheduling rules. This class simply 
 * provides the nesting mechanism in order to allow the client to determine when
 * the lock for the thread has been released. Therefore, this lock will block if
 * another thread already locks the same resource.</p>
 */
public class BatchingLock {
	// This is a placeholder rule used to indicate that no scheduling rule is needed
	/* internal use only */ static final ISchedulingRule NULL_SCHEDULING_RULE= new ISchedulingRule() {
		public boolean contains(ISchedulingRule rule) {
			return false;
		}
		public boolean isConflicting(ISchedulingRule rule) {
			return false;
		}
	};
	
	public class ThreadInfo {
		private Set changedResources = new HashSet();
		private IFlushOperation operation;
		private List rules = new ArrayList();
		public ThreadInfo(IFlushOperation operation) {
			this.operation = operation;
		}
		/**
		 * Push a scheduling rule onto the stack for this thread and
		 * acquire the rule if it is not the workspace root.
		 * @param resource
		 * @param monitor 
		 * @return the scheduling rule that was obtained
		 */
		public ISchedulingRule pushRule(ISchedulingRule resource, IProgressMonitor monitor) {
			// The scheduling rule is either the project or the resource's parent
			final ISchedulingRule rule = getRuleForResoure(resource);
			if (rule != NULL_SCHEDULING_RULE) {
				boolean success = false;
				try {
					Job.getJobManager().beginRule(rule, monitor);
					addRule(rule);
					success = true;
				} finally {
					if (!success) {
						try {
						    // The begin was canceled (or some other problem occurred).
							// Free the scheduling rule
							// so the clients of ReentrantLock don't need to
							// do an endRule when the operation is canceled.
							Job.getJobManager().endRule(rule);
						} catch (RuntimeException e) {
							// Log and ignore so the original exception is not lost
							TeamPlugin.log(IStatus.ERROR, "Failed to end scheduling rule", e); //$NON-NLS-1$
						}
					}
				}
			} else {
				// Record the fact that we didn't push a rule so we
				// can match it when we pop
				addRule(rule);
			}
			return rule;
		}
		/**
		 * Pop the scheduling rule from the stack and release it if it
		 * is not the workspace root. Flush any changed sync info to 
		 * disk if necessary. A flush is necessary if the stack is empty
		 * or if the top-most non-null scheduling rule was popped as a result
		 * of this operation.
		 * @param rule 
		 * @param monitor
		 * @throws TeamException
		 */
		public void popRule(ISchedulingRule rule, IProgressMonitor monitor) throws TeamException {
			try {
				if (isFlushRequired()) {
					flush(monitor);
				}
			} finally {
				ISchedulingRule stackedRule = removeRule();
				if (rule == null) {
					rule = NULL_SCHEDULING_RULE;
				}
				Assert.isTrue(stackedRule.equals(rule), "end for resource '" + rule + "' does not match stacked rule '" + stackedRule + "'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				if (rule != NULL_SCHEDULING_RULE) {
					Job.getJobManager().endRule(rule);
				}
			}
		}
		private ISchedulingRule getRuleForResoure(ISchedulingRule resourceRule) {
			ISchedulingRule rule;
			if (resourceRule instanceof IResource) {
				IResource resource = (IResource)resourceRule;
				if (resource.getType() == IResource.ROOT) {
					// Never lock the whole workspace
					rule = NULL_SCHEDULING_RULE;
				} else  if (resource.getType() == IResource.PROJECT) {
					rule = resource;
				} else {
					rule = resource.getParent();
				}
			} else if (resourceRule instanceof MultiRule) {
				// Create a MultiRule for all projects from the given rule
				ISchedulingRule[] rules = ((MultiRule)resourceRule).getChildren();
				Set projects = new HashSet();
				for (int i = 0; i < rules.length; i++) {
					ISchedulingRule childRule = rules[i];
					if (childRule instanceof IResource) {
						projects.add(((IResource)childRule).getProject());
					}
				}
				if (projects.isEmpty()) {
					rule = NULL_SCHEDULING_RULE;
				} else if (projects.size() == 1) {
					rule = (ISchedulingRule)projects.iterator().next();
				} else {
					rule = new MultiRule((ISchedulingRule[]) projects.toArray(new ISchedulingRule[projects.size()]));
				}
			} else {
				// Rule is not associated with resources so ignore it
				rule = NULL_SCHEDULING_RULE;
			}
			return rule;
		}
		/**
		 * Return <code>true</code> if we are still nested in
		 * an acquire for this thread.
		 * 
		 * @return whether there are still rules on the stack
		 */
		public boolean isNested() {
			return !rules.isEmpty();
		}
		public void addChangedResource(IResource resource) {
			changedResources.add(resource);
		}
		public boolean isEmpty() {
			return changedResources.isEmpty();
		}
		public IResource[] getChangedResources() {
			return (IResource[]) changedResources.toArray(new IResource[changedResources.size()]);
		}
		public void flush(IProgressMonitor monitor) throws TeamException {
			try {
				operation.flush(this, monitor);
			} catch (OutOfMemoryError e) {
				throw e;
			} catch (Error e) {
				handleAbortedFlush(e);
				throw e;
			} catch (RuntimeException e) {
				handleAbortedFlush(e);
				throw e;
			} finally {
			    // We have to clear the resources no matter what since the next attempt
				// to flush may not have an appropriate scheduling rule
			    changedResources.clear();
			}
		}
		private boolean isFlushRequired() {
			return rules.size() == 1 || remainingRulesAreNull();
		}
		/*
		 * Return true if all but the last rule in the stack is null
		 */
		private boolean remainingRulesAreNull() {
			for (int i = 0; i < rules.size() - 1; i++) {
				ISchedulingRule rule = (ISchedulingRule) rules.get(i);
				if (rule != NULL_SCHEDULING_RULE) {
					return false;
				}
			}
			return true;
		}
		private void handleAbortedFlush(Throwable t) {
			TeamPlugin.log(IStatus.ERROR, Messages.BatchingLock_11, t); 
		}
		private void addRule(ISchedulingRule rule) {
			rules.add(rule);
		}
		private ISchedulingRule removeRule() {
			return (ISchedulingRule)rules.remove(rules.size() - 1);
		}
		public boolean ruleContains(IResource resource) {
			for (Iterator iter = rules.iterator(); iter.hasNext();) {
				ISchedulingRule rule = (ISchedulingRule) iter.next();
				if (rule != NULL_SCHEDULING_RULE && rule.contains(resource)) {
					return true;
				}
			}
			return false;
		}
	}
	
	public interface IFlushOperation {
		public void flush(ThreadInfo info, IProgressMonitor monitor) throws TeamException;
	}
	
	private Map infos = new HashMap();
	
	/**
	 * Return the thread info for the current thread
	 * @return the thread info for the current thread
	 */
	protected ThreadInfo getThreadInfo() {
		Thread thisThread = Thread.currentThread();
		synchronized (infos) {
			ThreadInfo info = (ThreadInfo)infos.get(thisThread);
			return info;
		}
	}
	
	private ThreadInfo getThreadInfo(IResource resource) {
		synchronized (infos) {
			for (Iterator iter = infos.values().iterator(); iter.hasNext();) {
				ThreadInfo info = (ThreadInfo) iter.next();
				if (info.ruleContains(resource)) {
					return info;
				}
			}
			return null;
		}
	}
	
	public ISchedulingRule acquire(ISchedulingRule resourceRule, IFlushOperation operation, IProgressMonitor monitor) {
		ThreadInfo info = getThreadInfo();
		boolean added = false;
		synchronized (infos) {
			if (info == null) {
				info = createThreadInfo(operation);
				Thread thisThread = Thread.currentThread();
				infos.put(thisThread, info);
				added = true;
				if(Policy.DEBUG_THREADING) System.out.println("[" + thisThread.getName() + "] acquired batching lock on " + resourceRule); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		try {
			return info.pushRule(resourceRule, monitor);
		} catch (OperationCanceledException e) {
			// The operation was canceled.
			// If this is the outermost acquire then remove the info that was just added
			if (added) {
				synchronized (infos) {
					infos.remove(Thread.currentThread());
				}
			}
			throw e;
		}
	}
	
	/**
	 * Create the ThreadInfo instance used to cache the lock state for the 
	 * current thread. Subclass can override to provide a subclass of
	 * ThreadInfo.
     * @param operation the flush operation
     * @return a ThreadInfo instance
     */
    protected ThreadInfo createThreadInfo(IFlushOperation operation) {
        return new ThreadInfo(operation);
    }

    /**
	 * Release the lock held on any resources by this thread. The provided rule must
	 * be identical to the rule returned by the corresponding acquire(). If the rule
	 * for the release is non-null and all remaining rules held by the lock are null,
	 * the the flush operation provided in the acquire method will be executed.
     * @param rule the scheduling rule
     * @param monitor a progress monitor
     * @throws TeamException 
	 */
	public void release(ISchedulingRule rule, IProgressMonitor monitor) throws TeamException {
		ThreadInfo info = getThreadInfo();
		Assert.isNotNull(info, "Unmatched acquire/release."); //$NON-NLS-1$
		Assert.isTrue(info.isNested(), "Unmatched acquire/release."); //$NON-NLS-1$
		info.popRule(rule, monitor);
		synchronized (infos) {
			if (!info.isNested()) {
				Thread thisThread = Thread.currentThread();
				if(Policy.DEBUG_THREADING) System.out.println("[" + thisThread.getName() + "] released batching lock"); //$NON-NLS-1$ //$NON-NLS-2$
				infos.remove(thisThread);
			}
		}
	}

	public void resourceChanged(IResource resource) {
		ThreadInfo info = getThreadInfo();
		Assert.isNotNull(info, "Folder changed outside of resource lock"); //$NON-NLS-1$
		info.addChangedResource(resource);
	}

	/**
	 * Flush any changes accumulated by the lock so far.
	 * @param monitor a progress monitor
	 * @throws TeamException 
	 */
	public void flush(IProgressMonitor monitor) throws TeamException {
		ThreadInfo info = getThreadInfo();
		Assert.isNotNull(info, "Flush requested outside of resource lock"); //$NON-NLS-1$
		info.flush(monitor);
	}

	public boolean isWithinActiveOperationScope(IResource resource) {
		synchronized (infos) {
			return getThreadInfo(resource) != null;
		}
	}
}
