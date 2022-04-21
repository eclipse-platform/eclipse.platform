/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.runtime;

import org.eclipse.core.runtime.ILog;
import org.osgi.framework.*;

/**
 *
 */
public class LogServiceFactory implements ServiceFactory<ILog> {

	@Override
	public ILog getService(Bundle bundle, ServiceRegistration<ILog> registration) {
		return InternalPlatform.getDefault().getLog(bundle);
	}

	@Override
	public void ungetService(Bundle bundle, ServiceRegistration<ILog> registration, ILog service) {
		// nothing to do here
	}

}
