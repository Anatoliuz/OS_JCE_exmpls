package com.example.rushd.jceapplication;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.digests.GOST3411Digest;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;

import java.io.*;
import java.security.*;
import javax.crypto.*;
import java.util.HashMap;
import java.util.Map;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;

import static com.example.rushd.jceapplication.rsa.*;

public class MainActivity extends AppCompatActivity  implements AdapterView.OnItemSelectedListener {
    private static final String TAG = "MyActivity";
    private String entry = "MD5";

    private Map<String, Object> signData = null;
    AsymmetricKeyParameter privateKey;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Spinner spinner = (Spinner) findViewById(R.id.planets_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.options_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

        final EditText myEditField = (EditText) findViewById(R.id.editbox);
        final EditText answerfield = (EditText) findViewById(R.id.answer);

        final Button button = (Button) findViewById(R.id.calculate);

        button.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                try {
                    String editbox = myEditField.getText().toString();
                    String answer = "";

                    Log.i("", editbox);
                    if (entry.equals("MD5")) {
                        String prehash = get_MD5(editbox);

                        Log.v(TAG, prehash);
                        answerfield.setText(prehash);
                    } else if (entry.equals("MAC")) {
                        String MAC = getMAC(editbox);
                        Log.v(TAG, MAC);

                        answerfield.setText(getMAC("MAC"));

                    } else if (entry.equals("DigitalSignature")) {
                        signData = getEncryptedMD5( myEditField.getText().toString() );
                        String text = new String((byte[]) signData.get("cipherText"), "UTF8");
                        answerfield.setText(bytesToHex((byte[]) signData.get("cipherText")));

                    } else if (entry.equals("DigitalSignatureValidate")) {
                        PublicKey key = (PublicKey) signData.get("publicKey");
                        String str = myEditField.getText().toString();
                        boolean isVal = isSignatureValid((byte[]) signData.get("cipherText"), str, key);
                        Boolean b = new Boolean(isVal);
                        Context context = getApplicationContext();
                        CharSequence text = "";
                        if(isVal) {
                            text = "Signature is VALID!";
                        } else{
                            text = "Signature is NOT valid!";
                        }
                        int duration = Toast.LENGTH_LONG;
                        Toast toast = Toast.makeText(context, text, duration);
                        toast.show();
                        Log.v(TAG, b.toString());
                    }else if(entry.equals("GOST_hashing")){
                        Security.addProvider(new BouncyCastleProvider());

                        byte[] trial = myEditField.getText().toString().getBytes();

                        GOST3411Digest examplesha = new GOST3411Digest(); //256-bits
                        examplesha.update(trial, 0, trial.length);

                        byte[] digested = new byte[examplesha.getDigestSize()];
                        examplesha.doFinal(digested, 0);

                        answerfield.setText( new String(Hex.encode(digested)));
                    }else if(entry.equals("RSA_encrypt")){
                        AsymmetricCipherKeyPair keyPair = GenerateKeys();

                        String plainMessage = myEditField.getText().toString();

                        GetTimestamp("Encryption started: ");
                        String encryptedMessage = Encrypt(plainMessage.getBytes("UTF-8"), keyPair.getPublic());
                        privateKey = keyPair.getPrivate();
                        answerfield.setText(encryptedMessage);
                        GetTimestamp("Encryption ended: ");

                        GetTimestamp("Decryption started: ");

                    }else if(entry.equals("RSA_decrypt")){
                        String encryptedMessage = myEditField.getText().toString();
                        String decryptedMessage = Decrypt(encryptedMessage, privateKey);
                        answerfield.setText(decryptedMessage);
                    }

                } catch (Exception e) {
                   // Log.e("", "Failed to Calculate Tip:" + e.getMessage());
                    e.printStackTrace();
                    answerfield.setText(e.getMessage());
                }
            }
        });
    }

    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        // An item was selected. You can retrieve the selected item using
        entry = parent.getItemAtPosition(pos).toString();
        Log.v(TAG, "index=" + parent.getItemAtPosition(pos));

    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }

    private String get_MD5(String prehash) {
        final StringBuffer hexString = new StringBuffer();

        try {
            final byte[] prehashBytes = prehash.getBytes("iso-8859-1");
            System.out.println(prehash.length());
            System.out.println(prehashBytes.length);
            final MessageDigest digester = MessageDigest.getInstance("MD5");
            digester.update(prehashBytes);
            final byte[] digest = digester.digest();

            for (final byte b : digest) {
                final int intByte = 0xFF & b;

                if (intByte < 10) {
                    hexString.append("0");
                }

                hexString.append(
                        Integer.toHexString(intByte)
                );
            }
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
        return hexString.toString().toUpperCase();
    }


    private String getMAC(String str) {
        //
        Mac mac = null;
        try {
            byte[] plainText = str.getBytes("iso-8859-1");

            System.out.println("\nStart generating key");

            System.out.println("Finish generating key");
            //
            final KeyGenerator keyGen = KeyGenerator.getInstance("HmacMD5");
            final SecretKey MD5key = keyGen.generateKey();
            mac = Mac.getInstance("HmacMD5");

            mac.init(MD5key);
            mac.update(plainText);
            //
            Log.v(TAG, "\n" + mac.getProvider().getInfo());
            Log.v(TAG, "\nMAC: ");
            bytesToHex(mac.doFinal());
            Log.v(TAG, bytesToHex(mac.doFinal()));
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        } catch (InvalidKeyException ex) {
            ex.printStackTrace();
        }
        return bytesToHex(mac.doFinal());
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static Map<String, Object> getEncryptedMD5(String dataToSign) {
        try {
            byte[] plainText = dataToSign.getBytes("UTF8");
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            Log.v(TAG, "\n" + messageDigest.getProvider().getInfo());
            messageDigest.update(plainText);
            byte[] md = messageDigest.digest();
            Log.v(TAG, "\nDigest: ");
            Log.v(TAG, bytesToHex(md));
            Log.v(TAG, "\nStart generating RSA key");
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(1024);
            KeyPair key = keyGen.generateKeyPair();
            Log.v(TAG, "Finish generating RSA key");
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            Log.v(TAG, "\n" + cipher.getProvider().getInfo());
            Log.v(TAG, "\nStart encryption");
            cipher.init(Cipher.ENCRYPT_MODE, key.getPrivate());
            byte[] cipherText = cipher.doFinal(md);
            Log.v(TAG, "Finish encryption: ");
            Log.v(TAG, bytesToHex(cipherText));

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("cipherText", cipherText);
            params.put("publicKey", key.getPublic());
            return params;

        } catch (Exception ex) {

        }
        return null;
    }

    private static boolean isSignatureValid(byte[] cipherText, String plainIn, PublicKey key) {
        try {
            Log.v(TAG,"IS VALID CIPHER TEXT " + bytesToHex(cipherText));
            Log.v(TAG,"IS VALID PLAIN TEXT " + plainIn);
            byte[] plainText = plainIn.getBytes("UTF8");

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");

            System.out.println("\nStart decryption");
            cipher.init(Cipher.DECRYPT_MODE, key);

            byte[] newMD = cipher.doFinal(cipherText);
            Log.v(TAG, "Finish decryption: ");
            Log.v(TAG, bytesToHex(newMD));
            Log.v(TAG, "\nStart signature verification");
            messageDigest.reset();
            messageDigest.update(plainText);
            byte[] oldMD = messageDigest.digest();

            int len = newMD.length;
            if (len > oldMD.length) {
                Log.v(TAG, "Signature failed, length error");
                return false;
            }
            for (int i = 0; i < len; ++i)
                if (oldMD[i] != newMD[i]) {
                    Log.v(TAG, "Signature failed, element error");
                    return false;
                }

            Log.v(TAG, "Signature verified");
            return true;
        } catch (Exception ex){

        }
        return false;
    }
}

