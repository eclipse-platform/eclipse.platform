/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
 *     Sergey Prigogin (Google) - Bug 421375
 *******************************************************************************/
package org.eclipse.core.internal.expressions;

import java.util.Arrays;

import org.w3c.dom.Element;

import org.eclipse.core.expressions.CompositeExpression;
import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionInfo;
import org.eclipse.core.expressions.IEvaluationContext;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

public class AdaptExpression extends CompositeExpression {

	private static final String PREFERENCE_NAMESPACE = "org.eclipse.core.expressions"; //$NON-NLS-1$

	private static final String ATT_TYPE= "type"; //$NON-NLS-1$

	/**
	 * The seed for the hash code for all adapt expressions.
	 */
	private static final int HASH_INITIAL= AdaptExpression.class.getName().hashCode();

	private String fTypeName;

	public AdaptExpression(IConfigurationElement configElement) throws CoreException {
		fTypeName= configElement.getAttribute(ATT_TYPE);
		Expressions.checkAttribute(ATT_TYPE, fTypeName);
	}

	public AdaptExpression(Element element) throws CoreException {
		fTypeName= element.getAttribute(ATT_TYPE);
		Expressions.checkAttribute(ATT_TYPE, fTypeName.isEmpty() ? null : fTypeName);
	}

	public AdaptExpression(String typeName) {
		Assert.isNotNull(typeName);
		fTypeName= typeName;
	}

	@Override
	public boolean equals(final Object object) {
		if (!(object instanceof AdaptExpression))
			return false;

		final AdaptExpression that= (AdaptExpression)object;
		return this.fTypeName.equals(that.fTypeName)
				&& equals(this.fExpressions, that.fExpressions);
	}

	@Override
	protected int computeHashCode() {
		return HASH_INITIAL * HASH_FACTOR + hashCode(fExpressions)
			* HASH_FACTOR + fTypeName.hashCode();
	}

	@Override
	public EvaluationResult evaluate(IEvaluationContext context) throws CoreException {
		if (fTypeName == null)
			return EvaluationResult.FALSE;
		Object var = context.getDefaultVariable();
		if (var == null) {
			return EvaluationResult.FALSE;
		}
		Object adapted = null;
		IAdapterManager manager = Platform.getAdapterManager();
		if (Expressions.isInstanceOf(var, fTypeName)) {
			adapted = var;
		} else {
			// if the adapter manager doesn't have an adapter contributed,
			// try to see if the variable itself implements IAdaptable
			if (var instanceof IAdaptable) {
				Class<?> typeClazz = Expressions.loadClass(var.getClass().getClassLoader(), fTypeName);
				if (typeClazz != null) {
					adapted = ((IAdaptable) var).getAdapter(typeClazz);
				}
			}

			if (adapted == null) {
				if (forceLoadEnabled()) {
					adapted = manager.loadAdapter(var, fTypeName);
				} else {
					adapted = manager.getAdapter(var, fTypeName);
					if (adapted == null) {
						if (manager.queryAdapter(var, fTypeName) == IAdapterManager.NOT_LOADED) {
							return EvaluationResult.NOT_LOADED;
						} else {
							return EvaluationResult.FALSE;
						}
					}
				}
				if (adapted == null) {
					// all attempts failed, return false
					return EvaluationResult.FALSE;
				}
			}
		}
		// the adapted result is null but hasAdapter returned true check
		// if the adapter is loaded.
		return evaluateAnd(new DefaultVariable(context, adapted));
	}

	private boolean forceLoadEnabled() {
		return Platform.getPreferencesService().getBoolean(PREFERENCE_NAMESPACE, "forceLoadAdapters", true, //$NON-NLS-1$
				null);
	}

	@Override
	public void collectExpressionInfo(ExpressionInfo info) {
		// Although the default variable is passed to the children of this
		// expression as an instance of the adapted type it is OK to only
		// mark a default variable access.
		info.markDefaultVariableAccessed();
		super.collectExpressionInfo(info);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName());
		builder.append(" [type=").append(fTypeName); //$NON-NLS-1$
		Expression[] children = getChildren();
		if (children.length > 0) {
			builder.append(", children="); //$NON-NLS-1$
			builder.append(Arrays.toString(children));
		}
		builder.append("]"); //$NON-NLS-1$
		return builder.toString();
	}

}
