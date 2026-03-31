import { Component, OnInit } from '@angular/core';

import { ActivatedRoute, Router } from '@angular/router';
import { StockService } from '../../../services/stock.service';
import { Product, ProductVariant, MeasurementUnit, ProductBrand, ProductCategory, Warehouse, StorageLocation } from '../../../models/stock.models';
import { forkJoin } from 'rxjs';
import { BarcodeFormat } from '@zxing/library';

@Component({
    selector: 'app-product-form',
    templateUrl: './product-form.component.html',
    styleUrls: ['./product-form.component.css']
})
export class ProductFormComponent implements OnInit {

    showScanner = false;
    scanningField: 'productCode' | 'variantSku' | 'variantGtin' | null = null;
    allowedFormats = [BarcodeFormat.QR_CODE, BarcodeFormat.EAN_13, BarcodeFormat.EAN_8, BarcodeFormat.CODE_128, BarcodeFormat.CODE_39, BarcodeFormat.ITF, BarcodeFormat.UPC_A, BarcodeFormat.UPC_E];


    availableDevices: MediaDeviceInfo[] = [];
    currentDevice: MediaDeviceInfo | undefined;
    hasDevices: boolean = false;
    hasPermission: boolean = false;
    scannerEnabled: boolean = false;


    units: MeasurementUnit[] = [];
    brands: ProductBrand[] = [];
    categories: ProductCategory[] = [];
    warehouses: Warehouse[] = [];
    locations: StorageLocation[] = [];


    formFilteredLocations: StorageLocation[] = [];


    isEditing = false;
    isLoading = false;
    private pendingLoads = 0;
    productId: number | null = null;


    saveState: 'idle' | 'saving' | 'success' | 'error' = 'idle';
    private saveTimer: any;

    currentProduct: Product = this.getEmptyProduct();
    currentVariants: ProductVariant[] = [];
    newVariant: ProductVariant = { attributes: [] };
    editingVariantId: number | undefined;


    createModalType: 'unit' | 'brand' | 'category' | 'warehouse' | 'location' | null = null;
    showCreateModal = false;
    newItemName = '';
    newItemCode = '';
    newItemAddress = '';
    newItemDescription = '';
    newItemType = '';
    modalContextWarehouse: Warehouse | undefined;

    constructor(
        private stockService: StockService,
        private route: ActivatedRoute,
        private router: Router
    ) { }

    ngOnInit(): void {
        this.loadDropdownData();


        const idParam = this.route.snapshot.paramMap.get('id');
        if (idParam && idParam !== 'new') {
            this.productId = +idParam;
            this.isEditing = true;
            this.loadProduct(this.productId);
        }
    }

    private startLoading() {
        this.pendingLoads++;
        this.isLoading = true;
    }

    private finishLoading() {
        this.pendingLoads = Math.max(0, this.pendingLoads - 1);
        if (this.pendingLoads === 0) {
            this.isLoading = false;
        }
    }

    loadDropdownData() {
        this.startLoading();
        forkJoin({
            units: this.stockService.getMeasurementUnits(),
            brands: this.stockService.getBrands(),
            categories: this.stockService.getCategories(),
            warehouses: this.stockService.getWarehouses(),
            locations: this.stockService.getLocations()
        }).subscribe({
            next: ({ units, brands, categories, warehouses, locations }) => {
                this.units = units;
                this.brands = brands;
                this.categories = categories;
                this.warehouses = warehouses;
                this.locations = locations;


                if (this.currentProduct.productId) {
                    this.rebindReferences();
                }
                this.finishLoading();
            },
            error: (err) => {
                console.error('Error loading dropdowns', err);
                this.finishLoading();
            }
        });
    }

    loadProduct(id: number) {
        this.startLoading();
        this.stockService.getProducts().subscribe({
            next: products => {
                const product = products.find(p => p.productId === id);
                if (product) {
                    this.currentProduct = { ...product };
                    this.rebindReferences();
                    this.loadVariants(id);
                } else {
                    alert('Producto no encontrado');
                    this.router.navigate(['/stock/products']);
                }
                this.finishLoading();
            },
            error: (err) => {
                console.error('Error loading product', err);
                this.finishLoading();
            }
        });
    }

    loadVariants(productId: number) {
        this.stockService.getVariants(productId).subscribe(variants => {
            this.currentVariants = variants;
        });
    }

    getEmptyProduct(): Product {
        return {
            productName: '',
            isActive: true
        };
    }

    rebindReferences() {
        if (!this.currentProduct) return;




        if (this.currentProduct.measurementUnit) {
            const found = this.units.find(u => u.unitCode === this.currentProduct.measurementUnit?.unitCode);
            if (found) this.currentProduct.measurementUnit = found;
        }
        if (this.currentProduct.productBrand) {
            const found = this.brands.find(b => b.brandId === this.currentProduct.productBrand?.brandId);
            if (found) this.currentProduct.productBrand = found;
        }
        if (this.currentProduct.productCategory) {
            const found = this.categories.find(c => c.categoryId === this.currentProduct.productCategory?.categoryId);
            if (found) this.currentProduct.productCategory = found;
        }
        if (this.currentProduct.warehouse) {
            const warehouse = this.warehouses.find(w => w.warehouseId === this.currentProduct.warehouse?.warehouseId);
            if (warehouse) {
                this.currentProduct.warehouse = warehouse;
                this.filterFormLocations(warehouse.warehouseId);
            }
        } else {
            this.filterFormLocations(undefined);
        }
        if (this.currentProduct.storage) {
            const location = this.locations.find(l => l.storageLocationId === this.currentProduct.storage?.storageLocationId);
            if (location) this.currentProduct.storage = location;
        }
    }

    filterFormLocations(warehouseId: number | undefined) {
        if (!warehouseId) {
            this.formFilteredLocations = [];
            return;
        }
        this.formFilteredLocations = this.locations.filter(l => l.warehouse.warehouseId === warehouseId);
    }

    saveProduct() {
        if (this.saveState !== 'idle') return;
        this.saveState = 'saving';

        if (this.isEditing && this.currentProduct.productId) {
            this.stockService.updateProduct(this.currentProduct.productId, this.currentProduct).subscribe({
                next: () => {
                    this.saveState = 'success';
                    this.saveTimer = setTimeout(() => this.router.navigate(['/stock/products']), 1500);
                },
                error: () => {
                    this.saveState = 'error';
                    this.saveTimer = setTimeout(() => { this.saveState = 'idle'; }, 2000);
                }
            });
        } else {
            this.stockService.createProduct(this.currentProduct).subscribe({
                next: (newProduct) => {
                    this.saveState = 'success';
                    this.saveTimer = setTimeout(() => this.router.navigate(['/stock/products', newProduct.productId]), 1500);
                },
                error: () => {
                    this.saveState = 'error';
                    this.saveTimer = setTimeout(() => { this.saveState = 'idle'; }, 2000);
                }
            });
        }
    }

    cancel() {
        this.router.navigate(['/stock/products']);
    }


    compareUnits(o1: any, o2: any): boolean { return o1 && o2 ? o1.unitCode === o2.unitCode : o1 === o2; }
    compareBrands(o1: any, o2: any): boolean { return o1 && o2 ? o1.brandId === o2.brandId : o1 === o2; }
    compareCategories(o1: any, o2: any): boolean { return o1 && o2 ? o1.categoryId === o2.categoryId : o1 === o2; }
    compareWarehouses(o1: any, o2: any): boolean { return o1 && o2 ? o1.warehouseId === o2.warehouseId : o1 === o2; }
    compareLocations(o1: any, o2: any): boolean { return o1 && o2 ? o1.storageLocationId === o2.storageLocationId : o1 === o2; }


    onUnitChange(value: any) {
        if (value === 'NEW') { this.openModal('unit'); setTimeout(() => this.currentProduct.measurementUnit = undefined); }
        else { this.currentProduct.measurementUnit = value; }
    }
    onBrandChange(value: any) {
        if (value === 'NEW') { this.openModal('brand'); setTimeout(() => this.currentProduct.productBrand = undefined); }
        else { this.currentProduct.productBrand = value; }
    }
    onCategoryChange(value: any) {
        if (value === 'NEW') { this.openModal('category'); setTimeout(() => this.currentProduct.productCategory = undefined); }
        else { this.currentProduct.productCategory = value; }
    }
    onFormWarehouseChange(value: any) {
        if (value === 'NEW') { this.openModal('warehouse'); setTimeout(() => this.currentProduct.warehouse = undefined); }
        else {
            this.currentProduct.warehouse = value;
            this.filterFormLocations(value?.warehouseId);
            this.currentProduct.storage = undefined;
        }
    }
    onFormLocationChange(value: any) {
        if (value === 'NEW') {
            if (!this.currentProduct.warehouse) {
                alert('Seleccione un depósito primero');
                setTimeout(() => this.currentProduct.storage = undefined);
                return;
            }
            this.modalContextWarehouse = this.currentProduct.warehouse;
            this.openModal('location');
            setTimeout(() => this.currentProduct.storage = undefined);
        }
        else { this.currentProduct.storage = value; }
    }


    openModal(type: any) {
        this.createModalType = type;
        this.newItemName = '';
        this.newItemCode = '';
        this.newItemAddress = '';
        this.newItemDescription = '';
        this.newItemType = '';
        this.showCreateModal = true;
    }

    closeModal() {
        this.showCreateModal = false;
        this.createModalType = null;
    }

    getModalTitle(): string {
        switch (this.createModalType) {
            case 'unit': return 'Unidad de Medida';
            case 'brand': return 'Marca';
            case 'category': return 'Categoría';
            case 'warehouse': return 'Depósito';
            case 'location': return 'Ubicación';
            default: return '';
        }
    }

    saveNewItem() {
        if (!this.newItemName && this.createModalType !== 'location') return;
        if (this.createModalType === 'location' && !this.newItemCode) return;

        if (this.createModalType === 'brand') {
            this.stockService.createBrand({ brandName: this.newItemName }).subscribe(saved => {
                this.brands.push(saved);
                this.currentProduct.productBrand = saved;
                this.closeModal();
            });
        } else if (this.createModalType === 'category') {
            this.stockService.createCategory({ categoryName: this.newItemName }).subscribe(saved => {
                this.categories.push(saved);
                this.currentProduct.productCategory = saved;
                this.closeModal();
            });
        } else if (this.createModalType === 'unit') {
            if (!this.newItemCode) return;
            this.stockService.createMeasurementUnit({ unitCode: this.newItemCode, unitName: this.newItemName }).subscribe(saved => {
                this.units.push(saved);
                this.currentProduct.measurementUnit = saved;
                this.closeModal();
            });
        } else if (this.createModalType === 'warehouse') {
            if (!this.newItemCode) return;
            this.stockService.createWarehouse({ warehouseCode: this.newItemCode, warehouseName: this.newItemName, address: this.newItemAddress }).subscribe(saved => {
                this.warehouses.push(saved);
                this.currentProduct.warehouse = saved;
                this.filterFormLocations(saved.warehouseId);
                this.closeModal();
            });
        } else if (this.createModalType === 'location') {
            if (!this.modalContextWarehouse) return;
            this.stockService.createLocation({
                warehouse: this.modalContextWarehouse,
                locationCode: this.newItemCode,
                locationDescription: this.newItemDescription,
                locationType: this.newItemType
            }).subscribe(saved => {
                this.locations.push(saved);
                this.filterFormLocations(this.currentProduct.warehouse?.warehouseId);
                this.currentProduct.storage = saved;
                this.closeModal();
            });
        }
    }


    addVariantAttribute() {
        if (!this.newVariant.attributes) { this.newVariant.attributes = []; }
        this.newVariant.attributes.push({ attributeKey: '', attributeValue: '' });
    }

    removeVariantAttribute(index: number) {
        if (this.newVariant.attributes) {
            this.newVariant.attributes.splice(index, 1);
        }
    }

    saveVariant() {
        if (!this.currentProduct.productId) return;


        if (this.newVariant.attributes) {
            this.newVariant.attributes = this.newVariant.attributes.filter(a => a.attributeKey && a.attributeValue);
        }

        if (this.editingVariantId) {
            this.stockService.updateVariant(this.editingVariantId, this.newVariant).subscribe(() => {
                this.reloadVariants();
                this.cancelVariantEdit();
            });
        } else {
            this.stockService.createVariant(this.currentProduct.productId, this.newVariant).subscribe(() => {
                this.reloadVariants();
                this.cancelVariantEdit();
            });
        }
    }

    editVariant(variant: ProductVariant) {
        this.editingVariantId = variant.productVariantId;
        this.newVariant = JSON.parse(JSON.stringify(variant));
    }

    cancelVariantEdit() {
        this.editingVariantId = undefined;
        this.newVariant = { attributes: [] };
    }

    deleteVariant(variant: ProductVariant) {
        if (!confirm('¿Eliminar variante?')) return;
        if (this.currentProduct.productId && variant.productVariantId) {
            this.stockService.deleteVariant(variant.productVariantId).subscribe(() => {
                this.reloadVariants();
            });
        }
    }

    reloadVariants() {
        if (this.currentProduct.productId) {
            this.loadVariants(this.currentProduct.productId);
        }
    }


    startScanning(field: 'productCode' | 'variantSku' | 'variantGtin') {
        this.scanningField = field;
        this.showScanner = true;
        this.scannerEnabled = true;
        this.checkCameraPermission();
    }

    checkCameraPermission() {
        navigator.mediaDevices.getUserMedia({ video: true })
            .then(stream => {
                this.hasPermission = true;
                stream.getTracks().forEach(track => track.stop());
            })
            .catch(err => {
                this.hasPermission = false;
                console.warn('Camera permission denied:', err);
            });
    }

    onCamerasFound(devices: MediaDeviceInfo[]): void {
        this.availableDevices = devices;
        this.hasDevices = Boolean(devices && devices.length);
        if (this.hasDevices && !this.currentDevice) {
            const backCamera = devices.find(d => d.label.toLowerCase().includes('back') || d.label.toLowerCase().includes('trasera'));
            this.currentDevice = backCamera || devices[0];
        }
    }

    onPermissionResponse(permission: boolean): void {
        this.hasPermission = permission;
    }

    onDeviceSelectChange(selected: string) {
        const device = this.availableDevices.find(x => x.deviceId === selected);
        if (device) this.currentDevice = device;
    }

    handleScanSuccess(result: string) {
        if (result) {
            if (this.scanningField === 'productCode') {
                this.currentProduct.internalCode = result;
            } else if (this.scanningField === 'variantSku') {
                this.newVariant.variantSku = result;
            } else if (this.scanningField === 'variantGtin') {
                this.newVariant.variantGtin = result;
            }
            this.closeScanner();
        }
    }

    closeScanner() {
        this.showScanner = false;
        this.scanningField = null;
        this.scannerEnabled = false;
    }

    trackByVariant(index: number, variant: ProductVariant): number {
        return variant.productVariantId || index;
    }

    trackByAttribute(index: number, attr: any): any {
        return index;
    }
}
