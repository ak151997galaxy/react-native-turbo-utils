const path = require("path");
module.exports = function (api) {
  api.cache(true);
  return {
    presets: ["babel-preset-expo"],
    plugins: [
      [
        "module-resolver",
        {
          extensions: [".tsx", ".ts", ".js", ".json"],
          alias: {
            // For development, we want to alias the library to the source
            "react-native-turbo-utils": path.join(
              // eslint-disable-next-line no-undef
              __dirname,
              "..",
              "src",
              "index.ts"
            ),
          },
        },
      ],
    ],
  };
};
