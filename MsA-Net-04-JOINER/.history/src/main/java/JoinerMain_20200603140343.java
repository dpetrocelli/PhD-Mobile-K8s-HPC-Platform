import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.SharedAccessAccountPolicy;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlob;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.ListBlobItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.ResultSet;
import java.util.Date;
import java.util.Objects;

/**
 * Hello world!
 *
 */
public class JoinerMain
{
	String rabbitHost;
	String mariadbHost;
	MariaDBConnection mdbc;
	String jobsQueue = "jobsQueue";
	String pendantQueue = "pendantQueue";
	String enterQueue = "enterQueue";

	String username;
	String password;
	private static final Logger log = LoggerFactory.getLogger(JoinerMain.class);
	String content;
	public JoinerMain(){
		System.out.println( "Joiner starter!" );

		try {
			// content = new String(Files.readAllBytes(file.toPath()));
			String context = "kubernetes";
			System.out.println(context);
			String urlGit = "https://raw.githubusercontent.com/dpetrocelli/LinuxRepo/master/configFile";
			this.downloadConfigurationFile(urlGit, context);


			this.toolsConnection();

			String sql = "select * from job where state='new'";
			// [STEP 1 ] - Loop looking for finished jobs
			int totalparts;
			int partscompleted;
			String accountName = "testdpkub";
			String key = "o3czD5k/59lCXMUYa28H3H6SrnhXNLMSXV8y3ZPD1Aei9aZJ/Kp1E1oTsVdQ+d6WwTV7ogzKInvyC8gTxzignA==";
			String storageConnectionString = "DefaultEndpointsProtocol=https;AccountName="+accountName+";AccountKey="+key+";EndpointSuffix=core.windows.net";
			String name = "";
			CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
				// Create the blob client.
			CloudBlobClient blobClient = storageAccount.createCloudBlobClient();

			while (true){
				try{
					//System.out.println(" LOOPING");
					this.mdbc.createConnection();
					ResultSet rs = this.mdbc.st.executeQuery(sql);
					this.mdbc.st.close();
					this.mdbc.conn.close();
					String removeCompressed;
					String removeInAndSplitted;
					if (!Objects.isNull(rs)){
						while (rs.next()) {

							// [STEP 2] - Once opened open Thread to join parts and finish the task
							totalparts =rs.getInt("totalparts");
							partscompleted = rs.getInt("partscompleted");
							if (totalparts>0){
								if (totalparts == partscompleted){

									//this.joiner(rs, mdbc);

									JoinerThread jt = new JoinerThread(rs, mdbc, context);
									Thread jtThread = new Thread (jt);
									jtThread.start();
									jtThread.join();
									removeCompressed = rs.getString("assignedQueue");
									removeInAndSplitted = rs.getString("path");

									String compressedBucket = "s3-bcra-compressed";
									this.remover(blobClient, removeCompressed, compressedBucket);

									String splittedBucket = "s3-bcra-splitted";
									this.remover(blobClient, removeInAndSplitted, splittedBucket);

								}

							}

						}
					}
					Thread.sleep(5000);
				}catch (Exception e){
					System.out.println(" NO FINISHED TASKS");
					Thread.sleep(5000);
				}


			}


		} catch (Exception e) {
			System.err.println(" NOt CONNECTED ");
		}
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
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
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
					}/*else{
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


	}
    public void joiner (ResultSet rs, MariaDBConnection_old mdbc){
		try {
			String name = rs.getString("assignedQueue");
			int parts = rs.getInt("totalparts");
			name = name.substring(5, name.length());
			String baseName = name;
			name = "/tmp/video/resultJoined/"+name+"_";
			String endPath = "/tmp/video/finishedVideos/";
			String concatenate = "/tmp/ffmpeg";
			for (int k=0; k<parts; k++) {
				concatenate+=" -i "+name+k+".mp4";
			}
			concatenate+=" -filter_complex concat=n="+parts+":v=1:a=1 -y "+endPath+"compressed-"+baseName+".mp4";
			System.out.println(" TERRIBE FFMPEG: "+concatenate);

			this.runBash(concatenate);
			// [STEP 2] - AFTER I Have FINISHED joiner task UPDATE db
			long endTime = System.currentTimeMillis();
			long executionTime =  (endTime-rs.getInt("initTime"));
			String query = "update job set state='finished', endTime="+endTime+" , executionTime="+executionTime+" where id="+rs.getInt("id")+";";
			System.out.println(" SQL: "+query);
			mdbc.st.executeQuery(query);

		}catch (Exception e){

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
			//log.info("BASH RUNNER STARTED");
			String[] cmdArray = { "/bin/bash", "-c", command };
			System.err.println(command);
			//log.info(" BASH RUNNER IS GOING TO RUN: ", cmdArray.toString());
			Process runner = Runtime.getRuntime().exec(cmdArray);
           /* BufferedReader bf = new BufferedReader(new InputStreamReader(runner.getInputStream()));

            String line;

            while (((line = bf.readLine()) != null)) {
                System.err.println(("LINE: " + line));
                totalLines+= line;
            }*/
			runner.waitFor();



		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}
}
