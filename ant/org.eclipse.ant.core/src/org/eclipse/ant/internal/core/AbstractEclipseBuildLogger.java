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
package org.eclipse.ant.internal.core;

import java.util.Map;

import org.eclipse.ant.core.AntCorePlugin;

public abstract class AbstractEclipseBuildLogger {
    
    /**
     * Process identifier - used to link the Eclipse Ant build
     * loggers to a process.
     */
    public static final String ANT_PROCESS_ID = AntCorePlugin.PI_ANTCORE + ".ANT_PROCESS_ID"; //$NON-NLS-1$
    
    protected String fProcessId= null;
    
    public void configure(Map userProperties) {
        fProcessId= (String) userProperties.remove(ANT_PROCESS_ID);
    } 
}