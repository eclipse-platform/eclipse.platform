/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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
 ******************************************************************************/

package org.eclipse.e4.core.internal.tests.contexts;

import org.eclipse.e4.core.contexts.ContextFunction;
import org.eclipse.e4.core.contexts.IEclipseContext;


/**
 * A function provided as a declarative service. See OSGI-INF/adder.xml.
 */
public class AddContextFunction extends ContextFunction {

	@Override
	public Object compute(IEclipseContext context, String contextKey) {
		Integer xInt = (Integer) context.get("x");
		Integer yInt = (Integer) context.get("y");
		int sum = xInt == null ? 0 : xInt.intValue();
		sum += yInt == null ? 0 : yInt.intValue();
		return Integer.valueOf(sum);
	}
}
