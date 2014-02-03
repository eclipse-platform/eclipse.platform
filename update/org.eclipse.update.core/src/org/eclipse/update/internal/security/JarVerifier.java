/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.security;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.update.core.ContentReference;
import org.eclipse.update.core.IFeature;
import org.eclipse.update.core.IVerificationResult;
import org.eclipse.update.core.IVerifier;
import org.eclipse.update.core.InstallMonitor;
import org.eclipse.update.core.JarContentReference;
import org.eclipse.update.core.Utilities;
import org.eclipse.update.core.Verifier;
import org.eclipse.update.internal.core.Messages;
import org.eclipse.update.internal.core.UpdateCore;
import org.eclipse.update.internal.core.connection.ConnectionFactory;

/**
 * The JarVerifier will check the integrity of the JAR.
 * If the Jar is signed and the integrity is validated,
 * it will check if one of the certificate of each file
 * is in one of the keystore.
 *
 */

public class JarVerifier extends Verifier {

	private static final String MANIFEST = "META-INF"; //$NON-NLS-1$

	private JarVerificationResult result;
	private List /*of CertificatePair*/
	trustedCertificates;
	private boolean acceptUnsignedFiles;
	private List /* of KeyStore */
	listOfKeystores;
	private IProgressMonitor monitor;
	private File jarFile;

	private static	byte[] buffer = new byte[8192];
	
	/*
	 * Default Constructor
	 */
	public JarVerifier() {
		initialize();
	}

	/*
	 * Returns the list of the keystores.
	 */
	private List getKeyStores() throws CoreException {
		if (listOfKeystores == null) {
			listOfKeystores = new ArrayList(0);
			KeyStores listOfKeystoreHandles = new KeyStores();
			InputStream in = null;
			KeyStore keystore = null;
			KeystoreHandle handle = null;
			while (listOfKeystoreHandles.hasNext()) {
				try {
					handle = listOfKeystoreHandles.next();
					in = ConnectionFactory.get(handle.getLocation()).getInputStream();
					try {
						keystore = KeyStore.getInstance(handle.getType());
						keystore.load(in, null); // no password
					} catch (NoSuchAlgorithmException e) {
						throw Utilities.newCoreException(NLS.bind(Messages.JarVerifier_UnableToFindEncryption, (new String[] { handle.getLocation().toExternalForm() })), e);
					} catch (CertificateException e) {
						throw Utilities.newCoreException(NLS.bind(Messages.JarVerifier_UnableToLoadCertificate, (new String[] { handle.getLocation().toExternalForm() })), e);
					} catch (KeyStoreException e) {
						throw Utilities.newCoreException(NLS.bind(Messages.JarVerifier_UnableToFindProviderForKeystore, (new String[] { handle.getType() })), e);
					} finally {
						if (in != null) {
							try {
								in.close();
							} catch (IOException e) {
							} // nothing
						}
					} // try loading a keyStore

					// keystore was loaded
					listOfKeystores.add(keystore);
				} catch (IOException e) {
					// nothing... if the keystore doesn't exist, continue	
				}

			} // while all key stores

		}

		return listOfKeystores;
	}

	/*
	 * 
	 */
	private void initialize() {
		result = null;
		trustedCertificates = null;
		acceptUnsignedFiles = false;
		listOfKeystores = null;
	}

	/*
	 * init
	 */
	private void init(IFeature feature, ContentReference contentRef) throws CoreException {
		jarFile = null;
		if (contentRef instanceof JarContentReference) {
			JarContentReference jarReference = (JarContentReference) contentRef;
			try {
				jarFile = jarReference.asFile();
				if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_INSTALL)
					UpdateCore.debug("Attempting to read JAR file:"+jarFile); //$NON-NLS-1$
			
				// # of entries
				if (!jarFile.exists()) throw new IOException();
				JarFile jar = new JarFile(jarFile);
				if (jar !=null){
					try {
						jar.close();
					} catch (IOException ex) {
						// unchecked
					}
				}
			} catch (ZipException e){
				throw Utilities.newCoreException(NLS.bind(Messages.JarVerifier_InvalidJar, (new String[] { jarReference.toString() })), e);				
			} catch (IOException e) {
				throw Utilities.newCoreException(NLS.bind(Messages.JarVerifier_UnableToAccessJar, (new String[] { jarReference.toString() })), e);
			}
		}

		result = new JarVerificationResult();
		result.setVerificationCode(IVerificationResult.UNKNOWN_ERROR);
		result.setResultException(null);
		result.setFeature(feature);
		result.setContentReference(contentRef);
	}

	/*
	 * Returns true if one of the certificate exists in the keystore
	 */
	private boolean existsInKeystore(Certificate cert) throws CoreException {
		try {
			List keyStores = getKeyStores();
			if (!keyStores.isEmpty()) {
				Iterator listOfKeystores = keyStores.iterator();
				while (listOfKeystores.hasNext()) {
					KeyStore keystore = (KeyStore) listOfKeystores.next();

					if (keystore.getCertificateAlias(cert) != null) {
						return true;
					}
				}
			}
		} catch (KeyStoreException e) {
			throw Utilities.newCoreException(Messages.JarVerifier_KeyStoreNotLoaded, e); 
		}
		return false;
	}

	/*
	 * 
	 */
	private List readJarFile(JarFile jarFile, String identifier)
		throws IOException, InterruptedException {
		List list = new ArrayList();

		Enumeration entries = jarFile.entries();
		JarEntry currentEntry = null;
		InputStream in = null;
		if (monitor != null)
			monitor.setTaskName(NLS.bind(Messages.JarVerifier_Verify, (new String[] { identifier == null ? jarFile.getName(): identifier }))); 

		try {
			while (entries.hasMoreElements()) {
				currentEntry = (JarEntry) entries.nextElement();
				list.add(currentEntry);
				in = jarFile.getInputStream(currentEntry);
				while ((in.read(buffer, 0, buffer.length)) != -1) {
					// Security error thrown if tempered
				}
				if (in!=null)
					in.close();
			}
		} catch (IOException e) {
			result.setVerificationCode(IVerificationResult.UNKNOWN_ERROR);
			result.setResultException(e);
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (IOException e1) {
				// ignore
			}
		}

		return list;
	}

	/*
	 * @param newMonitor org.eclipse.core.runtime.IProgressMonitor
	 */
	public void setMonitor(IProgressMonitor newMonitor) {
		monitor = newMonitor;
	}

	/*
	 * @see IVerifier#verify(IFeature,ContentReference,boolean, InstallMonitor)
	 */
	public IVerificationResult verify(
		IFeature feature,
		ContentReference reference,
		boolean isFeatureVerification,
		InstallMonitor monitor)
		throws CoreException {

		if (reference == null)
			return result;

		// if parent knows how to verify, ask the parent first
		if (getParent() != null) {
			IVerificationResult vr =
				getParent().verify(feature, reference, isFeatureVerification, monitor);
			if (vr.getVerificationCode() != IVerificationResult.TYPE_ENTRY_UNRECOGNIZED)
				return vr;
		}

		// the parent couldn't verify
		setMonitor(monitor);
		init(feature, reference);
		result.isFeatureVerification(isFeatureVerification);

		if (jarFile!=null) {
				result = verify(jarFile.getAbsolutePath(), reference.getIdentifier());
		} else {
			result.setVerificationCode(IVerificationResult.TYPE_ENTRY_UNRECOGNIZED);
		}

		return result;
	}

	/*
	 * 
	 */
	private JarVerificationResult verify(String file, String identifier) {

		try {

			// verify integrity
			verifyIntegrity(file, identifier);

			// do not close input stream
			// as verifyIntegrity already did it

			//if user already said yes
			result.alreadySeen(alreadyValidated());

			// verify source certificate
			if (result.getVerificationCode()
				== IVerificationResult.TYPE_ENTRY_SIGNED_UNRECOGNIZED) {
				verifyAuthentication();
			}

			// save the fact the file is not signed, so the user will not be prompted again 
			if (result.getVerificationCode()
				== IVerificationResult.TYPE_ENTRY_NOT_SIGNED) {
				acceptUnsignedFiles = true;
			}

		} catch (Exception e) {
			result.setVerificationCode(IVerificationResult.UNKNOWN_ERROR);
			result.setResultException(e);
		}

		if (monitor != null) {
			monitor.worked(1);
			if (monitor.isCanceled()) {
				result.setVerificationCode(IVerificationResult.VERIFICATION_CANCELLED);
			}
		}

		return result;
	}

	/*
	 * Verifies that each file has at least one certificate
	 * valid in the keystore
	 *
	 * At least one certificate from each Certificate Array
	 * of the Jar file must be found in the known Certificates
	 */
	private void verifyAuthentication() throws CoreException {

		CertificatePair[] entries = result.getRootCertificates();
		boolean certificateFound = false;

		// If all the certificate of an entry are
		// not found in the list of known certifcate
		// the certificate is not trusted by any keystore.
		for (int i = 0; i < entries.length; i++) {
			certificateFound = existsInKeystore(entries[i].getRoot());
			if (certificateFound) {
				result.setVerificationCode(IVerificationResult.TYPE_ENTRY_SIGNED_RECOGNIZED);
				result.setFoundCertificate(entries[i]);
				return;
			}
		}
	}

	/*
	 * Verifies the integrity of the JAR
	 */
	private void verifyIntegrity(String file, String identifier) {

		JarFile jarFile = null;

		try {
			// If the JAR is signed and not valid
			// a security exception will be thrown
			// while reading it
			jarFile = new JarFile(file, true);
			List filesInJar = readJarFile(jarFile, identifier);

			// you have to read all the files once
			// before getting the certificates 
			if (jarFile.getManifest() != null) {
				Iterator iter = filesInJar.iterator();
				boolean certificateFound = false;
				while (iter.hasNext()) {
					JarEntry currentJarEntry = (JarEntry) iter.next();
					Certificate[] certs = currentJarEntry.getCertificates();
					if ((certs != null) && (certs.length != 0)) {
						certificateFound = true;
						result.addCertificates(certs);
					} else {
						String jarEntryName = currentJarEntry.getName();
						if (!jarEntryName.toUpperCase().startsWith(MANIFEST)
							&& !currentJarEntry.isDirectory()) {
							// if the jarEntry is not in MANIFEST, consider the whole file unsigned							
							break;
						}

					}
				}

				if (certificateFound)
					result.setVerificationCode(IVerificationResult.TYPE_ENTRY_SIGNED_UNRECOGNIZED);
				else
					result.setVerificationCode(IVerificationResult.TYPE_ENTRY_NOT_SIGNED);
			} else {
				Exception e = new Exception(NLS.bind(Messages.JarVerifier_InvalidFile, (new String[] { file })));
				result.setResultException(e);
				result.setVerificationCode(IVerificationResult.TYPE_ENTRY_NOT_SIGNED);
				UpdateCore.warn(null,e);
			}
		} catch (SecurityException e) {
			// Jar file is signed
			// but content has changed since signed
			result.setVerificationCode(IVerificationResult.TYPE_ENTRY_CORRUPTED);
		} catch (InterruptedException e) {
			result.setVerificationCode(IVerificationResult.VERIFICATION_CANCELLED);
		} catch (Exception e) {
			result.setVerificationCode(IVerificationResult.UNKNOWN_ERROR);
			result.setResultException(e);
		} finally {
			if (jarFile!=null){
				try {jarFile.close();} catch (IOException e){}
			}
		}

	}

	/*
	 * 
	 */
	private boolean alreadyValidated() {

		if (result.getVerificationCode() == IVerificationResult.TYPE_ENTRY_NOT_SIGNED)
			return (acceptUnsignedFiles);

		if (getTrustedCertificates() != null) {
			Iterator iter = getTrustedCertificates().iterator();
			CertificatePair[] jarPairs = result.getRootCertificates();

			// check if this is not a user accepted certificate for this feature	
			while (iter.hasNext()) {
				CertificatePair trustedCertificate = (CertificatePair) iter.next();
				for (int i = 0; i < jarPairs.length; i++) {
					if (trustedCertificate.equals(jarPairs[i])) {
						return true;
					}
				}
			}

			// if certificate pair not found in trusted add it for next time
			for (int i = 0; i < jarPairs.length; i++) {
				addTrustedCertificate(jarPairs[i]);
			}
		}

		return false;
	}

	/*
	 * 
	 */
	private void addTrustedCertificate(CertificatePair pair) {
		if (trustedCertificates == null)
			trustedCertificates = new ArrayList();
		if (pair != null)
			trustedCertificates.add(pair);
	}

	/*
	 * 
	 */
	private List getTrustedCertificates() {
		if (trustedCertificates == null)
			trustedCertificates = new ArrayList();
		return trustedCertificates;
	}

	/**
	 * @see IVerifier#setParent(IVerifier)
	 */
	public void setParent(IVerifier parentVerifier) {
		super.setParent(parentVerifier);
		initialize();
	}

}
