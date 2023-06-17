/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
 *     Ericsson AB, Julian Enoch - Bug 465594
 *******************************************************************************/
package org.eclipse.ant.tests.core.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.ant.core.AntCorePreferences;
import org.eclipse.ant.core.AntRunner;
import org.eclipse.ant.core.IAntClasspathEntry;
import org.eclipse.ant.core.Property;
import org.eclipse.ant.core.TargetInfo;
import org.eclipse.ant.internal.core.AntClasspathEntry;
import org.eclipse.ant.internal.core.IAntCoreConstants;
import org.eclipse.ant.tests.core.AbstractAntTest;
import org.eclipse.ant.tests.core.testplugin.AntTestChecker;
import org.eclipse.ant.tests.core.testplugin.ProjectHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.junit.Test;

public class FrameworkTests extends AbstractAntTest {

	/**
	 * Ensures that the deprecated means of setting the class path still works correctly Do not fix deprecations unless the deprecated methods are
	 * being removed.
	 * 
	 * @throws MalformedURLException
	 * @throws CoreException
	 */
	@SuppressWarnings("deprecation")
	@Test
	public void testClasspathOrderingDeprecated() throws MalformedURLException, CoreException {
		AntCorePreferences prefs = AntCorePlugin.getPlugin().getPreferences();

		String path = getProject().getFolder(ProjectHelper.LIB_FOLDER).getFile("classpathOrdering1.jar").getLocation().toFile().getAbsolutePath(); //$NON-NLS-1$
		URL url = new URL(IAntCoreConstants.FILE_PROTOCOL + path);

		path = getProject().getFolder(ProjectHelper.LIB_FOLDER).getFile("classpathOrdering2.jar").getLocation().toFile().getAbsolutePath(); //$NON-NLS-1$
		URL url2 = new URL(IAntCoreConstants.FILE_PROTOCOL + path);

		URL urls[] = prefs.getCustomURLs();
		URL newUrls[] = new URL[urls.length + 2];
		System.arraycopy(urls, 0, newUrls, 0, urls.length);
		newUrls[urls.length] = url;
		newUrls[urls.length + 1] = url2;
		prefs.setCustomURLs(newUrls);

		prefs.updatePluginPreferences();

		run("ClasspathOrdering.xml"); //$NON-NLS-1$
		String msg = AntTestChecker.getDefault().getMessages().get(1);
		assertTrue("Message incorrect: " + msg, msg.equals("classpathOrdering1")); //$NON-NLS-1$ //$NON-NLS-2$
		assertSuccessful();

		restorePreferenceDefaults();

		urls = prefs.getCustomURLs();
		newUrls = new URL[urls.length + 2];
		System.arraycopy(urls, 0, newUrls, 0, urls.length);
		newUrls[urls.length] = url2;
		newUrls[urls.length + 1] = url;
		prefs.setCustomURLs(newUrls);

		prefs.updatePluginPreferences();

		run("ClasspathOrdering.xml"); //$NON-NLS-1$
		msg = AntTestChecker.getDefault().getMessages().get(1);
		assertTrue("Message incorrect: " + msg, msg.equals("classpathOrdering2")); //$NON-NLS-1$ //$NON-NLS-2$
		assertSuccessful();
		restorePreferenceDefaults();
	}

	@Test
	public void testClasspathOrdering() throws CoreException {
		AntCorePreferences prefs = AntCorePlugin.getPlugin().getPreferences();

		String path = getProject().getFolder(ProjectHelper.LIB_FOLDER).getFile("classpathOrdering1.jar").getLocation().toFile().getAbsolutePath(); //$NON-NLS-1$
		IAntClasspathEntry entry = new AntClasspathEntry(path);

		path = getProject().getFolder(ProjectHelper.LIB_FOLDER).getFile("classpathOrdering2.jar").getLocation().toFile().getAbsolutePath(); //$NON-NLS-1$
		IAntClasspathEntry entry2 = new AntClasspathEntry(path);

		IAntClasspathEntry entries[] = prefs.getAdditionalClasspathEntries();
		IAntClasspathEntry newEntries[] = new IAntClasspathEntry[entries.length + 2];
		System.arraycopy(entries, 0, newEntries, 0, entries.length);
		newEntries[entries.length] = entry;
		newEntries[entries.length + 1] = entry2;
		prefs.setAdditionalClasspathEntries(newEntries);

		prefs.updatePluginPreferences();

		run("ClasspathOrdering.xml"); //$NON-NLS-1$
		String msg = AntTestChecker.getDefault().getMessages().get(1);
		assertTrue("Message incorrect: " + msg, msg.equals("classpathOrdering1")); //$NON-NLS-1$ //$NON-NLS-2$
		assertSuccessful();

		restorePreferenceDefaults();

		entries = prefs.getAdditionalClasspathEntries();
		newEntries = new IAntClasspathEntry[entries.length + 2];
		System.arraycopy(entries, 0, newEntries, 0, entries.length);
		newEntries[entries.length] = entry2;
		newEntries[entries.length + 1] = entry;
		prefs.setAdditionalClasspathEntries(newEntries);

		prefs.updatePluginPreferences();

		run("ClasspathOrdering.xml"); //$NON-NLS-1$
		msg = AntTestChecker.getDefault().getMessages().get(1);
		assertTrue("Message incorrect: " + msg, msg.equals("classpathOrdering2")); //$NON-NLS-1$ //$NON-NLS-2$
		assertSuccessful();
		restorePreferenceDefaults();
	}

	@Test
	public void testNoDefaultTarget() throws CoreException {
		run("NoDefault.xml"); //$NON-NLS-1$
		assertSuccessful();
	}

	/**
	 * Regression test for running a specific target from a script that has no default target. Associated with bug 294502.
	 * 
	 * @throws CoreException
	 */
	@Test
	public void testSpecificTargetWithNoDefaultTarget() throws CoreException {
		run("NoDefault.xml", new String[] { "test" }); //$NON-NLS-1$ //$NON-NLS-2$
		assertSuccessful();
	}

	/**
	 * Ensures that tasks like javac work when includeAntRuntime is specified bug 20857. This test will just return if the tests are conducted on a
	 * JRE (no tools.jar).
	 */
	@Test
	public void testIncludeAntRuntime() throws CoreException {
		IAntClasspathEntry toolsEntry = AntCorePlugin.getPlugin().getPreferences().getToolsJarEntry();
		if (toolsEntry == null) {
			// running on a JRE where tools.jar could not be found
			return;
		}
		run("javac.xml", new String[] { "build", "refresh" }, false); // standard compiler //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertSuccessful();
		IFile classFile = getProject().getFolder("temp.folder").getFolder("javac.bin").getFile("AntTestTask.class"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("Class file was not generated", classFile.exists()); //$NON-NLS-1$
		run("javac.xml", new String[] { "-Duse.eclipse.compiler=true", "clean", "build", "refresh" }, false); // JDTCompiler //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		assertSuccessful();
		classFile = getProject().getFolder("temp.folder").getFolder("javac.bin").getFile("AntTestTask.class"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("Class file was not generated", classFile.exists()); //$NON-NLS-1$
	}

	/**
	 * Tests the properties added using a global property file
	 */
	@Test
	public void testGlobalPropertyFile() throws CoreException {

		AntCorePreferences prefs = AntCorePlugin.getPlugin().getPreferences();

		String path = getPropertyFileName();
		prefs.setCustomPropertyFiles(new String[] { path });

		run("TestForEcho.xml", new String[] {}); //$NON-NLS-1$
		assertSuccessful();
		assertTrue("eclipse.is.cool should have been set as Yep", "Yep".equals(AntTestChecker.getDefault().getUserProperty("eclipse.is.cool"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("AntTests should have a value of testing", "testing from properties file".equals(AntTestChecker.getDefault().getUserProperty("AntTests"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertNull("my.name was not set and should be null", AntTestChecker.getDefault().getUserProperty("my.name")); //$NON-NLS-1$ //$NON-NLS-2$

		restorePreferenceDefaults();
	}

	/**
	 * Tests the properties added using a global property
	 */
	@Test
	public void testGlobalProperty() throws CoreException {

		AntCorePreferences prefs = AntCorePlugin.getPlugin().getPreferences();
		prefs.setCustomProperties(new Property[] { new Property("eclipse.is.cool", "Yep"), new Property("JUnitTest", "true") }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

		run("TestForEcho.xml", new String[] {}); //$NON-NLS-1$
		assertSuccessful();
		assertTrue("eclipse.is.cool should have been set as Yep", "Yep".equals(AntTestChecker.getDefault().getUserProperty("eclipse.is.cool"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("JUnitTests should have a value of true", "true".equals(AntTestChecker.getDefault().getUserProperty("JUnitTest"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertNull("my.name was not set and should be null", AntTestChecker.getDefault().getUserProperty("my.name")); //$NON-NLS-1$ //$NON-NLS-2$

		restorePreferenceDefaults();
	}

	@Test
	public void testGlobalPropertyFileWithMinusDTakingPrecedence() throws CoreException {
		AntCorePreferences prefs = AntCorePlugin.getPlugin().getPreferences();

		String path = getPropertyFileName();
		prefs.setCustomPropertyFiles(new String[] { path });

		run("echoing.xml", new String[] { "-DAntTests=testing", "-Declipse.is.cool=true" }, false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertSuccessful();
		assertTrue("eclipse.is.cool should have been set as true", "true".equals(AntTestChecker.getDefault().getUserProperty("eclipse.is.cool"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("AntTests should have a value of testing", "testing".equals(AntTestChecker.getDefault().getUserProperty("AntTests"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertNull("my.name was not set and should be null", AntTestChecker.getDefault().getUserProperty("my.name")); //$NON-NLS-1$ //$NON-NLS-2$
		restorePreferenceDefaults();
	}

	/**
	 * Tests that the default ANT_HOME is set and that it can be changed
	 */
	@Test
	public void testSettingAntHome() throws CoreException {
		try {
			AntCorePreferences prefs = AntCorePlugin.getPlugin().getPreferences();
			run("echoing.xml"); //$NON-NLS-1$
			assertTrue("ANT_HOME not set correctly", prefs.getDefaultAntHome().equals(System.getProperty("ant.home"))); //$NON-NLS-1$ //$NON-NLS-2$
			File antLibDir = new File(prefs.getDefaultAntHome(), ProjectHelper.LIB_FOLDER);
			assertTrue("ant.library.dir not set correctly", antLibDir.getAbsolutePath().equals(System.getProperty("ant.library.dir"))); //$NON-NLS-1$ //$NON-NLS-2$
			prefs.setAntHome(""); //$NON-NLS-1$
			run("echoing.xml"); //$NON-NLS-1$
			assertTrue("ANT_HOME not set correctly", null == System.getProperty("ant.home")); //$NON-NLS-1$ //$NON-NLS-2$
			assertTrue("ant.library.dir not set correctly", null == System.getProperty("ant.library.dir")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		finally {
			restorePreferenceDefaults();
		}
	}

	/**
	 * Tests retrieving target info using AntRunner Covers bug 73602 at the same time
	 */
	@Test
	public void testGetTargets() throws CoreException {

		AntCorePreferences prefs = AntCorePlugin.getPlugin().getPreferences();

		String path = getProject().getFolder("resources").getLocation().toFile().getAbsolutePath(); //$NON-NLS-1$
		IAntClasspathEntry entry = new AntClasspathEntry(path);

		IAntClasspathEntry entries[] = prefs.getAdditionalClasspathEntries();
		IAntClasspathEntry newEntries[] = new IAntClasspathEntry[entries.length + 1];
		System.arraycopy(entries, 0, newEntries, 0, entries.length);
		newEntries[entries.length] = entry;
		prefs.setAdditionalClasspathEntries(newEntries);

		prefs.updatePluginPreferences();

		AntRunner runner = new AntRunner();
		IFile buildFile = getBuildFile("Bug73602.xml"); //$NON-NLS-1$
		if (buildFile != null) {
			runner.setBuildFileLocation(buildFile.getLocation().toFile().toString());
		}
		TargetInfo[] infos = runner.getAvailableTargets();
		assertTrue("incorrect number of targets retrieved", infos != null && infos.length == 3); //$NON-NLS-1$
	}

	/**
	 * Tests bug 389564 for a class path entry as a url representing a remote file system location
	 * 
	 * @throws MalformedURLException
	 */
	@Test
	public void testAntClasspathEntryFromUrl() throws MalformedURLException {

		AntCorePreferences prefs = AntCorePlugin.getPlugin().getPreferences();

		String path = "//hub/home/tom/.eclipse/3.8/configuration/org.eclipse.osgi/bundles/21/2/.cp/lib/remote.jar"; //$NON-NLS-1$
		URL url = new URL(IAntCoreConstants.FILE_PROTOCOL + path);
		IAntClasspathEntry entry = new AntClasspathEntry(url);

		IAntClasspathEntry[] entries = prefs.getAntHomeClasspathEntries();
		IAntClasspathEntry[] newEntries = new IAntClasspathEntry[entries.length + 1];
		System.arraycopy(entries, 0, newEntries, 0, entries.length);
		newEntries[entries.length] = entry;
		prefs.setAntHomeClasspathEntries(newEntries);

		IAntClasspathEntry resultedEntries[] = prefs.getAntHomeClasspathEntries();
		int index = resultedEntries[entries.length].getLabel().indexOf("hub"); //$NON-NLS-1$
		assertNotSame("Missing machine details", index, -1); //$NON-NLS-1$
		assertFalse("Incorrect classpath entry. This would have been the value before the fix", resultedEntries[entries.length].getLabel().equals(IPath.fromOSString("/home/tom/.eclipse/3.8/configuration/org.eclipse.osgi/bundles/21/2/.cp/lib/remote.jar").toOSString())); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Incorrect classpath entry", resultedEntries[entries.length].getLabel().substring(index).equals(IPath.fromOSString("hub/home/tom/.eclipse/3.8/configuration/org.eclipse.osgi/bundles/21/2/.cp/lib/remote.jar").toOSString())); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
