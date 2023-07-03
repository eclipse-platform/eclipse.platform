/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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
 *     Martin Oberhuber (Wind River) Bug 354428
 *******************************************************************************/
package org.eclipse.help.internal.base;

import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.runtime.Platform;

/*
 * Supplies en evaluation context for filtering help documents. This is used for
 * resolving variables in enablement expressions.
 */
public final class HelpEvaluationContext {

	private static final String VARIABLE_PLATFORM = "platform"; //$NON-NLS-1$

	private static EvaluationContext context;

	/*
	 * Returns the evaluation context to use in help documents.
	 */
	public static EvaluationContext getContext() {
		if (context == null) {
			context = new EvaluationContext(null, Platform.class);
			context.addVariable(VARIABLE_PLATFORM, Platform.class);
			context.setAllowPluginActivation(true);
		}
		return context;
	}

	/*
	 * Sets the evaluation context to use in help documents. If help is running in
	 * workbench mode, the UI plug-in will contribute a context that also handles
	 * UI-related variables.
	 */
	public static void setContext(EvaluationContext context) {
		HelpEvaluationContext.context = context;
	}

	/*
	 * Not meant to be instantiated.
	 */
	private HelpEvaluationContext() {
	}
}
