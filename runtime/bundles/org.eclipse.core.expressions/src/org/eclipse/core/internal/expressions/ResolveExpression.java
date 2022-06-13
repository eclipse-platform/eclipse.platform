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
package org.eclipse.core.internal.expressions;

import java.util.Arrays;

import org.w3c.dom.Element;

import org.eclipse.core.expressions.CompositeExpression;
import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionInfo;
import org.eclipse.core.expressions.IEvaluationContext;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

public class ResolveExpression extends CompositeExpression {

	private String fVariable;
	private Object[] fArgs;

	private static final String ATT_VARIABLE= "variable";  //$NON-NLS-1$
	private static final String ATT_ARGS= "args";  //$NON-NLS-1$

	/**
	 * The seed for the hash code for all resolve expressions.
	 */
	private static final int HASH_INITIAL= ResolveExpression.class.getName().hashCode();

	public ResolveExpression(IConfigurationElement configElement) throws CoreException {
		fVariable= configElement.getAttribute(ATT_VARIABLE);
		Expressions.checkAttribute(ATT_VARIABLE, fVariable);
		fArgs= Expressions.getArguments(configElement, ATT_ARGS);
	}

	public ResolveExpression(Element element) throws CoreException {
		fVariable= element.getAttribute(ATT_VARIABLE);
		Expressions.checkAttribute(ATT_VARIABLE, fVariable.isEmpty() ? null : fVariable);
		fArgs= Expressions.getArguments(element, ATT_ARGS);
	}

	public ResolveExpression(String variable, Object[] args) {
		Assert.isNotNull(variable);
		fVariable= variable;
		fArgs= args;
	}

	@Override
	public EvaluationResult evaluate(IEvaluationContext context) throws CoreException {
		Object variable= context.resolveVariable(fVariable, fArgs);
		if (variable == null) {
			throw new CoreException(new ExpressionStatus(
				ExpressionStatus.VARIABLE_NOT_DEFINED,
				Messages.format(ExpressionMessages.ResolveExpression_variable_not_defined, fVariable)));
		}
		return evaluateAnd(new EvaluationContext(context, variable));
	}

	@Override
	public void collectExpressionInfo(ExpressionInfo info) {
		ExpressionInfo other= new ExpressionInfo();
		super.collectExpressionInfo(other);
		if (other.hasDefaultVariableAccess()) {
			info.addVariableNameAccess(fVariable);
		}
		info.mergeExceptDefaultVariable(other);
	}

	@Override
	public boolean equals(final Object object) {
		if (!(object instanceof ResolveExpression))
			return false;

		final ResolveExpression that= (ResolveExpression)object;
		return this.fVariable.equals(that.fVariable)
				&& equals(this.fArgs, that.fArgs)
				&& equals(this.fExpressions, that.fExpressions);
	}

	@Override
	protected int computeHashCode() {
		return HASH_INITIAL * HASH_FACTOR + hashCode(fExpressions)
			* HASH_FACTOR + hashCode(fArgs)
			* HASH_FACTOR + fVariable.hashCode();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName());
		builder.append(" [variable=").append(fVariable); //$NON-NLS-1$
		if (fArgs != null) {
			builder.append(", args=").append(Arrays.toString(fArgs)); //$NON-NLS-1$
		}
		Expression[] children = getChildren();
		if (children.length > 0) {
			builder.append(", children="); //$NON-NLS-1$
			builder.append(Arrays.toString(children));
		}
		builder.append("]"); //$NON-NLS-1$
		return builder.toString();
	}
}
