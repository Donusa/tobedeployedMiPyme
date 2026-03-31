# Stock API Examples

Use these curl commands to test the Stock Management module.

**Prerequisite:** You must be logged in. Replace `{{token}}` with your JWT token obtained from `/api/auth/login` (see `API_EXAMPLES.md`).

## 0. Setup (IMPORTANT)

If you are using an existing company created before the Stock module was added, you **MUST** run this command once to create the necessary tables in your database schema.

### Update Schema
```bash
curl --location --request POST 'http://localhost:8080/api/schema/update' \
--header 'Authorization: Bearer {{token}}'
```

## 1. Measurement Units

### Create Unit
```bash
curl --location 'http://localhost:8080/api/stock/units' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {{token}}' \
--data '{
    "unitCode": "KG",
    "unitName": "Kilogram"
}'
```

### List Units
```bash
curl --location 'http://localhost:8080/api/stock/units' \
--header 'Authorization: Bearer {{token}}'
```

### Update Unit
```bash
curl --location --request PUT 'http://localhost:8080/api/stock/units/KG' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {{token}}' \
--data '{
    "unitName": "Kilogram (Updated)"
}'
```

### Delete Unit
```bash
curl --location --request DELETE 'http://localhost:8080/api/stock/units/KG' \
--header 'Authorization: Bearer {{token}}'
```

## 2. Product Brands

### Create Brand
```bash
curl --location 'http://localhost:8080/api/stock/brands' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {{token}}' \
--data '{
    "brandName": "Acme Corp"
}'
```

### List Brands
```bash
curl --location 'http://localhost:8080/api/stock/brands' \
--header 'Authorization: Bearer {{token}}'
```

### Update Brand
```bash
curl --location --request PUT 'http://localhost:8080/api/stock/brands/1' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {{token}}' \
--data '{
    "brandName": "Acme Corp Updated"
}'
```

### Delete Brand
```bash
curl --location --request DELETE 'http://localhost:8080/api/stock/brands/1' \
--header 'Authorization: Bearer {{token}}'
```

## 3. Product Categories

### Create Category
```bash
curl --location 'http://localhost:8080/api/stock/categories' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {{token}}' \
--data '{
    "categoryName": "Electronics"
}'
```

### Create Sub-Category (assuming parent ID is 1)
Note: The system will verify the parent ID and prevent duplicate names under the same parent.
```bash
curl --location 'http://localhost:8080/api/stock/categories' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {{token}}' \
--data '{
    "categoryName": "Laptops",
    "parentCategory": { "categoryId": 1 }
}'
```

### List Categories (and Sub-Categories)
```bash
curl --location 'http://localhost:8080/api/stock/categories' \
--header 'Authorization: Bearer {{token}}'
```

### Update Category
```bash
curl --location --request PUT 'http://localhost:8080/api/stock/categories/1' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {{token}}' \
--data '{
    "categoryName": "Electronics & Gadgets"
}'
```

### Delete Category
```bash
curl --location --request DELETE 'http://localhost:8080/api/stock/categories/1' \
--header 'Authorization: Bearer {{token}}'
```

## 4. Warehouses & Locations

### Create Warehouse
Note: Warehouse names and codes must be unique.
```bash
curl --location 'http://localhost:8080/api/stock/warehouses' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {{token}}' \
--data '{
    "warehouseCode": "WH-MAIN",
    "warehouseName": "Main Warehouse",
    "address": "123 Industrial Park"
}'
```

### List Warehouses
```bash
curl --location 'http://localhost:8080/api/stock/warehouses' \
--header 'Authorization: Bearer {{token}}'
```

### Update Warehouse
```bash
curl --location --request PUT 'http://localhost:8080/api/stock/warehouses/1' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {{token}}' \
--data '{
    "warehouseCode": "WH-MAIN-UPDATED",
    "warehouseName": "Main Warehouse Updated",
    "address": "456 Industrial Park"
}'
```

### Delete Warehouse
```bash
curl --location --request DELETE 'http://localhost:8080/api/stock/warehouses/1' \
--header 'Authorization: Bearer {{token}}'
```

### Create Storage Location (in Warehouse 1)
```bash
curl --location 'http://localhost:8080/api/stock/warehouses/1/locations' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {{token}}' \
--data '{
    "locationCode": "A-01-01",
    "locationDescription": "Aisle A, Rack 1, Shelf 1",
    "locationType": "SHELF"
}'
```

### List Storage Locations (All)
```bash
curl --location 'http://localhost:8080/api/stock/locations' \
--header 'Authorization: Bearer {{token}}'
```

### List Storage Locations (By Warehouse)
```bash
curl --location 'http://localhost:8080/api/stock/warehouses/1/locations' \
--header 'Authorization: Bearer {{token}}'
```

### Update Storage Location
```bash
curl --location --request PUT 'http://localhost:8080/api/stock/locations/1' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {{token}}' \
--data '{
    "locationCode": "A-01-01-UPD",
    "locationDescription": "Aisle A, Rack 1, Shelf 1 Updated",
    "locationType": "BIN"
}'
```

### Delete Storage Location
```bash
curl --location --request DELETE 'http://localhost:8080/api/stock/locations/1' \
--header 'Authorization: Bearer {{token}}'
```

## 5. Products

### Create Product
Requires IDs for Unit, Category, and Brand created previously.

```bash
curl --location 'http://localhost:8080/api/stock/products' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {{token}}' \
--data '{
    "internalCode": "PROD-001",
    "productName": "Gaming Laptop X1",
    "measurementUnit": { "unitCode": "KG" },
    "productCategory": { "categoryId": 2 },
    "productBrand": { "brandId": 1 },
    "isActive": true
}'
```

### List Products
```bash
curl --location 'http://localhost:8080/api/stock/products' \
--header 'Authorization: Bearer {{token}}'
```

### Update Product
```bash
curl --location --request PUT 'http://localhost:8080/api/stock/products/1' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {{token}}' \
--data '{
    "internalCode": "PROD-001-UPD",
    "productName": "Gaming Laptop X1 Pro",
    "measurementUnit": { "unitCode": "KG" },
    "productCategory": { "categoryId": 2 },
    "productBrand": { "brandId": 1 },
    "isActive": true
}'
```

### Delete Product
```bash
curl --location --request DELETE 'http://localhost:8080/api/stock/products/1' \
--header 'Authorization: Bearer {{token}}'
```

### Create Product Variant (for Product 1)
```bash
curl --location 'http://localhost:8080/api/stock/products/1/variants' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer {{token}}' \
--data '{
    "variantSku": "SKU-GLX1-RED",
    "variantGtin": "1234567890123",
    "attributeValues": "{\"color\": \"red\", \"ram\": \"16GB\"}",
    "netWeightGrams": 2500,
    "sizeDimensionsCm": "35x25x2",
    "isActive": true
}'
```

### Delete Product Variant
```bash
curl --location --request DELETE 'http://localhost:8080/api/stock/variants/1' \
--header 'Authorization: Bearer {{token}}'
```
