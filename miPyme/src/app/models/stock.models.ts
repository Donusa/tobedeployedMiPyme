export interface MeasurementUnit {
  unitCode: string;
  unitName: string;
}

export interface ProductBrand {
  brandId?: number;
  brandName: string;
}

export interface ProductCategory {
  categoryId?: number;
  categoryName: string;
  parentCategory?: ProductCategory;
}

export interface Warehouse {
  warehouseId?: number;
  warehouseCode: string;
  warehouseName: string;
  address?: string;
}

export interface StorageLocation {
  storageLocationId?: number;
  warehouse: Warehouse;
  locationCode: string;
  locationDescription?: string;
  locationType?: string;
}

export interface Product {
  productId?: number;
  internalCode?: string;
  productName: string;
  measurementUnit?: MeasurementUnit;
  measurementValue?: string;
  productCategory?: ProductCategory;
  productBrand?: ProductBrand;
  warehouse?: Warehouse;
  storage?: StorageLocation;
  price?: number;
  cost?: number;
  stockQuantity?: number;
  minStock?: number;
  isActive?: boolean;
  tiendaNubeId?: number;
  mercadoLibreId?: string;
}

export interface ProductVariantAttribute {
  attributeId?: number;
  attributeKey: string;
  attributeValue: string;
}

export interface ProductVariant {
  productVariantId?: number;
  product?: Product;
  variantSku?: string;
  variantGtin?: string;
  attributes?: ProductVariantAttribute[];
  netWeightGrams?: number;
  sizeDimensionsCm?: string;
  price?: number;
  cost?: number;
  stockQuantity?: number;
  minStock?: number;
  isActive?: boolean;
  tiendaNubeId?: number;
}
