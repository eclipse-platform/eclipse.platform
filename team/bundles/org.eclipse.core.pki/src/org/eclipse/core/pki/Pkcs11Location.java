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

public class Pkcs11Location extends Pkcs11LocationImpl  {
	private static String userSpecifiedDirectory = "none"; //$NON-NLS-1$
	private static boolean userDirFound=false;


	public static String getUserSpecifiedDirectory() {
		return userSpecifiedDirectory;
	}
	public static  void setUserSpecifiedDirectory(String userSpecifiedDirectory) {
		Pkcs11Location.userSpecifiedDirectory = userSpecifiedDirectory;
	}
	public String getJavaPkcs11Cfg() {
		return getPath().toString();
	}
	public static boolean isUserSpecifiedDirectoryFound() {
		StringBuilder sb = new StringBuilder();
		sb.append( getUserSpecifiedDirectory() );
		userDirFound = Pkcs11LocationImpl.isPath(sb);
		return userDirFound;
	}
}
