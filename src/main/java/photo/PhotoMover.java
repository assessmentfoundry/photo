package photo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class PhotoMover extends PhotoFixer {

  @Override
  protected void handle(Stream<Path> walk) {
    walk.filter(p -> Files.isReadable(p)) // read permission
        .filter(Files::isRegularFile) // file only
        .filter(p -> isPhoto(p))
        .forEach(p -> addToAlbum(p));
  }

  @Override
  protected Path photoRoot() {
    return super.photoRoot().resolve("Saved Pictures");
  }

  private void addToAlbum(Path input) {
    ImageDateParser.shootTime(input)
        .ifPresentOrElse(
            ts -> {
              rename(input, ts);
            },
            () -> {
              System.out.printf("%s: %s%n", input, "missing time");
            });
  }
}
