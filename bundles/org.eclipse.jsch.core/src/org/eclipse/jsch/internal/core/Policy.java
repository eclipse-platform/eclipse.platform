/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jsch.internal.core;

import org.eclipse.core.runtime.*;

public class Policy{

  public static void checkCanceled(IProgressMonitor monitor){
    if(monitor.isCanceled())
      throw new OperationCanceledException();
  }

  public static IProgressMonitor monitorFor(IProgressMonitor monitor){
    if(monitor==null)
      return new NullProgressMonitor();
    return monitor;
  }

  public static IProgressMonitor subMonitorFor(IProgressMonitor monitor,
      int ticks){
    if(monitor==null)
      return new NullProgressMonitor();
    if(monitor instanceof NullProgressMonitor)
      return monitor;
    return SubMonitor.convert(monitor, ticks);
  }
  
}
