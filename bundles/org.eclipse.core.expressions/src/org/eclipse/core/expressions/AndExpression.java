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
 * @since 3.7
 */

public class AndExpression extends CompositeExpression {

	@Override
	public boolean equals(final Object object) {
		if (!(object instanceof AndExpression))
			return false;

		final AndExpression that= (AndExpression)object;
		return equals(this.fExpressions, that.fExpressions);
	}

	@Override
	public EvaluationResult evaluate(IEvaluationContext context) throws CoreException {
		return evaluateAnd(context);
	}
}
