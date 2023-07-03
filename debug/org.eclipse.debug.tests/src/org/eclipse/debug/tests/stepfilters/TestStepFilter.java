/*******************************************************************************
 * Copyright (c) 2014 itemis AG (http://www.itemis.eu) and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.debug.tests.stepfilters;

import org.eclipse.debug.core.model.IStepFilter;

public class TestStepFilter implements IStepFilter {

	@Override
	public boolean isFiltered(Object object) {
		if (object instanceof Boolean) {
			return (Boolean) object;
		}
		return false;
	}

}
