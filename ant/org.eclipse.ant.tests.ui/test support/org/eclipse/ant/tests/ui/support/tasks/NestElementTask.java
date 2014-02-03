/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ant.tests.ui.support.tasks;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Echo;

public class NestElementTask extends Task {  
	
	String message= "bar";
	NestedElement e;
    public static class NestedElement{
    	Boolean works= Boolean.FALSE;
    	public NestedElement() {	
    	}
    	
        public void setWorks(Boolean booleanValue) {
        	works= booleanValue;
        }

		public boolean works() {
			return works.booleanValue();
		}
    }
    public void addNestedElement(NestedElement nestedElement) {
    	e= nestedElement;
    }
   
	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Task#execute()
	 */
	public void execute() throws BuildException {
		Echo echo= new Echo();
		echo.setProject(getProject());
		if (e.works()) {
			echo.setMessage(message);
		} else {
			echo.setMessage("ack");
		}
		echo.execute();
	}
}
