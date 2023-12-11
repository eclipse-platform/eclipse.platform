package org.eclipse.pki.auth;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.framework.eventmgr.EventManager;
import org.eclipse.osgi.framework.eventmgr.ListenerQueue;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IStartup;
import org.eclipse.core.pki.AuthenticationBase;
/*import org.eclipse.ui.pki.AuthenticationPlugin;
import org.eclipse.ui.pki.PKCSpick;
import org.eclipse.ui.pki.PKISecureStorage;
import org.eclipse.ui.pki.util.KeyStoreFormat;
import org.eclipse.ui.pki.util.LogUtil;
import org.eclipse.ui.pki.wizard.TrustStoreSecureStorage;
import org.eclipse.pki.pkcs.PublicKeySecurity;
import org.eclipse.pki.pkcs.VendorImplementation;*/
import org.eclipse.pki.exception.UserCanceledException;
/*import org.eclipse.pki.pkiselection.PKCSSelected;
import org.eclipse.pki.preferences.AuthenticationPreferences;*/

public class PKISetup implements IStartup {
	protected static final String USER_HOME = System.getProperty("user.home");
	public static final File PKI_ECLIPSE_DIR = new File(USER_HOME, ".eclipse_pki");
	public static final String PKI_DIR = "eclipse_pki";
	public static final Path pkiHome = Paths.get(PKI_ECLIPSE_DIR.getAbsolutePath() + File.separator + PKI_DIR);
	static boolean isPkcs11Installed = false;
	ListenerQueue<PKISetup, Object, EventManager> queue = null;
	Properties pkiProperties = null;

	public PKISetup() {
	}

	@Override
	public void earlyStartup() {
		// TODO Auto-generated method stub
		//System.out.println("PKIController EARLY Startup");
		Startup();
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
			//System.out.println("PKIController get PKI TYPE");
			PublicKeySecurity.INSTANCE.getPkiPropertyFile();
			String pkiType = System.getProperty("javax.net.ssl.keyStoreType").trim();
			//System.out.println("PKIController PKI TYPE:["+pkiType+"]");
			if ( pkiType != null) {
				if (pkiType.equalsIgnoreCase("PKCS12")) {
					//System.out.println("PKIController PKI TYPE FROM FILE:"+System.getProperty("javax.net.ssl.keyStoreType"));
					PKIState.CONTROL.setPKCS11on(false);
					PKIState.CONTROL.setPKCS12on(true);
					
					PKCSSelected.setKeystoreformat(KeyStoreFormat.PKCS12);
					
					//AuthenticationPlugin.getDefault()
	    			//	.getPreferenceStore()
	    			//	.setValue(AuthenticationPreferences.PKCS11_CFG_FILE_LOCATION, null );
					
				} else {
					System.out.println("PKIController PKI TYPE NOT FOUND TO BE EQUAL");
				}
				if ("PKCS11".equalsIgnoreCase(System.getProperty("javax.net.ssl.keyStoreType"))) {
					if (VendorImplementation.getInstance().isInstalled()) {
						PKCSSelected.setKeystoreformat(KeyStoreFormat.PKCS11);
						PKIState.CONTROL.setPKCS11on(true);
						PKIState.CONTROL.setPKCS12on(false);
					} else {
						// need exception here no PROVIDER
					}
				}
			}
		} 
	}


	public Object eventRunner(int incoming) {
		final Integer value = Integer.valueOf(incoming);
		return new Runnable() {
			public void run() {
				//System.out.println("PKIController EVENT runner");
				if (value.equals(EventConstant.DONE.getValue())) {
					AuthenticationPlugin.getDefault().setUserKeyStore(VendorImplementation.getInstance().getKeyStore());
				} else if (value.equals(EventConstant.CANCEL.getValue())) {
					VendorImplementation.getInstance().off();
					isPkcs11Installed = false;
					PKIState.CONTROL.setPKCS11on(false);
				
					System.clearProperty("javax.net.ssl.keyStoreType");
					System.clearProperty("javax.net.ssl.keyStoreProvider");
					
					System.out.println("PKIController - TURNED OFF ALL PKCS11");
					
				} else if (value.equals(EventConstant.SETUP.getValue())) {
					setupSSLSystemProperties(isPkcs11Installed);
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
			File ConfigurationFile = new File(Platform.getInstallLocation().getURL().getPath() + File.separator
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
			LogUtil.logError("Issue writing default trust store to disk.", e);
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
}