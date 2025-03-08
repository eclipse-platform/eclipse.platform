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
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ExecFactory;

import com.pty4j.PtyProcessBuilder;

class Pty4jExecFactory implements ExecFactory {

	@Override
	public Process exec(String[] cmdLine, File workingDirectory, String[] envp, boolean mergeOutput)
			throws CoreException {
		Map<String, String> env = new HashMap<>(System.getenv());
		env.put("TERM", "xterm-256color");
		if (envp != null) {
			for (String e : envp) {
				int index = e.indexOf('=');
				if (index != -1) {
					env.put(e.substring(0, index), e.substring(index + 1));
				}
			}
		}
		try {
			PtyProcessBuilder builder = new PtyProcessBuilder().setRedirectErrorStream(true).setInitialRows(80)
					.setInitialColumns(25).setCommand(cmdLine).setEnvironment(env);
			if (workingDirectory != null) {
				builder.setDirectory(workingDirectory.getAbsolutePath());
			}
			return builder.start();
		} catch (IOException e) {
			throw new CoreException(Status.error("Can't start process", e));
		} catch (RuntimeException e) {
			throw new CoreException(Status.error("Internal error launching process", e));
		}
	}

}
