/*******************************************************************************
 * Copyright (c) 2006, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.core;

import org.eclipse.core.runtime.IProduct;
import org.eclipse.update.internal.configurator.FeatureEntry;
import org.osgi.framework.Bundle;

/**
 * Class used to wrapper feature entry objects. This class is necessary
 * to remove the depedancy of the org.eclipse.update.configurator on 
 * the org.eclipse.core.runtime bundle.
 *
 * @since 3.2
 */
public class FeatureEntryWrapper implements IProduct {

	private FeatureEntry entry;

	/*
	 * Constructor for the class.
	 */
	public FeatureEntryWrapper(FeatureEntry entry) {
		super();
		this.entry = entry;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IProduct#getApplication()
	 */
	public String getApplication() {
		return entry.getApplication();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IProduct#getName()
	 */
	public String getName() {
		return entry.getName();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IProduct#getDescription()
	 */
	public String getDescription() {
		return entry.getDescription();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IProduct#getId()
	 */
	public String getId() {
		return entry.getId();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IProduct#getProperty(java.lang.String)
	 */
	public String getProperty(String key) {
		return entry.getProperty(key);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IProduct#getDefiningBundle()
	 */
	public Bundle getDefiningBundle() {
		return entry.getDefiningBundle();
	}
}
