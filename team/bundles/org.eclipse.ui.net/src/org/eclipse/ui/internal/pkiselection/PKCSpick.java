package org.eclipse.ui.internal.pkiselection;

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
