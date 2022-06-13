/*******************************************************************************
 * Copyright (c) 2009, 2021 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.e4.core.internal.tests.contexts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.e4.core.contexts.ContextFunction;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.contexts.RunAndTrack;
import org.eclipse.e4.core.di.IInjector;
import org.eclipse.e4.core.internal.contexts.EclipseContext;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.FrameworkUtil;

public class EclipseContextTest {

	private static class ComputedValueBar extends ContextFunction {
		@Override
		public Object compute(IEclipseContext context, String contextKey) {
			return context.get("bar");
		}
	}

	private IEclipseContext context;
	private IEclipseContext parentContext;

	private int runCounter;

	@Before
	public void setUp() throws Exception {
		parentContext = EclipseContextFactory.create("EclipseContextTest" + "-parent");
		context = parentContext.createChild("EclipseContextTest");
		runCounter = 0;
	}

	@Test
	public void testContainsKey() {
		assertFalse("1.0", context.containsKey("function"));
		assertFalse("1.1", context.containsKey("separator"));

		context.set("separator", ",");
		assertTrue("2.1", context.containsKey("separator"));

		// null value is still a value
		context.set("separator", null);
		assertTrue("3.0", context.containsKey("separator"));

		context.remove("separator");
		assertFalse("4.0", context.containsKey("separator"));
	}

	@Test
	public void testGet() {
		assertNull(context.get("foo"));
		context.set("foo", "bar");
		assertEquals("bar", context.get("foo"));
		assertNull(parentContext.get("foo"));
		context.remove("foo");
		assertNull(context.get("foo"));
		parentContext.set("foo", "bar");
		assertEquals("bar", context.get("foo"));
		context.set("foo", new ComputedValueBar());
		assertNull(context.get("foo"));
		context.set("bar", "baz");
		assertEquals("baz", context.get("foo"));
	}

	@Test
	public void testGetLocal() {
		assertNull(context.getLocal("foo"));
		context.set("foo", "bar");
		assertEquals("bar", context.getLocal("foo"));
		assertNull(parentContext.getLocal("foo"));
		context.remove("foo");
		assertNull(context.getLocal("foo"));
		parentContext.set("foo", "bar");
		assertNull(context.getLocal("foo"));
		context.set("foo", new ComputedValueBar());
		assertNull(context.getLocal("foo"));
		context.set("bar", "baz");
		assertEquals("baz", context.getLocal("foo"));
	}

	/**
	 * Tests that a context no longer looks up values from its parent when disposed.
	 */
	@Test
	public void testDisposeRemovesParentReference() {
		assertNull(context.get("foo"));
		parentContext.set("foo", "bar");
		assertEquals("bar", context.get("foo"));
		context.dispose();
		assertNull(context.get("foo"));
		assertFalse(((EclipseContext) parentContext).getChildren().iterator().hasNext());
	}

	@Test
	public void testDisposeClearsNotifyOnDisposalSet() {
		((EclipseContext) context).notifyOnDisposal(context -> runCounter++);
		context.dispose();
		assertEquals(1, runCounter);
		context.dispose();
		assertEquals(1, runCounter);
	}

	/**
	 * Tests handling of a context function defined in the parent that uses values defined in the
	 * child
	 */
	@Test
	public void testContextFunctionInParent() {
		IEclipseContext parent = EclipseContextFactory.create();
		final IEclipseContext child = parent.createChild();
		parent.set("sum", new AddContextFunction());
		parent.set("x", Integer.valueOf(3));
		parent.set("y", Integer.valueOf(3));
		child.set("x", Integer.valueOf(1));
		child.set("y", Integer.valueOf(1));
		assertEquals(6, ((Integer) parent.get("sum")).intValue());
		assertEquals(2, ((Integer) child.get("sum")).intValue());
		child.set("x", Integer.valueOf(5));
		assertEquals(6, ((Integer) parent.get("sum")).intValue());
		assertEquals(6, ((Integer) child.get("sum")).intValue());
		child.remove("x");
		assertEquals(6, ((Integer) parent.get("sum")).intValue());
		assertEquals(4, ((Integer) child.get("sum")).intValue());
		parent.set("x", Integer.valueOf(10));
		assertEquals(13, ((Integer) parent.get("sum")).intValue());
		assertEquals(11, ((Integer) child.get("sum")).intValue());
	}

	@Test
	public void testRunAndTrack() {
		final Object[] value = new Object[1];
		context.runAndTrack(new RunAndTrack() {
			@Override
			public boolean changed(IEclipseContext context) {
				runCounter++;
				value[0] = context.get("foo");
				return true;
			}
		});
		assertEquals(1, runCounter);
		assertEquals(null, value[0]);
		context.set("foo", "bar");
		assertEquals(2, runCounter);
		assertEquals("bar", value[0]);
		context.remove("foo");
		assertEquals(3, runCounter);
		assertEquals(null, value[0]);
		context.set("foo", new ContextFunction() {
			@Override
			public Object compute(IEclipseContext context, String contextKey) {
				return context.get("bar");
			}
		});
		assertEquals(4, runCounter);
		assertEquals(null, value[0]);
		context.set("bar", "baz");
		assertEquals(5, runCounter);
		assertEquals("baz", value[0]);
		context.set("bar", "baf");
		assertEquals(6, runCounter);
		assertEquals("baf", value[0]);
		context.remove("bar");
		assertEquals(7, runCounter);
		assertEquals(null, value[0]);
		parentContext.set("bar", "bam");
		assertEquals(8, runCounter);
		assertEquals("bam", value[0]);
	}

	/**
	 * Tests registering a single run and track instance multiple times with the same context.
	 */
	@Test
	public void testRegisterRunAndTrackTwice() {
		final Object[] value = new Object[1];
		RunAndTrack runnable = new RunAndTrack() {
			@Override
			public boolean changed(IEclipseContext context) {
				runCounter++;
				value[0] = context.get("foo");
				return true;
			}
		};
		context.runAndTrack(runnable);
		assertEquals(1, runCounter);
		context.runAndTrack(runnable);
		assertEquals(2, runCounter);
		assertEquals(null, value[0]);
		context.set("foo", "bar");
		assertEquals(3, runCounter);
		assertEquals("bar", value[0]);
		context.remove("foo");
		assertEquals(4, runCounter);

	}

	@Test
	public void testRunAndTrackMultipleValues() {
		IEclipseContext parent = EclipseContextFactory.create("ParentContext");
		final IEclipseContext child = parent.createChild("ChildContext");
		parent.set("parentValue", "x");
		child.set("childValue", "x");
		RunAndTrack runnable = new RunAndTrack() {
			@Override
			public boolean changed(IEclipseContext context) {
				runCounter++;
				if (runCounter < 2) {
					child.get("childValue");
					return true;
				}
				if (runCounter < 3) {
					child.get("parentValue");
					return true;
				}
				return false;
			}
		};
		child.runAndTrack(runnable);
		assertEquals(1, runCounter);
		child.set("childValue", "z");
		assertEquals(2, runCounter);
		parent.set("parentValue", "z");
		assertEquals(3, runCounter);
	}

	@Test
	public void testModify() {
		IEclipseContext grandParent = EclipseContextFactory.create();
		IEclipseContext parent = grandParent.createChild();
		IEclipseContext child = parent.createChild();

		child.set("a", "a1");
		parent.set("b", "b2");
		grandParent.set("c", "c3");

		child.declareModifiable("a");
		parent.declareModifiable("b");
		grandParent.declareModifiable("c");

		// test pre-conditions
		assertNull(grandParent.get("b"));
		assertEquals("b2", parent.get("b"));
		assertEquals("b2", child.get("b"));
		assertNull(child.getLocal("b"));

		// modify value on the middle node via its child
		child.modify("b", "abc");

		assertFalse(grandParent.containsKey("b"));
		assertEquals("abc", parent.get("b"));
		assertEquals("abc", child.get("b"));
		assertNull(child.getLocal("b"));

		// modifying non-exist values adds it to the context
		child.modify("d", "123");

		assertFalse(grandParent.containsKey("d"));
		assertFalse(parent.containsKey("d"));
		assertNull(parent.get("d"));
		assertEquals("123", child.get("d"));

		// edge conditions: modify value in the top node
		grandParent.modify("c", "cNew");
		assertTrue(grandParent.containsKey("c"));
		assertEquals("cNew", grandParent.get("c"));
		assertNull(parent.getLocal("c"));
		assertNull(child.getLocal("c"));
		assertTrue(child.containsKey("c"));

		// edge condition: modify value in the leaf node
		child.modify("a", "aNew");
		assertTrue(child.containsKey("a"));
		assertFalse(parent.containsKey("a"));
		assertFalse(grandParent.containsKey("a"));
		assertEquals("aNew", child.get("a"));
		assertNull(parent.get("a"));

		// test access rules
		child.set("aNo", "a1");
		parent.set("bNo", "b2");
		grandParent.set("cNo", "c3");

		boolean exception = false;
		try {
			child.modify("bNo", "new");
		} catch (IllegalArgumentException e) {
			exception = true;
		}
		assertTrue(exception);

		exception = false;
		try {
			grandParent.modify("cNo", "new");
		} catch (IllegalArgumentException e) {
			exception = true;
		}
		assertTrue(exception);

		exception = false;
		try {
			child.modify("aNo", "new");
		} catch (IllegalArgumentException e) {
			exception = true;
		}
		assertTrue(exception);
	}

	@Test
	public void testRemoveValueComputationOnDispose() {
		IEclipseContext parent = EclipseContextFactory.create("ParentContext");
		IEclipseContext child = parent.createChild("ChildContext");
		parent.set("x", Integer.valueOf(1));
		parent.set("y", Integer.valueOf(1));
		parent.set("sum", new AddContextFunction());

		child.get("sum");
		assertEquals(1, listenersCount(child));
		child.dispose();
		assertEquals(0, listenersCount(parent));
	}

	@Test
	public void testNullInheritance() {
		IEclipseContext parent = EclipseContextFactory.create("ParentContext");
		IEclipseContext child = parent.createChild("ChildContext");
		parent.set("x", Integer.valueOf(1));
		child.set("x", null);
		assertNull(child.get("x"));
	}

	@Test
	public void testGetCFNotAValue() {
		IEclipseContext context = EclipseContextFactory.create("ParentContext");
		context.set("x", new ContextFunction() {

			@Override
			public Object compute(IEclipseContext context, String contextKey) {
				return IInjector.NOT_A_VALUE;
			}
		});

		// must call several times as the underlying ValueComputation wrapper is
		// created on the first time, but re-used for subsequent calls.
		assertNull(context.get("x"));
		assertNull(context.get("x"));
		assertNull(context.get("x"));
		context.dispose();
	}

	@Test
	public void testGetCFNotAValueToParent() {
		IEclipseContext parent = EclipseContextFactory.create("ParentContext");
		IEclipseContext child = parent.createChild();
		parent.set("x", Integer.valueOf(1));
		child.set("x", new ContextFunction() {

			@Override
			public Object compute(IEclipseContext context, String contextKey) {
				return IInjector.NOT_A_VALUE;
			}
		});

		assertEquals(1, child.get("x"));
		parent.dispose();
	}

	@Test
	public void testContextFunctionOrdering() {
		IEclipseContext osgiContext = EclipseContextFactory.getServiceContext(FrameworkUtil.getBundle(getClass()).getBundleContext());
		assertEquals("High",osgiContext.get("test.contextfunction.ranking"));
	}

	private int listenersCount(IEclipseContext context) {
		return ((EclipseContext) context).getListeners().size();
	}
}
