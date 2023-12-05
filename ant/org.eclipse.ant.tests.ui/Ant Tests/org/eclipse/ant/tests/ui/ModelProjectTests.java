/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
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
package org.eclipse.ant.tests.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Hashtable;
import java.util.Map;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.PropertyHelper;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.types.Path;
import org.eclipse.ant.internal.core.IAntCoreConstants;
import org.eclipse.ant.internal.ui.model.AntModelProject;
import org.junit.Test;

/**
 * Tests methods from {@link AntModelProject}
 * 
 * @since 3.5.1
 */
public class ModelProjectTests {

	/**
	 * Tests {@link AntModelProject#setNewProperty(String, String)}
	 */
	@Test
	public void testsetNewProperty1() throws Exception {
		AntModelProject p = new AntModelProject();
		p.setNewProperty("p1", "p1_value"); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("the property map must have key 'p1'", p.getProperties().containsKey("p1")); //$NON-NLS-1$ //$NON-NLS-2$
		assertNotNull("getProperty() did not return 'p1'", p.getProperty("p1")); //$NON-NLS-1$ //$NON-NLS-2$
		assertNotNull("the property help must have 'p1'", PropertyHelper.getProperty(p, "p1")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests {@link AntModelProject#reset()}
	 */
	@Test
	public void testReset1() throws Exception {
		AntModelProject p = new AntModelProject();
		p.setDefault("foo"); //$NON-NLS-1$
		p.addTarget("t1", new Target()); //$NON-NLS-1$
		p.setName("project1"); //$NON-NLS-1$
		p.setDescription("A test project with one target"); //$NON-NLS-1$
		p.reset();
		assertEquals("the name must be the empty string", p.getName(), IAntCoreConstants.EMPTY_STRING); //$NON-NLS-1$
		assertNull("the default target must be null", p.getDefaultTarget()); //$NON-NLS-1$
		assertNull("the description must be null", p.getDescription()); //$NON-NLS-1$
		assertEquals("The target list must be of size zero", p.getTargets().size(), 0); //$NON-NLS-1$
	}

	/**
	 * Tests {@link AntModelProject#getProperty(String)} where the property we want
	 * has been set using {@link AntModelProject#setNewProperty(String, String)}
	 */
	@Test
	public void testGetProprty1() throws Exception {
		AntModelProject p = new AntModelProject();
		p.setNewProperty("testGetProprty1", "p1_value"); //$NON-NLS-1$ //$NON-NLS-2$
		assertNotNull("getProperty() did not return 'testGetProprty1'", p.getProperty("testGetProprty1")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests {@link AntModelProject#getProperty(String)} where the property has been
	 * set into the user properties
	 */
	@Test
	public void testGetProprty2() throws Exception {
		AntModelProject p = new AntModelProject();
		p.setUserProperty("testGetProprty2", "p2_value"); //$NON-NLS-1$ //$NON-NLS-2$
		assertNotNull("getProperty() did not return 'testGetProprty2'", p.getProperty("testGetProprty2")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests {@link AntModelProject#getReference(String)} where the desired ref has
	 * been placed in the primary reference map using
	 * {@link AntModelProject#addReference(String, Object)}
	 */
	@Test
	public void testGetReference1() throws Exception {
		AntModelProject p = new AntModelProject();
		p.addReference("testGetReference1", new Object()); //$NON-NLS-1$
		assertNotNull("The reference 'testGetReference1' must exist", p.getReference("testGetReference1")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests {@link AntModelProject#getReference(String)} where the desired ref has
	 * been placed in the secondary 'idrefs' map using
	 * {@link AntModelProject#addIdReference(String, Object)}
	 */
	@Test
	public void testGetReference2() throws Exception {
		AntModelProject p = new AntModelProject();
		p.addIdReference("testGetReference2", new Object()); //$NON-NLS-1$
		assertNotNull("the reference 'testGetReference2' must exist", p.getReference("testGetReference2")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests {@link AntModelProject#getReference(String)} where the desired ref is
	 * in the secondary 'idrefs' map and is an {@link UnknownElement}
	 */
	@Test
	public void testGetReference3() throws Exception {
		AntModelProject p = new AntModelProject();
		String obj = new String("hello"); //$NON-NLS-1$
		UnknownElement ue = new UnknownElement("ue"); //$NON-NLS-1$
		ue.setProject(p);
		ue.setRealThing(obj);
		p.addIdReference("testGetReference3", ue); //$NON-NLS-1$
		Object ref = p.getReference("testGetReference3"); //$NON-NLS-1$
		assertNotNull("the reference 'testGetReference3' must exist", ref); //$NON-NLS-1$
		assertTrue("the reference must be a String", ref instanceof String); //$NON-NLS-1$
	}

	/**
	 * Tests {@link AntModelProject#getProperties()} such that all properties (user
	 * and 'normal') are returned in one call to
	 * {@link AntModelProject#getProperties()}
	 */
	@Test
	public void testGetProperties1() throws Exception {
		AntModelProject p = new AntModelProject();
		p.setProperty("p1", "p1v"); //$NON-NLS-1$ //$NON-NLS-2$
		p.setProperty("p2", "p2v"); //$NON-NLS-1$ //$NON-NLS-2$
		p.setUserProperty("p3", "p3v"); //$NON-NLS-1$ //$NON-NLS-2$
		// 3 above and 'basedir'
		assertEquals("there must be 4 properties returned", p.getProperties().size(), 4); //$NON-NLS-1$
	}

	/**
	 * Tests
	 * {@link AntModelProject#createClassLoader(org.apache.tools.ant.types.Path)}
	 * such that only one classloader is created for a given path - classloaders are
	 * cached per-Path
	 */
	@Test
	public void testCreateClassloader1() throws Exception {
		AntModelProject p = new AntModelProject();
		Path path = new Path(p);
		AntClassLoader loader = p.createClassLoader(path);
		assertNotNull("A classloader should have been created", loader); //$NON-NLS-1$
		// create a 'new' one, where passing null causes the classloader for the project
		// path to be returned
		AntClassLoader loader2 = p.createClassLoader(null);
		assertNotNull("A classloader for a null path should return the project classloader", loader2); //$NON-NLS-1$
		// pointer compare, they should reference the same class loader
		assertTrue("The class loaders should be the same object", loader == loader2); //$NON-NLS-1$
	}

	/**
	 * Tests {@link AntModelProject#getReferences()} to ensure the map returned is
	 * the live map https://bugs.eclipse.org/bugs/show_bug.cgi?id=336936
	 */
	@Test
	public void testGetReferences1() throws Exception {
		AntModelProject p = new AntModelProject();
		p.addReference("testGetReferences1", new Object()); //$NON-NLS-1$
		// 2 refs, ant.propertyHelper is auto-added
		assertEquals("there should be one reference", p.getReferences().size(), 2); //$NON-NLS-1$
		Hashtable<String, Object> ht = p.getReferences();
		ht.clear();
		assertEquals("The references table should be empty", p.getReferences().size(), 0); //$NON-NLS-1$
	}

	/**
	 * Tests {@link AntModelProject#getCopyOfReferences()}
	 */
	@Test
	public void testGetCopyOfReferences1() throws Exception {
		AntModelProject p = new AntModelProject();
		// 2 refs, ant.propertyHelper is auto-added
		p.addReference("testGetCopyOfReferences1", new Object()); //$NON-NLS-1$
		assertEquals("there should be one reference", p.getReferences().size(), 2); //$NON-NLS-1$
		Map<String, Object> ht = p.getCopyOfReferences();
		ht.clear();
		assertEquals("There should be one reference", p.getReferences().size(), 2); //$NON-NLS-1$
	}
}
