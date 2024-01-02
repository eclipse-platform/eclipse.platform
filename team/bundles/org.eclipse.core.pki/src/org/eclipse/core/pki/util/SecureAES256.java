package org.eclipse.core.pki.util;

import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public enum SecureAES256 {
	ENCRYPT;
	private static final int KEY_LENGTH = 256;
	private static final int ITERATION_COUNT = 65536;
	public String encrypt(String strToEncrypt, String secretKey, String salt) {

	    try {

	        SecureRandom secureRandom = new SecureRandom();
	        byte[] iv = new byte[16];
	        secureRandom.nextBytes(iv);
	        IvParameterSpec ivspec = new IvParameterSpec(iv);

			SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256"); //$NON-NLS-1$
	        KeySpec spec = new PBEKeySpec(secretKey.toCharArray(), salt.getBytes(), ITERATION_COUNT, KEY_LENGTH);
	        SecretKey tmp = factory.generateSecret(spec);
			SecretKeySpec secretKeySpec = new SecretKeySpec(tmp.getEncoded(), "AES"); //$NON-NLS-1$

			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); //$NON-NLS-1$
	        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivspec);

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
