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

package org.eclipse.ant.internal.ui.model;


public class AntModelChangeEvent {
	
	private IAntModel fModel;
	private boolean fPreferenceChange= false;
	
	public AntModelChangeEvent(IAntModel model) {
		fModel= model;
	}
	
	public AntModelChangeEvent(IAntModel model, boolean preferenceChange) {
		fModel= model;
		fPreferenceChange= preferenceChange;
	}
	
	public IAntModel getModel() {
		return fModel;
	}
	
	/**
	 * Returns whether the Ant model has changed as a result of a preference change.
	 * @return whether the model has changed from a preference change.
	 */
	public boolean isPreferenceChange() {
		return fPreferenceChange;
	}
}
