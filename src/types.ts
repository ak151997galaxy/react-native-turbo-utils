export type InstalledApps = {
  apps: {
    system: boolean;
    name: string;
    package: string;
    install: number;
    update: number;
  }[];
  hash: string;
};

export type Contacts = {
  contacts: {
    id: number;
    name?: string;
    phones?: string[];
    emails?: string[];
    photo?: string;
  }[];
  hash: string;
};
