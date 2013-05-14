/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal;

import org.eclipse.ui.Saveable;

/**
 * Interface defines API for checking if an object, preferably an instance of
 * {@link Saveable}, is being saved.
 * 
 * @since 3.7
 */
public interface ISavingSaveable {
	public boolean isSaving();
}
