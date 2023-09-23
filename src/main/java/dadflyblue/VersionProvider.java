package dadflyblue;

import org.eclipse.microprofile.config.ConfigProvider;
import picocli.CommandLine.IVersionProvider;

public final class VersionProvider implements IVersionProvider {

  final static String version;

  static {
    var c = ConfigProvider.getConfig();
    var vp = c.getOptionalValue("version", String.class);
    version = vp.orElse("null");
  }

  @Override
  public String[] getVersion() {
    return new String[]{version};
  }

}
