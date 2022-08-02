/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Atsuhiko Yamanaka, JCraft,Inc. - initial API and implementation.
 *     IBM Corporation - ongoing maintenance
 *******************************************************************************/
package org.eclipse.jsch.internal.core;

import java.io.File;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;

public class PreferenceInitializer extends AbstractPreferenceInitializer{

  public static String SSH_HOME_DEFAULT=null;

  public static String SSH_OLD_WIN32_HOME_DEFAULT=null;

  static{
    SSH_HOME_DEFAULT=System.getProperty(IConstants.SYSTEM_PROPERTY_USER_HOME);
    if(SSH_HOME_DEFAULT!=null){
      SSH_OLD_WIN32_HOME_DEFAULT=SSH_HOME_DEFAULT+File.separator
          +IConstants.SSH_OLD_DEFAULT_WIN32_HOME;
      SSH_HOME_DEFAULT=SSH_HOME_DEFAULT+File.separator
          +IConstants.SSH_DEFAULT_HOME;
    }
  }

  @Override
  public void initializeDefaultPreferences(){
    IEclipsePreferences defaultNode=DefaultScope.INSTANCE
        .getNode(JSchCorePlugin.ID);
    if(SSH_HOME_DEFAULT!=null)
      defaultNode.put(IConstants.KEY_SSH2HOME, SSH_HOME_DEFAULT);
    defaultNode.put(IConstants.KEY_PRIVATEKEY, IConstants.PRIVATE_KEYS_DEFAULT);
    changeDefaultWin32SshHome();
    Utils.migrateSSH2Preferences();
  }

  private void changeDefaultWin32SshHome(){
    if(!Platform.getOS().equals(Platform.OS_WIN32))
      return;

    IEclipsePreferences preferences=InstanceScope.INSTANCE
        .getNode(JSchCorePlugin.ID);

    // flag to check if the win32 default ssh home was alrady changed
    boolean defaultWin32SshHomeChanged=preferences.getBoolean(
        IConstants.PREF_HAS_CHANGED_DEFAULT_WIN32_SSH_HOME, false);

    // flag to check if it is an existing workspace
    // TODO bug 334508 needs to be fixed to determine that we are on an existing workspace
    boolean existingWorkspace=true;

    if(!defaultWin32SshHomeChanged){
      if(null==preferences.get(IConstants.KEY_SSH2HOME, null)){
        if(SSH_OLD_WIN32_HOME_DEFAULT!=null
            &&new File(SSH_OLD_WIN32_HOME_DEFAULT).exists()){
          if(!(SSH_HOME_DEFAULT!=null&&new File(SSH_HOME_DEFAULT).exists())
              ||existingWorkspace)
            preferences
                .put(IConstants.KEY_SSH2HOME, SSH_OLD_WIN32_HOME_DEFAULT);
        }
      }

      preferences.putBoolean(
          IConstants.PREF_HAS_CHANGED_DEFAULT_WIN32_SSH_HOME, true);
      try{
        preferences.flush();
      }
      catch(BackingStoreException e){
        JSchCorePlugin.log(new Status(IStatus.INFO, JSchCorePlugin.ID,
            "Could not flush preferences.", e)); //$NON-NLS-1$
      }
    }
  }
}
