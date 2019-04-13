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
 *     Atsuhiko Yamanaka, JCraft,Inc. - copying this class from o.e.team.cvs.ssh2.JSchSession.
 *******************************************************************************/
package org.eclipse.jsch.internal.core;

import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jsch.core.IJSchLocation;

import com.jcraft.jsch.UIKeyboardInteractive;

/**
 * User information delegates to the IUserAuthenticator. This allows
 * head-less access to the connection method.
 * @since 1.1
 */
class UserInfoImpl implements com.jcraft.jsch.UserInfo, UIKeyboardInteractive{
  private String username;
  private String password;
  private String passphrase;
  private IJSchLocation location;
  private IUserAuthenticator authenticator;
  private int attemptCount;
  private boolean passwordChanged;

  private long startTime;
  private long endTime;
  private boolean prompting;
  private long timeout;
  
  private int reuse=0;
  
  UserInfoImpl(IJSchLocation location, IUserAuthenticator authenticator, long timeout){
    this.location=location;
    this.username=location.getUsername();
    this.password=location.getPassword();
    this.authenticator=authenticator;
    this.timeout=timeout;
  }

  @Override
  public String getPassword(){
    return password;
  }

  @Override
  public String getPassphrase(){
    return passphrase;
  }

  @Override
  public boolean promptYesNo(String str){
    int prompt=authenticator.prompt(location, IUserAuthenticator.QUESTION,
        Messages.JSchSession_5, str, new int[] {IUserAuthenticator.YES_ID,
            IUserAuthenticator.NO_ID}, 0 //yes the default
        );
    return prompt==0;
  }

  private String promptSecret(String message, boolean includeLocation){
    final String[] _password=new String[1];
    final String username=location.getUsername();
    IUserInfo info=new IUserInfo(){
      @Override
      public String getUsername(){
        return username;
      }

      @Override
      public boolean isUsernameMutable(){
        return false;
      }

      @Override
      public void setPassword(String password){
        _password[0]=password;
      }

      @Override
      public void setUsername(String username){
        //
      }
    };
    try{
      authenticator.promptForUserInfo(includeLocation ? location : null,
          info, message);
    }
    catch(OperationCanceledException e){
      _password[0]=null;
    }
    return _password[0];
  }

  @Override
  public boolean promptPassphrase(String message){
    try{
      startTimer();
      String _passphrase=promptSecret(message, false);
      if(_passphrase!=null){
        passphrase=_passphrase;
      }
      return _passphrase!=null;
    }
    catch(OperationCanceledException e){
      // The prompt was canceled, but the next authentication
      // method should be tried.
      return false;
    }
    finally{
      endTimer();
    }
  }

  @Override
  public boolean promptPassword(String message){
    try{
      startTimer();
      
      String _password=promptSecret(message, true);
      if(_password!=null){
        password=_password;
        // Cache the password with the repository location on the memory.
        if(location!=null)
          location.setPassword(password);
      }
      return _password!=null;
    }
    finally{
      endTimer();
    }
  }

  @Override
  public void showMessage(String message){
    authenticator.prompt(location, IUserAuthenticator.INFORMATION,
        Messages.JSchSession_5, message,
        new int[] {IUserAuthenticator.OK_ID}, IUserAuthenticator.OK_ID);
  }

  @Override
  public String[] promptKeyboardInteractive(String destination, String name,
      String instruction, String[] prompt, boolean[] echo){
    if(prompt.length==0){
      // No need to prompt, just return an empty String array
      return new String[0];
    }
    try{
      startTimer();
      
      if(attemptCount==0&&password!=null&&prompt.length==1
          &&prompt[0].trim().equalsIgnoreCase("password:")){ //$NON-NLS-1$
        // Return the provided password the first time but always prompt on subsequent tries
        attemptCount++;
        return new String[] {password};
      }
      String[] result=authenticator.promptForKeyboradInteractive(location,
          destination, name, instruction, prompt, echo);
      if(result==null)
        return null; // canceled
      if(result.length==1&&prompt.length==1
          &&prompt[0].trim().equalsIgnoreCase("password:")){ //$NON-NLS-1$
        password=result[0];
        passwordChanged=true;
      }
      attemptCount++;
      return result;
    }
    catch(OperationCanceledException e){
      return null;
    }
    finally{
      endTimer();
    }
  }

  /**
   * Callback to indicate that a connection is about to be attempted
   */
  public void aboutToConnect(){
    attemptCount=0;
    passwordChanged=false;
  }

  /**
   * Callback to indicate that a connection was made
   */
  public void connectionMade(){
    attemptCount=0;
    if(passwordChanged&&password!=null&&location!=null){
      // We were prompted for and returned a password so record it with the location
      location.setPassword(password);
    }
  }
  
  private synchronized void startTimer(){
    prompting=true;
    startTime=System.currentTimeMillis();
  }

  private synchronized void endTimer(){
    prompting=false;
    endTime=System.currentTimeMillis();
  }
  
  public long getLastDuration(){
    return Math.max(0, endTime-startTime);
  }

  public boolean hasPromptExceededTimeout(){
    if(!isPrompting()){
      return getLastDuration()>timeout;
    }
    return false;
  }
  
  public boolean isPrompting(){
    return prompting;
  }

  public synchronized int incReuse(){
    return reuse++;
  }
  
  String getUsername(){
    return username;
  }
}
