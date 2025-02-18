/*******************************************************************************
 * Copyright (c) 2025 Eclipse Platform, Security Group and others.
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
package org.eclipse.core.security.identification;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;

public class FingerprintX509 {
	private static FingerprintX509 INSTANCE;
	private static final char[] HEX= {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
	private static final String cryptoAlg = "SHA-256"; //$NON-NLS-1$
	private FingerprintX509() {}
	public static FingerprintX509 getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new FingerprintX509();
		}
		return INSTANCE;
	}
	
	public String getFingerPrint(Certificate cert, String alg) {
		String fingerPrint=null;
		byte[] encodedCert=null;

		try {
			alg = cryptoAlg;
			encodedCert = cert.getEncoded();
			MessageDigest md = MessageDigest.getInstance(alg);
			md.update(encodedCert);
			byte[] digest = md.digest();
			fingerPrint = getHexValue(digest);

		} catch (CertificateEncodingException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
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
			e.printStackTrace();
		}
		return sb.toString();
	}
}
