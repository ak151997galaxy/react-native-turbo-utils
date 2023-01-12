import ReactNativeTurboUtilsModule from "./ReactNativeTurboUtilsModule";
import { Contacts, InstalledApps } from "./types";

export { Contacts, InstalledApps };

const { getDeviceData, getInstalledApps, getContacts, parseJwt, ...constants } =
  ReactNativeTurboUtilsModule;

export { getDeviceData, getInstalledApps, getContacts, parseJwt };

export default constants;
