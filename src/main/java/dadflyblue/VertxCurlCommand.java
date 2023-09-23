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
  @Option(names = {"--host"}, defaultValue = "localhost", description = "The host name of remote server.")
  String host;

  @Option(names = {"--port", "-p"}, defaultValue = "9000", description = "The port of remote server")
  int port;

  @Parameters(paramLabel = "<url>", defaultValue = "", description = "The relative URI(Path+Query).")
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
      .setPort(port)
      .setMethod(HttpMethod.valueOf(method))
      .setURI(url);

    if (unixSocketAddress != null) {
      options.setServer(SocketAddress.domainSocketAddress(unixSocketAddress));
    }

    client.request(options)
        .chain(r -> r.send(data))
        .map(r ->  {
          System.out.println(
            "< " + r.version().alpnName().toUpperCase() + " " + r.statusCode() + " " + r.statusMessage() + " >");
          System.out.println();
          return r;
        })
        .onItem().transformToMulti(HttpClientResponse::toMulti)
        .invoke(System.out::print)
        .collect().last()
        .await().indefinitely();

    System.out.println();
  }

}
