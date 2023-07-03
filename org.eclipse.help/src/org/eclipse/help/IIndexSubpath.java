/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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
 * A directive indicating the content at the given path should be included in
 * this document, and replace this node.
 *
 * @since 3.5
 */
public interface IIndexSubpath extends IUAElement {

	/**
	 * @return A segment of the keyword path of a seeAlso element
	 */
	public String getKeyword();
}
