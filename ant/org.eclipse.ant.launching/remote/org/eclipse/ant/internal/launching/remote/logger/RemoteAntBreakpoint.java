/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
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
package org.eclipse.ant.internal.launching.remote.logger;

import java.io.File;

import org.eclipse.ant.internal.launching.debug.model.DebugMessageIds;

public class RemoteAntBreakpoint {

	private final File fFile;
	private final int fLineNumber;
	private final String fFileName;

	public RemoteAntBreakpoint(String breakpointRepresentation) {
		String[] data = breakpointRepresentation.split(DebugMessageIds.MESSAGE_DELIMITER);
		String fileName = data[1];
		String lineNumber = data[2];
		fFileName = fileName;
		fFile = new File(fileName);
		fLineNumber = Integer.parseInt(lineNumber);
	}

	public boolean isAt(String fileName, int lineNumber) {
		return fLineNumber == lineNumber && fileName != null && fFile.equals(new File(fileName));
	}

	public String toMarshallString() {
		StringBuilder buffer = new StringBuilder(DebugMessageIds.BREAKPOINT);
		buffer.append(DebugMessageIds.MESSAGE_DELIMITER);
		buffer.append(fFileName);
		buffer.append(DebugMessageIds.MESSAGE_DELIMITER);
		buffer.append(fLineNumber);
		return buffer.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof RemoteAntBreakpoint other)) {
			return false;
		}
		return other.getLineNumber() == fLineNumber && other.getFile().equals(fFile);
	}

	@Override
	public int hashCode() {
		return fFileName.hashCode() + fLineNumber;
	}

	public int getLineNumber() {
		return fLineNumber;
	}

	public String getFileName() {
		return fFileName;
	}

	public File getFile() {
		return fFile;
	}
}
