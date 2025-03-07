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
import java.util.Map;
import java.util.Optional;

import org.eclipse.cdt.utils.pty.PTY;
import org.eclipse.cdt.utils.pty.PTY.Mode;
import org.eclipse.cdt.utils.spawner.ProcessFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ExecFactory;

public class PtyExecFactory implements ExecFactory {

	@Override
	public Optional<Process> exec(String[] cmdLine, Optional<File> workingDirectory,
			Optional<Map<String, String>> environment, boolean mergeOutput) throws CoreException {
		if (mergeOutput || !PTY.isSupported(Mode.TERMINAL)) {
			return Optional.empty();
		}
		try {
			PTY pty = new PTY(Mode.TERMINAL);
			pty.setTerminalSize(80, 24);
			String[] env;
			if (environment.isEmpty()) {
				env = null;
			} else {
				env = environment.stream().flatMap(m -> m.entrySet().stream()).map(e -> e.getKey() + "=" + e.getValue())
						.toArray(String[]::new);
			}
			File wd = workingDirectory.orElse(null);
			return Optional.of(ProcessFactory.getFactory().exec(cmdLine, env, wd, pty));
		} catch (IOException e) {
			throw new CoreException(Status.error("Execution failed", e));
		}
	}

}
