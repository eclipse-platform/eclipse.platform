/*
 * Copyright (c) 2002 IBM Corp.  All rights reserved.
 * This file is made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 */
package org.eclipse.update.internal.ui.model;

import java.net.URL;

import org.eclipse.update.core.*;
import org.eclipse.core.runtime.CoreException;

/**
 * @version 	1.0
 * @author
 */
public interface IFeatureAdapter {
	public URL getURL();
	public ISite getSite();
	public IFeature getFeature() throws CoreException;
	public IFeatureAdapter [] getIncludedFeatures();
	public boolean hasIncludedFeatures();
	public boolean isIncluded();
	public boolean isOptional();
}
