/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.pki.util;

import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public enum SecureGCM {
	ENCRYPT;

	private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding"; //$NON-NLS-1$
	private static final String FACTORY_INSTANCE = "PBKDF2WithHmacSHA512"; //$NON-NLS-1$
	private static final int GCM_IV_LENGTH = 12;
	private static final int KEY_LENGTH = 256;
	private static final int ITERATION_COUNT = 65536;

	public String encrypt(String strToEncrypt, String secretKey, String salt) {
		try {
			SecureRandom secureRandom = SecureRandom.getInstanceStrong();
			byte[] iv = new byte[GCM_IV_LENGTH];
			secureRandom.nextBytes(iv);
			GCMParameterSpec spec = new GCMParameterSpec(KEY_LENGTH / 2, iv);
			SecretKeyFactory factory = SecretKeyFactory.getInstance(FACTORY_INSTANCE);
			KeySpec skey = new PBEKeySpec(secretKey.toCharArray(), salt.getBytes(), ITERATION_COUNT, KEY_LENGTH);
			SecretKey tmp = factory.generateSecret(skey);
			SecretKeySpec secretKeySpec = new SecretKeySpec(tmp.getEncoded(), "AES"); //$NON-NLS-1$
			Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, spec);
			byte[] cipherText = cipher.doFinal(strToEncrypt.getBytes("UTF-8")); //$NON-NLS-1$
			byte[] encryptedData = new byte[iv.length + cipherText.length];
			System.arraycopy(iv, 0, encryptedData, 0, iv.length);
			System.arraycopy(cipherText, 0, encryptedData, iv.length, cipherText.length);
			return Base64.getEncoder().encodeToString(encryptedData);
		} catch (Exception e) {
			// Handle the exception properly
			e.printStackTrace();
			return null;
		}
	}
}
