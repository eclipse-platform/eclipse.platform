/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.core.mapping;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ProgressMonitorWrapper;

public class GroupProgressMonitor extends ProgressMonitorWrapper implements
		IProgressMonitor {

	private final IProgressMonitor group;
	private final int ticks;

	public GroupProgressMonitor(IProgressMonitor monitor, IProgressMonitor group, int groupTicks) {
		super(monitor);
		this.group = group;
		this.ticks = groupTicks;
	}

	public IProgressMonitor getGroup() {
		return group;
	}

	public int getTicks() {
		return ticks;
	}

}
