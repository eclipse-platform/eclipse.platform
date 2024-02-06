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

import java.nio.file.FileSystems;
import java.util.Properties;

public class Pkcs11UnixLocation extends Pkcs11Location implements Pkcs11LocationIfc {
	private static final String unixPKCS11Directory = "/opt/pkcs11"; //$NON-NLS-1$
	private static Pkcs11LocationIfc location = null;
	public static Pkcs11UnixLocation getInstance() {
		if ( location == null ) {
			synchronized(Pkcs11LocationImpl.class) {
				if ( location == null ) {
					location = new Pkcs11UnixLocation();
					location.initialize();
				}
			}
		}
		return (Pkcs11UnixLocation) location;
	}
	@Override
	public void initialize() {
		unixPath();
	}

	private void unixPath() {
		StringBuilder pkcs11Home = new StringBuilder();
		System.out.println("Pkcs11UnixLocation - unixPath"); //$NON-NLS-1$
		try {
			if (!(getUserSpecifiedDirectory().equals("none"))) { //$NON-NLS-1$
				pkcs11Home.append( getUserSpecifiedDirectory() );
				if ((isDirectory( pkcs11Home.toString() ))) {
					setDirectory(pkcs11Home.toString());
				}
			} else {
				pkcs11Home = new StringBuilder();
				pkcs11Home.append(unixPKCS11Directory);
				if ( (!(isDirectory(pkcs11Home.toString()))) ) {
					Properties p = System.getProperties();
					pkcs11Home = new StringBuilder();
					pkcs11Home.append(p.getProperty("user.home")); //$NON-NLS-1$
					pkcs11Home.append(FileSystems.getDefault().getSeparator());
					pkcs11Home.append("pkcs11"); //$NON-NLS-1$
					pkcs11Home.append(FileSystems.getDefault().getSeparator());
					pkcs11Home.append("java_pkcs11.cfg"); //$NON-NLS-1$
					if (isDirectory(pkcs11Home.toString())) {
						setDirectory(pkcs11Home.toString());
					}
				} else {
					pkcs11Home.append(FileSystems.getDefault().getSeparator());
					pkcs11Home.append("java_pkcs11.cfg"); //$NON-NLS-1$
					if (isDirectory(pkcs11Home.toString())) {
						setDirectory(pkcs11Home.toString());
					} else {
						System.out.println("Pkcs11UnixLocation - pkcs11 cfg file was NOT found."); //$NON-NLS-1$
					}
				}
			}
			if ( getDirectory() != null ) {
				if ( !(isPath( pkcs11Home ))) {
					setFound(false);
				}
			} else {
				System.out.println("Pkcs11UnixLocation -  DIDNT FIND PATH:" + pkcs11Home.toString()); //$NON-NLS-1$
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
