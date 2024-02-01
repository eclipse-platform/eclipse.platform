/*******************************************************************************
 * Copyright (c) 2009, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Ian Phillips - initial implementation.
 *******************************************************************************/
package org.eclipse.core.internal.expressions.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.eclipse.core.expressions.CountExpression;
import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.expressions.EvaluationResult;

import org.eclipse.core.runtime.CoreException;

public class CountExpressionTest {

	private static EvaluationContext evaluationContext(int size) {
		List<Integer> variable = new ArrayList<>(size + 1);
		for (int i = 0; i < size; ++i)
			variable.add(Integer.valueOf(i));
		return new EvaluationContext(null, variable);
	}

	@Test
	public void testNoneExpression() throws CoreException {
		CountExpression e = new CountExpression("!"); //$NON-NLS-1$
		assertEquals(EvaluationResult.TRUE, e.evaluate(evaluationContext(0)));
		assertEquals(EvaluationResult.FALSE, e.evaluate(evaluationContext(1)));
	}

	@Test
	public void testNoneOrOneExpression() throws CoreException {
		CountExpression e = new CountExpression("?"); //$NON-NLS-1$
		assertEquals(EvaluationResult.TRUE, e.evaluate(evaluationContext(0)));
		assertEquals(EvaluationResult.TRUE, e.evaluate(evaluationContext(1)));
		assertEquals(EvaluationResult.FALSE, e.evaluate(evaluationContext(2)));
	}

	@Test
	public void testExactExpression() throws CoreException {
		CountExpression e = new CountExpression("5"); //$NON-NLS-1$
		assertEquals(EvaluationResult.TRUE, e.evaluate(evaluationContext(5)));
		assertEquals(EvaluationResult.FALSE, e.evaluate(evaluationContext(7)));
	}

	@Test
	public void testAnyNumberExpression() throws CoreException {
		CountExpression e = new CountExpression("*"); //$NON-NLS-1$
		assertEquals(EvaluationResult.TRUE, e.evaluate(evaluationContext(5)));
	}

	@Test
	public void testLessThanOrEqualToExpression() throws CoreException {
		CountExpression e = new CountExpression("-3]"); //$NON-NLS-1$
		assertEquals(EvaluationResult.TRUE, e.evaluate(evaluationContext(1)));
		assertEquals(EvaluationResult.TRUE, e.evaluate(evaluationContext(3)));
		assertEquals(EvaluationResult.FALSE, e.evaluate(evaluationContext(4)));
	}

	@Test
	public void testLessThanExpression() throws CoreException {
		CountExpression e = new CountExpression("-3)"); //$NON-NLS-1$
		assertEquals(EvaluationResult.TRUE, e.evaluate(evaluationContext(1)));
		assertEquals(EvaluationResult.FALSE, e.evaluate(evaluationContext(3)));
		assertEquals(EvaluationResult.FALSE, e.evaluate(evaluationContext(4)));
	}

	@Test
	public void testGreaterThanOrEqualToExpression() throws CoreException {
		CountExpression e = new CountExpression("[3-"); //$NON-NLS-1$
		assertEquals(EvaluationResult.TRUE, e.evaluate(evaluationContext(5)));
		assertEquals(EvaluationResult.TRUE, e.evaluate(evaluationContext(3)));
		assertEquals(EvaluationResult.FALSE, e.evaluate(evaluationContext(2)));
	}

	@Test
	public void testGreaterThanExpression() throws CoreException {
		CountExpression e = new CountExpression("(3-"); //$NON-NLS-1$
		assertEquals(EvaluationResult.TRUE, e.evaluate(evaluationContext(5)));
		assertEquals(EvaluationResult.FALSE, e.evaluate(evaluationContext(3)));
		assertEquals(EvaluationResult.FALSE, e.evaluate(evaluationContext(2)));
	}

}
