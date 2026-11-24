import java.nio.file.*;
import java.io.IOException;
import java.nio.file.attribute.PosixFilePermission;
import java.security.*;
import java.util.*;
/** Creates missing private configuration. Existing keys and database secrets are preserved. */
public class GenerateLocalSetup {
 public static void main(String[] args) throws Exception {
  Path root=Path.of(args.length==0?".":args[0]).toAbsolutePath().normalize();
  Path local=root.resolve(".local"),keys=local.resolve("keys");Files.createDirectories(keys);
  // When source is copied over an older release, deleted files may remain.
  // Move only the explicitly retired source files into a private reversible archive.
  for(String retired:List.of(
   "auth-service/src/main/java/com/example/auth_service/account/DemoAccounts.java",
   "carmanage-service/src/main/java/com/example/carmanage_service/catalog/SystemDemoFleet.java",
   "carmanage-service/src/main/java/com/example/carmanage_service/catalog/CatalogService.java",
   "auth-service/src/test/java/com/example/auth_service/AuthLoginTest.java",
   "auth-service/src/test/java/com/example/auth_service/ProfilePrivacyTest.java",
   "carmanage-service/src/test/java/com/example/carmanage_service/SystemDemoFleetTest.java",
   "carmanage-service/src/test/java/com/example/carmanage_service/CarCatalogTest.java",
   "carmanage-service/src/main/resources/static/js/demo-data.js",
   "carmanage-service/src/main/resources/static/js/auth.js",
   "carmanage-service/src/main/resources/static/js/dashboard.js",
   "carmanage-service/src/main/resources/static/js/catalog.js",
   "carmanage-service/src/main/resources/static/js/privacy.js",
   "carmanage-service/src/main/resources/static/js/car-images.js",
   "carmanage-service/src/main/resources/static/css/dashboard.css",
   "tools/GenerateDevSetup.java","scripts/02_use_numeric_demo_passwords.cmd",
   "scripts/use-numeric-demo-passwords.sh","CAP_NHAT_TAI_KHOAN.md",
   "XE_MAU_CO_ANH.md","DANH_MUC_XE.md","BAO_MAT_VA_TICH_HOP.md")){
   Path source=root.resolve(retired);if(Files.isRegularFile(source)){
    Path archived=local.resolve("retired-source").resolve(retired);
    Files.createDirectories(archived.getParent());if(Files.exists(archived))archived=archived.resolveSibling(archived.getFileName()+"."+System.currentTimeMillis());
    Files.move(source,archived);System.out.println("Da luu tru source cu: "+retired);
   }
  }
  Path privateKey=keys.resolve("private-key.pem"),publicKey=keys.resolve("public-key.pem");
  if(Files.exists(privateKey)!=Files.exists(publicKey))throw new IllegalStateException("Can du cap key trong .local/keys. Khong tao de len key cu.");
  if(!Files.exists(privateKey)){
   var gen=KeyPairGenerator.getInstance("RSA");gen.initialize(3072);var pair=gen.generateKeyPair();
   Files.writeString(privateKey,pem("PRIVATE KEY",pair.getPrivate().getEncoded()),StandardOpenOption.CREATE_NEW);
   Files.writeString(publicKey,pem("PUBLIC KEY",pair.getPublic().getEncoded()),StandardOpenOption.CREATE_NEW);protect(privateKey);
  }
  Path env=root.resolve(".env");String content=Files.exists(env)?Files.readString(env):"";
  for(String key:List.of("AUTH_DB_PASSWORD","CAR_DB_PASSWORD","RENTAL_DB_PASSWORD"))
   if(!java.util.regex.Pattern.compile("(?m)^"+key+"=").matcher(content).find())content+=(content.endsWith("\n")||content.isEmpty()?"":"\n")+key+"="+secret()+"\n";
  try{
   if(!java.util.regex.Pattern.compile("(?m)^APP_UID=").matcher(content).find())content+=(content.endsWith("\n")?"":"\n")+"APP_UID="+Files.getAttribute(local,"unix:uid")+"\n";
   if(!java.util.regex.Pattern.compile("(?m)^APP_GID=").matcher(content).find())content+="APP_GID="+Files.getAttribute(local,"unix:gid")+"\n";
  }catch(IOException|UnsupportedOperationException|IllegalArgumentException ignored){}
  if(!Files.exists(env)||!Files.readString(env).equals(content)){Files.writeString(env,content);protect(env);}
  Path bootstrap=local.resolve("bootstrap-admin.properties");
  if(!Files.exists(bootstrap)){
   String password=secret();Files.writeString(bootstrap,"app.bootstrap-admin.username=admin\napp.bootstrap-admin.password="+password+"\n",StandardOpenOption.CREATE_NEW);protect(bootstrap);
   Path info=local.resolve("TAI_KHOAN_QUAN_TRI.txt");
   Files.writeString(info,"QUAN TRI VIEN KHOI TAO\nTai khoan: admin\nMat khau: "+password+"\n\nChi co hieu luc khi CSDL chua co ADMIN. Neu nang cap, mat khau admin cu duoc giu nguyen.\nDang nhap roi doi mat khau trong Tai khoan. Customer va owner tu dang ky tren web.\n");protect(info);
  }
  System.out.println("Da bo sung cau hinh con thieu; giu nguyen key va mat khau CSDL cu.");
  System.out.println("May moi: xem .local/TAI_KHOAN_QUAN_TRI.txt. May nang cap: dung tai khoan cu.");
 }
 static String secret(){byte[] b=new byte[24];new SecureRandom().nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
 static String pem(String t,byte[] b){return "-----BEGIN "+t+"-----\n"+Base64.getMimeEncoder(64,new byte[]{10}).encodeToString(b)+"\n-----END "+t+"-----\n";}
 static void protect(Path p)throws IOException{try{Files.setPosixFilePermissions(p,Set.of(PosixFilePermission.OWNER_READ,PosixFilePermission.OWNER_WRITE));}catch(UnsupportedOperationException ignored){}}
}
