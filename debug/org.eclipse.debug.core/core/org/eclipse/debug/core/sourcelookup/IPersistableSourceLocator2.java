/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
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

package org.eclipse.debug.core.sourcelookup;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IPersistableSourceLocator;

/**
 * Extensions to the <code>IPersistableSourceLocator</code> interface.
 * <p>
 * Clients may optionally implement this interface when implementing an
 * {@link org.eclipse.debug.core.model.IPersistableSourceLocator}.
 * </p>
 * @see org.eclipse.debug.core.model.IPersistableSourceLocator
 * @since 3.0
 */
public interface IPersistableSourceLocator2 extends IPersistableSourceLocator {

	/**
	 * Initializes this source locator based on the given
	 * memento, for the given launch configuration. This method
	 * is called instead of <code>initializeFrom(String memento)</code>
	 * defined in <code>IPersistableSourceLocator</code> when a source
	 * locator implements this interface.
	 *
	 * @param memento a memento to initialize this source locator
	 * @param configuration the launch configuration this source locator is
	 *  being created for
	 * @exception CoreException on failure to initialize
	 */
	void initializeFromMemento(String memento, ILaunchConfiguration configuration) throws CoreException;

	/**
	 * Disposes this source locator. This method is called when a source
	 * locator's associated launch is removed from the launch manager.
	 */
	void dispose();
}
