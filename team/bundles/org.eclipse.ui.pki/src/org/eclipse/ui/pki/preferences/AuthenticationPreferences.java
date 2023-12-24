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
package org.eclipse.ui.pki.preferences;

public class AuthenticationPreferences {

	private static final String PREFIX = AuthenticationPreferences.class.getPackage().getName();

	public static final String MANUAL_PKI_SELECTION = PREFIX + "." + "manualPkiSelectionPref";

	public static final boolean MANUAL_PKI_SELECTION_DEFAULT = true;

	public static final String PKI_SELECTION_TYPE = "pkiType";

	public static final String SELECTED_PKI_CERTIFICATE = PREFIX + "." + "selectedPkiCertificatePref";

	public static final String DEFAULT_PKI_CERTIFICATE_DIR = "U:\\private\\certificates";

	public static final String DONT_SAVE_SELECTED_PKI_PATH = PREFIX + "." + "dontSaveSelectedPkiPref";

	public static final String DEFAULT_AUTHENTICATION_DISABLED = PREFIX + "." + "defaultAuthenticationDisabledPref";

	public static final boolean DEFAULT_AUTHENTICATION_DISABLED_DEFAULT = false;

	public static final String SELECTED_DEFAULT_AUTHENTICATABLE = PREFIX + "." + "selectedDefaultAuthenticatablePref";

	public static final String SELECTED_DEFAULT_AUTHENTICATABLE_DEFAULT = "";

	public static final String PKI_CERTIFICATE_LOCATION = "pkiCertLocation";

	public static final String PKCS11_CFG_FILE_LOCATION = "pkcs11CfgFile";

	public static final String X500_CREDENTIAL = "x500Credential";

	public static final String TRUST_STORE_LOCATION = "trustStoreLocation";

	public static final String PKCS11_CONFIGURE_FILE_LOCATION = "pkcs11ConfigureFileLocation";

	public static final String SECURITY_PROVIDER = "securityProvider";

	public static final String[] FILTER_NAMES = { "PKCS #12 Files (*.p12)", "All Files (*.*)" };

	public static final String[] FILTER_EXTS = { "*.p12", "*.*" };

}
