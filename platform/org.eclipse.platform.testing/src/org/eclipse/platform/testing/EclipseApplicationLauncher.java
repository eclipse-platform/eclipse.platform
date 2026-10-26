/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
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
package org.eclipse.platform.testing;

import org.eclipse.osgi.service.runnable.ApplicationLauncher;
import org.eclipse.osgi.service.runnable.ParameterizedRunnable;

class EclipseApplicationLauncher implements ApplicationLauncher {

	@Override
	public void launch(ParameterizedRunnable runnable, Object context) {

	}

	@Override
	public void shutdown() {

	}

}
