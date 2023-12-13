/*******************************************************************************
 * Copyright (c) 2023 Security Team and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Security Team - initial API and implementation
 * <Java Joe> Joe@Schiavone.org
 * yyyymmdd bug      Email and other contact information
 * -------- -------- -----------------------------------------------------------
 *
 *******************************************************************************/
package org.eclipse.core.pki.auth;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

//import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.framework.eventmgr.EventManager;
import org.eclipse.osgi.framework.eventmgr.ListenerQueue;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.eclipse.core.resources.ResourcesPlugin;

public class PKISetup implements BundleActivator {
	public static final String ID = "org.eclipse.core.pki"; //$NON-NLS-1$
	private static PKISetup instance;
	protected static final String USER_HOME = System.getProperty("user.home");
	public static final File PKI_ECLIPSE_DIR = new File(USER_HOME, ".eclipse_pki");
	public static final String PKI_DIR = "eclipse_pki";
	public static final Path pkiHome = Paths.get(PKI_ECLIPSE_DIR.getAbsolutePath() + File.separator + PKI_DIR);
	static boolean isPkcs11Installed = false;
	ListenerQueue<PKISetup, Object, EventManager> queue = null;
	Properties pkiProperties = null;

	public PKISetup() {
		super();
		instance = this;
	}

	@SuppressWarnings("static-access")
	public void Startup() {
		createSigintEclipseDir();
		/*
		 * Check if .pki file exists. If it doesnt, then create one.
		 */
		
	
		/*
		 * NOTE: Initialize pki settings so that NO PKI is set on start up.
		 */
		PKIState.CONTROL.setPKCS11on(false);
		PKIState.CONTROL.setPKCS12on(false);
		
		/*
		 * PKCS11 will be the default certificate store, so check it first.
		 */

		
		
		if (PublicKeySecurity.INSTANCE.isTurnedOn()) {
			System.out.println("PKISetup get PKI TYPE");
			PublicKeySecurity.INSTANCE.getPkiPropertyFile();
			String pkiType = System.getProperty("javax.net.ssl.keyStoreType").trim();
			//System.out.println("PKISetup PKI TYPE:["+pkiType+"]");
			if ( pkiType != null) {
				if (pkiType.equalsIgnoreCase("PKCS12")) {
					//System.out.println("PKISetup PKI TYPE FROM FILE:"+System.getProperty("javax.net.ssl.keyStoreType"));
					PKIState.CONTROL.setPKCS11on(false);
					PKIState.CONTROL.setPKCS12on(true);
					
					//PKCSSelected.setKeystoreformat(KeyStoreFormat.PKCS12);
					
					//AuthenticationPlugin.getDefault()
	    			//	.getPreferenceStore()
	    			//	.setValue(AuthenticationPreferences.PKCS11_CFG_FILE_LOCATION, null );
					
				} else {
					System.out.println("PKISetup PKI TYPE NOT FOUND TO BE EQUAL");
				}
				if ("PKCS11".equalsIgnoreCase(System.getProperty("javax.net.ssl.keyStoreType"))) {
					//if (VendorImplementation.getInstance().isInstalled()) {
						//PKCSSelected.setKeystoreformat(KeyStoreFormat.PKCS11);
						PKIState.CONTROL.setPKCS11on(true);
						PKIState.CONTROL.setPKCS12on(false);
//					} else {
//						// need exception here no PROVIDER
//					}
				}
			}
		} 
	}


	public Object eventRunner(int incoming) {
		final Integer value = Integer.valueOf(incoming);
		return new Runnable() {
			public void run() {
				//System.out.println("PKISetup EVENT runner");
				if (value.equals(EventConstant.DONE.getValue())) {
					//AuthenticationPlugin.getDefault().setUserKeyStore(VendorImplementation.getInstance().getKeyStore());
				} else if (value.equals(EventConstant.CANCEL.getValue())) {
					//VendorImplementation.getInstance().off();
					isPkcs11Installed = false;
					PKIState.CONTROL.setPKCS11on(false);
				
					System.clearProperty("javax.net.ssl.keyStoreType");
					System.clearProperty("javax.net.ssl.keyStoreProvider");
					
					System.out.println("PKISetup - TURNED OFF ALL PKCS11");
					
				} else if (value.equals(EventConstant.SETUP.getValue())) {
					//setupSSLSystemProperties(isPkcs11Installed);
				}
			}
		};
	}

	/**
	 * @see AuthenticationPlugin#setSystemProperties()
	 */
	

	private void installTrustStore() {
		/*
		 * TODO: Create an enum of the IC comms and utilize the correct trust
		 */

		String filename = "cacert";
		File localTrustStore = new File(PKI_ECLIPSE_DIR, filename);

		// System.out.println("Install Truststore - local -> " + localTrustStore);

		//
		// we want to install the new one anyway
		//
		// if(!localTrustStore.exists()) {
		FileChannel fc = null;
		ReadableByteChannel rbc = null;
		FileOutputStream os = null;
		try {
			localTrustStore.createNewFile();
			os = new FileOutputStream(localTrustStore);
			fc = os.getChannel();

			//
			// open file in eclipses configuration directory
			//
			File ConfigurationFile = new File(ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString() + File.separator
					+ "configuration" + File.separator
					+ "cacert");

			InputStream is = new FileInputStream(ConfigurationFile);

			//
			// copy the contents of the eclipse/ cacerts file to our .pki directory
			//
			rbc = Channels.newChannel(is);
			ByteBuffer buffer = ByteBuffer.allocate(1024);
			buffer.clear();
			while (rbc.read(buffer) != -1) {
				buffer.flip();
				fc.write(buffer);
				buffer.compact();
			}

		} catch (IOException e) {
			//LogUtil.logError("Issue writing default trust store to disk.", e);
		} finally {
			if (fc != null) {
				try {
					fc.close();
				} catch (Exception e) {
				}
			}
			if (os != null) {
				try {
					os.close();
				} catch (Exception e) {
				}
			}
			if (rbc != null) {
				try {
					rbc.close();
				} catch (Exception e) {
				}
			}
			// }
		}
		//AuthenticationPlugin.getDefault().getPreferenceStore().setValue(AuthenticationPreferences.TRUST_STORE_LOCATION,
		//		localTrustStore.getAbsolutePath());

	}

	private void createSigintEclipseDir() {
		Lock fsLock = new ReentrantLock();
		fsLock.lock();
		try {
			if (Files.notExists(pkiHome)) {
				Files.createDirectories(pkiHome);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			fsLock.unlock();
		}
	}

	@Override
	public void start(BundleContext context) throws Exception {
		// TODO Auto-generated method stub
		System.out.println("PKISetup PKISetup START");
		Startup();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		// TODO Auto-generated method stub
		
	}
}