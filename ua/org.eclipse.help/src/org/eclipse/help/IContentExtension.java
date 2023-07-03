/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
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
 * An extension of a document's content.
 *
 * @since 3.3
 */
public interface IContentExtension extends IUAElement {

	/**
	 * Extension type for a contribution at an anchor.
	 */
	public static final int CONTRIBUTION = 0;

	/**
	 * Extension type for element replacement.
	 */
	public static final int REPLACEMENT = 1;

	/**
	 * Returns the extension's content path (what to contribute into the
	 * document). This is a bundle-relative path with an id, of the form
	 * "path/file.ext#elementId".
	 *
	 * @return path to the extension's content
	 */
	public String getContent();

	/**
	 * Returns the extensions target path (what to extend).
	 *
	 * @return path to the target element to extend
	 */
	public String getPath();

	/**
	 * Returns the type of extension this is. Must be one of the static
	 * constants defined by this interface.
	 *
	 * @return the extension type
	 */
	public int getType();
}
