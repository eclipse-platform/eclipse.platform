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

import org.eclipse.update.core.model.SiteModel;

public class ExtendedSiteURLFactory extends SiteURLFactory {
	
	public SiteModel createSiteMapModel() {
		return new ExtendedSite();
	}

	
    //public SiteFeatureReferenceModel createFeatureReferenceModel() {
    //    return new UpdateSiteLiteFeatureReference();
    //}
}
