/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package org.eclipse.core.security.encryption;

import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class NormalizeGCM {
	private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding"; //$NON-NLS-1$
	private static final String FACTORY_INSTANCE = "PBKDF2WithHmacSHA512"; //$NON-NLS-1$
	private static final int GCM_IV_LENGTH = 12;
	private static final int KEY_LENGTH = 256;
	private static final int ITERATION_COUNT = 65536;
	private static NormalizeGCM DECRYPT;
	private NormalizeGCM() {}
	public static NormalizeGCM getInstance() {
		if (DECRYPT == null) {
			DECRYPT = new NormalizeGCM();
		}
		return DECRYPT;
	}
	public String decrypt(String strToDecrypt, String secretKey, String salt) {
		try {
			byte[] encryptedData = Base64.getDecoder().decode(strToDecrypt);
			byte[] initVector = Arrays.copyOfRange(encryptedData, 0, GCM_IV_LENGTH);
			GCMParameterSpec spec = new GCMParameterSpec(KEY_LENGTH / 2, initVector);
			SecretKeyFactory factory = SecretKeyFactory.getInstance(FACTORY_INSTANCE);
			KeySpec keySpec = new PBEKeySpec(secretKey.toCharArray(), salt.getBytes(), ITERATION_COUNT, KEY_LENGTH);
			SecretKey tmp = factory.generateSecret(keySpec);
			SecretKeySpec secretKeySpec = new SecretKeySpec(tmp.getEncoded(), "AES"); //$NON-NLS-1$
			Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, spec);
			byte[] decryptedText = cipher.doFinal(encryptedData, GCM_IV_LENGTH, encryptedData.length - GCM_IV_LENGTH);
			return new String(decryptedText, "UTF-8"); //$NON-NLS-1$
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
