
###	Public Key Infrastructure Core Package

The comprehensive system required to provide public key encryption and digital signature services is known as a **Public Key Infrastructure (PKI)**. The purpose of a PKI is to manage keys and certificates. By managing keys and certificates through a PKI, an organization establishes and maintains a trustworthy networking environment \[^1].

This package is implemented in Java programming language and allows a user to setup an SSLContext to use inside the Eclipse architecture.  Using this core package there are two ways to initialize the PKI setup, (additionally,  A UI is forthcoming org.eclipse.pki.ui ), The preferred option is specified in the 2nd paragraph below. That option takes a clear text password and allows the core pki package to encrypt it for any subsequent usage.

1)  Add the following properties to the eclipse.ini or similar start up.

	-Djavax.net.ssl.keyStoreType=PKCS12 ( or specify PKCS11 )
	-Djavax.net.ssl.keyStorePassword=Clear Text Password
	-Djavax.net.ssl.keyStore=/home/user/Certificates/your_org.p12 ( or specify NONE for PKCS11 )

	-Djavax.net.ssl.trustStorePassword=changeit
	-Djavax.net.ssl.trustStore=/etc/pki/java/cacerts
	-Djavax.net.ssl.trustStoreType=JKS
	-Djavax.net.ssl.cfgFileLocation=NONE ( or for PKCS11; /etc/opensc/pkcs11_java.cfg )
	-Djavax.net.debug=keymanager

2)  Create a file called .pki inside of your user home .eclipse directory.  Once you startup eclipse it will create a template in your .eclipse directory that you can update with your personal PKI setup. You can use either a PKCS11 or PKCS12 KeyStore setup.  The .pki file for a PKCS12 setup should look similar to the following;

	javax.net.ssl.trustStore=/etc/pki/java/cacerts
	javax.net.ssl.trustStoreType=JKS
	javax.net.ssl.trustStorePassword=changeit
	
	javax.net.ssl.keyStore=/home/user/Certificates/your_org.p12 ( or specify NONE for PKCS11 )
	javax.net.ssl.keyStoreType=PKCS12 ( or specify PKCS11 )
	javax.net.ssl.keyStorePassword=Clear Text Password ( Eclipse will encrypt on startup )
	
	javax.net.ssl.keyStoreProvider=PKCS12 ( or for PKCS11 use; SunPKCS11 )
	javax.net.ssl.cfgFileLocation=NONE ( or for PKCS11; /etc/opensc/pkcs11_java.cfg )

 
 \[^1]: *[PKI definition: Eclipse Foundation Cyclone](https://cyclonedds.io/docs/cyclonedds/latest/security/public_key_infrastructure.html)*
	