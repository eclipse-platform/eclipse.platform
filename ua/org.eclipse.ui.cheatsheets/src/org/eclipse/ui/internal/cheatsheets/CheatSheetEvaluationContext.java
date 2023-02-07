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
 *******************************************************************************/
package org.eclipse.ui.internal.cheatsheets;

import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.PlatformUI;

/*
 * Supplies en evaluation context for filtering cheat sheets. This is used for
 * resolving variables in enablement expressions.
 */
public final class CheatSheetEvaluationContext {

	private static final String VARIABLE_PLATFORM = "platform"; //$NON-NLS-1$
	private static final String VARIABLE_WORKBENCH = "workbench"; //$NON-NLS-1$

	private static EvaluationContext context;

	/*
	 * Returns the evaluation context to use in cheat sheets.
	 */
	public static EvaluationContext getContext() {
		if (context == null) {
			context = new EvaluationContext(null, Platform.class);
			context.addVariable(VARIABLE_PLATFORM, Platform.class);
			context.addVariable(VARIABLE_WORKBENCH, PlatformUI.getWorkbench());
		}
		return context;
	}

	/*
	 * Not meant to be instantiated.
	 */
	private CheatSheetEvaluationContext() {
	}
}
