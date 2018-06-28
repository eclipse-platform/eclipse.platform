/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jsch.ui;

import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jsch.core.IJSchService;
import org.eclipse.jsch.internal.ui.*;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.jcraft.jsch.*;

/**
 * A {@link UserInfo} prompter implementation that can be used when connecting a
 * {@link Session}.
 * <p>
 * Clients may instantiate or subclass this class.
 *
 * @since 1.0
 * @see IJSchService#createSession(String, int, String)
 * @see IJSchService#connect(Session, int,
 *      org.eclipse.core.runtime.IProgressMonitor)
 */
public class UserInfoPrompter implements UserInfo, UIKeyboardInteractive{

  private String passphrase;
  private String password;
  private final Session session;
  private int attemptCount;

  /**
   * Create a prompter for the given session. This constructor will associate
   * this prompter with the session using {@link Session#setUserInfo(UserInfo)}.
   *
   * @param session
   *          the session
   */
  public UserInfoPrompter(Session session){
    super();
    this.session=session;
    session.setUserInfo(this);
  }

  /**
   * Return the session to which this prompter is assigned.
   *
   * @return the session to which this prompter is assigned
   */
  public Session getSession(){
    return session;
  }

  /*
   * (non-Javadoc)
   *
   * @see com.jcraft.jsch.UserInfo#getPassphrase()
   */
  public String getPassphrase(){
    return passphrase;
  }

  /*
   * (non-Javadoc)
   *
   * @see com.jcraft.jsch.UserInfo#getPassword()
   */
  public String getPassword(){
    return password;
  }

  /**
   * Set the pass phrase to be used when connecting the session. Return
   * <code>null</code> if the pass phrase is not known.
   *
   * @param passphrase
   *          the pass phrase to be used when connecting the session or
   *          <code>null</code>
   */
  public void setPassphrase(String passphrase){
    this.passphrase=passphrase;
  }

  /**
   * Set the password to be used when connecting the session. Return
   * <code>null</code> if the password is not known.
   *
   * @param password
   *          the password to be used when connecting the session or
   *          <code>null</code>
   */
  public void setPassword(String password){
    this.password=password;
  }

  /*
   * (non-Javadoc)
   *
   * @see com.jcraft.jsch.UserInfo#promptPassphrase(java.lang.String)
   */
  public boolean promptPassphrase(String message){
    String _passphrase=promptSecret(message);
    if(_passphrase!=null){
      setPassphrase(_passphrase);
    }
    return _passphrase!=null;
  }

  /*
   * (non-Javadoc)
   *
   * @see com.jcraft.jsch.UserInfo#promptPassword(java.lang.String)
   */
  public boolean promptPassword(String message){
    String _password=promptSecret(message);
    if(_password!=null){
      setPassword(_password);
    }
    return _password!=null;
  }

  private String promptSecret(final String message){
    // ask the user for a password
    final String[] result=new String[1];
    Display display=Display.getCurrent();
    if(display!=null){
      result[0]=promptForPassword(message);
    }
    else{
      // sync exec in default thread
      Display.getDefault().syncExec(new Runnable(){
        public void run(){
          result[0]=promptForPassword(message);
        }
      });
    }

    if(result[0]==null){
      throw new OperationCanceledException();
    }
    return result[0];
  }

  /* package */String promptForPassword(final String message){
    String username=getSession().getUserName();
    UserValidationDialog dialog=new UserValidationDialog(null, null,
        (username==null) ? "" : username, message); //$NON-NLS-1$
    dialog.setUsernameMutable(false);
    dialog.open();
    return dialog.getPassword();
  }

  /*
   * (non-Javadoc)
   *
   * @see com.jcraft.jsch.UIKeyboardInteractive#promptKeyboardInteractive(java.lang.String,
   *      java.lang.String, java.lang.String, java.lang.String[], boolean[])
   */
  public String[] promptKeyboardInteractive(String destination, String name,
      String instruction, String[] prompt, boolean[] echo){
    if(prompt.length==0){
      // No need to prompt, just return an empty String array
      return new String[0];
    }
    try{
      if(attemptCount==0&&password!=null&&prompt.length==1
          &&prompt[0].trim().equalsIgnoreCase("password:")){ //$NON-NLS-1$
        // Return the provided password the first time but always prompt on
        // subsequent tries
        attemptCount++;
        return new String[] {password};
      }
      String[] result=promptForKeyboradInteractiveInUI(destination, name,
          instruction, prompt, echo);
      if(result==null)
        return null; // canceled
      if(result.length==1&&prompt.length==1
          &&prompt[0].trim().equalsIgnoreCase("password:")){ //$NON-NLS-1$
        password=result[0];
      }
      attemptCount++;
      return result;
    }
    catch(OperationCanceledException e){
      return null;
    }
  }

  private String[] promptForKeyboradInteractiveInUI(final String destination,
      final String name, final String instruction, final String[] prompt,
      final boolean[] echo){
    final String[][] result=new String[1][];
    Display display=Display.getCurrent();
    if(display!=null){
      result[0]=internalPromptForUserInteractive(destination, name,
          instruction, prompt, echo);
    }
    else{
      // sync exec in default thread
      Display.getDefault().syncExec(new Runnable(){
        public void run(){
          result[0]=internalPromptForUserInteractive(destination, name,
              instruction, prompt, echo);
        }
      });
    }
    return result[0];
  }

  /* package */ String[] internalPromptForUserInteractive(String destination,
      String name, String instruction, String[] prompt, boolean[] echo){
    String domain=null;
    String userName=getSession().getUserName();
    KeyboardInteractiveDialog dialog=new KeyboardInteractiveDialog(null,
        domain, destination, name, userName, instruction, prompt, echo);
    dialog.open();
    String[] _result=dialog.getResult();
    return _result;
  }

  /*
   * (non-Javadoc)
   *
   * @see com.jcraft.jsch.UserInfo#promptYesNo(java.lang.String)
   */
  public boolean promptYesNo(String question){
    int prompt=prompt(MessageDialog.QUESTION, Messages.UserInfoPrompter_0,
        question, new int[] {IDialogConstants.YES_ID, IDialogConstants.NO_ID},
        0 // yes
    // the
    // default
    );
    return prompt==0;
  }

  /*
   * (non-Javadoc)
   *
   * @see com.jcraft.jsch.UserInfo#showMessage(java.lang.String)
   */
  public void showMessage(String message){
    prompt(MessageDialog.INFORMATION, Messages.UserInfoPrompter_1, message,
        new int[] {IDialogConstants.OK_ID}, 0);
  }

  private int prompt(final int promptType, final String title,
      final String message, final int[] promptResponses,
      final int defaultResponse){
    final Display display=getStandardDisplay();
    final int[] retval=new int[1];
    final String[] buttons=new String[promptResponses.length];
    for(int i=0; i<promptResponses.length; i++){
      int prompt=promptResponses[i];
      switch(prompt){
        case IDialogConstants.OK_ID:
          buttons[i]=IDialogConstants.OK_LABEL;
          break;
        case IDialogConstants.CANCEL_ID:
          buttons[i]=IDialogConstants.CANCEL_LABEL;
          break;
        case IDialogConstants.NO_ID:
          buttons[i]=IDialogConstants.NO_LABEL;
          break;
        case IDialogConstants.YES_ID:
          buttons[i]=IDialogConstants.YES_LABEL;
          break;
      }
    }

    display.syncExec(new Runnable(){
      public void run(){
        final MessageDialog dialog=new MessageDialog(new Shell(display), title,
            null /* title image */, message, promptType, buttons,
            defaultResponse);
        retval[0]=dialog.open();
      }
    });
    return retval[0];
  }

  private Display getStandardDisplay(){
    Display display=Display.getCurrent();
    if(display==null){
      display=Display.getDefault();
    }
    return display;
  }

}
