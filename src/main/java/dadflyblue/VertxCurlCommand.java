package dadflyblue;

import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.http.HttpClientResponse;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "vertx-curl",
        mixinStandardHelpOptions = true,
        versionProvider = VersionProvider.class)
public class VertxCurlCommand implements Runnable {

  @Option(names = {"--ssl"}, defaultValue = "false", description = "Use ssl or not?")
  boolean ssl;

  @Option(names = {"--host"}, defaultValue = "localhost", description = "The host name, default: localhost.")
  String host;

  @Option(names = {"--port", "-p"}, defaultValue = "9000", description = "The port, default: 9000")
  int port;

  @Parameters(paramLabel = "<url>", defaultValue = "", description = "The relative URI(path+query).")
  String url;

  @Option(names = {"--data", "-d"}, defaultValue = "", description = "The data(payload) to send.")
  String data;

  @Option(names = {"--method", "-m"}, defaultValue = "GET", description = "The http method.")
  String method;

  @Option(names = {"--unix-socket", "-x"}, description = "The unix socket address.")
  String unixSocketAddress;

  @Override
  public void run() {
    var client = Vertx.vertx(
      new VertxOptions().setPreferNativeTransport(true)).createHttpClient();

    var options = new RequestOptions()
      .setHost(host)
      .setSsl(ssl)
      .setPort(port)
      .setMethod(HttpMethod.valueOf(method))
      .setURI(url);

    // Currently, this only works for JVM but not native-image of Graalvm.
    if (unixSocketAddress != null) {
      options.setServer(SocketAddress.domainSocketAddress(unixSocketAddress));
    }

    client.request(options)
        .chain(r -> r.send(data))
        .onItem().transformToMulti(HttpClientResponse::toMulti)
        .invoke(System.out::print)
        .collect().last()
        .await().indefinitely();

    System.out.println();
  }

}
