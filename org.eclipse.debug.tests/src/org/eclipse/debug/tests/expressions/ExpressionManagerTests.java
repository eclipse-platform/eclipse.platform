/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
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
package org.eclipse.debug.tests.expressions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IExpressionListener;
import org.eclipse.debug.core.IExpressionManager;
import org.eclipse.debug.core.IExpressionsListener;
import org.eclipse.debug.core.model.IExpression;
import org.eclipse.debug.core.model.IWatchExpression;
import org.eclipse.debug.internal.core.ExpressionManager;
import org.eclipse.debug.internal.core.IExpressionsListener2;
import org.eclipse.debug.tests.AbstractDebugTest;
import org.junit.After;
import org.junit.Test;

/**
 * Tests expression manager and listener call backs
 */
public class ExpressionManagerTests extends AbstractDebugTest {

	static class SinlgeListener implements IExpressionListener {

		List<IExpression> added = new ArrayList<>();
		List<IExpression> removed = new ArrayList<>();
		List<IExpression> changed = new ArrayList<>();
		int addedCallbacks = 0;
		int removedCallbacks = 0;
		int changedCallbacks = 0;

		@Override
		public void expressionAdded(IExpression expression) {
			added.add(expression);
			addedCallbacks++;
		}

		@Override
		public void expressionRemoved(IExpression expression) {
			removed.add(expression);
			removedCallbacks++;
		}

		@Override
		public void expressionChanged(IExpression expression) {
			changed.add(expression);
			changedCallbacks++;
		}

	}

	static class MultiListener implements IExpressionsListener {

		List<IExpression> added = new ArrayList<>();
		List<IExpression> removed = new ArrayList<>();
		List<IExpression> changed = new ArrayList<>();
		int addedCallbacks = 0;
		int removedCallbacks = 0;
		int changedCallbacks = 0;

		@Override
		public void expressionsAdded(IExpression[] expressions) {
			Collections.addAll(added, expressions);
			addedCallbacks++;
		}

		@Override
		public void expressionsRemoved(IExpression[] expressions) {
			Collections.addAll(removed, expressions);
			removedCallbacks++;
		}

		@Override
		public void expressionsChanged(IExpression[] expressions) {
			Collections.addAll(changed, expressions);
			changedCallbacks++;
		}

	}

	static class InsertMoveListener extends MultiListener implements IExpressionsListener2 {

		List<IExpression> moved = new ArrayList<>();
		List<IExpression> inserted = new ArrayList<>();
		int insertIndex = -1;
		int movedCallbacks = 0;
		int insertedCallbacks = 0;

		@Override
		public void expressionsMoved(IExpression[] expressions, int index) {
			Collections.addAll(moved, expressions);
			movedCallbacks++;
			insertIndex = index;
		}

		@Override
		public void expressionsInserted(IExpression[] expressions, int index) {
			Collections.addAll(inserted, expressions);
			insertedCallbacks++;
			insertIndex = index;
		}

	}

	/**
	 * Returns the expression manager.
	 *
	 * @return expression manager
	 */
	protected IExpressionManager getManager() {
		return DebugPlugin.getDefault().getExpressionManager();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		// remove all expressions from the manager
		getManager().removeExpressions(getManager().getExpressions());
		super.tearDown();
	}

	/**
	 * Returns the index of the given expression in the given list or -1 if not present.
	 *
	 * @param expression candidate
	 * @param list list to search
	 * @return index or -1
	 */
	private int indexOf(IExpression expression, IExpression[] list) {
		for (int i = 0; i < list.length; i++) {
			if (expression.equals(list[i])) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Add expressions and ensure proper call backs are received.
	 */
	@Test
	public void testAddExpressions() {
		IExpressionManager manager = getManager();
		SinlgeListener single = new SinlgeListener();
		MultiListener multi = new MultiListener();
		manager.addExpressionListener(single);
		manager.addExpressionListener(multi);
		try {
			IWatchExpression exp1 = manager.newWatchExpression("exp1"); //$NON-NLS-1$
			IWatchExpression exp2 = manager.newWatchExpression("exp2"); //$NON-NLS-1$
			IWatchExpression exp3 = manager.newWatchExpression("exp3"); //$NON-NLS-1$
			manager.addExpressions(new IExpression[]{exp1, exp2, exp3});
			IExpression[] expressions = manager.getExpressions();
			assertEquals("Wrong number of expressions", 3, expressions.length); //$NON-NLS-1$
			assertEquals(single.addedCallbacks, 3);
			assertEquals(3, single.added.size());
			assertEquals(0, single.added.indexOf(exp1));
			assertEquals(1, single.added.indexOf(exp2));
			assertEquals(2, single.added.indexOf(exp3));
			assertEquals(0, single.removedCallbacks);
			assertEquals(0, single.changedCallbacks);
			assertEquals(1, multi.addedCallbacks);
			assertEquals(0, multi.removedCallbacks);
			assertEquals(0, multi.changedCallbacks);
			assertEquals(0, indexOf(exp1, expressions));
			assertEquals(1, indexOf(exp2, expressions));
			assertEquals(2, indexOf(exp3, expressions));
		} finally {
			manager.removeExpressionListener(single);
			manager.removeExpressionListener(multi);
		}
	}

	/**
	 * Remove expressions and ensure proper call backs are received.
	 */
	@Test
	public void testRemoveExpressions() {
		IExpressionManager manager = getManager();
		SinlgeListener single = new SinlgeListener();
		MultiListener multi = new MultiListener();
		manager.addExpressionListener(single);
		manager.addExpressionListener(multi);
		try {
			IWatchExpression exp1 = manager.newWatchExpression("exp1"); //$NON-NLS-1$
			IWatchExpression exp2 = manager.newWatchExpression("exp2"); //$NON-NLS-1$
			IWatchExpression exp3 = manager.newWatchExpression("exp3"); //$NON-NLS-1$
			manager.addExpressions(new IExpression[]{exp1, exp2, exp3});
			manager.removeExpressions(new IExpression[]{exp1, exp3});
			IExpression[] expressions = manager.getExpressions();
			assertEquals("Wrong number of expressions", 1, expressions.length); //$NON-NLS-1$
			assertEquals(single.addedCallbacks, 3);
			assertEquals(3, single.added.size());
			assertEquals(0, single.added.indexOf(exp1));
			assertEquals(1, single.added.indexOf(exp2));
			assertEquals(2, single.added.indexOf(exp3));
			assertEquals(2, single.removedCallbacks);
			assertEquals(0, single.removed.indexOf(exp1));
			assertEquals(1, single.removed.indexOf(exp3));
			assertEquals(0, single.changedCallbacks);
			assertEquals(1, multi.addedCallbacks);
			assertEquals(1, multi.removedCallbacks);
			assertEquals(0, multi.removed.indexOf(exp1));
			assertEquals(1, multi.removed.indexOf(exp3));
			assertEquals(0, multi.changedCallbacks);
			assertEquals(-1, indexOf(exp1, expressions));
			assertEquals(0, indexOf(exp2, expressions));
			assertEquals(-1, indexOf(exp3, expressions));
		} finally {
			manager.removeExpressionListener(single);
			manager.removeExpressionListener(multi);
		}
	}

	/**
	 * Change expressions and ensure proper call backs are received.
	 */
	@Test
	public void testChangeExpressions() {
		IExpressionManager manager = getManager();
		SinlgeListener single = new SinlgeListener();
		MultiListener multi = new MultiListener();
		manager.addExpressionListener(single);
		manager.addExpressionListener(multi);
		try {
			IWatchExpression exp1 = manager.newWatchExpression("exp1"); //$NON-NLS-1$
			IWatchExpression exp2 = manager.newWatchExpression("exp2"); //$NON-NLS-1$
			IWatchExpression exp3 = manager.newWatchExpression("exp3"); //$NON-NLS-1$
			manager.addExpressions(new IExpression[]{exp1, exp2, exp3});
			IExpression[] expressions = manager.getExpressions();
			exp1.setEnabled(false);
			exp2.setExpressionText("exp2changed"); //$NON-NLS-1$
			assertEquals("Wrong number of expressions", 3, expressions.length); //$NON-NLS-1$
			assertEquals(single.addedCallbacks, 3);
			assertEquals(3, single.added.size());
			assertEquals(0, single.added.indexOf(exp1));
			assertEquals(1, single.added.indexOf(exp2));
			assertEquals(2, single.added.indexOf(exp3));
			assertEquals(0, single.removedCallbacks);
			assertEquals(2, single.changedCallbacks);
			assertEquals(0, single.changed.indexOf(exp1));
			assertEquals(1, single.changed.indexOf(exp2));
			assertEquals(1, multi.addedCallbacks);
			assertEquals(0, multi.removedCallbacks);
			assertEquals(2, multi.changedCallbacks);
			assertEquals(0, multi.changed.indexOf(exp1));
			assertEquals(1, multi.changed.indexOf(exp2));
			assertEquals(0, indexOf(exp1, expressions));
			assertEquals(1, indexOf(exp2, expressions));
			assertEquals(2, indexOf(exp3, expressions));
		} finally {
			manager.removeExpressionListener(single);
			manager.removeExpressionListener(multi);
		}
	}

	/**
	 * Insert expressions and ensure proper call backs are received.
	 */
	@Test
	public void testInsertBeforeExpressions() {
		ExpressionManager manager = (ExpressionManager) getManager();
		SinlgeListener single = new SinlgeListener();
		MultiListener multi = new MultiListener();
		InsertMoveListener insert = new InsertMoveListener();
		try {
			IWatchExpression exp1 = manager.newWatchExpression("exp1"); //$NON-NLS-1$
			IWatchExpression exp2 = manager.newWatchExpression("exp2"); //$NON-NLS-1$
			IWatchExpression exp3 = manager.newWatchExpression("exp3"); //$NON-NLS-1$
			IWatchExpression exp4 = manager.newWatchExpression("exp4"); //$NON-NLS-1$
			IWatchExpression exp5 = manager.newWatchExpression("exp5"); //$NON-NLS-1$
			manager.addExpressions(new IExpression[]{exp1, exp2, exp3});
			IExpression[] expressions = manager.getExpressions();
			assertEquals("Wrong number of expressions", 3, expressions.length); //$NON-NLS-1$
			assertEquals(0, indexOf(exp1, expressions));
			assertEquals(1, indexOf(exp2, expressions));
			assertEquals(2, indexOf(exp3, expressions));
			// add listeners
			manager.addExpressionListener(single);
			manager.addExpressionListener(multi);
			manager.addExpressionListener(insert);

			manager.insertExpressions(new IExpression[] {exp4, exp5}, exp2, true);

			assertEquals(2, single.addedCallbacks);
			assertEquals(2, single.added.size());
			assertEquals(0, single.removedCallbacks);
			assertEquals(0, single.changedCallbacks);
			assertEquals(1, multi.addedCallbacks);
			assertEquals(2, multi.added.size());
			assertEquals(0, multi.removedCallbacks);
			assertEquals(0, multi.changedCallbacks);
			assertEquals(1, insert.insertedCallbacks);
			assertEquals(1, insert.insertIndex);
			assertEquals(0, insert.movedCallbacks);
			assertEquals(2, insert.inserted.size());
			assertEquals(0, insert.inserted.indexOf(exp4));
			assertEquals(1, insert.inserted.indexOf(exp5));

			expressions = manager.getExpressions();
			assertEquals("Wrong number of expressions", 5, expressions.length); //$NON-NLS-1$
			assertEquals(0, indexOf(exp1, expressions));
			assertEquals(1, indexOf(exp4, expressions));
			assertEquals(2, indexOf(exp5, expressions));
			assertEquals(3, indexOf(exp2, expressions));
			assertEquals(4, indexOf(exp3, expressions));

		} finally {
			manager.removeExpressionListener(single);
			manager.removeExpressionListener(multi);
			manager.removeExpressionListener(insert);
		}
	}

	/**
	 * Insert expressions and ensure proper call backs are received.
	 */
	@Test
	public void testInsertAfterExpressions() {
		ExpressionManager manager = (ExpressionManager) getManager();
		SinlgeListener single = new SinlgeListener();
		MultiListener multi = new MultiListener();
		InsertMoveListener insert = new InsertMoveListener();
		try {
			IWatchExpression exp1 = manager.newWatchExpression("exp1"); //$NON-NLS-1$
			IWatchExpression exp2 = manager.newWatchExpression("exp2"); //$NON-NLS-1$
			IWatchExpression exp3 = manager.newWatchExpression("exp3"); //$NON-NLS-1$
			IWatchExpression exp4 = manager.newWatchExpression("exp4"); //$NON-NLS-1$
			IWatchExpression exp5 = manager.newWatchExpression("exp5"); //$NON-NLS-1$
			manager.addExpressions(new IExpression[]{exp1, exp2, exp3});
			IExpression[] expressions = manager.getExpressions();
			assertEquals("Wrong number of expressions", 3, expressions.length); //$NON-NLS-1$
			assertEquals(0, indexOf(exp1, expressions));
			assertEquals(1, indexOf(exp2, expressions));
			assertEquals(2, indexOf(exp3, expressions));
			// add listeners
			manager.addExpressionListener(single);
			manager.addExpressionListener(multi);
			manager.addExpressionListener(insert);

			manager.insertExpressions(new IExpression[] {exp4, exp5}, exp2, false);

			assertEquals(2, single.addedCallbacks);
			assertEquals(2, single.added.size());
			assertEquals(0, single.removedCallbacks);
			assertEquals(0, single.changedCallbacks);
			assertEquals(1, multi.addedCallbacks);
			assertEquals(2, multi.added.size());
			assertEquals(0, multi.removedCallbacks);
			assertEquals(0, multi.changedCallbacks);
			assertEquals(1, insert.insertedCallbacks);
			assertEquals(2, insert.insertIndex);
			assertEquals(0, insert.movedCallbacks);
			assertEquals(2, insert.inserted.size());
			assertEquals(0, insert.inserted.indexOf(exp4));
			assertEquals(1, insert.inserted.indexOf(exp5));

			expressions = manager.getExpressions();
			assertEquals("Wrong number of expressions", 5, expressions.length); //$NON-NLS-1$
			assertEquals(0, indexOf(exp1, expressions));
			assertEquals(1, indexOf(exp2, expressions));
			assertEquals(2, indexOf(exp4, expressions));
			assertEquals(3, indexOf(exp5, expressions));
			assertEquals(4, indexOf(exp3, expressions));

		} finally {
			manager.removeExpressionListener(single);
			manager.removeExpressionListener(multi);
			manager.removeExpressionListener(insert);
		}
	}

	/**
	 * Move expressions and ensure proper call backs are received.
	 */
	@Test
	public void testMoveBeforeExpressions() {
		ExpressionManager manager = (ExpressionManager) getManager();
		SinlgeListener single = new SinlgeListener();
		MultiListener multi = new MultiListener();
		InsertMoveListener insert = new InsertMoveListener();
		try {
			IWatchExpression exp1 = manager.newWatchExpression("exp1"); //$NON-NLS-1$
			IWatchExpression exp2 = manager.newWatchExpression("exp2"); //$NON-NLS-1$
			IWatchExpression exp3 = manager.newWatchExpression("exp3"); //$NON-NLS-1$
			IWatchExpression exp4 = manager.newWatchExpression("exp4"); //$NON-NLS-1$
			IWatchExpression exp5 = manager.newWatchExpression("exp5"); //$NON-NLS-1$
			manager.addExpressions(new IExpression[]{exp1, exp2, exp3, exp4, exp5});
			// add listeners
			manager.addExpressionListener(single);
			manager.addExpressionListener(multi);
			manager.addExpressionListener(insert);

			manager.moveExpressions(new IExpression[]{exp1,exp2}, exp5, true);

			assertEquals(0, single.addedCallbacks);
			assertEquals(0, single.removedCallbacks);
			assertEquals(0, single.changedCallbacks);
			assertEquals(0, multi.addedCallbacks);
			assertEquals(0, multi.removedCallbacks);
			assertEquals(0, multi.changedCallbacks);
			assertEquals(0, insert.insertedCallbacks);
			assertEquals(1, insert.movedCallbacks);
			assertEquals(2, insert.moved.size());
			assertEquals(0, insert.moved.indexOf(exp1));
			assertEquals(1, insert.moved.indexOf(exp2));
			assertEquals(2, insert.insertIndex);

			IExpression[] expressions = manager.getExpressions();
			assertEquals("Wrong number of expressions", 5, expressions.length); //$NON-NLS-1$
			assertEquals(0, indexOf(exp3, expressions));
			assertEquals(1, indexOf(exp4, expressions));
			assertEquals(2, indexOf(exp1, expressions));
			assertEquals(3, indexOf(exp2, expressions));
			assertEquals(4, indexOf(exp5, expressions));

		} finally {
			manager.removeExpressionListener(single);
			manager.removeExpressionListener(multi);
			manager.removeExpressionListener(insert);
		}
	}

	/**
	 * Move expressions and ensure proper call backs are received.
	 */
	@Test
	public void testMoveAfterExpressions() {
		ExpressionManager manager = (ExpressionManager) getManager();
		SinlgeListener single = new SinlgeListener();
		MultiListener multi = new MultiListener();
		InsertMoveListener insert = new InsertMoveListener();
		try {
			IWatchExpression exp1 = manager.newWatchExpression("exp1"); //$NON-NLS-1$
			IWatchExpression exp2 = manager.newWatchExpression("exp2"); //$NON-NLS-1$
			IWatchExpression exp3 = manager.newWatchExpression("exp3"); //$NON-NLS-1$
			IWatchExpression exp4 = manager.newWatchExpression("exp4"); //$NON-NLS-1$
			IWatchExpression exp5 = manager.newWatchExpression("exp5"); //$NON-NLS-1$
			manager.addExpressions(new IExpression[]{exp1, exp2, exp3, exp4, exp5});
			// add listeners
			manager.addExpressionListener(single);
			manager.addExpressionListener(multi);
			manager.addExpressionListener(insert);

			manager.moveExpressions(new IExpression[]{exp1,exp2}, exp3, false);

			assertEquals(0, single.addedCallbacks);
			assertEquals(0, single.removedCallbacks);
			assertEquals(0, single.changedCallbacks);
			assertEquals(0, multi.addedCallbacks);
			assertEquals(0, multi.removedCallbacks);
			assertEquals(0, multi.changedCallbacks);
			assertEquals(0, insert.insertedCallbacks);
			assertEquals(1, insert.movedCallbacks);
			assertEquals(2, insert.moved.size());
			assertEquals(0, insert.moved.indexOf(exp1));
			assertEquals(1, insert.moved.indexOf(exp2));
			assertEquals(1, insert.insertIndex);

			IExpression[] expressions = manager.getExpressions();
			assertEquals("Wrong number of expressions", 5, expressions.length); //$NON-NLS-1$
			assertEquals(0, indexOf(exp3, expressions));
			assertEquals(1, indexOf(exp1, expressions));
			assertEquals(2, indexOf(exp2, expressions));
			assertEquals(3, indexOf(exp4, expressions));
			assertEquals(4, indexOf(exp5, expressions));

		} finally {
			manager.removeExpressionListener(single);
			manager.removeExpressionListener(multi);
			manager.removeExpressionListener(insert);
		}
	}

	/**
	 * Test persist and restore of expressions
	 */
	@Test
	public void testPersistExpressions() {
		ExpressionManager manager = (ExpressionManager) getManager();
		IWatchExpression exp1 = manager.newWatchExpression("exp1"); //$NON-NLS-1$
		IWatchExpression exp2 = manager.newWatchExpression("exp2"); //$NON-NLS-1$
		IWatchExpression exp3 = manager.newWatchExpression("exp3"); //$NON-NLS-1$
		IWatchExpression exp4 = manager.newWatchExpression("exp4"); //$NON-NLS-1$
		IWatchExpression exp5 = manager.newWatchExpression("exp5"); //$NON-NLS-1$
		manager.addExpressions(new IExpression[]{exp1, exp2, exp3, exp4, exp5});
		manager.storeWatchExpressions();

		// create a new manager that will restore the expressions
		ExpressionManager manager2 = new ExpressionManager();
		IExpression[] expressions = manager2.getExpressions();
		assertEquals("Wrong number of expressions", 5, expressions.length); //$NON-NLS-1$
		assertEquals("exp1", expressions[0].getExpressionText()); //$NON-NLS-1$
		assertEquals("exp2", expressions[1].getExpressionText()); //$NON-NLS-1$
		assertEquals("exp3", expressions[2].getExpressionText()); //$NON-NLS-1$
		assertEquals("exp4", expressions[3].getExpressionText()); //$NON-NLS-1$
		assertEquals("exp5", expressions[4].getExpressionText()); //$NON-NLS-1$
	}

	/**
	 * Tests concurrent access to expressions.
	 *
	 * @throws InterruptedException
	 */
	@Test
	public void testConcurrentAccess() throws InterruptedException {
		final boolean[] done = new boolean[]{false};
		final Exception[] ex = new Exception[]{null};
		Runnable add = () -> {
			try {
				for (int i = 0; i < 1000; i++) {
					getManager().addExpression(getManager().newWatchExpression(Integer.toHexString(i)));
				}
				done[0] = true;
			} catch (Exception e) {
				ex[0] = e;
			}
		};
		Runnable remove = () -> {
			try {
				do {
					getManager().removeExpressions(getManager().getExpressions());
				} while (!done[0] || getManager().getExpressions().length > 0);
			} catch (Exception e) {
				ex[0] = e;
			}
		};
		Thread t1 = new Thread(add);
		Thread t2 = new Thread(remove);
		t1.start();
		t2.start();
		t1.join();
		t2.join();
		assertEquals(0, getManager().getExpressions().length);
		assertNull(ex[0]);
	}

}
