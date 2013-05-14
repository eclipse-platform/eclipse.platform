/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare;

/**
 * An <code>IContentChangeListener</code> is informed about content changes of a 
 * <code>IContentChangeNotifier</code>.
 * <p>
 * Clients may implement this interface.
 * </p>
 *
 * @see IContentChangeNotifier
 */
public interface IContentChangeListener {

	/**
	 * Called whenever the content of the given source has changed.
	 *
	 * @param source the source whose contents has changed
	 */
	void contentChanged(IContentChangeNotifier source);
}
