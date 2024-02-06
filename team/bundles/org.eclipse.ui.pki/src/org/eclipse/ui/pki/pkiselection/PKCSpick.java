/*******************************************************************************
 * Copyright (c) 2023 Eclipse Platform, Security Group and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Eclipse Platform - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.pki.pkiselection;

public class PKCSpick {
	private static final PKCSpick pkcs = new PKCSpick(); 
	private static boolean isPKCS11on=false;
	private static boolean isPKCS12on=false;
	private PKCSpick(){}
	public static PKCSpick getInstance() {return pkcs;}
	public boolean isPKCS11on() {return isPKCS11on;}
	public boolean isPKCS12on() {return isPKCS12on;}
	public void setPKCS11on(boolean isPKCS11on) {PKCSpick.isPKCS11on = isPKCS11on;}
	public void setPKCS12on(boolean isPKCS12on) {PKCSpick.isPKCS12on = isPKCS12on;}
}
