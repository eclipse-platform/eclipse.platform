/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.tests.core.tests;


import java.net.URL;

import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.ant.core.AntCorePreferences;
import org.eclipse.ant.core.Type;
import org.eclipse.ant.internal.core.AntClasspathEntry;
import org.eclipse.ant.tests.core.AbstractAntTest;
import org.eclipse.ant.tests.core.testplugin.AntTestChecker;
import org.eclipse.core.runtime.CoreException;

public class TypeTests extends AbstractAntTest {

	public TypeTests(String name) {
		super(name);
	}
	
	public void testAddType() throws CoreException {
		AntCorePreferences prefs =AntCorePlugin.getPlugin().getPreferences();
		URL[] urls= prefs.getExtraClasspathURLs();
		Type newType= new Type();
		newType.setLibraryEntry(new AntClasspathEntry(urls[0]));
		newType.setTypeName("anttestpath");
		newType.setClassName("org.eclipse.ant.tests.core.support.types.AntTestPath");
		prefs.setCustomTypes(new Type[]{newType});
		
		run("CustomType.xml");
		String msg= (String)AntTestChecker.getDefault().getMessages().get(1);
		assertEquals("Message incorrect: " + msg, "Test adding a custom type", msg);
		assertSuccessful();
	}
	
	public void testRemoveType() {
		AntCorePreferences prefs =AntCorePlugin.getPlugin().getPreferences();
		prefs.setCustomTypes(new Type[]{});
		try {
			run("CustomType.xml");
		} catch (CoreException ce) {
			assertTrue("Exception from undefined type is incorrect: "+ ce.getMessage(), ce.getMessage().trim().endsWith("Action: Check that any <presetdef>/<macrodef> declarations have taken place."));
			return;
		} finally {
			restorePreferenceDefaults();	
		}
		assertTrue("Build should have failed as type no longer defined", false);
		
	}
	
	public void testTypeDefinedInExtensionPoint() throws CoreException {
		run("ExtensionPointType.xml");
		String msg= (String)AntTestChecker.getDefault().getMessages().get(1);
		assertEquals("Message incorrect: " + msg, "Ensure that an extension point defined type is present", msg);
		assertSuccessful();
	}
	
	public void testTypeDefinedInExtensionPointHeadless() {
		AntCorePlugin.getPlugin().setRunningHeadless(true);
		try {
			run("ExtensionPointType.xml");
		} catch (CoreException ce) {
			assertTrue("Exception from undefined type is incorrect: " + ce.getMessage(), ce.getMessage().trim().endsWith("Action: Check that any <presetdef>/<macrodef> declarations have taken place."));
			return;
		} finally {
			AntCorePlugin.getPlugin().setRunningHeadless(false);
		}
		assertTrue("Build should have failed as type was not defined to run in headless", false);
	}	
}