/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Atsuhiko Yamanaka, JCraft,Inc. - initial API and implementation.
 *     IBM Corporation - ongoing maintenance
 *******************************************************************************/
package org.eclipse.jsch.internal.core;

import java.util.Hashtable;
import java.util.ArrayList;

import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.*;
import org.eclipse.jsch.core.AbstractIdentityRepositoryFactory;
import org.eclipse.jsch.core.IJSchService;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import com.jcraft.jsch.IdentityRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;

public class JSchCorePlugin extends Plugin{

  public static String ID="org.eclipse.jsch.core"; //$NON-NLS-1$

  // communication timeout with the server
  public static final int DEFAULT_TIMEOUT=60;
  private int communicationsTimeout=DEFAULT_TIMEOUT;
  private boolean needToLoadKnownHosts=true;
  private boolean needToLoadKeys=true;

  private JSch jsch;

  private String current_pkeys=""; //$NON-NLS-1$

  public static final String PT_AUTHENTICATOR="authenticator"; //$NON-NLS-1$
  public static final String PT_IDENTITYREPOSITORY="identityrepository"; //$NON-NLS-1$

  private static JSchCorePlugin plugin;
  private ServiceTracker tracker;

  private ServiceRegistration jschService;

  public JSchCorePlugin(){
    plugin=this;
  }

  public static JSchCorePlugin getPlugin(){
    return plugin;
  }

  /**
   * Convenience method for logging CoreExceptions to the plugin log
   * 
   * @param e
   *          the exception
   */
  public static void log(CoreException e){
    log(e.getStatus().getSeverity(), e.getMessage(), e);
  }

  /**
   * Log the given status. Do not use this method for the IStatus from a
   * CoreException. Use<code>log(CoreException)</code> instead so the stack
   * trace is not lost.
   * 
   * @param status
   *          the status
   */
  public static void log(IStatus status){
    getPlugin().getLog().log(status);
  }

  public static void log(int severity, String message, Throwable e){
    log(new Status(severity, ID, 0, message, e));
  }

  /**
   * Get the communications timeout value in seconds
   * 
   * @return the timeout value in seconds
   */
  public int getTimeout(){
    return communicationsTimeout;
  }

  /**
   * Set the timeout value for communications to a value in seconds. The value
   * must be greater than or equal 0. If is it 0, there is no timeout.
   * 
   * @param timeout
   *          the timeout value in seconds
   */
  public void setTimeout(int timeout){
    this.communicationsTimeout=Math.max(0, timeout);
  }

  public synchronized JSch getJSch(){
    if(jsch==null){
      jsch=new JSch();
      setIdentityRepository();
    }
    return jsch;
  }

  public synchronized void setIdentityRepository(){

    IdentityRepository[] repositories = getPluggedInIdentityRepositries();
    String[] selected = Utils.getSelectedSSHAgent().split(","); //$NON-NLS-1$
    IdentityRepository irepo = null;

    for(int i=0; i<selected.length; i++){
      for(int j=0; j<repositories.length; j++){
        IdentityRepository _irepo = repositories[j];
        if(selected[i].equals(_irepo.getName()) &&
           _irepo.getStatus()==IdentityRepository.RUNNING){
          irepo = _irepo;
          break;
        }
      }
      if(irepo!=null)
        break;
    }

    if(irepo!=null){
      jsch.setIdentityRepository(irepo);
    }
    else{
      // set the internal default IdentityRepository
      jsch.setIdentityRepository(null);
    }

  }

  public IdentityRepository[] getPluggedInIdentityRepositries(){

    IExtension[] extensions=Platform.getExtensionRegistry().getExtensionPoint(
        JSchCorePlugin.ID, JSchCorePlugin.PT_IDENTITYREPOSITORY).getExtensions();

    if(extensions.length==0)
      return new IdentityRepository[0];

    ArrayList tmp = new ArrayList();
    for(int i=0; i<extensions.length; i++){
      IExtension extension=extensions[i];
      IConfigurationElement[] configs=extension.getConfigurationElements();
      if(configs.length==0){
        JSchCorePlugin
            .log(
                IStatus.ERROR,
                NLS
                    .bind(
                        "IdentityRepository {0} is missing required fields", (new Object[] {extension.getUniqueIdentifier()})), null);//$NON-NLS-1$
        continue;
      }
      try{
        IConfigurationElement config=configs[0];
        AbstractIdentityRepositoryFactory iirf =
            (AbstractIdentityRepositoryFactory)config.createExecutableExtension("run");//$NON-NLS-1$
        tmp.add(iirf.create());
      }
      catch(CoreException ex){
        JSchCorePlugin
            .log(
                IStatus.ERROR,
                NLS
                    .bind(
                        "Unable to instantiate identity repository {0}", (new Object[] {extension.getUniqueIdentifier()})), ex);//$NON-NLS-1$
      }
    }

    IdentityRepository[] repositories = new IdentityRepository[tmp.size()];
    for(int i=0; i<tmp.size(); i++){
      repositories[i]=(IdentityRepository)tmp.get(i);
    }
    return repositories;
  }

  public void loadKnownHosts(){
    Preferences preferences=JSchCorePlugin.getPlugin().getPluginPreferences();
    String ssh_home=preferences.getString(IConstants.KEY_SSH2HOME);

    if(ssh_home.length()==0)
      ssh_home=PreferenceInitializer.SSH_HOME_DEFAULT;

    java.io.File file=new java.io.File(ssh_home, "known_hosts"); //$NON-NLS-1$
    try{
      getJSch().setKnownHosts(file.getPath());
    }
    catch(JSchException e){
      JSchCorePlugin.log(IStatus.ERROR, NLS.bind(
          "An error occurred while loading the know hosts file {0}", file //$NON-NLS-1$
              .getAbsolutePath()), e);
    }
    needToLoadKnownHosts=false;
  }

  public boolean isNeedToLoadKnownHosts(){
    return needToLoadKnownHosts;
  }

  public void setNeedToLoadKnownHosts(boolean needToLoadKnownHosts){
    this.needToLoadKnownHosts=needToLoadKnownHosts;
  }

  public boolean isNeedToLoadKeys(){
    return needToLoadKeys;
  }

  public void setNeedToLoadKeys(boolean needToLoadKeys){
    this.needToLoadKeys=needToLoadKeys;
  }

  public void loadPrivateKeys(){
    current_pkeys=Utils.loadPrivateKeys(getJSch(), current_pkeys);
    setNeedToLoadKeys(false);
  }

  /**
   * Return the {@link IProxyService} or <code>null</code> if the service is
   * not available.
   * 
   * @return the {@link IProxyService} or <code>null</code>
   */
  public IProxyService getProxyService(){
    return (IProxyService)tracker.getService();
  }

  public void start(BundleContext context) throws Exception{
    super.start(context);
    tracker=new ServiceTracker(getBundle().getBundleContext(),
        IProxyService.class.getName(), null);
    tracker.open();
    jschService=getBundle().getBundleContext().registerService(
        IJSchService.class.getName(), JSchProvider.getInstance(),
        new Hashtable());
  }

  public void stop(BundleContext context) throws Exception{
    super.stop(context);
    tracker.close();
    jschService.unregister();
  }
}
