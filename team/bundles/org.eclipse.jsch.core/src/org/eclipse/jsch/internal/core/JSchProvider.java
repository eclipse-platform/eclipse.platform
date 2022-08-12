/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Atsuhiko Yamanaka, JCraft,Inc. - adding methods referring to IJSchLocation
 *******************************************************************************/
package org.eclipse.jsch.internal.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jsch.core.IJSchService;
import org.eclipse.jsch.core.IJSchLocation;
import org.eclipse.osgi.util.NLS;

import com.jcraft.jsch.*;

/**
 * A static class whose purpose is to ensure that all the JSch preferences are properly pushed into JSch
 * before a {@link Session} is created.
 * @since 1.0
 */
class JSchProvider implements IJSchService {
  
  private static JSchProvider instance;

  @Override
  public Session createSession(String host, int port, String username) throws JSchException {

    if(JSchCorePlugin.getPlugin().isNeedToLoadKnownHosts()){
      JSchCorePlugin.getPlugin().loadKnownHosts();
    }

    if(JSchCorePlugin.getPlugin().isNeedToLoadKeys()){
      JSchCorePlugin.getPlugin().loadPrivateKeys();
    }
    
    return Utils.createSession(JSchCorePlugin.getPlugin().getJSch(), username, host, port);
  }
  
  @Override
  public Session createSession(IJSchLocation location, UserInfo uinfo) throws JSchException {

    Session session=createSession(location.getHost(), location.getPort(), location.getUsername());
    
    if(uinfo==null){
      IUserAuthenticator authenticator=getPluggedInAuthenticator();
      if(authenticator==null)
        authenticator=new NullUserAuthenticator();
      uinfo=new UserInfoImpl(location, authenticator, (JSchCorePlugin.getPlugin().getTimeout() * 1000));
    }
    if(uinfo!=null)
      session.setUserInfo(uinfo);
    
    return session;
  }
  
  public Session createSession(IJSchLocation location) throws JSchException {
    return createSession(location, null);
  }
  
  @Override
  public void connect(Session session, int timeout,
      IProgressMonitor monitor) throws JSchException{
    session.setSocketFactory(new ResponsiveSocketFactory(monitor, timeout));
    
    UserInfo ui=session.getUserInfo();
    
    if(ui!=null && (ui instanceof UserInfoImpl))
      ((UserInfoImpl)ui).aboutToConnect();
    
    try{
      session.connect();
    }
    catch(java.lang.ArrayIndexOutOfBoundsException e){  
      // TODO This catch clause has been added to work around
      // Bug 217980 and will be deleted in the future.
      throw new JSchException("invalid server's version string");//$NON-NLS-1$
    }
    catch(JSchException e){
      
      // Try again since the previous prompt may have obtained
      // the proper credential from the user.
      // This logic had been implemented in org.eclipse.team.internal.ccvs.ssh2.JSchSession#getSession.
      if(isAuthenticationFailure(e)
          &&ui!=null&&(ui instanceof UserInfoImpl)
          &&hasPromptExceededTimeout(session)
          &&((UserInfoImpl)ui).incReuse()==0){
        String host=session.getHost();
        String user=session.getUserName();
        int port=session.getPort();
        session=Utils.createSession(getJSch(), user, host, port);
        session.setUserInfo(ui);
        session.setTimeout(timeout);
        connect(session, timeout, monitor);
        return;
      }
      
      if(session.isConnected())
        session.disconnect();
      throw e;
    }
    
    if(ui!=null && (ui instanceof UserInfoImpl))
      ((UserInfoImpl)ui).connectionMade();
  }
  
  @Override
  public Proxy getProxyForHost(String host, String proxyType) {
    return Utils.getProxyForHost(host, proxyType);
  }

  public static IJSchService getInstance(){
    if (instance == null)
      instance = new JSchProvider();
    return instance;
  }

  @Override
  public void connect(Proxy proxy, String host, int port, int timeout,
      IProgressMonitor monitor) throws JSchException {
    try{
      proxy.connect(new ResponsiveSocketFactory(monitor, timeout), host, port, timeout);
    }
    catch(JSchException e){
      throw e;
    }
    catch(Exception e){
      new JSchException(e.getMessage());
    }
  }
  
  /**
   * Search for an instance of IUserAuthenticator provided from other plug-in.
   * @see org.eclipse.jsch.internal.ui.authenticator.WorkbenchUserAuthenticator
   * @return an instance of IUserAuthenticator.
   */
  private IUserAuthenticator getPluggedInAuthenticator(){
    IExtension[] extensions=Platform.getExtensionRegistry().getExtensionPoint(
        JSchCorePlugin.ID, JSchCorePlugin.PT_AUTHENTICATOR).getExtensions();
    if(extensions.length==0)
      return null;
    IExtension extension=extensions[0];
    IConfigurationElement[] configs=extension.getConfigurationElements();
    if(configs.length==0){
      JSchCorePlugin
          .log(
              IStatus.ERROR,
              NLS
                  .bind(
                      "User autheticator {0} is missing required fields", (new Object[] {extension.getUniqueIdentifier()})), null);//$NON-NLS-1$ 
      return null;
    }
    try{
      IConfigurationElement config=configs[0];
      return (IUserAuthenticator)config.createExecutableExtension("run");//$NON-NLS-1$ 
    }
    catch(CoreException ex){
      JSchCorePlugin
          .log(
              IStatus.ERROR,
              NLS
                  .bind(
                      "Unable to instantiate user authenticator {0}", (new Object[] {extension.getUniqueIdentifier()})), ex);//$NON-NLS-1$ 
      return null;
    }
  }
  
  @Override
  public JSch getJSch(){
    return JSchCorePlugin.getPlugin().getJSch();
  }

  private boolean isAuthenticationFailure(JSchException ee){
    return ee.getMessage().equals("Auth fail"); //$NON-NLS-1$
  }
  
  private boolean hasPromptExceededTimeout(Session session){
    if(session.getUserInfo()==null || !(session.getUserInfo() instanceof UserInfoImpl))
      return false;
    return ((UserInfoImpl)session.getUserInfo()).hasPromptExceededTimeout();
  }

  @Override
  public IJSchLocation getLocation(String user, String host, int port){
    IJSchLocation location=null;
    location=new JSchLocation(user, host, port);
    return location;
  }
}
