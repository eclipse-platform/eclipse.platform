/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ant.tests.ui.performance;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.ant.tests.ui.AbstractAntUIBuildPerformanceTest;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.test.performance.Dimension;
import org.eclipse.ui.externaltools.internal.model.IExternalToolConstants;

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
    	tagAsSummary("Simple separate JRE Build", Dimension.CPU_TIME);
    	ILaunchConfiguration config= getLaunchConfiguration("echoingSepVM");
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
    	tagAsSummary("Simple separate JRE Build set to not capture output", Dimension.CPU_TIME);
    	ILaunchConfiguration config = getLaunchConfiguration("echoingSepVM");
		assertNotNull("Could not locate launch configuration for " + "echoingSepVM", config);
		ILaunchConfigurationWorkingCopy copy= config.getWorkingCopy();
		copy.setAttribute(IExternalToolConstants.ATTR_CAPTURE_OUTPUT, false);
    	for (int i = 0; i < 10; i++) {
    	    startMeasuring();
    		for (int j = 0; j < i; j++) {
    		    launchAndTerminate(copy, 20000);
    		}
    		stopMeasuring();
		}
    	commitMeasurements();
		assertPerformance(); 	
    }
    
    /**
     * Performance test for launching Ant in a separate vm with debug information.
     */
    public void testBuildMinusDebug() throws CoreException {
    	tagAsSummary("Simple separate JRE Build with debug information", Dimension.CPU_TIME);
    	ILaunchConfiguration config = getLaunchConfiguration("echoingSepVM");
		assertNotNull("Could not locate launch configuration for " + "echoingSepVM", config);
		ILaunchConfigurationWorkingCopy copy= config.getWorkingCopy();
		copy.setAttribute(IExternalToolConstants.ATTR_TOOL_ARGUMENTS, "-debug");
    	for (int i = 0; i < 10; i++) {
    		launch(copy, 10);
        }
    	commitMeasurements();
		assertPerformance();
	}  	
}