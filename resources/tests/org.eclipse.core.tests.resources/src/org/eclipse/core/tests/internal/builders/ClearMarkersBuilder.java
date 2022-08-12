/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *******************************************************************************/
package org.eclipse.core.tests.internal.builders;

import java.util.Map;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * This builder is for a regression test (Bug_147232).  It simply deletes all problem
 * markers on the project being built. If requested, it pauses after deleting the markers
 * to give time for an interim post change event to occur.
 * @see org.eclipse.core.tests.resources.regression.Bug_147232
 */
public class ClearMarkersBuilder extends TestBuilder {
	public static boolean pauseAfterBuild = false;
	public static final String BUILDER_NAME = "org.eclipse.core.tests.resources.clearmarkersbuilder";

	/*
	 * @see InternalBuilder#build(int, Map, IProgressMonitor)
	 */
	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		getProject().deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
		//wait after build if requested
		try {
			if (pauseAfterBuild) {
				Thread.sleep(5000);
			}
		} catch (InterruptedException e) {
			//ignore
		}
		return null;
	}
}
