/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.tests.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.ant.core.AntCorePreferences;
import org.eclipse.ant.core.AntRunner;
import org.eclipse.ant.core.IAntClasspathEntry;
import org.eclipse.ant.core.Property;
import org.eclipse.ant.core.TargetInfo;
import org.eclipse.ant.core.Task;
import org.eclipse.ant.core.Type;
import org.eclipse.ant.tests.core.testplugin.AntFileRunner;
import org.eclipse.ant.tests.core.testplugin.AntTestChecker;
import org.eclipse.ant.tests.core.testplugin.AntTestPlugin;
import org.eclipse.ant.tests.core.testplugin.ProjectHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.intro.IIntroManager;
import org.eclipse.ui.intro.IIntroPart;
import org.eclipse.ui.progress.UIJob;
import org.junit.Before;

/**
 * Tests for Ant core
 */
public abstract class AbstractAntTest {

	protected static final String BUILD_SUCCESSFUL = "BUILD SUCCESSFUL"; //$NON-NLS-1$
	public static final String ANT_TEST_BUILD_LOGGER = "org.eclipse.ant.tests.core.support.testloggers.TestBuildLogger"; //$NON-NLS-1$
	public static final String ANT_TEST_BUILD_LISTENER = "org.eclipse.ant.tests.core.support.testloggers.TestBuildListener"; //$NON-NLS-1$
	private static boolean welcomeClosed = false;

	@Before
	public void setUp() throws Exception {
		assertProject();
		assertWelcomeScreenClosed();
	}

	/**
	 * Ensure the welcome screen is closed because in 4.x the debug perspective opens a giant fast-view causing issues
	 * 
	 * @throws Exception
	 * @since 3.8
	 */
	void assertWelcomeScreenClosed() throws Exception {
		if (!welcomeClosed && PlatformUI.isWorkbenchRunning()) {
			final IWorkbench wb = PlatformUI.getWorkbench();
			if (wb != null) {
				UIJob job = new UIJob("close welcome screen for Ant test suite") { //$NON-NLS-1$
					@Override
					public IStatus runInUIThread(IProgressMonitor monitor) {
						IWorkbenchWindow window = wb.getActiveWorkbenchWindow();
						if (window != null) {
							IIntroManager im = wb.getIntroManager();
							IIntroPart intro = im.getIntro();
							if (intro != null) {
								welcomeClosed = im.closeIntro(intro);
							}
						}
						return Status.OK_STATUS;
					}
				};
				job.setPriority(Job.INTERACTIVE);
				job.setSystem(true);
				job.schedule();
			}
		}
	}

	/**
	 * Asserts that the test project has been created and all testing resources have been loaded each time the {@link #setUp()} method is called
	 * 
	 * @throws Exception
	 * @since 3.5
	 */
	protected void assertProject() throws Exception {
		// delete any pre-existing project
		IProject pro = ResourcesPlugin.getWorkspace().getRoot().getProject(ProjectHelper.PROJECT_NAME);
		if (!pro.exists()) {
			// create project and import build files and support files
			IProject project = ProjectHelper.createProject(ProjectHelper.PROJECT_NAME);
			IFolder folder = ProjectHelper.addFolder(project, ProjectHelper.BUILDFILES_FOLDER);
			File root = AntTestPlugin.getDefault().getFileInPlugin(ProjectHelper.TEST_BUILDFILES_DIR);
			ProjectHelper.importFilesFromDirectory(root, folder.getFullPath(), null);

			folder = ProjectHelper.addFolder(project, ProjectHelper.RESOURCES_FOLDER);
			root = AntTestPlugin.getDefault().getFileInPlugin(ProjectHelper.TEST_RESOURCES_DIR);
			ProjectHelper.importFilesFromDirectory(root, folder.getFullPath(), null);

			folder = ProjectHelper.addFolder(project, ProjectHelper.LIB_FOLDER);
			root = AntTestPlugin.getDefault().getFileInPlugin(ProjectHelper.TEST_LIB_DIR);
			ProjectHelper.importFilesFromDirectory(root, folder.getFullPath(), null);
		}
	}

	/**
	 * Returns the 'AntTests' project.
	 * 
	 * @return the test project
	 */
	protected IProject getProject() {
		return ResourcesPlugin.getWorkspace().getRoot().getProject(ProjectHelper.PROJECT_NAME);
	}

	protected IFile getBuildFile(String buildFileName) {
		IFile file = getProject().getFolder(ProjectHelper.BUILDFILES_FOLDER).getFile(buildFileName);
		assertTrue("Could not find build file named: " + buildFileName, file.exists()); //$NON-NLS-1$
		return file;
	}

	protected IFolder getWorkingDirectory(String workingDirectoryName) {
		IFolder folder = getProject().getFolder(workingDirectoryName);
		assertTrue("Could not find the working directory named: " + workingDirectoryName, folder.exists()); //$NON-NLS-1$
		return folder;
	}

	protected IFile checkFileExists(String fileName) throws CoreException {
		getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
		IFile file = getProject().getFolder(ProjectHelper.BUILDFILES_FOLDER).getFile(fileName);
		assertTrue("Could not find file named: " + fileName, file.exists()); //$NON-NLS-1$
		return file;
	}

	public void run(String buildFileName) throws CoreException {
		run(buildFileName, null, false);
	}

	public void run(String buildFileName, String[] args) throws CoreException {
		run(buildFileName, args, false);
	}

	public void run(String buildFileName, String[] args, boolean retrieveTargets) throws CoreException {
		run(buildFileName, args, retrieveTargets, ""); //$NON-NLS-1$
	}

	public void run(String buildFileName, String[] args, boolean retrieveTargets, String workingDir) throws CoreException {
		AntTestChecker.reset();
		IFile buildFile = null;
		if (buildFileName != null) {
			buildFile = getBuildFile(buildFileName);
		}
		AntFileRunner runner = new AntFileRunner();
		String[] targets = null;
		if (retrieveTargets) {
			targets = getTargetNames(buildFileName);
		}
		if (workingDir.length() > 0) {
			runner.run(buildFile, targets, args, getWorkingDirectory(workingDir).getLocation().toFile().getAbsolutePath(), true);
		} else {
			runner.run(buildFile, targets, args, workingDir, true);
		}
		assertTrue("Build starts did not equal build finishes", AntTestChecker.getDefault().getBuildsStartedCount() == AntTestChecker.getDefault().getBuildsFinishedCount()); //$NON-NLS-1$
	}

	protected TargetInfo[] getTargets(String buildFileName) throws CoreException {
		IFile buildFile = getBuildFile(buildFileName);

		AntRunner runner = new AntRunner();
		runner.setBuildFileLocation(buildFile.getLocation().toFile().getAbsolutePath());
		return runner.getAvailableTargets();
	}

	protected String[] getTargetNames(String buildFileName) throws CoreException {
		TargetInfo[] infos = getTargets(buildFileName);
		String[] names = new String[infos.length];
		for (int i = 0; i < infos.length; i++) {
			TargetInfo info = infos[i];
			names[i] = info.getName();
		}
		return names;
	}

	protected String[] getTargetDescriptions(String buildFileName) throws CoreException {
		TargetInfo[] infos = getTargets(buildFileName);
		String[] descriptions = new String[infos.length];
		for (int i = 0; i < infos.length; i++) {
			TargetInfo info = infos[i];
			descriptions[i] = info.getDescription();
		}
		return descriptions;
	}

	/**
	 * Returns the name of the project containing the given target in the given build file or <code>null</code> if no project name can be found.
	 */
	protected String getProjectName(String buildFileName, String targetName) throws CoreException {
		TargetInfo info = getTarget(buildFileName, targetName);
		if (info != null) {
			return info.getProject().getName();
		}
		return null;
	}

	/**
	 * Returns the dependencies of the target with the given name in the given build file or <code>null</code> if no such target can be found.
	 */
	protected String[] getDependencies(String buildFileName, String targetName) throws CoreException {
		TargetInfo info = getTarget(buildFileName, targetName);
		if (info != null) {
			return info.getDependencies();
		}
		return null;
	}

	/**
	 * Returns the target with the given name in the given build file or <code>null</code> if no such target can be found.
	 */
	protected TargetInfo getTarget(String buildFileName, String targetName) throws CoreException {
		for (TargetInfo info : getTargets(buildFileName)) {
			if (info.getName().equals(targetName)) {
				return info;
			}
		}
		return null;
	}

	/**
	 * Return the log message n from the last: e.g. getLoggedMessage(0) returns the most recent message
	 * 
	 * @param n
	 *              message index
	 * @return the nth last message
	 */
	protected String getLoggedMessage(int n) {
		return AntTestChecker.getDefault().getLoggedMessage(n);
	}

	protected String getLastMessageLogged() {
		return getLoggedMessage(0);
	}

	protected void assertSuccessful() {
		List<String> messages = AntTestChecker.getDefault().getMessages();
		String success = messages.get(messages.size() - 1);
		assertEquals("Build was not flagged as successful: " + success, BUILD_SUCCESSFUL, success); //$NON-NLS-1$
	}

	protected String getPropertyFileName() {
		return getProject().getFolder(ProjectHelper.RESOURCES_FOLDER).getFile("test.properties").getLocation().toFile().getAbsolutePath(); //$NON-NLS-1$
	}

	protected void restorePreferenceDefaults() {
		AntCorePreferences prefs = AntCorePlugin.getPlugin().getPreferences();
		IAntClasspathEntry toolsEntry = prefs.getToolsJarEntry();
		IAntClasspathEntry[] additionalEntries;
		if (toolsEntry == null) {
			additionalEntries = new IAntClasspathEntry[] {};
		} else {
			additionalEntries = new IAntClasspathEntry[] { toolsEntry };
		}
		prefs.setAdditionalClasspathEntries(additionalEntries);
		prefs.setAntHomeClasspathEntries(prefs.getDefaultAntHomeEntries());
		prefs.setCustomTasks(new Task[] {});
		prefs.setCustomTypes(new Type[] {});
		prefs.setCustomPropertyFiles(new String[] {});
		prefs.setCustomProperties(new Property[] {});
		prefs.setAntHome(prefs.getDefaultAntHome());
	}

	protected String getAntHome() {
		AntCorePreferences prefs = AntCorePlugin.getPlugin().getPreferences();
		IAntClasspathEntry[] entries = prefs.getAntHomeClasspathEntries();
		IAntClasspathEntry antjar = entries[0];
		IPath antHomePath = IPath.fromOSString(antjar.getEntryURL().getFile());
		antHomePath = antHomePath.removeLastSegments(1);
		return antHomePath.toFile().getAbsolutePath();
	}
}
