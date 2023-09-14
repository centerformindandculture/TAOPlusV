package core;

import com.google.common.collect.ImmutableList;
import simudyne.core.graph.Message;

import java.util.Comparator;
import java.util.List;

/**
 * A utility class with common utilities.
 */
public final class Utils {

  /**
   * Returns a sorted list of the messages, sorted by the sender. This assumes there is at most one message for
   * each sender, otherwise return order is not guaranteed across different simulations. We use
   * a copy here because it's better practice to reduce mutation.
   */
  public static <T extends Message> List<T> sortedCopyBySender(List<T> messages) {
    return ImmutableList.sortedCopyOf(Comparator.comparingLong(Message::getSender), messages);
  }

  // Prevent individual instances of class
  private Utils() {

  }
}
