/*******************************************************************************
 * Copyright (c) 2004, 2019 IBM Corporation and others.
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
package org.eclipse.core.tests.harness;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Callable;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.wiring.FrameworkWiring;

public class BundleTestingHelper {

	public static Bundle[] getBundles(BundleContext context, String symbolicName, String version) {
		return Platform.getBundles(symbolicName, version);
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	public static Bundle installBundle(BundleContext context, String location) throws BundleException, MalformedURLException, IOException {
		return installBundle("", context, location);
	}

	public static Bundle installBundle(String tag, BundleContext context, String location) throws BundleException, MalformedURLException, IOException {
		URL entry = context.getBundle().getEntry(location);
		assertNotNull(entry,
				tag + " entry " + location + " could not be found in " + context.getBundle().getSymbolicName());
		return context.installBundle(FileLocator.toFileURL(entry).toExternalForm());
	}

	/**
	 * Do FrameworkWiring.refreshPackages() in a synchronous way. After installing
	 * all the requested bundles we need to do a refresh and want to ensure that
	 * everything is done before returning.
	 *
	 * @param bundles TODO remove this since all we wanted was to resolve bundles,
	 *                what is done by #resolveBundles in this class
	 */
	//copied from EclipseStarter
	public static void refreshPackages(BundleContext context, Bundle[] bundles) {
		if (bundles.length == 0) {
			return;
		}
		FrameworkWiring wiring = context.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).adapt(FrameworkWiring.class);
		// TODO this is such a hack it is silly.  There are still cases for race conditions etc
		// but this should allow for some progress...
		// (patch from John A.)
		final boolean[] flag = new boolean[] {false};
		FrameworkListener listener = event -> {
			if (event.getType() == FrameworkEvent.PACKAGES_REFRESHED) {
				synchronized (flag) {
					flag[0] = true;
					flag.notifyAll();
				}
			}
		};
		wiring.refreshBundles(asList(bundles), listener);
		synchronized (flag) {
			while (!flag[0]) {
				try {
					flag.wait();
				} catch (InterruptedException e) {
					// who cares....
				}
			}
		}
	}

	public static boolean  resolveBundles(BundleContext context, Bundle[] bundles) {
		FrameworkWiring wiring = context.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).adapt(FrameworkWiring.class);
		return wiring.resolveBundles(asList(bundles));
	}

	public static void runWithBundles(Callable<Void> runnable, BundleContext context, String[] locations,
			TestRegistryChangeListener listener) throws Exception {
		if (listener != null) {
			listener.register();
		}
		try {
			Bundle[] installed = new Bundle[locations.length];
			for (int i = 0; i < locations.length; i++) {
				installed[i] = installBundle(context, locations[i]);
				assertEquals(Bundle.INSTALLED, installed[i].getState(), locations[i]);
			}
			if (listener != null) {
				listener.reset();
			}
			assertTrue(BundleTestingHelper.resolveBundles(context, installed));
			if (listener != null) {
				// ensure the contributions were properly added
				assertTrue(listener.eventReceived(installed.length * 10000));
			}
			try {
				runnable.call();
			} finally {
				if (listener != null) {
					listener.reset();
				}
				// remove installed bundles
				for (Bundle element : installed) {
					element.uninstall();
				}
				BundleTestingHelper.resolveBundles(context, installed);
				if (listener != null) {
					// ensure the contributions were properly added
					assertTrue(listener.eventReceived(installed.length * 10000));
				}
			}
		} finally {
			if (listener != null) {
				listener.unregister();
			}
		}
	}

}
