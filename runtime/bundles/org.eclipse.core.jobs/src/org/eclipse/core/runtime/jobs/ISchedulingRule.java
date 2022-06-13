/*******************************************************************************
 *  Copyright (c) 2003, 2012 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime.jobs;

/**
 * Scheduling rules are used by jobs to indicate when they need exclusive access
 * to a resource.  Scheduling rules can also be applied synchronously to a thread
 * using <code>IJobManager.beginRule(ISchedulingRule)</code> and
 * <code>IJobManager.endRule(ISchedulingRule)</code>.  The job manager guarantees that
 * no two jobs with conflicting scheduling rules will run concurrently.
 * Multiple rules can be applied to a given thread only if the outer rule explicitly
 * allows the nesting as specified by the <code>contains</code> method.
 * <p>
 * Clients may implement this interface.
 * </p>
 *
 * @see Job#getRule()
 * @see Job#setRule(ISchedulingRule)
 * @see Job#schedule(long)
 * @see IJobManager#beginRule(ISchedulingRule, org.eclipse.core.runtime.IProgressMonitor)
 * @see IJobManager#endRule(ISchedulingRule)
 * @since 3.0
 */
public interface ISchedulingRule {
	/**
	 * Returns whether this scheduling rule completely contains another scheduling
	 * rule.  Rules can only be nested within a thread if the inner rule is completely
	 * contained within the outer rule.
	 * <p>
	 * Implementations of this method must obey the rules of a partial order relation
	 * on the set of all scheduling rules.  In particular, implementations must be reflexive
	 * (a.contains(a) is always true), antisymmetric (a.contains(b) and b.contains(a) iff a.equals(b),
	 * and transitive (if a.contains(b) and b.contains(c), then a.contains(c)).  Implementations
	 * of this method must return <code>false</code> when compared to a rule they
	 * know nothing about.
	 *
	 * @param rule the rule to check for containment
	 * @return <code>true</code> if this rule contains the given rule, and
	 * <code>false</code> otherwise.
	 */
	boolean contains(ISchedulingRule rule);

	/**
	 * Returns whether this scheduling rule is compatible with another scheduling rule.
	 * If <code>true</code> is returned, then no job with this rule will be run at the
	 * same time as a job with the conflicting rule.  If <code>false</code> is returned,
	 * then the job manager is free to run jobs with these rules at the same time.
	 * <p>
	 * Implementations of this method must be reflexive, symmetric, and consistent,
	 * and must return <code>false</code> when compared  to a rule they know
	 * nothing about.
	 * <p>
	 * This method must return true if calling {@link #contains(ISchedulingRule)} on
	 * the same rule also returns true. This is required because it would otherwise
	 * allow two threads to be running concurrently with the same rule.
	 *
	 * @param rule the rule to check for conflicts
	 * @return <code>true</code> if the rule is conflicting, and <code>false</code>
	 * 	otherwise.
	 */
	boolean isConflicting(ISchedulingRule rule);
}
