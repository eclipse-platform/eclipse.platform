package org.eclipse.ui.internal.util;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;

import javax.security.auth.x500.X500Principal;

//import org.eclipse.core.resources.IMarker;
//import org.eclipse.core.resources.IResource;
//import org.eclipse.core.resources.IWorkspace;
//import org.eclipse.core.resources.ResourcesPlugin;
//import org.eclipse.core.runtime.CoreException;


public enum ExpiredCertCheck {
	INSTANCE;

	// private String expiredDN=null;
	public String getDate(String userKeyStoreLocation, char[] pin ) {
		KeyStore keyStore = getStore(userKeyStoreLocation, pin);
		ExpiredX509CertificateData expiredCert=null;
		ArrayList <ExpiredX509CertificateData>expiredCertList=new ArrayList<>();
		// Date today = Calendar.getInstance().getTime();
		String expirationDate = null;
		try {
			Enumeration<String> aliasesEnum = keyStore.aliases();
			while (aliasesEnum.hasMoreElements()) {
				String alias = aliasesEnum.nextElement();
				//System.out.println("ALIAS:"+ alias);
				X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);
				Date goodUntil = certificate.getNotAfter();
				DateFormat sdf = SimpleDateFormat.getDateInstance();
				expirationDate = sdf.format(goodUntil);
				//System.out.println("EXPIRATION:"+ expirationDate);

				try {
					certificate.checkValidity();
				} catch (CertificateExpiredException e) {
					BigInteger sn = certificate.getSerialNumber();
					X500Principal subject = certificate.getSubjectX500Principal();
					X500Principal issuer = certificate.getIssuerX500Principal();
					expiredCert = new ExpiredX509CertificateData();
					expiredCert.setAlias(alias);
					expiredCert.setCertLocation(userKeyStoreLocation);
					expiredCert.setExpirationDate(expirationDate);
					expiredCert.setDistinguishedName(subject.getName());
					expiredCert.setIssuedBy(issuer.getName());
					expiredCert.setSerialNumber(sn.toString());

					// TODO Auto-generated catch block
					//e.printStackTrace();
					//System.out.println("ExpiredCertCheck found an Expired Cert");
					/*
					 * IStatus status = new Status (IStatus.WARNING,
					 * Platform..getPluginId(),"   EXPIRED CERT..."+expirationDate); ILog logger =
					 * Platform.getLog(this.getClass()); logger.log(status);
					 */
				} catch (CertificateNotYetValidException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				expiredCertList.add(expiredCert);

			}

		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		/*
		 * IWorkspace workspace = ResourcesPlugin.getWorkspace(); IResource resource =
		 * workspace.getRoot();
		 *
		 * try { IMarker[] markers = resource.findMarkers(IMarker.MARKER, true,
		 * IResource.SHALLOW); for (IMarker m : markers) {
		 * //System.out.println("ExpiredCertCheck - MARKER MSG:" +
		 * m.getAttribute(IMarker.MESSAGE)); if
		 * (m.getAttribute(IMarker.MESSAGE).toString().contains("DN")) { //$NON-NLS-1$
		 * expiredDN = new String("EXPIRED"); //$NON-NLS-1$ } }
		 *
		 *
		 * if ( expiredDN == null ) { IMarker marker = null; ExpiredX509CertificateData
		 * data = null; for (Object d : expiredCertList.toArray()) { data =
		 * (ExpiredX509CertificateData) d; marker =
		 * resource.createMarker(IMarker.PROBLEM); marker.setAttribute(IMarker.MESSAGE,
		 * "Expired Certificate in your KeyStore;"); //$NON-NLS-1$
		 * marker.setAttribute(IMarker.LOCATION, data.getCertLocation()); marker =
		 * resource.createMarker(IMarker.PROBLEM); marker.setAttribute(IMarker.MESSAGE,
		 * "DN:" + data.getDistinguishedName()); //$NON-NLS-1$ marker =
		 * resource.createMarker(IMarker.PROBLEM); marker.setAttribute(IMarker.MESSAGE,
		 * "EXPIRATION DATE:" + data.getExpirationDate()); //$NON-NLS-1$ } }
		 *
		 * } catch (CoreException e) { // TODO Auto-generated catch block
		 * e.printStackTrace(); }
		 */
		/*
		 * IWorkbenchWindow workbenchWindow =
		 * PlatformUI.getWorkbench().getActiveWorkbenchWindow(); IWorkbenchPage page =
		 * workbenchWindow.getActivePage(); //IViewPart view =
		 * page.findView("sigint.eclipse.pki.util.pkiview"); try {
		 *
		 * PkiView part = (PkiView) page.showView("sigint.eclipse.pki.util.pkiview",
		 * null, IWorkbenchPage.VIEW_VISIBLE); part.init(expiredCertList);
		 *
		 * } catch (PartInitException e1) { // TODO Auto-generated catch block
		 * e1.printStackTrace(); }
		 */
		return ("EXPIRATION:" + expirationDate); //$NON-NLS-1$
	}
	public KeyStore getStore(String userKeyStoreLocation, char[] pin) {
		KeyStore keyStore=null;
		try {
			FileInputStream fis = new FileInputStream(userKeyStoreLocation);
			keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			keyStore.load(fis, pin);

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			System.out.println("ExpiredCertCheck found an Expired Cert"); //$NON-NLS-1$
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return keyStore;

	}
}
