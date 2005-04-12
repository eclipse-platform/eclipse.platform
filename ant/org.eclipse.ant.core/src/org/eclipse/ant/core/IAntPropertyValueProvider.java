/*******************************************************************************
 * Copyright (c) 2003, 2005 BBDO Detroit and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Thierry Lach (thierry.lach@bbdodetroit.com) - initial API and implementation for bug 40502
 *******************************************************************************/
package org.eclipse.ant.core;

/**
 * An interface that must be implemented by plugins that wish to contribute
 * predefined variables to an Ant project when run from within Eclipse.
 * Clients may implement this interface.
 * @since 3.0
 */
public interface IAntPropertyValueProvider {

	/**
	 * Returns a value that the Ant entry point will use to set the
	 * value of the Ant property.
	 *
	 * @param antPropertyName the Ant property to set
	 * @return the value for the property, 
	 *    or <code>null</code> if the property should not be set
	 */
	public String getAntPropertyValue(String antPropertyName);   
}