package photo;

import static java.lang.String.format;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

public class PhotoDirTimer extends PhotoFixer {

  private ZonedDateTime toDate(String ds) {
    System.err.println("toDate(" + ds + ")");
    return LocalDateTime.of(
            Integer.parseInt(ds.substring(0, 4)), Integer.parseInt(ds.substring(4, 6)),
            Integer.parseInt(ds.substring(6, 8)), Integer.parseInt(ds.substring(9, 11)),
            Integer.parseInt(ds.substring(11, 13)), Integer.parseInt(ds.substring(13, 15)))
        .atZone(ZoneId.systemDefault());
  }

  @Override
  protected void handle(Stream<Path> walk) {
    System.err.println(getClass().getName());
    walk.filter(Files::isReadable) // read permission
        .filter(Files::isDirectory) // file only
        .forEach(p -> movePhotos(p));
  }

  private void movePhotos(Path dir) {
    var tbase = dir.getFileName().toString();
    if (tbase.length() == 10) {
      var ts = toDate(tbase.replace("-", "") + "-123000");
      try {
        Files.list(dir)
            .forEach(
                p -> {
                  var p2 = rename(p, ts);
                  try {
                    Files.setAttribute(p2, "creationTime", FileTime.from(ts.toInstant()));
                  } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                  }
                });
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
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
