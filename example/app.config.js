export default {
  expo: {
    name: "react-native-turbo-utils-example",
    jsEngine: "hermes",
    slug: "react-native-turbo-utils-example",
    version: "1.0.0",
    orientation: "portrait",
    icon: "./assets/icon.png",
    userInterfaceStyle: "light",
    plugins: ["react-native-turbo-utils"],
    splash: {
      image: "./assets/splash.png",
      resizeMode: "contain",
      backgroundColor: "#ffffff",
    },
    updates: {
      fallbackToCacheTimeout: 0,
    },
    assetBundlePatterns: ["**/*"],
    ios: {
      supportsTablet: true,
      bundleIdentifier: "in.galaxycard.android.utils.example",
    },
    android: {
      adaptiveIcon: {
        foregroundImage: "./assets/adaptive-icon.png",
        backgroundColor: "#FFFFFF",
      },
      package: "in.galaxycard.android.utils.example",
    },
    web: {
      favicon: "./assets/favicon.png",
    },
  },
};
