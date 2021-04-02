import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.SharedAccessAccountPolicy;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Pattern;


public class JoinerThread implements Runnable {
    String name;
    String baseName;
    String storageProvider;
    int parts;
    MariaDBConnection mdbc;
    ResultSet rs;
    String context;
    public JoinerThread (ResultSet rs, MariaDBConnection mdbc, String context){
       try {
           this.context = context;
           this.rs = rs;
           this.storageProvider = this.rs.getString("storageprovider");
           this.name = this.rs.getString("assignedQueue");
           this.parts = this.rs.getInt("totalparts");

           this.baseName = this.name;

           this.name = "/tmp/video/resultJoined/"+this.name+"_";
           this.mdbc = mdbc;



       }catch (Exception e ){

       }

    }
    @Override
    public void run() {
        try {
            String inMp4;
            String finalName    =   this.rs.getString("path");
            finalName           =   finalName.split(Pattern.quote("."))[0];
            finalName           +=  "_"+this.rs.getString("params");
            inMp4               =   finalName+".mp4";
            finalName           +=  ".ts";

            String ffmpegPath = "";
            String ffmpegToExecute;
            if (!this.context.startsWith("internet")){
                ffmpegToExecute = ffmpegPath+"/tmp/ffmpeg ";
            }else{
                ffmpegToExecute = ffmpegPath+"ffmpeg ";
            }

            int nThread = (int) Thread.currentThread().getId();
            FileWriter myWriter = new FileWriter("urls" + nThread);
            String sourceBased = "";
            String curl = "";
            String blobUrl ="" ;
            if (storageProvider.startsWith("az")) {
                String AzStorageAccount = "testdpkub";
                String keyStr = "o3czD5k/59lCXMUYa28H3H6SrnhXNLMSXV8y3ZPD1Aei9aZJ/Kp1E1oTsVdQ+d6WwTV7ogzKInvyC8gTxzignA==";
                String sasToken = this.GetFileSAS(AzStorageAccount, keyStr);
                String AzCompressedContainer = "s3-bcra-compressed";
                String AzFinishedContainer = "s3-bcra-finished";
                sourceBased = "https://" + AzStorageAccount + ".blob.core.windows.net/" + AzCompressedContainer + "/";
                //String target = msgRearmed.getName() + "_" + msgRearmed.getPart() + ".ts";
                curl = "curl -X PUT --data-binary @- -H \"x-ms-date: $(date -u)\" -H \"x-ms-blob-type: BlockBlob\" \"https://" + AzStorageAccount + ".blob.core.windows.net/" + AzFinishedContainer + "/" + finalName + sasToken + "\"";
                // FOR MP4 curl = "curl -X PUT --data-binary @- -H \"x-ms-date: $(date -u)\" -H \"x-ms-blob-type: BlockBlob\" \"https://" + AzStorageAccount + ".blob.core.windows.net/" + AzFinishedContainer + "/" + inMp4 + sasToken + "\"";
                blobUrl = "https://" + AzStorageAccount + ".blob.core.windows.net/" + AzFinishedContainer + "/" + finalName;

            } else {
                if (storageProvider.startsWith("aws")){
                    String AwsCompressedS3Bucket = "s3-bcra-compressed";
                    String AwsFinishedS3Bucket = "s3-bcra-finished";
                    String compressedAws = AwsCompressedS3Bucket + ".s3.us-east-1.amazonaws.com";
                    String finishedAws = AwsFinishedS3Bucket + ".s3.us-east-1.amazonaws.com";

                    String s3AccessKey = "AKIATKQHK5AW2H5R436F";
                    String s3SecretKey = "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";

                    sourceBased = "https://" + compressedAws + "/";


                    // DATE and HASH for curl
                    SimpleDateFormat sdf = new SimpleDateFormat("YYYYMMdd'T'HHmmss'Z'");
                    sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
                    String dateFormatted = sdf.format(new Date());
                    String relativePath = "/" + AwsFinishedS3Bucket + "/"+finalName; // + target;
                    // FOR MP4 String relativePath = "/" + AwsFinishedS3Bucket + "/"+inMp4; // + target;
                    String contType = "application/octet-stream";
                    String stringToSign = "PUT\n\n" + contType + "\n" + dateFormatted + "\n" + relativePath;
                    String signature = hmacSha1(stringToSign, s3SecretKey);
                    curl = "curl -X PUT --data-binary @- -H \"Host: " + finishedAws + "\" -H \"Date: " + dateFormatted + "\" -H \"Content-Type: " + contType + "\" -H \"Authorization: AWS " + s3AccessKey + ":" + signature + "\" \"https://" + finishedAws + "/" + finalName + "\"\n";
                    // FOR MP4 curl = "curl -X PUT --data-binary @- -H \"Host: " + finishedAws + "\" -H \"Date: " + dateFormatted + "\" -H \"Content-Type: " + contType + "\" -H \"Authorization: AWS " + s3AccessKey + ":" + signature + "\" \"https://" + finishedAws + "/" + inMp4 + "\"\n";
                    blobUrl = "https://" + finishedAws + "/" + finalName;
                }else{
                    sourceBased = "http://" + this.context+":8080/downloadFile?folder=s3-bcra-compressed&fileName=";
                    curl = 	"curl -X PUT -F \"file=@-\" -H \"Content-Type: multipart/form-data\" -H \"Host: " + this.context + "\" \"http://" + this.context + ":8080/uploadFile?folder=s3-bcra-finished&fileName=" + finalName + "\"\n";
                    blobUrl = "http://" + this.context + ":8080/downloadFile?folder=s3-bcra-finished&fileName="+finalName;
                }

            }
            for (int k=0; k<this.parts; k++) {
                myWriter.write("file '"+sourceBased+this.baseName+"_"+k+".ts'\n");
            }
            myWriter.close();
            //this.runBash(ffmpegToExecute);
            String concatenate = "";
            if (!this.context.startsWith("internet")){
                concatenate  =   "/tmp/ffmpeg -f concat -safe 0 -protocol_whitelist file,https,tls,http,tcp -i urls"+nThread+" -c copy -f mpegts pipe:1 | ";
            }else{
                concatenate  =   "ffmpeg -f concat -safe 0 -protocol_whitelist file,https,tls,http,tcp -i urls"+nThread+" -c copy -f mpegts pipe:1 | ";
            }

            concatenate         +=  curl;
            System.out.println(" VIDEO: "+finalName);
            this.runBash(concatenate);
            Files.deleteIfExists(Paths.get("urls" + nThread));

            /*
            if (!context.startsWith("kubernetes")){
                ffmpegToExecute  =   "ffmpeg";
            }else{
                ffmpegToExecute  =   "/tmp/ffmpeg";
            }

            for (int k=0; k<this.parts; k++) {
                ffmpegToExecute+= " -i \""+sourceBased+ this.baseName+"_"+k+".ts\"";
            }
            ffmpegToExecute +=  " -filter_complex concat=n="+this.parts+":v=1:a=1 -f mpegts pipe:1 | ";
            ffmpegToExecute +=  curl;
            System.out.println(" VIDEO: "+finalName);

            this.runBash(ffmpegToExecute);
            */


            // [STEP 2] - AFTER I Have FINISHED joiner task UPDATE db
            long endTime = System.currentTimeMillis();

            long executionTime =  (endTime- rs.getLong("initTime"));
            String query = "update job set state='finished', endTime="+endTime+", bloburl='"+blobUrl+"' , executionTime="+executionTime+" where id="+rs.getInt("id")+";";
            System.out.println(" SQL: "+query);

            this.mdbc.createConnection();
            this.mdbc.st.executeQuery(query);
            this.mdbc.st.close();
            this.mdbc.conn.close();

        }catch (Exception e){
            System.out.println(" ERROR FOUND");
        }


    }

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
    public void runBash(String command) {
        String totalLines = "";
        try {
           
            String[] cmdArray = { "/bin/bash", "-c", command };
            System.err.println(command);
           
            Process runner = Runtime.getRuntime().exec(cmdArray);
           
            runner.waitFor();
            Thread.sleep(3000);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

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
}
