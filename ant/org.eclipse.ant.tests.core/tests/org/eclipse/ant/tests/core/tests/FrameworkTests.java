/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.tests.core.tests;


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
import org.eclipse.ant.tests.core.AbstractAntTest;
import org.eclipse.ant.tests.core.testplugin.AntTestChecker;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

public class FrameworkTests extends AbstractAntTest {
	
	public FrameworkTests(String name) {
		super(name);
	}
	
	/**
	 * Ensures that the deprecated means of setting the classpath still works correctly
	 * Do not fix deprecations unless the deprecated methods are being removed.
	 * @throws MalformedURLException
	 * @throws CoreException
	 */
	public void testClasspathOrderingDeprecated() throws MalformedURLException, CoreException {
		AntCorePreferences prefs =AntCorePlugin.getPlugin().getPreferences();
		
		String path= getProject().getFolder("lib").getFile("classpathOrdering1.jar").getLocation().toFile().getAbsolutePath();
		URL url= new URL("file:" + path);
		
		path= getProject().getFolder("lib").getFile("classpathOrdering2.jar").getLocation().toFile().getAbsolutePath();
		URL url2= new URL("file:" + path);
		
		URL urls[] = prefs.getCustomURLs();
		URL newUrls[] = new URL[urls.length + 2];
		System.arraycopy(urls, 0, newUrls, 0, urls.length);
		newUrls[urls.length] = url;
		newUrls[urls.length + 1] = url2;
		prefs.setCustomURLs(newUrls);
		
		prefs.updatePluginPreferences();
		
		run("ClasspathOrdering.xml");
		String msg= (String)AntTestChecker.getDefault().getMessages().get(1);
		assertTrue("Message incorrect: " + msg, msg.equals("classpathOrdering1"));
		assertSuccessful();
		
		restorePreferenceDefaults();
		
		urls = prefs.getCustomURLs();
		newUrls = new URL[urls.length + 2];
		System.arraycopy(urls, 0, newUrls, 0, urls.length);
		newUrls[urls.length] = url2;
		newUrls[urls.length + 1] = url;
		prefs.setCustomURLs(newUrls);
		
		prefs.updatePluginPreferences();
		
		run("ClasspathOrdering.xml");
		msg= (String)AntTestChecker.getDefault().getMessages().get(1);
		assertTrue("Message incorrect: " + msg, msg.equals("classpathOrdering2"));
		assertSuccessful();
		restorePreferenceDefaults();
	}
	
	public void testClasspathOrdering() throws CoreException {
		AntCorePreferences prefs =AntCorePlugin.getPlugin().getPreferences();
		
		String path= getProject().getFolder("lib").getFile("classpathOrdering1.jar").getLocation().toFile().getAbsolutePath();
		IAntClasspathEntry entry= new AntClasspathEntry(path);
		
		path= getProject().getFolder("lib").getFile("classpathOrdering2.jar").getLocation().toFile().getAbsolutePath();
		IAntClasspathEntry entry2= new AntClasspathEntry(path);
		
		IAntClasspathEntry entries[] = prefs.getAdditionalClasspathEntries();
		IAntClasspathEntry newEntries[] = new IAntClasspathEntry[entries.length + 2];
		System.arraycopy(entries, 0, newEntries, 0, entries.length);
		newEntries[entries.length] = entry;
		newEntries[entries.length + 1] = entry2;
		prefs.setAdditionalClasspathEntries(newEntries);
		
		prefs.updatePluginPreferences();
		
		run("ClasspathOrdering.xml");
		String msg= (String)AntTestChecker.getDefault().getMessages().get(1);
		assertTrue("Message incorrect: " + msg, msg.equals("classpathOrdering1"));
		assertSuccessful();
		
		restorePreferenceDefaults();
		
		entries = prefs.getAdditionalClasspathEntries();
		newEntries = new IAntClasspathEntry[entries.length + 2];
		System.arraycopy(entries, 0, newEntries, 0, entries.length);
		newEntries[entries.length] = entry2;
		newEntries[entries.length + 1] = entry;
		prefs.setAdditionalClasspathEntries(newEntries);
		
		prefs.updatePluginPreferences();
		
		run("ClasspathOrdering.xml");
		msg= (String)AntTestChecker.getDefault().getMessages().get(1);
		assertTrue("Message incorrect: " + msg, msg.equals("classpathOrdering2"));
		assertSuccessful();
		restorePreferenceDefaults();
	}
    
    public void testNoDefaultTarget() throws CoreException {
        run("NoDefault.xml");
        assertSuccessful();
    }
    
    /**
     * Regression test for running a specific target from a script that has no default target.
     * Associated with bug 294502.
     * 
     * @throws CoreException
     */
    public void testSpecificTargetWithNoDefaultTarget() throws CoreException {
    	run("NoDefault.xml", new String[]{"test"});
    	assertSuccessful();
    }
	
	/**
	 * Ensures that tasks like javac work when includeAntRuntime is specified
	 * bug 20857.
	 * This test will just return if the tests are conducted on a JRE (no tools.jar).
	 */
	public void testIncludeAntRuntime() throws CoreException {
		IAntClasspathEntry toolsEntry= AntCorePlugin.getPlugin().getPreferences().getToolsJarEntry();
		if (toolsEntry == null) {
			//running on a JRE where tools.jar could not be found
			return;
		}
		run("javac.xml", new String[]{"build","refresh"}, false); //standard compiler
		assertSuccessful();
		IFile classFile= getProject().getFolder("temp.folder").getFolder("javac.bin").getFile("AntTestTask.class");
		assertTrue("Class file was not generated", classFile.exists());
		run("javac.xml", new String[]{"-Duse.eclipse.compiler=true", "clean", "build", "refresh"}, false); //JDTCompiler
		assertSuccessful();
		classFile= getProject().getFolder("temp.folder").getFolder("javac.bin").getFile("AntTestTask.class");
		assertTrue("Class file was not generated", classFile.exists());
	}
	
	/**
	 * Tests the properties added using a global property file
	 */
	public void testGlobalPropertyFile() throws CoreException {
		
		AntCorePreferences prefs =AntCorePlugin.getPlugin().getPreferences();
		
		String path= getPropertyFileName();
		prefs.setCustomPropertyFiles(new String[]{path});
		
		run("TestForEcho.xml", new String[]{});
		assertSuccessful();
		assertTrue("eclipse.is.cool should have been set as Yep", "Yep".equals(AntTestChecker.getDefault().getUserProperty("eclipse.is.cool")));
		assertTrue("AntTests should have a value of testing", "testing from properties file".equals(AntTestChecker.getDefault().getUserProperty("AntTests")));
		assertNull("my.name was not set and should be null", AntTestChecker.getDefault().getUserProperty("my.name"));
		
		restorePreferenceDefaults();
	}
	
	/**
	 * Tests the properties added using a global property
	 */
	public void testGlobalProperty() throws CoreException {
		
		AntCorePreferences prefs =AntCorePlugin.getPlugin().getPreferences();
		prefs.setCustomProperties(new Property[]{new Property("eclipse.is.cool", "Yep"), new Property("JUnitTest", "true")});
		
		run("TestForEcho.xml", new String[]{});
		assertSuccessful();
		assertTrue("eclipse.is.cool should have been set as Yep", "Yep".equals(AntTestChecker.getDefault().getUserProperty("eclipse.is.cool")));
		assertTrue("JUnitTests should have a value of true", "true".equals(AntTestChecker.getDefault().getUserProperty("JUnitTest")));
		assertNull("my.name was not set and should be null", AntTestChecker.getDefault().getUserProperty("my.name"));
		
		restorePreferenceDefaults();
	}
	
	public void testGlobalPropertyFileWithMinusDTakingPrecedence() throws CoreException {
		AntCorePreferences prefs =AntCorePlugin.getPlugin().getPreferences();
		
		String path= getPropertyFileName();
		prefs.setCustomPropertyFiles(new String[]{path});
		
		run("echoing.xml", new String[]{"-DAntTests=testing", "-Declipse.is.cool=true"}, false);
		assertSuccessful();
		assertTrue("eclipse.is.cool should have been set as true", "true".equals(AntTestChecker.getDefault().getUserProperty("eclipse.is.cool")));
		assertTrue("AntTests should have a value of testing", "testing".equals(AntTestChecker.getDefault().getUserProperty("AntTests")));
		assertNull("my.name was not set and should be null", AntTestChecker.getDefault().getUserProperty("my.name"));
		restorePreferenceDefaults();
	}
	
	/**
	 * Tests that the default ANT_HOME is set and that it can be changed
	 */
	public void testSettingAntHome() throws CoreException {
		try {
			AntCorePreferences prefs= AntCorePlugin.getPlugin().getPreferences();
			run("echoing.xml");
			assertTrue("ANT_HOME not set correctly", prefs.getDefaultAntHome().equals(System.getProperty("ant.home")));
			File antLibDir= new File(prefs.getDefaultAntHome(), "lib"); //$NON-NLS-1$
			assertTrue("ant.library.dir not set correctly", antLibDir.getAbsolutePath().equals(System.getProperty("ant.library.dir")));
			prefs.setAntHome("");
			run("echoing.xml");
			assertTrue("ANT_HOME not set correctly", null == System.getProperty("ant.home"));
			assertTrue("ant.library.dir not set correctly", null == System.getProperty("ant.library.dir"));
		} finally {
			restorePreferenceDefaults();
		}	
	}
	
	/**
	 * Tests retrieving target info using AntRunner
     * Covers bug 73602 at the same time
	 */
	public void testGetTargets() throws CoreException {
	  
			AntCorePreferences prefs =AntCorePlugin.getPlugin().getPreferences();
			
			String path= getProject().getFolder("resources").getLocation().toFile().getAbsolutePath();
			IAntClasspathEntry entry= new AntClasspathEntry(path);
			
			
			IAntClasspathEntry entries[] = prefs.getAdditionalClasspathEntries();
			IAntClasspathEntry newEntries[] = new IAntClasspathEntry[entries.length + 1];
			System.arraycopy(entries, 0, newEntries, 0, entries.length);
			newEntries[entries.length] = entry;
			prefs.setAdditionalClasspathEntries(newEntries);
			
			prefs.updatePluginPreferences();
			
			AntRunner runner= new AntRunner();
			IFile buildFile= getBuildFile("Bug73602.xml");
			if (buildFile != null) {
				runner.setBuildFileLocation(buildFile.getLocation().toFile().toString());
			}
			TargetInfo[] infos= runner.getAvailableTargets();
			assertTrue("incorrect number of targets retrieved", infos != null && infos.length == 3);
	}
}
