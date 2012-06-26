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
package org.eclipse.update.internal.core;
import org.eclipse.core.runtime.*;
import org.eclipse.update.core.*;

/**
 * ContentConsumer for a non plugin entry of a feature
 */

public class NonPluginEntryContentConsumer extends ContentConsumer {

	private boolean closed = false;

	private IContentConsumer contentConsumer;
		
	/*
	 * Constructor
	 */
	public NonPluginEntryContentConsumer(IContentConsumer contentConsumer){
		this.contentConsumer = contentConsumer;
	}

	/*
	 * @see ContentConsumer#store(ContentReference, IProgressMonitor)
	 */
	public void store(ContentReference contentReference, IProgressMonitor monitor) throws CoreException {
		if (!closed){
			contentConsumer.store( contentReference,monitor);
		} else {
			UpdateCore.warn("Attempt to store in a closed NonPluginEntryContentConsumer",new Exception()); //$NON-NLS-1$
		}
	}

	/*
	 * @see ContentConsumer#close()
	 */
	public void close() throws CoreException  {
		if (!closed){
			closed = true;
			contentConsumer.close();
		} else {
			UpdateCore.warn("Attempt to close a closed NonPluginEntryContentConsumer",new Exception()); //$NON-NLS-1$
		}
	}

}
