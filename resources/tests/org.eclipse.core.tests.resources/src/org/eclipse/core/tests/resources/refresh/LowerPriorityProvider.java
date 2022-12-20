/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.resources.refresh;

import static org.junit.Assert.assertFalse;

import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.refresh.*;
import org.eclipse.core.runtime.IProgressMonitor;

public class LowerPriorityProvider extends RefreshProvider {

	static AtomicBoolean called = new AtomicBoolean();

	@Override
	public IRefreshMonitor installMonitor(IResource resource, IRefreshResult result, IProgressMonitor progressMonitor) {
		called.set(true);
		return super.installMonitor(resource, result, progressMonitor);
	}

	static void reset() {
		called.set(false);
	}

	static void check() {
		assertFalse("Low Priority provider was called!", called.get());
	}
}
