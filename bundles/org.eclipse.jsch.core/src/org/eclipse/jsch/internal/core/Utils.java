/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jsch.internal.core;

import java.util.Hashtable;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Proxy;
import com.jcraft.jsch.ProxyHTTP;
import com.jcraft.jsch.ProxySOCKS5;
import com.jcraft.jsch.Session;

/**
 * 
 * @since 1.0
 */
public class Utils{
  
  /* should have at least one element */
  private static final String[] PREFERRED_AUTH_METHODS=new String[] {
      "gssapi-with-mic", "publickey", "password", "keyboard-interactive"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

  public static String getDefaultAuthMethods(){
    return getDefaultMethods(PREFERRED_AUTH_METHODS);
  }
  
  private static final String[] PREFERRED_KEX_METHODS=new String[] {
    "diffie-hellman-group1-sha1",          //$NON-NLS-1$
    "diffie-hellman-group14-sha1",         //$NON-NLS-1$
    "diffie-hellman-group-exchange-sha1",  //$NON-NLS-1$
    "diffie-hellman-group-exchange-sha256" //$NON-NLS-1$
  };

  public static String getDefaultKEXMethods(){
    return getDefaultMethods(PREFERRED_KEX_METHODS);
  }
  
  private static final String[] PREFERRED_MAC_METHODS=new String[] {
    "hmac-md5",      //$NON-NLS-1$
    "hmac-sha1",     //$NON-NLS-1$
    "hmac-sha2-256", //$NON-NLS-1$
    "hmac-sha1-96",  //$NON-NLS-1$
    "hmac-md5-96"    //$NON-NLS-1$
  };

  public static String getDefaultMACMethods(){
    return getDefaultMethods(PREFERRED_MAC_METHODS);
  }
  
  private static String getDefaultMethods(String[] methods){
    String defaultValue = methods[0];
    for(int i = 1; i < methods.length; i++){
      defaultValue += "," + methods[i]; //$NON-NLS-1$
    }
    return defaultValue;
  }

  public static String loadPrivateKeys(JSch jsch, String current_pkeys){
    Preferences preferences=JSchCorePlugin.getPlugin().getPluginPreferences();
    String ssh_home=preferences.getString(IConstants.KEY_SSH2HOME);
    String pkeys=preferences.getString(IConstants.KEY_PRIVATEKEY);

    if(ssh_home.length()==0)
      ssh_home=PreferenceInitializer.SSH_HOME_DEFAULT;

    java.io.File file;
    String[] pkey=pkeys.split(","); //$NON-NLS-1$
    String[] _pkey=current_pkeys.split(","); //$NON-NLS-1$
    String result=""; //$NON-NLS-1$
    for(int i=0; i<pkey.length; i++){
      file=new java.io.File(pkey[i]);
      if(!file.isAbsolute()){
        file=new java.io.File(ssh_home, pkey[i]);
      }
      if(file.exists()){
        boolean notyet=true;
        for(int j=0; j<_pkey.length; j++){
          if(pkey[i].equals(_pkey[j])){
            notyet=false;
            break;
          }
        }
        try{
          if(notyet)
            jsch.addIdentity(file.getPath());
          if(result.length()==0){
            result=pkey[i];
          }
          else{
            result+=(","+pkey[i]); //$NON-NLS-1$
          }
        }
        catch(JSchException e){
          JSchCorePlugin.log(IStatus.ERROR,
              "An error occurred loading the SSH2 private keys", e); //$NON-NLS-1$
        }
      }
    }
    return result;
  }

  public static Session createSession(JSch jsch, String username,
      String hostname, int port) throws JSchException{
    if(port == -1)
      port = IConstants.SSH_DEFAULT_PORT;
    Session session=jsch.getSession(username, hostname, port);
    setProxy(session);
    Hashtable config=new Hashtable();
    config.put("PreferredAuthentications", //$NON-NLS-1$ 
        getEnabledPreferredAuthMethods());
    config.put("kex", //$NON-NLS-1$ 
        getEnabledPreferredKEXMethods());
    config.put("mac.c2s", //$NON-NLS-1$ 
        getEnabledPreferredMACMethods());
    config.put("mac.s2c", //$NON-NLS-1$ 
        getEnabledPreferredMACMethods());
    session.setConfig(config);
    return session;
  }

  public static void setProxy(Session session){
    Proxy proxy=getProxyForHost(session.getHost(), IProxyData.SOCKS_PROXY_TYPE);
    if(proxy==null)
      proxy=getProxyForHost(session.getHost(), IProxyData. HTTPS_PROXY_TYPE);
    if(proxy!=null)
      session.setProxy(proxy);
  }

  private static int getPort(IProxyData data){
    int port=data.getPort();
    if(port==-1){
      if(data.getType().equals(IProxyData.HTTP_PROXY_TYPE))
        port=80;
      else if(data.getType().equals(IProxyData.HTTPS_PROXY_TYPE))
        port=443;
      else if(data.getType().equals(IProxyData.SOCKS_PROXY_TYPE))
        port=1080;
    }
    return port;
  }

  private static IProxyData getProxyData(String host, String type){
    IProxyService proxyService=JSchCorePlugin.getPlugin().getProxyService();
    if(proxyService==null)
      return null;
    IProxyData data=proxyService.getProxyDataForHost(host, type);
    if(data==null||data.getHost()==null||getJSchProxyType(data)==null)
      return null;
    return data;
  }

  private static String getJSchProxyType(IProxyData data){
    if(data.getType().equals(IProxyData.HTTPS_PROXY_TYPE))
      return IConstants.PROXY_TYPE_HTTP;
    if(data.getType().equals(IProxyData.SOCKS_PROXY_TYPE))
      return IConstants.PROXY_TYPE_SOCKS5;
    return null;
  }

  public static Proxy getProxyForHost(String host, String proxyType){
    IProxyService proxyService=JSchCorePlugin.getPlugin().getProxyService();
    if(proxyService==null)
      return null;
    boolean useProxy=proxyService.isProxiesEnabled();
    if(!useProxy)
      return null;
    Proxy proxy=null;
    IProxyData data=getProxyData(host, proxyType);
    if(data==null)
      return null;
    String _type=getJSchProxyType(data);
    if(_type==null)
      return null;
    String _host=data.getHost();
    int _port=getPort(data);

    boolean useAuth=data.isRequiresAuthentication();
    String _user=""; //$NON-NLS-1$
    String _pass=""; //$NON-NLS-1$

    // Retrieve username and password from keyring.
    if(useAuth){
      _user=data.getUserId();
      _pass=data.getPassword();
    }

    String proxyhost=_host+":"+_port; //$NON-NLS-1$
    if(_type.equals(IConstants.PROXY_TYPE_HTTP)){
      proxy=new ProxyHTTP(proxyhost);
      if(useAuth){
        ((ProxyHTTP)proxy).setUserPasswd(_user, _pass);
      }
    }
    else if(_type.equals(IConstants.PROXY_TYPE_SOCKS5)){
      proxy=new ProxySOCKS5(proxyhost);
      if(useAuth){
        ((ProxySOCKS5)proxy).setUserPasswd(_user, _pass);
      }
    }
    return proxy;
  }
  
  public static void migrateSSH2Preferences() {
    Preferences preferences = JSchCorePlugin.getPlugin().getPluginPreferences();
    if(!preferences.getBoolean(IConstants.PREF_HAS_MIGRATED_SSH2_PREFS)){
      preferences.setValue(IConstants.PREF_HAS_MIGRATED_SSH2_PREFS, true);
      migrateSSH2Preferences(InstanceScope.INSTANCE.getNode("")); //$NON-NLS-1$
    }
  }
  
  public static void migrateSSH2Preferences(org.osgi.service.prefs.Preferences node) {
    try{
      if(node.nodeExists("org.eclipse.team.cvs.ssh2")){ //$NON-NLS-1$
        org.osgi.service.prefs.Preferences ssh2Prefs=node.node("org.eclipse.team.cvs.ssh2"); //$NON-NLS-1$
        String oldHome=ssh2Prefs.get(IConstants.KEY_OLD_SSH2HOME, null);
        String oldKey=ssh2Prefs.get(IConstants.KEY_OLD_PRIVATEKEY, null);
        if(oldHome!=null){
          org.osgi.service.prefs.Preferences jschPrefs=node.node(JSchCorePlugin.ID);
          jschPrefs.put(IConstants.KEY_SSH2HOME, oldHome);
          ssh2Prefs.remove(IConstants.KEY_OLD_SSH2HOME);
        }
        if(oldKey!=null){
          org.osgi.service.prefs.Preferences jschPrefs=node.node(JSchCorePlugin.ID);
          jschPrefs.put(IConstants.KEY_PRIVATEKEY, oldKey);
          ssh2Prefs.remove(IConstants.KEY_OLD_PRIVATEKEY);
        }
      }
    }
    catch(BackingStoreException e){
      // do nothing
    }
  }
  
  public static String getEnabledPreferredAuthMethods(){
    IPreferencesService service = Platform.getPreferencesService();
    return service.getString(JSchCorePlugin.ID,
        IConstants.PREF_PREFERRED_AUTHENTICATION_METHODS, getDefaultAuthMethods(), null);
  }

  public static String getMethodsOrder(){
    IPreferencesService service = Platform.getPreferencesService();
    return service.getString(JSchCorePlugin.ID,
        IConstants.PREF_PREFERRED_AUTHENTICATION_METHODS_ORDER, getDefaultAuthMethods(), null);
  }
  
  public static void setEnabledPreferredAuthMethods(String methods, String order){
    IPreferencesService service=Platform.getPreferencesService();
    service.getRootNode().node(InstanceScope.SCOPE).node(JSchCorePlugin.ID).put(
        IConstants.PREF_PREFERRED_AUTHENTICATION_METHODS, methods);
    service.getRootNode().node(InstanceScope.SCOPE).node(JSchCorePlugin.ID).put(
        IConstants.PREF_PREFERRED_AUTHENTICATION_METHODS_ORDER, order);}
  
  public static String getEnabledPreferredKEXMethods(){
    IPreferencesService service = Platform.getPreferencesService();
    return service.getString(JSchCorePlugin.ID,
        IConstants.PREF_PREFERRED_KEYEXCHANGE_METHODS, getDefaultKEXMethods(), null);
  }
  
  public static String getKEXMethodsOrder(){
    IPreferencesService service = Platform.getPreferencesService();
    return service.getString(JSchCorePlugin.ID,
        IConstants.PREF_PREFERRED_KEYEXCHANGE_METHODS_ORDER, getDefaultKEXMethods(), null);
  }
  
  public static void setEnabledPreferredKEXMethods(String methods, String order){
    IPreferencesService service=Platform.getPreferencesService();
    service.getRootNode().node(InstanceScope.SCOPE).node(JSchCorePlugin.ID).put(
        IConstants.PREF_PREFERRED_KEYEXCHANGE_METHODS, methods);
    service.getRootNode().node(InstanceScope.SCOPE).node(JSchCorePlugin.ID).put(
        IConstants.PREF_PREFERRED_KEYEXCHANGE_METHODS_ORDER, order);
  }
  
  public static String getEnabledPreferredMACMethods(){
    IPreferencesService service = Platform.getPreferencesService();
    return service.getString(JSchCorePlugin.ID,
        IConstants.PREF_PREFERRED_MAC_METHODS, getDefaultMACMethods(), null);
  }
  
  public static String getMACMethodsOrder(){
    IPreferencesService service = Platform.getPreferencesService();
    return service.getString(JSchCorePlugin.ID,
        IConstants.PREF_PREFERRED_MAC_METHODS_ORDER, getDefaultMACMethods(), null);
  }
  
  public static void setEnabledPreferredMACMethods(String methods, String order){
    IPreferencesService service=Platform.getPreferencesService();
    service.getRootNode().node(InstanceScope.SCOPE).node(JSchCorePlugin.ID).put(
        IConstants.PREF_PREFERRED_MAC_METHODS, methods);
    service.getRootNode().node(InstanceScope.SCOPE).node(JSchCorePlugin.ID).put(
        IConstants.PREF_PREFERRED_MAC_METHODS_ORDER, order);
  }
}
