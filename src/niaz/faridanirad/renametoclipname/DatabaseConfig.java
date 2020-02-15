package niaz.faridanirad.renametoclipname;

public class DatabaseConfig {
	String hostname;
	String port;
	String database;
	String username;
	String password;
	
	public DatabaseConfig(String hostname, String port, String database, String username, String password) {
		super();
		this.hostname = hostname;
		this.port = port;
		this.database = database;
		this.username = username;
		this.password = password;
	}
	
	public String getHostname() {
		return hostname;
	}
	public String getPort() {
		return port;
	}
	public String getDatabase() {
		return database;
	}
	public String getUsername() {
		return username;
	}
	public String getPassword() {
		return password;
	}

	@Override
	public String toString() {
		return "DatabaseConfig [hostname=" + hostname + ", port=" + port + ", database=" + database + ", username="
				+ username + ", password=" + password + "]";
	}
	
	
	
}
