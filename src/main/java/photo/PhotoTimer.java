package photo;

import static java.lang.String.format;

import com.beust.jcommander.Parameter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import photo.util.TimeZoneFinder;

@Slf4j
public class PhotoTimer extends PhotoFixer {
  @Parameter(names = "-z", description = "time zone")
  private String timeZonePattern = ZoneId.systemDefault().toString();

  @Override
  public boolean handle() {
    System.err.println("handle " + timeZonePattern);
    if (timeZonePattern != null) {
      TimeZoneFinder.get().show(timeZonePattern);
      return true;
    } else return false;
    //    var dateString = Paths.get(arg(1)).getFileName().toString();
    //    var input = Paths.get(arg(2));
    //    var ts = toDate(dateString);
    //    var out = dirName(ts);
    //    System.err.println(input + " to " + out);
    //    try {
    //      Files.setAttribute(rename(input, ts), "creationTime", FileTime.from(ts.toInstant()));
    //      return true;
    //    } catch (IOException e) {
    //      log.atWarn().setCause(e).log();
    //      return false;
    //    }
  }

  private ZonedDateTime toDate(String ds) {
    return LocalDateTime.of(
            Integer.parseInt(ds.substring(0, 4)), Integer.parseInt(ds.substring(4, 6)),
            Integer.parseInt(ds.substring(6, 8)), Integer.parseInt(ds.substring(9, 11)),
            Integer.parseInt(ds.substring(11, 13)), Integer.parseInt(ds.substring(13, 15)))
        .atZone(ZoneId.systemDefault());
  }

  @Override
  protected void handle(Stream<Path> walk) {
    walk.filter(Files::isReadable) // read permission
        .filter(Files::isRegularFile) // file only
        .filter(
            p ->
                p.toString().endsWith(".jpeg")
                    || p.toString().endsWith(".JPG")
                    || p.toString().endsWith(".jpg"))
        .forEach(p -> addToAlbum(p));
  }

  private void addToAlbum(Path input) {
    ImageDateParser.shootTime(input)
        .ifPresentOrElse(
            ts -> {
              var copyto = dirName(ts);
              var prefix = copyto.getFileName().toString();
              System.out.println(copyto);
              if (!(input.getParent().equals(copyto)
                  && input.getFileName().toString().startsWith(prefix))) {
                try {
                  Files.createDirectories(copyto);
                  var seq = Files.list(copyto).count() * 2 + 1;
                  var dest = copyto.resolve(format("%s-%03d.jpg", prefix, seq));
                  System.err.println("mv " + input + " " + dest);
                  Files.move(input, dest);
                  Files.setLastModifiedTime(dest, FileTime.from(ts.toInstant()));
                } catch (IOException e) {
                  // TODO Auto-generated catch block
                  e.printStackTrace();
                }
              }
            },
            () -> System.out.printf("%s: %s%n", input, "missing time"));
  }
}
