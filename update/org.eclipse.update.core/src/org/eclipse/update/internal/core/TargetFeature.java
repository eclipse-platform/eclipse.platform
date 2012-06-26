/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.update.core.Feature;
import org.eclipse.update.core.IFeatureContentConsumer;
import org.eclipse.update.core.Utilities;

/**
 * 
 */
public class TargetFeature extends Feature {


	/**
	 * The content consumer of the DefaultFeature
	 */
	private IFeatureContentConsumer contentConsumer;
	

	/**
	 * Constructor for TargetFeature.
	 */
	public TargetFeature() {
		super();
	}

	/**
	 * Sets the content Consumer
	 */
	public void setContentConsumer(IFeatureContentConsumer contentConsumer) {
		this.contentConsumer = contentConsumer;
		contentConsumer.setFeature(this);
	}

	/*
	 * @see IFeature#getFeatureContentConsumer()
	 */
	public IFeatureContentConsumer getFeatureContentConsumer() throws CoreException {
		if (this.contentConsumer == null) {
			throw Utilities.newCoreException( NLS.bind(Messages.Feature_NoFeatureContentConsumer, (new String[] { getURL().toExternalForm() })), null);
		}
		return contentConsumer;
	}

}
