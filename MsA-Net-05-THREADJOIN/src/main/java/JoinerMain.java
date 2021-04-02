import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.SharedAccessAccountPolicy;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlob;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.ListBlobItem;
import com.rabbitmq.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

/**
 * Hello world!
 *
 */
public class JoinerMain
{
	String rabbitHost;
	ConnectionFactory factory;
	String mariadbHost;
	MariaDBConnection mdbc;
	String joinerQueue = "joinerQueue";
	Connection enterConnection;
	Channel enterChannel;
	Connection pendantConnection;
	Channel pendantChannel;
	String username;
	String password;
	private static final Logger log = LoggerFactory.getLogger(JoinerMain.class);
	String context;
	public JoinerMain(){
		System.out.println( "Unifier has been started!" );

		try {
			// content = new String(Files.readAllBytes(file.toPath()));
			context = "localkub";
			System.out.println(context);
			String urlGit = "https://raw.githubusercontent.com/dpetrocelli/LinuxRepo/master/configFile";
			this.downloadConfigurationFile(urlGit, context);

			String accountName = "testdpkub";
			String key = "o3czD5k/59lCXMUYa28H3H6SrnhXNLMSXV8y3ZPD1Aei9aZJ/Kp1E1oTsVdQ+d6WwTV7ogzKInvyC8gTxzignA==";
			String storageConnectionString = "DefaultEndpointsProtocol=https;AccountName="+accountName+";AccountKey="+key+";EndpointSuffix=core.windows.net";
			String name = "";
			CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
			// Create the blob client.
			CloudBlobClient blobClient = storageAccount.createCloudBlobClient();

			this.toolsConnection();

			boolean autoAck = false;
			this.enterChannel.basicConsume(this.joinerQueue, autoAck, "",
					new DefaultConsumer(this.enterChannel) {
						@Override
						public void handleDelivery(String consumerTag,
												   Envelope envelope,
												   AMQP.BasicProperties properties,
												   byte[] body)
								throws IOException {

							try {
								String routingKey = envelope.getRoutingKey();
								String contentType = properties.getContentType();
								long deliveryTag = envelope.getDeliveryTag();
								log.info("STEP 1 - MSG Obtained ");

								mdbc.createConnection();
								// STEP 1 - Obtain Job structure from msg stored in rabbitMQ
								String sql = "select * from job where id=" + new String(body);
								log.info (" SQL: "+sql);
								ResultSet rs = mdbc.st.executeQuery(sql);

								mdbc.st.close();
								mdbc.conn.close();
								log.info("STEP 2 - DB Structure obtained ");
								String removeCompressed;
								String removeInAndSplitted;

								if (!Objects.isNull(rs)) {

									try{
										rs.next();
										// [STEP 2] - Once opened join parts and finish the task
										int totalparts = rs.getInt("totalparts");
										int partscompleted = rs.getInt("partscompleted");
										String state = rs.getString("state");
										String job = rs.getString("assignedQueue");
										String storageProvider = rs.getString("storageprovider");
										log.info("STEP 3 - ResultSet is not null "+totalparts+" / "+partscompleted+" / "+state+" / "+job+" / "+storageProvider);
										// STEP 3 - Obtain basic structure

										String baseName = job;
										String name = "/tmp/video/resultJoined/" + baseName + "_";
										String finalName = rs.getString("path");
										finalName = finalName.split(Pattern.quote("."))[0];
										finalName += "_" + rs.getString("params");

										finalName += ".ts";

										String ffmpegPath = "";
										String ffmpegToExecute;
										if (!context.startsWith("internet")) {
											ffmpegToExecute = ffmpegPath + "/tmp/ffmpeg ";
										} else {
											ffmpegToExecute = ffmpegPath + "ffmpeg ";
										}

										int nThread = (int) Thread.currentThread().getId();
										FileWriter myWriter = new FileWriter("urls" + nThread);
										String sourceBased = "";
										String curl = "";
										String blobUrl = "";

										if (storageProvider.startsWith("az")) {
											String AzStorageAccount = "testdpkub";
											String keyStr = "o3czD5k/59lCXMUYa28H3H6SrnhXNLMSXV8y3ZPD1Aei9aZJ/Kp1E1oTsVdQ+d6WwTV7ogzKInvyC8gTxzignA==";
											String sasToken = GetFileSAS(AzStorageAccount, keyStr);
											String AzCompressedContainer = "s3-bcra-compressed";
											String AzFinishedContainer = "s3-bcra-finished";
											sourceBased = "https://" + AzStorageAccount + ".blob.core.windows.net/" + AzCompressedContainer + "/";
											//String target = msgRearmed.getName() + "_" + msgRearmed.getPart() + ".ts";
											curl = "curl -X PUT --data-binary @- -H \"x-ms-date: $(date -u)\" -H \"x-ms-blob-type: BlockBlob\" \"https://" + AzStorageAccount + ".blob.core.windows.net/" + AzFinishedContainer + "/" + finalName + sasToken + "\"";
											// FOR MP4 curl = "curl -X PUT --data-binary @- -H \"x-ms-date: $(date -u)\" -H \"x-ms-blob-type: BlockBlob\" \"https://" + AzStorageAccount + ".blob.core.windows.net/" + AzFinishedContainer + "/" + inMp4 + sasToken + "\"";
											blobUrl = "https://" + AzStorageAccount + ".blob.core.windows.net/" + AzFinishedContainer + "/" + finalName;

										} else {
											if (storageProvider.startsWith("aws")) {
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
												String relativePath = "/" + AwsFinishedS3Bucket + "/" + finalName; // + target;
												// FOR MP4 String relativePath = "/" + AwsFinishedS3Bucket + "/"+inMp4; // + target;
												String contType = "application/octet-stream";
												String stringToSign = "PUT\n\n" + contType + "\n" + dateFormatted + "\n" + relativePath;
												String signature = hmacSha1(stringToSign, s3SecretKey);
												curl = "curl -X PUT --data-binary @- -H \"Host: " + finishedAws + "\" -H \"Date: " + dateFormatted + "\" -H \"Content-Type: " + contType + "\" -H \"Authorization: AWS " + s3AccessKey + ":" + signature + "\" \"https://" + finishedAws + "/" + finalName + "\"\n";
												// FOR MP4 curl = "curl -X PUT --data-binary @- -H \"Host: " + finishedAws + "\" -H \"Date: " + dateFormatted + "\" -H \"Content-Type: " + contType + "\" -H \"Authorization: AWS " + s3AccessKey + ":" + signature + "\" \"https://" + finishedAws + "/" + inMp4 + "\"\n";
												blobUrl = "https://" + finishedAws + "/" + finalName;
											} else {
												sourceBased = "http://" + context + ":8080/downloadFile?folder=s3-bcra-compressed&fileName=";
												curl = "curl -X PUT -F \"file=@-\" -H \"Content-Type: multipart/form-data\" -H \"Host: " + context + "\" \"http://" + context + ":8080/uploadFile?folder=s3-bcra-finished&fileName=" + finalName + "\"\n";
												blobUrl = "http://" + context + ":8080/downloadFile?folder=s3-bcra-finished&fileName=" + finalName;
											}

										}
										log.info("REARM: "+storageProvider);
										for (int k=0; k<totalparts; k++) {
											myWriter.write("file '"+sourceBased+baseName+"_"+k+".ts'\n");
										}
										myWriter.close();
										//this.runBash(ffmpegToExecute);
										String concatenate = "";
										if (!context.startsWith("internet")){
											concatenate  =   "/tmp/ffmpeg -f concat -safe 0 -protocol_whitelist file,https,tls,http,tcp -i urls"+nThread+" -c copy -f mpegts pipe:1 | ";
										}else{
											concatenate  =   "ffmpeg -f concat -safe 0 -protocol_whitelist file,https,tls,http,tcp -i urls"+nThread+" -c copy -f mpegts pipe:1 | ";
										}

										concatenate         +=  curl;
										System.out.println(" STEP 4 - PROCCESS: "+finalName);
										runBash(concatenate);
										Files.deleteIfExists(Paths.get("urls" + nThread));

										long endTime = System.currentTimeMillis();

										long executionTime =  (endTime- rs.getLong("initTime"));
										String query = "update job set state='finished', endTime="+endTime+", bloburl='"+blobUrl+"' , executionTime="+executionTime+" where id="+rs.getInt("id")+";";
										System.out.println(" SSTEP 5 - UPDATE DB : "+query);

										mdbc.createConnection();
										mdbc.st.executeQuery(query);
										mdbc.st.close();
										mdbc.conn.close();
										enterChannel.basicAck(deliveryTag, false);

										removeCompressed = rs.getString("assignedQueue");
										removeInAndSplitted = rs.getString("path");

										System.out.println(" STEP 6 - Delete PROCCESS: "+finalName);

										if (context.startsWith("az")){

											String compressedBucket = "s3-bcra-compressed";
											remover(blobClient, removeCompressed, compressedBucket);

											String splittedBucket = "s3-bcra-splitted";
											remover(blobClient, removeInAndSplitted, splittedBucket);

										}else{
											if (context.startsWith("aws")){
												// NOTHING
											}else{
												// REMOVE SPLITTED
												removeAll("s3-bcra-splitted", removeInAndSplitted);
												// REMOVE COMPRESSED
												removeAll("s3-bcra-compressed", removeCompressed);
											}
										}

									}catch (Exception e){
										log.info("ERROR: "+e.getMessage());
										try {
											enterChannel.basicNack(deliveryTag, false, true);
										} catch (IOException ioException) {
											ioException.printStackTrace();
										}
										e.printStackTrace();
									}




								}


							} catch (Exception e) {
								System.err.println(" Error with RabbitMQ Consume Looper ");

							}

						}
					});






			} catch (Exception e) {
			System.err.println(" NOt CONNECTED ");
			}

	}

	private void removeAll (String folder, String pattern){

		String url = "curl -X DELETE \"http://"+this.context+":8080/removeAllFiles?folder="+folder+"&pattern="+pattern+"\"";
		this.runBash(url);

	}
	private void remover(CloudBlobClient blobClient, String remove, String bucket) {
		// Loop into compressed container looking for assignedQueue profiles ended

		try {
			String name = "";
			CloudBlobContainer container = blobClient.getContainerReference(bucket);
			for (ListBlobItem blobItem : container.listBlobs()) {
				try{
					CloudBlob cloudBlob = (CloudBlob) blobItem;
					name = ((CloudBlob) blobItem).getName();


						if (name.contains(remove)){
							cloudBlob.deleteIfExists();
							log.info("CONTAINER: "+bucket+ " / file: "+name + " DELETED");

						}

				}catch (Exception e){

				}

			}
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (StorageException e) {
			e.printStackTrace();
		}

		//
	}





	public static void main( String[] args )
    {
        JoinerMain joinerMain = new JoinerMain();
    }

	private void downloadConfigurationFile(String url, String mode) {

		// [STEP 1] - Obtain Job
		try {
			URL obj = new URL(url);
			System.out.println(" Obtaining Configuration file");
			HttpURLConnection con;
			if (this.context.startsWith("localkub")){
				Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("192.168.1.231", 80));
				con = (HttpURLConnection) obj.openConnection(proxy);
			}else{
				con = (HttpURLConnection) obj.openConnection();
			}
			int responseCode = con.getResponseCode();

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();
			int i = 0;
			while ((inputLine = in.readLine()) != null) {
				System.out.println(" MSG: " + inputLine);
				if (i == 0) {
					if (mode.startsWith("kubernet")){
						this.rabbitHost = inputLine.split(":")[1];
					}else{
						this.rabbitHost = inputLine.split(":")[2];
					}

					//this.rabbitHost = "a16e64467fa6b4bde9adaf78edc87fb1-992766720.us-east-1.elb.amazonaws.com";
				} else {
					if (i==1){
						if (mode.startsWith("kubernet")){
							this.mariadbHost = inputLine.split(":")[1];
						}else{
							this.mariadbHost = inputLine.split(":")[2];
						}

						//this.mariadbHost = "af0c97f9cb85140dd8ca759fd1b3ca5b-1051178316.us-east-1.elb.amazonaws.com";
					}if (i == 2) {
						if (mode.startsWith("kubernet")) {
							this.context = inputLine.split(":")[1];
						} else {
							this.context = inputLine.split(":")[2];
						}

						//this.mariadbHost = "af0c97f9cb85140dd8ca759fd1b3ca5b-1051178316.us-east-1.elb.amazonaws.com";
					}
				}

				i++;
			}
			in.close();

		} catch (Exception e) {
			// TODO: handle exception
		}

	}

	private void toolsConnection() {
		//String hostDB = "52.147.212.141";
		//String hostDB = mariadbHost;

		String dbname = "distributedProcessing";
		String url = "jdbc:mariadb://" + mariadbHost + "/" + dbname;
		String username = "david";
		String password = "david";

		this.mdbc = new MariaDBConnection(mariadbHost, dbname, username, url, password);

		this.mdbc.createConnection();
		log.info(" MARIADB CONNECTED");

		String usernameRabbit = "admin";
		String passwordRabbit = "admin";
		this.factory = new ConnectionFactory();
		this.factory.setHost(rabbitHost);

		this.factory.setUsername(usernameRabbit);
		this.factory.setPassword(passwordRabbit);
		log.info(" RABBIT CONNECTED");
		boolean durable = true;
		try {

			this.enterConnection = this.factory.newConnection();
			this.enterChannel = this.enterConnection.createChannel();
			this.enterChannel.queueDeclare(this.joinerQueue, durable, false, false, null);


		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println(" ERROR: "+e.getMessage());
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			System.out.println(" ERROR: "+e.getMessage());
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
