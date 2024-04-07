package com.example.antistos;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.role.RoleManager;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Telephony;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;

import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;
import java.util.Random;

@RequiresApi(api = Build.VERSION_CODES.O)
public class MainActivity extends AppCompatActivity {

private Button mButton1;
private TextView mTexteHaut, mTexteBas;

Context context;
String defaultSmsApp, sender;
int respDelay = 0;
int maxMin = 6;
int minMin = 2;


    protected void ecritMessages(int minLess){
        DateTimeFormatter hrmin = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter jour = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter heures = DateTimeFormatter.ofPattern("HH:mm:ss");
        DateTimeFormatter pdf = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm");
        LocalDateTime envoi = LocalDateTime.now().plusMinutes(-minLess);
        String rndSerial = getRndStr(15, false, true, false);
        String rndRecu = getRndStr(6, false, true, true);
        String bodyStas = String.format("STAS\nTitre 1 voyage 1h30\nValable jusqu'au %s à %s\n1.60€\n\n%s\n\nRecu : http://a.hcnx.eu/%s\nTransfert interdit", jour.format(envoi.plusMinutes(90).plusSeconds(respDelay)), hrmin.format(envoi.plusMinutes(90).plusSeconds(respDelay)) , rndSerial, rndRecu);
        WriteSms("1voyage", sender, minLess);
        ReceiveSms(bodyStas, sender, minLess, respDelay);
        String filename=createPDF(jour.format(envoi), heures.format(envoi), jour.format(envoi.plusMinutes(90)), heures.format(envoi.plusMinutes(90).minusSeconds(envoi.getSecond())), rndSerial, pdf.format(envoi));
        sendNotif();
    }

//Envoi SMS
    protected View.OnClickListener lst1(String toPck, String curPck) {
        ActivityResultLauncher<Intent> sARL = activityResultLauncherDef();
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            //Get the package name and check if my app is not the default sms app
                sARL.launch(changeDefSmsApp(toPck));
                if(String.format("%sr",Telephony.Sms.getDefaultSmsPackage(context)).equals(getPackageName()+"r")) {
                    int rndDelay = (int) (Math.random() * (maxMin - minMin) + minMin);
                    ecritMessages(rndDelay);
                    Log.d("test", "3");
                    mTexteBas.setText(String.format("Ticket généré il y a %d minutes", rndDelay));
                }
                else{
                    mTexteBas.setText("Rappuyer pour obtenir un ticket");
                }
            }
        };
    }

    protected Intent changeDefSmsApp(String to){
        //Change the default sms app to my app
        Intent roleRequestIntent = null;
        Log.d("test", "2");
        if (Build.VERSION.SDK_INT  >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = null;
            roleManager = context.getSystemService(RoleManager.class);

            if (roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                roleRequestIntent = roleManager.createRequestRoleIntent(
                        RoleManager.ROLE_SMS);
                return roleRequestIntent;
            }
        }
        else {
            roleRequestIntent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            roleRequestIntent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, to);
            return roleRequestIntent;
        }
        return null;
    }

    protected ActivityResultLauncher<Intent> activityResultLauncherDef() {

        return registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            // There are no request codes
                            if (Telephony.Sms.getDefaultSmsPackage(context).equals(getPackageName())) {
                                //Write to the default sms app
                                mTexteHaut.setText("L'application SMS par défaut est bien AntiSTOS");
                            }
                            else{
                                mTexteHaut.setText("Merci de mettre AntiSTOS comme application de SMS par défaut");
                            }
                        }
                    }
                });
    }

//DL ticket
    protected String createPDF(String date, String heure, String df, String hf, String ticket, String fName){
        float x, y;
        double sautLigne = 2.7;
        //Creer le ticket
        PdfDocument recu = new PdfDocument();
        PdfDocument.PageInfo infos = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = recu.startPage(infos);
        Canvas canvas = page.getCanvas();
        canvas.setDensity(96);

        // Fonts
        Paint regular = new Paint();
        Typeface regTf = getResources().getFont(R.font.dejavusans);
        regular.setTypeface(regTf);
        regular.setTextSize(8);
        regular.setLetterSpacing((float) 0.04);

        Paint bold = new Paint();
        Typeface boldTf = getResources().getFont(R.font.dejavusans_bold);
        bold.setTypeface(boldTf);
        bold.setTextSize(8);
        bold.setLetterSpacing((float) 0.04);

        // Image logo
        Bitmap bmp, resizedBmp;
        bmp = BitmapFactory.decodeResource(getResources(), R.drawable.logo_stas);
        resizedBmp = Bitmap.createScaledBitmap(bmp, 506, 320, true);
        canvas.drawBitmap(resizedBmp, 37, 52, regular);

        // Texte haut gauche
        String haut = "STAS Transdev-Saint-Étienne\n1 avenue Pierre Mendes France\nCS 90055 42272, Saint-Priest-en-Jarez\n0800 041 042 (service et appel\ngratuit)\nwww.reseau-stas.fr";
        x = 165;
        y = 62;
        for (String line:haut.split("\n")){
            canvas.drawText(line, x, y, regular);
            y+=regular.descent()-regular.ascent()+sautLigne;
        }
        // Texte bas gauche
        String basgB = "Reçu d'achat - PAIEMENT PAR\nSMS";
        String basg = String.format("Le %s à %s\nN° Ticket = %s\nValable du %s %s au\n%s %s", date, heure, ticket, date, heure, df, hf);
        x = 37;
        y = 181;
        for (String line:basgB.split("\n")){
            canvas.drawText(line, x, y, bold);
            y+=bold.descent()-bold.ascent()+sautLigne;
        }
        y = 204;
        for (String line:basg.split("\n")){
            canvas.drawText(line, x, y, regular);
            y+=regular.descent()-regular.ascent()+sautLigne;
        }

        // Texte bas droit
        String basdB = "JUSTIFICATIF A CONSERVER";
        String basd = "1 VOYAGE 1H30\nMontant HT : 1.45 €\nTVA (10%) : 0.15 €\nMontant TTC : 1.60 €";
        x = 260;
        y = 181;
        for (String line:basdB.split("\n")){
            canvas.drawText(line, x, y, bold);
            y+=bold.descent()-bold.ascent()+sautLigne;
        }
        y = 193;
        for (String line:basd.split("\n")){
            canvas.drawText(line, x, y, regular);
            y+=regular.descent()-regular.ascent()+sautLigne;
        }

        // Texte tout en bas
        String bas = "Conditions générales d’utilisation disponibles sur www.reseau-stas.fr";
        x = 29;
        y = 329;
        for (String line:bas.split("\n")){
            canvas.drawText(line, x, y, regular);
            y+=regular.descent()-regular.ascent()+1;
        }

        // Exportation du pdf
        recu.finishPage(page);
        String filename=String.format("recu_transaction_%s_%s.pdf",ticket, fName);
        File fichier = createCommonDocumentDirFile(filename);

        try {
            recu.writeTo(new FileOutputStream(fichier));
        } catch (IOException e) {
            e.printStackTrace();
            filename="";
            Log.d("Error", e.toString());
        }
        recu.close();
        return filename;
    }
    public static String commonDocumentDirPath(String fileName)
    {
        String dir = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        {
            dir = (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + fileName);
        }
        else
        {
            dir = (Environment.getExternalStorageDirectory() + "/" + fileName);
        }
        return dir;
    }
    public static File createCommonDocumentDirFile(String fileName)
    {
        File dir = new File(commonDocumentDirPath(fileName));

        // Make sure the path directory exists.
        if (!Objects.requireNonNull(dir.getParentFile()).exists())
        {
            // Make it, if it doesn't exit
            boolean success = dir.getParentFile().mkdirs();
            if (!success)
            {
                dir = null;
            }
        }
        return dir;
    }

    static String getRndStr(int n, boolean lower, boolean nmbrs, boolean caps)
    {
        // choose a Character random from this String
        String AlphaNumericString = "";
        if (nmbrs){
            AlphaNumericString += "0123456789";
        }
        if (caps){
            AlphaNumericString += "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        }
        if (lower){
            AlphaNumericString += "abcdefghijklmnopqrstuvxyz";
        }
        // create StringBuffer size of AlphaNumericString
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            // generate a random number between
            // 0 to AlphaNumericString variable length
            int index
                    = (int)(AlphaNumericString.length()
                    * Math.random());
            // add Character one by one in end of sb
            sb.append(AlphaNumericString
                    .charAt(index));
        }
        return sb.toString();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PackageManager.PERMISSION_GRANTED);

        mButton1 = (Button) findViewById(R.id.generer);
        mTexteHaut = findViewById(R.id.afficheHaut);
        mTexteBas = findViewById(R.id.afficheBas);

        context = this;
        //Get default sms app
        defaultSmsApp = Telephony.Sms.getDefaultSmsPackage(context);
        final String myPackageName = getPackageName();

        //Set the number and the body for the sms
        sender = "93042";

        ActivityResultLauncher<Intent> startChDef = activityResultLauncherDef();
        startChDef.launch(changeDefSmsApp(myPackageName));

        createNotificationChannel();

        View.OnClickListener lancer1 = lst1(myPackageName, defaultSmsApp);

        mButton1.setOnClickListener(lancer1);
    }

    //Write to the default sms app
    private void WriteSms(String message, String phoneNumber, int nbMinLess) {

        //Put content values
        ContentValues values = new ContentValues();
        values.put(Telephony.Sms.ADDRESS, phoneNumber);
        values.put(Telephony.Sms.DATE, System.currentTimeMillis()-nbMinLess*60*1000);
        values.put(Telephony.Sms.BODY, message);

        //Insert the message
        context.getContentResolver().insert(Telephony.Sms.Sent.CONTENT_URI, values);

        //Change my sms app to the last default sms
        /*Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
        intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, defaultSmsApp);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);*/
    }

    //Receive to the default sms app
    private void ReceiveSms(String message, String phoneNumber, int nbMinLess, int delay) {

        //Put content values
        ContentValues values = new ContentValues();
        values.put(Telephony.Sms.ADDRESS, phoneNumber);
        values.put(Telephony.Sms.DATE, System.currentTimeMillis()-nbMinLess*60*1000+delay*1000);
        values.put(Telephony.Sms.BODY, message);
        //Insert the message
        context.getContentResolver().insert(Telephony.Sms.Inbox.CONTENT_URI, values);

        //Change my sms app to the last default sms
        /*Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
        intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, defaultSmsApp);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);*/
    }

    String CHANNEL_ID = "download.channel";
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Download tips";
            String description = "Affiche le message concernant l'ajout du PDF aux téléchargments";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    public void sendNotif() {
        NotificationCompat.Builder builder;
        Intent downloadIntent;
        PendingIntent pDownloadsIntent;

        downloadIntent = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
        pDownloadsIntent = PendingIntent.getActivity(this, 0, downloadIntent, PendingIntent.FLAG_IMMUTABLE);
        builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.logo_notif)
                .setContentTitle("Génération terminée")
                .setContentText("Cliquez pour accéder au fichier téléchargé")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pDownloadsIntent)
                .setAutoCancel(true);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(0, builder.build());
    }


}