

export interface MobileCardColumn {

  field: string;

  label: string;

  isPrimary?: boolean;

  isBadge?: boolean;

  badgeClass?: string | ((item: any) => string);

  format?: (value: any, item: any) => string;

  expandOnly?: boolean;
}

export interface MobileCardAction {

  label: string;

  icon: string;

  callback: (item: any) => void;

  condition?: (item: any) => boolean;

  variant?: 'default' | 'danger' | 'success';
}
