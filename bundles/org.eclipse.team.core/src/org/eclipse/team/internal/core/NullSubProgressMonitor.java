/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.core;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

/**
 * This sub-progress monitor can be used to ignore progress indication for
 * methods but allow cancellation.
 * <p>
 * This implementation supports cancelation. The default implementations of the
 * other methods do nothing.
 * </p>
 * @see SubProgressMonitor
 */
public class NullSubProgressMonitor extends SubProgressMonitor {
	/**
	 * Constructor for InfiniteSubProgressMonitor.
	 * @param monitor
	 */
	public NullSubProgressMonitor(IProgressMonitor monitor) {
		super(monitor, 0, 0);
	}

	@Override
	public void beginTask(String name, int totalWork) {
	}

	@Override
	public void done() {
	}

	@Override
	public void internalWorked(double work) {
	}

	@Override
	public void subTask(String name) {
	}

	@Override
	public void worked(int work) {
	}
}
