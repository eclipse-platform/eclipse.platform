package org.eclipse.update.internal.security;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import java.security.Principal;
import java.security.cert.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.Policy;
import org.eclipse.update.internal.core.UpdateManagerPlugin;
import sun.security.x509.X500Name;

/**
 * Result of the service
 */
public class JarVerificationResult implements IVerificationResult {


	private int resultCode;
	private int verificationCode;
	private Exception resultException;
	private List /*of Certificates[] */
	certificates;
	private CertificatePair[] rootCertificates;
	private CertificatePair foundCertificate; // certificate found in one keystore
	
	private String signerInfo;
	private String verifierInfo;
	private ContentReference contentReference;
	private String text;
	private IFeature feature;
	private boolean featureVerification;
	private boolean alreadySeen;

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

	/*
	 * adds an array of Certificates to the list
	 * force recomputation of root cert
	 */
	public void addCertificates(Certificate[] certs) {
		if (certificates == null)
			certificates = new ArrayList();
		certificates.add(certs);
		rootCertificates = null;
	}

	/*
	 * 
	 */
	private List getCertificates() {
		return certificates;
	}

	/*
	 * Returns the list of root certificates
	 * The list of certificates we received is an array of certificates
	 * we have to determine 
	 * 1) how many chain do we have (a chain stops when verifier of a cert is 
	 * not the signer of the next cert in the list 
	 * 2) build a cert with the leaf signer and the root verifier for each chain
	 */
	public CertificatePair[] getRootCertificates() {
		if (rootCertificates == null) {
			rootCertificates = new CertificatePair[0];			
			List rootCertificatesList = new ArrayList();
			if (certificates != null && certificates.size() > 0) {
				Iterator iter = certificates.iterator();
				while (iter.hasNext()) {

					Certificate[] certs = (Certificate[]) iter.next();
					if (certs != null && certs.length > 0) {

						CertificatePair pair = new CertificatePair();
						pair.setIssuer(certs[0]);

						for (int i = 0; i < certs.length - 1; i++) {
							X509Certificate x509certRoot = (X509Certificate) certs[i];
							X509Certificate x509certIssuer = (X509Certificate) certs[i+1];
							if (!x509certRoot.getIssuerDN().equals(x509certIssuer.getSubjectDN())) {
								pair.setRoot(x509certRoot);
								if (!rootCertificatesList.contains(pair)) {
									rootCertificatesList.add(pair);
								}
								pair = new CertificatePair();
								pair.setIssuer(x509certIssuer);
							}
						}

						// add the latest one
						if (pair != null) {
							pair.setRoot(certs[certs.length - 1]);
							if (!rootCertificatesList.contains(pair)) {
								rootCertificatesList.add(pair);
							}
						}
					}
				}

			}
			
			if (rootCertificatesList.size() > 0) {
				rootCertificates = new CertificatePair[rootCertificatesList.size()];				
				rootCertificatesList.toArray(rootCertificates);
			}
		}
		return rootCertificates;
	}

	/*
	 * 
	 */
	private CertificatePair getFoundCertificate() {
		return foundCertificate;
	}

	/*
	 * 
	 */
	public void setFoundCertificate(CertificatePair foundCertificate) {
		this.foundCertificate = foundCertificate;
	}


	/*
	 * Initializes the signerInfo and the VerifierInfo from the Certificate Pair
	 */
	private void initializeCertificates(){
		X509Certificate certRoot = null;
		X509Certificate certIssuer = null;
		CertificatePair trustedCertificate;
		if (getFoundCertificate() == null) {
			CertificatePair[] certs = getRootCertificates();
			if (certs.length == 0)
				return;
			trustedCertificate = (CertificatePair) certs[0];
		} else {
			trustedCertificate = (CertificatePair) getFoundCertificate();
		}
		certRoot = (X509Certificate) trustedCertificate.getRoot();
		certIssuer = (X509Certificate) trustedCertificate.getIssuer();

		StringBuffer strb = new StringBuffer();
		strb.append(issuerString(certIssuer.getSubjectDN()));
		strb.append("\r\n"); //$NON-NLS-1$
		strb.append(Policy.bind("JarVerificationResult.ValidBetween", dateString(certIssuer.getNotBefore()), dateString(certIssuer.getNotAfter()))); //$NON-NLS-1$
		strb.append(checkValidity(certIssuer));
		signerInfo = strb.toString();
		if (certIssuer != null && !certIssuer.equals(certRoot)) {
			strb = new StringBuffer();	
			strb.append(issuerString(certIssuer.getIssuerDN()));
			strb.append("\r\n"); //$NON-NLS-1$
			strb.append(Policy.bind("JarVerificationResult.ValidBetween", dateString(certRoot.getNotBefore()), dateString(certRoot.getNotAfter()))); //$NON-NLS-1$ 
			strb.append(checkValidity(certRoot));
			verifierInfo = strb.toString();
		}

	}

	/*
	 * Returns a String to show if the certificate is valid
	 */
	private String checkValidity(X509Certificate cert) {

		try {
			cert.checkValidity();
		} catch (CertificateExpiredException e) {
			return ("\r\n" + Policy.bind("JarVerificationResult.ExpiredCertificate")); //$NON-NLS-1$ 
		} catch (CertificateNotYetValidException e) {
			return ("\r\n" + Policy.bind("JarVerificationResult.CertificateNotYetValid")); //$NON-NLS-1$ 
		}
		return ("\r\n" + Policy.bind("JarVerificationResult.CertificateValid")); //$NON-NLS-1$
	}

	/*
	 * Returns the label String from a X50name
	 */
	private String issuerString(Principal principal) {
		try {
			if (principal instanceof X500Name) {
				StringBuffer buf = new StringBuffer();
				X500Name name = (X500Name) principal;
				buf.append((name.getDNQualifier() != null) ? name.getDNQualifier() + ", " : "");
				buf.append(name.getCommonName());
				buf.append((name.getOrganizationalUnit() != null) ? ", " + name.getOrganizationalUnit() : "");
				buf.append((name.getOrganization() != null) ? ", " + name.getOrganization() : "");
				buf.append((name.getLocality() != null) ? ", " + name.getLocality() : "");
				buf.append((name.getCountry() != null) ? ", " + name.getCountry() : "");
				return new String(buf);
			}
		} catch (Exception e) {
			if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_WARNINGS){
				IStatus status = Utilities.newCoreException("Error parsing X500 Certificate",e).getStatus();
				UpdateManagerPlugin.getPlugin().getLog().log(status);
			}
		}
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
	public void setText(String text) {
		this.text = text;
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