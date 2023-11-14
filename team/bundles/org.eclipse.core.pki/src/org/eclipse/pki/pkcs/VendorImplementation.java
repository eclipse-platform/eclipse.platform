package org.eclipse.pki.pkcs;

import java.security.KeyStore;

import javax.net.ssl.SSLContext;

import org.eclipse.pki.pkcs.ProviderImpl;
import org.eclipse.core.pki.AuthenticationBase;
import org.eclipse.pki.interfaces.IProvider;


public class VendorImplementation extends ProviderImpl implements IProvider{
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
			
			if ( security.isPKCS11Enabled()) {
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
		getNewInstance();
	}
	public void enable(boolean changeValue) {
		security.setPKCS11on(changeValue);
	}
	public boolean isEnabled() {
		return security.isPKCS11Enabled();
	}
	public boolean isInstalled() {
		return security.isPKCS11Installed();
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
		System.out.println("VendorImplementation ---FIX THIS------ setSelectedX509Fingerprint PRINT:"+fingerprint);
		
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
}
