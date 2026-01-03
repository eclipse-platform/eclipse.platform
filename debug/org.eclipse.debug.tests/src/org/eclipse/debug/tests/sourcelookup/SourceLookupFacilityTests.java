/*******************************************************************************
 * Copyright (c) Jul 30, 2014 IBM Corporation and others.
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
package org.eclipse.debug.tests.sourcelookup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.internal.ui.sourcelookup.SourceLookupFacility;
import org.eclipse.debug.internal.ui.sourcelookup.SourceLookupResult;
import org.eclipse.debug.tests.DebugTestExtension;
import org.eclipse.debug.ui.sourcelookup.ISourceLookupResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests {@link SourceLookupFacility}
 *
 * @since 3.9.200
 */
@ExtendWith(DebugTestExtension.class)
public class SourceLookupFacilityTests {

	/**
	 * {@link IStackFrame} to be reused
	 */
	TestStackFrame fReusableFrame = new TestStackFrame(new TestLaunch());
	/**
	 * Testing source director
	 */
	TestSourceDirector fTestDirector = new TestSourceDirector();
	/**
	 * Test source locator
	 */
	TestSourceLocator fTestLocator = new TestSourceLocator();

	/**
	 * Tests calling
	 * {@link SourceLookupFacility#lookup(Object, org.eclipse.debug.core.model.ISourceLocator, boolean)}
	 * with simple type, no locator and no forcing
	 */
	@Test
	public void testLookupStringNoLocatorNoForce() throws Exception {
		try {
			String artifact = "Empty"; //$NON-NLS-1$
			ISourceLookupResult result = SourceLookupFacility.getDefault().lookup(artifact, null, false);
			assertNotNull(result, "There should be a result"); //$NON-NLS-1$
			assertNull(result.getSourceElement(), "Source element should be null"); //$NON-NLS-1$
		} finally {
			SourceLookupFacility.shutdown();
		}
	}

	/**
	 * Tests calling
	 * {@link SourceLookupFacility#lookup(Object, org.eclipse.debug.core.model.ISourceLocator, boolean)}
	 * with simple type and no forcing
	 */
	@Test
	public void testLookupStringNoForce() throws Exception {
		try {
			String artifact = "One"; //$NON-NLS-1$
			ISourceLookupResult result = SourceLookupFacility.getDefault().lookup(artifact, fTestDirector, false);
			assertNotNull(result, "There should be a result"); //$NON-NLS-1$
			assertTrue(result.getArtifact() instanceof String, "The result artifact should be a String"); //$NON-NLS-1$
			assertTrue(result.getSourceElement() instanceof String, "The result source element should be a String"); //$NON-NLS-1$
			String value = (String) result.getSourceElement();
			result = SourceLookupFacility.getDefault().lookup(artifact, fTestDirector, false);
			assertNotNull(result, "There should be a result"); //$NON-NLS-1$
			assertTrue(result.getArtifact() instanceof String, "The result artifact should be a String"); //$NON-NLS-1$
			assertTrue(result.getSourceElement() instanceof String, "The result source element should be a String"); //$NON-NLS-1$
			assertEquals(value, result.getSourceElement(), "The results should be equal"); //$NON-NLS-1$
		} finally {
			SourceLookupFacility.shutdown();
		}
	}

	/**
	 * Tests calling
	 * {@link SourceLookupFacility#lookup(Object, org.eclipse.debug.core.model.ISourceLocator, boolean)}
	 * with simple type and forcing
	 */
	@Test
	public void testLookupStringForce() throws Exception {
		try {
			String artifact = "Two"; //$NON-NLS-1$
			ISourceLookupResult result = SourceLookupFacility.getDefault().lookup(artifact, fTestDirector, true);
			assertNotNull(result, "There should be a result"); //$NON-NLS-1$
			assertTrue(result.getArtifact() instanceof String, "The result artifact should be a String"); //$NON-NLS-1$
			assertTrue(result.getSourceElement() instanceof String, "The result source element should be a String"); //$NON-NLS-1$
			String value = (String) result.getSourceElement();
			result = SourceLookupFacility.getDefault().lookup(artifact, fTestDirector, true);
			assertNotNull(result, "There should be a result"); //$NON-NLS-1$
			assertTrue(result.getArtifact() instanceof String, "The result artifact should be a String"); //$NON-NLS-1$
			assertTrue(result.getSourceElement() instanceof String, "The result source element should be a String"); //$NON-NLS-1$
			assertNotSame(value, result.getSourceElement(), "The results should not be equal"); //$NON-NLS-1$
		} finally {
			SourceLookupFacility.shutdown();
		}
	}

	/**
	 * Tests calling
	 * {@link SourceLookupFacility#lookup(Object, org.eclipse.debug.core.model.ISourceLocator, boolean)}
	 * with simple type and no forcing
	 */
	@Test
	public void testLookupStringLocatorNoForce() throws Exception {
		try {
			String artifact = "Three"; //$NON-NLS-1$
			ISourceLookupResult result = SourceLookupFacility.getDefault().lookup(artifact, fTestLocator, false);
			assertNotNull(result, "There should be a result"); //$NON-NLS-1$
			assertNull(result.getSourceElement(), "The source element should be null"); //$NON-NLS-1$
		} finally {
			SourceLookupFacility.shutdown();
		}
	}

	/**
	 * Tests calling
	 * {@link SourceLookupFacility#lookup(Object, org.eclipse.debug.core.model.ISourceLocator, boolean)}
	 * with an {@link IStackFrame} impl and no forcing
	 */
	@Test
	public void testLookupStackframeNoForce() throws Exception {
		try {
			ISourceLookupResult result = SourceLookupFacility.getDefault().lookup(fReusableFrame, fTestDirector, false);
			assertNotNull(result, "There should be a result"); //$NON-NLS-1$
			assertTrue(result.getArtifact() instanceof IStackFrame, "The result source element should be a String"); //$NON-NLS-1$
			assertTrue(result.getSourceElement() instanceof String, "The result source element should be a String"); //$NON-NLS-1$
			String value = (String) result.getSourceElement();
			result = SourceLookupFacility.getDefault().lookup(fReusableFrame, fTestDirector, false);
			assertNotNull(result, "There should be a result"); //$NON-NLS-1$
			assertTrue(result.getArtifact() instanceof IStackFrame, "The result artifact should be a String"); //$NON-NLS-1$
			assertTrue(result.getSourceElement() instanceof String, "The result source element should be a String"); //$NON-NLS-1$
			assertEquals(value, result.getSourceElement(), "The results should not be equal"); //$NON-NLS-1$
		} finally {
			SourceLookupFacility.shutdown();
		}
	}

	/**
	 * Tests calling
	 * {@link SourceLookupFacility#lookup(Object, org.eclipse.debug.core.model.ISourceLocator, boolean)}
	 * with an {@link IStackFrame} impl and forcing
	 */
	@Test
	public void testLookupStackframeForce() throws Exception {
		try {
			ISourceLookupResult result = SourceLookupFacility.getDefault().lookup(fReusableFrame, fTestDirector, true);
			assertNotNull(result, "There should be a result"); //$NON-NLS-1$
			assertTrue(result.getArtifact() instanceof IStackFrame, "The result source element should be a String"); //$NON-NLS-1$
			assertTrue(result.getSourceElement() instanceof String, "The result source element should be a String"); //$NON-NLS-1$
			String value = (String) result.getSourceElement();
			result = SourceLookupFacility.getDefault().lookup(fReusableFrame, fTestDirector, true);
			assertNotNull(result, "There should be a result"); //$NON-NLS-1$
			assertTrue(result.getArtifact() instanceof IStackFrame, "The result artifact should be a String"); //$NON-NLS-1$
			assertTrue(result.getSourceElement() instanceof String, "The result source element should be a String"); //$NON-NLS-1$
			assertNotSame(value, result.getSourceElement(), "The results should not be equal"); //$NON-NLS-1$
		} finally {
			SourceLookupFacility.shutdown();
		}
	}

	/**
	 * Tests calling
	 * {@link SourceLookupFacility#lookup(Object, org.eclipse.debug.core.model.ISourceLocator, boolean)}
	 * with an {@link IStackFrame} impl, no forcing, no locator, no launch
	 */
	@Test
	public void testLookupStackframeWithDebugElement1() throws Exception {
		try {
			ISourceLookupResult result = SourceLookupFacility.getDefault().lookup(new TestStackFrame(null), null, false);
			assertNotNull(result, "There should be a result"); //$NON-NLS-1$
			assertNull(result.getSourceElement(), "Source element should be null"); //$NON-NLS-1$
		} finally {
			SourceLookupFacility.shutdown();
		}
	}

	/**
	 * Tests calling
	 * {@link SourceLookupFacility#lookup(Object, org.eclipse.debug.core.model.ISourceLocator, boolean)}
	 * with an {@link IStackFrame} impl, no forcing, no locator
	 */
	@Test
	public void testLookupStackframeWithDebugElement2() throws Exception {
		try {
			ISourceLookupResult result = SourceLookupFacility.getDefault().lookup(fReusableFrame, null, false);
			assertNotNull(result, "There should be a result"); //$NON-NLS-1$
			assertTrue(result.getArtifact() instanceof IStackFrame, "The result source element should be a String"); //$NON-NLS-1$
			assertTrue(result.getSourceElement() instanceof String, "The result source element should be a String"); //$NON-NLS-1$
			String value = (String) result.getSourceElement();
			result = SourceLookupFacility.getDefault().lookup(fReusableFrame, null, false);
			assertNotNull(result, "There should be a result"); //$NON-NLS-1$
			assertTrue(result.getArtifact() instanceof IStackFrame, "The result artifact should be a String"); //$NON-NLS-1$
			assertTrue(result.getSourceElement() instanceof String, "The result source element should be a String"); //$NON-NLS-1$
			assertEquals(value, result.getSourceElement(), "The results should be equal"); //$NON-NLS-1$
		} finally {
			SourceLookupFacility.shutdown();
		}
	}

	/**
	 * Tests calling
	 * {@link SourceLookupFacility#lookup(Object, org.eclipse.debug.core.model.ISourceLocator, boolean)}
	 * with an {@link IStackFrame} impl, forcing, no locator
	 */
	@Test
	public void testLookupStackframeWithDebugElement3() throws Exception {
		try {
			ISourceLookupResult result = SourceLookupFacility.getDefault().lookup(fReusableFrame, null, true);
			assertNotNull(result, "There should be a result"); //$NON-NLS-1$
			assertTrue(result.getArtifact() instanceof IStackFrame, "The result source element should be a String"); //$NON-NLS-1$
			assertTrue(result.getSourceElement() instanceof String, "The result source element should be a String"); //$NON-NLS-1$
			String value = (String) result.getSourceElement();
			result = SourceLookupFacility.getDefault().lookup(fReusableFrame, null, true);
			assertNotNull(result, "There should be a result"); //$NON-NLS-1$
			assertTrue(result.getArtifact() instanceof IStackFrame, "The result artifact should be a String"); //$NON-NLS-1$
			assertTrue(result.getSourceElement() instanceof String, "The result source element should be a String"); //$NON-NLS-1$
			assertNotSame(value, result.getSourceElement(), "The results should not be equal"); //$NON-NLS-1$
		} finally {
			SourceLookupFacility.shutdown();
		}
	}

	/**
	 * Tests calling
	 * {@link SourceLookupFacility#lookup(Object, org.eclipse.debug.core.model.ISourceLocator, boolean)}
	 * with an {@link IStackFrame} impl, no forcing, ISourceLocator impl
	 */
	@Test
	public void testLookupStackframeWithDebugElement4() throws Exception {
		try {
			ISourceLookupResult result = SourceLookupFacility.getDefault().lookup(fReusableFrame, fTestLocator, false);
			assertNotNull( result, "There should be a result"); //$NON-NLS-1$
			assertTrue( result.getArtifact() instanceof IStackFrame, "The result source element should be a String"); //$NON-NLS-1$
			assertTrue( result.getSourceElement() instanceof String, "The result source element should be a String"); //$NON-NLS-1$
			String value = (String) result.getSourceElement();
			result = SourceLookupFacility.getDefault().lookup(fReusableFrame, fTestLocator, false);
			assertNotNull( result, "There should be a result"); //$NON-NLS-1$
			assertTrue( result.getArtifact() instanceof IStackFrame, "The result artifact should be a String"); //$NON-NLS-1$
			assertTrue( result.getSourceElement() instanceof String, "The result source element should be a String"); //$NON-NLS-1$
			assertEquals(value, result.getSourceElement(), "The results should not be equal"); //$NON-NLS-1$
		} finally {
			SourceLookupFacility.shutdown();
		}
	}

	/**
	 * Tests calling
	 * {@link SourceLookupFacility#lookup(Object, org.eclipse.debug.core.model.ISourceLocator, boolean)}
	 * with an {@link IStackFrame} impl, forcing, ISourceLocator impl
	 */
	@Test
	public void testLookupStackframeWithDebugElement5() throws Exception {
		try {
			ISourceLookupResult result = SourceLookupFacility.getDefault().lookup(fReusableFrame, fTestLocator, true);
			assertNotNull(result, "There should be a result"); //$NON-NLS-1$
			assertTrue(result.getArtifact() instanceof IStackFrame, "The result source element should be a String"); //$NON-NLS-1$
			assertTrue(result.getSourceElement() instanceof String, "The result source element should be a String"); //$NON-NLS-1$
			String value = (String) result.getSourceElement();
			result = SourceLookupFacility.getDefault().lookup(fReusableFrame, fTestLocator, true);
			assertNotNull(result, "There should be a result"); //$NON-NLS-1$
			assertTrue(result.getArtifact() instanceof IStackFrame, "The result artifact should be a String\""); //$NON-NLS-1$
			assertTrue(result.getSourceElement() instanceof String, "The result source element should be a String"); //$NON-NLS-1$
			assertNotSame(value, result.getSourceElement(), "The results should not be equal"); //$NON-NLS-1$
		} finally {
			SourceLookupFacility.shutdown();
		}
	}

	@Test
	public void testLRU() throws Exception {
		try {
			final int MAX_LRU_SIZE = 10;

			// Get the original map
			Field field = SourceLookupFacility.class.getDeclaredField("fLookupResults"); //$NON-NLS-1$
			field.setAccessible(true);
			Map<?, ?> map = (Map<?, ?>) field.get(SourceLookupFacility.getDefault());
			LinkedHashMap<String, ISourceLookupResult> cached = new LinkedHashMap<>();

			// fill the LRU with one element overflow
			for (int i = 0; i < MAX_LRU_SIZE + 1; i++) {
				String artifact = "" + i; //$NON-NLS-1$
				ISourceLookupResult result = SourceLookupFacility.getDefault().lookup(artifact, fTestLocator, false);
				assertNotNull(result, "There should be a result"); //$NON-NLS-1$
				assertFalse(cached.containsValue(result));
				cached.put(artifact, result);

				result = SourceLookupFacility.getDefault().lookup(artifact, fTestLocator, false);
				assertTrue(cached.containsValue(result));
				assertTrue(map.size() <= MAX_LRU_SIZE);
			}
			assertEquals(MAX_LRU_SIZE, map.size());


			// The LRU cache is full now, and the very first element should be
			// *not* in the LRU cache anymore
			assertFalse(map.containsValue(cached.values().iterator().next()));

			// If we lookup for the first element again, we should get new one
			String artifact = "" + 0; //$NON-NLS-1$
			SourceLookupResult result = SourceLookupFacility.getDefault().lookup(artifact, fTestLocator, false);
			assertNotNull(result, "There should be a result"); //$NON-NLS-1$
			assertFalse(new IdentityHashMap<>(cached).containsValue(result));

			// Check: the LRU map size should not grow
			assertEquals(MAX_LRU_SIZE, map.size());
		} finally {
			SourceLookupFacility.shutdown();
		}
	}
}
