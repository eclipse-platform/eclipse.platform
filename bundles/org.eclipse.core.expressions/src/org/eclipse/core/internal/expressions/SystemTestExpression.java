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

import org.w3c.dom.Element;

import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionInfo;
import org.eclipse.core.expressions.IEvaluationContext;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

public class SystemTestExpression extends Expression {

	private String fProperty;
	private String fExpectedValue;

	private static final String ATT_PROPERTY= "property"; //$NON-NLS-1$

	/**
	 * The seed for the hash code for all system test expressions.
	 */
	private static final int HASH_INITIAL= SystemTestExpression.class.getName().hashCode();

	public SystemTestExpression(IConfigurationElement element) throws CoreException {
		fProperty= element.getAttribute(ATT_PROPERTY);
		Expressions.checkAttribute(ATT_PROPERTY, fProperty);
		fExpectedValue= element.getAttribute(ATT_VALUE);
		Expressions.checkAttribute(ATT_VALUE, fExpectedValue);
	}

	public SystemTestExpression(Element element) throws CoreException {
		fProperty= element.getAttribute(ATT_PROPERTY);
		Expressions.checkAttribute(ATT_PROPERTY, fProperty.isEmpty() ? null : fProperty);
		fExpectedValue= element.getAttribute(ATT_VALUE);
		Expressions.checkAttribute(ATT_VALUE, fExpectedValue.isEmpty() ? null : fExpectedValue);
	}

	public SystemTestExpression(String property, String expectedValue) {
		Assert.isNotNull(property);
		Assert.isNotNull(expectedValue);
		fProperty= property;
		fExpectedValue= expectedValue;
	}

	@Override
	public EvaluationResult evaluate(IEvaluationContext context) throws CoreException {
		String str= System.getProperty(fProperty);
		if (str == null)
			return EvaluationResult.FALSE;
		return EvaluationResult.valueOf(str.equals(fExpectedValue));
	}

	@Override
	public void collectExpressionInfo(ExpressionInfo info) {
		info.markSystemPropertyAccessed();
	}

	@Override
	public boolean equals(final Object object) {
		if (!(object instanceof SystemTestExpression))
			return false;

		final SystemTestExpression that= (SystemTestExpression)object;
		return this.fProperty.equals(that.fProperty)
				&& this.fExpectedValue.equals(that.fExpectedValue);
	}

	@Override
	protected int computeHashCode() {
		return HASH_INITIAL * HASH_FACTOR + fExpectedValue.hashCode()
			* HASH_FACTOR + fProperty.hashCode();
	}

	// ---- Debugging ---------------------------------------------------

	@Override
	public String toString() {
		return "<systemTest property=\"" + fProperty +  //$NON-NLS-1$
		  "\" value=\"" + fExpectedValue + "\""; //$NON-NLS-1$ //$NON-NLS-2$
	}
}
