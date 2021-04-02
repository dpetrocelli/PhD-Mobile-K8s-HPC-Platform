package ar.com.craving.ffmpeg;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.SharedAccessAccountPolicy;
import com.microsoft.azure.storage.StorageCredentials;
import com.microsoft.azure.storage.StorageCredentialsAccountAndKey;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.SharedAccessBlobPermissions;
import com.microsoft.azure.storage.blob.SharedAccessBlobPolicy;
import com.rabbitmq.client.ConnectionFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import com.rabbitmq.client.*;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "[WORKER-ANDROID]-"+ this.getClass().getSimpleName();

    String ipStringServer;
    int port;
    String jobsQueue = "jobsQueue";
    String pendantQueue = "pendantQueue";
    String enterQueue = "enterQueue";
    ConnectionFactory factory;
    Connection enterConnection;
    Channel enterChannel;
    Connection pendantConnection;
    Channel pendantChannel;

    String username;
    String password;
    String rabbitHost;
    String mariadbHost;
    String mFfmpegBin;
    String curlBin;
    private Button processTaskButton = null;
    private TextView progressLogTextView = null;
    private TextView out;
    private String root = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator;
    private WorkerAndroidLogger logger= null;
    private Boolean isGranted(String permission) {
        return ContextCompat.checkSelfPermission(MainActivity.this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            setContentView(R.layout.activity_main);
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
            out= findViewById(R.id.text);


            configureServices();
            toolsConnection();
            initControls();
            setupButton();
        } catch (Exception e) {
            out.append("\n" + "[ERROR] " + e.getMessage());
        }
    }

    private void initControls(){
        logger = new WorkerAndroidLogger(this);
        //logger.logToUI(TAG,"SALIDA");
        if (processTaskButton == null) {
            processTaskButton = (Button) findViewById(R.id.process_task_button);
        }

        if (progressLogTextView == null) {
            progressLogTextView = (TextView) findViewById(R.id.et);

        }
    }
    private void setupButton() {

        processTaskButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                if (isGranted(Manifest.permission.READ_EXTERNAL_STORAGE) &&
                        isGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE) &&
                        isGranted(Manifest.permission.INTERNET)) {
                    workDarling();
                    return;
                }

                out.append("\n" + "Solicitando permisos");
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[] {
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.INTERNET
                        },
                        1);

            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onRequestPermissionsResult(int code, String[] permissions, int[] grantResults) {
        try {
            if (!isGranted(Manifest.permission.READ_EXTERNAL_STORAGE) ||
                    !isGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                    !isGranted(Manifest.permission.INTERNET)) {
                out.append("\n" + "[ERROR] Sin permisos");
                return;
            }

            out.append("\n" + "Permisos listos");
            workDarling();
        } catch (Exception e) {
            out.append("\n" + "[ERROR] " + e.getMessage());
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String hmacSha1(String value, String key) {
        try {
            // Get an hmac_sha1 key from the raw key bytes
            byte[] keyBytes = key.getBytes();
            SecretKeySpec signingKey = new SecretKeySpec(keyBytes, "HmacSHA1");

            // Get an hmac_sha1 Mac instance and initialize with the signing key
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(signingKey);

            // Compute the hmac on input data bytes
            byte[] rawHmac = mac.doFinal(value.getBytes());

            //  Covert array of bytes to a base64
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void workDarling() {
        try {
            //out.setText("");

            String processorInfo = Arrays.toString(Build.SUPPORTED_ABIS);
            //Log.i(TAG, "SUPPORTED_ABIS : " + processorInfo);
            logger.logToUI(TAG, "SUPPORTED_CPUS : " + processorInfo);
            //String msgx = new String ("[TIME]-"+new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(new Date()) + " - [MSG] SUPPORTED_ABIS : " + processorInfo);
            ////add(msgx);
            ////add("[TIME]-"+new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(new Date()) + " - [MSG] SUPPORTED_ABIS : " + processorInfo);
            ////add("[TIME]-"+new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(new Date()) + " - [MSG] SUPPORTED_ABIS : " + processorInfo);
            //  PARAMS AND FFMPEG BASE-------------------------------------------------------------------------------------
            String inS3Bucket       = "s3-bcra-splitted";
            String outS3Bucket      = "s3-bcra-compressed";
            String inAws            = inS3Bucket + ".s3.us-east-1.amazonaws.com";
            String outAws            = outS3Bucket + ".s3.us-east-1.amazonaws.com";

            String s3AccessKey   = "AKIATKQHK5AW2H5R436F";
            String s3SecretKey   = "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";


            //String curlBin = installBinary(MainActivity.this, R.raw.curl, "curl", true);
            //String curlBin = installBinary(MainActivity.this, R.raw.curl, "curl", true);

            if (processorInfo.contains("arm64")){
                curlBin = installBinary(MainActivity.this, R.raw.curl, "curl", true);
                mFfmpegBin    = installBinary(MainActivity.this, R.raw.ffmpeg, "ffmpeg", true);
                //String resultx = this.runBash(curlBin+" -O https://cdn.jsdelivr.net/npm/vue/dist/vue.js", true);
                //msgx = new String ("[TIME]-"+new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(new Date()) + " - [MSG] curl has been installed");
                //add(msgx);
                logger.logToUI(TAG, "curl arm64 is installed");
            }else{
                if (processorInfo.contains("x86")){
                    //mFfmpegBin    = installBinary(MainActivity.this, R.raw.ffmpeg_x86, "ffmpeg", true);
                    //curlBin = installBinary(MainActivity.this, R.raw.curl_i386, "curl", true);
                    //curlBin = "curl";
                    logger.logToUI(TAG, "x86 (curl) by default ");
                }else{
                    mFfmpegBin    = installBinary(MainActivity.this, R.raw.ffmpeg, "ffmpeg", true);
                    curlBin = "curl";

                    logger.logToUI(TAG, "32 bits processor (curl) by default ");
                }

            }
            //String curlBin       = installBinary(MainActivity.this, R.raw.curl, "curl", true);
            //String wgetBin       = installBinary(MainActivity.this, R.raw.wget, "wget", true);


            try {

                //[STEP 1] - Get Msg
                boolean autoAck = false;
                this.enterChannel.basicConsume(this.jobsQueue, autoAck, "myConsumerTag",
                        new DefaultConsumer(this.enterChannel) {
                            @Override
                            public void handleDelivery(String consumerTag,
                                                       Envelope envelope,
                                                       AMQP.BasicProperties properties,
                                                       byte[] body)
                                    throws IOException
                            {


                                String routingKey = envelope.getRoutingKey();
                                String contentType = properties.getContentType();
                                long deliveryTag = envelope.getDeliveryTag();
                                // (process the message components here ...)
                                //channel.basicAck(deliveryTag, false);

                                long initTime = System.currentTimeMillis();
                                RabbitMsg rmsg = new RabbitMsg(deliveryTag,body,routingKey,contentType);
                                String msgNotEncoded = new String (rmsg.getMsg());
                                JsonUtility jsonUt = new JsonUtility();
                                jsonUt.setType("Message");

                                Message msgRearmed = (Message) jsonUt.fromJson(msgNotEncoded);
                                System.out.println(" [STEP 1] -  Job Obtained ");
                                ////Log.i(TAG, "JOB OBTAINED: "+msgRearmed.getName()+"_"+msgRearmed.getPart());
                                logger.logToUI(TAG, "********** STARTING WITH A NEW JOB ********"+msgRearmed.getName()+"_"+msgRearmed.getPart());

                                String service = msgRearmed.getService();

                                String returnQueue = msgRearmed.getName();

                                EncodingStructure parametersFromMsg = msgRearmed.getParamsEncoding();
                                String cloudStorage = msgRearmed.getCloudStorage();
                                String magic = "";
                                String source = "";
                                String curl = "";
                                if (cloudStorage.startsWith("az")) {
                                    String AzStorageAccount ="testdpkub";
                                    String keyStr = "o3czD5k/59lCXMUYa28H3H6SrnhXNLMSXV8y3ZPD1Aei9aZJ/Kp1E1oTsVdQ+d6WwTV7ogzKInvyC8gTxzignA==";
                                    //String storageConnectionString = "DefaultEndpointsProtocol=https;AccountName="+AzStorageAccount+";AccountKey="+keyStr+";EndpointSuffix=core.windows.net";
                                    String sasToken= GetFileSAS(AzStorageAccount, keyStr);

                                    String AzSplittedContainer = "s3-bcra-splitted";
                                    String AzCompressedContainer = "s3-bcra-compressed";
                                    source = "https://" + AzStorageAccount + ".blob.core.windows.net/" + AzSplittedContainer + "/" + msgRearmed.getOriginalName();
                                    String target = msgRearmed.getName()+"_"+msgRearmed.getPart()+".ts";
                                    curl = curlBin+" -X PUT --data-binary @- -H \"x-ms-date: $(date -u)\" -H \"x-ms-blob-type: BlockBlob\" \"https://"+AzStorageAccount+".blob.core.windows.net/"+AzCompressedContainer+"/"+target+sasToken+"\"";

                                }else{
                                    String inS3Bucket       = "s3-bcra-splitted";
                                    String outS3Bucket      = "s3-bcra-compressed";
                                    String inAws            = inS3Bucket + ".s3.us-east-1.amazonaws.com";
                                    String outAws            = outS3Bucket + ".s3.us-east-1.amazonaws.com";

                                    String s3AccessKey   = "AKIATKQHK5AW2H5R436F";
                                    String s3SecretKey   = "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";

                                    source       = "https://" + inAws + "/" + msgRearmed.getOriginalName();
                                    //String source = "https://vod-progressive.akamaized.net/exp=1588550387~acl=%2A%2F1594271967.mp4%2A~hmac=943f21a32958247e6fc36566c761d0316aee5ef7a2d405c0bb62cff948821285/vimeo-prod-skyfire-std-us/01/1094/15/380473795/1594271967.mp4?download=1&filename=Trees+-+30222.mp4";
                                    String target       = msgRearmed.getName()+"_"+msgRearmed.getPart()+".ts";

                                    // DATE and HASH for curl
                                    SimpleDateFormat sdf = new SimpleDateFormat("YYYYMMdd'T'HHmmss'Z'");
                                    sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
                                    String dateFormatted = sdf.format(new Date());
                                    String relativePath  = "/" + outS3Bucket + "/" + target;
                                    String contType   = "application/octet-stream";
                                    String stringToSign  = "PUT\n\n" + contType + "\n" + dateFormatted + "\n" + relativePath;
                                    String signature     = hmacSha1(stringToSign, s3SecretKey);
                                    curl =  curlBin+" -X PUT --data-binary @- -H \"Host: " + outAws + "\" -H \"Date: " + dateFormatted + "\" -H \"Content-Type: " + contType + "\" -H \"Authorization: AWS " + s3AccessKey + ":" + signature + "\" \"https://" + outAws + "/" + target + "\"\n";

                                }

                                magic       		= curlBin+" \""+ source + "\" | " + mFfmpegBin+" -i -";
                                magic               += " -s "+parametersFromMsg.getVideoResolution();
                                magic               += " -aspect 16:9 -c:v "+parametersFromMsg.getVideoCodec();
                                magic               += " -b:v "+parametersFromMsg.getVideoBitrate()+"k";
                                magic               += " -profile:v "+parametersFromMsg.getVideoProfile();
                                magic               += " -level "+parametersFromMsg.getVideoLevel();
                                magic               += " -preset "+parametersFromMsg.getVideoPreset();
                                magic               += " -threads 0 ";
                                magic               += "-f mpegts - | ";
                                magic				+= curl;




                                String fCmd = root + getString(R.string.command);
                                File file = new File(fCmd);
                                BufferedWriter bw = new BufferedWriter(new FileWriter(file));
                                bw.write(magic);
                                bw.close();

                                logger.logToUI (TAG, "Storage: "+cloudStorage+" Start to execute: " + msgRearmed.getName()+"_"+msgRearmed.getPart()+".ts");
                                //add("nada");
                                ////add("Worker is going to work with JOB OBTAINED: " + msgRearmed.getName()+"_"+msgRearmed.getPart()+".ts");
                                try {
                                    String executionResult = runBash(fCmd, true);

                                    if (!((executionResult.contains("Invalid data found when processing input") || (executionResult.contains("null"))))){
                                        // PROBLEMS
                                        //Log.i (TAG, "Work Uploaded to Cloud:" +msgRearmed.getName()+"_"+msgRearmed.getPart()+".ts" );
                                        logger.logToUI (TAG, "Task has been done and uploaded to Cloud:" +msgRearmed.getName()+"_"+msgRearmed.getPart()+".ts" );
                                        //add("Work has been uploaded to Cloud Storage:" +msgRearmed.getName()+"_"+msgRearmed.getPart()+".ts" );


                                        long endTime = System.currentTimeMillis();
                                        String cpuInfo= System.getProperty("os.arch");


                                        String urlPost = "http://"+ipStringServer+":"+port+"/uploadFinishedJob?id=";
                                        urlPost+=deliveryTag;
                                        urlPost+="&service=";
                                        urlPost+=msgRearmed.getService();
                                        urlPost+="&job=";
                                        urlPost+=msgRearmed.getName();
                                        urlPost+="&part=";
                                        urlPost+=msgRearmed.getPart();
                                        urlPost+="&workerName=";
                                        //urlPost+=msgRearmed.getWorkerName();
                                        urlPost+="Android";
                                        urlPost+="&workerArchitecture=";
                                        //urlPost+=msgRearmed.getWorkerArchitecture();
                                        urlPost+=cpuInfo;
                                        urlPost+="&initTime=";
                                        urlPost+=initTime;
                                        urlPost+="&endTime=";
                                        urlPost+=endTime;
                                        urlPost+="&executionTime=";
                                        urlPost+=endTime-initTime;
                                        urlPost+="";
                                        //add("Sending statistics to WebServer");
                                        try{
                                            int responseCode = httpGet(urlPost);
                                            //Log.i(TAG, "Response Code: "+deliveryTag);
                                            logger.logToUI(TAG, "Web server Resp Code: "+deliveryTag);
                                            if (responseCode==200){
                                                enterChannel.basicAck(deliveryTag, false);
                                                //add("Task has been removed from Queue");
                                                //Log.i(TAG, "ACK: "+deliveryTag);
                                                logger.logToUI(TAG, "ACK: "+deliveryTag);
                                                //add("Task has been completed");
                                            }else{
                                                //Log.i(TAG, "Backend Web NACK:  "+deliveryTag);
                                                logger.logToUI(TAG, "**** FAILED BEND OFFLINE ****** DT:  "+deliveryTag);
                                                Thread.sleep(1000);
                                                //add("Web server doesn't reply, aborting task (requeue) ");
                                                enterChannel.basicNack(deliveryTag, false, true);
                                            }
                                        }catch (Exception e){
                                            //Log.i(TAG, "Exeption.. aborting task (requeue) ");
                                            logger.logToUI(TAG, "Exeption.. aborting task (requeue) ");
                                            Thread.sleep(1000);
                                            //add("Exeption.. aborting task (requeue) ");
                                            enterChannel.basicNack(deliveryTag, false, true);
                                        }
                                    }else{
                                        //Log.i (TAG, "Problems with FFMPEG or CURL binaries, aborting ");
                                        logger.logToUI (TAG, "Problems with FFMPEG or CURL binaries, aborting ");
                                        //add("Problems with FFMPEG or CURL binaries, aborting ");
                                        enterChannel.basicNack(deliveryTag, false, true);
                                    }



                                }catch (Exception e){
                                    try {
                                        enterChannel.basicNack(deliveryTag, false, true);
                                    } catch (IOException ioException) {
                                        //ioException.printStackTrace();
                                    }
                                    //e.printStackTrace();


                                }



                            }


                        });


            }catch (Exception e) {
                System.err.println(" Waiting for a job ");

            }

        } catch (Exception e) {
            out.append("\n" + "[ERROR] " + e.getMessage());
        }


    }

    private int httpGet (String urls){

        int result = 0;   //Instantiate new instance of our class
        HttpGetRequest getRequest = new HttpGetRequest();   //Perform the doInBackground method, passing in our url
        try {
            result = Integer.parseInt(getRequest.execute(urls).get());
        } catch (Exception e) {
            result = 0;
        }
        return result;
    }

    private String convertInputStream(InputStream is, String encoding) {
        Scanner scanner = new Scanner(is, encoding).useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }


    private String GetFileSAS(String AzStorageAccount, String keyStr){

        String accountName = AzStorageAccount;
        String key = keyStr;
        String storageConnectionString = "DefaultEndpointsProtocol=https;AccountName="+accountName+";AccountKey="+key+";EndpointSuffix=core.windows.net";

        String sasToken="";
        try {
            CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
            SharedAccessAccountPolicy sharedAccessAccountPolicy = new SharedAccessAccountPolicy();
            sharedAccessAccountPolicy.setPermissionsFromString("racwdlup");
            sharedAccessAccountPolicy.setSharedAccessExpiryTime(new Date(new Date().getTime()+ 8640000));
            sharedAccessAccountPolicy.setResourceTypeFromString("sco");
            sharedAccessAccountPolicy.setServiceFromString("bfqt");
            sasToken = "?" + storageAccount.generateSharedAccessSignature(sharedAccessAccountPolicy);
        }catch (Exception e){
            e.getMessage();
        }

        return sasToken;
    }

   /* private void verifyCurl() {
        try {
            String result = runBash("curl -V", true);
            if (result.contains("No such ")) {
                //Log.i(TAG, "Curl not available, install" + result);
                curlBin = installBinary(MainActivity.this, R.raw.curl, "curl", true);
                result = runBash(curlBin+" -V", true);
                //Log.i(TAG, "Curl has been installed");
            } else {
                curlBin = "curl";
            }
        } catch (Exception e) {
            Log.e(TAG, "ERROR EN VERIFY CURL");
        }
    }*/

    private String runBash (String fCmd, boolean show) {
        String response = "";
        try {
            ProcessBuilder pb = new ProcessBuilder("/system/bin/sh", fCmd);
            Process process = pb.start();

                BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
                BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                String line;
                while ((line = stdout.readLine()) != null) {
                    // out.setText("\n[STD] " + line);
                    response+=line+"\n";
                    if (show) {
                        //Log.i(TAG, "STD" + line);
                    }

                }

                while ((line = stderr.readLine()) != null) {
                    //out.append("\n[ERR] " + line);
                    response+=line+"\n";
                    if (show){
                        Log.e(TAG, "ERR" + line);
                    }
                }


            process.waitFor();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return response;
    }
    private String installBinary(Context ctx, int resId, String filename, boolean upgrade) {
        try {
            File f = new File(ctx.getDir("bin", 0), filename);
            if (f.exists()) {
                f.delete();
            }
            copyRawFile(ctx, resId, f, "0777");
            return f.getCanonicalPath();
        } catch (Exception e) {
            out.append("\n" + "[ERROR] " + e.getMessage());
            return null;
        }
    }


    private void copyRawFile(Context ctx, int resid, File file, String mode) {
        try {
            final String abspath = file.getAbsolutePath();
            final FileOutputStream out = new FileOutputStream(file);
            final InputStream is = ctx.getResources().openRawResource(resid);
            byte buf[] = new byte[1024];
            int len;
            while ((len = is.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.close();
            is.close();
            mode = "0777";
            Runtime.getRuntime().exec("chmod "+mode+" "+abspath).waitFor();
        } catch (Exception e) {
            out.append("\n" + "[ERROR] " + e.getMessage());
        }
    }

    private void configureServices(){
        //Log.i (TAG, "CONFIGURATING SERVICES");
        //this.ipStringServer = "52.154.71.162";
        this.ipStringServer = "192.168.0.25";
        this.port = 8081;
        // CREATE THE CONSTRUCTOR
        // DEFINE BASICS FOR MARIADBSERVICE
        String urlGit = "https://raw.githubusercontent.com/dpetrocelli/LinuxRepo/master/configFile";
        this.downloadConfigurationFile(urlGit);



    }


    private void downloadConfigurationFile(String url) {

        // [STEP 1] - Obtain Job
        try {
            URL obj = new URL(url);
            //Log.i (TAG, "Obtaining configuration files");
            this.rabbitHost = "a16e64467fa6b4bde9adaf78edc87fb1-992766720.us-east-1.elb.amazonaws.com";
            this.mariadbHost = "af0c97f9cb85140dd8ca759fd1b3ca5b-1051178316.us-east-1.elb.amazonaws.com";

        } catch (Exception e) {
            Log.e (TAG, "ERROR DOWNLOADING "+e.getMessage());
        }

    }

/*
    public void //add(String result) {

        progressLogTextView.append(result+"\n");

    }*/

    public void updateResults(String result) {

        progressLogTextView.append(result + "\n" + "\n");

    }
    public void cleanScreen (){
        progressLogTextView.setText(null);
    }
    private void toolsConnection() {
        /*
        String dbname = "distributedProcessing";
        String url = "jdbc:mariadb://" + mariadbHost + "/" + dbname;
        String username = "david";
        String password = "david";

        this.mdbc = new MariaDBConnection(mariadbHost, dbname, username, url, password);
        this.mdbc.createConnection();

        //Log.i (TAG, "Maria DB Connected ");
        */
        // DEFINE BASICS FOR RABBITMQ SERVICE

        String usernameRabbit = "admin";
        String passwordRabbit = "admin";
        this.factory = new ConnectionFactory();
        this.factory.setHost(rabbitHost);
        //this.factory.setPort(5672);
        this.factory.setUsername(usernameRabbit);
        this.factory.setPassword(passwordRabbit);

        boolean durable = true;
        try {

            try {
                this.enterConnection = this.factory.newConnection();
                //Log.i (TAG, "Rabbit Connected");
            }catch (Exception e){
                //Log.e (TAG, " la vieja " + String.valueOf(e.getCause()));
                Log.e (TAG, "---------------");
                e.printStackTrace();
                Log.e (TAG, "---------------");
            }


            this.enterChannel = this.enterConnection.createChannel();
            this.enterChannel.queueDeclare(this.enterQueue, durable, false, false, null);
            this.enterChannel.queueDeclare(this.jobsQueue, durable, false, false, null);
            this.enterChannel.basicQos(1);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            Log.e (TAG, "Error IOEXCEPTION "+e.getMessage());
        }


    }
}