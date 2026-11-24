package com.example.carmanage_service.service;
import com.example.carmanage_service.model.CarPhoto;
import com.example.carmanage_service.repository.CarPhotoRepository;
import java.nio.file.*;
import java.util.*;
import java.io.*;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@Service
public class PhotoService {
    private final CarPhotoRepository photos;private final Path directory;
    public PhotoService(CarPhotoRepository photos,@Value("${app.media.directory:./media}") String directory)throws IOException{this.photos=photos;this.directory=Path.of(directory).toAbsolutePath().normalize();Files.createDirectories(this.directory);}
    public CarPhoto save(MultipartFile file,String subject)throws IOException{
        if(file.isEmpty()||file.getSize()>8*1024*1024)throw new IllegalArgumentException("Ảnh cần nhỏ hơn 8 MB");
        if(photos.countByOwnerId(subject)>=500)throw new IllegalArgumentException("Tài khoản đã đạt giới hạn ảnh");
        String format;
        try(var input=ImageIO.createImageInputStream(file.getInputStream())){
            var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw new IllegalArgumentException("Chỉ chấp nhận ảnh JPEG hoặc PNG");
            var reader=readers.next();try{reader.setInput(input);format=reader.getFormatName().toLowerCase(Locale.ROOT);int w=reader.getWidth(0),h=reader.getHeight(0);if(w<200||h<150||(long)w*h>40000000)throw new IllegalArgumentException("Kích thước ảnh không phù hợp");}finally{reader.dispose();}
        }
        if(!Set.of("jpeg","jpg","png").contains(format))throw new IllegalArgumentException("Chỉ chấp nhận ảnh JPEG hoặc PNG");
        String id=UUID.randomUUID().toString().replace("-",""),ext=format.equals("png")?"png":"jpg",filename=id+"."+ext;
        Path target=directory.resolve(filename);
        // Decode and re-encode: reject damaged images and remove EXIF/GPS metadata.
        try(var input=file.getInputStream()){
            var decoded=ImageIO.read(input);if(decoded==null)throw new IllegalArgumentException("Ảnh không đọc được");
            try(var output=Files.newOutputStream(target,StandardOpenOption.CREATE_NEW)){if(!ImageIO.write(decoded,ext,output))throw new IllegalArgumentException("Định dạng ảnh không hỗ trợ");}
        }catch(IOException|RuntimeException ex){Files.deleteIfExists(target);throw new IllegalArgumentException("Ảnh bị hỏng hoặc không lưu được");}
        try{return photos.saveAndFlush(new CarPhoto(id,subject,filename,ext.equals("png")?"image/png":"image/jpeg"));}catch(RuntimeException e){Files.deleteIfExists(target);throw e;}
    }
    public CarPhoto find(String id){if(id==null||!id.matches("[a-f0-9]{32}"))throw new ResponseStatusException(HttpStatus.NOT_FOUND);return photos.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));}
    public Path path(CarPhoto photo){return directory.resolve(photo.filename);}
    public void requireOwned(List<String> ids,String subject,boolean admin){
        if(ids.size()>6||new HashSet<>(ids).size()!=ids.size())throw new IllegalArgumentException("Mỗi xe có tối đa 6 ảnh khác nhau");
        for(String id:ids){CarPhoto photo=find(id);if(!admin&&!photo.ownerId.equals(subject))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Ảnh không thuộc tài khoản của bạn");}
    }
}
