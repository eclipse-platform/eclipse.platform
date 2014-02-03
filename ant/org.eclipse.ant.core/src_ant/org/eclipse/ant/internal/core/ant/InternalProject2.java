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

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;

public class InternalProject2 extends Project {

    /* (non-Javadoc)
     * @see org.apache.tools.ant.Project#createClassLoader(org.apache.tools.ant.types.Path)
     */
    public AntClassLoader createClassLoader(Path path) {
    	AntClassLoader loader= super.createClassLoader(path);
    	if (path == null) {
    		//use the "fake" Eclipse runtime classpath for Ant
    		loader.setClassPath(Path.systemClasspath);
    	}
        
        return loader;
    }
}