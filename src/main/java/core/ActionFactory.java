package core;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;

import java.util.function.Consumer;

/**
 * Util class for alternative Actions
 *
 * This class is no longer really necessary for TAO+V but removing it would be a slight hassle.
 */
public final class ActionFactory {
  private ActionFactory() {}

  public static <T extends Agent<Globals>> Action<T> createSuppressibleAction(Class<T> clazz, Consumer<T> runnableAction) {
    return Action.create(clazz, agent -> {
      runnableAction.accept(agent);
    });
  }
}