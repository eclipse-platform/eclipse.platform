/*******************************************************************************
 * Copyright (c) 2004, 2013 IBM Corporation and others.
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
package org.eclipse.ant.internal.launching.debug.model;

import org.eclipse.ant.internal.launching.AntLaunchingUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;

/**
 * Ant stack frame.
 */
public class AntStackFrame extends AntDebugElement implements IStackFrame {

	private AntThread fThread;
	private String fName;
	private int fLineNumber;
	private String fFilePath;
	private int fId;
	private String fFullPath;

	/**
	 * Constructs a stack frame in the given thread with the given id.
	 *
	 * @param antThread
	 * @param id
	 *            stack frame id (0 is the top of the stack)
	 */
	public AntStackFrame(AntThread antThread, int id, String name, String fullPath, int lineNumber) {
		super((AntDebugTarget) antThread.getDebugTarget());
		fId = id;
		fThread = antThread;
		fLineNumber = lineNumber;
		fName = name;
		setFilePath(fullPath);
	}

	public void setId(int id) {
		fId = id;
	}

	@Override
	public IThread getThread() {
		return fThread;
	}

	@Override
	public IVariable[] getVariables() throws DebugException {
		return fThread.getVariables();
	}

	@Override
	public boolean hasVariables() {
		return isSuspended();
	}

	@Override
	public int getLineNumber() {
		return fLineNumber;
	}

	public void setLineNumber(int lineNumber) {
		fLineNumber = lineNumber;
	}

	public void setFilePath(String fullPath) {
		fFullPath = fullPath;
		IFile file = AntLaunchingUtil.getFileForLocation(fullPath, null);
		if (file != null) {
			fFilePath = file.getProjectRelativePath().toString();
		} else {
			fFilePath = IPath.fromOSString(fullPath).lastSegment();
		}
	}

	public String getFilePath() {
		return fFullPath;
	}

	@Override
	public int getCharStart() {
		return -1;
	}

	@Override
	public int getCharEnd() {
		return -1;
	}

	@Override
	public String getName() {
		return fName;
	}

	public void setName(String name) {
		fName = name;
	}

	@Override
	public IRegisterGroup[] getRegisterGroups() {
		return null;
	}

	@Override
	public boolean hasRegisterGroups() {
		return false;
	}

	@Override
	public boolean canStepInto() {
		return getThread().canStepInto();
	}

	@Override
	public boolean canStepOver() {
		return getThread().canStepOver();
	}

	@Override
	public boolean canStepReturn() {
		return getThread().canStepReturn();
	}

	@Override
	public boolean isStepping() {
		return getThread().isStepping();
	}

	@Override
	public void stepInto() throws DebugException {
		getThread().stepInto();
	}

	@Override
	public void stepOver() throws DebugException {
		getThread().stepOver();
	}

	@Override
	public void stepReturn() throws DebugException {
		getThread().stepReturn();
	}

	@Override
	public boolean canResume() {
		return getThread().canResume();
	}

	@Override
	public boolean canSuspend() {
		return getThread().canSuspend();
	}

	@Override
	public boolean isSuspended() {
		return getThread().isSuspended();
	}

	@Override
	public void resume() throws DebugException {
		getThread().resume();
	}

	@Override
	public void suspend() throws DebugException {
		getThread().suspend();
	}

	@Override
	public boolean canTerminate() {
		return getThread().canTerminate();
	}

	@Override
	public boolean isTerminated() {
		return getThread().isTerminated();
	}

	@Override
	public void terminate() throws DebugException {
		getThread().terminate();
	}

	/**
	 * Returns the name of the buildfile this stack frame is associated with.
	 *
	 * @return the name of the buildfile this stack frame is associated with
	 */
	public String getSourceName() {
		return fFilePath;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof AntStackFrame) {
			AntStackFrame sf = (AntStackFrame) obj;
			if (getSourceName() != null) {
				return getSourceName().equals(sf.getSourceName()) && sf.getLineNumber() == getLineNumber() && sf.fId == fId;
			}
			return sf.fId == fId;
		}
		return false;
	}

	@Override
	public int hashCode() {
		if (getSourceName() == null) {
			return fId;
		}
		return getSourceName().hashCode() + fId;
	}

	/**
	 * Returns this stack frame's unique identifier within its thread
	 *
	 * @return this stack frame's unique identifier within its thread
	 */
	protected int getIdentifier() {
		return fId;
	}

	/**
	 * Returns the system, user or runtime property name, or <code>null</code> if unable to resolve a property with the name.
	 *
	 * @param propertyName
	 *            the name of the variable to search for
	 * @return a property, or <code>null</code> if none
	 */
	public AntProperty findProperty(String propertyName) {
		try {
			for (IVariable group : getVariables()) {
				AntProperties propertiesGrouping = (AntProperties) group;
				AntPropertiesValue value = (AntPropertiesValue) propertiesGrouping.getValue();
				for (IVariable currproperty : value.getVariables()) {
					AntProperty property = (AntProperty) currproperty;
					if (property.getName().equals(propertyName)) {
						return property;
					}
				}
			}
		}
		catch (DebugException e) {
			// do nothing
		}
		return null;
	}
}
