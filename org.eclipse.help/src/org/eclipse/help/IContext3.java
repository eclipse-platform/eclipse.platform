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
 * Extends <code>IContext2</code> to provide support for related
 * command links.
 *
 * @since 3.3
 */
public interface IContext3 extends IContext2 {

	/**
	 * Returns the command links related to this context-sensitive help
	 * entry.
	 *
	 * @return the related command links
	 */
	public ICommandLink[] getRelatedCommands();
}
