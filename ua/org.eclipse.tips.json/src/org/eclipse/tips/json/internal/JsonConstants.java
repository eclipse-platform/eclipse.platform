/*******************************************************************************
 * Copyright (c) 2018 Remain Software
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     wim.jongman@remainsoftware.com - initial API and implementation
 *******************************************************************************/
package org.eclipse.tips.json.internal;

/**
 * Parser constants for the Json TipProvider.Tip[] Structure
 *
 * <pre>
 * {
 *   "provider": {
 *     "image": "data:image/png;base64,iVBORw0K ... y543==",
 *     "description": "TipProvider Description",
 *     "expression": "&lt;eclipse core expression&gt;",
 *     "tips": [
 *       {
 *         "subject": "Tip Subject",
 *         "date": "YYYY-MM-DD",
 *         "image": "data:image/png;base64,iVBORw0KGgo ... CYII=",
 *         "ratio": 1.5,
 *         "maxHeight": 300,
 *         "maxWidth": 200,
 *         "url": "url or html, not both",
 *         "html": "html or url, not both"
 *     }]
 *   }
 * }
 * </pre>
 */
public class JsonConstants {

	/**
	 * Provider description field
	 */
	public static final String P_DESCRIPTION = "description"; //$NON-NLS-1$

	/**
	 * Provider core expression field
	 */
	public static final String P_EXPRESSION = "expression"; //$NON-NLS-1$

	/**
	 * Provider image field
	 */
	public static final String P_IMAGE = "image"; //$NON-NLS-1$

	/**
	 * Provider provider field
	 */
	public static final String P_PROVIDER = "provider"; //$NON-NLS-1$

	/**
	 * The provider tips array
	 */
	public static final String P_TIPS = "tips"; //$NON-NLS-1$

	/**
	 * Tip date field
	 */
	public static final String T_DATE = "date"; //$NON-NLS-1$

	/**
	 * Tip HTML
	 */
	public static final String T_HTML = "html"; //$NON-NLS-1$

	/**
	 * Tip image field
	 */
	public static final String T_IMAGE = "image"; //$NON-NLS-1$

	/**
	 * Tip image maximum height (int)
	 */
	public static final String T_MAXHEIGHT = "maxHeight"; //$NON-NLS-1$

	/**
	 * Tip image maximum width (int)
	 */
	public static final String T_MAXWIDTH = "maxWidth"; //$NON-NLS-1$

	/**
	 * Tip image ratio field (double)
	 */
	public static final String T_RATIO = "ratio"; //$NON-NLS-1$

	/**
	 * Tip subject field
	 */
	public static final String T_SUBJECT = "subject"; //$NON-NLS-1$

	/**
	 * Tip URL
	 */
	public static final String T_URL = "url"; //$NON-NLS-1$

	/**
	 * Tip variables
	 */
	public static final String T_VARIABLES = "variables"; //$NON-NLS-1$
}