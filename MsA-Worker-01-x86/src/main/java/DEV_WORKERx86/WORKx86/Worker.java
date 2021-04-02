package DEV_WORKERx86.WORKx86;

import com.rabbitmq.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.sql.Timestamp;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;


public class Worker {
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
	String context;
	MariaDBConnection mdbc;
	private static final Logger log = LoggerFactory.getLogger(Worker.class);


		public Worker () {

			try {
				// content = new String(Files.readAllBytes(file.toPath()));
				this.context = "localkub";
				System.out.println(context);
				String urlGit = "https://raw.githubusercontent.com/dpetrocelli/LinuxRepo/master/configFile";
				this.downloadConfigurationFile(urlGit, context);
				this.toolsConnection();
				this.activateWorker();
			} catch (Exception e) {

				System.out.println(" ERROR ");
			}
		}
			
		private void activateWorker() {
			
			String workerName;
			try {
				InetAddress addr = InetAddress.getLocalHost();
				workerName = addr.getHostName();
			}catch (Exception e) {
				workerName = "noName";
			}
			System.out.println("WORKER NAME: "+workerName);

			int threadId = (int) Thread.currentThread().getId();
			int i = 0;
			boolean isEven;

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
									//System.out.println(" [STEP 1] -  Job Obtained "+msgRearmed.getName());

									// - CREATE OBJECT depending on the task received.
									String service = msgRearmed.getService();

									if (service.equals("videoCompression")) {
										try {
											FFMpegClass ffmpegClass = new FFMpegClass (msgRearmed, jsonUt, "home", deliveryTag, ipStringServer, null, initTime, port, mdbc, enterChannel, deliveryTag, context);
										} catch (InterruptedException e) {
											e.printStackTrace();
										}
									}


								}


							});

			}catch (Exception e) {
				System.err.println(" Error with RabbitMQ Consume Looper ");
			}
		}
	public static void main(String[] args) throws IOException {
		Worker wk = new Worker();
	}

	private void downloadConfigurationFile(String url, String mode) {

				// [STEP 1] - Obtain Job
				try {
					URL obj = new URL(url);
					HttpURLConnection con;
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
									this.context = inputLine.split(":")[1];
								} else {
									this.context = inputLine.split(":")[2];
								}

								//this.mariadbHost = "af0c97f9cb85140dd8ca759fd1b3ca5b-1051178316.us-east-1.elb.amazonaws.com";
							}
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

		String dbname = "distributedProcessing";
		String url = "jdbc:mariadb://" + mariadbHost + "/" + dbname;
		String username = "david";
		String password = "david";

		this.mdbc = new MariaDBConnection(mariadbHost, dbname, username, url, password);
		//this.mdbc.createConnection();

		System.out.println(" MARIADB CONNECTED");

		// DEFINE BASICS FOR RABBITMQ SERVICE

		String usernameRabbit = "admin";
		String passwordRabbit = "admin";
		this.factory = new ConnectionFactory();
		this.factory.setHost(rabbitHost);

		this.factory.setUsername(usernameRabbit);
		this.factory.setPassword(passwordRabbit);
		System.out.println(" RABBIT CONNECTED");
		boolean durable = true;
		try {

			this.enterConnection = this.factory.newConnection();
			this.enterChannel = this.enterConnection.createChannel();
			this.enterChannel.queueDeclare(this.enterQueue, durable, false, false, null);
			this.enterChannel.queueDeclare(this.jobsQueue, durable, false, false, null);
			this.enterChannel.basicQos(1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println(" ERROR: "+e.getMessage());
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			System.out.println(" ERROR: "+e.getMessage());
		}
	}
}
