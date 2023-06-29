/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.debug.internal.core;


import java.util.Comparator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

import org.eclipse.debug.core.DebugPlugin;

/**
 * Proxy to a runtime classpath entry resolver extension.
 *
 * @see IConfigurationElementConstants
 */
public class LaunchConfigurationComparator implements Comparator<Object> {

	private IConfigurationElement fConfigurationElement;

	private Comparator<Object> fDelegate;

	/**
	 * Constructs a new resolver on the given configuration element
	 *
	 * @param element configuration element
	 */
	public LaunchConfigurationComparator(IConfigurationElement element) {
		fConfigurationElement = element;
	}

	/**
	 * Returns the {@link Comparator} delegate
	 *
	 * @return the {@link Comparator}
	 */
	protected Comparator<Object> getComparator() {
		if (fDelegate == null) {
			try {
				@SuppressWarnings("unchecked")
				Comparator<Object> delegate = (Comparator<Object>) fConfigurationElement.createExecutableExtension(IConfigurationElementConstants.CLASS);
				fDelegate = delegate;
			} catch (CoreException e) {
				DebugPlugin.log(e);
			}
		}
		return fDelegate;
	}


	/**
	 * @see Comparator#compare(Object, Object)
	 */
	@Override
	public int compare(Object o1, Object o2) {
		return getComparator().compare(o1, o2);
	}

	/**
	 * @see Object#equals(Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return getComparator().equals(obj);
	}

	@Override
	public int hashCode() {
		return getComparator().hashCode();
	}
}
