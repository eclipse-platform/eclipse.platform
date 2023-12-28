/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public enum AEStringEncryptDecrypt {
	BLOCK;

	protected final SecureRandom random = new SecureRandom();
	protected final byte[] pin = "EiEiOIoIoIoOff2W".getBytes(); //$NON-NLS-1$
	protected Cipher cipher;
	protected byte[] ivBytes = new byte[16];
	protected IvParameterSpec iv = null;
	public void configure() {
		// KeyGenerator keyGenerator = null;
		byte[] salt = null;
		byte[] key = null;
		SecretKeySpec keySpec = null;
		try {
			random.nextBytes(ivBytes);
			iv = new IvParameterSpec(ivBytes);
			salt = new byte[16];
			random.nextBytes(salt);
			KeySpec spec = new PBEKeySpec(new String(pin).toCharArray(), salt, 1000000, 256); // AES-256
			SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1"); //$NON-NLS-1$
			key = f.generateSecret(spec).getEncoded();
			keySpec = new SecretKeySpec(key, "AES"); //$NON-NLS-1$

		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// keyGenerator.init(128); // block size is 128bits
		// SecretKey secretKey = keyGenerator.generateKey();
		// SecretKey secretKey = new SecretKeySpec(key, "AES"); //$NON-NLS-1$
		try {
			cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); //$NON-NLS-1$
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		String plainText = "MyAsecretP"; //$NON-NLS-1$
		System.out.println("Plain Text Before Encryption: " + plainText); //$NON-NLS-1$

		String encryptedText = encrypt(plainText, keySpec);
		System.out.println("Encrypted Text After Encryption: " + encryptedText); //$NON-NLS-1$

		String decryptedText = decrypt(encryptedText, keySpec);
		System.out.println("Decrypted Text After Decryption: " + decryptedText); //$NON-NLS-1$
	}

	public String encrypt(String plainText, SecretKeySpec secretKey) {
		byte[] encryptedByte = null;
		byte[] plainTextByte = plainText.getBytes();
		try {
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
			encryptedByte = cipher.doFinal(plainTextByte);
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Base64.Encoder encoder = Base64.getEncoder();
		String encryptedText = encoder.encodeToString(encryptedByte);
		return encryptedText;
	}

	public String decrypt(String encryptedText, SecretKeySpec secretKey) {
		byte[] decryptedByte = null;
		Base64.Decoder decoder = Base64.getDecoder();
		byte[] encryptedTextByte = decoder.decode(encryptedText);
		try {
			cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
			decryptedByte = cipher.doFinal(encryptedTextByte);
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String decryptedText = new String(decryptedByte);
		return decryptedText;
	}

	public static void main(String[] args) {
		AEStringEncryptDecrypt.BLOCK.configure();
	}
}
