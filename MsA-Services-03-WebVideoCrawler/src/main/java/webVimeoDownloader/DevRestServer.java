package webVimeoDownloader;


import com.clickntap.vimeo.VimeoResponse;
import com.google.gson.Gson;
import org.json.JSONArray;
import org.json.JSONObject;
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
/*
		HOW TO BE DEPLOYED
// https://medium.com/@urbanswati/deploying-spring-boot-restapi-using-docker-maven-heroku-and-accessing-it-using-your-custom-aa04798c0112
# YOU MUST STAND IN DOCKERFILE PATH
/snap/bin/heroku container:rm web --app dp-vimeo-dwl ; bash deploy-automation.sh ; /snap/bin/heroku container:push web --app dp-vimeo-dwl ; /snap/bin/heroku container:release web --app dp-vimeo-dwl

 */



@RestController
@CrossOrigin(origins = "*", methods= {RequestMethod.GET, RequestMethod.POST})
public class DevRestServer {

	String token;
	Gson gson;
	private static final Logger log = LoggerFactory.getLogger(DevRestServer.class);

	public DevRestServer() {

		this.gson = new Gson();
		this.token = "";



	}



	@RequestMapping(value = "/setToken", method = RequestMethod.GET)
	public ResponseEntity<String> setToken(@RequestParam("token") String token) throws InterruptedException {
		this.token = token;

		return new ResponseEntity<String>("OK", HttpStatus.OK);
	}

	@RequestMapping(value = "/getSourceFiles", method = RequestMethod.GET)
	public ResponseEntity<String> getSourceFiles(@RequestParam("query") String query, @RequestParam("itemsPerPage") String itemsPerPage) throws InterruptedException {
		Vimeo vimeo = new Vimeo(this.token);
		System.out.println ( "STEP 1 - VIMEO OBJECT HAS BEEN CREATED ");
		VimeoResponse content = null;
		ArrayList<Video> videosLoaded = new ArrayList<Video>();

		try {
			// first obtain total videos

			content = vimeo.searchVideos2("1","1", query);
			JSONObject total = content.getJson();
			int totalObjects = Integer.valueOf(total.get("total").toString());
			System.out.println ( "STEP 2 - Basic structure obtained ");
			int loop = 0;



			while (videosLoaded.size()==0 && (loop<31)){
				int randomNum = 1 + (int) (Math.random() * totalObjects);
				System.out.println("STEP 3 - Index obtained: "+randomNum);
				try{
					content = vimeo.searchVideos2(String.valueOf(randomNum/Integer.valueOf(itemsPerPage)),String.valueOf(itemsPerPage), query);
					JSONObject jsonObject = content.getJson();
					JSONArray array = jsonObject.getJSONArray("data");
					String link;
					int duration;
					int width;
					int height;
					String name;
					for (int i = 0; i < array.length (); ++i) {
						JSONObject object = array.getJSONObject(i);
						link = object.get("link").toString();
						width = Integer.valueOf(object.get("width").toString());
						height = Integer.valueOf(object.get("height").toString());
						duration = Integer.valueOf(object.get("duration").toString());
						name =  object.get("name").toString();
						if ((width>=1000) && (height>=2160) && (duration>=30 && duration<=100)){
							videosLoaded.add(new Video(link,duration,width,height,name));
							System.out.println(link+ "STEP 4.x - Video has been added - "+name);

						}
					}

					//counter++;

				}catch (Exception e){
					System.out.println("Vimeo ERROR: "+e.getMessage());
				}
				loop++;
			}


			System.out.println ("Will be return: "+videosLoaded.toString());
			return new ResponseEntity<String>(this.gson.toJson(videosLoaded), HttpStatus.OK);
		} catch (Exception e) {
			System.out.println("Error: "+e.getMessage());
			return new ResponseEntity<String>("NOOK", HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	// --------- RELATED TO UPLOAD JOBS --------- //



	@PostMapping("/upload")
	//public ResponseEntity<String> updSystem(@RequestParam("cloudstorage") String cloudstorage, @RequestParam("file") MultipartFile file) throws InterruptedException {
	public ResponseEntity<String> updSystem(@RequestParam("file") MultipartFile file, @RequestParam("user") String user, @RequestParam("encodingParams") String encodingParams) throws InterruptedException {
		try {


			return new ResponseEntity<String>("  FILE:  has been published" , HttpStatus.OK);
		} catch (Exception e) {
			//throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
			return new ResponseEntity<String>( "COULDN't store file " , HttpStatus.BAD_REQUEST);
		}

	}

}

