/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
 * An <code>IToc</code> represents the root node of a toc, for either a complete
 * toc (book) or a part of one to be assembled into a larger one.
 *
 * @since 2.0
 */
public interface IToc extends IUAElement, IHelpResource {
	/**
	 * This is element name used for TOC in XML files.
	 */
	public final static String TOC = "toc"; //$NON-NLS-1$
	/**
	 * This is the attribute used for description topic in XML files.
	 */
	public final static String TOPIC = "topic"; //$NON-NLS-1$

	/**
	 * Obtains the topics directly contained by a toc.
	 *
	 * @return Array of ITopic
	 */
	public ITopic[] getTopics();

	/**
	 * Returns a topic with the specified href defined by this TOC. <br>
	 * If the TOC contains multiple topics with the same href only of them
	 * (arbitrarily chosen) will be returned.
	 * <p>
	 * If no topic is specified, then the TOC description topic is returned, or
	 * null if there is no description topic for the TOC.
	 * </p>
	 *
	 * @param href the topic's URL or null
	 * @return ITopic or null
	 */
	public ITopic getTopic(String href);
}

