/*******************************************************************************
 * Copyright (c) 2007, 2019 JCraft,Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Atsuhiko Yamanaka, JCraft,Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jsch.internal.core;

import org.eclipse.core.runtime.*;
import org.eclipse.jsch.core.IJSchLocation;
import org.eclipse.jsch.core.IPasswordStore;

/**
 * This class implements IJSchLocation interface.
 * @since 1.1
 */
public class JSchLocation extends PlatformObject implements IJSchLocation{
  /**
   * port value which indicates to a connection method to use the default port
   */
  private static int DEFAULT_PORT=22;

  private String user;
  private String password;
  private String host;
  private int port=DEFAULT_PORT;
  private boolean userFixed=true;
  private String comment=null;
  private IPasswordStore passwordStore=null;

  /*
   * Create a JSchLocation from its composite parts.
   */
  public JSchLocation(String user, String host, int port){
    this.user=user;
    this.host=host;
    this.port=port;
  }

  public JSchLocation(String user, String host){
    this(user, host, DEFAULT_PORT);
  }

  @Override
  public String getHost(){
    return host;
  }

  @Override
  public int getPort(){
    return port;
  }

  @Override
  public void setUsername(String user){
    if(userFixed)
      throw new UnsupportedOperationException();
    this.user=user;
  }

  @Override
  public String getUsername(){
    return user==null ? "" : user; //$NON-NLS-1$
  }

  @Override
  public void setPassword(String password){
    if(password!=null)
      this.password=password;
  }

  @Override
  public String getPassword(){
    return password;
  }

  @Override
  public void setComment(String comment){
    this.comment=comment;
  }

  @Override
  public String getComment(){
    return comment;
  }

  @Override
  public void setPasswordStore(IPasswordStore store){
    this.passwordStore=store;
  }

  @Override
  public IPasswordStore getPasswordStore(){
    return passwordStore;
  }

  @Override
  public String toString(){
    return user
        +"@"+host+((port==DEFAULT_PORT) ? "" : ":"+(Integer.valueOf(port).toString())); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
  }
}
