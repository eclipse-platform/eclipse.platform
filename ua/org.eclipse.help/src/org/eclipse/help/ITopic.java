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
 * An <code>ITopic</code> is one topic in the table of contents, which may
 * contain subtopics.
 *
 * @since 2.0
 */
public interface ITopic extends IUAElement, IHelpResource {
	/**
	 * This is element name used for topic in XML files.
	 */
	public final static String TOPIC = "topic"; //$NON-NLS-1$

	/**
	 * Obtains the topics contained in this node.
	 *
	 * @return Array of ITopic
	 */
	public ITopic[] getSubtopics();
}

