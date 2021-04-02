import java.sql.*;

public class MariaDBConnection {
	String host;
	String dbname;
	String url;
	String password;
	String username;
	Connection conn;
	Statement st;
	
	public MariaDBConnection(String host, String dbname, String username, String url, String password) {
		this.host = host;
		this.dbname = dbname;
		this.url = url;
		this.username = username;
		this.password = password;
		this.conn = null;
		this.st = null;
	}
	public void createStructure(){

		// validate if the structure is created
		this.createConnection();
		try {
			// TABLA JOBS
			String createJobTable = "create table job (id int not null, `user` varchar(20) NOT NULL, `path` varchar(60) NOT NULL, `params` varchar(100) NOT NULL,`assignedQueue` varchar(100) NOT NULL,`state` varchar(10) NOT NULL,`totalparts` int(5) NOT NULL,`partscompleted` int(5) NOT NULL, `initTime` bigint(20),`endTime` bigint(20),`executionTime` int(11), `storageprovider` varchar(20), `bloburl` varchar(100)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
			this.st.executeQuery(createJobTable);
			createJobTable = "ALTER TABLE `job` ADD PRIMARY KEY (`id`);";
			this.st.executeQuery(createJobTable);
			createJobTable = "ALTER TABLE job CHANGE id id INT(10)AUTO_INCREMENT;";
			this.st.executeQuery(createJobTable);
		} catch (Exception e) {
			//System.err.println(" Estaba la tabla jobs");
		}

		try {
			// TABLA JobsTracker
			String createJobTable = "CREATE TABLE `jobTracker` (id int not null,`service` varchar(20) NOT NULL,`job` varchar(140) NOT NULL,`part` int(5) NOT NULL, `workerName` varchar(60) NOT NULL, `workerArchitecture` varchar(10) NOT NULL,`initTime` bigint(20) NOT NULL,`endTime` bigint(20) NOT NULL,`executionTime` int(11) NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
			this.st.executeQuery(createJobTable);
			createJobTable = "ALTER TABLE `jobTracker` ADD PRIMARY KEY (`id`);";
			this.st.executeQuery(createJobTable);
			createJobTable = "ALTER TABLE jobTracker CHANGE id id INT(10)AUTO_INCREMENT;";
			this.st.executeQuery(createJobTable);

		} catch (Exception e) {
			//System.err.println(" Estaba la tabla jobTracker");
		}

		try {
			// TABLA ACKList
			String createackListTable = "create table ackList (id int not null, idForAck int not null, timestamp bigint(20) not null) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
			this.st.executeQuery(createackListTable);
			createackListTable = "ALTER TABLE `ackList` ADD PRIMARY KEY (`id`);";
			this.st.executeQuery(createackListTable);
			createackListTable = "ALTER TABLE ackList CHANGE id id INT(10)AUTO_INCREMENT;";
			this.st.executeQuery(createackListTable);

		} catch (Exception e) {
			//System.err.println(" Estaba la tabla jobs");
		}
	}
	public void createConnection () {
		try {
			this.conn = DriverManager.getConnection(this.url, this.username, this.password);
			this.st = conn.createStatement();

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void closeConnection(){
		try {
			this.st.close();
			this.conn.close();
		} catch (SQLException throwables) {
			throwables.printStackTrace();
		}

	}
	public void doSelectOperation (String query) {
		// execute the query, and get a java resultset
	    ResultSet rs;
		try {
			rs = this.st.executeQuery(query);
			// iterate through the java resultset
		    while (rs.next())
		    {
		      String engine = rs.getString("job");
		      System.out.println("ENGINE "+engine);
		    }
		    
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
	    
	}
	
	public void doInsertOperation (String query) {
		// execute the query, and get a java resultset
	    ResultSet rs;
		try {
			this.st.executeUpdate(query);
					    
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
	    
	}
	

}
