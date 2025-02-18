/*******************************************************************************
 * Copyright (c) 2025 Eclipse Platform, Security Group and others.
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
package org.eclipse.core.security.incoming;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.security.ActivateSecurity;
import org.eclipse.core.security.encryption.NormalizeGCM;
import org.eclipse.core.security.encryption.SecureGCM;
import org.eclipse.core.security.encryption.SecurityOpRequest;
import org.eclipse.core.security.identification.PkiPasswordGrabberWidget;
import org.eclipse.core.security.identification.PublishPasswordUpdate;

public class SecurityFileSnapshot {
	
	private static SecurityFileSnapshot INSTANCE;
	Path pkiFile = null;
	Path userM2Home = null;
	Path userHome = null;
	Path userDotEclipseHome = null;
	Properties originalProperties = new Properties();
	public static final String DotEclipse = ".eclipse";
	public static final String USER_HOME = System.getProperty("user.home"); //$NON-NLS-1$
	
	private SecurityFileSnapshot() {}
	
	public static SecurityFileSnapshot getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new SecurityFileSnapshot();
		}
		return INSTANCE;
	}
	public boolean image() {
		/*
		 * CHeck if .pki file is present.
		 */
		try {
			Optional<Boolean> eclipseHome = Optional.ofNullable(Files.exists(Paths.get(USER_HOME))); // $NON-NLS-1$
			if (!(eclipseHome.isEmpty())) {
				if (Files.exists(Paths.get(USER_HOME + FileSystems.getDefault().getSeparator() + DotEclipse
						+ FileSystems.getDefault().getSeparator() + ".pki"))) {

					userDotEclipseHome = Paths.get(USER_HOME + FileSystems.getDefault().getSeparator() + DotEclipse
							+ FileSystems.getDefault().getSeparator() + ".pki");
					if (!DotPkiPropertiesRequired.getInstance().testFile(userDotEclipseHome)) {
						TemplateForPKIfile.getInstance().setup();
						return false;
					}
				} else {
					/*
					 *  This would be the completion of this bundle,
					 *  since no configuration has been detected
					 */
					ActivateSecurity.getInstance().log("NO PKI file detected");// $NON-NLS-1$
					/*
					 * Files.createFile(Paths.get(USER_HOME+
					 * FileSystems.getDefault().getSeparator()+DotEclipse+
					 * FileSystems.getDefault().getSeparator()+ ".pki"));
					 */
					TemplateForPKIfile.getInstance().setup();
					return false;
				}
			}

		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		if (Files.exists(userDotEclipseHome)) {
			ActivateSecurity.getInstance().log("A PKI config file detected;"+ userDotEclipseHome.toString());// $NON-NLS-1$
			return true;
		}
		return false;
	}
	public boolean createPKI() {
		Optional<Boolean> eclipseHome = Optional.ofNullable(Files.exists(Paths.get(USER_HOME))); // $NON-NLS-1$
		if (!(eclipseHome.isEmpty())) {
			if (!(Files.exists(Paths.get(USER_HOME + FileSystems.getDefault().getSeparator() + DotEclipse
					+ FileSystems.getDefault().getSeparator() + ".pki")))) {
				String pkiFileFQN=USER_HOME + FileSystems.getDefault().getSeparator() + DotEclipse
						+ FileSystems.getDefault().getSeparator() + ".pki";

				userDotEclipseHome = Paths.get(pkiFileFQN);
				// create the PKI file
				try {
					Files.createFile(userDotEclipseHome);
				} catch (IOException e) {
					e.printStackTrace();
				}
				isSecurityFileRequired(pkiFileFQN);
				return true;
			} else {
				//PKI file already exists
				return false;
			}
		}
		return false;
	}

	public Properties load(String password, String salt) {
		Properties properties = new Properties();
		String passwd = null;
		try {
			IncomingSubscriber subscriber = IncomingSubscriber.getInstance();
			FileChannel fileChannel = FileChannel.open(userDotEclipseHome, StandardOpenOption.READ);
			FileChannel updateChannel = FileChannel.open(userDotEclipseHome, StandardOpenOption.WRITE);
			FileLock lock = fileChannel.lock(0L, Long.MAX_VALUE, true);
			InputStream fileInputStream = Channels.newInputStream(fileChannel);
			properties.load(fileInputStream);
			originalProperties.putAll(properties);
			for (Entry<Object, Object> entry : properties.entrySet()) {
				entry.setValue(entry.getValue().toString().trim());
			}
			
			Optional<String> passwdContainer = Optional
					.ofNullable(properties.getProperty("javax.net.ssl.keyStorePassword")); //$NON-NLS-1$
			Optional<String> encryptedPasswd = Optional
					.ofNullable(properties.getProperty("javax.net.ssl.encryptedPassword")); //$NON-NLS-1$
			if (passwdContainer.isEmpty()) {
				Optional<String> keyStoreContainer = Optional.ofNullable(
						properties.getProperty("javax.net.ssl.keyStore")); //$NON-NLS-1$
				if (!(keyStoreContainer.isEmpty() )) {
					System.setProperty("javax.net.ssl.keyStore", keyStoreContainer.get().toString().trim());
				}
				Optional<String> keyStoreTypeContainer = Optional.ofNullable(
						properties.getProperty("javax.net.ssl.keyStoreType")); //$NON-NLS-1$
				if (!(keyStoreTypeContainer.isEmpty() )) {
					String keyStoreType = keyStoreTypeContainer.get().toString().trim();
					if (keyStoreType.equalsIgnoreCase("PKCS12" )) { //$NON-NLS-1$
						System.setProperty("javax.net.ssl.keyStoreType", keyStoreType);//$NON-NLS-1$
						// get the passwd from console
						//PokeInConsole.PASSWD.get();
						try {
							try {
								Optional<String> testKeyContainer = Optional.ofNullable(
										System.getProperty("core.key"));
								if (!(testKeyContainer.isEmpty() ))  {
									String testKey = testKeyContainer.get().toString().trim();
									if (testKey.equalsIgnoreCase("eclipse.core.pki.testing")) {
										return properties;
									}
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
							PublishPasswordUpdate.getInstance().subscribe(subscriber);
							PkiPasswordGrabberWidget runner = PkiPasswordGrabberWidget.getInstance();
							Thread t1 = new Thread(runner);
							t1.start();
							
						} catch(Exception xe) {
							// User may have said cancel
						}
						
						
					} else {
						System.setProperty("javax.net.ssl.keyStorePassword", "");//$NON-NLS-1$
					}
				}
			} else {
				if ((encryptedPasswd.isEmpty()) && (!(passwdContainer.isEmpty()))) {

					properties.setProperty("javax.net.ssl.encryptedPassword", "true"); //$NON-NLS-1$ //$NON-NLS-2$
					passwd = passwdContainer.get();
					properties.setProperty("javax.net.ssl.keyStorePassword", //$NON-NLS-1$
					SecureGCM.getInstance().encrypt(passwd, password, salt));
					OutputStream os = Channels.newOutputStream(updateChannel);
					properties.save(os, null);
					// After saving encrypted passwd to properties file, switch to unencrypted
					properties.setProperty("javax.net.ssl.keyStorePassword", passwd); //$NON-NLS-1$
					SecurityOpRequest.getInstance().setConnected(true);
					PublishPasswordUpdate.getInstance().publishMessage(passwd);
				} else {
					String ePasswd = passwdContainer.get();
					passwd = NormalizeGCM.getInstance().decrypt(ePasswd, password, salt);
					System.setProperty("javax.net.ssl.decryptedPassword", "true"); //$NON-NLS-1$ //$NON-NLS-2$
					properties.setProperty("javax.net.ssl.keyStorePassword", passwd); //$NON-NLS-1$
					properties.setProperty("javax.net.ssl.decryptedPassword", "true"); //$NON-NLS-1$ //$NON-NLS-2$

				}
				subscriber.publishedIncoming();
			}
			
			properties.setProperty("javax.net.ssl.decryptedPassword", "true"); //$NON-NLS-1$ //$NON-NLS-2$

			System.getProperties().putAll(properties);

			lock.release();
			ActivateSecurity.getInstance().log("Loaded PKI System Properties");// $NON-NLS-1$
		} catch (IOException e) {
			e.printStackTrace();
		}
		return properties;
	}

	public void restoreProperties() {
		try {
			Files.deleteIfExists(userDotEclipseHome);
			Files.createFile(userDotEclipseHome);
			FileChannel updateChannel = FileChannel.open(userDotEclipseHome, StandardOpenOption.WRITE);
			OutputStream os = Channels.newOutputStream(updateChannel);
			String date = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
			originalProperties.store(os, "Restored to Original:" + date);
			os.flush();
			os.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private static void isSecurityFileRequired(String securityFileLocation) {
		Path dir = null;
		StringBuilder sb = new StringBuilder();

		try {
			sb.append(securityFileLocation);
			sb.append(FileSystems.getDefault().getSeparator());
			dir = Paths.get(sb.toString());
			try {
				//just in case it hasnt been created yet
				Files.createDirectories(dir);
			} catch(Exception createFileErr) {}
				
			Path path = Paths.get(sb.toString());

			if (!(path.toFile().exists())) {
				Files.deleteIfExists(path);
				Files.createFile(path);
				Charset charset = Charset.forName("UTF-8");//$NON-NLS-1$
				ArrayList<String> a = fileContents();
				if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) { //$NON-NLS-1$
					PosixFileAttributeView posixAttributes = Files.getFileAttributeView(path,
							PosixFileAttributeView.class);
					Set<PosixFilePermission> permissions = posixAttributes.readAttributes().permissions();
					permissions.remove(PosixFilePermission.GROUP_READ);
					posixAttributes.setPermissions(permissions);
					Files.write(path, a, charset, StandardOpenOption.TRUNCATE_EXISTING);

					permissions.remove(PosixFilePermission.OWNER_WRITE);
					posixAttributes.setPermissions(permissions);
				} else {
					Files.write(path, a, charset, StandardOpenOption.TRUNCATE_EXISTING);
					Files.setAttribute(path, "dos:hidden", Boolean.valueOf(true));//$NON-NLS-1$
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static ArrayList<String> fileContents() {

		ArrayList<String> a = new ArrayList<>();

		try {
			a.add("javax.net.ssl.trustStoreType=" + System.getProperty("javax.net.ssl.trustStoreType"));//$NON-NLS-1$ //$NON-NLS-2$
			a.add("javax.net.ssl.trustStorePassword=" + System.getProperty("javax.net.ssl.trustStorePassword"));//$NON-NLS-1$ //$NON-NLS-2$
			a.add("javax.net.ssl.trustStore=" + System.getProperty("javax.net.ssl.trustStore"));//$NON-NLS-1$ //$NON-NLS-2$
			a.add("");//$NON-NLS-1$

			if (System.getProperty("javax.net.ssl.keyStoreType") != null) {//$NON-NLS-1$
				a.add("javax.net.ssl.keyStoreType=" + System.getProperty("javax.net.ssl.keyStoreType"));//$NON-NLS-1$ //$NON-NLS-2$
				a.add("javax.net.ssl.keyStore=" + System.getProperty("javax.net.ssl.keyStore")); //$NON-NLS-1$ //$NON-NLS-2$
				if (System.getProperty("javax.net.ssl.keyStoreType").equalsIgnoreCase("PKCS12")) { //$NON-NLS-1$ //$NON-NLS-2$
					
				} else {
					a.add("javax.net.ssl.keyStorePassword=");//$NON-NLS-1$
					a.add("javax.net.ssl.keyStoreProvider=" + System.getProperty("javax.net.ssl.keyStoreProvider")); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return a;
	}
}
