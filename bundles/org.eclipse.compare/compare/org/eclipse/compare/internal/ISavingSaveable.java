/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
