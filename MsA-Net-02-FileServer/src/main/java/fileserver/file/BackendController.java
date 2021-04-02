package fileserver.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

@RestController
@CrossOrigin(origins = "*")
public class BackendController {
    private static final Logger logger = LoggerFactory.getLogger(BackendController.class);
    private static String BASE_PATH = "home/storage/";

    @PutMapping("/uploadFile")
    public ResponseEntity<String> uploadFile(@RequestParam("folder") String folder, @RequestParam(value = "fileName", required = false) String fileName, @RequestParam("file") MultipartFile file) {

        try{

            File storage = new File(BASE_PATH+"/"+folder);
                /*
                    si es la primera vez que levanta el sistema
                    no tengo la imagen por defecto.  Por eso la descargo de internet
                     y creo el directorio
                 */
            if (!storage.exists()) {
                if (storage.mkdirs()) {
                    System.out.println("Directorio creado"+storage.getAbsolutePath());

                } else {
                    System.out.println("Error al crear directorio");
                }
            }
            String name;
            if (!Objects.isNull(fileName)){
                name = fileName;
            }else{
                name = file.getOriginalFilename();
            }

            Path path = Paths.get(BASE_PATH+"/"+folder +"/"+ name);
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            System.out.println(" [STEP 0] - FILE: "+name+ " has been stored");
            return new ResponseEntity<String>("  FILE: "+name+ " has been published" , HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity<String>( " File hasn't been published" , HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/removeAllFiles")
    public ResponseEntity<String> removeAllFiles(@RequestParam("folder") String folder, @RequestParam("pattern") String pattern) {
        System.out.println(" [STEP 0] - DELETE REQUEST HAS ARRIVED : "+pattern);
        try{


            File directory = new File(BASE_PATH+"/"+folder);
            File[] listOfFiles = directory.listFiles();

            for (File file : listOfFiles) {
                if (file.isFile()) {

                    if (file.getName().startsWith(pattern)) {

                        if(file.delete())
                        {
                            System.out.println("File "+file.getName()+" deleted successfully");

                        }
                        else
                        {
                            //System.out.println("Failed to delete the file "+file.getName());
                            //return new ResponseEntity<String>( "Failed to delete the file" , HttpStatus.BAD_REQUEST);

                        }
                    }

                }
            }
            return new ResponseEntity<String>( " Files deleted successfully" , HttpStatus.OK);








        }catch (Exception e){
            System.out.println (" SE FUE TODO A LA MIERDA " + e.getMessage());
            return new ResponseEntity<String>( " Files hasn't been deleted for some reason" , HttpStatus.BAD_REQUEST);
        }

    }

    @DeleteMapping("/deleteFile")
    public ResponseEntity<String> deleteFile(@RequestParam("folder") String folder, @RequestParam("fileName") String fileName) {

        try{

            File file = new File(BASE_PATH+"/"+folder+"/"+fileName);


            if (!file.exists()) {
                System.out.println(" File doesn't exist "+fileName);
                return new ResponseEntity<String>( " File doesn't exist" , HttpStatus.NOT_FOUND);
            } else {
                if(file.delete())
                {
                    System.out.println("File deleted successfully");
                    return new ResponseEntity<String>( " File deleted successfully" , HttpStatus.OK);
                }
                else
                {
                    System.out.println("Failed to delete the file");
                    return new ResponseEntity<String>( "Failed to delete the file" , HttpStatus.BAD_REQUEST);
                }

            }
        }catch (Exception e){
            System.out.println (" SE FUE TODO A LA MIERDA " + e.getMessage());
            return new ResponseEntity<String>( " File hasn't been published" , HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/downloadFile")
    public ResponseEntity<Resource> downloadFile(@RequestParam("folder") String folder, @RequestParam("fileName") String fileName) {
        // Load file as Resource
        try {
            //String current = new java.io.File( "." ).getCanonicalPath();
            String current=BASE_PATH;
            current+="/"+folder;
            current+="/"+fileName;

            Resource resource;

            Path filePath = Paths.get(current);
            resource = new UrlResource(filePath.toUri());


            String contentType= Files.probeContentType(filePath);



            // Fallback to the default content type if type could not be determined
            if(contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
             } catch (MalformedURLException e) {
                  System.out.println (" SE FUE TODO A LA MIERDA " + e.getMessage());
                return null;
            } catch (IOException e) {
                System.out.println (" SE FUE TODO A LA MIERDA " + e.getMessage());
                return null;
            }


    }
}
