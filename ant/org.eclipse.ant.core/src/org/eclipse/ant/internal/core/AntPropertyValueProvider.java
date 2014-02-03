/*******************************************************************************
 * Copyright (c) 2003, 2012 BBDO Detroit and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Thierry Lach (thierry.lach@bbdodetroit.com) - initial API and implementation for bug 40502
 *     IBM Corporation - added eclipse.running property, bug 65655
 *******************************************************************************/
package org.eclipse.ant.internal.core;

import java.io.File;
import java.net.URL;

import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.ant.core.IAntPropertyValueProvider;
import org.eclipse.core.runtime.FileLocator;

/**
 * Dynamic provider for Ant properties.
 * 
 * Provides the dynamic values for the following Ant properties:
 * 
 * <ul>
 * <li><code>eclipse.home</code> - set to the Eclipse installation directory</li>
 * </ul>
 * * <ul>
 * <li><code>eclipse.running</code> - set (to "true") when Eclipse is running</li>
 * </ul>
 * 
 * @since 3.0
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
		if ("eclipse.running".equals(propertyName)){ //$NON-NLS-1$
			return "true"; //$NON-NLS-1$
		} else if ("eclipse.home".equals(propertyName)) { //$NON-NLS-1$
			try {
				value = new File(FileLocator.resolve(new URL("platform:/base/")).getPath()).getAbsolutePath(); //$NON-NLS-1$
				if (value.endsWith("/")) { //$NON-NLS-1$
				    value = value.substring(0, value.length() - 1);
				}
			} catch (Exception e) {
				AntCorePlugin.log(e);
			}
		}
		return value;
	}
}