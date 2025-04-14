package photo;

import static java.lang.String.format;

import com.beust.jcommander.Parameter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import photo.cmd.PhotoTask;

@Data
@Slf4j
public abstract class PhotoFixer implements PhotoTask {
  private int maxDepth = Integer.MAX_VALUE;
  private LocalDate defaultDate;
  private LocalTime defaultTime;

  @Parameter(description = "paths")
  private List<String> paths;

  private Path albumPath;

  protected String arg(int offset) {
    System.err.println("arg(" + offset + ") vs " + paths.size());
    return offset < paths.size() ? paths.get(offset) : "";
  }

  @Override
  public boolean handle() {
    log.info("{} {}", getClass().getSimpleName(), paths);
    var photoRoot = photoRoot();
    for (var x = 0; x < paths.size(); x++) {
      try (var walk = Files.walk(photoRoot.resolve(arg(x)))) {
        handle(walk);
      } catch (IOException e) {
        System.err.println(e.getMessage());
        e.printStackTrace();
      }
    }
    return true;
  }

  protected Path photoRoot() {
    return Paths.get(System.getProperty("user.home"), "Pictures");
  }

  protected boolean isPhoto(Path p) {
    var filename = p.toString();
    return filename.endsWith(".jpeg")
        || filename.endsWith(".JPG")
        || filename.endsWith(".jpg")
        || filename.endsWith(".heif");
  }

  protected abstract void handle(Stream<Path> walk);

  protected Path rename(Path input, ZonedDateTime ts) {
    var copyto = dirName(ts);
    var prefix = copyto.getFileName().toString();
    System.out.println(copyto);
    if (!(input.getParent().equals(copyto) && input.getFileName().toString().startsWith(prefix))) {
      try {
        Files.createDirectories(copyto);
        var seq = Files.list(copyto).count() * 2 + 1;
        var dest = copyto.resolve(format("%s-%03d.jpg", prefix, seq));
        if (!input.equals(dest)) {
          while (Files.exists(dest)) {
            seq += 2;
            dest = copyto.resolve(format("%s-%03d.jpg", prefix, seq));
          }
        }
        System.err.println("mv " + input + " " + dest);
        Files.move(input, dest);
        Files.setLastModifiedTime(dest, FileTime.from(ts.toInstant()));
        return dest;
      } catch (IOException e) {
        e.printStackTrace();
        return null;
      }
    } else {
      return input;
    }
  }

  protected Path dirName(ZonedDateTime ts) {
    var year = format("%04d", ts.getYear());
    var month = format("%02d", ts.getMonthValue());
    var day = format("%02d", ts.getDayOfMonth());
    var copyto =
        albumPath
            .resolve(format("%s", year))
            .resolve(format("%s-%s", year, month))
            .resolve(format("%s-%s-%s", year, month, day));
    return copyto;
  }

  protected String baseName(Path path) {
    var baseName = path.getFileName().toString();
    var period = baseName.lastIndexOf(".");
    if (period > 0) baseName = baseName.substring(0, period);
    return baseName;
  }

  protected String ext(Path path) {
    var r = path.getFileName().toString();
    return r.substring(r.lastIndexOf(".") + 1);
  }
}
