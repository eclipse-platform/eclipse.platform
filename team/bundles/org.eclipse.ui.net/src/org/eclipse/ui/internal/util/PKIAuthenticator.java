package org.eclipse.ui.internal.util;

import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * The PKIAuthenticator is a Singleton class used to load and store a user's
 * Certificate information. It currently supports the PKCS12 format for user
 * keystores.
 *
 * @version 1.0
 * @since 1.0
 */
public class PKIAuthenticator extends java.net.Authenticator {
    private final static PKIAuthenticator INSTANCE = new PKIAuthenticator();

    private String DN = null;
    private String sid = null;
    private String userPassword = null;
    private String keystoreFilename = null;

    private X509Certificate certificate = null;
    private X509Certificate[] certChain = null;
    private KeyStore keystore = null;
    private KeyPair keypair = null;

    private PKIAuthenticator() {
        java.net.Authenticator.setDefault( INSTANCE );
    }

    /**
     * Returns a unique instance of the PKIAuthenticator class.
     *
     * @return <code>PKIAuthenticator</code>
     */
    public synchronized static PKIAuthenticator getInstance() {
        return INSTANCE;
    }

    /**
     * Gets the subject DN of the user certificate.
     *
     * @return DN
     * @uml.property name="dN"
     */
    public String getDN() {
        return DN;
    }

    /**
     * Gets the sid embedded in the subject DN of the user certificate.
     *
     * @return sid
     * @uml.property name="sid"
     */
    public String getSid() {
        return sid;
    }

    /**
     * Gets the password needed to decrypt the PKCS#12 file.
     *
     * @return password
     */
    public String getPassword() {
        return userPassword;
    }

    /**
     * Gets the name of the PKCS#12 file.
     *
     * @return filename
     * @uml.property name="keystoreFilename"
     */
    public String getKeystoreFilename() {
        return keystoreFilename;
    }

    /**
     * Gets the digital signature public key certificate for the user.
     *
     * @return certificate
     */
    public X509Certificate getDSCertificate() {
        return certificate;
    }

    /**
     * Gets the chain of certificates in the PKCS#12 file. The chain begins with
     * the user's digital signature certificate.
     *
     * @return certificate chain (as a defensive copy)
     */
    public X509Certificate[] getCertificateChain() {
        if ( certChain == null )
            return null;

        X509Certificate[] cc = new X509Certificate[ certChain.length ];
        System.arraycopy( certChain, 0, cc, 0, certChain.length );
        return cc;
    }

    /**
     * Gets the contents of the PKCS#12 file as a <code>KeyStore</code>.
     *
     * @return keystore
     * @uml.property name="keystore"
     */
    public KeyStore getKeystore() {
        return keystore;
    }

    /**
     * Gets the user's public and private keys.
     *
     * @return key pair
     * @uml.property name="keypair"
     */
    public KeyPair getKeypair() {
        return keypair;
    }

    /**
     * Gets the user's private key.
     *
     * @return private key
     */
    public PrivateKey getPrivateKey() {
        return keypair.getPrivate();
    }

    /**
     * Gets the user's public key.
     *
     * @return public key
     */
    public PublicKey getPublicKey() {
        return keypair.getPublic();
    }



    /**
	 * Extracts and returns the user's SID from the DN of the the specified
	 * certificate. The DN looks like this: CN=Doe John Keith
	 * jkdoe,OU=D904,OU=OSI,OU=DiD,O=C.A. Gubment,C=CA This algorithm was adapted
	 * from this perl regex: my ($affil, $sid) = ($client_dn =~
	 * /C=(\w\w).*?CN=.*?\(?(\S+?)\)?$/);
	 *
	 * @param certificate The Certificate containing the user's SID.
	 * @return The user's SID.
	 */
    public static String getSid( X509Certificate certificate ) {
        String distinguishedName = certificate.getSubjectX500Principal().getName();
        //System.out.println("distingushed name " + distinguishedName);
		String[] attributes = distinguishedName.split(","); //$NON-NLS-1$

        //
        // pattern to pull out sid from "Doe John Keith jkdoe" The sid may have parens
        // around it which means it's second party.
        //
		Pattern filenamePat = Pattern.compile(".*?\\(?(\\S+?)\\)?$", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$

        for ( int i = 0; i < attributes.length; i++ )
          {
				String[] attribute = attributes[i].split("="); //$NON-NLS-1$
				if (attribute[0].equalsIgnoreCase("CN")) //$NON-NLS-1$
              {
            	//System.out.println(attribute[1]);
                Matcher m = filenamePat.matcher(attribute[1]);
				if (m.matches())
            	  {
            		 //System.out.println("Found it " + m.group(1));
            		 return m.group(1);
            	  }
              }
          }
        return null;
    }


    /**
	 * Extracts and returns the user's affiliation from the DN of the the specified
	 * certificate. The DN looks like this: CN=Doe John Keith
	 * jkdoe,OU=D094,OU=OSI,OU=DiD,O=C.A. Gubment,C=CA This algorithm was adapted
	 * from this perl regex: my ($affil, $sid) = ($client_dn =~
	 * /C=(\w\w).*?CN=.*?\(?(\S+?)\)?$/);
	 *
	 * @param certificate The Certificate containing the user's SID.
	 * @return The user's affiliation CB, NB, AR, UN, UK, etc
	 */
    public static String getAffiliation( X509Certificate certificate ) {
        String distinguishedName = certificate.getSubjectX500Principal().getName();
        //System.out.println("distingushed name " + distinguishedName);
		String[] attributes = distinguishedName.split(","); //$NON-NLS-1$

        for ( int i = 0; i < attributes.length; i++ )
          {
				String[] attribute = attributes[i].split("="); //$NON-NLS-1$

				if (attribute[0].equalsIgnoreCase("C")) //$NON-NLS-1$
              {
            	//System.out.println("Country code is " + attribute[1]);
            	return attribute[1];
              }
          }
        return null;
    }
}
