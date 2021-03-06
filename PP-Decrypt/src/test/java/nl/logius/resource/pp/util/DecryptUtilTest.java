package nl.logius.resource.pp.util;

/*
 * This source code is protected by the EUPL version 1.2 and is part of the "PP Decrypt" example package.
 * 
 * Copyright: Logius (2018)
 * @author: Bram van Pelt 
 */
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Security;
import java.util.Map;

import javax.crypto.Cipher;

import org.junit.Test;

public class DecryptUtilTest {
	
	@Test
	public void testDecrypt()
	{	
		try 
		{
			// In dit voorbeeld werken we me langere sleutels dan normaal wordt gebruikt in Java. 
			// Hiervoor moet een aanpassing tijdelijk gemaakt worden aan de java security opties. Dit gebeurd in de bijgevoegde methode
			// Normaal kan hiervoor een patch van Oracle worden geinstalleerd. 
			// voor java 1.8 is dat http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html
			fixKeyLength();
			
			// ook bouncycastle moet worden geregistreerd als JAVA security provider. Normaal wordt dit op de server gedaan voordat de software wordt gerunned.
			Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
			
			
			run2();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void run() throws Exception {
		// laad de sleutels in het geheugen, normaal komen deze uit een keystore
		KeyUtil keys = new KeyUtil(); 
		keys.setIdentityKeyLocation("target/test-classes/p7/ID-4.p7");
		keys.setPseudoKeyLocation("target/test-classes/p7/PD-4.p7");
		keys.setPseudoClosingKeyLocation("target/test-classes/p7/PC-4.p7");
		keys.setPrivatep8("target/test-classes/private.p8");
		keys.init();
		
		//simuleer een EncryptedID (ei) en EncryptedPseudonym (ep)
		String ei = new String ( Files.readAllBytes( Paths.get("target/test-classes/signed/900095222-2-4-I.txt") ) );
		String ep = new String ( Files.readAllBytes( Paths.get("target/test-classes/signed/900095222-2-4-P.txt") ) );
		
		//Pre-load complete, Decrypt de ei en ep
		String simBsn = DecryptUtil.getIdentity(ei,keys.getDecryptKey(), keys.getVerifiers());
		String simPseudo = DecryptUtil.getPseudonym(ep, keys.getPDecryptKey(), keys.getPClosingKey(), keys.getPVerifiers());
		
		//Doe er iets nuttigs mee ;)
		System.out.println("Identity:" + simBsn);
		System.out.println("Pseudo:" + simPseudo);
	}
	
	public static void run2() throws Exception {
		// laad de sleutels in het geheugen, normaal komen deze uit een keystore
		KeyUtil keys = new KeyUtil(); 
		keys.setIdentityKeyLocation("/temp/keys/dv_keys_ID_D_00000003273785290000.p7");
		keys.setPseudoKeyLocation("/temp/keys/dv_keys_PD_D_00000003273785290000.p7");
		keys.setPseudoClosingKeyLocation("/temp/keys/dv_keys_PC_D_00000003273785290000.p7");
		keys.setPrivatep8("/temp/keys/private.p8");
		keys.setIdentityPoint("BAVhvzQkCzzIpoIX7uIfauLa6gPsMZp3Ka4DguHlDt5tGwbLeIsXorAwsSYm8QvNDntoKkqc+HF//OpATfa0028wefZnnMvRwgWqHS1/0t9G");
		keys.setPseudonymPoint("BLHPFwnFpnoTvQm03lMQnLdOSpWOnTpsYfxHPmAg/Y3umIcW2PKuwW3Pk5+VDR1ba2z15114Ib8k62aVwieolneiO6bDOaQGKDzXi/4k1/MX");
		keys.init();
		
		//simuleer een EncryptedID (ei) en EncryptedPseudonym (ep)
		String ei = new String ( Files.readAllBytes( Paths.get("/temp/keys/bsn1.txt") ) );
		String ep = new String ( Files.readAllBytes( Paths.get("/temp/keys/pseudoid1.txt") ) );
		
		//Pre-load complete, Decrypt de ei en ep
		String simBsn = DecryptUtil.getIdentity(ei,keys.getDecryptKey(), keys.getVerifiers());
		String simPseudo = DecryptUtil.getPseudonym(ep, keys.getPDecryptKey(), keys.getPClosingKey(), keys.getPVerifiers());
		
		//Doe er iets nuttigs mee ;)
		System.out.println("Identity:" + simBsn);
		System.out.println("Pseudo:" + simPseudo);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void fixKeyLength() {
	    String errorString = "Failed manually overriding key-length permissions.";
	    int newMaxKeyLength;
	    try {
	        if ((newMaxKeyLength = Cipher.getMaxAllowedKeyLength("AES")) < 256) {
	            Class c = Class.forName("javax.crypto.CryptoAllPermissionCollection");
	            Constructor con = c.getDeclaredConstructor();
	            con.setAccessible(true);
	            Object allPermissionCollection = con.newInstance();
	            Field f = c.getDeclaredField("all_allowed");
	            f.setAccessible(true);
	            f.setBoolean(allPermissionCollection, true);

	            c = Class.forName("javax.crypto.CryptoPermissions");
	            con = c.getDeclaredConstructor();
	            con.setAccessible(true);
	            Object allPermissions = con.newInstance();
	            f = c.getDeclaredField("perms");
	            f.setAccessible(true);
	            ((Map) f.get(allPermissions)).put("*", allPermissionCollection);

	            c = Class.forName("javax.crypto.JceSecurityManager");
	            f = c.getDeclaredField("defaultPolicy");
	            f.setAccessible(true);
	            Field mf = Field.class.getDeclaredField("modifiers");
	            mf.setAccessible(true);
	            mf.setInt(f, f.getModifiers() & ~Modifier.FINAL);
	            f.set(null, allPermissions);

	            newMaxKeyLength = Cipher.getMaxAllowedKeyLength("AES");
	        }
	    } catch (Exception e) {
	        throw new RuntimeException(errorString, e);
	    }
	    if (newMaxKeyLength < 256)
	        throw new RuntimeException(errorString); // hack failed
	}

}
