package dadflyblue.unixsocket;

import javax.net.SocketFactory;

import io.vertx.mutiny.core.net.SocketAddress;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.SocketChannel;

public class UnixDomainSocketFactory extends SocketFactory {
  // implementation refers:
  // - https://www.morling.dev/blog/talking-to-postgres-through-java-16-unix-domain-socket-channels/
  // - https://github.com/square/okhttp/tree/master/samples/unixdomainsockets/src/main/java/okhttp3/unixdomainsockets
  // - https://inside.java/2021/02/03/jep380-unix-domain-sockets-channels/
  // - https://gist.github.com/jedvardsson/7ba7bbc94b4951f82da4b590ace725d2

  private final String path;

  public UnixDomainSocketFactory(String path) {
    this.path = path;
  }

  @Override
  public Socket createSocket() throws IOException {
    var channel = SocketChannel.open(StandardProtocolFamily.UNIX);
    return UnixDomainSocket.create(channel, path);
  }

  @Override
  public Socket createSocket(String host, int port) throws IOException {
    return createSocket();
  }

  @Override
  public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
    return createSocket();
  }

  @Override
  public Socket createSocket(InetAddress host, int port) throws IOException {
    return createSocket();
  }

  @Override
  public Socket createSocket(InetAddress host, int port, InetAddress localAddress, int localPort) throws IOException {
    return createSocket();
  }
}
