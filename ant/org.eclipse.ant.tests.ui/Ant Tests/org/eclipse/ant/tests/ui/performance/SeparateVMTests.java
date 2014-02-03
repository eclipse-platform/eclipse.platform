/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ant.tests.ui.performance;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.ant.tests.ui.AbstractAntUIBuildPerformanceTest;
import org.eclipse.core.externaltools.internal.IExternalToolConstants;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.IDebugUIConstants;

public class SeparateVMTests extends AbstractAntUIBuildPerformanceTest {
		
    public SeparateVMTests(String name) {
        super(name);
    }
    
	public static Test suite() {
		return new TestSuite(SeparateVMTests.class);
	}

    /**
     * Performance test for launching Ant in a separate vm.
     */
	public void testBuild() throws CoreException {
    	//tagAsSummary("Separate JRE Build", Dimension.ELAPSED_PROCESS);
    	ILaunchConfiguration config= getLaunchConfiguration("echoingSepVM");
    	//possible first time hit of the SWT pieces getting written from the JAR to the 
    	//metadata area
    	launchAndTerminate(config, 20000);
    	for (int i = 0; i < 10; i++) {
    		launch(config, 10);
		}
    	commitMeasurements();
		assertPerformance(); 	
    }
	
	 /**
     * Performance test for launching Ant in a separate vm with no console output.
     */
	public void testBuildNoConsole() throws CoreException {
    	//tagAsSummary("Separate JRE Build; capture output off", Dimension.ELAPSED_PROCESS);
    	ILaunchConfiguration config = getLaunchConfiguration("echoingSepVM");
		assertNotNull("Could not locate launch configuration for " + "echoingSepVM", config);
		ILaunchConfigurationWorkingCopy copy= config.getWorkingCopy();
		copy.setAttribute(IDebugUIConstants.ATTR_CAPTURE_IN_CONSOLE, false);
		copy.setAttribute(DebugPlugin.ATTR_CAPTURE_OUTPUT, false);
		//possible first time hit of the SWT pieces getting written from the JAR to the 
    	//metadata area
		launchAndTerminate(copy, 20000);
		for (int i = 0; i < 10; i++) {
    		launch(copy, 10);
        }
    	commitMeasurements();
		assertPerformance(); 	
    }
    
    /**
     * Performance test for launching Ant in a separate vm with debug information.
     */
    public void testBuildMinusDebug() throws CoreException {
    	//tagAsSummary("Separate JRE Build; -debug", Dimension.ELAPSED_PROCESS);
    	ILaunchConfiguration config = getLaunchConfiguration("echoingSepVM");
		assertNotNull("Could not locate launch configuration for " + "echoingSepVM", config);
		ILaunchConfigurationWorkingCopy copy= config.getWorkingCopy();
		copy.setAttribute(IExternalToolConstants.ATTR_TOOL_ARGUMENTS, "-debug");
		//possible first time hit of the SWT pieces getting written from the JAR to the 
    	//metadata area
		launchAndTerminate(copy, 20000);
    	for (int i = 0; i < 10; i++) {
    		launch(copy, 10);
        }
    	commitMeasurements();
		assertPerformance();
	}  	
    
     /**
     * Performance test for launching Ant in a separate vm with lots of links
     */
    public void testBuildWithLotsOfLinks() throws CoreException {
        //tagAsSummary("Separate JRE Build; links", Dimension.ELAPSED_PROCESS);
        ILaunchConfiguration config = getLaunchConfiguration("echoPropertiesSepVM");
        //possible first time hit of the SWT pieces getting written from the JAR to the 
        //metadata area
        launchAndTerminate(config, 20000);
        for (int i = 0; i < 10; i++) {
            launch(config, 10);
        }
        commitMeasurements();
        assertPerformance();
    }   
}
