package org.eclipse.ui.internal.util;

public enum KeyStoreFormat
{
	JKS("JKS"), //$NON-NLS-1$
	PKCS12("PKCS12"), //$NON-NLS-1$
	PKCS11("PKCS11"); //$NON-NLS-1$

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
