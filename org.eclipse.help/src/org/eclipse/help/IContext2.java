/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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
package org.eclipse.help;

/**
 * Extends <code>IContext</code> to provide support for styled text and topic
 * categorization.
 *
 * @since 3.1
 */

public interface IContext2 extends IContext {
	/**
	 * Returns the optional title for this context. If the title is specified,
	 * it will be used for the presentation of this context. Otherwise, a
	 * default title will be used.
	 *
	 * @return the title to use for this context or <code>null</code> to use
	 *         the default title.
	 */
	public String getTitle();

	/**
	 * Returns the text description for this context with bold markers. The
	 * markers are used to allow the UI to distinguish between bold markup and
	 * bold tags that are intended to remain part of the original text.
	 *
	 * @return String with &lt;@#$b&gt;and &lt;/@#$b&gt; to mark bold range (as
	 *         IContext.getText() used to in 2.x)
	 */
	public String getStyledText();

	/**
	 * Returns the category of the provided topic. The category will be used in
	 * the UI to render all the topics that belong to the same category grouped
	 * together. The category string is expected to be NL-ready i.e. presentable
	 * in all NL locales.
	 *
	 * @param topic
	 *            the topic to be categorized
	 * @return the presentable name of the category that the topic belongs to,
	 *         or <code>null</code> if the topic belongs to the default
	 *         category.
	 */
	public String getCategory(IHelpResource topic);
}