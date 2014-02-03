/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.verifier;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.signedcontent.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.Messages;
import org.eclipse.update.internal.core.UpdateCore;

/**
 * The JarVerifier will check the integrity of the JAR.
 * If the Jar is signed and the integrity is validated,
 * it will check if one of the certificate of each file
 * is in one of the keystore.
 *
 */

public class CertVerifier extends Verifier {

	private CertVerificationResult result;
	private boolean acceptUnsignedFiles;
	private IProgressMonitor monitor;
	private File jarFile;
	private SignedContentFactory factory;
	private List trustedSignerInfos;

	/*
	 * Default Constructor
	 */
	public CertVerifier(SignedContentFactory factory) {
		this.factory = factory;
		initialize();
	}


	/*
	 * 
	 */
	private void initialize() {
		result = null;
		acceptUnsignedFiles = false;
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

		result = new CertVerificationResult();
		result.setVerificationCode(IVerificationResult.UNKNOWN_ERROR);
		result.setResultException(null);
		result.setFeature(feature);
		result.setContentReference(contentRef);
	}

	/*
	 * @param newMonitor org.eclipse.core.runtime.IProgressMonitor
	 */
	private void setMonitor(IProgressMonitor newMonitor) {
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
	private CertVerificationResult verify(String file, String identifier) {

		try {
			SignedContent verifier = factory.getSignedContent(new File(file));
			// verify integrity
			verifyIntegrity(verifier, identifier);

			//if user already said yes
			result.alreadySeen(alreadyValidated());

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
	 * Verifies the integrity of the JAR
	 */
	private void verifyIntegrity(SignedContent verifier, String identifier) {
		try {
			if (verifier.isSigned()) {
				// If the JAR is signed and invalid then mark as corrupted
				if (hasValidContent(verifier.getSignedEntries())) {
					result.setSignedContent(verifier);
					SignerInfo[] signers = verifier.getSignerInfos();
					for (int i = 0; i < signers.length; i++)
						if (signers[i].isTrusted()) {
							result.setVerificationCode(IVerificationResult.TYPE_ENTRY_SIGNED_RECOGNIZED);
							break;
						}
					if (result.getVerificationCode() != IVerificationResult.TYPE_ENTRY_SIGNED_RECOGNIZED)
						result.setVerificationCode(IVerificationResult.TYPE_ENTRY_SIGNED_UNRECOGNIZED);
				} else
					result.setVerificationCode(IVerificationResult.TYPE_ENTRY_CORRUPTED);
			} else {
				result.setVerificationCode(IVerificationResult.TYPE_ENTRY_NOT_SIGNED);
				return;
			}
		} catch (Exception e) {
			result.setVerificationCode(IVerificationResult.UNKNOWN_ERROR);
			result.setResultException(e);
		}
	}

	private boolean hasValidContent(SignedContentEntry[] signedEntries) {
		try {
			for (int i = 0; i < signedEntries.length; i++)
				signedEntries[i].verify();
		} catch (InvalidContentException e) {
			return false;
		} catch (IOException e) {
			return false;
		}
		return true;
	}


	/*
	 * 
	 */
	private boolean alreadyValidated() {
		int verifyCode = result.getVerificationCode();
		if (verifyCode == IVerificationResult.TYPE_ENTRY_NOT_SIGNED)
			return (acceptUnsignedFiles);
		if (verifyCode == IVerificationResult.UNKNOWN_ERROR)
			return false;
		if (result.getSigners() != null) { //getTrustedCertificates() can't be null as it is lazy initialized
			Iterator iter = getTrustedInfos().iterator();
			SignerInfo[] signers = result.getSigners();

			// check if this is not a user accepted certificate for this feature	
			while (iter.hasNext()) {
				SignerInfo chain = (SignerInfo) iter.next();
				for (int i = 0; i < signers.length; i++)
					if (chain.equals(signers[i]))
						return true;
			}

			// if certificate pair not found in trusted add it for next time
			for (int i = 0; i < signers.length; i++) {
				addTrustedSignerInfo(signers[i]);
			}
		}

		return false;
	}

	/*
	 * 
	 */
	private void addTrustedSignerInfo(SignerInfo signer) {
		if (trustedSignerInfos == null)
			trustedSignerInfos = new ArrayList();
		if (signer != null)
			trustedSignerInfos.add(signer);
	}

	/*
	 * 
	 */
	private List getTrustedInfos() {
		if (trustedSignerInfos == null)
			trustedSignerInfos = new ArrayList();
		return trustedSignerInfos;
	}

	/**
	 * @see IVerifier#setParent(IVerifier)
	 */
	public void setParent(IVerifier parentVerifier) {
		super.setParent(parentVerifier);
		initialize();
	}

}
