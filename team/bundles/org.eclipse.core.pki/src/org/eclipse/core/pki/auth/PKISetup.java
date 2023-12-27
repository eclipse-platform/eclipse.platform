/*******************************************************************************
 * Copyright (c) 2023 Eclipse Platform, Security Group and others.
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
package org.eclipse.core.pki.auth;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.Properties;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.eclipse.core.pki.util.ConfigureTrust;
import org.eclipse.core.pki.util.KeyStoreFormat;
import org.eclipse.core.pki.util.KeyStoreManager;
import org.eclipse.core.pki.util.LogUtil;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.ServiceCaller;
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
	private static final ServiceCaller<ILog> logger = new ServiceCaller(PKISetup.class, ILog.class);
	// ListenerQueue<PKISetup, Object, EventManager> queue = null;
	protected static KeyStore keyStore = null;
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

	public void log(String message) {

		logger.call(logger -> logger.info(message));
	}

	public void Startup() {

		log("Startup method is now running"); //$NON-NLS-1$

		Optional<String>type = null;


		PKIState.CONTROL.setPKCS11on(false);
		PKIState.CONTROL.setPKCS12on(false);
		/*
		 * First see if parameters were passed into eclipse via the command line -D
		 */
		type = Optional.ofNullable(System.getProperty("javax.net.ssl.keyStoreType")); //$NON-NLS-1$

		if (type.isEmpty()) {
			// System.out.println("PKISetup WAS a -D parameter list passed into command
			// line"); //$NON-NLS-1$

			PKIState.CONTROL.setPKCS11on(false);
			PKIState.CONTROL.setPKCS12on(false);
			if (PublicKeySecurity.INSTANCE.isTurnedOn()) {
				PublicKeySecurity.INSTANCE.getPkiPropertyFile();
			}
		}

		if (IncomingSystemProperty.SETTINGS.checkType()) {
			if (IncomingSystemProperty.SETTINGS.checkKeyStore()) {
				if (PKIState.CONTROL.isPKCS12on()) {
					try {
						keyStore = KeyStoreManager.INSTANCE.getKeyStore(System.getProperty("javax.net.ssl.keyStore"), //$NON-NLS-1$
								System.getProperty("javax.net.ssl.keyStorePassword"), //$NON-NLS-1$ ,
								KeyStoreFormat.valueOf(System.getProperty("javax.net.ssl.keyStoreType"))); //$NON-NLS-1$
					} catch (Exception e) {
						// TODO Auto-generated catch block
						LogUtil.logError("A Truststore and Password are detected.", e); //$NON-NLS-1$
					}
				}
				LogUtil.logError("A Keystore and Password are detected.", null); //$NON-NLS-1$
				if (IncomingSystemProperty.SETTINGS.checkTrustStoreType()) {
					if (IncomingSystemProperty.SETTINGS.checkTrustStore()) {
						LogUtil.logError("A Truststore and Password are detected.", null);  //$NON-NLS-1$
						Optional<X509TrustManager> PKIXtrust = ConfigureTrust.MANAGER.setUp();

						try {
							TrustManager[] tm = new TrustManager[] { ConfigureTrust.MANAGER };
							if (PKIXtrust.isEmpty()) {
								LogUtil.logError("Invalid TrustManager Initialization.", null); //$NON-NLS-1$
							} else {
								SSLContext ctx = SSLContext.getInstance("TLS");//$NON-NLS-1$
								ctx.init(null, tm, null);
							}
						} catch (NoSuchAlgorithmException e) {
							// TODO Auto-generated catch block
							LogUtil.logError("Initialization Error", e); //$NON-NLS-1$
						} catch (KeyManagementException e) {
							// TODO Auto-generated catch block
							LogUtil.logError("Initialization Error", e); //$NON-NLS-1$
						}
					}
				}
			}
		}
	}


	/**
	 * @see AuthenticationPlugin#setSystemProperties()
	 */

	/*
	 * private void installTrustStore() { final String USER_HOME =
	 * System.getProperty("user.home"); //$NON-NLS-1$ final File PKI_ECLIPSE_DIR =
	 * new File(USER_HOME, ".eclipse_pki"); //$NON-NLS-1$ final String PKI_DIR =
	 * "eclipse_pki"; //$NON-NLS-1$ //final Path pkiHome =
	 * Paths.get(PKI_ECLIPSE_DIR.getAbsolutePath() + File.separator + PKI_DIR);
	 *
	 * final Path pkiHome = Paths.get(PKI_ECLIPSE_DIR.getAbsolutePath() +
	 * File.separator + PKI_DIR); createSigintEclipseDir(pkiHome);
	 *
	 * TODO: Create an enum of the IC comms and utilize the correct trust
	 *
	 *
	 * String filename = "cacert"; //$NON-NLS-1$ File localTrustStore = new
	 * File(PKI_ECLIPSE_DIR, filename);
	 *
	 * // System.out.println("Install Truststore - local -> " + localTrustStore);
	 *
	 * // // we want to install the new one anyway // //
	 * if(!localTrustStore.exists()) { FileChannel fc = null; ReadableByteChannel
	 * rbc = null; FileOutputStream os = null; try {
	 * localTrustStore.createNewFile(); os = new FileOutputStream(localTrustStore);
	 * fc = os.getChannel();
	 *
	 *
	 * // //File ConfigurationFile = new
	 * File(ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString() +
	 * File.separator // + "configuration" + File.separator // + "cacert");
	 *
	 * File ConfigurationFile = new File(File.separator + "configuration" +
	 * File.separator //$NON-NLS-1$ + "cacert"); //$NON-NLS-1$ InputStream is = new
	 * FileInputStream(ConfigurationFile);
	 *
	 * // // copy the contents of the eclipse/ cacerts file to our .pki directory //
	 * rbc = Channels.newChannel(is); ByteBuffer buffer = ByteBuffer.allocate(1024);
	 * buffer.clear(); while (rbc.read(buffer) != -1) { buffer.flip();
	 * fc.write(buffer); buffer.compact(); }
	 *
	 * } catch (IOException e) {
	 * //LogUtil.logError("Issue writing default trust store to disk.", e); } // }
	 * //AuthenticationPlugin.getDefault().getPreferenceStore().setValue(
	 * AuthenticationPreferences.TRUST_STORE_LOCATION, //
	 * localTrustStore.getAbsolutePath());
	 *
	 * }
	 */

	/*
	 * private void createSigintEclipseDir(Path pkiHome) { Lock fsLock = new
	 * ReentrantLock(); fsLock.lock(); try { if (Files.notExists(pkiHome)) {
	 * Files.createDirectories(pkiHome); } } catch (IOException e) { // TODO
	 * Auto-generated catch block e.printStackTrace(); } finally { fsLock.unlock();
	 * } }
	 */
}