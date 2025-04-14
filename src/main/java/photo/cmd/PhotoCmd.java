package photo.cmd;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.ParameterException;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import photo.PhotoDirTimer;
import photo.PhotoFixer;
import photo.PhotoMover;
import photo.PhotoSorter;
import photo.PhotoTimer;

public class PhotoCmd {
  private static class ParameterComparator
      implements Comparator<ParameterDescription>, Serializable {
    private static final long serialVersionUID = 1L;

    @Override
    public int compare(ParameterDescription left, ParameterDescription right) {
      return getName(left).compareTo(getName(right));
    }

    private String getName(ParameterDescription p) {
      var r = p.getNames();
      r = p.getLongestName();
      switch (r) {
        case "--help":
          r = "|" + r;
          break;
        case "-508start":
          r = "-section_508" + "aaa";
          break;
        case "-508end":
          r = "-section_508" + "zzz";
          break;
        case "-host":
          r = "-url" + r;
          break;
        case "-shots":
          r = "-record" + "001";
          break;
        default:
      }
      return r;
    }
  }

  protected static Logger logger = LoggerFactory.getLogger(PhotoCmd.class);

  @Parameter(
      names = {"-d", "-depth"},
      description = "Depth",
      hidden = false)
  private int maxDepth = Integer.MAX_VALUE;

  @Parameter(
      names = {"-a", "-p"},
      description = "Adjusted photos root",
      hidden = false)
  private Path albumPath = Paths.get(System.getProperty("user.home"), "Pictures/saved");

  private boolean usageMode;

  @Parameter(description = "paths")
  private List<String> paths;

  protected String arg(int offset) {
    return paths.get(offset);
  }

  public int run(String[] args) throws IOException {
    Map<String, PhotoTask> cmdSet = new HashMap<>();
    cmdSet.put("sort", new PhotoSorter());
    cmdSet.put("retime", new PhotoTimer());
    cmdSet.put("time2", new PhotoDirTimer());
    cmdSet.put("move", new PhotoMover());
    var p2 = createCmdParser(cmdSet);
    try {
      p2.parse(args);
    } catch (ParameterException e) {
      System.err.println("parse error:" + e);
      usageMode = true;
    }
    var cmd = p2.getParsedCommand();
    var task = (PhotoFixer) cmdSet.get(cmd);
    if (usageMode || cmd == null) {
      var inbuff = new StringBuilder();
      var outbuff = new StringBuffer();
      Comparator<ParameterDescription> sort = new ParameterComparator();
      if (cmd != null) p2 = createCmdParser(Collections.singletonMap(cmd, task));
      p2.setParameterDescriptionComparator(sort);
      for (JCommander cmd2 : p2.getCommands().values())
        cmd2.setParameterDescriptionComparator(sort);
      p2.getUsageFormatter().usage(inbuff);
      var p = Pattern.compile("[\r\n] * Default:");
      var m = p.matcher(inbuff);
      while (m.find()) {
        m.appendReplacement(outbuff, " --");
      }
      m.appendTail(outbuff);
      System.err.println(outbuff);
      return -1;
    } else {
      task.setAlbumPath(albumPath);
      var ok = task.handle();
      return ok ? 0 : 50;
    }
  }

  protected JCommander createCmdParser(Map<String, PhotoTask> cmdSet) {
    var p2 = new JCommander(this);
    cmdSet.entrySet().forEach(c -> p2.addCommand(c.getKey(), c.getValue()));
    return p2;
  }

  protected void addCommand(String cmd, String className, Map<String, PhotoCmd> cmdSet) {
    try {
      cmdSet.put(cmd, Class.forName(className).asSubclass(PhotoCmd.class).newInstance());
    } catch (ClassNotFoundException e) {
      System.err.println("skip " + cmd + ": " + className);
    } catch (RuntimeException | InstantiationException | IllegalAccessException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) throws IOException {
    new PhotoCmd().run(args);
    // var worker = switch (args.length > 0 ? args[0] : "move") {
    // case "sort" -> new PhotoSorter();
    // case "retime" -> new PhotoTimer();
    // case "time2" -> new PhotoDirTimer();
    // default -> new PhotoMover();
    // };
    // worker.handle(args);
    // logger.info("worker {}",worker.getClass().getSimpleName());
  }
}
