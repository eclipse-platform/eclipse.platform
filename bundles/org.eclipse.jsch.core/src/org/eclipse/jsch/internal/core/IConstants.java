/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jsch.internal.core;

/**
 * Defines the constants used by the SSH2 Plugin
 */
public interface IConstants{

  public static final  String KEY_PROXY="CVSSSH2PreferencePage.PROXY"; //$NON-NLS-1$
  public static final  String KEY_PROXY_TYPE="CVSSSH2PreferencePage.PROXY_TYPE"; //$NON-NLS-1$
  public static final  String KEY_PROXY_HOST="CVSSSH2PreferencePage.PROXY_HOST"; //$NON-NLS-1$
  public static final  String KEY_PROXY_PORT="CVSSSH2PreferencePage.PROXY_PORT"; //$NON-NLS-1$
  public static final  String KEY_PROXY_AUTH="CVSSSH2PreferencePage.PROXY_AUTH"; //$NON-NLS-1$
  public static final  String KEY_PROXY_USER="CVSSSH2PreferencePage.PROXY_USER"; //$NON-NLS-1$
  public static final  String KEY_PROXY_PASS="CVSSSH2PreferencePage.PROXY_PASS"; //$NON-NLS-1$
  public static final  String KEY_OLD_SSH2HOME="CVSSSH2PreferencePage.SSH2HOME"; //$NON-NLS-1$
  public static final  String KEY_OLD_PRIVATEKEY="CVSSSH2PreferencePage.PRIVATEKEY"; //$NON-NLS-1$
  public static final  String KEY_KEYFILE="CVSSSH2PreferencePage.KEYFILE"; //$NON-NLS-1$
  
  public static final  String KEY_SSH2HOME="SSH2HOME"; //$NON-NLS-1$
  public static final  String KEY_PRIVATEKEY="PRIVATEKEY"; //$NON-NLS-1$

  public static final  String PROXY_TYPE_SOCKS5="SOCKS5"; //$NON-NLS-1$
  public static final  String PROXY_TYPE_HTTP="HTTP"; //$NON-NLS-1$
  public static final  String HTTP_DEFAULT_PORT="80"; //$NON-NLS-1$
  public static final  String SOCKS5_DEFAULT_PORT="1080"; //$NON-NLS-1$
  public static final  String PRIVATE_KEYS_DEFAULT="id_dsa,id_rsa"; //$NON-NLS-1$

  public static final  String DSA="DSA"; //$NON-NLS-1$
  public static final  String RSA="RSA"; //$NON-NLS-1$
  
  public static final int SSH_DEFAULT_PORT=22;
  public static final String SSH_DEFAULT_HOME=".ssh"; //$NON-NLS-1$
  public static final String SSH_OLD_DEFAULT_WIN32_HOME="ssh"; //$NON-NLS-1$
  public static final String SYSTEM_PROPERTY_USER_HOME="user.home"; //$NON-NLS-1$
  
  public final String PREF_USE_PROXY="proxyEnabled"; //$NON-NLS-1$
  public final String PREF_PROXY_TYPE="proxyType"; //$NON-NLS-1$
  public final String PREF_PROXY_HOST="proxyHost"; //$NON-NLS-1$
  public final String PREF_PROXY_PORT="proxyPort"; //$NON-NLS-1$
  public final String PREF_PROXY_AUTH="proxyAuth"; //$NON-NLS-1$

  public final String PREF_HAS_MIGRATED_SSH2_PREFS="org.eclipse.jsch.core.hasMigratedSsh2Preferences"; //$NON-NLS-1$
  public final String PREF_HAS_CHANGED_DEFAULT_WIN32_SSH_HOME="org.eclipse.jsch.core.hasChangedDefaultWin32SshHome"; //$NON-NLS-1$
  
  public static final String PREF_PREFERRED_AUTHENTICATION_METHODS="CVSSSH2PreferencePage.PREF_AUTH_METHODS"; //$NON-NLS-1$
  public static final String PREF_PREFERRED_AUTHENTICATION_METHODS_ORDER="CVSSSH2PreferencePage.PREF_AUTH_METHODS_ORDER"; //$NON-NLS-1$
  
  public static final String PREF_PREFERRED_SSHAGENT="CVSSSH2PreferencePage.PREF_SSHAGENT"; //$NON-NLS-1$

  public static final String PREF_PREFERRED_KEYEXCHANGE_METHODS="CVSSSH2PreferencePage.PREF_KEX_METHODS"; //$NON-NLS-1$
  public static final String PREF_PREFERRED_KEYEXCHANGE_METHODS_ORDER="CVSSSH2PreferencePage.PREF_KEX_METHODS_ORDER"; //$NON-NLS-1$
  
  public static final String PREF_PREFERRED_MAC_METHODS="CVSSSH2PreferencePage.PREF_MAC_METHODS"; //$NON-NLS-1$
  public static final String PREF_PREFERRED_MAC_METHODS_ORDER="CVSSSH2PreferencePage.PREF_MAC_METHODS_ORDER"; //$NON-NLS-1$
}
