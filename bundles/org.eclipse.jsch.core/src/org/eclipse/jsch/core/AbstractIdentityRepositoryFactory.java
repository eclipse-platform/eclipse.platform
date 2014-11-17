/*******************************************************************************
 * Copyright (c) 2014 JCraft,Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     JCraft,Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jsch.core;

import com.jcraft.jsch.IdentityRepository;

/**
 * This class abstracts the communications with the identity repository,
 * and will be mainly used for ssh-agent.
 * @since 1.2
 */
public abstract class AbstractIdentityRepositoryFactory{
  /**
   * This method will return an instance of
   * <code>com.jcraft.com.jsch.IdentityRepository</code>. The ssh client will
   * retrieve public keys from it, ask for signing data with a private key
   * included in it.
   * 
   * @return an instance of <code>IdentityRepository</code>
   */
  public abstract IdentityRepository create();
}
