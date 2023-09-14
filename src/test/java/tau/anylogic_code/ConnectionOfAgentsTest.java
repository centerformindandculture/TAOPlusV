package tau.anylogic_code;

import core.Globals;
import org.junit.Before;
import org.junit.Test;
import simudyne.core.abm.testkit.TestKit;

import static com.google.common.truth.Truth.assertThat;

public class ConnectionOfAgentsTest {

  private TestKit<Globals> testKit;

  @Before
  public void setUp() throws Exception {
    testKit = TestKit.create(Globals.class);
  }

  @Test
  public void testInit() {
    testKit.getGlobals().tOneDay = 2;
    ConnectionOfAgents.tOneDay = testKit.getGlobals().tOneDay;

    ConnectionOfAgents c = new ConnectionOfAgents(1, 1, 1);

    assertThat(c.frequency).isEqualTo(1);
    assertThat(c.stepWithinDay).isEqualTo(1);
    assertThat(c._id).isEqualTo(1);
  }

  @Test(expected = IllegalStateException.class)
  public void testBadInit() {
    testKit.getGlobals().tOneDay = 2;
    ConnectionOfAgents.tOneDay = testKit.getGlobals().tOneDay;

    ConnectionOfAgents c = new ConnectionOfAgents(1, 2, 1);
  }
}
