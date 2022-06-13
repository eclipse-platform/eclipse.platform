/*******************************************************************************
 * Copyright (c) 2003, 2012 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime.jobs;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * A MultiRule is a compound scheduling rule that represents a fixed group of child
 * scheduling rules.  A MultiRule conflicts with another rule if any of its children conflict
 * with that rule.  More formally, a compound rule represents a logical intersection
 * of its child rules with respect to the <code>isConflicting</code> equivalence
 * relation.
 * <p>
 * A MultiRule will never contain other MultiRules as children.  If a MultiRule is provided
 * as a child, its children will be added instead.
 * </p>
 *
 * @since 3.0
 * @noextend This class is not intended to be subclassed by clients.
 */
public class MultiRule implements ISchedulingRule {
	private ISchedulingRule[] rules;

	/**
	 * Returns a scheduling rule that encompasses all provided rules.  The resulting
	 * rule may or may not be an instance of <code>MultiRule</code>.  If all
	 * provided rules are <code>null</code> then the result will be
	 * <code>null</code>.
	 *
	 * @param ruleArray An array of scheduling rules, some of which may be <code>null</code>
	 * @return a combined scheduling rule, or <code>null</code>
	 * @since 3.1
	 */
	public static ISchedulingRule combine(ISchedulingRule[] ruleArray) {
		ISchedulingRule result = null;
		for (ISchedulingRule element : ruleArray) {
			if (element == null)
				continue;
			if (result == null) {
				result = element;
				continue;
			}
			result = combine(result, element);
		}
		return result;
	}

	/**
	 * Returns a scheduling rule that encompasses both provided rules.  The resulting
	 * rule may or may not be an instance of <code>MultiRule</code>.  If both
	 * provided rules are <code>null</code> then the result will be
	 * <code>null</code>.
	 *
	 * @param rule1 a scheduling rule, or <code>null</code>
	 * @param rule2 another scheduling rule, or <code>null</code>
	 * @return a combined scheduling rule, or <code>null</code>
	 */
	public static ISchedulingRule combine(ISchedulingRule rule1, ISchedulingRule rule2) {
		if (rule1 == rule2)
			return rule1;
		if (rule1 == null)
			return rule2;
		if (rule2 == null)
			return rule1;
		if (rule1.contains(rule2))
			return rule1;
		if (rule2.contains(rule1))
			return rule2;
		MultiRule result = new MultiRule();
		result.rules = new ISchedulingRule[] {rule1, rule2};
		//make sure we don't end up with nested multi-rules
		if (rule1 instanceof MultiRule || rule2 instanceof MultiRule)
			result.rules = flatten(result.rules);
		return result;
	}

	/*
	 * Collapses an array of rules that may contain MultiRules into an
	 * array in which no rules are MultiRules.
	 */
	private static ISchedulingRule[] flatten(ISchedulingRule[] nestedRules) {
		ArrayList<ISchedulingRule> myRules = new ArrayList<>(nestedRules.length);
		for (ISchedulingRule nestedRule : nestedRules) {
			if (nestedRule instanceof MultiRule) {
				ISchedulingRule[] children = ((MultiRule) nestedRule).getChildren();
				myRules.addAll(Arrays.asList(children));
			} else {
				myRules.add(nestedRule);
			}
		}
		return myRules.toArray(new ISchedulingRule[myRules.size()]);
	}

	/**
	 * Creates a new scheduling rule that composes a set of nested rules.
	 *
	 * @param nestedRules the nested rules for this compound rule.
	 */
	public MultiRule(ISchedulingRule[] nestedRules) {
		this.rules = flatten(nestedRules);
	}

	/**
	 * Creates a new scheduling rule with no nested rules. For
	 * internal use only.
	 */
	private MultiRule() {
		//to be invoked only by factory methods
	}

	/**
	 * Returns the child rules within this rule.
	 * @return the child rules
	 */
	public ISchedulingRule[] getChildren() {
		return rules.clone();
	}

	@Override
	public boolean contains(ISchedulingRule rule) {
		if (this == rule)
			return true;
		if (rule instanceof MultiRule) {
			ISchedulingRule[] otherRules = ((MultiRule) rule).getChildren();
			//for each child of the target, there must be some child in this rule that contains it.
			for (ISchedulingRule otherRule : otherRules) {
				boolean found = false;
				for (int mine = 0; !found && mine < rules.length; mine++)
					found = rules[mine].contains(otherRule);
				if (!found)
					return false;
			}
			return true;
		}
		for (ISchedulingRule rule2 : rules)
			if (rule2.contains(rule))
				return true;
		return false;
	}

	@Override
	public boolean isConflicting(ISchedulingRule rule) {
		if (this == rule)
			return true;
		if (rule instanceof MultiRule) {
			ISchedulingRule[] otherRules = ((MultiRule) rule).getChildren();
			for (ISchedulingRule otherRule : otherRules)
				for (ISchedulingRule rule2 : rules)
					if (rule2.isConflicting(otherRule))
						return true;
		} else {
			for (ISchedulingRule rule3 : rules)
				if (rule3.isConflicting(rule))
					return true;
		}
		return false;
	}

	/*
	 * For debugging purposes only.
	 */
	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("MultiRule["); //$NON-NLS-1$
		int last = rules.length - 1;
		for (int i = 0; i < rules.length; i++) {
			buffer.append(rules[i]);
			if (i != last)
				buffer.append(',');
		}
		buffer.append(']');
		return buffer.toString();
	}
}
