package dadflyblue.unixsocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.util.Set;

class UnixDomainSocket extends Socket {
  static final SocketOption<Boolean> SO_OOBINLINE =
      new SocketOption<>() {
        public String name() {
          return "SO_OOBINLINE";
        }

        public Class<Boolean> type() {
          return Boolean.class;
        }

        public String toString() {
          return name();
        }
  };

  private final SocketChannel channel;
  private final UnixDomainSocketAddress address;
  private volatile boolean inputShutdown;
  private volatile boolean outputShutdown;
  private volatile int timeout;

  private UnixDomainSocket(SocketChannel channel, String path) throws IOException {
    super(DummySocketImpl.create());
    this.channel = channel;
    this.address = UnixDomainSocketAddress.of(path);
  }

  static Socket create(SocketChannel channel, String path) {
    try {
      return new UnixDomainSocket(channel, path);
    } catch (IOException e) {
      throw new InternalError(e);
    }
  }

  private InetSocketAddress localAddress() throws IOException {
    return InetSocketAddress.createUnresolved("localhost", 8000);
  }

  private InetSocketAddress remoteAddress() throws IOException {
    return localAddress();
  }

  @Override
  public void connect(SocketAddress remote) {
    if (!channel.isConnected()) {
      connect(remote, 0);
    }
  }

  @Override
  public void connect(SocketAddress remote, int timeout) {
    if (remote == null)
      throw new IllegalArgumentException("connect: The address can't be null");
    if (timeout < 0)
      throw new IllegalArgumentException("connect: timeout can't be negative");
    try {
      channel.connect(address);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void bind(SocketAddress local) {
    try {
      channel.bind(local);
    } catch (Exception x) {
      throw new RuntimeException(x);
    }
  }

  @Override
  public InetAddress getInetAddress() {
    try {
      return InetAddress.getByName("localhost");
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public InetAddress getLocalAddress() {
    if (channel.isOpen()) {
      return getLocalAddress();
    }
    return new InetSocketAddress(0).getAddress();
  }

  @Override
  public int getPort() {
    InetSocketAddress remote;
    try {
      remote = remoteAddress();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return remote.getPort();
  }

  @Override
  public int getLocalPort() {
    InetSocketAddress local;
    try {
      local = localAddress();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return local.getPort();
  }

  @Override
  public SocketAddress getRemoteSocketAddress() {
    try {
      return channel.getRemoteAddress();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public SocketAddress getLocalSocketAddress() {
    try {
      return channel.getLocalAddress();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public SocketChannel getChannel() {
    return channel;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    if (!channel.isOpen())
      throw new SocketException("Socket is closed");
    if (!channel.isConnected())
      throw new SocketException("Socket is not connected");
    if (isInputShutdown())
      throw new SocketException("Socket input is shutdown");
    return Channels.newInputStream(channel);
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    if (!channel.isOpen())
      throw new SocketException("Socket is closed");
    if (!channel.isConnected())
      throw new SocketException("Socket is not connected");
    if (isOutputShutdown())
      throw new SocketException("Socket output is shutdown");
    return Channels.newOutputStream(channel);
  }

  private void setBooleanOption(SocketOption<Boolean> name, boolean value) {
    try {
      channel.setOption(name, value);
    } catch (IOException x) {
      throw new RuntimeException(x);
    }
  }

  private void setIntOption(SocketOption<Integer> name, int value) {
    try {
      channel.setOption(name, value);
    } catch (IOException x) {
      throw new RuntimeException(x);
    }
  }

  private boolean getBooleanOption(SocketOption<Boolean> name) {
    try {
      return channel.getOption(name);
    } catch (IOException x) {
      throw new RuntimeException(x);
    }
  }

  private int getIntOption(SocketOption<Integer> name) {
    try {
      return channel.getOption(name);
    } catch (IOException x) {
      throw new RuntimeException(x);
    }
  }

  @Override
  public boolean getTcpNoDelay() {
    return getBooleanOption(StandardSocketOptions.TCP_NODELAY);
  }

  @Override
  public void setTcpNoDelay(boolean on) {
    setBooleanOption(StandardSocketOptions.TCP_NODELAY, on);
  }

  @Override
  public void setSoLinger(boolean on, int linger) {
    if (!on)
      linger = -1;
    setIntOption(StandardSocketOptions.SO_LINGER, linger);
  }

  @Override
  public int getSoLinger() {
    return getIntOption(StandardSocketOptions.SO_LINGER);
  }

  @Override
  public void sendUrgentData(int data) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean getOOBInline() {
    if (channel.supportedOptions().contains(SO_OOBINLINE)) {
      return getBooleanOption(SO_OOBINLINE);
    }
    return false;
  }

  @Override
  public void setOOBInline(boolean on) {
    if (channel.supportedOptions().contains(SO_OOBINLINE)) {
      setBooleanOption(SO_OOBINLINE, on);
    }
  }

  @Override
  public int getSoTimeout() throws SocketException {
    if (!channel.isOpen())
      throw new SocketException("Socket is closed");
    return timeout;
  }

  @Override
  public void setSoTimeout(int timeout) throws SocketException {
    if (!channel.isOpen())
      throw new SocketException("Socket is closed");
    if (timeout < 0)
      throw new IllegalArgumentException("timeout < 0");
    this.timeout = timeout;
  }

  @Override
  public int getSendBufferSize() {
    return getIntOption(StandardSocketOptions.SO_SNDBUF);
  }

  @Override
  public void setSendBufferSize(int size) {
    // size 0 valid for SocketChannel, invalid for Socket
    if (size <= 0)
      throw new IllegalArgumentException("Invalid send size");
    setIntOption(StandardSocketOptions.SO_SNDBUF, size);
  }

  @Override
  public int getReceiveBufferSize() {
    return getIntOption(StandardSocketOptions.SO_RCVBUF);
  }

  @Override
  public void setReceiveBufferSize(int size) {
    // size 0 valid for SocketChannel, invalid for Socket
    if (size <= 0)
      throw new IllegalArgumentException("Invalid receive size");
    setIntOption(StandardSocketOptions.SO_RCVBUF, size);
  }

  @Override
  public boolean getKeepAlive() {
    return getBooleanOption(StandardSocketOptions.SO_KEEPALIVE);
  }

  @Override
  public void setKeepAlive(boolean on) {
    setBooleanOption(StandardSocketOptions.SO_KEEPALIVE, on);
  }

  @Override
  public int getTrafficClass() {
    return getIntOption(StandardSocketOptions.IP_TOS);
  }

  @Override
  public void setTrafficClass(int tc) {
    setIntOption(StandardSocketOptions.IP_TOS, tc);
  }

  @Override
  public boolean getReuseAddress() {
    return getBooleanOption(StandardSocketOptions.SO_REUSEADDR);
  }

  @Override
  public void setReuseAddress(boolean on) {
    setBooleanOption(StandardSocketOptions.SO_REUSEADDR, on);
  }

  @Override
  public void close() throws IOException {
    channel.close();
  }

  @Override
  public void shutdownInput() {
    try {
      inputShutdown = true;
      channel.shutdownInput();
    } catch (Exception x) {
      throw new RuntimeException(x);
    }
  }

  @Override
  public void shutdownOutput() {
    try {
      channel.shutdownOutput();
      outputShutdown = true;
    } catch (Exception x) {
      throw new RuntimeException(x);
    }
  }

  @Override
  public String toString() {
    if (channel.isConnected())
      return "Socket[addr=" + getInetAddress() +
          ",port=" + getPort() +
          ",localport=" + getLocalPort() + "]";
    return "Socket[unconnected]";
  }

  @Override
  public boolean isConnected() {
    return channel.isConnected();
  }

  @Override
  public boolean isBound() {
    try {
      return channel.getLocalAddress() != null;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean isClosed() {
    return !channel.isOpen();
  }

  @Override
  public boolean isInputShutdown() {
    return inputShutdown;
  }

  @Override
  public boolean isOutputShutdown() {
    return outputShutdown;
  }

  @Override
  public <T> Socket setOption(SocketOption<T> name, T value) throws IOException {
    channel.setOption(name, value);
    return this;
  }

  @Override
  public <T> T getOption(SocketOption<T> name) throws IOException {
    return channel.getOption(name);
  }

  @Override
  public Set<SocketOption<?>> supportedOptions() {
    return channel.supportedOptions();
  }
}

