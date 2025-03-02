import { TurboModule, TurboModuleRegistry } from 'react-native';

export interface Spec extends TurboModule {
  getConstants(): {
    uniqueId: string;
    deviceId: string;
    bundleId: string;
    systemVersion: string;
    appVersion: string;
    buildNumber: string;
    appName: string;
    brand: string;
    model: string;
    manufacturer: string;
    screenWidth: number;
    screenHeight: number;
    screenDensity: number;
    installReferrer: string;
  };
  getContacts(): Promise<{
    contacts: {
      id: number;
      name?: string;
      phones?: string[];
      emails?: string[];
      photo?: string;
    }[];
    hash: string;
  }>;
  getDeviceData(): {
    hasHeadphones: boolean;
    carrier: string;
    airplaneMode: boolean;
    batteryState: string;
    batteryLevel: number;
    lowPowerMode: boolean;
    pinOrFingerprintSet: boolean;
    fontScale: number;
    freeDiskStorage: number;
    totalDiskCapacity: number;
    maxMemory: number;
    usedMemory: number;
    hasLocation: boolean;
    wifiName: string;
    accessPointName: string;
    deviceName: string;
  };
  getInstalledApps(): Promise<{
    apps: {
      system: boolean;
      name: string;
      package: string;
      install: number;
      update: number;
    }[];
    hash: string;
  }>;
  parseJwt(
    jwt: string,
    key: string | null
  ): {
    user_id: number;
    phone: number;
    email: string;
    status: string;
    status_data: string;
    name: string;
  };
}

export default TurboModuleRegistry.getEnforcing<Spec>('TurboUtils');
