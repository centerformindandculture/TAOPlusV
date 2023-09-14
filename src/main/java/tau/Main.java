package tau;

import simudyne.nexus.Server;

public class Main {
  public static void main(String[] args) {
    Server.register("TAU", TAUModel.class);
    Server.run();
  }
}
