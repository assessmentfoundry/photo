package photo.util;

import org.testng.annotations.Test;

public class TimeZoneFinderTest {
  private TimeZoneFinder engine = TimeZoneFinder.get();

  @Test
  public void shouldList() {
    System.err.println("start test");
    engine.show("den");
  }
}
