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

public class OrExpression extends CompositeExpression {

	@Override
	public EvaluationResult evaluate(IEvaluationContext context) throws CoreException {
		return evaluateOr(context);
	}

	@Override
	public boolean equals(final Object object) {
		if (!(object instanceof OrExpression))
			return false;

		final OrExpression that= (OrExpression)object;
		return equals(this.fExpressions, that.fExpressions);
	}
}
