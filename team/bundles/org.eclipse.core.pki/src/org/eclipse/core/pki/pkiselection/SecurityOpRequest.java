/**
 * Copyright (c) 2014 Codetrails GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Delmarva Security -  implementation.
 */
package org.eclipse.core.pki.pkiselection;

public enum SecurityOpRequest {
	INSTANCE;
	public boolean isConnected=false;
	
	public boolean getConnected() {
		return isConnected;	
	}
	public void setConnected(boolean b) {
		isConnected=b;	
	}
}
