package photo.util;

import org.testng.annotations.Test;

import photo.util.TimeZoneFinder;

public class TimeZoneFinderTest {
  private TimeZoneFinder engine = TimeZoneFinder.get();

  @Test
  public void shouldList() {
    System.err.println("start test");
    engine.show("den");
  }

}
