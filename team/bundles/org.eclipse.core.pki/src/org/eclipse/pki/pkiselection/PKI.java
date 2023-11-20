package org.eclipse.pki.pkiselection;

public class PKI {
	private String keyStore="";
	private String keyStoreType = "";
	private String keyStoreProvider = "";
	private transient String keyStorePassword = "";
	private boolean isSecureStorage=false;
	public PKI() {}
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
	
	public boolean isSecureStorage() {
		return isSecureStorage;
	}
	public void setSecureStorage(boolean isSecureStorage) {
		this.isSecureStorage = isSecureStorage;
	}
	public void reSetSystem() {
		try {
			if ( this.getKeyStore() != null )  {
				System.setProperty("javax.net.ssl.keyStore", this.getKeyStore() );
			} else {
				System.clearProperty("javax.net.ssl.keyStore");
			}
			
			if ( this.getKeyStoreType() != null )  {
				System.setProperty("javax.net.ssl.keyStoreType", this.getKeyStoreType() );
			} else {
				System.clearProperty("javax.net.ssl.keyStoreType");
			}
			
			if( this.getKeyStoreProvider() != null) {
				System.out.println("PKI - CLEARING keystoreprovider");
				if ( this.getKeyStoreProvider().isEmpty()) {
					System.clearProperty("javax.net.ssl.keyStoreProvider");
				} else {
					if (this.getKeyStoreType().equalsIgnoreCase("PKCS12") ) {
						System.clearProperty("javax.net.ssl.keyStoreProvider");
					} else {
						System.setProperty("javax.net.ssl.keyStoreProvider", this.getKeyStoreProvider() );
					}
				}
			} else {
				System.clearProperty("javax.net.ssl.keyStoreProvider");
			}
			
			if ( this.getKeyStorePassword() != null) {
				System.setProperty("javax.net.ssl.keyStorePassword", getKeyStorePassword() );
			} else {
				System.clearProperty("javax.net.ssl.keyStorePassword");
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}