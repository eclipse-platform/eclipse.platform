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
package org.eclipse.ui.pki.pkcs;

import java.util.List;

import javax.net.ssl.SSLContext;


public class ProviderImpl  {
	public static SSLPkcs11Provider security = SSLPkcs11Provider.getInstance();
	public static SSLPkcs11Provider getNewInstance() {
		security=SSLPkcs11Provider.getInstance();
		return security;
	}
	@SuppressWarnings("unchecked")
	public List<String> getList() {
		return (List<String>) security.getList();
	}
	public String getAlias() {
		return security.getAlias();
	}
	@SuppressWarnings("static-access")
	public void off() {
		security.disable();
	}
}
