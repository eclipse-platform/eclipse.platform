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

/**
 * Class defining the tag names of the XML elements of the common
 * expression language.
 *
 * @since 3.0
 */
public final class ExpressionTagNames {

	/** The tag name of the enablement expression (value: <code>enablement</code>) */
	public static final String ENABLEMENT= "enablement"; //$NON-NLS-1$

	/** The tag name of the and expression (value: <code>and</code>) */
	public static final String AND= "and"; //$NON-NLS-1$

	/** The tag name of the or expression (value: <code>or</code>) */
	public static final String OR= "or"; //$NON-NLS-1$

	/** The tag name of the not expression (value: <code>not</code>) */
	public static final String NOT= "not"; //$NON-NLS-1$

	/** The tag name of the instanceof expression (value: <code>instanceof</code>) */
	public static final String INSTANCEOF= "instanceof"; //$NON-NLS-1$

	/** The tag name of the test expression (value: <code>test</code>) */
	public static final String TEST= "test"; //$NON-NLS-1$

	/** The tag name of the with expression (value: <code>with</code>) */
	public static final String WITH= "with"; //$NON-NLS-1$

	/** The tag name of the adapt expression (value: <code>adapt</code>) */
	public static final String ADAPT= "adapt"; //$NON-NLS-1$

	/** The tag name of the count expression (value: <code>count</code>) */
	public static final String COUNT= "count"; //$NON-NLS-1$

	/** The tag name of the adapt expression (value: <code>iterate</code>) */
	public static final String ITERATE= "iterate"; //$NON-NLS-1$

	/** The tag name of the resolve expression (value: <code>resolve</code>) */
	public static final String RESOLVE= "resolve"; //$NON-NLS-1$

	/** The tag name of the systemTest expression (value: <code>systemTest</code>) */
	public static final String SYSTEM_TEST= "systemTest"; //$NON-NLS-1$

	/** The tag name of the equals expression (value: <code>equals</code>) */
	public static final String EQUALS= "equals"; //$NON-NLS-1$

	/**
	 * The tag name of the reference expression (value: <code>reference</code>)
	 *
	 * @since 3.3
	 */
	public static final String REFERENCE= "reference"; //$NON-NLS-1$
}
