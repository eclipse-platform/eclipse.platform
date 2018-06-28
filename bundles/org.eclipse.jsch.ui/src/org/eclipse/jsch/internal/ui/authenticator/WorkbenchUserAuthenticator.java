/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Atsuhiko Yamanaka, JCraft,Inc. - implementation of promptForKeyboradInteractive
 *     Atsuhiko Yamanaka, JCraft,Inc. - copying this class from o.e.team.cvs.ui plug-in.
 *******************************************************************************/

package org.eclipse.jsch.internal.ui.authenticator;

import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.dialogs.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.jsch.core.IJSchLocation;
import org.eclipse.jsch.internal.core.IUserAuthenticator;
import org.eclipse.jsch.internal.core.IUserInfo;
import org.eclipse.jsch.internal.ui.Messages;

/**
 * An authenticator that prompts the user for authentication info,
 * and stores the results in the Platform's authentication key-ring.
 * @since 1.1
 */
public class WorkbenchUserAuthenticator implements IUserAuthenticator{
  /**
   * WorkbenchUserAuthenticator constructor.
   */
  public WorkbenchUserAuthenticator(){
    super();
  }

  /**
   * @see IUserAuthenticator#promptForUserInfo(IJSchLocation, IUserInfo, String)
   */
  public void promptForUserInfo(final IJSchLocation location,
      final IUserInfo userinfo, final String message){

    // ask the user for a password
    final String[] result=new String[2];
    Display display=Display.getCurrent();
    final boolean allowCaching[]= {false};
    if(display!=null){
      allowCaching[0]=promptForPassword(location, userinfo.getUsername(),
          message, userinfo.isUsernameMutable(), result);
    }
    else{
      // sync exec in default thread
      Display.getDefault().syncExec(new Runnable(){
        public void run(){
          allowCaching[0]=promptForPassword(location, userinfo.getUsername(),
              message, userinfo.isUsernameMutable(), result);
        }
      });
    }

    if(result[0]==null){
      throw new OperationCanceledException(
          Messages.WorkbenchUserAuthenticator_cancelled);
    }

    if(userinfo.isUsernameMutable()){
      userinfo.setUsername(result[0]);

    }
    userinfo.setPassword(result[1]);

    if(location!=null){
      if(userinfo.isUsernameMutable()){
        location.setUsername(result[0]);
      }
      location.setPassword(result[1]);
      if(location.getPasswordStore()!=null){
        if(allowCaching[0])
          location.getPasswordStore().update(location);
        else
          location.getPasswordStore().clear(location);
      }
    }
  }

  /**
   * Asks the user to enter a password. Places the
   * results in the supplied string[].  result[0] must
   * contain the username, result[1] must contain the password.
   * If the user canceled, both values must be zero.
   *
   * @param location  the location to obtain the password for
   * @param username  the username
   * @param message  a message to display to the user
   * @param userMutable  whether the user can be changed in the dialog
   * @param result  a String array of length two in which to put the result
   */
  protected boolean promptForPassword(final IJSchLocation location,
      final String username, final String message, final boolean userMutable,
      final String[] result){
    String comment=location==null ? null : location.getComment();
    UserValidationDialog dialog=new UserValidationDialog(null, comment,
        (username==null) ? "" : username, message, (location!=null && location.getPasswordStore()!=null));//$NON-NLS-1$
    dialog.setUsernameMutable(userMutable);
    dialog.open();
    result[0]=dialog.getUsername();
    result[1]=dialog.getPassword();
    return dialog.getAllowCaching();
  }

  /**
   * Asks the user to enter values.
   *
   * @param location  the location to obtain the password for
   * @param destination the location
   * @param name the name
   * @param instruction the instruction
   * @param prompt the titles for text fields
   * @param echo '*' should be used or not
   * @return the entered values, or null if user canceled.
   */
  public String[] promptForKeyboradInteractive(
      final IJSchLocation location, final String destination,
      final String name, final String instruction, final String[] prompt,
      final boolean[] echo){
    final String[][] result=new String[1][];
    final boolean[] allowCaching=new boolean[1];
    Display display=Display.getCurrent();
    if(display!=null){
      result[0]=_promptForUserInteractive(location, destination, name,
          instruction, prompt, echo, allowCaching);
    }
    else{
      // sync exec in default thread
      Display.getDefault().syncExec(new Runnable(){
        public void run(){
          result[0]=_promptForUserInteractive(location, destination, name,
              instruction, prompt, echo, allowCaching);
        }
      });
    }
    if(result[0]!=null && location!=null &&
        prompt!=null && prompt.length==1 && prompt[0].trim().equalsIgnoreCase("password:")){ //$NON-NLS-1$
      location.setPassword(result[0][0]);
      if(location.getPasswordStore()!=null){
        if(allowCaching[0])
          location.getPasswordStore().update(location);
        else
          location.getPasswordStore().clear(location);
      }
    }
    return result[0];
  }

  protected String[] _promptForUserInteractive(
      final IJSchLocation location, final String destination,
      final String name, final String instruction, final String[] prompt,
      final boolean[] echo, final boolean[] allowCaching){
    String comment=location==null ? null : location.getComment();
    String username=location==null ? "" : location.getUsername(); //$NON-NLS-1$
    KeyboardInteractiveDialog dialog=new KeyboardInteractiveDialog(null,
        comment, destination, name, username, instruction, prompt, echo);
    dialog.setUsernameMutable(false);
    dialog.open();
    String[] _result=dialog.getResult();
    if(_result!=null)
      allowCaching[0]=dialog.getAllowCaching();
    return _result;
  }


  /* (non-Javadoc)
   * @see org.eclipse.team.internal.ccvs.core.IUserAuthenticator#prompt(org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation, int, java.lang.String, java.lang.String, int[], int)
   */
  public int prompt(IJSchLocation location, final int promptType,
      final String title, final String message, final int[] promptResponses,
      final int defaultResponse){
    final Display display=getStandardDisplay();
    final int[] retval=new int[1];
    final String[] buttons=new String[promptResponses.length];
    for(int i=0; i<promptResponses.length; i++){
      int prompt=promptResponses[i];
      switch(prompt){
        case IUserAuthenticator.OK_ID:
          buttons[i]=IDialogConstants.OK_LABEL;
          break;
        case IUserAuthenticator.CANCEL_ID:
          buttons[i]=IDialogConstants.CANCEL_LABEL;
          break;
        case IUserAuthenticator.NO_ID:
          buttons[i]=IDialogConstants.NO_LABEL;
          break;
        case IUserAuthenticator.YES_ID:
          buttons[i]=IDialogConstants.YES_LABEL;
          break;
      }
    }

    display.syncExec(new Runnable(){
      public void run(){
        final MessageDialog dialog=new MessageDialog(new Shell(display), title,
            null, message, promptType, buttons, 1);
        retval[0]=dialog.open();
      }
    });
    return retval[0];
  }

  public boolean promptForHostKeyChange(final IJSchLocation location){
    final boolean[] openConfirm=new boolean[] {false};
    final Display display=getStandardDisplay();
    display.syncExec(new Runnable(){
      public void run(){
        openConfirm[0]=MessageDialog.openConfirm(null,
            Messages.WorkbenchUserAuthenticator_1, NLS.bind(
                Messages.WorkbenchUserAuthenticator_2, new String[] {location
                    .getHost()})); //
      }
    });
    if(!openConfirm[0]){
      throw new OperationCanceledException();
    }
    return openConfirm[0];
  }

  /**
   * Returns the standard display to be used. The method first checks, if
   * the thread calling this method has an associated display. If so, this
   * display is returned. Otherwise the method returns the default display.
   * @return standard display
   */
  private Display getStandardDisplay(){
    Display display=Display.getCurrent();
    if(display==null){
      display=Display.getDefault();
    }
    return display;
  }
}
