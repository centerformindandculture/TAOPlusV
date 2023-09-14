package externalDataManagers;

import java.io.*;
import java.util.HashMap;

public class StateManager {
  private static StateManager manager = null;
  private static final String DELIMITER = " ";

  private HashMap<String, String> state;
  private String stateFile;

  private StateManager() {
    state = new HashMap<>();
  }

  public static StateManager getManager() {
    if (manager == null) {
      manager = new StateManager();
    }
    return manager;
  }

  public StateManager getStateFromFile(String fileName) {
    state.clear();
    stateFile = System.getProperty("user.dir") + "/" + fileName;
    readFromFile();
    return this;
  }

  public StateManager writeStateToFile(String fileName) {
    this.stateFile = fileName;
    writeToFile();
    return this;
  }

  public StateManager writeStateToFile(String fileName, HashMap<String, String> state) {
    this.state = state;
    return writeStateToFile(fileName);
  }

  public String get(String key) {
    return manager.state.get(key);
  }

  public String put(String key, String val) {
    return manager.state.put(key, val);
  }

  private void readFromFile() {
    String line;
    try (BufferedReader reader = new BufferedReader(new FileReader(stateFile))) {

      while ((line = reader.readLine()) != null) {
        if (line.startsWith("//")) {
          continue;
        }
        String[] parts = line.split(DELIMITER);
        if (parts.length != 2) {
          System.out.println("Invalid state entry.");
          continue;
        }
        state.put(parts[0], parts[1]);
      }
    } catch (FileNotFoundException fe) {
      throw new IllegalStateException("Invalid input state file");
    } catch (IOException ioe) {
      throw new IllegalStateException("Couldn't read state file");
    }
  }

  private void writeToFile() {

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(stateFile))) {

      for (String k : state.keySet()) {
        String line = k + " " + state.get(k);
        writer.write(line);
        writer.newLine();
      }
    } catch (FileNotFoundException fe) {
      throw new IllegalStateException("Invalid output state file");
    } catch (IOException ioe) {
      throw new IllegalStateException("Couldn't write to state file");
    }
  }

  public void printState() {
    for (String k : state.keySet()) {
      System.out.println(k + ": " + state.get(k));
    }
    System.out.println("\n");
  }


}
