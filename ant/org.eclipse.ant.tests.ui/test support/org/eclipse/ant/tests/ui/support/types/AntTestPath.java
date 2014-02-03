/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.tests.ui.support.types;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;

public class AntTestPath extends Path {
	
	String message;
	
	public AntTestPath(Project project) {
		super(project);
	}
	
	public void setMessage(String message) {
		this.message= message;
	}
	
	public String getMessage() {
		return message;
	}
	
}
