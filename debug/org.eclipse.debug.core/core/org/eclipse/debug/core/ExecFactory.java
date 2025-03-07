/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.core;

import java.io.File;

import org.eclipse.core.runtime.CoreException;

/**
 * A {@link ExecFactory} can be used to control how Eclipse forks a
 * new {@link Process}. As this is a global behavior, only one factory can be
 * set for the whole application lifetime.
 *
 * @since 3.23
 */
public interface ExecFactory {

	Process exec(String[] cmdLine, File workingDirectory, String[] envp, boolean mergeOutput) throws CoreException;

	static void setDefault(ExecFactory factory) {
		synchronized (DebugPlugin.class) {
			if (DebugPlugin.factory != null) {
				throw new IllegalStateException("A factory was already set for this application"); //$NON-NLS-1$
			}
			DebugPlugin.factory = factory;
		}
	}

}
