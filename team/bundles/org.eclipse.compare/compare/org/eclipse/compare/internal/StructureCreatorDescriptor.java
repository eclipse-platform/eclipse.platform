/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others.
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
package org.eclipse.compare.internal;

import org.eclipse.compare.structuremergeviewer.IStructureCreator;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

/**
 * A factory proxy for creating a StructureCreator.
 */
public class StructureCreatorDescriptor {

	private final static String CLASS_ATTRIBUTE= "class"; //$NON-NLS-1$
	private final static String EXTENSIONS_ATTRIBUTE= "extensions"; //$NON-NLS-1$

	private final IConfigurationElement fElement;

	/*
	 * Creates a new sorter node with the given configuration element.
	 */
	public StructureCreatorDescriptor(IConfigurationElement element) {
		fElement= element;
	}

	/*
	 * Creates a new sorter from this node.
	 */
	public IStructureCreator createStructureCreator() {
		try {
			return (IStructureCreator)fElement.createExecutableExtension(CLASS_ATTRIBUTE);
		} catch (CoreException ex) {
			CompareUIPlugin.log(ex.getStatus());
			//ExceptionHandler.handle(ex, SearchMessages.getString("Search.Error.createSorter.title"), SearchMessages.getString("Search.Error.createSorter.message")); //$NON-NLS-2$ //$NON-NLS-1$
			return null;
		} catch (ClassCastException ex) {
			//ExceptionHandler.displayMessageDialog(ex, SearchMessages.getString("Search.Error.createSorter.title"), SearchMessages.getString("Search.Error.createSorter.message")); //$NON-NLS-2$ //$NON-NLS-1$
			return null;
		}
	}

	/*
	 * Returns the structure creator's extensions.
	 */
	public String getExtension() {
		return fElement.getAttribute(EXTENSIONS_ATTRIBUTE);
	}
}
