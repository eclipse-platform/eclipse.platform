/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.security;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Security;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.update.internal.core.UpdateCore;
import org.eclipse.update.internal.core.connection.ConnectionFactory;
/**
 * Class to manage the different KeyStores we should
 * check for certificates of Signed JAR
 */
public class KeyStores {


	/**
	 * java.policy files properties of the java.security file
	 */
	private static final String JAVA_POLICY_URL = "policy.url."; //$NON-NLS-1$

	/**
	 * Default keystore type in java.security file
	 */	
    private static final String DEFAULT_KEYSTORE_TYPE = "keystore.type"; //$NON-NLS-1$

	/**
	 * List of KeystoreHandle pointing of valid KeyStores
	 * the URL of the KeystoreHandle is not tested yet...
	 */
	private List /* of KeystoreHandle */ listOfKeyStores;

	/**
	 * Iterator
	 */
	private Iterator iterator;
	/**
	 * KeyStores constructor comment.
	 */
	public KeyStores() {
		super();
		initializeDefaultKeyStores();
	}
	/**
	 * 
	 */
	private Iterator getIterator() {
		if (iterator == null)
			iterator = listOfKeyStores.iterator();
		return iterator;
	}
	/**
	 * returns trus if there is more Keystores in the list
	 */
	public boolean hasNext() {
		return getIterator().hasNext();
	}
	/**
	 * populate the list of Keystores
	 * should be done with Dialog with Cancel/Skip button if
	 * the connection to the URL is down...
	 */
	private void initializeDefaultKeyStores() {

		listOfKeyStores = new ArrayList(5);

		// get JRE cacerts
		try {
			URL url = new URL("file", null, 0, System.getProperty("java.home") + File.separator + "lib" + File.separator + "security" + File.separator + "cacerts");  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			listOfKeyStores.add(new KeystoreHandle(url,Security.getProperty(DEFAULT_KEYSTORE_TYPE)));
		}
		catch (MalformedURLException e) {
			// should not happen, hardcoded...
		}

		// get java.home .keystore
		try {
			URL url = new URL("file", null, 0, System.getProperty("user.home") + File.separator + ".keystore"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			listOfKeyStores.add(new KeystoreHandle(url,Security.getProperty(DEFAULT_KEYSTORE_TYPE)));
		}
		catch (MalformedURLException e) {
			// should not happen, hardcoded...
		}

		// get KeyStores from policy files...
		int index = 1;
		String java_policy = Security.getProperty(JAVA_POLICY_URL+index);
		while (java_policy!=null){
			// retrieve keystore url from java.policy
			// also retrieve keystore type
			KeystoreHandle keystore = getKeystoreFromLocation(java_policy);
			if (keystore!=null){
				listOfKeyStores.add(keystore);
			}
			index++;	
			java_policy = Security.getProperty(JAVA_POLICY_URL+index);			
		}

	}
	/**
	 * returns the URL for the Next KeystoreHandle
	 */
	public KeystoreHandle next() {
		return (KeystoreHandle) getIterator().next();
	}
	
	/**
	 * retrieve the keystore from java.policy file
	 */
	private KeystoreHandle getKeystoreFromLocation(String location){
		
		InputStream in = null;
		char[] buff = new char[4096];
		
		
		int indexOf$ = location.indexOf("${"); //$NON-NLS-1$
		int indexOfCurly = location.indexOf('}',indexOf$);
		if (indexOf$!=-1 && indexOfCurly!=-1){
			String prop = System.getProperty(location.substring(indexOf$+2,indexOfCurly));
			String location2 = location.substring(0,indexOf$);
			location2 += prop;
			location2 += location.substring(indexOfCurly+1);
			location = location2;
		}
		
		
		try {
			URL url = new URL(location);
			in = ConnectionFactory.get(url).getInputStream();
			Reader reader = new InputStreamReader(in);
			int result = reader.read(buff);
			StringBuffer contentBuff = new StringBuffer();
			while (result!=-1){
				contentBuff.append(buff,0,result);
				result = reader.read(buff);				
			}

			if (contentBuff.length()>0){			
				String content = new String(contentBuff);
				int indexOfKeystore = content.indexOf("keystore"); //$NON-NLS-1$
				if (indexOfKeystore != -1){
					int indexOfSemiColumn = content.indexOf(';',indexOfKeystore);
					return getKeystoreFromString(content.substring(indexOfKeystore,indexOfSemiColumn),url);
				}
			}
		} catch (MalformedURLException e){
			log(e);
		} catch (IOException e){
			// url.openStream, reader.read (x2)
			// only log, the keystore may not exist
			log(e);	
		} finally {
			if (in!=null){
				try {
					in.close();
				} catch (IOException e){}
			}
		}
		return null;
	}
	
	/**
	 * retrieve the keystore from java.policy file
	 */
	private KeystoreHandle getKeystoreFromString(String content,URL rootURL){
		KeystoreHandle handle = null;
		String keyStoreType = Security.getProperty(DEFAULT_KEYSTORE_TYPE);
		
		
		int indexOfSpace = content.indexOf(' ');
		if (indexOfSpace==-1) return null;
		
		int secondSpace = content.lastIndexOf(',');
		if (secondSpace==-1) {
			secondSpace = content.length();
		} else {
			keyStoreType = content.substring(secondSpace+1,content.length()).trim();
		}
		
		URL url = null;
		try {
			url = new URL(content.substring(indexOfSpace,secondSpace));
		} catch (MalformedURLException e){
			log(e);
			// the url maybe relative
			try {
			url = new URL(rootURL,content.substring(indexOfSpace,secondSpace));				
			} catch (MalformedURLException e1){
				log(e1);			
			}
		}

		if (url!=null)
			handle = new KeystoreHandle(url,keyStoreType);				
			
		return handle;
	}	
	
	private void log(Exception e){
		UpdateCore.warn("Cannot retrieve a KeyStore",e); //$NON-NLS-1$
	}
}
