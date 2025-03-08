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
package org.eclipse.debug.terminal;

import org.eclipse.debug.core.ExecFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	private boolean cdt = true;

	@Override
	public void start(BundleContext bundleContext) {
		try {
			System.out.println("Activate terminal support...");
			if (cdt) {
				System.out.println(" ... with cdt!");
				ExecFactory.setDefault(new CDTFactory());
			} else {
				System.out.println(" ... with pty4j!");
				ExecFactory.setDefault(new Pty4jExecFactory());
			}
		} catch (Throwable t) {
			System.err.println("Can't activate terminal support");
		}
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
	}

}
