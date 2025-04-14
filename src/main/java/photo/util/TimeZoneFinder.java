package photo.util;

import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import java.io.Serializable;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TimeZoneFinder {
  public static class ZoneDossier implements Comparable<ZoneDossier>, Serializable {
    private static final long serialVersionUID = 1L;
    private ZoneId zone;
    private ZoneOffset offset;

    public ZoneDossier(ZoneId zone, LocalDateTime refDate) {
      this.zone = zone;
      offset = ZonedDateTime.of(refDate, zone).getOffset();
    }

    @Override
    public int compareTo(ZoneDossier o) {
      var r = sortValue().compareTo(o.sortValue());
      if (r == 0) r = zone.toString().compareTo(o.zone.toString());
      return r;
    }

    private Float sortValue() {
      var parts = offset.toString().split(":");
      if (parts.length == 1) {
        return 0f;
      } else {
        float r = Integer.parseInt(parts[0].substring(1));
        if (parts[0].startsWith("+")) {
          r = r * -1f;
        }
        r = r - (Integer.parseInt(parts[1]) / 60f);
        return r;
      }
    }

    @Override
    public String toString() {
      return zone + ": " + offset;
    }
  }

  /**
   * SingletonHolder is loaded on the first execution of Singleton.get() or the first access to
   * SingletonHolder.INSTANCE, not before.
   */
  private static class SingletonHolder {
    private static final TimeZoneFinder INSTANCE = new TimeZoneFinder();
  }

  public static TimeZoneFinder get() {
    return SingletonHolder.INSTANCE;
  }

  private LocalDateTime refDate;

  private TimeZoneFinder() {
    refDate = LocalDateTime.of(2020, 1, 1, 0, 0, 0);
  }

  private static LocalDateTime defaultShootTime;

  public void show(String pattern) {

    var zonePicker =
        pattern.startsWith("+") || pattern.startsWith("-") || pattern.equals("Z")
            ? (Predicate<ZoneDossier>) z -> z.offset.toString().startsWith(pattern)
            : (Predicate<ZoneDossier>) z -> z.zone.toString().toLowerCase().contains(pattern);
    var r =
        ZoneId.getAvailableZoneIds().stream()
            .map(z -> ZoneId.of(z))
            .map(z -> new ZoneDossier(z, refDate))
            .filter(zonePicker)
            .sorted()
            .toList();
    System.out.println(
        r.stream().map(z -> z.toString()).collect(Collectors.joining(System.lineSeparator())));
  }

  protected ZoneDossier digest(ZoneId zone) {
    return new ZoneDossier(zone, refDate);
  }

  public static void setDefaultShootTime(LocalDateTime dflt) {
    defaultShootTime = dflt;
  }

  //  public static Optional<ZonedDateTime> shootTime(Path resource) {
  //    try (InputStream in = new BufferedInputStream(Files.newInputStream(resource))) {
  //      var metadata = ImageMetadataReader.readMetadata(in);
  //      var d = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
  //      if (d != null && d.getDateOriginal() != null) {
  //        var ts = d.getDateOriginal().toInstant().atZone(ZoneId.systemDefault());
  //        if (ts.getYear() < 0) {
  //          ts = guessTime(resource);
  //        }
  //        if (ts != null)
  //          return Optional.of(ts);
  //      } else {
  //        showMetadata(metadata);
  //        return Optional.of(guessTime(resource));
  //      }
  //    } catch (IOException | ImageProcessingException e) {
  //      e.printStackTrace();
  //    }
  //    return Optional.empty();
  //  }

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
