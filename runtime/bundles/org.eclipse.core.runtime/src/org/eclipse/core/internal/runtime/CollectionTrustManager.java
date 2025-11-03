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
package org.eclipse.core.internal.runtime;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.net.ssl.X509TrustManager;

public class CollectionTrustManager implements X509TrustManager {

	private final List<X509TrustManager> trustManagers;

	public CollectionTrustManager(List<X509TrustManager> trustManagers) {
		this.trustManagers = trustManagers;
	}

	public List<X509TrustManager> getTrustManagers() {
		return trustManagers;
	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		CertificateException ce = null;
		for (X509TrustManager trustManager : this.getTrustManagers()) {
			try {
				trustManager.checkClientTrusted(chain, authType);
				return;
			} catch (CertificateException e) {
				if (ce == null) {
					ce = e;
				} else {
					ce.addSuppressed(e);
				}
			}
		}
		if (ce != null) {
			throw ce;
		}
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		CertificateException ce = null;
		for (X509TrustManager trustManager : this.getTrustManagers()) {
			try {
				trustManager.checkServerTrusted(chain, authType);
				return;
			} catch (CertificateException e) {
				if (ce == null) {
					ce = e;
				} else {
					ce.addSuppressed(e);
				}
			}
		}
		if (ce != null) {
			throw ce;
		}
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		return this.getTrustManagers().stream() //
				.map(X509TrustManager::getAcceptedIssuers) //
				.filter(Objects::nonNull).flatMap(Arrays::stream).toArray(X509Certificate[]::new);
	}

}
