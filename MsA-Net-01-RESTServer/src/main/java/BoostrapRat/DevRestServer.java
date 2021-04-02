package BoostrapRat;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import com.azure.storage.blob.BlobContainerClient;
import com.google.gson.Gson;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.SharedAccessAccountPolicy;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.*;
import io.netty.util.internal.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

@RestController
@CrossOrigin(origins = "*", methods= {RequestMethod.GET,RequestMethod.POST})
public class DevRestServer {
	String jobsQueue = "jobsQueue";
	String pendantQueue = "pendantQueue";
	String enterQueue = "enterQueue";
	ConnectionFactory factory;
	Connection enterConnection;
	Channel enterChannel;
	Connection pendantConnection;
	Channel pendantChannel;
	JsonUtility jsonUtility;
	String username;
	String password;
	HashMap<String, EncodingStructure> paramsList;
	MariaDBConnection mdbc;
	String context;
	HashMap<String, ArrayList<String>> filterParameters;
	HashMap<Long, Long> AckService;
	JsonUtility jsonUt;
	Gson gson;
	//ManageAckList mal;
	AckDatabaseOperations dop;
	String rabbitHost;
	String mariadbHost;
	String basePath;
	AmazonS3 s3;
	String s3UploadPath;
	String s3InPath;
	BlobContainerClient azureBlob;
	String azureUploadBlob;
	String blobAccount;
	String localStorage;

	private static final Logger log = LoggerFactory.getLogger(DevRestServer.class);

	public DevRestServer() {

		// General GSON object
		this.basePath = "/tmp/";
		this.jsonUt = new JsonUtility();
		this.gson = new Gson();
		this.AckService = new HashMap<Long, Long>();

		this.jsonUtility = new JsonUtility();



		try {
			// content = new String(Files.readAllBytes(file.toPath()));
			context = "localkub";
			//context = "test";
			System.out.println(context);
			String urlGit = "https://raw.githubusercontent.com/dpetrocelli/LinuxRepo/master/configFile";
			this.downloadConfigurationFile(urlGit, context);

			System.out.println(this.rabbitHost + "//" + this.mariadbHost);
			this.toolsConnection();
			this.paramsList = new HashMap<String, EncodingStructure>();
			this.loadParameters();
			this.filterParameters = new HashMap<String, ArrayList<String>>();
			// ACTIVATE THREAD FOR LOOPING AND DELETING UNCORRESPONDANT MSG int

			int timeCheckInterval = 5000;
			int timeoutPackage = 60000;
			/*
				//dop = new AckDatabaseOperations(this.enterChannel, this.mdbc, timeCheckInterval, timeoutPackage);
				//mal = new ManageAckList(this.enterChannel, this.mdbc, timeCheckInterval, timeoutPackage);
				//Thread threadACK = new Thread(mal);
				//threadACK.start();
			*/
			this.filterParameters = new HashMap<String, ArrayList<String>>();
			System.out.println("FILTER:" + filterParameters.toString());

		} catch (Exception e) {

		}

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

		this.mdbc = new MariaDBConnection(mariadbHost, dbname, username, url, password);
		this.mdbc.createStructure();
		//

		log.info(" MARIADB CONNECTED");

		// DEFINE BASICS FOR RABBITMQ SERVICE

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

	private void downloadConfigurationFile(String url, String mode) {

		// [STEP 1] - Obtain Job
		try {
			URL obj = new URL(url);
			HttpURLConnection con = null;
			System.out.println(" Obtaining Configuration file");
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
					}else {
						if (i == 2) {
							if (mode.startsWith("kubernet")) {
								this.localStorage = inputLine.split(":")[1];
							} else {
								this.localStorage = inputLine.split(":")[2];
							}

							//this.mariadbHost = "af0c97f9cb85140dd8ca759fd1b3ca5b-1051178316.us-east-1.elb.amazonaws.com";
						}
					}
						/*else{
								this.ipSpringServer = inputLine.split(":")[1];
							}*/
				}

				i++;
			}
			in.close();

		} catch (Exception e) {
			// TODO: handle exception
		}

	}




	@RequestMapping(value = "/listJobs", method = RequestMethod.GET)
	public ResponseEntity<String> listJobs(@RequestParam("name") String name) throws InterruptedException {
		String sql = "select * from job where user='" + name + "'";
		StringBuilder sb = null;
		try {
			ResultSet rs = this.mdbc.st.executeQuery(sql);
			// iterate through the java resultset
			ArrayList<Job> response = new ArrayList<Job>();
			while (rs.next()) {
				/*Job job = new Job(rs.getInt("id"), rs.getString("user"), rs.getString("path"), rs.getString("params"),
						rs.getString("state"), rs.getInt("totalparts"), rs.getInt("partscompleted"));
				response.add(job);*/
			}

			// jsonUtility.setType("Job");

			sb = new StringBuilder();
			for (Job d : response) {
				sb.append(this.gson.toJson(d));
			}

		} catch (Exception e) {
			// TODO: handle exception
		}

		return new ResponseEntity<String>(sb.toString().trim(), HttpStatus.OK);
	}

	// --------- RELATED TO UPLOAD JOBS --------- //



	@PostMapping("/upload")
	//public ResponseEntity<String> updSystem(@RequestParam("cloudstorage") String cloudstorage, @RequestParam("file") MultipartFile file) throws InterruptedException {
	public ResponseEntity<String> updSystem(@RequestParam("file") MultipartFile file, @RequestParam("user") String user, @RequestParam("encodingParams") String encodingParams) throws InterruptedException {
		try {
			/*
			a = buffer
			while info {
			a + = inputstream.read()
			}
			 */
			//String cloudstorage = "azure";
			String cloudstorage = this.localStorage;
			String name = file.getOriginalFilename();
			int randomNum = ThreadLocalRandom.current().nextInt(1, 100000+ 1);
			name = name.substring(0,(name.length()-4))+randomNum+"."+name.substring((name.length()-3),name.length());
			String newPath = "/tmp/" + name.substring(0,(name.length()-4))+randomNum+"."+name.substring((name.length()-3),name.length());
			System.out.println(" NEWPATH: "+newPath);
			Path path = Paths.get(newPath);



			Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			//String msg = this.createAndSaveJob(cloudstorage, path, name);
			//this.splitAndSave(cloudstorage, path, name, user, encodingParams);
			System.out.println(" [STEP 0] - FILE: "+name+ " has been stored");
			try{
				ThreadRequest tr = new ThreadRequest(cloudstorage, path, name, user, encodingParams, this.mariadbHost, this.rabbitHost, context, this.mdbc);
				Thread trThread = new Thread(tr);
				trThread.start();
				trThread.join();
				System.out.println(" FILE: "+name+ " has been published");
				return new ResponseEntity<String>("  FILE: "+name+ " has been published" , HttpStatus.OK);
			} catch (Exception e){
				return new ResponseEntity<String>( " File hasn't been published" , HttpStatus.BAD_REQUEST);
			}


		} catch (Exception e) {
			//throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
			return new ResponseEntity<String>( "COULDN't store file " , HttpStatus.BAD_REQUEST);
		}

	}

	private void splitAndSave(String cloudstorage, Path path, String name, String user, String encodingParams) {
		try{
			// 1 - Create TS
			String tsName 	= name.split(Pattern.quote("."))[0];
			tsName          +=  ".ts";

			String tsFile   = "/tmp/"+tsName;
			String convert = "";
			//if (this.context.startsWith("kubernetes") || (this.context.startsWith("localkub"))){
				convert  = "/tmp/ffmpeg -y -i "+path.toString()+" -acodec copy -vcodec copy "+tsFile;
			//}else{
			//	convert  = "ffmpeg -y -i "+path.toString()+" -acodec copy -vcodec copy "+tsFile;
			//}

			this.processBuilder(convert);
			Files.delete(path);

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
			this.runBash(command);

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
				qJobsFromParameters[0] = params;
			}

			// 5 - Create sql jobs (only based on profiles)
			for (String profiles : qJobsFromParameters) {
				String assignedQueue = tsName + "_profile_" + profiles;
				long initTime = System.currentTimeMillis();
				String sql = "insert into job (user,path,params,assignedQueue, state,totalparts,partscompleted,initTime,storageprovider) values ('"
						+ user + "','" + tsName + "','" + profiles + "','"+assignedQueue+"','new'," + chunks + ",'0'," + initTime + ",'" + cloudstorage + "');";
				System.err.println("SQL: " + sql);
				this.mdbc.createConnection();


				PreparedStatement pstmt = this.mdbc.conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
				pstmt.executeUpdate();
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
				for (int i = 0; i < chunks; i++) {

					// STEP 9 - Con el video cargar la parte en la cola
					output = tsFile + "_part_" + i + ".ts";
					//System.out.println(" PART: " + output);
					// TEST IF VIDEO HAS ALREADY SPLITTED
					saveName = tsName + "_part_" + i + ".ts";
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
						try {
							String AwsSpllitedS3Bucket = "s3-bcra-splitted";
							String head = "https://" + AwsSpllitedS3Bucket + ".s3.us-east-1.amazonaws.com/" + saveName;
							int responseCode = this.httpHead(head);
							// code = 200 exist ; code = 400 doesn't exist
							if (responseCode != 200) {
								File file = new File("/tmp/" + saveName);
								upload = tm.upload(AwsSpllitedS3Bucket, saveName, file);
								upload.waitForCompletion();
								log.info(" FILE: " + saveName + " has been uploaded");
								System.out.println("AWS  FILE: " + saveName + "has been uploaded ");
							}

						} catch (Exception e) {
							log.error(" AWS ERROR : " + e.getMessage());
						}
					}

					// 6.2 - Create task in RabbitMQ (1 per each profile)
					for (String profiles : qJobsFromParameters) {

						String responseQueue = tsName + "_profile_" + profiles;

						// String[] filters =
						// msgRearmed.getEncodingProfiles().split(Pattern.quote("_"));
						EncodingStructure profileCoding = this.obtainDetailsProfile(profiles);

						Message msg = new Message();
						//msg.setInsertedJobID();
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
						System.out.println(" JOB: " + responseQueue + "_"+i+" has created");
					}

					this.mdbc.st.close();
					this.mdbc.conn.close();
				}
			} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}


	}
	/*
	private String createAndSaveJob(String cloudstorage, Path path, String fileName) {
		String response = "";
		try {
			String tsName 	= fileName.split(Pattern.quote("."))[0];
			tsName          +=  ".ts";

			String tsFile   = "/tmp/"+tsName;
			String convert  = "/tmp/ffmpeg -y -i "+path.toString()+" -acodec copy -vcodec copy "+tsFile;
			this.processBuilder(convert);
			Files.delete(path);

			if (cloudstorage.startsWith("azu")){
				// AZ
				try{
					this.blobAccount="testdpkub";
					String keyName = "key1";
					String keyStr = "o3czD5k/59lCXMUYa28H3H6SrnhXNLMSXV8y3ZPD1Aei9aZJ/Kp1E1oTsVdQ+d6WwTV7ogzKInvyC8gTxzignA==";
					String endpoint = "https://"+this.blobAccount+".blob.core.windows.net";
					this.azureUploadBlob = "s3-bcra-in";
					String sasToken = this.GetFileSAS();
					System.out.println ("SAS TOKEN" + sasToken);

					String head = endpoint+"/"+this.azureUploadBlob+"/"+tsName;
					int responseCode = this.httpHead(head);
					// code = 200 exist ; code = 400 doesn't exist
					if (responseCode!=200){
						String curl = "curl -X PUT --data-binary @"+tsFile+" -H \"x-ms-date: $(date -u)\" -H \"x-ms-blob-type: BlockBlob\" \"https://"+this.blobAccount+".blob.core.windows.net/"+this.azureUploadBlob+"/"+tsName+sasToken+"\"";
						/*this.azureBlob = new BlobContainerClientBuilder()
								.endpoint(endpoint)
								.sasToken(sasToken)
								.containerName(this.azureUploadBlob)
								.buildClient();
						BlobClient blobClient = this.azureBlob.getBlobClient(tsName);
						blobClient.uploadFromFile(tsFile);*/
/*
						this.processBuilder(curl);
						System.err.println("Azure Blob Storage has been filled" );
					}


				}catch (Exception e){
					System.err.println(" ERROR: "+e.getMessage());
				}

			}else{
				// AWS //if (!this.s3.doesObjectExist(this.s3InPath, fileName)){
				try{
					this.s3 = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
					this.s3UploadPath = "s3-bcra-in";

					this.s3.putObject(this.s3UploadPath, tsName, new File(tsFile));
					System.err.println("AWS S3 Bucket has been filled" );
				}catch (AmazonServiceException e) {

					System.err.println(" AWS ERROR: "+e.getErrorMessage());
				}

			}

			//}
			//String params="hd-720-480";
			//String user = "yo";


			String[] qJobsFromParameters = null;
			try {
				qJobsFromParameters = params.split("-");
			} catch (Exception e) {
				qJobsFromParameters[0] = params;
			}


			for (String profile : qJobsFromParameters) {
				long initTime = System.currentTimeMillis();
				String sql = "insert into job (user,path,params,assignedQueue, state,totalparts,partscompleted,initTime,storageprovider) values ('"
						+ user + "','" + tsName + "','" + profile + "','','new','0','0'," + initTime + ",'" + cloudstorage + "');";
				System.err.println("SQL: " + sql);
				this.mdbc.createConnection();


				PreparedStatement pstmt = this.mdbc.conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
				pstmt.executeUpdate();
				ResultSet keys = pstmt.getGeneratedKeys();
				keys.next();
				String lastId = String.valueOf(keys.getInt(1));

				Job job = new Job(Integer.parseInt(lastId), user, tsName, profile, "new", 0, 0, cloudstorage);

				this.jsonUt.setObject(job);
				String jobString = this.jsonUt.toJson();
				// Generate Job in Queue System
				this.enterChannel.basicPublish("", this.enterQueue, MessageProperties.PERSISTENT_TEXT_PLAIN,
						jobString.getBytes());

				System.out.println(" JOB: " + lastId + " has been saved in enterQueue");
				response += lastId + "-";
				this.mdbc.st.close();
				this.mdbc.conn.close();
			}



		} catch (Exception e) {
			log.info(e.getMessage());
			response = " HOLY ERROR ";
		}
		return response;
	}

	*/

	@RequestMapping(value = "/getFile", method = RequestMethod.GET)
	public StreamingResponseBody getFile(@RequestParam("path") String path, HttpServletResponse response)
			throws InterruptedException {
		// System.err.println("WORKER "+name+" IS GETTING JOB");

		byte[] responseByte = null;
		final int BUFFER_SIZE = 1024 * 1024;
		response.setContentType("application/octet-stream");
		InputStream inputStream;
		try {
			inputStream = new FileInputStream(new File(path));
			return outputStream -> {
				int nRead;
				byte[] data = new byte[1024];
				while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
					// System.out.println("Writing some bytes..");
					outputStream.write(data, 0, nRead);
				}
			};
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			return null;
		}

	}

	@RequestMapping(value = "/existsFile", method = RequestMethod.HEAD)
	public ResponseEntity<String> existsFile(@RequestParam("name") String name) throws InterruptedException {
		String path = "/tmp/video/splittedVideo/" + name;
		File file = new File(path);
		boolean exists = file.exists();

		if (exists) {
			return new ResponseEntity<String>("Exists", HttpStatus.OK);
		} else {
			return new ResponseEntity<String>("Doesn't Exist", HttpStatus.BAD_REQUEST);
		}

	}

	@RequestMapping(value = "/saveFile", method = RequestMethod.POST)
	public ResponseEntity<String> saveFile(@RequestBody byte[] file, @RequestParam("name") String name) {
		String basePath = "/tmp/video/splittedVideo/" + name;
		log.info("PATH : " + basePath);
		Path path = Paths.get(basePath);

		try {
			Files.write(path, file);
			return new ResponseEntity<String>(" OK ", HttpStatus.OK);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			log.info(" EXPLOTE POR EL SAVE");
			return new ResponseEntity<String>(" NO OK ", HttpStatus.BAD_REQUEST);
		}

	}

	@RequestMapping(value = "/downloadJob", method = RequestMethod.GET)
	public ResponseEntity<String> downloadJob(@RequestParam("id") String id) throws InterruptedException {

		return new ResponseEntity<String>(" OK ", HttpStatus.OK);
	}

	@RequestMapping(value = "/uploadFinishedJob", method = RequestMethod.GET)
	public ResponseEntity<String> uload(@RequestParam("id") String idForAck,
										@RequestParam("service") String service, @RequestParam("job") String job, @RequestParam("part") String part,
										@RequestParam("workerName") String workerName,
										@RequestParam("workerArchitecture") String workerArchitecture, @RequestParam("initTime") String initTime,
										@RequestParam("endTime") String endTime, @RequestParam("executionTime") String executionTime) {


		String query = "insert into jobTracker (service, job, part, workerName, workerArchitecture, initTime, endTime, executionTime) values ('" + service + "', '" + job + "', '" + part + "', '" + workerName + "','" + workerArchitecture
				+ "'," + initTime + "," + endTime + "," + executionTime + ")";
		try {
			this.mdbc.createConnection();

			System.out.println("SQL INSERT "+query);
			this.mdbc.doInsertOperation(query);


			query = "update job set partscompleted=partscompleted+1 where assignedQueue='" + job + "' and state='new';";
			System.out.println("SQL UPDATE"+query);
			this.mdbc.st.executeQuery(query);
			this.mdbc.st.close();
			this.mdbc.conn.close();
			return new ResponseEntity<String>(" OK READY ", HttpStatus.OK);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			return new ResponseEntity<String>(" FAILED READY ", HttpStatus.BAD_REQUEST);
		}


	}




	public synchronized String getJob() {
		String response = "";
		GetResponse data = null;
		byte[] responseByte = null;
		try {
			synchronized (this.enterChannel) {
				if (this.enterChannel.queueDeclarePassive(this.jobsQueue).getMessageCount() > 0) {
					// read data without ACK
					data = this.enterChannel.basicGet(this.jobsQueue, false);

					// obtain ID (header), and timestamp
					long idForAck = data.getEnvelope().getDeliveryTag();
					long timestamp = (new Timestamp(System.currentTimeMillis())).getTime();

					// put the ACK in a shared array (controlling )
					/*
					 * synchronized (this.AckService) { this.AckService.put(idForAck, timestamp); }
					 */
					// sqlid int not null, idForAck int not null, timestamp int not null
					String sql = "insert into ackList (idForAck,timestamp) values (" + idForAck + "," + timestamp+ ");";
					ResultSet rs = this.mdbc.st.executeQuery(sql);
					// obtain the body
					responseByte = data.getBody();
					response = new String(responseByte, "UTF-8");
					// arreglar este parche horrible
					response = response.substring(0, (response.length() - 1));
					String end = ",\"idForAck\":\"" + String.valueOf(idForAck) + "\"}";
					response += end;

					// Thread.sleep(100000000);

				} else {
					response = "NO DATA INFO";
				}
			}

		} catch (IOException | SQLException e) {
			// TODO Auto-generated catch block

		}

		return response;
	}

	public String runBash(String path, String command) {
		String totalLines = "";
		try {
			//log.info("BASH RUNNER STARTED");
			String[] cmdArray = { "/bin/bash", "-c", command };
			System.err.println(command);
			//log.info(" BASH RUNNER IS GOING TO RUN: ", cmdArray.toString());
			Process runner = Runtime.getRuntime().exec(cmdArray);
			BufferedReader bf = new BufferedReader(new InputStreamReader(runner.getInputStream()));

			String line;

			while (((line = bf.readLine()) != null)) {
				System.err.println(("LINE: " + line));
				totalLines+= line;
			}


		} catch (IOException e) {
			// TODO Auto-generated catch block

		}
		return totalLines;
	}

	public void createDatabase (String host){
		/*String sql = "mysql -h "+host+" -u david -pdavid distributedProcessing \n";
		sql+="CREATE TABLE `jobTracker` (`id` int(4) NOT NULL, `service` varchar(20) NOT NULL, `job` varchar(40) NOT NULL,	`workerName` varchar(30) NOT NULL,`workerArchitecture` varchar(10) NOT NULL, `initTime` long NOT NULL, `endTime` long NOT NULL,`executionTime` long NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 \";\" \n";
		sql+="ALTER TABLE `jobTracker` ADD PRIMARY KEY (`id`) \";\" ";
		*/

	}

	private void readFromContext(HashMap<String, ArrayList<String>> filterParameters2) {
		// TODO Auto-generated method stub
		String par = "4k|high|4096x2160|libx264|15600|5.1|60|slow|6|3|2|ac3|512|48000|6";
		par+="//2K|high|2560x1440|libx264|7800|5.1|48|slower|6|3|2|ac3|512|48000|6";
		par+="//hd|high|1920x1080|libx264|3900|4.1|30|slow|6|3|2|ac3|320|48000|6";
		par+="//720|main|1280x720|libx264|2000|4.1|25|medium|3|3|1|aac|320|44100|2";
		par+="//480|main|852x480|libx264|900|3.1|25|fast|3|3|1|aac|256|44100|2";
		par+="//360|baseline|640x360|libx264|700|3.0|24|faster|0|0|0|aac|128|44100|2";
		par+="//240|baseline|424x240|libx264|500|3.0|24|ultrafast|0|0|0|aac|128|44100|2";

		String[] eachParam = par.split(Pattern.quote("//"));
		// this is grouped per line
		for (String string : eachParam) {
			String[] eachLineParts = string.split(Pattern.quote("|"));
			// 1st, header, 2nd parameters
			ArrayList<String> values = new ArrayList<String>();
			for (int i =1; i<eachLineParts.length; i++) {
				values.add(eachLineParts[i]);
			}
			filterParameters.put(eachLineParts[0], values);
		}




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

	private EncodingStructure obtainDetailsProfile (String params){

		return this.paramsList.get(params);
	}

	private void loadParameters () {
		EncodingStructure es = new EncodingStructure("2k", "2560x1440", "libx264", "7800", "high", "5.1", "48", "ultrafast", "ac3", "512", "48000", "6");
		//EncodingStructure es = (EncodingStructure) this.jsonUtility.fromJson(parameters);
		this.paramsList.put("2k", es);

		es = new EncodingStructure("4k","4096x2160","libx264", "15600", "high", "5.1", "60", "ultrafast", "ac3", "512", "48000", "6");
		this.paramsList.put("4k", es);

		es =new EncodingStructure ("hd", "1920x1080","libx264", "3900", "high", "4.1", "30","ultrafast", "ac3", "320", "48000", "6");
		this.paramsList.put("hd", es);
		es = new EncodingStructure("720","1280x720","libx264","2000", "main", "4.1", "25", "ultrafast", "aac", "192", "44100", "2");
		this.paramsList.put("720", es);

		es = new EncodingStructure ("480","852x480","libx264","900","main","3.1","25","ultrafast","aac","128","44100","2");
		this.paramsList.put("480", es);

		es = new EncodingStructure("360","640x360","libx264","700","baseline","3.0","24","ultrafast","aac","128","44100","2");
		this.paramsList.put("360", es);

		es = new EncodingStructure ("240","424x240","libx264","500","baseline","3.0","24","ultrafast","aac","128","44100","2");
		this.paramsList.put("240", es);

	}

}

