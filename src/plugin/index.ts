import {
  AndroidConfig,
  ConfigPlugin,
  createRunOncePlugin,
  withMainApplication,
  withPlugins,
  withProjectBuildGradle,
} from '@expo/config-plugins';

const withHeaderInterceptor: ConfigPlugin = (config) => {
  return withMainApplication(config, async (config) => {
    // Add an OkHttpClientFactory
    // This factory will include our Device Headers interceptor
    // to the NetworkModule of RN
    config.modResults.contents = config.modResults.contents.replace(
      'public class MainApplication extends Application implements ReactApplication {',
      `import in.galaxycard.android.utils.DeviceHeadersInterceptor;
import com.facebook.react.modules.network.OkHttpClientFactory;
import com.facebook.react.modules.network.NetworkingModule;
import com.facebook.react.modules.network.OkHttpClientProvider;
import okhttp3.OkHttpClient;

public class MainApplication extends Application implements ReactApplication, OkHttpClientFactory {
  @Override
  public OkHttpClient createNewNetworkModuleClient() {
    OkHttpClient.Builder builder = OkHttpClientProvider.createClientBuilder(this);
    builder
      .addNetworkInterceptor(new DeviceHeadersInterceptor(this));
    return builder.build();
  }
  `
    );
    return config;
  });
};

const withKotlinGradlePlugin: ConfigPlugin = (config) => {
  return withProjectBuildGradle(config, async (config) => {
    config.modResults.contents = config.modResults.contents.replace(
      "classpath('de.undercouch:gradle-download-task:4.1.2')",
      "classpath('de.undercouch:gradle-download-task:4.1.2')\n" +
        'classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"'
    );
    return config;
  });
};

const withGalaxyCardUtils: ConfigPlugin = (config) => {
  const androidPermissions = [
    'android.permission.ACCESS_WIFI_STATE',
    'android.permission.READ_CONTACTS',
    'android.permission.ACCESS_NETWORK_STATE',
  ];
  return withPlugins(config, [
    withHeaderInterceptor,
    withKotlinGradlePlugin,
    [AndroidConfig.Permissions.withPermissions, androidPermissions],
  ]);
};

const pak = require('@galaxycard/react-native-turbo-utils/package.json');
export default createRunOncePlugin(withGalaxyCardUtils, pak.name, pak.version);
