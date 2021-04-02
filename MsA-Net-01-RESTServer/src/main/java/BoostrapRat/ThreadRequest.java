package BoostrapRat;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import com.azure.storage.blob.BlobContainerClient;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.SharedAccessAccountPolicy;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

public class ThreadRequest implements Runnable{
    String cloudstorage;
    Path path;
    String name;
    String user;
    String encodingParams;
    AmazonS3 s3;
    String s3UploadPath;
    String s3InPath;
    BlobContainerClient azureBlob;
    String azureUploadBlob;
    String blobAccount;
    JsonUtility jsonUtility;
    MariaDBConnection mdbc;
    String jobsQueue = "jobsQueue";
    String pendantQueue = "pendantQueue";
    String enterQueue = "enterQueue";
    ConnectionFactory factory;
    Connection enterConnection;
    Channel enterChannel;
    Connection pendantConnection;
    Channel pendantChannel;
    String rabbitHost;
    String mariadbHost;
    String username;
    String password;
    HashMap<String, EncodingStructure> paramsList;
    String context;
    long insertedJobId;

    public ThreadRequest(String cloudstorage, Path path, String name, String user, String encodingParams, String mariadbHost, String rabbitHost, String context, MariaDBConnection mdbc) {
        this.cloudstorage = cloudstorage;
        this.path = path;
        this.name = name;
        this.user = user;
        this.encodingParams = encodingParams;
        this.rabbitHost = rabbitHost;
        this.mariadbHost = mariadbHost;
        this.context = context;
        this.mdbc = mdbc;

    }

    @Override
    public void run() {
        this.jsonUtility = new JsonUtility();
        this.toolsConnection();
        this.paramsList = new HashMap<String, EncodingStructure>();
        this.loadParameters();
        this.splitAndSave(this.cloudstorage, this.path, this.name, this.user, this.encodingParams);
    }

    private void loadParameters () {
        EncodingStructure es = new EncodingStructure("2k", "2560x1440", "libx264", "7800", "high", "5.1", "48", "slower", "ac3", "512", "48000", "6");
        //EncodingStructure es = (EncodingStructure) this.jsonUtility.fromJson(parameters);
        this.paramsList.put("2k", es);

        es = new EncodingStructure("4k","4096x2160","libx264", "15600", "high", "5.1", "60", "slow", "ac3", "512", "48000", "6");
        this.paramsList.put("4k", es);

        es =new EncodingStructure ("hd", "1920x1080","libx264", "3900", "high", "4.1", "30","slow", "ac3", "320", "48000", "6");
        this.paramsList.put("hd", es);
        es = new EncodingStructure("720","1280x720","libx264","2000", "main", "4.1", "25", "medium", "aac", "192", "44100", "2");
        this.paramsList.put("720", es);

        es = new EncodingStructure ("480","852x480","libx264","900","main","3.1","25","fast","aac","128","44100","2");
        this.paramsList.put("480", es);

        es = new EncodingStructure("360","640x360","libx264","700","baseline","3.0","24","faster","aac","128","44100","2");
        this.paramsList.put("360", es);

        es = new EncodingStructure ("240","424x240","libx264","500","baseline","3.0","24","ultrafast","aac","128","44100","2");
        this.paramsList.put("240", es);

    }
    private void splitAndSave(String cloudstorage, Path path, String name, String user, String encodingParams) {
        try{
            // 1 - Create TS

            String tsName 	= name.split(Pattern.quote("."))[0];
            tsName          +=  ".ts";

            String tsFile   = "/tmp/"+tsName;
            String convert;

            // Recibimos el video original
            if (this.context.startsWith("kubernetes") || (this.context.startsWith("localkub"))){
                convert  = "/tmp/ffmpeg -y -i "+path.toString()+" -c copy "+tsFile;
            }else{
                convert  = "ffmpeg -y -i "+path.toString()+" -c copy "+tsFile;
            }
            // Armamos el comando "FFMPEG para convertir MP4 a .ts"

            System.out.println(" CONVERT: "+convert);
            boolean state = false;

            while (!state){
                state = this.splitterCommand(convert);
                if (!state){
                    System.out.println (" TIME TO DO IT AGAIN (CONVERT) "+convert );
                    Thread.sleep(10000);
                }

            }


            Files.delete(path);
            System.out.println(" [STEP 1] - TS file created / mp4 file deleted ");

            // 2 - Split the file
            String command ="";
            if (this.context.startsWith("kubernetes") || (this.context.startsWith("localkub"))){
                command	= 	"/tmp/ffmpeg -i ";
            }else{
                command	= 	"ffmpeg -i ";
            }

            command			+=	tsFile;
            command			+= 	" -codec copy -map 0 -f segment -segment_time 10 -segment_format mpegts  ";
            command			+= 	"/tmp/"+tsName+"_part_%d.ts";
            System.out.println(" SPLIT CMD: "+command);

           // System.out.println(" CONVERT: "+command);
            state = false;

            while (!state){
                state = this.splitterCommand(command);
                if (!state){
                    System.out.println (" TIME TO DO IT AGAIN (SPLITTER) "+command );
                    Thread.sleep(10000);
                }

            }
            //this.runBash(command);

            // 2.5 - Delete origin
            Files.delete(Paths.get(tsFile));
            System.out.println(" [STEP 2] - FILE Splitted / origin file removed  ");
            /*
            mp4 version

            -f mp4 -movflags frag_keyframe+empty_moov

            // FIRST TO ATOM
            String tsName 	= name.split(Pattern.quote("."))[0];
            tsName          +=  "atom.mp4";
            String tsFile   = "/tmp/"+tsName;
            String convert;
            if (!context.startsWith("kubernetes")){
                convert  = "ffmpeg -y -i "+path.toString()+" -movflags faststart -acodec copy -vcodec copy "+tsFile;
            }else{
                convert  = "/tmp/ffmpeg -y -i "+path.toString()+" -movflags faststart -acodec copy -vcodec copy "+tsFile;
            }

            //String convert  = "/tmp/ffmpeg -y -i "+path.toString()+" -movflags faststart -acodec copy -vcodec copy "+tsFile;
            this.processBuilder(convert);

            //String command	= 	"/tmp/ffmpeg -i ";
            String command;
            if (!context.startsWith("kubernetes")){
                command	= 	"ffmpeg -i ";
            }else{
                command	= 	"/tmp/ffmpeg -i ";
            }


            command			+=	tsFile;
            command			+= 	" -codec copy -map 0 -f segment -segment_time 10 -segment_format -movflags faststart mp4  ";
            command			+= 	"/tmp/"+tsName+"_part_%d.mp4";
            System.out.println(" SPLIT CMD: "+command);
            this.runBash(command);

            // 2.5 - Delete origin
            Files.delete(Paths.get(tsFile));
            System.out.println(" [STEP 2] - FILE Splitted / origin file removed  ");
              */
            // 3 - Obtain chunk's number

            String numberOfFiles = "find /tmp/ -name \""+tsName+"_part*\" 2>/dev/null | wc -l";
            int chunks = Integer.parseInt(this.runBash(numberOfFiles));

            // 4 - Create jobs in DB (based on params)
            // ADDING PARAMETERS URL
            //String params="480-720-240";
            String params=encodingParams;
            //String user = "yo";


            String[] qJobsFromParameters = null;
            try {
                qJobsFromParameters = params.split("-");
            } catch (Exception e) {
                if (params.length()>0){
                    qJobsFromParameters[0] = params;
                }else{
                    qJobsFromParameters[0] = "480";
                }

            }
            System.out.println(" [STEP 3] - Params loaded ");
            // 5 - Create sql jobs (only based on profiles) - new
            ArrayList <Integer> ids = new ArrayList<Integer>();
            for (String profiles : qJobsFromParameters) {
                String assignedQueue = tsName + "_profile_" + profiles;
                long initTime = System.currentTimeMillis();
                String sql = "insert into job (user,path,params,assignedQueue, state,totalparts,partscompleted,initTime,storageprovider) values ('"
                        + user + "','" + tsName + "','" + profiles + "','"+assignedQueue+"','new'," + chunks + ",'0'," + initTime + ",'" + cloudstorage + "');";
                System.err.println("SQL: " + sql);
                //this.mdbc.createConnection();


                PreparedStatement pstmt = this.mdbc.conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ids.add(pstmt.executeUpdate());



                //this.mdbc.st.close();
                //this.mdbc.conn.close();
            }



            String output ="";
            String saveName = "";
            boolean alreadyUploaded = false;
            String AzStorageAccount ="testdpkub";
            String keyStr = "o3czD5k/59lCXMUYa28H3H6SrnhXNLMSXV8y3ZPD1Aei9aZJ/Kp1E1oTsVdQ+d6WwTV7ogzKInvyC8gTxzignA==";
            String sasToken = this.GetFileSAS(AzStorageAccount, keyStr);
            TransferManager tm = TransferManagerBuilder.standard()
                    .withS3Client(this.s3)
                    .withMultipartUploadThreshold((long) (5 * 1024 * 1025))
                    .build();
            Upload upload;

            // 6 - Per each part, create "profile" tasks in queue System
            //this.connectRabbit();
            for (int i = 0; i < chunks; i++) {

                // STEP 9 - Con el video cargar la parte en la cola

                output = tsFile + "_part_" + i + ".ts";
                //System.out.println(" PART: " + output);
                // TEST IF VIDEO HAS ALREADY SPLITTED
                saveName = tsName + "_part_" + i + ".ts";

                 /*
                 MP4

                output = tsFile + "_part_" + i + ".mp4";
                //System.out.println(" PART: " + output);
                // TEST IF VIDEO HAS ALREADY SPLITTED
                saveName = tsName + "_part_" + i + ".mp4";
                */

                System.out.println(" WORKING ON: " + saveName);
                //if (!this.s3.doesObjectExist(this.spllitedS3Bucket, saveName)) {

                // 6.1 - Upload chunk to AZ
                if (cloudstorage.startsWith("az")) {
                    try {

                        String endpoint = "https://" + AzStorageAccount + ".blob.core.windows.net";
                        String AzSplitterContainer = "s3-bcra-splitted";

                        String head = endpoint + "/" + AzSplitterContainer + "/" + saveName;
                        int responseCode = this.httpHead(head);
                        // code = 200 exist ; code = 400 doesn't exist
                        if (responseCode != 200) {
                            String cmd = "curl -X PUT -T \"/tmp/" + saveName + "\" -H \"x-ms-date: $(date -u)\" -H \"x-ms-blob-type: BlockBlob\" \"https://" + AzStorageAccount + ".blob.core.windows.net/" + AzSplitterContainer + "/" + saveName + sasToken + "\"";
                            System.out.println(" CURL: " + cmd);
                            this.runBash(cmd);
                            System.out.println(" UPLOADED AZ " + saveName);
                        }


                    } catch (Exception e) {
                        System.err.println(" ERROR: " + e.getMessage());
                    }


                } else {
                    if (this.cloudstorage.startsWith("aws")){
                        try {
                            String AwsSpllitedS3Bucket = "s3-bcra-splitted";
                            String head = "https://" + AwsSpllitedS3Bucket + ".s3.us-east-1.amazonaws.com/" + saveName;
                            int responseCode = this.httpHead(head);
                            // code = 200 exist ; code = 400 doesn't exist
                            if (responseCode != 200) {
                                File file = new File("/tmp/" + saveName);
                                upload = tm.upload(AwsSpllitedS3Bucket, saveName, file);
                                upload.waitForCompletion();
                                //System.out.println(" FILE: " + saveName + " has been uploaded");
                                System.out.println("AWS  FILE: " + saveName + "has been uploaded ");
                            }

                        } catch (Exception e) {
                            System.err.println(" AWS ERROR : " + e.getMessage());
                        }
                    }else{
                        // IS LOCAL STORAGE
                        String saveCurl = "curl -X PUT -F \"file=@/tmp/" + saveName + "\" -H \"Content-Type: multipart/form-data\" \"http://" + this.cloudstorage + ":8080/uploadFile?folder=s3-bcra-splitted\"";
                        this.runBash(saveCurl);
                    }

                }

                // 6.2 - Create task in RabbitMQ (1 per each profile)
                // reconnect fix
                for (String profiles : qJobsFromParameters) {

                    String responseQueue = tsName + "_profile_" + profiles;

                    // String[] filters =
                    // msgRearmed.getEncodingProfiles().split(Pattern.quote("_"));
                    EncodingStructure profileCoding = this.obtainDetailsProfile(profiles);
                    Message msg = new Message();
                    //msg.setInsertedJobID(this.obtainDBID(responseQueue));
                    msg.setOriginalName(output);
                    msg.setName(responseQueue);
                    msg.setPart(i);
                    msg.setqParts(chunks);
                    msg.setService("videoCompression");
                    msg.setCloudStorage(cloudstorage);
                    //msg.setData(binaryData);
                    msg.setEncodingProfiles(profiles);
                    msg.setParamsEncoding(profileCoding);
                    jsonUtility.setType("Message");
                    jsonUtility.setObject(msg);
                    String encodedMsg = jsonUtility.toJson();

                    this.enterChannel.basicPublish("", this.jobsQueue, MessageProperties.PERSISTENT_TEXT_PLAIN,
                            encodedMsg.getBytes());
                    System.out.println(" JOB: " + responseQueue + "_"+i+" has been created and published on: "+this.rabbitHost);
                }

                //this.mdbc.st.close();
                //this.mdbc.conn.close();

                // NOW DELETE part
                Path deleting = Paths.get(output);
                Files.delete(deleting);

            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {

            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }

    private long obtainDBID(String responseQueue) {
        String sql = "select id from job where partscompleted=0 and state='new' and assignedQueue='" + responseQueue + "';";
        System.out.println(" ID : "+sql);
        System.out.println(" ---------------");
        long result = -1;
        this.mdbc.createConnection();
        try{
            ResultSet rs = this.mdbc.st.executeQuery(sql);
            rs.next();
            result = rs.getInt("id");
            this.mdbc.st.close();
            this.mdbc.conn.close();
        }catch (Exception e){
            System.out.println(" ERRR " + e.getMessage());

        }
        System.out.println(" IDRES : "+result);
        return result;
    }

    private EncodingStructure obtainDetailsProfile (String params){

        return this.paramsList.get(params);
    }

    public String runBash (String command){
        String totalLines = "";
        try {
            //System.out.println("BASH RUNNER STARTED");

            String[] cmdArray = {"/bin/bash", "-c", command};
            System.err.println(command);
            //System.out.println(" BASH RUNNER IS GOING TO RUN: ", cmdArray.toString());
            Process runner = Runtime.getRuntime().exec(cmdArray);


            BufferedReader stdOut = new BufferedReader(new InputStreamReader(runner.getInputStream()));
            BufferedReader stdErr = new BufferedReader(new InputStreamReader(runner.getErrorStream()));


            String line = "";

            while ((line = stdOut.readLine()) != null) {
                //System.err.println(" OUTPUT: " + line);
                totalLines=line;
            }





        } catch (IOException e) {
            // TODO Auto-generated catch block

        }
        return totalLines;
    }

    public void processBuilder(String command) {

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("/bin/bash", "-c", command);

        try {
            Process process = processBuilder.start();
            // blocked :(
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                System.out.println(" INPUTStream"+line); }

            int exitCode = process.waitFor();
            System.out.println("\n Exited with error code : " + exitCode);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    public boolean splitterCommand (String command) {
        boolean state = true;
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("/bin/bash", "-c", command);
        long pid;
        String totalLines;
        Process process = null;
        try {
            process = processBuilder.start();
            pid = process.pid();
            // blocked :(



            BufferedReader stdOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader stdErr = new BufferedReader(new InputStreamReader(process.getErrorStream()));


            String line = "";

           /*while ((line = stdErr.readLine()) != null) {
                //System.err.println(" OUTPUT: " + line);
                totalLines=line;
            }*/

            process.waitFor(5, TimeUnit.MINUTES);
            int status = process.exitValue();


            //1, TimeUnit.MILLISECONDS);



        } catch (Exception e) {
            process.destroy();
            state = false;
            System.out.println("---------------------------------------");
            System.out.println("\n PROBLEM: EXCEPT - task : " + command);
            System.out.println("\n PROBLEM: EXCEPT - MESSAGE : " + e.getMessage());
            System.out.println("---------------------------------------");


            //e.printStackTrace();
        }
        return state;
    }
    public String GetFileSAS(String accountName , String key ){

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

    public int httpHead (String stringUrl){
        int code = 0;
        HttpURLConnection urlConnection = null;
        System.setProperty("http.keepAlive", "false");
        try {
            URL url = new URL(stringUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("HEAD");
            code = urlConnection.getResponseCode();
            //System.out.println(" CODE: "+code);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return code;
    }

    public String GetFileSAS(){

        String accountName = "testdpkub";
        String key = "o3czD5k/59lCXMUYa28H3H6SrnhXNLMSXV8y3ZPD1Aei9aZJ/Kp1E1oTsVdQ+d6WwTV7ogzKInvyC8gTxzignA==";
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

    private void toolsConnection() {
        this.s3 = AmazonS3ClientBuilder.standard().withRegion(Regions.DEFAULT_REGION).build();
        this.s3InPath = "s3://s3-bcra-in";
        String dbname = "distributedProcessing";
        if (context.startsWith("test")){
            this.mariadbHost = "127.0.0.1";
            this.rabbitHost = "127.0.0.1";
        }
        String url = "jdbc:mariadb://" + mariadbHost + "/" + dbname;
        String username = "david";
        String password = "david";

        //this.mdbc = new MariaDBConnection(mariadbHost, dbname, username, url, password);
        //this.mdbc.createConnection();

        //System.out.println(" MARIADB CONNECTED");

        // DEFINE BASICS FOR RABBITMQ SERVICE

        String usernameRabbit = "admin";
        String passwordRabbit = "admin";
        this.factory = new ConnectionFactory();
        this.factory.setHost(rabbitHost);

        this.factory.setUsername(usernameRabbit);
        this.factory.setPassword(passwordRabbit);
        //System.out.println(" RABBIT CONNECTED");
        boolean durable = true;
        try {

            this.enterConnection = this.factory.newConnection();
            this.enterChannel = this.enterConnection.createChannel();
            this.enterChannel.queueDeclare(this.enterQueue, durable, false, false, null);
            this.enterChannel.queueDeclare(this.jobsQueue, durable, false, false, null);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            System.out.println(" ERROR: "+e.getMessage());
        } catch (TimeoutException e) {
            // TODO Auto-generated catch block
            System.out.println(" ERROR: "+e.getMessage());
        }
    }



}
