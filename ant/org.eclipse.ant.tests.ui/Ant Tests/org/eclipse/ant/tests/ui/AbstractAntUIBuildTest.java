/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ant.tests.ui;

import junit.framework.TestResult;

import org.eclipse.ant.tests.ui.testplugin.AbstractAntUITest;
import org.eclipse.ant.tests.ui.testplugin.ConsoleLineTracker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.ui.console.IConsoleHyperlink;
import org.eclipse.swt.widgets.Display;


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
	 * @param mainTypeName the program to launch
	 * @return thread in which the first suspend event occurred
	 */
	protected void launch(String buildFileName) throws CoreException {
		super.launch(buildFileName);
		ConsoleLineTracker.waitForConsole();
	}
	
	protected void activateLink(final IConsoleHyperlink link) {
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
		ConsoleLineTracker.waitForConsole();
	}
}
