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

import java.nio.file.Paths;



/*
 *  This class was a best guess to find out where the required java and pkcs11
 *  files were located.   A PKCS11_HOME variable overrides the implementation.
 */

public class Pkcs11MSLocation extends Pkcs11Location implements Pkcs11LocationIfc {
	private static final String vendorDirectory = System.getProperty("vendor.install.dir"); //$NON-NLS-1$

	private static final String dir64bit = "C:\\Progra~2\\" + vendorDirectory; //$NON-NLS-1$
	private static final String dir32bit = "C:\\Windows\\SysWow64\\" + vendorDirectory; //$NON-NLS-1$
	private static  Pkcs11LocationIfc location=null;
	public static Pkcs11MSLocation getInstance() {
		if ( location == null ) {
			synchronized(Pkcs11LocationImpl.class) {
				if ( location == null ) {
					location = new Pkcs11MSLocation();
					location.initialize();
				}
			}
		}
		return (Pkcs11MSLocation) location;
	}
	@Override
	public void initialize() {
		windowsPath();
	}
	private void windowsPath() {
		StringBuilder pkcs11Path = new StringBuilder();
		Pkcs11FixConfigFile configFile = null;
		try {
			if (!(getUserSpecifiedDirectory().equals("none"))) { //$NON-NLS-1$
				pkcs11Path.append( getUserSpecifiedDirectory() );
				if ((isDirectory( pkcs11Path.toString() ))) {
					setDirectory(pkcs11Path.toString());
				}
			} else {
				pkcs11Path = new StringBuilder();
				if (System.getenv("PKCS11_HOME") != null) { //$NON-NLS-1$
					pkcs11Path = new StringBuilder();
					pkcs11Path.append(System.getenv("PKCS11_HOME")); //$NON-NLS-1$
					System.out.println(
							"Pkcs11MSlocation --  Setting pkcs11 loaction from PKCS11_HOME:" + pkcs11Path.toString()); //$NON-NLS-1$
					if ( isDirectory( pkcs11Path.toString() )) {
						configFile = Pkcs11FixConfigFile.getCfgInstance(pkcs11Path.toString());
						setDirectory(configFile.getCfgFilePath());
					} else {
						setDirectory(null);
					}
				}
				if (getDirectory() == null) {
					pkcs11Path = new StringBuilder();
					pkcs11Path.append(dir64bit);
					if (!(isDirectory( pkcs11Path.toString() ))) {
						pkcs11Path = new StringBuilder();
						pkcs11Path.append(dir32bit);
						if ((isDirectory(pkcs11Path.toString()))) {
							System.out.println("Pkcs11MSlocation --   64bit  FOUND LOACTION:" + pkcs11Path.toString()); //$NON-NLS-1$
							configFile = Pkcs11FixConfigFile.getCfgInstance(pkcs11Path.toString());
							setDirectory(configFile.getCfgFilePath());
						}
					} else {
						System.out.println("Pkcs11MSlocation --   32bit  FOUND LOACTION:" + pkcs11Path.toString()); //$NON-NLS-1$
						configFile = Pkcs11FixConfigFile.getCfgInstance(pkcs11Path.toString());
						setDirectory(configFile.getCfgFilePath());
					}
				}
			}
			System.out.println("Pkcs11MSlocation --   LOCATION:" + pkcs11Path.toString()); //$NON-NLS-1$
			if ( getDirectory() != null ) {
				if (!( isPath( pkcs11Path ))) {
					setFound(false);
				} else {
					setFound(true);
					setPath( Paths.get( getDirectory()) );
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
