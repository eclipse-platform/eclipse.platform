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
package org.eclipse.core.security.managers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import javax.net.ssl.X509KeyManager;

import org.eclipse.core.security.ActivateSecurity;
import org.eclipse.core.security.identification.FingerprintX509;
import org.eclipse.core.security.util.KeyStoreFormat;

public class KeyStoreManager implements X509KeyManager {
	private static KeyStoreManager INSTANCE;
	protected final int KEY_ENCIPHERMENT = 2;
	protected final int DIGITAL_SIGNATURE = 0;
	protected boolean isKeyStoreInitialized = false;
	protected String selectedFingerprint = "NOSET"; //$NON-NLS-1$
	protected KeyStore keyStore = null;
	private KeyStoreManager() {}
	public static KeyStoreManager getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new KeyStoreManager();
		}
		return INSTANCE;
	}

	public KeyStore getKeyStore(String fileLocation, String password, KeyStoreFormat format) {
		InputStream in = null;
		try {

			try {
				Path p = Paths.get(fileLocation);
				in = Files.newInputStream(p);

				keyStore = KeyStore.getInstance(format.getValue());
				keyStore.load(in, password.toCharArray());

				setKeyStoreInitialized(true);
			} catch (FileNotFoundException e) {
				ActivateSecurity.getInstance().log("Configure KeyStore - No File Found:"); //$NON-NLS-1$
			} catch (KeyStoreException e) {
				ActivateSecurity.getInstance().log("Configure KeyStore - Initialize keystore, bad password?"); //$NON-NLS-1$
			} catch (NoSuchAlgorithmException e) {
				ActivateSecurity.getInstance().log("Configure KeyStore - No algorithm found from provider."); //$NON-NLS-1$
			} catch (CertificateException e) {
				ActivateSecurity.getInstance().log("Configure KeyStore - Certificate Error."); //$NON-NLS-1$
			} catch (IOException e) {
				ActivateSecurity.getInstance().log("Configure KeyStore - I/O Error, bad password?"); //$NON-NLS-1$
			}
			if ( keyStore != null) {
				return keyStore;
			}
			return null;
		} finally {
			try {
				in.close();
			} catch (IOException e) {
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
			/*
			 * User may have pressed the cancel button.
			 */
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
						e.printStackTrace();
					}
				}
			}
		} catch (KeyStoreException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		return table;

	}

	public ArrayList<String> getAliases(KeyStore keyStore) {

		ArrayList<String>aliasList = new ArrayList<>();
		try {
			Enumeration<String> aliasesEnum = keyStore.aliases();
			while (aliasesEnum.hasMoreElements())
			{
			      String alias = aliasesEnum.nextElement();
			      X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);
				  try {
					if ( isDigitalSignature(certificate.getKeyUsage()) ) {
						aliasList.add(alias);
			    	 }

				  } catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch (KeyStoreException e) {
			e.printStackTrace();
		} catch (Exception e) {
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
			ActivateSecurity.getInstance().log(message.toString()); //$NON-NLS-1$
		} catch (NoSuchAlgorithmException e) {
			message.append("An unexpected error '"); //$NON-NLS-1$
			message.append(e.getClass().getName());
			message.append("' occurred: "); //$NON-NLS-1$
			message.append(e.getMessage());
			message.append(" Please select a different file and/or "); //$NON-NLS-1$
			message.append("check the logs for more information."); //$NON-NLS-1$
			ActivateSecurity.getInstance().log(message.toString()); //$NON-NLS-1$
		} catch (CertificateException e) {
			message.append("Either your password was incorrect or the "); //$NON-NLS-1$
			message.append("the selected file is corrupt. Please try "); //$NON-NLS-1$
			message.append("a different password or PKCS file."); //$NON-NLS-1$
			ActivateSecurity.getInstance().log(message.toString()); //$NON-NLS-1$
		} catch (IOException e) {
			if (e.getCause().toString().contains("FailedLoginException")) { //$NON-NLS-1$
				message.append("\tYou entered an incorrect password. \n\r"); //$NON-NLS-1$
				message.append("\tPlease check your password and re-enter it. \n\r  \n\r"); //$NON-NLS-1$
				ActivateSecurity.getInstance().log(message.toString()); //$NON-NLS-1$
			} else {

				message.append("Either your password was incorrect or the "); //$NON-NLS-1$
				message.append("selected file is corrupt. Please try "); //$NON-NLS-1$
				message.append("a different password or PKCS file."); //$NON-NLS-1$
				ActivateSecurity.getInstance().log(message.toString()); //$NON-NLS-1$
			}
		}
    	return false;
    }

	@Override
	public String[] getClientAliases(String keyType, Principal[] issuers) {
		return new String[] { chooseClientAlias(null, issuers, null) };
	}

	@Override
	public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
		String message = "Presenting X509 fingerprint:"; //$NON-NLS-1$
		String amessage = " using certificate alias:"; //$NON-NLS-1$
		StringBuilder sb = new StringBuilder();
		String selectedAlias = "testX509";
		String alias = null;
		String fingerprint = null;
		boolean isOK = true;

		try {

			Enumeration<String> aliases = this.keyStore.aliases();
			sb.append(message);
			while (aliases.hasMoreElements()) {
				alias = aliases.nextElement();
				ActivateSecurity.getInstance().log(amessage.toString()+alias); //$NON-NLS-1$
				if (this.getPrivateKey(alias) != null) {
					X509Certificate x509 = (X509Certificate) this.keyStore.getCertificate(alias);
					try {
						x509.checkValidity();
						if (!(isKeyEncipherment(x509.getKeyUsage()))) {
							fingerprint = FingerprintX509.getInstance().getFingerPrint(x509, "SHA-256"); //$NON-NLS-1$
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
						} else {
							ActivateSecurity.getInstance().log(amessage.toString()+alias); //$NON-NLS-1$
							selectedAlias = "testX509";
						}
					} catch (CertificateExpiredException e) {
						ActivateSecurity.getInstance().log("KeyManager: Please remove EXPIRED certificate:" + alias //$NON-NLS-1$
								+ " using your pkcs11 Manager."); //$NON-NLS-1$
					} catch (CertificateNotYetValidException e) {
						ActivateSecurity.getInstance().log("KeyManager: Please check invalid certificate:" + alias //$NON-NLS-1$
								+ " using your pkcs11 Manager."); //$NON-NLS-1$
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (!(isOK)) {
			message = (selectedAlias == null) ? "PKI misconfiguration. Please check " : message + selectedAlias; //$NON-NLS-1$
			ActivateSecurity.getInstance().log("KeyManager: "+ message); //$NON-NLS-1$
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
		return null;
	}

	@Override
	public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
		return null;
	}

	@Override
	public X509Certificate[] getCertificateChain(String alias) {
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
			e.printStackTrace();
		}
		ActivateSecurity.getInstance().log("KeyStoreManager CERTIFICATE CHAIN  COUNT:" + X509Certs.length); //$NON-NLS-1$
		try {
			X509Certs = new X509Certificate[1];
			X509Certs[0] = (X509Certificate) this.keyStore.getCertificate(alias);
		} catch (KeyStoreException e) {
			e.printStackTrace();
		}
		ActivateSecurity.getInstance().log("CustomKeyManager CERTIFICATE CHAIN  COUNT:" + X509Certs.length); //$NON-NLS-1$
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
		PrivateKey privateKey = null;
		try {
			String passwd = System.getProperty("javax.net.ssl.keyStorePassword");
			privateKey = (PrivateKey) keyStore.getKey(alias, passwd.toCharArray()); //$NON-NLS-1$
		} catch (UnrecoverableKeyException e) {
			e.printStackTrace();
		} catch (KeyStoreException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return privateKey;
	}
}