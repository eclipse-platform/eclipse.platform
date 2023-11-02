/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 474274
 ******************************************************************************/

package org.eclipse.e4.core.internal.tests.contexts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.contexts.ContextFunction;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.contexts.RunAndTrack;
import org.eclipse.e4.core.internal.tests.contexts.inject.ObjectSuperClass;
import org.junit.Test;

/**
 * Test for changing a context's parent.
 */
public class ReparentingTest {


	/**
	 * Tests handling of a context function defined in the parent when the parent is changed to no
	 * longer have the function.
	 */
	@Test
	public void testContextFunctionInParentRemove() {
		IEclipseContext parent = EclipseContextFactory.create("parent");
		final IEclipseContext child = parent.createChild("child");
		parent.set("sum", new AddContextFunction());
		parent.set("x", Integer.valueOf(3));
		parent.set("y", Integer.valueOf(3));
		child.set("x", Integer.valueOf(1));
		child.set("y", Integer.valueOf(1));
		assertEquals(6, ((Integer) parent.get("sum")).intValue());
		assertEquals(2, ((Integer) child.get("sum")).intValue());
		child.setParent(EclipseContextFactory.create());
		assertEquals(6, ((Integer) parent.get("sum")).intValue());
		assertNull("Expected null but was: " + child.get("sum"), child.get("sum"));
	}

	/**
	 * Tests handling of a context function defined in the parent when the parent is changed to have
	 * the function
	 */
	@Test
	public void testContextFunctionInParentAdd() {
		// setup
		IEclipseContext parent = EclipseContextFactory.create();
		final IEclipseContext child = parent.createChild();
		child.set("x", Integer.valueOf(1));
		child.set("y", Integer.valueOf(1));
		assertEquals(null, parent.get("sum"));
		assertEquals(null, child.get("sum"));

		// switch parent
		IEclipseContext newParent = EclipseContextFactory.create();
		child.setParent(newParent);
		newParent.set("sum", new AddContextFunction());
		assertEquals(0, ((Integer) newParent.get("sum")).intValue());
		assertEquals(2, ((Integer) child.get("sum")).intValue());

		// changed values in parent shouldn't affect child
		newParent.set("x", Integer.valueOf(3));
		newParent.set("y", Integer.valueOf(3));
		assertEquals(6, ((Integer) newParent.get("sum")).intValue());
		assertEquals(2, ((Integer) child.get("sum")).intValue());
	}

	@Test
	public void testContextFunctionNullBecomeParent() {
		final IEclipseContext child = EclipseContextFactory.create();
		child.set("sum", new AddContextFunction());
		assertEquals(0, ((Integer) child.get("sum")).intValue());
		IEclipseContext parent = EclipseContextFactory.create();
		parent.set("x", Integer.valueOf(3));
		parent.set("y", Integer.valueOf(3));
		child.setParent(parent);
		assertEquals(6, ((Integer) child.get("sum")).intValue());

	}

	@Test
	public void testContextFunctionParentBecomeNull() {
		IEclipseContext parent = EclipseContextFactory.create();
		final IEclipseContext child = parent.createChild();
		parent.set("x", Integer.valueOf(3));
		parent.set("y", Integer.valueOf(3));
		child.set("sum", new AddContextFunction());
		assertEquals(6, ((Integer) child.get("sum")).intValue());
		child.setParent(null);
		assertEquals(0, ((Integer) child.get("sum")).intValue());
	}

	@Test
	public void testContextFunctionSwitchParent() {
		IEclipseContext parent = EclipseContextFactory.create();
		final IEclipseContext child = parent.createChild();
		parent.set("x", Integer.valueOf(3));
		parent.set("y", Integer.valueOf(3));
		child.set("sum", new AddContextFunction());
		assertEquals(6, ((Integer) child.get("sum")).intValue());
		IEclipseContext newParent = EclipseContextFactory.create();
		newParent.set("x", Integer.valueOf(1));
		newParent.set("y", Integer.valueOf(1));
		child.setParent(newParent);
		assertEquals(2, ((Integer) child.get("sum")).intValue());
	}

	/**
	 * Tests a child switching from a null parent to a non-null parent.
	 */
	@Test
	public void testRunAndTrackNullBecomesParent() {
		final String[] value = new String[1];
		final IEclipseContext child = EclipseContextFactory.create();
		child.runAndTrack(new RunAndTrack() {
			@Override
			public boolean changed(IEclipseContext context) {
				value[0] = (String) child.get("x");
				return true;
			}
		});
		assertEquals(null, value[0]);
		IEclipseContext parent = EclipseContextFactory.create();
		parent.set("x", "newParent");
		child.setParent(parent);
		assertEquals("newParent", value[0]);
	}

	/**
	 * Tests a child switching from a non-null parent to a null parent.
	 */
	@Test
	public void testRunAndTrackParentBecomeNull() {
		final String[] value = new String[1];
		IEclipseContext parent = EclipseContextFactory.create();
		final IEclipseContext child = parent.createChild();
		parent.set("x", "oldParent");
		child.runAndTrack(new RunAndTrack() {
			@Override
			public boolean changed(IEclipseContext context) {
				value[0] = (String) child.get("x");
				return true;
			}
		});
		assertEquals("oldParent", value[0]);
		child.setParent(null);
		assertNull(value[0]);
	}

	@Test
	public void testRunAndTrackSwitchParent() {
		final String[] value = new String[1];
		IEclipseContext parent = EclipseContextFactory.create();
		final IEclipseContext child = parent.createChild();
		parent.set("x", "oldParent");
		child.runAndTrack(new RunAndTrack() {
			@Override
			public boolean changed(IEclipseContext context) {
				value[0] = (String) child.get("x");
				return true;
			}
		});
		assertEquals("oldParent", value[0]);
		IEclipseContext newParent = EclipseContextFactory.create();
		newParent.set("x", "newParent");
		child.setParent(newParent);
		assertEquals("newParent", value[0]);
	}

	/**
	 * Tests an object consuming simple values from a parent context, and a parent change causes a
	 * change in simple values. TODO: Still fails
	 */
	@Test
	public void testInjectSwitchParent() {

		IEclipseContext oldParent = EclipseContextFactory.create();
		oldParent.set("String", "oldField");
		oldParent.set(String.class.getName(), "old");
		oldParent.set(Float.class.getName(), Float.valueOf(12.3f));
		IEclipseContext newParent = EclipseContextFactory.create();
		newParent.set("String", "newField");
		newParent.set(String.class.getName(), "new");
		newParent.set(Float.class.getName(), Float.valueOf(34.5f));
		IEclipseContext child = oldParent.createChild();

		ObjectSuperClass object = new ObjectSuperClass();
		ContextInjectionFactory.inject(object, child);
		assertEquals(1, object.setStringCalled);
		assertEquals("old", object.getStringViaMethod());

		child.setParent(newParent);
		assertEquals("new", object.getStringViaMethod());
		assertEquals(2, object.setStringCalled);

	}

	/**
	 * Tests an object consuming services from a grandparent. A parent switch where the grandparent
	 * stays unchanged should ideally not cause changes for the injected object.
	 */
	@Test
	public void testInjectSwitchParentSameGrandparent() {
		IEclipseContext grandpa = EclipseContextFactory.create();
		grandpa.set("String", "field");
		grandpa.set(String.class.getName(), "s");
		grandpa.set(Float.class.getName(), Float.valueOf(12.3f));

		IEclipseContext oldParent = grandpa.createChild();
		IEclipseContext newParent = grandpa.createChild();
		IEclipseContext child = oldParent.createChild();

		ObjectSuperClass object = new ObjectSuperClass();
		ContextInjectionFactory.inject(object, child);
		assertEquals(1, object.setStringCalled);

		child.setParent(newParent);
		assertEquals(1, object.setStringCalled);
	}

	@Test
	public void testUpdateSameParent() {
		final Boolean[] called = new Boolean[1] ;
		IEclipseContext parent = EclipseContextFactory.create("parent");
		IEclipseContext newParent = EclipseContextFactory.create("newParent");
		IEclipseContext child = parent.createChild("child");
		parent.set("x", "1");
		newParent.set("x", "2");

		child.runAndTrack(new RunAndTrack() {
			@Override
			public boolean changed(IEclipseContext context) {
				called[0] = true;
				context.get("x"); // creates a link
				return true;
			}
		});
		called[0] = false;

		// make sure setting parent to the same value does not trigger updates
		child.setParent(parent);
		assertFalse(called[0]);

		child.setParent(newParent);
		assertTrue(called[0]);
	}

	static public class TestService {
		// empty
	}

	@Test
	public void testUpdateSameParentCalculated() {
		final int[] testServiceCount = new int[1];
		testServiceCount[0] = 0;
		IEclipseContext parentContext = EclipseContextFactory.create("parent");
		parentContext.set(TestService.class.getName(), new ContextFunction() {
			@Override
			public Object compute(IEclipseContext context, String contextKey) {
				testServiceCount[0]++;
				return ContextInjectionFactory.make(TestService.class, context);
			}
		});

		IEclipseContext childContext = parentContext.createChild("child");
		childContext.get(TestService.class);
		assertEquals(1, testServiceCount[0]);

		childContext.setParent(childContext.getParent());
		assertEquals(1, testServiceCount[0]);
	}

	@Test
	public void testBug468048_contextFunction() {
		IEclipseContext p1 = EclipseContextFactory.create("parent1");
		p1.set("sample", new ContextFunction() {
			@Override
			public Object compute(IEclipseContext context) {
				return Integer.valueOf(1);
			}
		});

		IEclipseContext p2 = EclipseContextFactory.create("parent2");
		p2.set("sample", new ContextFunction() {
			@Override
			public Object compute(IEclipseContext context) {
				return Integer.valueOf(2);
			}
		});

		final IEclipseContext intermed = p1.createChild("intermed");

		final IEclipseContext leaf = intermed.createChild("leaf");
		assertEquals(Integer.valueOf(1), leaf.get("sample"));
		intermed.setParent(p2);
		assertEquals(Integer.valueOf(2), leaf.get("sample"));
	}

	@Test
	public void testBug468048_injection() {
		IEclipseContext p1 = EclipseContextFactory.create("parent1");
		p1.set("sample", new ContextFunction() {
			@Override
			public Object compute(IEclipseContext context) {
				return Integer.valueOf(1);
			}
		});

		IEclipseContext p2 = EclipseContextFactory.create("parent2");
		p2.set("sample", new ContextFunction() {
			@Override
			public Object compute(IEclipseContext context) {
				return Integer.valueOf(2);
			}
		});

		final IEclipseContext intermed = p1.createChild("intermed");

		final IEclipseContext leaf = intermed.createChild("leaf");
		Bug468048 b = ContextInjectionFactory.make(Bug468048.class, leaf);

		assertEquals(Integer.valueOf(1), b.sample);
		intermed.setParent(p2);
		assertEquals(Integer.valueOf(2), b.sample);
	}

	@Test
	public void testContextFunctionSwitchParent_2() {
		IEclipseContext superParent = EclipseContextFactory.create("root");

		IEclipseContext parent = superParent.createChild("parent-1");
		final IEclipseContext child = parent.createChild("child-1");
		child.set("x", Integer.valueOf(3));
		child.set("y", Integer.valueOf(3));

		superParent.set("sum", new ContextFunction() {
			@Override
			public Object compute(IEclipseContext context, String contextKey) {
				if (context != child) {
					throw new IllegalStateException("Invalid context state");
				}
				return (Integer) context.get("x") + (Integer) context.get("y");
			}
		});

		Bug541498 bug = ContextInjectionFactory.make(Bug541498.class, child);
		assertEquals(Integer.valueOf(6), bug.value);

		IEclipseContext newParent = superParent.createChild("parent-2");
		child.setParent(newParent);

		assertEquals(Integer.valueOf(6), bug.value);
	}

	public static class Bug541498 {
		@Inject
		@Named("sum")
		Integer value;
	}

	public static class Bug468048 {
		@Inject
		@Named("sample")
		public Integer sample;

	}
}
