package DEV_WORKERx86.WORKx86;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.SharedAccessAccountPolicy;
import com.rabbitmq.client.Channel;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;



public class FFMpegClass {
	Message msgRearmed;
	JsonUtility jsonUt; 
	String workerName;
	long idForAck;
	String ipSpringServer;
	HttpURLConnection con;
	Timestamp timestamp;
	long initTime;
	int port;
	MariaDBConnection mdbc;
	Channel enterChannel;
	Long deliveryTag;
	String context;

	public FFMpegClass(Message msgRearmed, JsonUtility jsonUt, String workerName, long idForAck, String ipSpringServer, HttpURLConnection con, long initTime, int port, MariaDBConnection mdbc, Channel enterChannel, Long deliveryTag, String context) throws InterruptedException {
		this.enterChannel = enterChannel;
		this.deliveryTag = deliveryTag;
		this.msgRearmed =msgRearmed;
		this.context = context;
		this.jsonUt = jsonUt; 
		this.workerName = workerName;
		this.idForAck = idForAck;
		this.ipSpringServer = ipSpringServer; 
		this.con = con;
		String OS = System.getProperty("os.name").toLowerCase();
		String cmdRunner="";
		this.port = port;
		this.mdbc = mdbc;

		initTime = initTime;
		
		// STEP 2.5 - Save the queue where i must reply my msg
		// CHANGE 1 - msgRearmed.getName()
		String returnQueue = msgRearmed.getName();
		System.out.println(" [STEP 1] - Job Obtained: "+returnQueue + msgRearmed.part);
		//System.out.println(" [STEP 2] - CONTEXT: "+this.context);
		String basePath = "";
		if (System.getProperty("os.name").startsWith("Windows")){
			basePath = "C:/DTP/";
		}else {
			basePath = "/tmp/";
		}

		String saveVideoName=msgRearmed.getName()+"_"+msgRearmed.getPart();
		try {
			String[] parts = saveVideoName.split("/");
			saveVideoName = parts[2];
		} catch (Exception e) {
			//TODO: handle exception
		}

		EncodingStructure parametersFromMsg = msgRearmed.getParamsEncoding();
		String cloudStorage = msgRearmed.getCloudStorage();
		String magic = "";
		String source = "";
		String curl = "";
		if (this.context.startsWith("az")) {
			String AzStorageAccount ="testdpkub";
			String keyStr = "o3czD5k/59lCXMUYa28H3H6SrnhXNLMSXV8y3ZPD1Aei9aZJ/Kp1E1oTsVdQ+d6WwTV7ogzKInvyC8gTxzignA==";
			String sasToken = this.GetFileSAS(AzStorageAccount, keyStr);
			String AzSplittedContainer = "s3-bcra-splitted";
			String AzCompressedContainer = "s3-bcra-compressed";
			source = "https://" + AzStorageAccount + ".blob.core.windows.net/" + AzSplittedContainer + "/" + msgRearmed.getOriginalName().substring(5,msgRearmed.getOriginalName().length());
			String target = msgRearmed.getName()+"_"+msgRearmed.getPart()+".ts";
			// FOR MP4 FORMAT String target = msgRearmed.getName()+"_"+msgRearmed.getPart()+".mp4";
			curl = "curl -X PUT --data-binary @- -H \"x-ms-date: $(date -u)\" -H \"x-ms-blob-type: BlockBlob\" \"https://"+AzStorageAccount+".blob.core.windows.net/"+AzCompressedContainer+"/"+target+sasToken+"\"";

		}else{
			if (this.context.startsWith("aws")) {
				String inS3Bucket       = "s3-bcra-splitted";
				String outS3Bucket      = "s3-bcra-compressed";
				String inAws            = inS3Bucket + ".s3.us-east-1.amazonaws.com";
				String outAws            = outS3Bucket + ".s3.us-east-1.amazonaws.com";

				String s3AccessKey   = "AKIATKQHK5AW2H5R436F";
				String s3SecretKey   = "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";

				source       = "https://" + inAws + "/" + msgRearmed.getOriginalName();
				//String source = "https://vod-progressive.akamaized.net/exp=1588550387~acl=%2A%2F1594271967.mp4%2A~hmac=943f21a32958247e6fc36566c761d0316aee5ef7a2d405c0bb62cff948821285/vimeo-prod-skyfire-std-us/01/1094/15/380473795/1594271967.mp4?download=1&filename=Trees+-+30222.mp4";
				String target       = msgRearmed.getName()+"_"+msgRearmed.getPart()+".ts";
				// FOR MP4 FORMAT String target       = msgRearmed.getName()+"_"+msgRearmed.getPart()+".mp4";

				// DATE and HASH for curl
				SimpleDateFormat sdf = new SimpleDateFormat("YYYYMMdd'T'HHmmss'Z'");
				sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
				String dateFormatted = sdf.format(new Date());
				String relativePath  = "/" + outS3Bucket + "/" + target;
				String contType   = "application/octet-stream";
				String stringToSign  = "PUT\n\n" + contType + "\n" + dateFormatted + "\n" + relativePath;
				String signature     = hmacSha1(stringToSign, s3SecretKey);
				curl = "curl -X PUT --data-binary @- -H \"Host: " + outAws + "\" -H \"Date: " + dateFormatted + "\" -H \"Content-Type: " + contType + "\" -H \"Authorization: AWS " + s3AccessKey + ":" + signature + "\" \"https://" + outAws + "/" + target + "\"\n";
			}else{
				// IS A LOCAL STORAGE
				String target       = msgRearmed.getName()+"_"+msgRearmed.getPart()+".ts";

				source = "http://" + this.context + ":8080/downloadFile?folder=s3-bcra-splitted&fileName=" + msgRearmed.getOriginalName().substring(5);

				curl = 	"curl -X PUT -F \"file=@-\" -H \"Content-Type: multipart/form-data\" -H \"Host: " + this.context + "\" \"http://" + this.context + ":8080/uploadFile?folder=s3-bcra-compressed&fileName=" + target + "\"\n";
			}


		}
		if (this.context.startsWith("kubernetes") || (this.context.startsWith("localkub"))){
			magic       		= "curl \""+ source + "\" | " + basePath+"/tmp/ffmpeg -i -";
		}else{
			magic       		= "curl \""+ source + "\" | " + basePath+"ffmpeg -i -";
		}


		magic               += " -s "+parametersFromMsg.getVideoResolution();
		magic               += " -aspect 16:9 -c:v "+parametersFromMsg.getVideoCodec();
		magic               += " -b:v "+parametersFromMsg.getVideoBitrate()+"k";
		magic               += " -profile:v "+parametersFromMsg.getVideoProfile();
		magic               += " -level "+parametersFromMsg.getVideoLevel();
		magic               += " -preset "+parametersFromMsg.getVideoPreset();
		magic               += " -threads 0 ";
		magic               += "-f mpegts - | ";
		// FOR MP4 FORMAT magic               += "-f mp4 -movflags frag_keyframe+empty_moov - | ";
		magic				+= curl;

		System.out.println("[STEP 2] - Run Process (Cloud) "+magic);

		try {
			this.runBash(magic);
			System.out.println("[STEP 3] -  File has been processed and uploaded, update DB and RMQ");
			//Thread.sleep(100000);

			long endTime = System.currentTimeMillis();
			String osType= System.getProperty("os.arch");

			String query = "insert into jobTracker (service, job, part, workerName, workerArchitecture, initTime, endTime, executionTime) values ('"
					+ msgRearmed.getService() + "', '" + msgRearmed.getName() + "', '" + msgRearmed.getPart() + "', '" + workerName + "','" + "x86"
					+ "'," + initTime + "," + endTime + "," + (endTime-initTime) + ")";


			this.mdbc.createConnection();
			this.mdbc.doInsertOperation(query);



			//System.out.println("SQL INSERT "+query);

			//query = "update job set partscompleted=partscompleted+1 where assignedQueue='" + msgRearmed.getName() + "' and state='new';";
			query = "update job set partscompleted=partscompleted+1 where assignedQueue='" + msgRearmed.getName() + "';";
			System.out.println("SQL UPDATE: "+query);
			this.mdbc.st.executeQuery(query);

			this.mdbc.closeConnection();

			enterChannel.basicAck(deliveryTag, false);
			System.out.println("ACK: "+deliveryTag);
			System.out.println("[STEP 4] -  DB and RMQ has been updated");
			// REMOVE FROM ACK LIST

		} catch (IOException e) {
				// TODO Auto-generated catch block
			try {
				enterChannel.basicNack(deliveryTag, false, true);
			} catch (IOException ioException) {
				ioException.printStackTrace();
			}
			e.printStackTrace();
			} catch (SQLException e) {
			try {
				enterChannel.basicNack(deliveryTag, false, true);
			} catch (IOException ioException) {
				ioException.printStackTrace();
			}
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

	public void runBash(String command) {
		String totalLines = "";
		try {
			String[] cmdArray = { "/bin/bash", "-c", command };
			Process runner = Runtime.getRuntime().exec(cmdArray);
			runner.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
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



}

