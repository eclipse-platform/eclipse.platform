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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.security.Principal;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.net.ssl.X509TrustManager;
import org.eclipse.core.internal.runtime.CollectionTrustManager;
import org.junit.jupiter.api.Test;

@SuppressWarnings("restriction")
public class CollectionTrustManagerTest {

	@Test
	public void testAcceptedIssuers() throws Exception {
		X509Certificate[] acceptedIssuers1 = { new StubX509Certificate(), new StubX509Certificate() };
		X509Certificate[] acceptedIssuers2 = { new StubX509Certificate(), new StubX509Certificate() };
		X509TrustManager manager1 = new StubX509TrustManager(List.<X509Certificate[]>of(acceptedIssuers1));
		X509TrustManager manager2 = new StubX509TrustManager(List.<X509Certificate[]>of(acceptedIssuers2));
		CollectionTrustManager collectionTrustManager = new CollectionTrustManager(Arrays.asList(manager1, manager2));

		X509Certificate[] allAcceptedIssuers = collectionTrustManager.getAcceptedIssuers();

		assertThat(allAcceptedIssuers).containsExactly(acceptedIssuers1[0], acceptedIssuers1[1], acceptedIssuers2[0],
				acceptedIssuers2[1]);
	}

	@Test
	public void testCheckClientTrusted() throws Exception {
		X509Certificate[] chainTrustedBy1 = { new StubX509Certificate() };
		X509Certificate[] chainTrustedBy2 = { new StubX509Certificate() };
		X509Certificate[] chainTrustedByNone = { new StubX509Certificate() };
		X509Certificate[] chainTrustedByBoth = { new StubX509Certificate() };
		String authType = "testAuthType";

		StubX509TrustManager manager1 = new StubX509TrustManager(List.of(chainTrustedBy1, chainTrustedByBoth));
		StubX509TrustManager manager2 = new StubX509TrustManager(List.of(chainTrustedBy2, chainTrustedByBoth));

		CollectionTrustManager collectionTrustManager = new CollectionTrustManager(Arrays.asList(manager1, manager2));

		collectionTrustManager.checkClientTrusted(chainTrustedBy1, authType);
		collectionTrustManager.checkClientTrusted(chainTrustedBy2, authType);
		collectionTrustManager.checkClientTrusted(chainTrustedByBoth, authType);

		CertificateException exception = assertThrows(CertificateException.class, () -> {
			collectionTrustManager.checkClientTrusted(chainTrustedByNone, authType);
		});
		assertThat(exception).isSameAs(manager1.exception); // first in the list
		assertThat(exception.getSuppressed()).containsExactly(manager2.exception); // second, suppressed
	}

	@Test
	public void testCheckServerTrusted() throws Exception {
		X509Certificate[] chainTrustedBy1 = { new StubX509Certificate() };
		X509Certificate[] chainTrustedBy2 = { new StubX509Certificate() };
		X509Certificate[] chainTrustedByNone = { new StubX509Certificate() };
		X509Certificate[] chainTrustedByBoth = { new StubX509Certificate() };
		String authType = "testAuthType";

		StubX509TrustManager manager1 = new StubX509TrustManager(List.of(chainTrustedBy1, chainTrustedByBoth));
		StubX509TrustManager manager2 = new StubX509TrustManager(List.of(chainTrustedBy2, chainTrustedByBoth));

		CollectionTrustManager collectionTrustManager = new CollectionTrustManager(Arrays.asList(manager1, manager2));

		collectionTrustManager.checkServerTrusted(chainTrustedBy1, authType);
		collectionTrustManager.checkServerTrusted(chainTrustedBy2, authType);
		collectionTrustManager.checkServerTrusted(chainTrustedByBoth, authType);

		CertificateException exception = assertThrows(CertificateException.class, () -> {
			collectionTrustManager.checkServerTrusted(chainTrustedByNone, authType);
		});
		assertThat(exception).isSameAs(manager1.exception); // first in the list
		assertThat(exception.getSuppressed()).containsExactly(manager2.exception); // second, suppressed
	}

	private static class StubX509TrustManager implements X509TrustManager {

		public final CertificateException exception = new CertificateException();
		private List<X509Certificate[]> trusted;

		public StubX509TrustManager(List<X509Certificate[]> trusted) {
			this.trusted = trusted;
		}

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			for (X509Certificate[] trustedChain : trusted) {
				if (Arrays.equals(chain, trustedChain)) {
					return;
				}
			}
			throw exception;
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			for (X509Certificate[] trustedChain : trusted) {
				if (Arrays.equals(chain, trustedChain)) {
					return;
				}
			}
			throw exception;
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return trusted.stream().flatMap(Arrays::stream).toArray(X509Certificate[]::new);
		}

	}

	private static class StubX509Certificate extends X509Certificate {

		@Override
		public boolean hasUnsupportedCriticalExtension() {
			throw new IllegalStateException();
		}

		@Override
		public Set<String> getCriticalExtensionOIDs() {
			throw new IllegalStateException();
		}

		@Override
		public Set<String> getNonCriticalExtensionOIDs() {
			throw new IllegalStateException();
		}

		@Override
		public byte[] getExtensionValue(String oid) {
			throw new IllegalStateException();
		}

		@Override
		public void checkValidity() {
			throw new IllegalStateException();
		}

		@Override
		public void checkValidity(Date date) {
			throw new IllegalStateException();
		}

		@Override
		public int getVersion() {
			throw new IllegalStateException();
		}

		@Override
		public BigInteger getSerialNumber() {
			throw new IllegalStateException();
		}

		@SuppressWarnings("deprecation")
		@Override
		public Principal getIssuerDN() {
			throw new IllegalStateException();
		}

		@SuppressWarnings("deprecation")
		@Override
		public Principal getSubjectDN() {
			throw new IllegalStateException();
		}

		@Override
		public Date getNotBefore() {
			throw new IllegalStateException();
		}

		@Override
		public Date getNotAfter() {
			throw new IllegalStateException();
		}

		@Override
		public byte[] getTBSCertificate() {
			throw new IllegalStateException();
		}

		@Override
		public byte[] getSignature() {
			throw new IllegalStateException();
		}

		@Override
		public String getSigAlgName() {
			throw new IllegalStateException();
		}

		@Override
		public String getSigAlgOID() {
			throw new IllegalStateException();
		}

		@Override
		public byte[] getSigAlgParams() {
			throw new IllegalStateException();
		}

		@Override
		public boolean[] getIssuerUniqueID() {
			throw new IllegalStateException();
		}

		@Override
		public boolean[] getSubjectUniqueID() {
			throw new IllegalStateException();
		}

		@Override
		public boolean[] getKeyUsage() {
			throw new IllegalStateException();
		}

		@Override
		public int getBasicConstraints() {
			throw new IllegalStateException();
		}

		@Override
		public byte[] getEncoded() {
			throw new IllegalStateException();
		}

		@Override
		public void verify(PublicKey key) {
			throw new IllegalStateException();
		}

		@Override
		public void verify(PublicKey key, String sigProvider) {
			throw new IllegalStateException();
		}

		@Override
		public PublicKey getPublicKey() {
			throw new IllegalStateException();
		}

		@Override
		public String toString() {
			return Integer.toHexString(System.identityHashCode(this));
		}

		@Override
		public boolean equals(Object other) {
			return other == this;
		}

	}
}
