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
 *******************************************************************************/
package org.eclipse.ui.internal.console;

import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionConverter;
import org.eclipse.core.expressions.ExpressionTagNames;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.ui.IPluginContribution;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsolePageParticipant;

public class ConsolePageParticipantExtension implements IPluginContribution {

	private IConfigurationElement fConfig;
	private Expression fEnablementExpression;

	public ConsolePageParticipantExtension(IConfigurationElement config) {
		fConfig = config;
	}

	@Override
	public String getLocalId() {
		return fConfig.getAttribute("id"); //$NON-NLS-1$
	}

	@Override
	public String getPluginId() {
		return fConfig.getContributor().getName();
	}

	public boolean isEnabledFor(IConsole console) throws CoreException {
		EvaluationContext context = new EvaluationContext(null, console);
		Expression expression = getEnablementExpression();
		if (expression != null){
			EvaluationResult evaluationResult = expression.evaluate(context);
			return evaluationResult == EvaluationResult.TRUE;
		}
		return true;
	}

	public Expression getEnablementExpression() throws CoreException {
		if (fEnablementExpression == null) {
			IConfigurationElement[] elements = fConfig.getChildren(ExpressionTagNames.ENABLEMENT);
			IConfigurationElement enablement = elements.length > 0 ? elements[0] : null;

			if (enablement != null) {
				fEnablementExpression = ExpressionConverter.getDefault().perform(enablement);
			}
		}
		return fEnablementExpression;
	}

	public IConsolePageParticipant createDelegate() throws CoreException {
		return (IConsolePageParticipant) fConfig.createExecutableExtension("class"); //$NON-NLS-1$;
	}

}
