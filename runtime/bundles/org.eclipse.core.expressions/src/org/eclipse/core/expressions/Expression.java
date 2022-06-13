/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.core.expressions;

import org.eclipse.core.runtime.CoreException;

/**
 * Abstract base class for all expressions provided by the common
 * expression language.
 * <p>
 * An expression is evaluated by calling {@link #evaluate(IEvaluationContext)}.
 * </p>
 * <p>
 * This class may be subclassed to provide specific expressions.
 * </p>
 *
 * @since 3.0
 */
public abstract class Expression {

	/**
	 * Checks whether two objects are equal using the
	 * <code>equals(Object)</code> method of the <code>left</code> object.
	 * This method handles <code>null</code> for either the <code>left</code>
	 * or <code>right</code> object.
	 *
	 * @param left the first object to compare; may be <code>null</code>.
	 * @param right the second object to compare; may be <code>null</code>.
	 * @return <code>true</code> if the two objects are equivalent;
	 *         <code>false</code> otherwise.
	 *
	 * @since 3.2
	 */
	protected static final boolean equals(final Object left, final Object right) {
		return left == null ? right == null : ((right != null) && left
				.equals(right));
	}

	/**
	 * Tests whether two arrays of objects are equal to each other. The arrays
	 * must not be <code>null</code>, but their elements may be
	 * <code>null</code>.
	 *
	 * @param leftArray the left array to compare; may be <code>null</code>, and
	 *  may be empty and may contain <code>null</code> elements.
	 * @param rightArray the right array to compare; may be <code>null</code>,
	 *  and may be empty and may contain <code>null</code> elements.
	 *
	 * @return <code>true</code> if the arrays are equal length and the elements
	 *  at the same position are equal; <code>false</code> otherwise.
	 *
	 * @since 3.2
	 */
	protected static final boolean equals(final Object[] leftArray, final Object[] rightArray) {
		if (leftArray == rightArray) {
			return true;
		}

		if (leftArray == null) {
			return (rightArray == null);
		} else if (rightArray == null) {
			return false;
		}

		if (leftArray.length != rightArray.length) {
			return false;
		}

		for (int i= 0; i < leftArray.length; i++) {
			final Object left= leftArray[i];
			final Object right= rightArray[i];
			final boolean equal= (left == null) ? (right == null) : (left.equals(right));
			if (!equal) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Returns the hash code for the given <code>object</code>. This method
	 * handles <code>null</code>.
	 *
	 * @param object the object for which the hash code is desired; may be
	 *  <code>null</code>.
	 *
	 * @return The hash code of the object; zero if the object is
	 *  <code>null</code>.
	 *
	 * @since 3.2
	 */
	protected static final int hashCode(final Object object) {
		return object != null ? object.hashCode() : 0;
	}

	/**
	 * Returns the hash code for the given array. This method handles
	 * <code>null</code>.
	 *
	 * @param array the array for which the hash code is desired; may be
	 *  <code>null</code>.
	 * @return the hash code of the array; zero if the object is
	 *  <code>null</code>.
	 *
	 * @since 3.2
	 */
	protected static final int hashCode(final Object[] array) {
		if (array == null) {
			return 0;
		}
		int hashCode= array.getClass().getName().hashCode();
		for (Object element : array) {
			hashCode= hashCode * HASH_FACTOR + hashCode(element);
		}
		return hashCode;
	}

	/**
	 * The constant integer hash code value meaning the hash code has not yet
	 * been computed.
	 */
	protected static final int HASH_CODE_NOT_COMPUTED = -1;

	/**
	 * A factor for computing the hash code for all expressions.
	 */
	protected static final int HASH_FACTOR = 89;

	/**
	 * Name of the value attribute of an expression (value is <code>value</code>).
	 */
	protected static final String ATT_VALUE= "value"; //$NON-NLS-1$

	/**
	 * The expression corresponding to {@link EvaluationResult#TRUE}.
	 */
	public static final Expression TRUE= new Expression() {
		@Override
		public EvaluationResult evaluate(IEvaluationContext context) {
			return EvaluationResult.TRUE;
		}
		@Override
		public void collectExpressionInfo(ExpressionInfo info) {
		}
	};

	/**
	 * The expression corresponding to {@link EvaluationResult#FALSE}.
	 */
	public static final Expression FALSE= new Expression() {
		@Override
		public EvaluationResult evaluate(IEvaluationContext context) {
			return EvaluationResult.FALSE;
		}
		@Override
		public void collectExpressionInfo(ExpressionInfo info) {
		}
	};

	/**
	 * The hash code for this object. This value is computed lazily.  If it is
	 * not yet computed, it is equal to {@link #HASH_CODE_NOT_COMPUTED}.
	 */
	private transient int fHashCode= HASH_CODE_NOT_COMPUTED;

	/**
	 * Evaluates this expression.
	 *
	 * @param context an evaluation context providing information like variable,
	 *  name spaces, etc. necessary to evaluate this expression
	 *
	 * @return the result of the expression evaluation
	 *
	 * @throws CoreException if the evaluation failed. The concrete reason is
	 *  defined by the subclass implementing this method
	 */
	public abstract EvaluationResult evaluate(IEvaluationContext context) throws CoreException;

	/**
	 * Computes the expression information for the given expression tree.
	 * <p>
	 * This is a convenience method for collecting the expression information
	 * using {@link Expression#collectExpressionInfo(ExpressionInfo)}.
	 * </p>
	 *
	 * @return the expression information
	 *
	 * @since 3.2
	 */
	public final ExpressionInfo computeExpressionInfo() {
		ExpressionInfo result= new ExpressionInfo();
		collectExpressionInfo(result);
		return result;
	}

	/**
	 * Collects information about this expression tree. This default
	 * implementation add the expression's type to the set of misbehaving
	 * expression types.
	 *
	 * @param info the expression information object used
	 *  to collect the information
	 *
	 * @since 3.2
	 */
	public void collectExpressionInfo(ExpressionInfo info) {
		info.addMisBehavingExpressionType(getClass());
	}

	/**
	 * Method to compute the hash code for this object. The result
	 * returned from this method in cached in the <code>fHashCode</code>
	 * field. If the value returned from the method equals {@link #HASH_CODE_NOT_COMPUTED}
	 * (e.g. <code>-1</code>) then the value is incremented by one.
	 * <p>
	 * This default implementation calls <code>super.hashCode()</code>
	 * </p>
	 * @return a hash code for this object.
	 *
	 * @since 3.2
	 */
	protected int computeHashCode() {
		return super.hashCode();
	}

	@Override
	public int hashCode() {
		if (fHashCode != HASH_CODE_NOT_COMPUTED)
			return fHashCode;
		fHashCode= computeHashCode();
		if (fHashCode == HASH_CODE_NOT_COMPUTED)
			fHashCode++;
		return fHashCode;
	}
}
