/*******************************************************************************
 * Copyright (c) 2006, 2010 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.viewers;

import org.eclipse.core.runtime.jobs.ISchedulingRule;

/**
 * Scheduling rule factory for asynchronous operations.
 *
 * @since 3.2
 */
public class AsynchronousSchedulingRuleFactory {

	private static AsynchronousSchedulingRuleFactory fgFactory = null;

	/**
	 * Rule allows only one job to run at a time
	 */
	static class SerialRule implements ISchedulingRule {

		public SerialRule() {
		}

		@Override
		public boolean contains(ISchedulingRule rule) {
			return rule == this;
		}

		@Override
		public boolean isConflicting(ISchedulingRule rule) {
			return rule instanceof SerialRule;
		}
	}

	static class SerialPerObjectRule implements ISchedulingRule {

		private Object fObject = null;

		public SerialPerObjectRule(Object lock) {
			fObject = lock;
		}

		@Override
		public boolean contains(ISchedulingRule rule) {
			return rule == this;
		}

		@Override
		public boolean isConflicting(ISchedulingRule rule) {
			if (rule instanceof SerialPerObjectRule) {
				SerialPerObjectRule vup = (SerialPerObjectRule) rule;
				return fObject == vup.fObject;
			}
			return false;
		}

	}

	private AsynchronousSchedulingRuleFactory() {}

	public static AsynchronousSchedulingRuleFactory getDefault() {
		if (fgFactory == null) {
			fgFactory = new AsynchronousSchedulingRuleFactory();
		}
		return fgFactory;
	}

	/**
	 * Returns a scheduling rule that allows all jobs with an instance
	 * of the rule to run one at a time.
	 *
	 * @return scheduling rule
	 */
	public ISchedulingRule newSerialRule() {
		return new SerialRule();
	}

	/**
	 * Returns a scheduling rule that allows all jobs with an instance
	 * of the rule on the same object to run one at a time.
	 *
	 * @param lock object to serialize one
	 * @return scheduling rule
	 */
	public ISchedulingRule newSerialPerObjectRule(Object lock) {
		return new SerialPerObjectRule(lock);
	}

}
