package core;

import simudyne.core.graph.Link;

public class Links {

  public static class PersonToPersonLink extends Link {
    // Best practice, anytime you have a particular link between two agents, and you want to have a
    // specific link that
    // captures that because when you want to send the message, and you can send the message across
    // the links, and its easier to call the links and actions.
  }

  public static class CentralAgentLink extends Link {
  }

  public static class SocialLink extends Link {
  }
}
