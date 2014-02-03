/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ant.internal.ui;

import org.eclipse.ant.core.IAntPropertyValueProvider;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.JavaCore;

/**
 * Dynamic provider for Ant properties.
 * 
 * Provides the dynamic values for the following Ant properties:
 * 
 * <ul>
 * <li><code>eclipse.target</code> - set to the Eclipse target platform location</li>
 * </ul>
 * 
 * @since 3.1
 */
public class AntPropertyValueProvider implements IAntPropertyValueProvider {
	/**
	 * Returns the dynamic property values for Ant properties.
	 * 
	 * @param propertyName The name of the property to resolve the value for
	 * @return The resolved value for the property
	 * @see org.eclipse.ant.core.IAntPropertyValueProvider#getAntPropertyValue(String)
	 */
	public String getAntPropertyValue(String propertyName) {
		String value = null;
		if ("eclipse.target".equals(propertyName)) { //$NON-NLS-1$
			try {
			    IPath home= JavaCore.getClasspathVariable("ECLIPSE_HOME"); //$NON-NLS-1$
			    if (home != null) {
			    	value = home.toFile().getAbsolutePath();
			    	if (value.endsWith("/")) { //$NON-NLS-1$
			    		value = value.substring(0, value.length() - 1);
			    	}
			    }
			} catch (Exception e) {
				AntUIPlugin.log(e);
			}
		}
		return value;
	}
}
