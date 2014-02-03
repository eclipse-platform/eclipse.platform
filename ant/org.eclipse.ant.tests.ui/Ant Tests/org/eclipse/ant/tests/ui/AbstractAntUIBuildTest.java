/*******************************************************************************
 * Copyright (c) 2003, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ant.tests.ui;

import junit.framework.TestResult;

import org.eclipse.ant.tests.ui.testplugin.AbstractAntUITest;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.IHyperlink;


public abstract class AbstractAntUIBuildTest extends AbstractAntUITest {

	/**
	 * Flag that indicates test are in progress
	 */
	protected boolean testing = true;
		
	public AbstractAntUIBuildTest(String name) {
		super(name);
	}
	
	/**
	 * Runs the test and collects the result in a TestResult without blocking
	 * the UI thread.
	 */
	public void run(final TestResult result) {
		final Display display = Display.getCurrent();
		Thread thread = null;
		try {
			Runnable r = new Runnable() {
				public void run() {
					AbstractAntUIBuildTest.super.run(result);		
					testing = false;
					display.wake();
				}
			};
			thread = new Thread(r);
			thread.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		while (testing) {
			try {
				if (!display.readAndDispatch())
					display.sleep();
			} catch (Throwable e) {
				e.printStackTrace();
			}			
		}		
	}
	
	/**
	 * Launches the Ant build with the buildfile name (no extension).
	 * Waits for all of the lines to be appended to the console.
	 * 
	 * @param buildFileName the buildfile to execute
	 * @return thread in which the first suspend event occurred
	 */
	protected void launch(String buildFileName) throws CoreException {
		super.launch(buildFileName);
	}
	
	/**
	 * Launches the launch configuration
	 * Waits for all of the lines to be appended to the console.
	 * 
	 * @param config the config to execute
	 * @return thread in which the first suspend event occurred
	 */
	protected void launch(ILaunchConfiguration config) throws CoreException {
	    launchAndTerminate(config, 20000);
	}
	
	protected void activateLink(final IHyperlink link) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				link.linkActivated();
			}
		});
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ant.tests.ui.testplugin.AbstractAntUITest#launch(java.lang.String, java.lang.String)
	 */
	protected void launch(String buildFileName, String arguments) throws CoreException {
		super.launch(buildFileName, arguments);
	}
}
