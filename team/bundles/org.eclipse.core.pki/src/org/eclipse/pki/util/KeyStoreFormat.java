package org.eclipse.pki.util;

public enum KeyStoreFormat
{
	JKS("JKS"),
	PKCS12("PKCS12"),
	PKCS11("PKCS11");
	
	private String value;

	KeyStoreFormat (String value)
	{
		this.value = value;
	}
	
	public String getValue()
	{
		return value;
	}
}
