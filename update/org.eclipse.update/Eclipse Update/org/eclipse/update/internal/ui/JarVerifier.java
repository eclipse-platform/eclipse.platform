package org.eclipse.update.internal.ui;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.File;import java.io.FileInputStream;import java.io.IOException;import java.io.InputStream;import java.lang.reflect.InvocationTargetException;import java.security.KeyStore;import java.security.KeyStoreException;import java.security.NoSuchAlgorithmException;import java.security.cert.Certificate;import java.security.cert.CertificateException;import java.util.ArrayList;import java.util.Arrays;import java.util.Collection;import java.util.Enumeration;import java.util.HashSet;import java.util.Iterator;import java.util.List;import java.util.jar.JarEntry;import java.util.jar.JarFile;import java.util.jar.JarInputStream;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.jface.operation.IRunnableWithProgress;import org.eclipse.update.internal.core.UpdateManagerStrings;

/**
 * The JarVerifier will check the integrity of the JAR.
 * If the Jar is signed and the integrity is validated,
 * it will check if one of the certificate of each file
 * is in one of the keystore.
 *
 */

public class JarVerifier {

	/**
	 * Set of certificates of the JAR file
	 */
	private Collection certificateEntries;

	/**
	 * List of certificates of the KeyStores
	 */
	private List listOfKeystoreCertifcates;

	/**
	 * FUTURE: check validity of keystore
	 * default == FALSE 
	 */
	private boolean shouldVerifyKeystore = false;

	/**
	 * FUTURE: check validity of keystore
	 * default == FALSE 
	 */
	private boolean shouldRetrieveKeystoreCertificates = false;

	/**
	 * Number of files in the JarFile
	 */
	private int entries;

	/**
	 * ProgressMonitor during integrity validation
	 */
	private IProgressMonitor monitor;

	/**
	 * JAR File Name: used in the readJarFile.
	 */
	private String jarFileName;

	/**
	 * ResultCode
	 */
	private int resultCode;

	/**
	 * Result Error
	 */
	private Exception resultException;

	//RESULT VALUES
	public static final int NOT_SIGNED = 0;
	public static final int CORRUPTED = 1;
	public static final int INTEGRITY_VERIFIED = 2;
	public static final int SOURCE_VERIFIED = 3;
	public static final int UNKNOWN_ERROR = 4;
	public static final int VERIFICATION_CANCELLED = 5;

	/**
	 * Default Constructor
	 */
	public JarVerifier() {
	}
	/**
	 * 
	 */
	public JarVerifier(IProgressMonitor monitor) {
		this.monitor = monitor;
	}
	/**
	 * Returns the list of certificates of the keystore.
	 *
	 * Can be optimize, within an operation, we only need to get the
	 * list of certificate once.
	 */
	private List getKeyStoreCertificates() {
		if (listOfKeystoreCertifcates == null || shouldRetrieveKeystoreCertificates) {
			listOfKeystoreCertifcates = new ArrayList(0);
			KeyStores listOfKeystores = new KeyStores();
			InputStream in = null;

			try {
				KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
				while (listOfKeystores.hasNext()) {
					try {
						in = listOfKeystores.next().openStream();
						keystore.load(in, null); // no password
					}
					catch (NoSuchAlgorithmException e) {
					}
					catch (CertificateException e) {
					}
					catch (IOException e) {
						// open error message, the keystore is not valid
					}
					finally {
						if (in != null) {
							try {
								in.close();
							}
							catch (IOException e) {
							} // nothing
						}
					} // try loading a keyStore

					// keystore was loaded
					Enumeration enum = keystore.aliases();
					if (enum != null) {
						while (enum.hasMoreElements()) {
							listOfKeystoreCertifcates.add(keystore.getCertificate((String) enum.nextElement()));
						}
					}
				} // while all key stores

			}
			catch (KeyStoreException e) {
				// hum... what to do , what to do ???
				// I cannot instanciate a default keystore...
			}
		}

		return listOfKeystoreCertifcates;
	}
	/**
	 */
	public Exception getResultException() {
		return resultException;
	}
	/**
	 * initialize instance variables
	 */
	private void initializeVariables(File jarFile) throws IOException {
		resultCode = UNKNOWN_ERROR;
		resultException = new Exception(UpdateManagerStrings.getString("S_File_is_not_a_valid_JAR_file"));
		JarFile jar = new JarFile(jarFile);
		entries = jar.size();
		try {
			jar.close();
		}
		catch (java.io.IOException ex) {
			// unchecked
		}
		jarFileName = jarFile.getName();
		certificateEntries = new HashSet();
	}
	/**
	 * Returns true if the 2 collections
	 * have an intersection
	 */
	private boolean intersect(Collection c1, Collection c2) {
		Iterator e = c1.iterator();
		while (e.hasNext())
			if (c2.contains(e.next()))
				return true;

		return false;
	}
	/**
	 * Throws exception or set the resultcode to UNKNOWN_ERROR
	 */
	private List readJarFile(final JarInputStream jis) throws IOException, InterruptedException, InvocationTargetException {
		final List list = new ArrayList(0);

		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				byte[] buffer = new byte[4096];
				JarEntry ent;

				if (monitor != null)
					monitor.beginTask(UpdateManagerStrings.getString("S_Verify") + ": " + jarFileName, entries);
				try {
					while ((ent = jis.getNextJarEntry()) != null) {
						list.add(ent);
						int n = 0;
						if (monitor != null)
							monitor.worked(1);
						while ((n = jis.read(buffer, 0, buffer.length)) != -1) {
						}
					}
				}
				catch (IOException e) {
					resultCode = UNKNOWN_ERROR;
					resultException = e;
				}
				finally {
					if (monitor != null)
						monitor.done();
				}
			}
		};

		op.run(monitor);

		return list;
	}
	/**
	 * 
	 * @param newMonitor org.eclipse.core.runtime.IProgressMonitor
	 */
	public void setMonitor(IProgressMonitor newMonitor) {
		monitor = newMonitor;
	}
	/**
	 * 
	 * [future]
	 */
	public void shouldRetrieveCertificate(boolean value) {
		shouldRetrieveKeystoreCertificates = value;
	}
	/**
	 * 
	 * [future]
	 */
	public void shouldVerifyKeystore(boolean value) {
		shouldVerifyKeystore = value;
	}
	/**
	 * Verifies integrity and the validity of a valid
	 * URL representing a JAR file
	 * the possible results are:
	 *
	 * result == NOT_SIGNED	 				if the jar file is not signed.
	 * result == INTEGRITY_VERIFIED		 	if the Jar file has not been
	 *										modified since it has been
	 *										signed
	 * result == CORRUPTED 					if the Jar file has been changed
	 * 										since it has been signed.
	 * result == SOURCE_VERIFIED	 		if all the files in the Jar
	 *										have a certificate that is
	 * 										present in the keystore
	 * result == UNKNOWN_ERROR		 		an occured during process, do
	 *                                      not install.
	 * result == VERIFICATION.CANCELLED     if process was cancelled, do
	 *										not install.
	 * @return int
	 */
	public int verify(File jarFile) {

		try {
			// new verification, clean instance variables
			initializeVariables(jarFile);

			// verify integrity
			verifyIntegrity(jarFile);

			// do not close input stream
			// as verifyIntegrity already did it

			// verify source certificate
			if (resultCode == INTEGRITY_VERIFIED)
				verifyAuthentication();

		}
		catch (Exception e) {
			resultCode = UNKNOWN_ERROR;
			resultException = e;
		}

		return resultCode;
	}
	/**
	 * Verifies that each file has at least one certificate
	 * valid in the keystore
	 *
	 * At least one certificate from each Certificate Array
	 * of the Jar file must be found in the known Certificates
	 */
	private void verifyAuthentication() {

		if (!getKeyStoreCertificates().isEmpty()) {
			Iterator entries = certificateEntries.iterator();
			boolean certificateFound = true;

			// If all the cartificate of an entry are
			// not found in the list of known certifcate
			// we exit the loop.
			while (entries.hasNext() && certificateFound) {
				List certs = (List) entries.next();
				certificateFound = intersect(getKeyStoreCertificates(), certs);
			}
			if (certificateFound)
				resultCode = SOURCE_VERIFIED;
			// else installCertificates()[future] 
		}

	}
	/**
	 * Verifies the integrity of the JAR
	 */
	private void verifyIntegrity(File jarFile) {

		JarInputStream jis = null;

		try {
			// If the JAR is signed and not valid
			// a security exception will be thrown
			// while reading it
			jis = new JarInputStream(new FileInputStream(jarFile), true);
			List filesInJar = readJarFile(jis);

			// you have to read all the files once
			// before getting the certificates 
			if (jis.getManifest() != null) {
				Iterator iter = filesInJar.iterator();
				boolean certificateFound = false;
				while (iter.hasNext()) {
					Certificate[] certs = ((JarEntry) iter.next()).getCertificates();
					if ((certs != null) && (certs.length != 0)) {
						certificateFound = true;
						certificateEntries.add(Arrays.asList(certs));
					};
				}

				if (certificateFound)
					resultCode = INTEGRITY_VERIFIED;
				else
					resultCode = NOT_SIGNED;
			}
		}
		catch (SecurityException e) {
			// Jar file is signed
			// but content has changed since signed
			resultCode = CORRUPTED;
		}
		catch (InterruptedException e) {
			resultCode = VERIFICATION_CANCELLED;
		}
		catch (Exception e) {
			resultCode = UNKNOWN_ERROR;
			resultException = e;
		}
		finally {
			if (jis != null) {
				try {
					jis.close();
				}
				catch (IOException e) {
				} // nothing
			}
		}

	}
	/**
	 * [future]
	 */
	private boolean verifyIntegrityOfKeyStore() {
		return shouldVerifyKeystore;
	}
}