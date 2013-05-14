/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal;

import org.eclipse.compare.IStreamMerger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

/**
 * A factory proxy for creating a StructureCreator.
 */
class StreamMergerDescriptor {
    
	private final static String CLASS_ATTRIBUTE= "class"; //$NON-NLS-1$
    
	private IConfigurationElement fElement;
	
	/*
	 * Creates a new sorter node with the given configuration element.
	 */
	public StreamMergerDescriptor(IConfigurationElement element) {
		fElement= element;
	}

	/*
	 * Creates a new stream merger from this node.
	 */
	public IStreamMerger createStreamMerger() {
		try {
			return (IStreamMerger)fElement.createExecutableExtension(CLASS_ATTRIBUTE);
		} catch (CoreException ex) {
			//ExceptionHandler.handle(ex, SearchMessages.getString("Search.Error.createSorter.title"), SearchMessages.getString("Search.Error.createSorter.message")); //$NON-NLS-2$ //$NON-NLS-1$
			return null;
		} catch (ClassCastException ex) {
			//ExceptionHandler.displayMessageDialog(ex, SearchMessages.getString("Search.Error.createSorter.title"), SearchMessages.getString("Search.Error.createSorter.message")); //$NON-NLS-2$ //$NON-NLS-1$
			return null;
		}
	}
}
