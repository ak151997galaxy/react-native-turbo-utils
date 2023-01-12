import {
  AndroidConfig,
  ConfigPlugin,
  createRunOncePlugin,
  withAndroidManifest,
  withAppBuildGradle,
  withGradleProperties,
  withMainApplication,
  withPlugins,
  withProjectBuildGradle,
} from "@expo/config-plugins";

interface AdGydeProps {
  adgydeKey: string;
}

interface BugsnagProps {
  bugsnagKey: string;
}

interface FacebookProps {
  facebookAppId: string;
}

interface Props extends AdGydeProps, BugsnagProps, FacebookProps {}

const { addMetaDataItemToMainApplication, getMainApplicationOrThrow } =
  AndroidConfig.Manifest;

const withApplicationChanges: ConfigPlugin<Props> = (config, props: Props) => {
  return withMainApplication(config, async (config) => {
    // Add an OkHttpClientFactory
    // This factory will include our Device Headers interceptor
    // to the NetworkModule of RN
    config.modResults.contents = config.modResults.contents.replace(
      /public class MainApplication(.*) {$/m,
      `import in.galaxycard.android.utils.DeviceHeadersInterceptor;
    import com.facebook.react.modules.network.OkHttpClientFactory;
    import com.facebook.react.modules.network.NetworkingModule;
    import com.facebook.react.modules.network.OkHttpClientProvider;
    import okhttp3.OkHttpClient;
    import com.bugsnag.android.Bugsnag;
    import com.adgyde.android.AdGyde;

    public class MainApplication$1, OkHttpClientFactory {
      @Override
      public OkHttpClient createNewNetworkModuleClient() {
        OkHttpClient.Builder builder = OkHttpClientProvider.createClientBuilder(this);
        builder
          .addNetworkInterceptor(new DeviceHeadersInterceptor(this));
        return builder.build();
      }
      `
    );
    config.modResults.contents = config.modResults.contents.replace(
      "super.onCreate();",
      `$&
        OkHttpClientProvider.setOkHttpClientFactory(this);
        AdGyde.init(this, "${props.adgydeKey}", "Organic");
        AdGyde.setDebugEnabled(BuildConfig.DEBUG);`
    );
    config.modResults.contents = config.modResults.contents.replace(
      "SoLoader.init(this, /* native exopackage */ false);",
      `$&

        SoLoader.loadLibrary("bugsnag-ndk");
        SoLoader.loadLibrary("bugsnag-plugin-android-anr");

        Bugsnag.start(this);`
    );

    return config;
  });
};
const withBugsnagGradle: ConfigPlugin = (config) => {
  config = withProjectBuildGradle(config, async (config) => {
    config.modResults.contents = config.modResults.contents.replace(
      /classpath\('de.undercouch:gradle-download-task.*?$/m,
      `$&
            classpath("com.bugsnag:bugsnag-android-gradle-plugin:7.+")`
    );
    return config;
  });
  return withAppBuildGradle(config, async (config) => {
    config.modResults.contents = config.modResults.contents.replace(
      /^apply from: .*react.gradle.*$/m,
      `$&
    apply plugin: "com.bugsnag.android.gradle"

    bugsnag {
        uploadReactNativeMappings = true
    }`
    );
    config.modResults.contents = config.modResults.contents.replace(
      /dependencies {/,
      `$&
        implementation 'com.google.mlkit:barcode-scanning:17.0.2'
        implementation 'com.facebook.android:facebook-core:12.0.1'
        implementation 'com.adgyde:adgyde-androidx-sdk:4.1.12'`
    );
    return config;
  });
};

const withMetadata: ConfigPlugin<Props> = (config, props: Props) => {
  return withAndroidManifest(config, async (config) => {
    const mainApplication = getMainApplicationOrThrow(config.modResults);
    addMetaDataItemToMainApplication(
      mainApplication,
      "com.facebook.sdk.ApplicationId",
      props.facebookAppId
    );
    addMetaDataItemToMainApplication(
      mainApplication,
      "com.bugsnag.android.API_KEY",
      props.bugsnagKey
    );
    return config;
  });
};

const withBuildProperties: ConfigPlugin = (config) => {
  return withGradleProperties(config, async (config) => {
    config.modResults = config.modResults.map((item) => {
      // if (item.type === 'property' && item.key === 'expo.webp.animated') {
      //   item.value = 'true';
      // }
      // if (item.type === "property" && item.key === "newArchEnabled") {
      //   item.value = "true";
      // }
      return item;
    });
    return config;
  });
};

const withGalaxyCardUtils: ConfigPlugin<Props> = (config, props: Props) => {
  return withPlugins(config, [
    [withApplicationChanges, props],
    withBugsnagGradle,
    [withMetadata, props],
    withBuildProperties,
  ]);
};

const pak = require("@galaxycard/react-native-turbo-utils/package.json");
export default createRunOncePlugin(withGalaxyCardUtils, pak.name, pak.version);
