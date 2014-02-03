/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.core;


import org.eclipse.ant.internal.core.AntObject;

/**
 * Represents an Ant task.
 * Clients may instantiate this class; it is not intended to be subclassed.
 * @since 2.1
 * @noextend This class is not intended to be subclassed by clients.
 */
public class Task extends AntObject {

	/**
	 * Returns the name of the task
	 * @return the name of the task
	 */
	public String getTaskName() {
		return fName;
	}

	/**
	 * Sets the name of the task
	 * @param taskName The taskName to set
	 */
	public void setTaskName(String taskName) {
		fName= taskName;
	}
}
