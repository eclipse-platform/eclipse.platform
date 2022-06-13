/*******************************************************************************
 * Copyright (c) 2010, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Simon Scholz <simon.scholz@vogella.com> - Bug 460405, 480098
 ******************************************************************************/
package org.eclipse.e4.core.internal.services;

import org.eclipse.core.runtime.Adapters;
import org.eclipse.e4.core.services.adapter.Adapter;

public class EclipseAdapter extends Adapter {

	@Override
	public <T> T adapt(Object element, Class<T> adapterType) {
		return Adapters.adapt(element, adapterType);
	}

}
