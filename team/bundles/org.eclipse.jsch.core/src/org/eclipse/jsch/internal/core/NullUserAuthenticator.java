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

import org.eclipse.jsch.core.IJSchLocation;

/**
 * This class will be used whenever nobody gives 
 * the implementation of IUserAuthenticator.
 * @since 1.1
 */
class NullUserAuthenticator implements IUserAuthenticator{

  @Override
  public int prompt(IJSchLocation location, int promptType, String title,
      String message, int[] promptResponses, int defaultResponseIndex){
    return IUserAuthenticator.CANCEL_ID;
  }

  @Override
  public boolean promptForHostKeyChange(IJSchLocation location){
    return false;
  }

  @Override
  public String[] promptForKeyboradInteractive(IJSchLocation location,
      String destination, String name, String instruction, String[] prompt,
      boolean[] echo){
    return null;
  }

  @Override
  public void promptForUserInfo(IJSchLocation location, IUserInfo userInfo,
      String message){
    // no operation
  }
}
