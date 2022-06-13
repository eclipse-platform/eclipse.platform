/*******************************************************************************
 * Copyright (c) 2008, 2015 Wind River Systems and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.expressions.tests;

import org.eclipse.core.runtime.IAdaptable;

/**
 *
 */
public class AdaptableAdaptee implements IAdaptable {

	private Adapter fAdapter = new Adapter();

	@Override
	public <T> T getAdapter(Class<T> adapterType) {
		if (adapterType.isInstance(fAdapter)) {
			return adapterType.cast(fAdapter);
		}
		return null;
	}

}
