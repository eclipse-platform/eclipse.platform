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
 *******************************************************************************/
package org.eclipse.e4.core.internal.contexts;

import java.util.Set;
import org.eclipse.e4.core.internal.contexts.EclipseContext.Scheduled;

abstract public class Computation {

	abstract protected int calcHashCode();

	@Override
	abstract public boolean equals(Object obj);

	/* final */protected int hashCode;

	abstract public void handleInvalid(ContextChangeEvent event, Set<Scheduled> scheduled);

	@Override
	public int hashCode() {
		return hashCode;
	}

	protected void init() {
		hashCode = calcHashCode();
	}

	public boolean isValid() {
		return true;
	}

}
