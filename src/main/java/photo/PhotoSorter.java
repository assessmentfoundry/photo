package photo;

import static java.lang.String.format;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class PhotoSorter extends PhotoFixer {
  @Override
  protected void handle(Stream<Path> walk) {
    walk.filter(Files::isReadable) // read permission
        .filter(Files::isDirectory) // file only
        .forEach(p -> sortPhotos(p));
  }

  @Override
  protected Path photoRoot() {
    return super.photoRoot().resolve("saved");
  }

  private void sortPhotos(Path dir) {
    Map<ZonedDateTime, Path> ordered = new TreeMap<>();
    try {
      Files.list(dir)
          .filter(p -> isPhoto(p))
          .forEach(p -> ImageDateParser.shootTime(p).ifPresent(ts -> ordered.put(ts, p)));
      sort(ordered);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void sort(Map<ZonedDateTime, Path> files) {
    var slot = new AtomicInteger();
    files.forEach(
        (ts, p) -> {
          var p2 =
              p.getParent()
                  .resolve(
                      format("%s-%03d.%s", dirName(ts).getFileName(), slot.addAndGet(2), ext(p)));
          System.err.println(p2 + " vs " + p);
          if (p.equals(p2)) {
            System.err.printf("%s in right spot", p2);
          } else {
            if (Files.exists(p2)) moveAway(p2);
            move(p, p2);
          }
        });
  }

  protected void move(Path input, Path dest) {
    try {
      Files.move(input, dest);
    } catch (IOException e) {
      e.printStackTrace();
      throw new UncheckedIOException(e);
    }
  }

  private void moveAway(Path path) {
    final var baseName = baseName(path);
    try {
      var slot2 =
          Files.list(path.getParent())
                  .filter(p -> p.getFileName().toString().startsWith(baseName))
                  .count()
              + 1;
      var p2 = path.getParent().resolve(format("%s-%03d.%s", baseName, slot2, ext(path)));
      move(path, p2);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
