/*******************************************************************************
 * Copyright (c) 2005, 2017 IBM Corporation and others.
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
package org.eclipse.core.tests.net;

import org.eclipse.core.net.proxy.IProxyService;
import org.osgi.framework.*;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator {

	public static IProxyService getProxyService() {
		Bundle bundle = FrameworkUtil.getBundle(Activator.class);
		ServiceReference<IProxyService> serviceReference = bundle.getBundleContext().getServiceReference(IProxyService.class);
		if (serviceReference != null)
			return bundle.getBundleContext().getService(serviceReference);
		return null;
	}

}
