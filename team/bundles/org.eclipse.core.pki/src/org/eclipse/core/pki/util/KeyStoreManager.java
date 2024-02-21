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
package org.eclipse.core.pki.util;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Optional;

import javax.net.ssl.X509KeyManager;

import org.eclipse.core.pki.FingerprintX509;

//import sun.security.pkcs11.wrapper.PKCS11Exception;

public enum KeyStoreManager implements X509KeyManager {
	INSTANCE;

	protected final int KEY_ENCIPHERMENT = 2;
	protected final int DIGITAL_SIGNATURE = 0;
	protected boolean isKeyStoreInitialized = false;
	protected String selectedFingerprint = "NOSET"; //$NON-NLS-1$
	protected KeyStore keyStore = null;

	public KeyStore getKeyStore(String fileLocation, String password, KeyStoreFormat format) {
		InputStream in = null;
		try {

			try {
				in = new FileInputStream(fileLocation);
				in = new BufferedInputStream(in);

				keyStore = KeyStore.getInstance(format.getValue());
				keyStore.load(in, password.toCharArray());

				setKeyStoreInitialized(true);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				LogUtil.logError("Configure KeyStore - No File Found:", e); //$NON-NLS-1$
			} catch (KeyStoreException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				LogUtil.logError("Configure KeyStore - Initialize keystore, bad password? ", e); //$NON-NLS-1$
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				LogUtil.logError("Configure KeyStore - No algorythm found, ", e); //$NON-NLS-1$
			} catch (CertificateException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				LogUtil.logError("Configure KeyStore - Certificate Error", e); //$NON-NLS-1$
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				LogUtil.logError("Configure KeyStore - I/O Error, bad password?", e); //$NON-NLS-1$
			}
			if ( keyStore != null) {
				return keyStore;
			}
			return null;
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Returns a KeyStore object loaded from provided {@link InputStream} and decrypted with given password
	 * @param in
	 * @param password
	 * @param format "JKS", "PKCS12", "PKCS11"
	 * @throws NoSuchAlgorithmException
	 * @throws CertificateException
	 * @throws IOException
	 */

	public KeyStore getKeyStore(InputStream in, String password, KeyStoreFormat format)
	throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {

		keyStore = KeyStore.getInstance(format.getValue());
		char pwd[] = null;
		if(password != null)
			pwd = password.toCharArray();

		keyStore.load(in, pwd);
		return keyStore;

	}

	public KeyStore getKeyStore(KeyStoreFormat format)
			throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, NoSuchProviderException {

		String pin = ""; //$NON-NLS-1$
			KeyStore.PasswordProtection pp = new KeyStore.PasswordProtection(pin.toCharArray());
			keyStore = KeyStore.getInstance("pkcs11", "SunPKCS11"); //$NON-NLS-1$ //$NON-NLS-2$
			try {
				keyStore.load(null, pp.getPassword());

				setKeyStoreInitialized(true);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				//System.out.println("KeyStoreUtil ------ The user elected to press cancel, KEYSOTRE is NOT initialized.");
				keyStore = null;
			}
			return keyStore;
	}

	public KeyStore getKeyStore() {
		return keyStore;
	}
	public void setKeyStore(KeyStore keyStore) {
		this.keyStore=keyStore;
		setKeyStoreInitialized(true);
	}

	public Hashtable<X509Certificate, PrivateKey> getCertificates(KeyStore keyStore) {

		Hashtable<X509Certificate, PrivateKey> table = new Hashtable<>();
		PrivateKey privateKey=null;

		try {
			if (isKeyStoreInitialized()) {
				Enumeration<String> aliasesEnum = keyStore.aliases();
				while (aliasesEnum.hasMoreElements())
				{
				      String alias = aliasesEnum.nextElement();
				      X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);
					  try {
						if ( isDigitalSignature(certificate.getKeyUsage()) ) {
			    	  		privateKey = (PrivateKey) keyStore.getKey(alias, null);
							if ( privateKey != null) {
								table.put(certificate, privateKey);
							}
				    	 }
					  } catch (UnrecoverableKeyException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return table;

	}

	public ArrayList<String> getAliases(KeyStore keyStore) {

		ArrayList<String>aliasList = new ArrayList<>();
		// PrivateKey privateKey=null;
		try {
			Enumeration<String> aliasesEnum = keyStore.aliases();
			while (aliasesEnum.hasMoreElements())
			{
			      String alias = aliasesEnum.nextElement();
			      X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);
				  try {
					if ( isDigitalSignature(certificate.getKeyUsage()) ) {
//		    	  		privateKey = (PrivateKey) keyStore.getKey(alias, null);
//						if ( privateKey != null) {
//							aliasList.add(alias);
//						}
						aliasList.add(alias);
			    	 }

				  } catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//System.out.println("END OF WHILE STATUEM");
			}
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return aliasList;

	}
	public boolean checkUserKeystorePass(String certPath, String password, String certType)
    {
		StringBuilder message = new StringBuilder();
		message.append("Problem reading your certificate.  \n\r \n\r"); //$NON-NLS-1$

		KeyStore keyStore;
		try {
			InputStream in = null;

			in = new FileInputStream(certPath);
			in = new BufferedInputStream(in);

			keyStore = KeyStore.getInstance(certType);
			char pwd[] = null;
			if (password != null)
				pwd = password.toCharArray();

			keyStore.load(in, pwd);

			getAliases(keyStore);
			return true;

		} catch (KeyStoreException e) {
			message.append("The selected file does not appear "); //$NON-NLS-1$
			message.append("to be a valid PKCS file.  Please "); //$NON-NLS-1$
			message.append("select a different file and/or "); //$NON-NLS-1$
			message.append("check the logs for more information."); //$NON-NLS-1$
			System.err.printf("%s\n", message.toString()); //$NON-NLS-1$
		} catch (NoSuchAlgorithmException e) {
			message.append("An unexpected error '"); //$NON-NLS-1$
			message.append(e.getClass().getName());
			message.append("' occurred: "); //$NON-NLS-1$
			message.append(e.getMessage());
			message.append(" Please select a different file and/or "); //$NON-NLS-1$
			message.append("check the logs for more information."); //$NON-NLS-1$
			System.err.printf("%s\n", message.toString()); //$NON-NLS-1$
		} catch (CertificateException e) {
			message.append("Either your password was incorrect or the "); //$NON-NLS-1$
			message.append("the selected file is corrupt. Please try "); //$NON-NLS-1$
			message.append("a different password or PKCS file."); //$NON-NLS-1$
			System.err.printf("%s CertificateException: %s\n", message.toString(), e.getMessage()); //$NON-NLS-1$
		} catch (IOException e) {
			if (e.getCause().toString().contains("FailedLoginException")) { //$NON-NLS-1$
				message.append("\tYou entered an incorrect password. \n\r"); //$NON-NLS-1$
				message.append("\tPlease check your password and re-enter it. \n\r  \n\r"); //$NON-NLS-1$
				System.err.printf("%s IOException: %s\n", message.toString(), e.getMessage()); //$NON-NLS-1$
			} else {

				message.append("Either your password was incorrect or the "); //$NON-NLS-1$
				message.append("selected file is corrupt. Please try "); //$NON-NLS-1$
				message.append("a different password or PKCS file."); //$NON-NLS-1$
				System.err.printf("%s IOException: %s\n", message.toString(), e.getMessage()); //$NON-NLS-1$
			}
		}

	    //System.out.println("KeyStoreUtil ----------checkUserKeystorePass   NEEDS TO RETURN FALSE and its NOT");
    	return false;
    }

	@Override
	public String[] getClientAliases(String keyType, Principal[] issuers) {
		// TODO Auto-generated method stub
		return new String[] { chooseClientAlias(null, issuers, null) };
	}

	@Override
	public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
		// TODO Auto-generated method stub
		String message = "Presenting X509 fingerprint:"; //$NON-NLS-1$
		String amessage = " using certificate alias:"; //$NON-NLS-1$
		StringBuilder sb = new StringBuilder();
		String selectedAlias = null;
		String alias = null;
		String fingerprint = null;
		boolean isOK = true;

		try {

			Enumeration<String> aliases = this.keyStore.aliases();
			sb.append(message);
			while (aliases.hasMoreElements()) {
				alias = aliases.nextElement();
				LogUtil.logInfo(amessage);
				if (this.getPrivateKey(alias) != null) {
					X509Certificate x509 = (X509Certificate) this.keyStore.getCertificate(alias);
					try {
						x509.checkValidity();
						if (!(isKeyEncipherment(x509.getKeyUsage()))) {
							// selectedAlias=alias;
							fingerprint = FingerprintX509.INSTANCE.getFingerPrint(x509, "MD5"); //$NON-NLS-1$
							System.out.println("KeyManager -  SELECTED finger:" + getSelectedFingerprint()); //$NON-NLS-1$
							// System.err.println("KeyManager - DUMP OUT DATA:"+info);

							if (getSelectedFingerprint() != null) {
								if (getSelectedFingerprint().equals("NOTSET")) { //$NON-NLS-1$
									setSelectedFingerprint(fingerprint);
								}
							} else {
								setSelectedFingerprint(fingerprint);
							}
							if (getSelectedFingerprint().equals(fingerprint)) {
								isOK = true;
								selectedAlias = alias;
								sb.append(fingerprint);
								sb.append(amessage);
								sb.append(alias);
								message = sb.toString();
								break;
							}
						}
					} catch (CertificateExpiredException e) {
						// TODO Auto-generated catch block
						System.err.println("KeyManager: Please remove EXPIRED certificate:" + alias //$NON-NLS-1$
								+ " using your pkcs11 Manager."); //$NON-NLS-1$
						// e.printStackTrace();
					} catch (CertificateNotYetValidException e) {
						// TODO Auto-generated catch block
						System.err.println("KeyManager: Please check invalid certificate:" + alias //$NON-NLS-1$
								+ " using your pkcs11 Manager."); //$NON-NLS-1$
						// e.printStackTrace();
					}
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (!(isOK)) {
			message = (selectedAlias == null) ? "PKI misconfiguration. Please check " : message + selectedAlias; //$NON-NLS-1$
			System.out.println("KeyManager: " + message); //$NON-NLS-1$
		}
		return selectedAlias;
	}

	public boolean isKeyStoreInitialized() {
		return isKeyStoreInitialized;
	}

	private void setKeyStoreInitialized(boolean isKeyStoreInitialized) {
		this.isKeyStoreInitialized = isKeyStoreInitialized;
	}

	private boolean isDigitalSignature(boolean[] ba) {
		if (ba != null) {

			return ba[DIGITAL_SIGNATURE];
		} else {
			return false;
		}
	}

	private boolean isKeyEncipherment(boolean[] ba) {
		if (ba != null) {

			return ba[KEY_ENCIPHERMENT];
		} else {
			return false;
		}
	}

	@Override
	public String[] getServerAliases(String keyType, Principal[] issuers) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public X509Certificate[] getCertificateChain(String alias) {
		// TODO Auto-generated method stub
		X509Certificate[] X509Certs = null;
		X509Certificate X509Cert = null;
		try {
			Certificate[] certificates = this.keyStore.getCertificateChain(alias);
			if (certificates != null) {
				X509Certs = new X509Certificate[certificates.length];
				for (int i = 0; i < certificates.length; i++) {
					X509Cert = (X509Certificate) certificates[i];
					if (!(isKeyEncipherment(X509Cert.getKeyUsage()))) {
						X509Certs[i] = X509Cert;
					} else {
						if ((isKeyEncipherment(X509Cert.getKeyUsage())) && alias.contains("PKI")) { //$NON-NLS-1$
							X509Certs[i] = X509Cert;
						}
					}
				}

			} else {
				X509Cert = (X509Certificate) this.keyStore.getCertificate(alias);
				if (X509Cert != null) {
					X509Certs = new X509Certificate[1];
					if (isDigitalSignature(X509Cert.getKeyUsage())) {
						X509Certs[0] = X509Cert;
					} else {
						if (alias.contains("PKI")) { //$NON-NLS-1$
							X509Certs[0] = X509Cert;
						}
					}
				}

			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("KeyStoreManager CERTIFICATE CHAIN  COUNT:" + X509Certs.length); //$NON-NLS-1$
		// return X509Certs;
		try {
			X509Certs = new X509Certificate[1];
			X509Certs[0] = (X509Certificate) this.keyStore.getCertificate(alias);
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("CustomKeyManager CERTIFICATE CHAIN  COUNT:" + X509Certs.length); //$NON-NLS-1$
		return X509Certs;
	}

	public String getSelectedFingerprint() {
		return selectedFingerprint;
	}

	public void setSelectedFingerprint(String selectedFingerprint) {
		this.selectedFingerprint = selectedFingerprint;
	}
	@Override
	public PrivateKey getPrivateKey(String alias) {
		// TODO Auto-generated method stub
		PrivateKey privateKey = null;
		try {
			privateKey = (PrivateKey) keyStore.getKey(alias, "".toCharArray()); //$NON-NLS-1$
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
}