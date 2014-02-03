/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.core;

import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.update.core.BaseFeatureFactory;
import org.eclipse.update.core.IFeature;
import org.eclipse.update.core.ISite;
import org.eclipse.update.core.model.FeatureModel;

public class LiteFeatureFactory extends BaseFeatureFactory {

	public LiteFeatureFactory() {
		super();
	}

	public IFeature createFeature(URL url, ISite site, IProgressMonitor monitor)
			throws CoreException {
		
		return null;
	}

	public FeatureModel createFeatureModel() {
		return new LiteFeature();
	}
}
