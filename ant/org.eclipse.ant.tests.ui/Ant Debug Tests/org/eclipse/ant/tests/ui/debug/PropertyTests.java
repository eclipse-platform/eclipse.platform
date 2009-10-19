/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.tests.ui.debug;

import org.eclipse.ant.internal.launching.debug.model.AntProperty;
import org.eclipse.ant.internal.launching.debug.model.AntStackFrame;
import org.eclipse.ant.internal.launching.debug.model.AntThread;
import org.eclipse.ant.internal.launching.debug.model.AntValue;
import org.eclipse.ant.internal.ui.launchConfigurations.IAntLaunchConfigurationConstants;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.core.model.IVariable;

public class PropertyTests extends AbstractAntDebugTest {
	
	private static final String ANT_VERSION = "Apache Ant version 1.7.0 compiled on December 13 2006";

    public PropertyTests(String name) {
		super(name);
	}

	public void testSystemProperties() throws Exception {
		systemProperties(false);
	}

	public void testSystemPropertiesSepVM() throws Exception {
		systemProperties(true);
	}

	private void systemProperties(boolean sepVM) throws Exception, CoreException {
		String fileName = "breakpoints";
		ILineBreakpoint bp = createLineBreakpoint(30, "breakpoints" + ".xml");
		AntThread thread= null;
		try {
			if (sepVM) {
				fileName+= "SepVM";
			}
			ILaunchConfiguration config= getLaunchConfiguration(fileName);
			ILaunchConfigurationWorkingCopy copy= config.getWorkingCopy();
			copy.setAttribute(IAntLaunchConfigurationConstants.ATTR_ANT_TARGETS, "properties");
			thread= launchToLineBreakpoint(copy, bp);

			AntStackFrame frame = (AntStackFrame)thread.getTopStackFrame();
			IVariable[] vars = frame.getVariables();
			assertTrue("Should be a bunch of properties", 0 < vars.length);
			AntProperty property= frame.findProperty("ant.library.dir");
			assertNotNull(property);
			
			assertProperty(thread, "ant.project.name", "debugEcho");
		} finally {
			terminateAndRemove(thread);
			removeAllBreakpoints();
		}
	}
	
	public void testUserProperties() throws Exception {
		userProperties(false);		
	}
	
	public void testUserPropertiesSepVM() throws Exception {
		userProperties(true);
	}

	private void userProperties(boolean sepVM) throws Exception{
		String fileName = "breakpoints";
		ILineBreakpoint bp = createLineBreakpoint(30, "breakpoints" + ".xml");
		AntThread thread= null;
		try {
			if (sepVM) {
				fileName+= "SepVM";
			}
			ILaunchConfiguration config= getLaunchConfiguration(fileName);
			ILaunchConfigurationWorkingCopy copy= config.getWorkingCopy();
			copy.setAttribute(IAntLaunchConfigurationConstants.ATTR_ANT_TARGETS, "properties");
			thread= launchToLineBreakpoint(copy, bp);

			AntStackFrame frame = (AntStackFrame)thread.getTopStackFrame();
			IVariable[] vars = frame.getVariables();
			assertTrue("Should be a bunch of properties", 0 < vars.length);
			AntProperty property= frame.findProperty("ant.home");
			assertNotNull(property);
			
			assertProperty(thread, "ant.version", ANT_VERSION);
			assertProperty(thread, "ant.project.name", "debugEcho");
		} finally {
			terminateAndRemove(thread);
			removeAllBreakpoints();
		}
	}
	
	public void testRuntimeProperties() throws Exception {
		runtimeProperties(false);		
	}

    //TODO timing issues with retrieving properties
//	public void testRuntimePropertiesSepVM() throws Exception {
//		runtimeProperties(true);		
//	}

	private void runtimeProperties(boolean sepVM) throws Exception, CoreException {
		String fileName = "breakpoints";
		ILineBreakpoint bp = createLineBreakpoint(30, fileName + ".xml");
		AntThread thread= null;
		try {
			if (sepVM) {
				fileName+="SepVM";
			}
			ILaunchConfiguration config= getLaunchConfiguration(fileName);
			ILaunchConfigurationWorkingCopy copy= config.getWorkingCopy();
			copy.setAttribute(IAntLaunchConfigurationConstants.ATTR_ANT_TARGETS, "properties");
			thread= launchToLineBreakpoint(copy, bp);

			AntStackFrame frame = (AntStackFrame)thread.getTopStackFrame();
			IVariable[] vars = frame.getVariables();
			assertTrue("Should be a bunch of properties", 0 < vars.length);
			AntProperty property= frame.findProperty("ant.home");
			assertNotNull(property);
			
			stepOver(frame);
			frame = assertProperty(thread, "AAA", "aaa");
			
			stepOver(frame);
			frame = assertProperty(thread, "BBB", "bbb");
			
			stepOver(frame);
			assertProperty(thread, "CCC", "ccc");
		} finally {
			terminateAndRemove(thread);
			removeAllBreakpoints();
		}
	}

	private AntStackFrame assertProperty(AntThread thread, String propertyName, String propertyValue) throws DebugException {
		AntStackFrame frame = (AntStackFrame)thread.getTopStackFrame();
		AntProperty property= frame.findProperty(propertyName);
		assertNotNull("Did not find property: " + propertyName, property);
		AntValue value= (AntValue) property.getValue();
		assertTrue("Value of property" + propertyName + " incorrect: " + value.getValueString(), propertyValue.equals(value.getValueString()));
		return frame;
	}
}