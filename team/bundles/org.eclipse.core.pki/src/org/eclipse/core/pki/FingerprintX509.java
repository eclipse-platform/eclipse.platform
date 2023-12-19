package org.eclipse.core.pki;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;

public enum FingerprintX509 {
	INSTANCE;
	private static final char[] HEX= {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};

	// private static final String ALG="MD5"; //$NON-NLS-1$
	public String getFingerPrint(Certificate cert, String alg) {
		String fingerPrint=null;
		byte[] encodedCert=null;

		try {
			encodedCert = cert.getEncoded();
			MessageDigest md = MessageDigest.getInstance(alg);
			md.update(encodedCert);
			byte[] digest = md.digest();
			fingerPrint = getHexValue(digest);

		} catch (CertificateEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//System.err.println("FingerprintX509 -----------------------------------PRINT:"+fingerPrint);
		return fingerPrint;
	}
	protected String getHexValue( byte[] bytes ) {
		StringBuffer sb = new StringBuffer(bytes.length * 2);
		try {
			for( int i=0; i < bytes.length; i++) {
				sb.append(HEX[(bytes[i] & 0xf0) >> 4 ]);
				sb.append(HEX[bytes[i] & 0xf]);
				if ( i < bytes.length-1) {
					sb.append(":"); //$NON-NLS-1$
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return sb.toString();
	}
}
