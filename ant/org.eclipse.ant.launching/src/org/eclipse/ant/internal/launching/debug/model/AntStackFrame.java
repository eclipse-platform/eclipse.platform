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
package org.eclipse.ant.internal.launching.debug.model;

import org.eclipse.ant.internal.launching.AntLaunchingUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Path;
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
	 * @param id stack frame id (0 is the top of the stack)
	 */
	public AntStackFrame(AntThread antThread, int id, String name, String fullPath, int lineNumber) {
		super((AntDebugTarget) antThread.getDebugTarget());
		fId = id;
		fThread = antThread;
		fLineNumber= lineNumber;
		fName= name;
		setFilePath(fullPath);
	}
	
	public void setId(int id) {
		fId= id;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IStackFrame#getThread()
	 */
	public IThread getThread() {
		return fThread;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IStackFrame#getVariables()
	 */
	public IVariable[] getVariables() throws DebugException {
	   return fThread.getVariables();
	}

    /* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IStackFrame#hasVariables()
	 */
	public boolean hasVariables() {
		return isSuspended();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IStackFrame#getLineNumber()
	 */
	public int getLineNumber() {
		return fLineNumber;
	}
	
	public void setLineNumber(int lineNumber) {
		fLineNumber= lineNumber;
	}
	
	public void setFilePath(String fullPath) {
        fFullPath= fullPath;
        IFile file= AntLaunchingUtil.getFileForLocation(fullPath, null);
        if (file != null) {
            fFilePath= file.getProjectRelativePath().toString();
        } else {
            fFilePath= new Path(fullPath).lastSegment();
        }
	}
	
	public String getFilePath() {
		return fFullPath;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IStackFrame#getCharStart()
	 */
	public int getCharStart() {
		return -1;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IStackFrame#getCharEnd()
	 */
	public int getCharEnd() {
		return -1;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IStackFrame#getName()
	 */
	public String getName() {
		return fName;
	}
	
	public void setName(String name) {
		fName= name;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IStackFrame#getRegisterGroups()
	 */
	public IRegisterGroup[] getRegisterGroups() {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IStackFrame#hasRegisterGroups()
	 */
	public boolean hasRegisterGroups() {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IStep#canStepInto()
	 */
	public boolean canStepInto() {
		return getThread().canStepInto();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IStep#canStepOver()
	 */
	public boolean canStepOver() {
		return getThread().canStepOver();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IStep#canStepReturn()
	 */
	public boolean canStepReturn() {
		return getThread().canStepReturn();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IStep#isStepping()
	 */
	public boolean isStepping() {
		return getThread().isStepping();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IStep#stepInto()
	 */
	public void stepInto() throws DebugException {
		getThread().stepInto();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IStep#stepOver()
	 */
	public void stepOver() throws DebugException {
		getThread().stepOver();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IStep#stepReturn()
	 */
	public void stepReturn() throws DebugException {
		getThread().stepReturn();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.ISuspendResume#canResume()
	 */
	public boolean canResume() {
		return getThread().canResume();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.ISuspendResume#canSuspend()
	 */
	public boolean canSuspend() {
		return getThread().canSuspend();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.ISuspendResume#isSuspended()
	 */
	public boolean isSuspended() {
		return getThread().isSuspended();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.ISuspendResume#resume()
	 */
	public void resume() throws DebugException {
		getThread().resume();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.ISuspendResume#suspend()
	 */
	public void suspend() throws DebugException {
		getThread().suspend();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.ITerminate#canTerminate()
	 */
	public boolean canTerminate() {
		return getThread().canTerminate();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.ITerminate#isTerminated()
	 */
	public boolean isTerminated() {
		return getThread().isTerminated();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.ITerminate#terminate()
	 */
	public void terminate() throws DebugException {
		getThread().terminate();
	}
	
	/**
	 * Returns the name of the buildfile this stack frame is associated
	 * with.
	 * 
	 * @return the name of the buildfile this stack frame is associated
	 * with
	 */
	public String getSourceName() {
		return fFilePath;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof AntStackFrame) {
			AntStackFrame sf = (AntStackFrame)obj;
			if (getSourceName() != null) {
				return getSourceName().equals(sf.getSourceName()) &&
					sf.getLineNumber() == getLineNumber() &&
					sf.fId == fId;
			} 
			return sf.fId == fId;
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
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
     * Returns the system, user or runtime property
     * name, or <code>null</code> if unable to resolve a property with the name.
     *
     * @param propertyName the name of the variable to search for
     * @return a property, or <code>null</code> if none
     */
    public AntProperty findProperty(String propertyName) {
        try {
            IVariable[] groups= getVariables();
            for (int i = 0; i < groups.length; i++) {
                AntProperties propertiesGrouping = (AntProperties) groups[i];
                AntPropertiesValue value= (AntPropertiesValue) propertiesGrouping.getValue();
                IVariable[] properties= value.getVariables();
                for (int j = 0; j < properties.length; j++) {
                    AntProperty property = (AntProperty) properties[j];
                    if (property.getName().equals(propertyName)) {
                        return property;
                    }
                }
            }
        } catch (DebugException e) {
        }
        return null;
    } 
}
