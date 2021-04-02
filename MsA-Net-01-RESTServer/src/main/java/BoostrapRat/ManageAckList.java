package BoostrapRat;

import com.rabbitmq.client.Channel;

import java.sql.ResultSet;
import java.sql.Timestamp;

public class ManageAckList implements Runnable {

	MariaDBConnection_old mdbc;
	int timeCheckInterval;
	int timeoutPackage;
	Channel enterChannel;
	String jobQueue;

	public ManageAckList(Channel enterChannel, MariaDBConnection_old mdbc, int timeCheckInterval, int timeoutPackage) {
		this.mdbc = mdbc;
		this.timeCheckInterval = timeCheckInterval;
		this.timeoutPackage = timeoutPackage;
		this.enterChannel = enterChannel;

	}

	@Override
	public void run() {
		// ONCE ACTIVATED TEST IF Some package is alive after time expected

		long timestamp;
		long currentTimestamp;
		long parameter;
		String sql;
		ResultSet rs;

		while (true) {
			// STEP 1 - Check for each msg;
			synchronized (this.enterChannel) {

				try {
					currentTimestamp = (new Timestamp(System.currentTimeMillis())).getTime();
					parameter = currentTimestamp - this.timeoutPackage;
					sql = "select idForAck FROM ackList WHERE timestamp<= " + parameter + ";";

					rs = this.mdbc.st.executeQuery(sql);
					while (rs.next()) {
						System.out.println(" SQL1 : " + sql);
						this.enterChannel.basicNack(rs.getInt("idForAck"), false, true);
						System.out.println(" RENCOOLO ID: " + rs.getInt("idForAck"));
					}
					sql = "DELETE FROM ackList WHERE timestamp<= " + parameter + ";";
					// System.out.println(" SQL2 : "+sql);
					this.mdbc.st.executeQuery(sql);

				} catch (Exception e) {
					System.err.println(" ERROR " + e.getMessage());
				}
			}
			try {
				Thread.sleep(this.timeCheckInterval);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				System.out.println(" ERROR: "+e.getMessage());
			}
				
			
			
			
			
		}
		
	}

	

	

}
