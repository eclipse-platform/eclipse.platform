/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.core.internal.watson;

/**
 * User data that can be attached to the element tree itself.
 */
public interface IElementTreeData extends Cloneable {
	/**
	 * ElementTreeData must define a publicly accessible clone method.
	 * This method can simply invoke Object's clone method.
	 */
	Object clone();
}
