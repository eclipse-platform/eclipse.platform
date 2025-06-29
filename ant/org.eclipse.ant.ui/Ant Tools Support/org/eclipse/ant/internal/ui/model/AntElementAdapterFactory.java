/*******************************************************************************
 * Copyright (c) 2008, 2015 IBM Corporation and others.
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
package org.eclipse.ant.internal.ui.model;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;

/**
 * Adapter factory for Ant elements in editor outline
 */
public class AntElementAdapterFactory implements IAdapterFactory {

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adaptableObject instanceof AntElementNode node) {
			if (IResource.class.equals(adapterType)) {
				return (T) node.getIFile();
			}
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class[] { IResource.class };
	}

}
