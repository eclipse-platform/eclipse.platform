package org.eclipse.pki.pkiselection;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.pki.auth.AuthenticationPlugin;

public class PKIProperties extends Authenticator {
	
	
	private String keyStore="";
	private String keyStoreType = "";
	private String keyStoreProvider = "";
	private String username = null;
	private transient String keyStorePassword = "";
	private static PKI lastPKI=null;
	private static PKIProperties sslProperties=null;
	public static PKIProperties getNewInstance() {
		return new PKIProperties();
	}
	public static PKIProperties getInstance() {
		if ( sslProperties == null ) {
			synchronized(PKIProperties.class) {
				if ( sslProperties == null ) {
					sslProperties = new PKIProperties();
					try {
						sslProperties.load();
					} catch(Exception ignoreException) {
						ignoreException.printStackTrace();
					}
				}
			}
		}
		return sslProperties; 
	}
	private PKIProperties() {}
	public PasswordAuthentication getPasswordAuthentication() {
		PasswordAuthentication auth = null;
		
		try {
			auth = new PasswordAuthentication(this.getUsername(), this.getKeyStorePassword().toCharArray() );
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return auth;
	}
	public String getKeyStore() {
		return keyStore;
	}
	public void setKeyStore(String keyStore) {
		this.keyStore = keyStore;
	}
	public String getKeyStoreType() {
		return keyStoreType;
	}
	public void setKeyStoreType(String keyStoreType) {
		this.keyStoreType = keyStoreType;
	}
	public String getKeyStoreProvider() {
		return keyStoreProvider;
	}
	public void setKeyStoreProvider(String keyStoreProvider) {
		this.keyStoreProvider = keyStoreProvider;
	}
	public String getKeyStorePassword() {
		return keyStorePassword;
	}
	public void setKeyStorePassword(String keyStorePassword) {
		this.keyStorePassword = keyStorePassword;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public void restore() {
		try {
			if (( this.getKeyStore() != null ) &&
				( this.getKeyStoreType() != null ) &&
				( this.getKeyStoreProvider() != null) &&
				( this.getKeyStorePassword() != null) ) {
			
				if ( !(this.getKeyStore().isEmpty()) ) {
					System.setProperty("javax.net.ssl.keyStore", this.getKeyStore() );
				}
			
				if ( !(this.getKeyStoreType().isEmpty()) ) {
					System.setProperty("javax.net.ssl.keyStoreType", this.getKeyStoreType() );
				}
			
				if ( !(this.getKeyStoreProvider().isEmpty() )) {
					System.setProperty("javax.net.ssl.keyStoreProvider", this.getKeyStoreProvider() );
				}
			
				if ( !(this.getKeyStorePassword().isEmpty() )) {
					if ( lastPKI != null ) {
						if ( lastPKI.getKeyStorePassword().isEmpty() ) {
							System.clearProperty("javax.net.ssl.keyStorePassword");
						}
					} else {
						System.setProperty("javax.net.ssl.keyStorePassword", getKeyStorePassword() );
					}
				}
			} else {
				clear();
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void load() {
		if ( System.getProperty("javax.net.ssl.keyStore") != null) {
			sslProperties.setKeyStore(System.getProperty("javax.net.ssl.keyStore"));
		} else {
			sslProperties.setKeyStore("");
		}
		if ( System.getProperty("javax.net.ssl.keyStoreType") != null ) {
			sslProperties.setKeyStoreType(System.getProperty("javax.net.ssl.keyStoreType"));
		} else {
			sslProperties.setKeyStoreType("");
		}
		if (System.getProperty("javax.net.ssl.keyStoreProvider") != null) {
			sslProperties.setKeyStoreProvider(System.getProperty("javax.net.ssl.keyStoreProvider"));
		} else {
			if ( System.getProperty("javax.net.ssl.keyStoreType") != null ) {
				if ( System.getProperty("javax.net.ssl.keyStoreType").equalsIgnoreCase("pkcs12") ) {
					System.clearProperty("javax.net.ssl.keyStoreProvider");
				} else {
					sslProperties.setKeyStoreProvider("");
				}
			}
		}
		if (System.getProperty("javax.net.ssl.keyStorePassword") != null ) {
			sslProperties.setKeyStorePassword(System.getProperty("javax.net.ssl.keyStorePassword"));
			
		} else {
			sslProperties.setKeyStorePassword("");
		}
		
		sslProperties.setUsername(System.getProperty("user.name"));
	}
	public void setLastPkiValue( PKI pki ) {
		lastPKI = pki;
	}
	public void clear() {
		System.clearProperty("javax.net.ssl.keyStoreProvider");
		System.clearProperty("javax.net.ssl.keyStore");
		System.clearProperty("javax.net.ssl.keyStoreProvider");
		System.clearProperty("javax.net.ssl.keyStorePassword");
	}
	public void dump() {
		StringBuffer sb = new StringBuffer();
		sb.append("javax.net.ssl.keyStore=");
		sb.append(sslProperties.getKeyStore());
		sb.append("\n");
		sb.append("javax.net.ssl.keyStoreType=");
		sb.append(sslProperties.getKeyStoreType());
		sb.append("\n");
		sb.append("javax.net.ssl.keyStoreProvider=");
		sb.append(sslProperties.getKeyStoreProvider());
		sb.append("\n");
		
		Status status = new Status(IStatus.INFO, sb.toString(), null);
		AuthenticationPlugin.getDefault().getLogger().log(status);
	}
}
