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
package org.eclipse.debug.terminal;

import java.io.File;
import java.io.IOException;

import org.eclipse.cdt.utils.pty.PTY;
import org.eclipse.cdt.utils.pty.PTY.Mode;
import org.eclipse.cdt.utils.spawner.ProcessFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ExecFactory;

public class CDTFactory implements ExecFactory {

	@Override
	public Process exec(String[] cmdLine, File workingDirectory, String[] envp, boolean mergeOutput)
			throws CoreException {
		try {
			System.out.println("Using CDT Process Factory to exec new process...");
			for (Mode mode : Mode.values()) {
				System.out.println(mode + " is " + (PTY.isSupported(mode) ? "supported" : "not supported"));
			}
			PTY pty = new PTY(Mode.TERMINAL);
			pty.setTerminalSize(80, 24);
			return ProcessFactory.getFactory().exec(cmdLine, envp, workingDirectory, pty);
		} catch (IOException e) {
			throw new CoreException(Status.error("Execution failed", e));
		}
	}

}
