package BoostrapRat;

public class ClientMessage {
	
	String name;
	String service;
	byte[] data;
	String encodingProfiles;
	
	public ClientMessage () {
	
	}
	
	public ClientMessage(String name, String service, byte[] data, String encodingProfiles) {
		super();
		
		this.name = name;
		this.service = service;
		this.data = data;
		this.encodingProfiles = encodingProfiles;
		
	}
	

	
	public String getEncodingProfiles() {
		return encodingProfiles;
	}

	public void setEncodingProfiles(String encodingProfiles) {
		this.encodingProfiles = encodingProfiles;
	}
	
	public String getService() {
		return service;
	}
	public void setService(String service) {
		this.service = service;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public byte[] getData() {
		return data;
	}
	public void setData(byte[] data) {
		this.data = data;
	}
	


}
