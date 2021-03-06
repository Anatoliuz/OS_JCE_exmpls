package com.example.rushd.jceapplication;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Date;
import java.sql.Timestamp;
import java.math.*;

import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;


public class rsa {

    public static String getHexString(byte[] b) throws Exception {
        String result = "";
        for (int i=0; i < b.length; i++) {
            result +=
                    Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
        }
        return result;
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static void GetTimestamp(String info){
        System.out.println(info + new Timestamp((new Date()).getTime()));
    }

    public static AsymmetricCipherKeyPair GenerateKeys() throws NoSuchAlgorithmException{
        RSAKeyPairGenerator generator = new RSAKeyPairGenerator();
        generator.init(new RSAKeyGenerationParameters
                (
                        new BigInteger("10001", 16),
                        SecureRandom.getInstance("SHA1PRNG"),
                        4096,//strength
                        80//certainty
                ));

        return generator.generateKeyPair();
    }

    public static String Encrypt(byte[] data, AsymmetricKeyParameter publicKey) throws Exception{
        Security.addProvider(new BouncyCastleProvider());

        RSAEngine engine = new RSAEngine();
        engine.init(true, publicKey); //true if encrypt

        byte[] hexEncodedCipher = engine.processBlock(data, 0, data.length);

        return getHexString(hexEncodedCipher);
    }

    public static String Decrypt(String encrypted, AsymmetricKeyParameter privateKey) throws InvalidCipherTextException{
        Security.addProvider(new BouncyCastleProvider());

        AsymmetricBlockCipher engine = new RSAEngine();
        engine.init(false, privateKey); //false for decryption

        byte[] encryptedBytes = hexStringToByteArray(encrypted);
        byte[] hexEncodedCipher = engine.processBlock(encryptedBytes, 0, encryptedBytes.length);

        return new String (hexEncodedCipher);
    }


}