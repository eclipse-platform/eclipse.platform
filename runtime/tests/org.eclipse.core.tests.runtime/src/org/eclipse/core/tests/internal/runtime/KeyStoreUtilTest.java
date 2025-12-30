/*******************************************************************************
 * Copyright (c) 2025 SAP SE and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.internal.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.core.internal.runtime.CollectionTrustManager;
import org.eclipse.core.internal.runtime.KeyStoreUtil;
import org.eclipse.core.runtime.Platform;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

@SuppressWarnings("restriction")
public class KeyStoreUtilTest {

	private static final List<String> SYSTEM_PROPERTIES_TO_BACKUP_AND_RESTORE = List.of( //
			"eclipse.platform.mergeTrust", //
			"javax.net.ssl.trustStore", //
			"javax.net.ssl.trustStorePassword", //
			"javax.net.ssl.trustStoreProvider", //
			"javax.net.ssl.trustStoreType", //
			"javax.net.ssl.keyStore", //
			"javax.net.ssl.keyStorePassword", //
			"javax.net.ssl.keyStoreProvider", //
			"javax.net.ssl.keyStoreType");

	@TempDir
	private Path tempDir;

	private Map<String, String> systemPropertyBackups = new HashMap<>();
	private SSLContext previousSslContext;

	@BeforeEach
	public void setup() throws Exception {
		for (String property : SYSTEM_PROPERTIES_TO_BACKUP_AND_RESTORE) {
			systemPropertyBackups.put(property, System.getProperty(property, null));
		}
		previousSslContext = SSLContext.getDefault();
		System.setProperty("eclipse.platform.mergeTrust", "true");
	}

	@AfterEach
	public void teardown() {
		systemPropertyBackups.forEach((property, backupValue) -> {
			if (backupValue == null) {
				System.clearProperty(property);
			} else {
				System.setProperty(property, backupValue);
			}
		});
		SSLContext.setDefault(previousSslContext);
	}

	@Test
	public void loadTrustManagers_Default() throws Exception {

		TestSpecificKeyStoreUtil keyStoreUtil = new TestSpecificKeyStoreUtil();

		keyStoreUtil.setUpSslContext();

		assertThat(SSLContext.getDefault()).isEqualTo(keyStoreUtil.recordedSslContext);

		assertThat(keyStoreUtil.recordedTrustManagers).hasSize(1);
		assertThat(keyStoreUtil.recordedTrustManagers[0])
				.asInstanceOf(InstanceOfAssertFactories.type(CollectionTrustManager.class))
				.satisfies(manager -> assertThat(manager.getAcceptedIssuers()).isNotEmpty());

		CollectionTrustManager tm = (CollectionTrustManager) keyStoreUtil.recordedTrustManagers[0];

		// jvm
		assertThat(tm.getTrustManagers()).isNotEmpty();
		assertThat(keyStoreUtil.createdTrustManagersAndKeyStores).isNotEmpty();
		assertThat(keyStoreUtil.createdTrustManagersAndKeyStores.get(0).manager())
				.isEqualTo(tm.getTrustManagers().get(0));
		assertThat(keyStoreUtil.createdTrustManagersAndKeyStores.get(0).store()).isNull();
		assertThat(
				Arrays.stream(tm.getTrustManagers().get(0).getAcceptedIssuers())
						.map(X509Certificate::getSubjectX500Principal).map(X500Principal::getName))
								.anySatisfy(name -> assertThat(name).matches("(?i).*digicert.*root.*"));

		if (OS.WINDOWS.equals(OS.current())) {
			assertThat(tm.getTrustManagers()).hasSize(2);
			assertThat(keyStoreUtil.createdTrustManagersAndKeyStores).hasSize(2);
			assertThat(keyStoreUtil.createdTrustManagersAndKeyStores.get(1).manager())
					.isEqualTo(tm.getTrustManagers().get(1));
			assertThat(keyStoreUtil.createdTrustManagersAndKeyStores.get(1).store().getType())
					.isEqualTo("Windows-ROOT");
			assertThat(
					Arrays.stream(tm.getTrustManagers().get(1).getAcceptedIssuers())
							.map(X509Certificate::getSubjectX500Principal).map(X500Principal::getName))
									.anySatisfy(name -> assertThat(name).matches("(?i).*digicert.*root.*"));
		} else if (OS.MAC.equals(OS.current())) {
			assertThat(tm.getTrustManagers()).hasSize(2);
			assertThat(keyStoreUtil.createdTrustManagersAndKeyStores).hasSize(2);
			assertThat(keyStoreUtil.createdTrustManagersAndKeyStores.get(1).manager())
					.isEqualTo(tm.getTrustManagers().get(1));
			assertThat(keyStoreUtil.createdTrustManagersAndKeyStores.get(1).store().getType())
					.isEqualTo("KeychainStore");
			// Apple KeychainStore only includes the 'System' certificates
			// (enterprise/admin managed)
			// but not the 'System Roots' ones (public CAs).
			// There's nothing guaranteed / deterministic in the 'System' on CI machines
			// that we could check for here...
		} else {
			assertThat(tm.getTrustManagers()).hasSize(1);
			assertThat(keyStoreUtil.createdTrustManagersAndKeyStores).hasSize(1);
		}

		// no private keys
		assertThat(keyStoreUtil.recordedKeyManagers).isEmpty();
	}

	@Test
	public void loadTrustManagers_TrustSystemPropertiesPointToCustomTrustStore() throws Exception {

		System.setProperty("javax.net.ssl.trustStore", copyResourceToTempDirAndGetPath("truststore.jks"));
		System.setProperty("javax.net.ssl.trustStorePassword", "verysecret");

		TestSpecificKeyStoreUtil keyStoreUtil = new TestSpecificKeyStoreUtil();

		keyStoreUtil.setUpSslContext();

		assertThat(SSLContext.getDefault()).isEqualTo(keyStoreUtil.recordedSslContext);

		assertThat(keyStoreUtil.recordedTrustManagers).hasSize(1);
		assertThat(((CollectionTrustManager) keyStoreUtil.recordedTrustManagers[0]).getAcceptedIssuers()).isNotEmpty();

		CollectionTrustManager tm = (CollectionTrustManager) keyStoreUtil.recordedTrustManagers[0];

		assertThat(tm.getTrustManagers()).hasSize(1); // only the properties-based store

		assertThat(
				Arrays.stream(tm.getTrustManagers().get(0).getAcceptedIssuers())
						.map(X509Certificate::getSubjectX500Principal).map(X500Principal::getName).toList())
								.contains("CN=Test,C=DE");
		assertThat(keyStoreUtil.createdTrustManagersAndKeyStores).hasSize(1);
		assertThat(keyStoreUtil.createdTrustManagersAndKeyStores.get(0).manager()).isEqualTo(tm.getTrustManagers().get(0));
		// null caused KeyManagerFactory to load default system properties
		assertThat(keyStoreUtil.createdTrustManagersAndKeyStores.get(0).store()).isNull();

		assertThat(keyStoreUtil.recordedKeyManagers).isEmpty();
	}

	@Test
	@EnabledOnOs({ OS.WINDOWS, OS.MAC })
	public void loadTrustManagers_TrustSystemPropertiesPointToPlatformSpecificKeystore() throws Exception {
		if (OS.WINDOWS.equals(OS.current())) {
			System.setProperty("javax.net.ssl.trustStore", "NONE");
			System.setProperty("javax.net.ssl.trustStoreType", "Windows-ROOT");
		} else if (OS.MAC.equals(OS.current())) {
			System.setProperty("javax.net.ssl.trustStore", "NONE");
			System.setProperty("javax.net.ssl.trustStoreType", "KeychainStore");
			System.setProperty("javax.net.ssl.trustStoreProvider", "Apple");
		}

		TestSpecificKeyStoreUtil keyStoreUtil = new TestSpecificKeyStoreUtil();

		keyStoreUtil.setUpSslContext();

		assertThat(SSLContext.getDefault()).isEqualTo(keyStoreUtil.recordedSslContext);

		assertThat(keyStoreUtil.recordedTrustManagers).hasSize(1);

		CollectionTrustManager tm = (CollectionTrustManager) keyStoreUtil.recordedTrustManagers[0];

		assertThat(tm.getTrustManagers()).hasSize(1); // only the properties-based store
		assertThat(keyStoreUtil.createdTrustManagersAndKeyStores).hasSize(1);
		assertThat(keyStoreUtil.createdTrustManagersAndKeyStores.get(0).manager())
				.isEqualTo(tm.getTrustManagers().get(0));
		assertThat(keyStoreUtil.createdTrustManagersAndKeyStores.get(0).store()).isNull();

		if (OS.WINDOWS.equals(OS.current())) {
			assertThat(((CollectionTrustManager) keyStoreUtil.recordedTrustManagers[0]).getAcceptedIssuers())
					.isNotEmpty();
			assertThat(
					Arrays.stream(tm.getTrustManagers().get(0).getAcceptedIssuers())
							.map(X509Certificate::getSubjectX500Principal).map(X500Principal::getName))
									.anySatisfy(name -> assertThat(name).matches("(?i).*digicert.*root.*"));
		} else if (OS.MAC.equals(OS.current())) {
			// Apple KeychainStore only includes the 'System' certificates
			// (enterprise/admin managed)
			// but not the 'System Roots' ones (public CAs).
			// There's nothing guaranteed / deterministic in the 'System' on CI machines
			// that we could check for here...
		}
	}

	@Test
	public void loadKeyManagers_Default() throws Exception {

		TestSpecificKeyStoreUtil keyStoreUtil = new TestSpecificKeyStoreUtil();

		keyStoreUtil.setUpSslContext();

		assertThat(SSLContext.getDefault()).isEqualTo(keyStoreUtil.recordedSslContext);

		assertThat(keyStoreUtil.recordedKeyManagers).isEmpty();
		assertThat(keyStoreUtil.createdKeyManagersAndKeyStores).isEmpty();
	}

	@Test
	public void loadKeyManagers_KeySystemPropertiesPointToCustomKeyStore() throws Exception {

		System.setProperty("javax.net.ssl.keyStore", copyResourceToTempDirAndGetPath("keystore.p12"));
		System.setProperty("javax.net.ssl.keyStorePassword", "verysecret");
		System.setProperty("javax.net.ssl.keyStoreType", "PKCS12");

		TestSpecificKeyStoreUtil keyStoreUtil = new TestSpecificKeyStoreUtil();

		keyStoreUtil.setUpSslContext();

		assertThat(SSLContext.getDefault()).isEqualTo(keyStoreUtil.recordedSslContext);

		assertThat(keyStoreUtil.recordedKeyManagers).hasSize(1);
		assertThat(keyStoreUtil.recordedKeyManagers[0]).isInstanceOf(X509KeyManager.class);

		X509KeyManager km = (X509KeyManager) keyStoreUtil.recordedKeyManagers[0];

		assertThat(keyStoreUtil.createdKeyManagersAndKeyStores).hasSize(1);
		assertThat(keyStoreUtil.createdKeyManagersAndKeyStores.get(0).manager()).isEqualTo(km);
		assertThat(keyStoreUtil.createdKeyManagersAndKeyStores.get(0).store().getType()).isEqualTo("PKCS12");

		assertThat(km.getPrivateKey("test.key")).isNotNull();
	}

	@Test
	public void optInSystemPropertyNotSet() throws Exception {
		System.clearProperty("eclipse.platform.mergeTrust");

		TestSpecificKeyStoreUtil keyStoreUtil = new TestSpecificKeyStoreUtil();

		keyStoreUtil.setUpSslContext();

		assertThat(SSLContext.getDefault()).isEqualTo(previousSslContext);
	}

	private String copyResourceToTempDirAndGetPath(String resourceName) throws IOException {
		Path file = tempDir.resolve(resourceName);
		Files.copy(getClass().getResourceAsStream(resourceName), file, StandardCopyOption.REPLACE_EXISTING);
		return file.toAbsolutePath().toString();
	}

	private static final class TestSpecificKeyStoreUtil extends KeyStoreUtil {

		public TestSpecificKeyStoreUtil() {
			super(Platform.getOS());
		}

		public static record X509TrustManagerAndKeyStore(X509TrustManager manager, KeyStore store) {
		}

		public static record X509KeyManagerAndKeyStore(X509KeyManager manager, KeyStore store) {
		}

		public TrustManager[] recordedTrustManagers;
		public KeyManager[] recordedKeyManagers;
		public SSLContext recordedSslContext;
		public final List<X509TrustManagerAndKeyStore> createdTrustManagersAndKeyStores = new ArrayList<>();
		public final List<X509KeyManagerAndKeyStore> createdKeyManagersAndKeyStores = new ArrayList<>();

		@Override
		protected X509TrustManager createX509TrustManager(KeyStore keyStore) throws GeneralSecurityException {
			X509TrustManager manager = super.createX509TrustManager(keyStore);
			this.createdTrustManagersAndKeyStores.add(new X509TrustManagerAndKeyStore(manager, keyStore));
			return manager;
		}

		@Override
		protected X509KeyManager createX509KeyManager(KeyStore keyStore, char[] password)
				throws GeneralSecurityException {
			X509KeyManager manager = super.createX509KeyManager(keyStore, password);
			this.createdKeyManagersAndKeyStores.add(new X509KeyManagerAndKeyStore(manager, keyStore));
			return manager;
		}

		@Override
		protected void initSSLContext(SSLContext context, TrustManager[] trustManagers, KeyManager[] keyManagers,
				SecureRandom random) throws KeyManagementException {
			this.recordedTrustManagers = trustManagers;
			this.recordedKeyManagers = keyManagers;
			this.recordedSslContext = context;
			super.initSSLContext(context, trustManagers, keyManagers, random);
		}
	}

}
