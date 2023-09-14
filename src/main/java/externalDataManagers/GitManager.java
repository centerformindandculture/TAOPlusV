package externalDataManagers;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.util.FS;

import java.io.File;
import java.io.IOException;

/**
 * Handles external git repositories.
 * Uses SSH keys to clone and pull
 */
public class GitManager {
  private static void cloneRepo(String URI, String directory, String sshKeyLocation, String sshKeyPassword)
      throws GitAPIException {
    TransportConfigCallback transportConfigCallback = new SshTransportConfigCallback(sshKeyLocation, sshKeyPassword);

    CloneCommand cloneCommand = Git.cloneRepository();
    cloneCommand.setURI(URI);
    cloneCommand.setDirectory(new File(directory));
    cloneCommand.setTransportConfigCallback(transportConfigCallback);
    cloneCommand.call();
  }

  private static void pullRepo(String directory) throws IOException {
    File repo = new File(directory);

    Git.open(repo).pull();
  }

  public static void getRepo(String URI, String directory, String sshKeyLocation, String sshKeyPassword)
      throws GitAPIException {
    try {
      pullRepo(directory);
    } catch (IOException e) {
      cloneRepo(URI, directory, sshKeyLocation, sshKeyPassword);
    }
  }

  /**
   * Handles SSH verification for the git manager.
   * You must point createDefaultJSch to a directory with your rsa key.
   * <p>
   * You must use a RSA key. To create one, use "ssh-keygen -t rsa".
   * Check the key. If it starts with -----BEGIN OPENSSH PRIVATE KEY-----
   * instead of -----BEGIN RSA PRIVATE KEY-----, you must add -m PEM to the
   * command: "ssh-keygen -t rsa -m PEM"
   */
  private static class SshTransportConfigCallback implements TransportConfigCallback {
    private String sshKeyLocation = "";
    private String sshKeyPassword = "";

    public SshTransportConfigCallback() {
      throw new IllegalStateException("Must pass in an RSA key");
    }

    public SshTransportConfigCallback(String sshKeyLocation, String sshKeyPassword) {
      if (sshKeyLocation == null) {
        throw new IllegalStateException("Must pass in an RSA key");
      }
      this.sshKeyLocation = sshKeyLocation;
      this.sshKeyPassword = (sshKeyPassword.equals(".")) ? "" : sshKeyPassword;
    }

    private final SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {

      @Override
      protected void configure(OpenSshConfig.Host hc, Session session) {
        session.setConfig("StrictHostKeyChecking", "no");
      }

      @Override
      protected JSch createDefaultJSch(FS fs) throws JSchException {
        JSch jSch = super.createDefaultJSch(fs);

        // The arguments to the following function are the directory of your rsa key file
        // and the password for the file
        jSch.addIdentity(sshKeyLocation, sshKeyPassword.getBytes());
        return jSch;
      }


    };

    @Override
    public void configure(Transport transport) {
      SshTransport sshTransport = (SshTransport) transport;
      sshTransport.setSshSessionFactory(sshSessionFactory);
    }
  }
}
