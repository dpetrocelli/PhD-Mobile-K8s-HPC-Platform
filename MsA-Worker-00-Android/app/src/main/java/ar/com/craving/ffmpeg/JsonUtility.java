package ar.com.craving.ffmpeg;


import com.google.gson.Gson;



public class JsonUtility {
	
	public Object object;
	String type;
	
	public JsonUtility () {
		
	}
	public Object getObject() {
		return object;
	}
	public void setObject(Object object) {
		this.object = object;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public JsonUtility (Object conversion, String type) {
		this.object = conversion;
		this.type = type;
	}
	
	public String toJson () {
		Gson gson = new Gson();
		return gson.toJson(this.object);
	}
	
	public Object fromJson (String object) {
		Object obj = null;
		if (type.equals("Message")) {
			Gson gson = new Gson();
			obj = gson.fromJson(object, Message.class);
			
		}
		return obj;
	}
	
	
}
