/*******************************************************************************
 * Copyright (c) 2016 Eclipse Foundation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Mikael Barbero (Eclipse Foundation) - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.refresh;

import java.util.Collection;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;

/**
 * A specific kind of system job used for installing and uninstalling auto-refresh monitors on resources.
 * <p>
 * MonitorJobs have specific scheduling rules. These rules only apply to instances of MonitorJob. The
 * rules conflicts with and contains the same rules as the resource(s) given to the factory methods
 * for creating the MonitorJob.
 * <p>
 * MonitorJob belongs to the {@link ResourcesPlugin#FAMILY_AUTO_REFRESH auto refresh family}
 * for testing purpose. Joining on jobs of this family is sometime mandatory for testing conditions
 * after changing some resources.
 */
public class MonitorJob extends Job {
	private final ICoreRunnable runnable;

	private MonitorJob(String name, MonitorRule rule, ICoreRunnable runnable) {
		super(name);
		this.runnable = runnable;
		setSystem(true);
		setRule(rule);
	}

	/**
	 * Returns a new monitor job with the given name, resource on which the scheduling rule will
	 * be built and core runnable.
	 *
	 * @param name the name of the returned job
	 * @param resource the resource on which the scheduling rule of the returned job will be built.
	 * @param runnable the core runnable that will be executed by the returned job
	 */
	static Job createSystem(final String name, IResource resource, final ICoreRunnable runnable) {
		return new MonitorJob(name, MonitorRule.create(resource), runnable);
	}

	/**
	 * Returns a new monitor job with the given name, resources on which the scheduling rule will
	 * be built and core runnable.
	 *
	 * @param name the name of the returned job
	 * @param resources the resources on which the scheduling rule of the returned job will be built.
	 * @param runnable the core runnable that will be executed by the returned job
	 */
	static Job createSystem(final String name, Collection<IResource> resources, final ICoreRunnable runnable) {
		return new MonitorJob(name, MonitorRule.create(resources), runnable);
	}

	/**
	 * A specific scheduling rule for {@link MonitorJob}.
	 * <p>
	 * It conflicts with and contains other instances of this kind of scheduling
	 * rule which have been created with conflicting and containing {@link IResource}s.
	 */
	private static class MonitorRule implements ISchedulingRule {
		private static final ISchedulingRule[] SCHEDULING_RULE__EMPTY_ARR = new ISchedulingRule[0];

		private final ISchedulingRule resourceRule;

		MonitorRule(ISchedulingRule schedulingRule) {
			resourceRule = schedulingRule;
		}

		/**
		 * Create a new {@link MonitorRule} that will be conflicting with
		 * the same rules and will contain the same rules as the given resource.
		 *
		 * @param resource the resource to wrap
		 * @return a new {@link MonitorRule}.
		 */
		static MonitorRule create(IResource resource) {
			return new MonitorRule(resource);
		}

		/**
		 * Create a new {@link MonitorRule} that will be conflicting with
		 * the same rules and will contain the same rules as the given resources.
		 *
		 * @param resources the resources to wrap
		 * @return a new {@link MonitorRule}.
		 */
		static MonitorRule create(Collection<IResource> resources) {
			return new MonitorRule(MultiRule.combine(resources.toArray(SCHEDULING_RULE__EMPTY_ARR)));
		}

		@Override
		public boolean isConflicting(ISchedulingRule rule) {
			if (rule instanceof MonitorRule) {
				return resourceRule.isConflicting(((MonitorRule) rule).resourceRule);
			}
			return false;
		}

		@Override
		public boolean contains(ISchedulingRule rule) {
			if (rule instanceof MonitorRule) {
				return resourceRule.contains(((MonitorRule) rule).resourceRule);
			}
			return false;
		}
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		try {
			runnable.run(monitor);
		} catch (CoreException e) {
			IStatus st = e.getStatus();
			return new Status(st.getSeverity(), st.getPlugin(), st.getCode(), st.getMessage(), e);
		}
		return Status.OK_STATUS;
	}

	@Override
	public boolean belongsTo(Object family) {
		return ResourcesPlugin.FAMILY_AUTO_REFRESH == family;
	}
}
