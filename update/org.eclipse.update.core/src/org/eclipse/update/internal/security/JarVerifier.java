package org.eclipse.update.internal.security;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipException;

import org.eclipse.core.runtime.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.Policy;
import org.eclipse.update.internal.core.UpdateManagerPlugin;

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
	private int entries;
	private IProgressMonitor monitor;
	private String jarFileName;
	private File jarFile;

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
					in = UpdateManagerPlugin.getPlugin().get(handle.getLocation()).getInputStream();;
					try {
						keystore = KeyStore.getInstance(handle.getType());
						keystore.load(in, null); // no password
					} catch (NoSuchAlgorithmException e) {
						throw Utilities.newCoreException(Policy.bind("JarVerifier.UnableToFindEncryption", handle.getLocation().toExternalForm()), e); //$NON-NLS-1$
					} catch (CertificateException e) {
						throw Utilities.newCoreException(Policy.bind("JarVerifier.UnableToLoadCertificate", handle.getLocation().toExternalForm()), e); //$NON-NLS-1$
					} catch (KeyStoreException e) {
						throw Utilities.newCoreException(Policy.bind("JarVerifier.UnableToFindProviderForKeystore", handle.getType()), e); //$NON-NLS-1$
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
				if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_INSTALL)
					UpdateManagerPlugin.debug("Attempting to read JAR file:"+jarFile);
			
				// # of entries
				if (!jarFile.exists()) throw new IOException();
				JarFile jar = new JarFile(jarFile);
				entries = jar.size();
				try {
					jar.close();
				} catch (IOException ex) {
					// unchecked
				}
				jarFileName = jarFile.getName();
			} catch (ZipException e){
				throw Utilities.newCoreException(Policy.bind("JarVerifier.InvalidJar", jarReference.toString()), e); //$NON-NLS-1$				
			} catch (IOException e) {
				throw Utilities.newCoreException(Policy.bind("JarVerifier.UnableToAccessJar", jarReference.toString()), e); //$NON-NLS-1$
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
			throw Utilities.newCoreException(Policy.bind("JarVerifier.KeyStoreNotLoaded"), e); //$NON-NLS-1$
		}
		return false;
	}

	/*
	 * 
	 */
	private List readJarFile(JarFile jarFile)
		throws IOException, InterruptedException {
		List list = new ArrayList();
		byte[] buffer = new byte[4096];
		Enumeration entries = jarFile.entries();
		JarEntry currentEntry = null;
		InputStream in = null;
		if (monitor != null)
			monitor.setTaskName(Policy.bind("JarVerifier.Verify", jarFile.getName())); //$NON-NLS-1$ 

		try {
			while (entries.hasMoreElements()) {
				currentEntry = (JarEntry) entries.nextElement();
				list.add(currentEntry);
				in = jarFile.getInputStream(currentEntry);
				while ((in.read(buffer, 0, buffer.length)) != -1) {
					// Security error thrown if tempered
				}
				in.close();
			}
		} catch (IOException e) {
			result.setVerificationCode(IVerificationResult.UNKNOWN_ERROR);
			result.setResultException(e);
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
				result = verify(jarFile.getAbsolutePath());
		} else {
			result.setVerificationCode(IVerificationResult.TYPE_ENTRY_UNRECOGNIZED);
		}

		return result;
	}

	/*
	 * 
	 */
	private JarVerificationResult verify(String file) {

		try {

			// verify integrity
			verifyIntegrity(file);

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
	private void verifyIntegrity(String file) {

		JarFile jarFile = null;

		try {
			// If the JAR is signed and not valid
			// a security exception will be thrown
			// while reading it
			jarFile = new JarFile(file, true);
			List filesInJar = readJarFile(jarFile);
			jarFile.close();

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
				Exception e = new Exception(Policy.bind("JarVerifier.InvalidFile", file)); //$NON-NLS-1$
				result.setResultException(e);
				result.setVerificationCode(IVerificationResult.TYPE_ENTRY_NOT_SIGNED);
				UpdateManagerPlugin.warn(null,e);
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