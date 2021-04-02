package BoostrapRat;

import com.rabbitmq.client.Channel;

import java.sql.ResultSet;

public class AckDatabaseOperations {

	MariaDBConnection_old mdbc;
	int timeCheckInterval; 
	int timeoutPackage;
	Channel enterChannel;
	String jobQueue; 

	
	public AckDatabaseOperations(Channel enterChannel, MariaDBConnection_old mdbc, int timeCheckInterval, int timeoutPackage) {
		this.mdbc = mdbc;
		this.timeCheckInterval = timeCheckInterval;
		this.timeoutPackage = timeoutPackage;
		this.enterChannel = enterChannel;
		
	}

	public boolean verifyIdAck (long idForAck){
		boolean result = false;
		try {
			String sql = "select * FROM ackList WHERE idForAck = "+idForAck+";";
			ResultSet rs = this.mdbc.st.executeQuery(sql);
			if (rs.next()){
				result = true;
			}
		} catch (Exception e) {
			//TODO: handle exception
		}
		
		return result;
	}
	public void removeId(long idForAck) {
		String sql = "delete from ackList WHERE idForAck = "+idForAck+";";
		try {
			this.mdbc.st.executeQuery(sql);	
		} catch (Exception e) {
			//TODO: handle exception
		}
		
	}
	public void listTable() {
		try {
			String sql = "select * FROM ackList;";
			ResultSet rs = this.mdbc.st.executeQuery(sql);
			while (rs.next()){
				System.err.println("ACKLIST: "+rs.getInt("idForAck")+"-"+rs.getInt("timestamp"));			}	
		} catch (Exception e) {
			//TODO: handle exception
		}
	}

	
			
		
		
	

	

	

}
