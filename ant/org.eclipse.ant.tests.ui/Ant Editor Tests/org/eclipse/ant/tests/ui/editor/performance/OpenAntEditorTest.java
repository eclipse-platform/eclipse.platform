/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
import org.eclipse.ant.tests.ui.performance.AbstractAntPerformanceTest;
import org.eclipse.ant.tests.ui.testplugin.ProjectHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.test.performance.Dimension;
import org.eclipse.ui.PartInitException;

public class OpenAntEditorTest extends AbstractAntPerformanceTest {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		EditorTestHelper.runEventQueue();
	}

	public void testOpenAntEditor1() throws Exception {
		// cold run
		IFile file = getIFile("build.xml"); //$NON-NLS-1$
		measureOpenInEditor(file);
	}

	public void testOpenAntEditor2() throws Exception {
		// warm run
		IFile file = getIFile("build.xml"); //$NON-NLS-1$
		tagAsGlobalSummary("Open Ant Editor", Dimension.ELAPSED_PROCESS); //$NON-NLS-1$
		measureOpenInEditor(file);
	}

	public void testOpenAntEditorNoFolding() throws Exception {
		IPreferenceStore store = AntUIPlugin.getDefault().getPreferenceStore();
		try {
			IFile file = getIFile("build.xml"); //$NON-NLS-1$
			store.setValue(AntEditorPreferenceConstants.EDITOR_FOLDING_ENABLED, false);
			tagAsSummary("Open Ant Editor; No folding", Dimension.ELAPSED_PROCESS); //$NON-NLS-1$
			measureOpenInEditor(file);
		}
		finally {
			store.setToDefault(AntEditorPreferenceConstants.EDITOR_FOLDING_ENABLED);
		}
	}

	protected IFile getIFile(String buildFileName) {
		return getProject().getFolder("buildfiles").getFolder("performance").getFile(buildFileName); //$NON-NLS-1$ //$NON-NLS-2$
	}

	protected File getBuildFile(String buildFileName) {
		IFile file = getIFile(buildFileName);
		assertTrue("Could not find build file named: " + buildFileName, file.exists()); //$NON-NLS-1$
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

	synchronized protected void measureOpenInEditor(IFile file) throws PartInitException, InterruptedException {
		try {
			for (int i = 0; i < 15; i++) {
				startMeasuring();
				EditorTestHelper.openInEditor(file, true);
				stopMeasuring();
				EditorTestHelper.closeAllEditors();
				wait(2000); // NOTE: runnables posted from other threads, while the main thread waits here, are executed and measured only in the next
				// iteration
			}
			commitMeasurements();
			assertPerformance();
		}
		finally {
			EditorTestHelper.closeAllEditors();
		}
	}
}
