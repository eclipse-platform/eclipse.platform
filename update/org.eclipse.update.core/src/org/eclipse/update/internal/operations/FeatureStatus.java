/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.operations;

import org.eclipse.core.runtime.*;
import org.eclipse.update.core.*;


/**
 * The feature status provide info about the broken features and what is
 * wrong.
 */
public class FeatureStatus extends Status {
	public final static int CODE_OTHER = IStatus.OK;//0
	public final static int CODE_PREREQ_FEATURE = 1;
	public final static int CODE_PREREQ_PLUGIN = 2;
	public final static int CODE_EXCLUSIVE = 4;
	public final static int CODE_CYCLE = 8;
	public final static int CODE_OPTIONAL_CHILD = 16;
	public final static int CODE_ENVIRONMENT = 32;
	IFeature feature;

	public FeatureStatus(IFeature feature, int severity, String pluginId, int code, String message, Throwable exception) {
		super(severity, pluginId, code, message, exception);
		this.feature = feature;
	}
	public IFeature getFeature() {
		return feature;
	}
	public boolean equals(Object obj) {
		if (!(obj instanceof FeatureStatus))
			return false;
		FeatureStatus fs = (FeatureStatus) obj;
		// only check for feature, regardless of status type
		if (fs.getFeature() == feature)
			return true;
		else if (fs.getFeature() == null && feature == null)
			return fs.getMessage().equals(getMessage());
		else if (fs.getFeature() == null && feature != null)
			return false;
		else if (fs.getFeature() != null && feature == null)
			return false;
		else if (fs.getFeature().equals(feature))
			return true;
		else
			return false;
	}

}
