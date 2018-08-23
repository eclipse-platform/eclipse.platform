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
package org.eclipse.team.internal.ccvs.ui.model;

import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;

/**
 * A simple job scheduling rule for serializing jobs for an ICVSRepositoryLocation
 */
public class RepositoryLocationSchedulingRule implements ISchedulingRule {
	ICVSRepositoryLocation location;
	public RepositoryLocationSchedulingRule(ICVSRepositoryLocation location) {
		this.location = location;
	}		
	public boolean isConflicting(ISchedulingRule rule) {
		if(rule instanceof RepositoryLocationSchedulingRule) {
			return ((RepositoryLocationSchedulingRule)rule).location.equals(location);
		}
		return false;
	}
	public boolean contains(ISchedulingRule rule) {		
		return isConflicting(rule);
	}
}
