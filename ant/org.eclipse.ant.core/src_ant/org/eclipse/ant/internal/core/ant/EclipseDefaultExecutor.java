/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.core.ant;

import java.util.Arrays;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Executor;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.helper.DefaultExecutor;

public class EclipseDefaultExecutor extends DefaultExecutor {

    private static final EclipseSingleCheckExecutor SUB_EXECUTOR = new EclipseSingleCheckExecutor();
    
    /* (non-Javadoc)
     * @see org.apache.tools.ant.Executor#executeTargets(org.apache.tools.ant.Project, java.lang.String[])
     */
    public void executeTargets(Project project, String[] targetNames) throws BuildException {
        Vector v= new Vector();
        v.addAll(Arrays.asList(targetNames));
        project.addReference("eclipse.ant.targetVector", v); //$NON-NLS-1$
        super.executeTargets(project, targetNames);
    }
    
    /* (non-Javadoc)
     * @see org.apache.tools.ant.Executor#getSubProjectExecutor()
     */
    public Executor getSubProjectExecutor() {
       return SUB_EXECUTOR;
    }
}
