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
//import org.eclipse.osgi.framework.eventmgr.EventManager;
//import org.eclipse.osgi.framework.eventmgr.ListenerQueue;
import org.eclipse.ui.IStartup;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class PKISetup implements BundleActivator, IStartup {
	public static final String ID = "org.eclipse.core.pki"; //$NON-NLS-1$
	private static PKISetup instance;
	static boolean isPkcs11Installed = false;
	//ListenerQueue<PKISetup, Object, EventManager> queue = null;
	Properties pkiProperties = null;

	public PKISetup() {
		super();
		setInstance(this);
	}

	@Override
	public void start(BundleContext context) throws Exception {
		// TODO Auto-generated method stub
		System.out.println("PKISetup PKISetup ------------------- START"); //$NON-NLS-1$
		Startup();
	}

	@Override
	public void earlyStartup() {
		// TODO Auto-generated method stub
		System.out.println("PKISetup PKISetup -------------------early START"); //$NON-NLS-1$
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		// TODO Auto-generated method stub

	}
	public static PKISetup getInstance() {
		return instance;
	}

	public static void setInstance(PKISetup instance) {
		PKISetup.instance = instance;
	}

	public void Startup() {

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
			System.out.println("PKISetup get PKI TYPE"); //$NON-NLS-1$
			PublicKeySecurity.INSTANCE.getPkiPropertyFile();

		}
		installTrustStore();
	}


	/**
	 * @see AuthenticationPlugin#setSystemProperties()
	 */


	private void installTrustStore() {
		final String USER_HOME = System.getProperty("user.home"); //$NON-NLS-1$
		final File PKI_ECLIPSE_DIR = new File(USER_HOME, ".eclipse_pki"); //$NON-NLS-1$
		final String PKI_DIR = "eclipse_pki"; //$NON-NLS-1$
		//final Path pkiHome = Paths.get(PKI_ECLIPSE_DIR.getAbsolutePath() + File.separator + PKI_DIR);

		final Path pkiHome = Paths.get(PKI_ECLIPSE_DIR.getAbsolutePath() + File.separator + PKI_DIR);
		createSigintEclipseDir(pkiHome);
		/*
		 * TODO: Create an enum of the IC comms and utilize the correct trust
		 */

		String filename = "cacert"; //$NON-NLS-1$
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
			//File ConfigurationFile = new File(ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString() + File.separator
			//		+ "configuration" + File.separator
			//		+ "cacert");

			File ConfigurationFile = new File(File.separator
					+ "configuration" + File.separator //$NON-NLS-1$
					+ "cacert"); //$NON-NLS-1$
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
		}
			// }
		//AuthenticationPlugin.getDefault().getPreferenceStore().setValue(AuthenticationPreferences.TRUST_STORE_LOCATION,
		//		localTrustStore.getAbsolutePath());

	}

	private void createSigintEclipseDir(Path pkiHome) {
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
}