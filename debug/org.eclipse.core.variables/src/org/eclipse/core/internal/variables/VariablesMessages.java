/*******************************************************************************
 * Copyright (c) 2000, 2025 IBM Corporation and others.
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
package org.eclipse.core.internal.variables;

import org.eclipse.osgi.util.NLS;

public class VariablesMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.core.internal.variables.VariablesMessages";//$NON-NLS-1$

	public static String StringSubstitutionEngine_3;
	public static String StringSubstitutionEngine_4;

	public static String StringVariableManager_26;
	public static String StringVariableManager_27;

	public static String DynamicVariable_0;

	public static String VarMissingResolver;

	public static String VarResolverNotIContextVariableResolver;

	public static String VarEvalError;

	public static String VarInitFailDueNonIValueVariable;

	public static String VarInitFail;

	public static String StringVarExceptionOnChange;

	public static String StringVarMissingNameExt;

	public static String StringVarExtOverridesOnBundles;

	public static String StringVarContExtOverridesOnBundles;

	public static String StringVarExceptionOnLoad;

	public static String StringVarInvalidFormat;

	public static String StringVarInvalidXML;

	public static String StringVarNameNull;

	public static String StringVarLaunchExcep;

	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, VariablesMessages.class);
	}
}