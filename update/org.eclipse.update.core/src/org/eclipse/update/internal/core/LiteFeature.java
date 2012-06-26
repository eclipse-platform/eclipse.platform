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

import org.eclipse.update.core.Feature;

public class LiteFeature extends Feature {
	
	private boolean fullFeature = true;

	public boolean isFullFeature() {
		return fullFeature;
	}

	public void setFullFeature(boolean fullFeature) {
		this.fullFeature = fullFeature;
	}

}
