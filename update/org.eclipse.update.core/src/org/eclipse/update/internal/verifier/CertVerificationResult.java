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

import java.security.Principal;
import java.security.cert.*;
import java.text.DateFormat;
import java.util.Date;
import org.eclipse.osgi.signedcontent.SignedContent;
import org.eclipse.osgi.signedcontent.SignerInfo;
import org.eclipse.osgi.util.NLS;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.Messages;

/**
 * Result of the service
 */
public class CertVerificationResult implements IVerificationResult {


	private int resultCode;
	private int verificationCode;
	private Exception resultException;

	private SignedContent signedContent;
	private String signerInfo;
	private String verifierInfo;
	private ContentReference contentReference;
	private IFeature feature;
	private boolean featureVerification;
	private boolean alreadySeen;

	public CertVerificationResult() {
	}
	
	/*
	 * 
	 */
	public int getResultCode() {
		return resultCode;
	}
	
	/*
	 * 
	 */
	public Exception getVerificationException() {
		return resultException;
	}
	
	/*
	 * 
	 */
	public void setResultCode(int newResultCode) {
		resultCode = newResultCode;
	}
	
	/*
	 * 
	 */
	public void setResultException(Exception newResultException) {
		resultException = newResultException;
	}
	
	/*
	 * 
	 */
	public int getVerificationCode() {
		return verificationCode;
	}

	/*
	 * 
	 */
	public void setVerificationCode(int verificationCode) {
		this.verificationCode = verificationCode;
	}

	void setSignedContent(SignedContent signedContent) {
		this.signedContent = signedContent;
	}

	public SignerInfo[] getSigners() {
		return signedContent.getSignerInfos();
	}

	/*
	 * Initializes the signerInfo and the VerifierInfo from the Certificate Pair
	 */
	private void initializeCertificates(){
		X509Certificate certRoot = null;
		X509Certificate certIssuer = null;
		SignerInfo trustedSigner;
		SignerInfo[] signers = getSigners();
		if (signers.length == 0)
			return;
		trustedSigner = signers[0];
		for (int i = 0; i < signers.length; i++) {
			if (signers[i].isTrusted()) {
				trustedSigner = signers[i];
				break;
			}
		}
		Certificate[] certs = trustedSigner.getCertificateChain();
		if (certs == null || certs.length == 0)
			return;
		certRoot = (X509Certificate) certs[certs.length - 1];
		certIssuer = (X509Certificate) certs[0];

		StringBuffer strb = new StringBuffer();
		strb.append(issuerString(certIssuer.getSubjectDN()));
		strb.append("\r\n"); //$NON-NLS-1$
		strb.append(NLS.bind(Messages.JarVerificationResult_ValidBetween, (new String[] { dateString(certIssuer.getNotBefore()), dateString(certIssuer.getNotAfter()) })));
		strb.append(checkValidity(trustedSigner));
		signerInfo = strb.toString();
		if (certIssuer != null && !certIssuer.equals(certRoot)) {
			strb = new StringBuffer();	
			strb.append(issuerString(certIssuer.getIssuerDN()));
			strb.append("\r\n"); //$NON-NLS-1$
			strb.append(NLS.bind(Messages.JarVerificationResult_ValidBetween, (new String[] { dateString(certRoot.getNotBefore()), dateString(certRoot.getNotAfter()) }))); 
			verifierInfo = strb.toString();
		}

	}

	/*
	 * Returns a String to show if the certificate is valid
	 */
	private String checkValidity(SignerInfo signer) {

		try {
			signedContent.checkValidity(signer);
		} catch (CertificateExpiredException e) {
			return ("\r\n" + Messages.JarVerificationResult_ExpiredCertificate);  //$NON-NLS-1$
		} catch (CertificateNotYetValidException e) {
			return ("\r\n" + Messages.JarVerificationResult_CertificateNotYetValid);  //$NON-NLS-1$
		}
		return ("\r\n" + Messages.JarVerificationResult_CertificateValid);  //$NON-NLS-1$
	}

	/*
	 * Returns the label String from a X50name
	 */
	private String issuerString(Principal principal) {
// 19902
//		try {
//			if (principal instanceof X500Name) {
//				StringBuffer buf = new StringBuffer();
//				X500Name name = (X500Name) principal;
//				buf.append((name.getDNQualifier() != null) ? name.getDNQualifier() + ", " : "");
//				buf.append(name.getCommonName());
//				buf.append((name.getOrganizationalUnit() != null) ? ", " + name.getOrganizationalUnit() : "");
//				buf.append((name.getOrganization() != null) ? ", " + name.getOrganization() : "");
//				buf.append((name.getLocality() != null) ? ", " + name.getLocality() : "");
//				buf.append((name.getCountry() != null) ? ", " + name.getCountry() : "");
//				return new String(buf);
//			}
//		} catch (Exception e) {
//			UpdateCore.warn("Error parsing X500 Certificate",e);
//		}
		return principal.toString();
	}

	/*
	 * 
	 */
	private String dateString(Date date) {
		return DateFormat.getDateInstance().format(date);
	}

	/*
	 *
	 */
	public String getSignerInfo() {
		if (signerInfo==null) initializeCertificates();
		return signerInfo;
	}

	/*
	 *
	 */
	public String getVerifierInfo() {
		if (signerInfo==null) initializeCertificates();		
		return verifierInfo;
	}

	/*
	 *
	 */
	public ContentReference getContentReference() {
		return contentReference;
	}

	/*
	 * 
	 */
	public void setContentReference(ContentReference ref) {
		this.contentReference = ref;
	}


	/*
	 *
	 */
	public IFeature getFeature() {
		return feature;
	}

	/*
	 * 
	 */
	public void setFeature(IFeature feature) {
		this.feature = feature;
	}

	/*
	 * 
	 */
	public String getText() {
		return null;
	}


	/*
	 * 
	 */
	public boolean isFeatureVerification() {
		return featureVerification;
	}
	
	/*
	 * 
	 */
	public void isFeatureVerification(boolean featureVerification) {
		this.featureVerification = featureVerification;
	}

	/*
	 *
	 */
	public boolean alreadySeen() {
		return alreadySeen;
	}

	/*
	 * 
	 */
	public boolean alreadySeen(boolean seen) {
		return this.alreadySeen = seen;
	}

}
