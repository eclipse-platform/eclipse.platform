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
package org.eclipse.team.internal.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.operation.IRunnableWithProgress;

/**
 * An ITeamRunnableContext is used to provide the context for a Team operation.
 * The hierarchy of contexts is used to configure the following:
 * 1) whether the operation is run in the background as a job
 * 2) whether the operation modifies the workspace
 * 3) what shell the operation should use to display info to the user
 */
public interface ITeamRunnableContext {
	/**
	 * Run the given runnable in the context of the receiver. By default, the
	 * progress is provided by the active workbench windows but subclasses may
	 * override this to provide progress in some other way (Progress Monitor or
	 * job).
	 */
	public abstract void run(IRunnableWithProgress runnable) throws InvocationTargetException, InterruptedException;
}
