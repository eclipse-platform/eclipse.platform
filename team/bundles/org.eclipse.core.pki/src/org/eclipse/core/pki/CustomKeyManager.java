package org.eclipse.core.pki;

import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashMap;

import javax.net.ssl.KeyManager;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509KeyManager;

public class CustomKeyManager extends X509ExtendedKeyManager implements X509KeyManager {
	private static final int KEY_ENCIPHERMENT = 2;
	private static final int DIGITAL_SIGNATURE = 0;
	private KeyStore keyStore;
	private char[] password;
	protected static String selectedFingerprint="NOTSET";

	public CustomKeyManager(KeyStore keyStore, char[] passwd, HashMap <InetAddress, String> hosts) {
		this.keyStore=keyStore;
		this.password=password;
	}
	public String chooseClientAlias(String[] arg0, Principal[] arg1, Socket arg2) {
		// TODO Auto-generated method stub
		String message="Presenting X509 fingerprint:";
		String amessage=" using certificate alias:";
		StringBuilder sb=new StringBuilder();
		String selectedAlias=null;
		String alias = null;
		String info=null;
		String fingerprint=null;
		boolean isOK=true;
		
		try {
			
			
			Enumeration<String> aliases = this.keyStore.aliases();
			sb.append(message);
			while ( aliases.hasMoreElements() ) {
				alias = aliases.nextElement();
				DebugLogger.printDebug("CustomKeyManager --------------------- chooseClientAlias:"+alias);
				if ( this.getPrivateKey(alias) != null ) {
					X509Certificate x509 = (X509Certificate) this.keyStore.getCertificate(alias);
					try {
						x509.checkValidity();
						if (!(isKeyEncipherment(x509.getKeyUsage()))) {
							//selectedAlias=alias;
							fingerprint=FingerprintX509.INSTANCE.getFingerPrint(x509,"MD5" );
							info ="ALIAS:"+ alias+" FINGERPRINT:"+fingerprint+" ALG:"+x509.getSigAlgName();
							DebugLogger.printDebug("KeyManager -  SELECTED finger:"+getSelectedFingerprint());
							//System.err.println("KeyManager -  DUMP OUT DATA:"+info);
							
							if ( getSelectedFingerprint() != null ) {
								if ( getSelectedFingerprint().equals("NOTSET")) {
									setSelectedFingerprint(fingerprint);
								} 
							} else {
								setSelectedFingerprint(fingerprint);
							}
							if ( getSelectedFingerprint().equals(fingerprint)) { 
								isOK=true;
								selectedAlias=alias;
								sb.append(fingerprint);
								sb.append(amessage);
								sb.append(alias);
								message = sb.toString();
								break;
							}
						}
					} catch (CertificateExpiredException e) {
						// TODO Auto-generated catch block
						System.err.println("KeyManager: Please remove EXPIRED certificate:"+ alias+" using your CSPid Manager.");
						//e.printStackTrace();
					} catch (CertificateNotYetValidException e) {
						// TODO Auto-generated catch block
						System.err.println("KeyManager: Please check invalid certificate:"+ alias+" using your CSPid Manager.");
						//e.printStackTrace();
					}
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (!(isOK)) {
			message = (selectedAlias == null) ? "PKI misconfiguration. Please check pkcs11" : message + selectedAlias;
			System.out.println("KeyManager: "+message);
		}
		return selectedAlias;
	}
	private static boolean isDigitalSignature(boolean[] ba) {
		if ( ba != null) {
			
			return ba[DIGITAL_SIGNATURE];
		} else {
			return false;
		}
	}

	private static boolean isKeyEncipherment(boolean[] ba) {
		if ( ba != null) {
			
			return ba[KEY_ENCIPHERMENT];
		} else {
			return false;
		}
	}

	public String chooseServerAlias(String arg0, Principal[] arg1, Socket arg2) {
		// TODO Auto-generated method stub
		DebugLogger.printDebug("CustomKeyManager ---- chooseServertAlias");
		return null;
	}

	public X509Certificate[] getCertificateChain(String alias) {
		// TODO Auto-generated method stub
		
		DebugLogger.printDebug("CustomKeyManager ---- getCertificateChain INCOMING ALIAS:"+alias);
		X509Certificate[] X509Certs=null;
		X509Certificate X509Cert=null;
		try {
			Certificate[] certificates = this.keyStore.getCertificateChain(alias);
			if ( certificates != null ) {
				X509Certs = new X509Certificate[ certificates.length ];
				for(int i=0; i < certificates.length; i++) {
					X509Cert= (X509Certificate ) certificates[i];
					if (!(isKeyEncipherment(X509Cert.getKeyUsage()))) {
						X509Certs[i] = X509Cert;
					} else {
						if ((isKeyEncipherment(X509Cert.getKeyUsage())) && alias.contains("PKI")) {
							X509Certs[i] = X509Cert;
						}
					}
				}

			} else {
				X509Cert = (X509Certificate) this.keyStore.getCertificate(alias);
				if ( X509Cert != null ) {
					X509Certs = new X509Certificate[1];
					if (isDigitalSignature(X509Cert.getKeyUsage()) ) {
						X509Certs[0] = X509Cert;
					} else {
						if (alias.contains("PKI")) {
							X509Certs[0] = X509Cert;
						}
					}
				}

			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		DebugLogger.printDebug("CustomKeyManager CERTIFICATE CHAIN  COUNT:"+X509Certs.length);
		//return X509Certs;
		try {
			X509Certs = new X509Certificate[1];
			X509Certs[0] = (X509Certificate) this.keyStore.getCertificate(alias);
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		DebugLogger.printDebug("CustomKeyManager CERTIFICATE CHAIN  COUNT:"+X509Certs.length);
		return X509Certs;
	}

	public String[] getClientAliases(String arg0, Principal[] arg1) {
		// TODO Auto-generated method stub
		//return null;
		DebugLogger.printDebug("CustomKeyManager -----------------------------getClientAliases");
		return new String[] {chooseClientAlias(null, arg1, null) };
		
	}

	public PrivateKey getPrivateKey(String alias) {
		// TODO Auto-generated method stub
		DebugLogger.printDebug("CustomKeyManager ---------------------getPrivateKey  ALIAS:"+alias);
		PrivateKey privateKey = null;
		try {
			privateKey = (PrivateKey) keyStore.getKey(alias, "".toCharArray());
		} catch (UnrecoverableKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return privateKey;
	}

	public String[] getServerAliases(String arg0, Principal[] arg1) {
		// TODO Auto-generated method stub
		DebugLogger.printDebug("CustomKeyManager ---- getServertAliases");
		return null;
	}
	public static String getSelectedFingerprint() {
		return selectedFingerprint;
	}
	public void setSelectedFingerprint(String selectedFingerprint) {
		CustomKeyManager.selectedFingerprint = selectedFingerprint;
	}	
}
