/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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

import org.w3c.dom.Element;

import org.eclipse.core.internal.expressions.DefinitionRegistry;
import org.eclipse.core.internal.expressions.Expressions;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

/**
 * This class makes use of the <b>org.eclipse.core.expressions.definitions</b>
 * extension point to evaluate the current context against pre-defined
 * expressions. It provides core expression re-use.
 *
 * @since 3.7
 */
public class ReferenceExpression extends Expression {

	// consider making this a more general extension manager
	// for now it's just part of the reference expression
	private static DefinitionRegistry fgDefinitionRegistry= null;

	private static DefinitionRegistry getDefinitionRegistry() {
		if (fgDefinitionRegistry == null) {
			fgDefinitionRegistry= new DefinitionRegistry();
		}
		return fgDefinitionRegistry;
	}

	private static final String ATT_DEFINITION_ID= "definitionId"; //$NON-NLS-1$

	/**
	 * The seed for the hash code for all equals expressions.
	 */
	private static final int HASH_INITIAL= ReferenceExpression.class.getName().hashCode();

	private String fDefinitionId;

	public ReferenceExpression(String definitionId) {
		Assert.isNotNull(definitionId);
		fDefinitionId= definitionId;
	}

	public ReferenceExpression(IConfigurationElement element) throws CoreException {
		fDefinitionId= element.getAttribute(ATT_DEFINITION_ID);
		Expressions.checkAttribute(ATT_DEFINITION_ID, fDefinitionId);
	}

	public ReferenceExpression(Element element) throws CoreException {
		fDefinitionId= element.getAttribute(ATT_DEFINITION_ID);
		Expressions.checkAttribute(ATT_DEFINITION_ID, fDefinitionId.isEmpty() ? null : fDefinitionId);
	}

	@Override
	public EvaluationResult evaluate(IEvaluationContext context) throws CoreException {
		Expression expr= getDefinitionRegistry().getExpression(fDefinitionId);
		return expr.evaluate(context);
	}

	@Override
	public void collectExpressionInfo(ExpressionInfo info) {
		Expression expr;
		try {
			expr= getDefinitionRegistry().getExpression(fDefinitionId);
		} catch (CoreException e) {
			// We didn't find the expression definition. So no
			// expression info can be collected.
			return;
		}
		expr.collectExpressionInfo(info);
	}

	@Override
	public boolean equals(final Object object) {
		if (!(object instanceof ReferenceExpression))
			return false;

		final ReferenceExpression that= (ReferenceExpression)object;
		return this.fDefinitionId.equals(that.fDefinitionId);
	}

	@Override
	protected int computeHashCode() {
		return HASH_INITIAL * HASH_FACTOR + fDefinitionId.hashCode();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName());
		builder.append(" [definitionId="); //$NON-NLS-1$
		builder.append(fDefinitionId);
		builder.append("]"); //$NON-NLS-1$
		return builder.toString();
	}
}