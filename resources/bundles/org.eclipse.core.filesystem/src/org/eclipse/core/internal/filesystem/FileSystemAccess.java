/*******************************************************************************
 * Copyright (c) 2007, 2022 IBM Corporation and others.
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
 *     Christoph Läubrich - Issue #62 - regression from Bug 550548
 *******************************************************************************/
package org.eclipse.core.internal.filesystem;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.datalocation.Location;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

/**
 * This class abstracts away implementation details of the filesystem
 * bundle that depend on the OSGi framework being started. All
 * file system bundle functionality is enabled regardless of whether
 * OSGi is running.
 * @since org.eclipse.core.filesystem 1.1
 */
public class FileSystemAccess {

	/**
	 * Returns the local file system location that should be used for
	 * caching file data.
	 */
	public static IPath getCacheLocation() {
		//try to put the cache in the instance location if possible (3.2 behaviour)
		try {
			Bundle bundle = FrameworkUtil.getBundle(FileSystemAccess.class);
			if (bundle != null) {
				BundleContext context = bundle.getBundleContext();
				if (context != null) {
					ServiceTracker<Location, Location> tracker = new ServiceTracker<>(context, context.createFilter(Location.INSTANCE_FILTER), null);
					tracker.open();
					try {
						Location location = tracker.getService();
						if (location != null) {
							IPath instancePath = new Path(new File(location.getURL().getFile()).toString());
							return instancePath.append(".metadata/.plugins").append(Policy.PI_FILE_SYSTEM); //$NON-NLS-1$
						}
					} finally {
						tracker.close();
					}
				}
			}
		} catch (InvalidSyntaxException e) {
			Policy.log(IStatus.INFO, null, e);
			//fall through below and use user home
		}
		//just put the cache in the user home directory
		return Path.fromOSString(System.getProperty("user.home")); //$NON-NLS-1$
	}

	public static Enumeration<URL> findEntries(String path, String filePattern, boolean recurse) {
		Bundle bundle = FrameworkUtil.getBundle(FileSystemAccess.class);
		if (bundle != null) {
			return bundle.findEntries(path, filePattern, recurse);
		}
		return Collections.emptyEnumeration();
	}

}
