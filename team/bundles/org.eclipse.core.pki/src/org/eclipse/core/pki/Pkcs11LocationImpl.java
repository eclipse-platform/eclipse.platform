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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;



public class Pkcs11LocationImpl extends Pkcs11LibraryFinder implements Pkcs11LocationIfc {
	private static String directory=null;
	private static Path path =null;
	private static Path jarDir = null;
	protected static boolean found = false;
	protected static boolean pkcs11Found = false;
	private static Pkcs11LocationIfc location = null;
	static Pkcs11LibraryFinder jarLocation = null;
	public static Pkcs11LocationImpl getPkcs11LocationInstance() {

		if ( location == null ) {
			synchronized(Pkcs11LocationImpl.class) {
				if ( location == null ) {
					jarLocation = findSunPkcs11JarInstance();
					if (jarLocation.isPkcs11() ) {
						pkcs11Found=true;
						System.out.println("PKCS11Locationimp jarDIR:" + jarLocation.getJarDirectory().toString()); //$NON-NLS-1$
						setJarDir( Paths.get(jarLocation.getJarDirectory().toString()) );
					}
					if ( isUnix() ) {
						location = Pkcs11UnixLocation.getInstance();
					} else {
						location = Pkcs11MSLocation.getInstance();
					}
				}
			}
		}
		return (Pkcs11LocationImpl) location;
	}

	@Override
	public void initialize() {
		// TODO Auto-generated method stub
		System.out.println("PKCS11Locationimpl  OVERRIDE INITIALIZE"); //$NON-NLS-1$

	}

	@SuppressWarnings("resource")
	private static boolean isUnix() {
		for ( Path path : FileSystems.getDefault().getRootDirectories()) {
			if (path.startsWith(FileSystems.getDefault().getSeparator())) {
				return true;
			}
		}
		return false;
	}

	protected static boolean isPath( StringBuilder pkcs11Path ) {

		try {
			if (!(pkcs11Path.toString().endsWith("java_pkcs11.cfg"))) { //$NON-NLS-1$
				pkcs11Path.append(FileSystems.getDefault().getSeparator());
				pkcs11Path.append("java_pkcs11.cfg"); //$NON-NLS-1$
			}
			if (isDirectory(pkcs11Path.toString())) {
				setPath(Paths.get(pkcs11Path.toString()));
				setFound(true);
				return true;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return false;
	}
	protected static boolean isDirectory(String incoming) {
		try {
			Path path = Paths.get( incoming );
			if (Files.exists(path)) {
				return true;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	public static String getDirectory() {
		return directory;
	}
	public static void setDirectory(String directory) {
		Pkcs11LocationImpl.directory = directory;
	}
	public static Path getPath() {
		return path;
	}
	public static void setPath(Path path) {
		Pkcs11LocationImpl.path = path;
	}

	public static Path getJarDir() {
		return jarDir;
	}

	public static void setJarDir(Path jarDir) {
		Pkcs11LocationImpl.jarDir = jarDir;
	}

	public static void setFound(boolean found) {
		Pkcs11LocationImpl.found = found;
	}
	public static boolean isFound() {
		return found;
	}
	public static boolean isPkcs11Found() {
		return pkcs11Found;
	}
}
