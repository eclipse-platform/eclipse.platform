/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
 * Anton Leherbauer (Wind River) - [198591] Allow Builder to specify scheduling rule
 * Anton Leherbauer (Wind River) - [305858] Allow Builder to return null rule
 * James Blackburn (Broadcom) - [306822] Provide Context for Builder getRule()
 * Broadcom Corporation - build configurations and references
 * Torbjörn Svensson (STMicroelectronics) - bug #552606
 *******************************************************************************/
package org.eclipse.core.resources;

import java.util.Map;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * This interfaces extends {@link IncrementalProjectBuilder}. This class
 * provides optional additional API for the the
 * <code>org.eclipse.core.resources.builders</code> standard extension point.
 * <p>
 * All builders must subclass {@link IncrementalProjectBuilder} and can
 * optionally implement this interface.
 * </p>
 *
 * @see IncrementalProjectBuilder
 * @since 3.14
 */
public interface IIncrementalProjectBuilder2 {
	/**
	 * Clean is an opportunity for a builder to discard any additional state that
	 * has been computed as a result of previous builds. It is recommended that
	 * builders override this method to delete all derived resources created by
	 * previous builds, and to remove all markers of type {@link IMarker#PROBLEM}
	 * that were created by previous invocations of the builder. The platform will
	 * take care of discarding the builder's last built state (there is no need to
	 * call <code>forgetLastBuiltState</code>).
	 * <p>
	 * This method is called as a result of invocations of
	 * <code>IWorkspace.build</code> or <code>IProject.build</code> where the build
	 * kind is {@link IncrementalProjectBuilder#CLEAN_BUILD}.
	 * </p>
	 * <p>
	 * This method is long-running; progress and cancellation are provided by the
	 * given progress monitor. All builders should report their progress and honor
	 * cancel requests in a timely manner. Cancelation requests should be propagated
	 * to the caller by throwing <code>OperationCanceledException</code>.
	 * </p>
	 *
	 * @param args    a table of builder-specific arguments keyed by argument name
	 *                (key type: <code>String</code>, value type:
	 *                <code>String</code>); <code>null</code> is equivalent to an
	 *                empty map
	 * @param monitor a progress monitor, or <code>null</code> if progress reporting
	 *                and cancellation are not desired
	 * @exception CoreException if this build fails.
	 * @see IWorkspace#build(int, IProgressMonitor)
	 * @see IncrementalProjectBuilder#clean(IProgressMonitor)
	 * @see IncrementalProjectBuilder#CLEAN_BUILD
	 * @noreference This method is not intended to be referenced by clients.
	 * @since 3.14
	 */
	public void clean(Map<String, String> args, IProgressMonitor monitor) throws CoreException;
}
