/*******************************************************************************
 * Copyright (c) 2000, 2008, 2009 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation.
 *     Ian Phillips - additional expressions support ( "-N)", "(N-" ).
 *******************************************************************************/
package org.eclipse.core.expressions;

import java.util.Collection;

import org.w3c.dom.Element;

import org.eclipse.core.internal.expressions.Expressions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

/**
 * @since 3.7
 */
public class CountExpression extends Expression {

	private static final int GREATER_THAN_OR_EQUAL = 9; // [N-
	private static final int LESS_THAN_OR_EQUAL = 8; // -N]
	private static final int GREATER_THAN = 7; // (N-
	private static final int LESS_THAN = 6; // -N)
	private static final int ANY_NUMBER = 5; // *
	private static final int EXACT = 4; // N
	private static final int ONE_OR_MORE = 3; // +
	private static final int NONE_OR_ONE = 2; // ?
	private static final int NONE = 1; // !
	private static final int UNKNOWN = 0;

	/**
	 * The seed for the hash code for all count expressions.
	 */
	private static final int HASH_INITIAL = CountExpression.class.getName().hashCode();

	private int fMode;
	private int fSize;

	public CountExpression(IConfigurationElement configElement) {
		String size = configElement.getAttribute(ATT_VALUE);
		initializeSize(size);
	}

	public CountExpression(Element element) {
		String size = element.getAttribute(ATT_VALUE);
		initializeSize(size.isEmpty() ? null : size);
	}

	public CountExpression(String size) {
		initializeSize(size);
	}

	private void initializeSize(String size) {
		if (size == null)
			size = "*"; //$NON-NLS-1$
		if ("*".equals(size)) //$NON-NLS-1$
			fMode = ANY_NUMBER;
		else if ("?".equals(size)) //$NON-NLS-1$
			fMode = NONE_OR_ONE;
		else if ("!".equals(size)) //$NON-NLS-1$
			fMode = NONE;
		else if ("+".equals(size)) //$NON-NLS-1$
			fMode = ONE_OR_MORE;
		else if (size.charAt(0) == '-' && size.charAt(size.length() - 1) == ')') {
			try {
				fMode = LESS_THAN;
				fSize = Integer.parseInt(size.substring(1, size.length() - 1));
			} catch (NumberFormatException e) {
				fMode = UNKNOWN;
			}
		} else if (size.charAt(0) == '-' && size.charAt(size.length() - 1) == ']') {
			try {
				fMode = LESS_THAN_OR_EQUAL;
				fSize = Integer.parseInt(size.substring(1, size.length() - 1));
			} catch (NumberFormatException e) {
				fMode = UNKNOWN;
			}
		} else if (size.charAt(0) == '(' && size.charAt(size.length() - 1) == '-') {
			try {
				fMode = GREATER_THAN;
				fSize = Integer.parseInt(size.substring(1, size.length() - 1));
			} catch (NumberFormatException e) {
				fMode = UNKNOWN;
			}
		} else if (size.charAt(0) == '[' && size.charAt(size.length() - 1) == '-') {
			try {
				fMode = GREATER_THAN_OR_EQUAL;
				fSize = Integer.parseInt(size.substring(1, size.length() - 1));
			} catch (NumberFormatException e) {
				fMode = UNKNOWN;
			}
		} else {
			try {
				fSize = Integer.parseInt(size);
				fMode = EXACT;
			} catch (NumberFormatException e) {
				fMode = UNKNOWN;
			}
		}
	}

	@Override
	public EvaluationResult evaluate(IEvaluationContext context) throws CoreException {
		Object var = context.getDefaultVariable();
		int size;
		if (var instanceof Collection collection) {
			size = collection.size();
		} else {
			ICountable countable = Expressions.getAsICountable(var, this);
			if (countable == null)
				return EvaluationResult.NOT_LOADED;
			size = countable.count();
		}
		switch (fMode) {
		case UNKNOWN:
			return EvaluationResult.FALSE;
		case NONE:
			return EvaluationResult.valueOf(size == 0);
		case NONE_OR_ONE:
			return EvaluationResult.valueOf(size == 0 || size == 1);
		case ONE_OR_MORE:
			return EvaluationResult.valueOf(size >= 1);
		case EXACT:
			return EvaluationResult.valueOf(fSize == size);
		case ANY_NUMBER:
			return EvaluationResult.TRUE;
		case LESS_THAN:
			return EvaluationResult.valueOf(size < fSize);
		case GREATER_THAN:
			return EvaluationResult.valueOf(size > fSize);
		case LESS_THAN_OR_EQUAL:
			return EvaluationResult.valueOf(size <= fSize);
		case GREATER_THAN_OR_EQUAL:
			return EvaluationResult.valueOf(size >= fSize);
		}
		return EvaluationResult.FALSE;
	}

	@Override
	public void collectExpressionInfo(ExpressionInfo info) {
		info.markDefaultVariableAccessed();
	}

	@Override
	public boolean equals(final Object object) {
		if (!(object instanceof CountExpression))
			return false;

		final CountExpression that = (CountExpression) object;
		return (this.fMode == that.fMode) && (this.fSize == that.fSize);
	}

	@Override
	protected int computeHashCode() {
		return HASH_INITIAL * HASH_FACTOR + fMode * HASH_FACTOR + fSize;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName());
		builder.append(" [ size="); //$NON-NLS-1$
		builder.append(fSize);
		builder.append(", mode: "); //$NON-NLS-1$
		builder.append(fMode);
		switch (fMode) {
		case GREATER_THAN_OR_EQUAL:
			builder.append(" GREATER_THAN_OR_EQUAL"); //$NON-NLS-1$ [N-
			break;
		case LESS_THAN_OR_EQUAL:
			builder.append(" LESS_THAN_OR_EQUAL"); //$NON-NLS-1$ // -N]
			break;
		case GREATER_THAN:
			builder.append(" GREATER_THAN"); //$NON-NLS-1$
			break;
		case LESS_THAN:
			builder.append(" LESS_THAN"); //$NON-NLS-1$ // -N)
			break;
		case ANY_NUMBER:
			builder.append(" ANY_NUMBER"); //$NON-NLS-1$ // *
			break;
		case EXACT:
			builder.append(" EXACT"); //$NON-NLS-1$ // N
			break;
		case ONE_OR_MORE:
			builder.append(" ONE_OR_MORE"); //$NON-NLS-1$ // +
			break;
		case NONE_OR_ONE:
			builder.append(" NONE_OR_ONE"); //$NON-NLS-1$ // ?
			break;
		case NONE:
			builder.append(" NONE"); //$NON-NLS-1$ // !
			break;
		case UNKNOWN:
			builder.append(" UNKNOWN"); //$NON-NLS-1$ ;
			break;
		default:
			break;
		}
		builder.append("]"); //$NON-NLS-1$
		return builder.toString();
	}
}
