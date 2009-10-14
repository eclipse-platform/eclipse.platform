/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ant.tests.ui.editor.performance;

import java.io.File;

import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.preferences.AntEditorPreferenceConstants;
import org.eclipse.ant.tests.ui.testplugin.ProjectHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.PerformanceTestCase;
import org.eclipse.ui.PartInitException;

public class OpenAntEditorTest extends PerformanceTestCase {
	
	protected void setUp() throws Exception {
		super.setUp();
		EditorTestHelper.runEventQueue();
	}

	public void testOpenAntEditor1() throws PartInitException {
		// cold run
		IFile file= getIFile("build.xml");
		measureOpenInEditor(file);
	}
	
	public void testOpenAntEditor2() throws PartInitException {
		// warm run
		IFile file= getIFile("build.xml");
		tagAsGlobalSummary("Open Ant Editor", Dimension.ELAPSED_PROCESS);
		measureOpenInEditor(file);
	}
	
	public void testOpenAntEditorNoFolding() throws PartInitException {
	    IPreferenceStore store= AntUIPlugin.getDefault().getPreferenceStore();
	    try {
		IFile file= getIFile("build.xml");
		store.setValue(AntEditorPreferenceConstants.EDITOR_FOLDING_ENABLED, false);
		tagAsSummary("Open Ant Editor; No folding", Dimension.ELAPSED_PROCESS);
		measureOpenInEditor(file);
	    } finally {
	        store.setToDefault(AntEditorPreferenceConstants.EDITOR_FOLDING_ENABLED);
	    }
	}
	
	protected IFile getIFile(String buildFileName) {
		return getProject().getFolder("buildfiles").getFolder("performance").getFile(buildFileName);	
	}
	
	protected File getBuildFile(String buildFileName) {
		IFile file = getIFile(buildFileName);
		assertTrue("Could not find build file named: " + buildFileName, file.exists());
		return file.getLocation().toFile();
	}
	
	/**
	 * Returns the 'AntUITests' project.
	 * 
	 * @return the test project
	 */
	protected IProject getProject() {
		return ResourcesPlugin.getWorkspace().getRoot().getProject(ProjectHelper.PROJECT_NAME);
	}
	
	protected void measureOpenInEditor(IFile file) throws PartInitException {
		try {
			for (int i= 0; i < 15; i++) {
				startMeasuring();
				EditorTestHelper.openInEditor(file, true);
				stopMeasuring();
				EditorTestHelper.closeAllEditors();
				sleep(2000); // NOTE: runnables posted from other threads, while the main thread waits here, are executed and measured only in the next iteration
			}
			 commitMeasurements();
	 		 assertPerformance();
		} finally {
			EditorTestHelper.closeAllEditors();
		}
	}
	
	private synchronized void sleep(int time) {
		try {
			wait(time);
		} catch (InterruptedException e) {
		}
	}
}
