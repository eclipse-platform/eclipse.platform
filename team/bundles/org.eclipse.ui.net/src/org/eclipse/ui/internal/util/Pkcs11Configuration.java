/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.internal.util;

public class Pkcs11Configuration implements PkcsConfigurationIfc {

	public String implementationName;
	public String configurationFilename;
	public String configurationLocationDir;
	public String libraryFilename;
	public String libraryLocationDir;
	public String pkcs11Vendor;

	public String getPkcs11Vendor() {
		return pkcs11Vendor;
	}

	public void setPkcs11Vendor(String pkcs11Vendor) {
		this.pkcs11Vendor = pkcs11Vendor;
	}

	public void setImplementationName(String implementationName) {
		this.implementationName = implementationName;
	}

	public void setConfigurationFilename(String configurationFilename) {
		this.configurationFilename = configurationFilename;
	}

	public void setConfigurationLocationDir(String configurationLocationDir) {
		this.configurationLocationDir = configurationLocationDir;
	}

	public void setLibraryFilename(String libraryFilename) {
		this.libraryFilename = libraryFilename;
	}

	public void setLibraryLocationDir(String libraryLocationDir) {
		this.libraryLocationDir = libraryLocationDir;
	}

	@Override
	public String getImplementationName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getConfigurationFilename() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getConfigurationLocationDir() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getLibraryFilename() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getLibraryLocationDir() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getCertPassPhrase() {
		// TODO Auto-generated method stub
		return null;
	}

}
