/*******************************************************************************
 * Copyright (c) 2003, 2015 IBM Corporation and others.
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
package org.eclipse.core.tests.runtime.jobs;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

/**
 * A hierarchical rule based on IPath.  A path rule contains another path
 * rule if its path is a prefix of the other rule's path.  A path rule is conflicting
 * with another rule if either one is a prefix of the other.
 */
public class PathRule implements ISchedulingRule {
	private IPath path;

	public PathRule(IPath path) {
		this.path = path;
	}

	public PathRule(String pathString) {
		this.path = new Path(pathString);
	}

	@Override
	public boolean contains(ISchedulingRule rule) {
		if (this == rule)
			return true;
		if (!(rule instanceof PathRule))
			return false;
		return path.isPrefixOf(((PathRule) rule).getFullPath());
	}

	@Override
	public boolean isConflicting(ISchedulingRule rule) {
		if (!(rule instanceof PathRule))
			return false;
		IPath otherPath = ((PathRule) rule).getFullPath();
		return path.isPrefixOf(otherPath) || otherPath.isPrefixOf(path);
	}

	public IPath getFullPath() {
		return path;
	}

	@Override
	public String toString() {
		return "PathRule(" + path + ")";
	}
}
