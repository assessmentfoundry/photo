package photo;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

public class ImageDateParser {
  private static LocalDateTime defaultShootTime;

  public static void setDefaultShootTime(LocalDateTime dflt) {
    defaultShootTime = dflt;
  }

  public static Optional<ZonedDateTime> shootTime(Path resource) {
    try (InputStream in = new BufferedInputStream(Files.newInputStream(resource))) {
      var metadata = ImageMetadataReader.readMetadata(in);
      var d = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
      if (d != null && d.getDateOriginal() != null) {
        var ts = d.getDateOriginal().toInstant().atZone(ZoneId.systemDefault());
        if (ts.getYear() < 0) {
          ts = guessTime(resource);
        }
        if (ts != null) return Optional.of(ts);
      } else {
        showMetadata(metadata);
        return Optional.of(guessTime(resource));
      }
    } catch (IOException | ImageProcessingException e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }

  private static ZonedDateTime guessTime(Path resource) throws IOException {
    var ts =
        min(
            Files.readAttributes(resource, BasicFileAttributes.class).creationTime(),
            Files.getLastModifiedTime(resource));
    if (ts != null) return ts.toInstant().atZone(ZoneId.systemDefault());
    return null;
  }

  private static FileTime min(FileTime... times) {
    FileTime r = null;
    for (var time : times) {
      if (r == null) r = time;
      else if (time != null && time.toInstant().isBefore(r.toInstant()) && time.toMillis() > 0L)
        r = time;
    }
    return r;
  }

  private static void showMetadata(Metadata metadata) {
    for (Directory d : metadata.getDirectories()) {
      System.out.println(d + " " + d.getClass().getName());
      System.out.println("---------------");
      for (Tag tag : d.getTags()) System.out.println(tag);
      System.out.println();
    }
  }
}
