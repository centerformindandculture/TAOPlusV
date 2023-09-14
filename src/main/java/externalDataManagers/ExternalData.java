package externalDataManagers;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

/**
 * Class to handle the parsing of External Data from various sources
 */
public abstract class ExternalData {

  /**
   * @param file      The file from which to read data
   * @param delimiter The delimiter between data items on each line of the file
   * @param match     An array with the elements that need to match the data.
   *                  Tries to match with the line in order, so if the only the
   *                  second element is important, pass in [".", secondElementMatch]
   * @return An array of the data items if found, null otherwise.
   */
  public static String[] getData(File file, String delimiter, String[] match) {
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      String row;
      while ((row = reader.readLine()) != null) {
        String[] data = row.split(delimiter);

        for (int i = 0; i < match.length; i++) {
          if (match[i].contains(".")) {
            continue;
          }
          if (!data[i].contains(match[i])) {
            break;
          }
          if (i == match.length - 1) {
            return data;
          }
        }
      }
    } catch (IOException e) {
      System.err.println(e);
      System.err.println("Couldn't read data: " + file.getName());
      return null;
    }
    System.err.println("Couldn't find data: " + Arrays.toString(match) + " in " + file.getPath());
    return null;
  }
}
