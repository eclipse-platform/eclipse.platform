/*******************************************************************************
 * Copyright (c) 2023 Eclipse Platform, Security Group and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Eclipse Platform - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.pki;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

//import org.eclipse.ui.pki.AuthenticationBase;


public enum SecurePKIVersionInfo {
	INSTANCE;

	@SuppressWarnings("resource")
	public static String getVersion() {
		String version = null;
		Path path = null;
		//System.out.println("[GeT Version of this Build]");

		try {

			/*
			 * See if this is a valid path check to see if its eclipse testing
			 */
			path = Paths.get("PKI.jar"); //$NON-NLS-1$

			if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
				//System.out.println(" GOOD PATH");
			} else {

				try {
					/*
					 * path =
					 * Paths.get(AuthenticationBase.class.getProtectionDomain().getCodeSource().
					 * getLocation().toURI()); if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
					 * DebugLogger.printDebug("PKIVersionInfo -- PATH:"+path.toAbsolutePath()); }
					 * else { path = null; }
					 */
				} catch (Exception e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
					version = null;
				}
			}

			if (path != null) {
				try {
					//System.out.println("PATH:" + path.toAbsolutePath());
					Manifest manifest = new JarFile(path.toAbsolutePath().toString()).getManifest();
					Attributes attributes = manifest.getMainAttributes();
					version = attributes.getValue("Build-Label"); //$NON-NLS-1$
					System.out.println(" VALUE:" + version); //$NON-NLS-1$
				} catch (Exception e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
					version = null;
				}
			}

			if (version == null) {
				version = "isEmbeded?"; //$NON-NLS-1$
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return version;
	}
}
