package utils;

import java.io.*;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;

public final class Utils {

  private static BufferedReader inReader =
      new BufferedReader(new InputStreamReader(System.in));

  public static List<String> asResourceLines(String path) {
    InputStream resource = Utils.class.getResourceAsStream(path);

    if (resource == null)
      throw new IllegalArgumentException("Resource doesn't exist: " + path);

    // Using the "stupid scanner trick"
    // https://community.oracle.com/blogs/pat/2004/10/23/stupid-scanner-tricks
    return Arrays.asList(
        new Scanner(resource, "UTF-8").useDelimiter("\\A").next().split("\\n"));
  }

  public static void writeToFileOrStdout(String file, Object value) {
    if (file == null) {
      System.out.println(value);
    } else {
      try {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write(value.toString() + "\n");
        writer.flush();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public static String readFromFileOrStdin(String file) {
    if (file != null) {
      try {
        return new Scanner(new FileReader(file)).useDelimiter("\\A").next();
      } catch (FileNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
    return new Scanner(inReader).useDelimiter("\\A").next();
  }

  public static <T, E> E map(T t, Function<T, E> f) {
    return t != null ? f.apply(t) : null;
  }

  public static DayOfWeek parseDayOfWeek(String dayOfWeek) {
    switch (dayOfWeek) {
    case "Mo":
      return DayOfWeek.MONDAY;
    case "Tu":
      return DayOfWeek.TUESDAY;
    case "We":
      return DayOfWeek.WEDNESDAY;
    case "Th":
      return DayOfWeek.THURSDAY;
    case "Fr":
      return DayOfWeek.FRIDAY;
    case "Sa":
      return DayOfWeek.SATURDAY;
    case "Su":
      return DayOfWeek.SUNDAY;
    default:
      return DayOfWeek.valueOf(dayOfWeek);
    }
  }

  public static boolean deleteFile(File f) {
    if (f.isDirectory()) {
      for (File c : f.listFiles())
        deleteFile(c);
    }
    return f.delete();
  }

  public static void setObject(PreparedStatement stmt, int index, Object obj)
      throws SQLException {
    if (obj instanceof NullWrapper) {
      NullWrapper nullable = (NullWrapper)obj;
      if (nullable.value == null) {
        stmt.setNull(index, nullable.type);
      } else {
        setObject(stmt, index, nullable.value);
      }
    } else if (obj instanceof String) {
      stmt.setString(index, (String)obj);
    } else if (obj instanceof Integer) {
      stmt.setInt(index, (Integer)obj);
    } else if (obj instanceof Timestamp) {
      stmt.setTimestamp(index, (Timestamp)obj);
    } else if (obj instanceof Long) {
      stmt.setLong(index, (Long)obj);
    } else if (obj instanceof Array) {
      stmt.setArray(index, (Array)obj);
    } else if (obj instanceof Float) {
      stmt.setFloat(index, (Float)obj);
    } else if (obj instanceof Double) {
      stmt.setDouble(index, (Double)obj);
    } else if (obj == null) {
      throw new IllegalArgumentException("object is null");
    } else {
      throw new IllegalArgumentException(
          "type of object is incompatible for object=" + obj.toString());
    }
  }

  static class NullWrapper {
    int type;
    Object value;

    NullWrapper(int type, Object value) {
      this.type = type;
      this.value = value;
    }
  }

  public static NullWrapper nullable(int type, Object value) {
    return new NullWrapper(type, value);
  }

  public static PreparedStatement setArray(PreparedStatement stmt,
                                           Object... objs) {
    int i = 0;
    try {
      for (i = 0; i < objs.length; i++) {
        setObject(stmt, i + 1, objs[i]);
      }
      return stmt;
    } catch (Exception e) {
      System.err.println("at index " + i);
      throw new RuntimeException(e);
    }
  }
}
