package org.eclipse.core.pki.util;

import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public enum NormalizeAES256 {
	DECRYPT;
	private static final int KEY_LENGTH = 256;
	private static final int ITERATION_COUNT = 65536;
	public String decrypt(String strToDecrypt, String secretKey, String salt) {

		try {

			byte[] encryptedData = Base64.getDecoder().decode(strToDecrypt);
			byte[] iv = new byte[16];
			System.arraycopy(encryptedData, 0, iv, 0, iv.length);
			IvParameterSpec ivspec = new IvParameterSpec(iv);

			SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256"); //$NON-NLS-1$
			KeySpec spec = new PBEKeySpec(secretKey.toCharArray(), salt.getBytes(), ITERATION_COUNT, KEY_LENGTH);
			SecretKey tmp = factory.generateSecret(spec);
			SecretKeySpec secretKeySpec = new SecretKeySpec(tmp.getEncoded(), "AES"); //$NON-NLS-1$

			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); //$NON-NLS-1$
			cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivspec);

			byte[] cipherText = new byte[encryptedData.length - 16];
			System.arraycopy(encryptedData, 16, cipherText, 0, cipherText.length);

			byte[] decryptedText = cipher.doFinal(cipherText);
			return new String(decryptedText, "UTF-8"); //$NON-NLS-1$
		} catch (Exception e) {
			// Handle the exception properly
			e.printStackTrace();
			return null;
		}
	}
}
