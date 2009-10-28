/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.launching.remote.logger;

import java.io.File;

import org.eclipse.ant.internal.launching.debug.model.DebugMessageIds;

public class RemoteAntBreakpoint {
	
    private File fFile;
	private int fLineNumber;
	private String fFileName;
	
	public RemoteAntBreakpoint(String breakpointRepresentation) {
		String[] data= breakpointRepresentation.split(DebugMessageIds.MESSAGE_DELIMITER);
		String fileName= data[1];
		String lineNumber= data[2];
		fFileName= fileName;
		fFile= new File(fileName);
		fLineNumber= Integer.parseInt(lineNumber);
	}

	public boolean isAt(String fileName, int lineNumber) {
		return fLineNumber == lineNumber && fileName != null && fFile.equals(new File(fileName));
	}
	
	public String toMarshallString() {
		StringBuffer buffer= new StringBuffer(DebugMessageIds.BREAKPOINT);
		buffer.append(DebugMessageIds.MESSAGE_DELIMITER);
		buffer.append(fFileName);
		buffer.append(DebugMessageIds.MESSAGE_DELIMITER);
		buffer.append(fLineNumber);
		return buffer.toString();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (!(obj instanceof RemoteAntBreakpoint)) {
			return false;
		}
		RemoteAntBreakpoint other= (RemoteAntBreakpoint) obj;
		return other.getLineNumber() == fLineNumber && other.getFile().equals(fFile);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
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
