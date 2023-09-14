package tau;

import simudyne.nexus.Server;

public class MainCLI {
  public static void main(String[] args) {
    Server.register("TAU", TAUModel.class);
    Server.run(args);
  }
}
