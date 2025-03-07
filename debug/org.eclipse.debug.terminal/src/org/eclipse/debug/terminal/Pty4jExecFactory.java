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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ExecFactory;

import com.pty4j.PtyProcessBuilder;

class Pty4jExecFactory implements ExecFactory {

	@Override
	public Optional<Process> exec(String[] cmdLine, Optional<File> workingDirectory,
			Optional<Map<String, String>> environment, boolean mergeOutput) throws CoreException {
		try {
			PtyProcessBuilder builder = new PtyProcessBuilder().setRedirectErrorStream(true).setInitialRows(80)
					.setInitialColumns(25).setCommand(cmdLine).setEnvironment(environment.orElse(System.getenv()));
			workingDirectory.map(File::getAbsolutePath).ifPresent(builder::setDirectory);
			return Optional.of(builder.start());
		} catch (IOException e) {
			throw new CoreException(Status.error("Can't start process", e));
		} catch (RuntimeException e) {
			return Optional.empty();
		}
	}

}
