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
package org.eclipse.ui.pki.pkcs;

import java.security.KeyStore;
import java.util.List;

import javax.net.ssl.SSLContext;

import org.eclipse.core.pki.AuthenticationBase;

public class VendorImplementation implements EclipsePkiProvider {
	private static final  VendorImplementation venderImpl=new VendorImplementation();
	private static VendorImplementation csp=new VendorImplementation();
	
	private static boolean isquiet=false;
	private static String selectedX509Fingerprint;
	
	private static final VendorImplementation silentPkcs11=new VendorImplementation(isquiet);
	private transient boolean enabled=false;
	public static VendorImplementation getInstance(){ return venderImpl;}
	public static VendorImplementation getInstance(boolean quiet){	isquiet=quiet;return silentPkcs11;}
	
	private VendorImplementation() {
		try {
			
			if ( security.isPkcs11Setup()) {
				enabled=true;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
	}
	private VendorImplementation(boolean quiet) {
		//super(quiet);
	}
	public static void refresh() {
		getInstance();
	}
	public void enable(boolean changeValue) {
		enabled=changeValue;
	}
	public boolean isEnabled() {
		return security.isPkcs11Setup();
	}
	public boolean isInstalled() {
		return security.isPkcs11Setup();
	}
	public KeyStore getKeyStore() {
		return security.getKeyStore();
	}
	public String getSelectedX509FingerPrint() {
		return selectedX509Fingerprint;
	}
	public void setSelectedX509Fingerprint( String fingerprint ) {
		//System.out.println("VendorImplementation--------- setSelectedX509Fingerprint   PRINTE:"+fingerprint);
		selectedX509Fingerprint=fingerprint;
		//System.out.println("VendorImplementation ---FIX THIS------ setSelectedX509Fingerprint PRINT:"+fingerprint);
		
		AuthenticationBase.INSTANCE.setFingerprint(selectedX509Fingerprint);
		AuthenticationBase.INSTANCE.setSSLContext(this.getKeyStore());
	}
	public SSLContext getSSLContext() {
		return security.getSSLContext();
	}
	public boolean login( final String pin ) {
		security.setPin(pin);
		return security.login();
	}
	public void logoff() {
		security.logoff();
	}
	@Override
	public List<String> getList() {
		// TODO Auto-generated method stub
		return AuthenticationBase.INSTANCE.getList();
	}
	@Override
	public String getAlias() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void off() {
		// TODO Auto-generated method stub
		
	}
}
