/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.team.core.mapping.IStorageMerger;

/**
 * A factory proxy for creating a StructureCreator.
 */
class StorageMergerDescriptor {

	private final static String CLASS_ATTRIBUTE= "class"; //$NON-NLS-1$

	private IConfigurationElement fElement;

	/*
	 * Creates a new sorter node with the given configuration element.
	 */
	public StorageMergerDescriptor(IConfigurationElement element) {
		fElement= element;
	}

	/*
	 * Creates a new stream merger from this node.
	 */
	public IStorageMerger createStreamMerger() {
		try {
			return (IStorageMerger)fElement.createExecutableExtension(CLASS_ATTRIBUTE);
		} catch (CoreException ex) {
			//ExceptionHandler.handle(ex, SearchMessages.getString("Search.Error.createSorter.title"), SearchMessages.getString("Search.Error.createSorter.message")); //$NON-NLS-2$ //$NON-NLS-1$
			return null;
		} catch (ClassCastException ex) {
			//ExceptionHandler.displayMessageDialog(ex, SearchMessages.getString("Search.Error.createSorter.title"), SearchMessages.getString("Search.Error.createSorter.message")); //$NON-NLS-2$ //$NON-NLS-1$
			return null;
		}
	}
}
