/*******************************************************************************
 * Copyright (c) 2005, 2016 IBM Corporation and others.
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
import org.eclipse.ant.launching.IAntLaunchConstants;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.core.model.IVariable;

public class PropertyTests extends AbstractAntDebugTest {

	private static final String ANT_VERSION = "Apache Ant(TM) version 1.9.6"; //$NON-NLS-1$

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
		String fileName = "breakpoints"; //$NON-NLS-1$
		ILineBreakpoint bp = createLineBreakpoint(30, "breakpoints" + ".xml"); //$NON-NLS-1$ //$NON-NLS-2$
		AntThread thread = null;
		try {
			if (sepVM) {
				fileName += "SepVM"; //$NON-NLS-1$
			}
			ILaunchConfiguration config = getLaunchConfiguration(fileName);
			ILaunchConfigurationWorkingCopy copy = config.getWorkingCopy();
			copy.setAttribute(IAntLaunchConstants.ATTR_ANT_TARGETS, "properties"); //$NON-NLS-1$
			thread = launchToLineBreakpoint(copy, bp);

			AntStackFrame frame = (AntStackFrame) thread.getTopStackFrame();
			IVariable[] vars = frame.getVariables();
			assertTrue("Should be a bunch of properties", 0 < vars.length); //$NON-NLS-1$
			AntProperty property = frame.findProperty("ant.library.dir"); //$NON-NLS-1$
			assertNotNull(property);

			assertProperty(thread, "ant.project.name", "debugEcho"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		finally {
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

	private void userProperties(boolean sepVM) throws Exception {
		String fileName = "breakpoints"; //$NON-NLS-1$
		ILineBreakpoint bp = createLineBreakpoint(30, "breakpoints" + ".xml"); //$NON-NLS-1$ //$NON-NLS-2$
		AntThread thread = null;
		try {
			if (sepVM) {
				fileName += "SepVM"; //$NON-NLS-1$
			}
			ILaunchConfiguration config = getLaunchConfiguration(fileName);
			ILaunchConfigurationWorkingCopy copy = config.getWorkingCopy();
			copy.setAttribute(IAntLaunchConstants.ATTR_ANT_TARGETS, "properties"); //$NON-NLS-1$
			thread = launchToLineBreakpoint(copy, bp);

			AntStackFrame frame = (AntStackFrame) thread.getTopStackFrame();
			IVariable[] vars = frame.getVariables();
			assertTrue("Should be a bunch of properties", 0 < vars.length); //$NON-NLS-1$
			AntProperty property = frame.findProperty("ant.home"); //$NON-NLS-1$
			assertNotNull(property);

			assertPropertyStartsWith(thread, "ant.version", ANT_VERSION); //$NON-NLS-1$
			assertProperty(thread, "ant.project.name", "debugEcho"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		finally {
			terminateAndRemove(thread);
			removeAllBreakpoints();
		}
	}

	public void testRuntimeProperties() throws Exception {
		runtimeProperties(false);
	}

	private void runtimeProperties(boolean sepVM) throws Exception, CoreException {
		String fileName = "breakpoints"; //$NON-NLS-1$
		ILineBreakpoint bp = createLineBreakpoint(30, fileName + ".xml"); //$NON-NLS-1$
		AntThread thread = null;
		try {
			if (sepVM) {
				fileName += "SepVM"; //$NON-NLS-1$
			}
			ILaunchConfiguration config = getLaunchConfiguration(fileName);
			ILaunchConfigurationWorkingCopy copy = config.getWorkingCopy();
			copy.setAttribute(IAntLaunchConstants.ATTR_ANT_TARGETS, "properties"); //$NON-NLS-1$
			thread = launchToLineBreakpoint(copy, bp);

			AntStackFrame frame = (AntStackFrame) thread.getTopStackFrame();
			IVariable[] vars = frame.getVariables();
			assertTrue("Should be a bunch of properties", 0 < vars.length); //$NON-NLS-1$
			AntProperty property = frame.findProperty("ant.home"); //$NON-NLS-1$
			assertNotNull(property);

			stepOver(frame);
			frame = assertProperty(thread, "AAA", "aaa"); //$NON-NLS-1$ //$NON-NLS-2$

			stepOver(frame);
			frame = assertProperty(thread, "BBB", "bbb"); //$NON-NLS-1$ //$NON-NLS-2$

			stepOver(frame);
			assertProperty(thread, "CCC", "ccc"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		finally {
			terminateAndRemove(thread);
			removeAllBreakpoints();
		}
	}

	private AntStackFrame assertProperty(AntThread thread, String propertyName, String propertyValue) throws DebugException {
		AntStackFrame frame = (AntStackFrame) thread.getTopStackFrame();
		AntProperty property = frame.findProperty(propertyName);
		assertNotNull("Did not find property: " + propertyName, property); //$NON-NLS-1$
		AntValue value = (AntValue) property.getValue();
		assertEquals("Value of property " + propertyName + " incorrect", propertyValue, value.getValueString()); //$NON-NLS-1$ //$NON-NLS-2$
		return frame;
	}

	private AntStackFrame assertPropertyStartsWith(AntThread thread, String propertyName, String propertyValue) throws DebugException {
		AntStackFrame frame = (AntStackFrame) thread.getTopStackFrame();
		AntProperty property = frame.findProperty(propertyName);
		assertNotNull("Did not find property: " + propertyName, property); //$NON-NLS-1$
		AntValue value = (AntValue) property.getValue();
		assertTrue("Value of property" + propertyName + " incorrect: " + value.getValueString(), value.getValueString().startsWith(propertyValue)); //$NON-NLS-1$ //$NON-NLS-2$
		return frame;
	}
}