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
			return (IStreamMerger) fElement.createExecutableExtension(CLASS_ATTRIBUTE);
		} catch (CoreException | ClassCastException ex) {
			return null;
		}
	}
}
