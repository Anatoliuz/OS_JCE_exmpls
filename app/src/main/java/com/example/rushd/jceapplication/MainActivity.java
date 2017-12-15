package com.example.rushd.jceapplication;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

import java.io.UnsupportedEncodingException;
import java.security.*;
import javax.crypto.*;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity  implements AdapterView.OnItemSelectedListener {
    private static final String TAG = "MyActivity";
    private String entry = "MD5";

    private Map<String, Object> signData = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Spinner spinner = (Spinner) findViewById(R.id.planets_spinner);
// Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.planets_array, android.R.layout.simple_spinner_item);
// Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
// Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

        final EditText myEditField = (EditText) findViewById(R.id.mealprice);
        final EditText answerfield = (EditText) findViewById(R.id.answer);

        final Button button = (Button) findViewById(R.id.calculate);


        button.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                try {


                    String mealprice = myEditField.getText().toString();
                    String answer = "";
                    mealprice = "$" + mealprice;
                    Log.i("", mealprice);


                    if (entry.equals("MD5")) {
                        String prehash = get_MD5(mealprice);

                        Log.v(TAG, prehash);
                        answerfield.setText(prehash);
                    } else if (entry.equals("MAC")) {
                        String MAC = getMAC(mealprice);
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
                        Log.v(TAG, b.toString());
                    }

                } catch (Exception e) {
                    Log.e("", "Failed to Calculate Tip:" + e.getMessage());
                    e.printStackTrace();
                    answerfield.setText(e.getMessage());
                }
            }
        });


        //


    }

    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        // An item was selected. You can retrieve the selected item using
        System.out.print("AAAAAAA");
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
        String MAC = null;
        try {
            byte[] plainText = str.getBytes("iso-8859-1");

            System.out.println("\nStart generating key");

            System.out.println("Finish generating key");
            //
            final KeyGenerator keyGen = KeyGenerator.getInstance("HmacMD5");
            final SecretKey MD5key = keyGen.generateKey();
            mac = Mac.getInstance("HmacMD5");
            // get a MAC object and update it with the plaintext
            mac.init(MD5key);
            mac.update(plainText);
            //
            // print out the provider used and the MAC
            Log.v(TAG, "\n" + mac.getProvider().getInfo());
            Log.v(TAG, "\nMAC: ");
            bytesToHex(mac.doFinal());
            MAC = new String(mac.doFinal(), "iso-8859-1");
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
            //
            // get an MD5 message digest object and compute the plaintext digest
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            Log.v(TAG, "\n" + messageDigest.getProvider().getInfo());
            messageDigest.update(plainText);
            byte[] md = messageDigest.digest();
            Log.v(TAG, "\nDigest: ");
            //Log.v(TAG,new String(md, "UTF8"));
            Log.v(TAG, bytesToHex(md));
            //
            // generate an RSA keypair
            Log.v(TAG, "\nStart generating RSA key");
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(1024);
            KeyPair key = keyGen.generateKeyPair();
            Log.v(TAG, "Finish generating RSA key");
            //
            // get an RSA cipher and list the provider
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            Log.v(TAG, "\n" + cipher.getProvider().getInfo());
            //
            // encrypt the message digest with the RSA private key
            // to create the signature
            Log.v(TAG, "\nStart encryption");
            cipher.init(Cipher.ENCRYPT_MODE, key.getPrivate());
            byte[] cipherText = cipher.doFinal(md);
            Log.v(TAG, "Finish encryption: ");
            //Log.v(TAG,new String(cipherText, "UTF8"));
            Log.v(TAG, bytesToHex(cipherText));

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("cipherText", cipherText);
            params.put("publicKey", key.getPublic());
            return params;

        } catch (UnsupportedEncodingException ex) {

        } catch (IllegalBlockSizeException ex) {

        } catch (NoSuchAlgorithmException ex) {

        } catch (NoSuchPaddingException ex) {

        } catch (BadPaddingException ex) {

        } catch (InvalidKeyException ex) {

        }
        return null;
    }

    private static boolean isSignatureValid(byte[] cipherText, String plainIn, PublicKey key) {
        //
        // to verify, start by decrypting the signature with the
        // RSA private key
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
            // Log.v(TAG, new String(newMD, "UTF8"));
            Log.v(TAG, bytesToHex(newMD));
            //
            // then, recreate the message digest from the plaintext
            // to simulate what a recipient must do
            Log.v(TAG, "\nStart signature verification");
            messageDigest.reset();
            messageDigest.update(plainText);
            byte[] oldMD = messageDigest.digest();
            //
            // verify that the two message digests match
            int len = newMD.length;
            if (len > oldMD.length) {
                Log.v(TAG, "Signature failed, length error");
                //  System.exit(1);
                return false;
            }
            for (int i = 0; i < len; ++i)
                if (oldMD[i] != newMD[i]) {
                    Log.v(TAG, "Signature failed, element error");
                    //  System.exit(1);
                    return false;
                }
            Log.v(TAG, "Signature verified");
            return true;
        } catch (IllegalBlockSizeException ex) {

        } catch (NoSuchAlgorithmException ex) {

        } catch (NoSuchPaddingException ex) {

        } catch (BadPaddingException ex) {

        } catch (InvalidKeyException ex) {

        }catch (UnsupportedEncodingException ex){

        }
        return false;
    }
}

