import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { StockService } from '../../../services/stock.service';
import { TiendaNubeService } from '../../../services/tienda-nube.service';
import { MercadoLibreService } from '../../../services/mercadolibre.service';
import { Product, ProductVariant, MeasurementUnit, ProductBrand, ProductCategory, Warehouse, StorageLocation } from '../../../models/stock.models';
import { forkJoin, Subject } from 'rxjs';
import { finalize, timeout, catchError, takeUntil } from 'rxjs/operators';
import { BarcodeFormat } from '@zxing/library';
import { PlanService } from '../../../services/plan.service';
import { LayoutService } from '../../../services/layout.service';
import { MobileCardColumn, MobileCardAction } from '../../../shared/components/mobile-card-list/mobile-card-list.models';

@Component({
    selector: 'app-product',
    templateUrl: './product.component.html',
    styleUrls: ['./product.component.css']
})
export class ProductComponent implements OnInit, OnDestroy {
    private destroy$ = new Subject<void>();

    showScanner = false;
    showEmbeddedScanner = false;
    scanningField: 'productCode' | 'search' | null = null;
    allowedFormats = [BarcodeFormat.QR_CODE, BarcodeFormat.EAN_13, BarcodeFormat.EAN_8, BarcodeFormat.CODE_128, BarcodeFormat.CODE_39, BarcodeFormat.ITF, BarcodeFormat.UPC_A, BarcodeFormat.UPC_E];


    availableDevices: MediaDeviceInfo[] = [];
    currentDevice: MediaDeviceInfo | undefined;
    hasDevices: boolean = false;
    hasPermission: boolean = false;
    scannerEnabled: boolean = false;
    lastScanTime: number = 0;

    isLoading = false;
    loadError = false;

    products: Product[] = [];
    displayedProducts: Product[] = [];
    units: MeasurementUnit[] = [];
    brands: ProductBrand[] = [];
    categories: ProductCategory[] = [];
    warehouses: Warehouse[] = [];
    locations: StorageLocation[] = [];
    filteredLocations: StorageLocation[] = [];
    formFilteredLocations: StorageLocation[] = [];
    selectedGlobalWarehouse: Warehouse | undefined;
    selectedGlobalLocation: StorageLocation | undefined;


    searchTerm: string = '';

    currentProduct: Product = this.getEmptyProduct();



    isEditing = false;
    showProductForm = false;


    showCreateModal = false;
    createModalType: 'unit' | 'brand' | 'category' | 'warehouse' | 'location' = 'brand';
    modalContextWarehouse: Warehouse | undefined;
    newItemName = '';
    newItemCode = '';
    newItemAddress = '';
    newItemDescription = '';
    newItemType = '';


    showDeleteModal = false;
    deleteType: 'product' | 'variant' = 'product';

    itemToDeleteId: number | undefined;
    itemToDeleteName: string = '';


    showPublishModal = false;
    publishStep = 1;
    publishProduct_: Product | null = null;
    publishChannel: 'tiendanube' | 'mercadolibre' = 'tiendanube';
    publishLoading = false;
    publishError: string | null = null;
    publishSuccess: string | null = null;
    tnConnected = false;
    mlConnected = false;
    publishFields: any = {};


    mlCategories: any[] = [];
    mlListingTypes: any[] = [];
    mlAttributes: any[] = [];
    mlAttributesValues: any = {};
    categorySearchTerm = '';
    showCategoryDropdown = false;
    private categorySearchTimeout: any = null;

    constructor(
        private stockService: StockService,
        private route: ActivatedRoute,
        private router: Router,
        private tiendaNubeService: TiendaNubeService,
        private mercadoLibreService: MercadoLibreService,
        public planService: PlanService,
        public layoutService: LayoutService
    ) { }


    readonly mobileColumns: MobileCardColumn[] = [
      { field: 'productName',               label: 'Producto',    isPrimary: true },
      { field: 'internalCode',              label: 'Código',      format: (v: any) => v || '—' },
      { field: 'productCategory.categoryName', label: 'Categoría', format: (v: any) => v || '—' },
      {
        field: 'price', label: 'Precio',
        format: (v: any) => new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS', maximumFractionDigits: 0 }).format(v ?? 0)
      },
      {
        field: 'stockQuantity', label: 'Stock',
        format: (v: any) => String(v ?? 0),
        isBadge: true,
        badgeClass: (p: Product) => {
          const s = p.stockQuantity ?? 0;
          const m = p.minStock ?? 0;
          if (m > 0 && s <= m)         return 'badge-danger';
          if (m > 0 && s <= m * 1.5)   return 'badge-warning';
          return 'badge-success';
        }
      }
    ];

    readonly mobileActions: MobileCardAction[] = [
      { label: 'Editar',    icon: 'bx bx-edit-alt',   callback: (p: Product) => this.edit(p) },
      { label: 'Eliminar', icon: 'bx bx-trash',        variant: 'danger', callback: (p: Product) => this.delete(p) }
    ];



    trackByProduct(index: number, product: Product): number {
        return product.productId || index;
    }

    trackByVariant(index: number, variant: ProductVariant): number {
        return variant.productVariantId || index;
    }

    trackByAttribute(index: number, attr: any): any {
        return index;
    }

    trackById(index: number, item: any): any {
        return item.id || item.code || index;
    }

    ngOnInit(): void {
        this.loadData();
    }

    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
    }

    loadData() {

        this.destroy$.next();

        this.isLoading = true;
        this.loadError = false;



        const safe = <T>(obs: any, fallback: T) =>
            obs.pipe(
                timeout(15000),
                catchError(() => {
                    this.loadError = true;
                    return [fallback];
                })
            );

        forkJoin({
            products:   safe(this.stockService.getProducts(),           []),
            units:      safe(this.stockService.getMeasurementUnits(),   []),
            brands:     safe(this.stockService.getBrands(),             []),
            categories: safe(this.stockService.getCategories(),         []),
            warehouses: safe(this.stockService.getWarehouses(),         []),
            locations:  safe(this.stockService.getLocations(),          [])
        }).pipe(
            takeUntil(this.destroy$),
            finalize(() => this.isLoading = false)
        ).subscribe({
            next: (data: any) => {
                const { products, units, brands, categories, warehouses, locations } = data;
                this.products = products;
                this.units = units;
                this.brands = brands;
                this.categories = categories;
                this.warehouses = warehouses;
                this.locations = locations;

                this.filterProducts();


                const editId = this.route.snapshot.queryParamMap.get('editId');
                const variantId = this.route.snapshot.queryParamMap.get('variantId');

                if (editId) {
                    this.router.navigate(['/stock/products', editId]);
                }
            },
            error: (err) => {
                console.error('Error loading data', err);
                this.loadError = true;
                this.isLoading = false;
            }
        });
    }

    filterProducts() {
        this.displayedProducts = this.products.filter(p => {
            let matchWarehouse = true;
            let matchLocation = true;
            let matchSearch = true;

            if (this.selectedGlobalWarehouse) {
                matchWarehouse = p.warehouse?.warehouseId === this.selectedGlobalWarehouse.warehouseId;
            }

            if (this.selectedGlobalLocation) {
                matchLocation = p.storage?.storageLocationId === this.selectedGlobalLocation.storageLocationId;
            }

            if (this.searchTerm) {
                const terms = this.searchTerm.toLowerCase().split(' ').filter(t => t.length > 0);


                const searchString = [
                    p.internalCode,
                    p.productName,
                    p.productBrand?.brandName,
                    p.productCategory?.categoryName,
                    p.warehouse?.warehouseName,
                    p.storage?.locationCode,
                    p.storage?.locationDescription
                ].map(s => (s || '').toLowerCase()).join(' ');


                matchSearch = terms.every(term => searchString.includes(term));
            }

            return matchWarehouse && matchLocation && matchSearch;
        });
    }

    getEmptyProduct(): Product {
        return {
            productName: '',
            isActive: true
        };
    }

    saveProduct() {







        if (this.isEditing && this.currentProduct.productId) {
            this.stockService.updateProduct(this.currentProduct.productId, this.currentProduct).subscribe(() => {
                this.loadData();
                this.resetForm();
            });
        } else {
            this.stockService.createProduct(this.currentProduct).subscribe(createdProduct => {
                this.loadData();
                this.resetForm();
            });
        }
    }

    edit(product: Product) {
        this.router.navigate(['/stock/products', product.productId]);
    }

    openCreateForm() {
        this.router.navigate(['/stock/products/new']);
    }

    filterLocations(warehouseId: number | undefined) {
        if (!warehouseId) {
            this.filteredLocations = [];
            return;
        }
        this.filteredLocations = this.locations.filter(l => l.warehouse.warehouseId === warehouseId);
    }

    filterFormLocations(warehouseId: number | undefined) {
        if (!warehouseId) {
            this.formFilteredLocations = [];
            return;
        }
        this.formFilteredLocations = this.locations.filter(l => l.warehouse.warehouseId === warehouseId);
    }

    onFormWarehouseChange(value: any) {
        if (value === 'NEW') {
            this.openModal('warehouse');

            setTimeout(() => this.currentProduct.warehouse = undefined);
        } else {
            this.currentProduct.warehouse = value;
            this.filterFormLocations(value?.warehouseId);
            this.currentProduct.storage = undefined;
        }
    }

    onFormLocationChange(value: any) {
        if (value === 'NEW') {
            if (!this.currentProduct.warehouse) {
                alert('Debe seleccionar un depósito primero');
                setTimeout(() => this.currentProduct.storage = undefined);
                return;
            }
            this.modalContextWarehouse = this.currentProduct.warehouse;
            this.openModal('location');
            setTimeout(() => this.currentProduct.storage = undefined);
        } else {
            this.currentProduct.storage = value;
        }
    }

    delete(p: Product) {
        this.deleteType = 'product';
        this.itemToDeleteId = p.productId;
        this.itemToDeleteName = p.productName || 'este producto';
        this.showDeleteModal = true;
    }

    confirmDelete() {
        if (!this.itemToDeleteId) return;

        if (this.deleteType === 'product') {
            this.stockService.deleteProduct(this.itemToDeleteId).subscribe(() => {
                this.loadData();
                this.closeDeleteModal();
            });
        }
    }

    closeDeleteModal() {
        this.showDeleteModal = false;
        this.itemToDeleteId = undefined;
        this.itemToDeleteName = '';
    }

    resetForm() {
        this.currentProduct = this.getEmptyProduct();
        this.isEditing = false;
        this.showProductForm = false;
    }













    onUnitChange(value: any) {
        if (value === 'NEW') {
            this.openModal('unit');

            setTimeout(() => this.currentProduct.measurementUnit = undefined);
        } else {
            this.currentProduct.measurementUnit = value;
        }
    }

    onBrandChange(value: any) {
        if (value === 'NEW') {
            this.openModal('brand');
            setTimeout(() => this.currentProduct.productBrand = undefined);
        } else {
            this.currentProduct.productBrand = value;
        }
    }

    onCategoryChange(value: any) {
        if (value === 'NEW') {
            this.openModal('category');
            setTimeout(() => this.currentProduct.productCategory = undefined);
        } else {
            this.currentProduct.productCategory = value;
        }
    }

    onGlobalWarehouseChange(value: any) {
        if (value === 'NEW') {
            this.openModal('warehouse');
        } else {
            this.selectedGlobalWarehouse = value;
            this.filterLocations(value?.warehouseId);

            this.selectedGlobalLocation = undefined;
            this.filterProducts();
        }
    }

    onGlobalLocationChange(value: any) {
        if (value === 'NEW') {
            this.modalContextWarehouse = this.selectedGlobalWarehouse;
            this.openModal('location');
        } else {
            this.selectedGlobalLocation = value;
            this.filterProducts();
        }
    }





    openModal(type: 'unit' | 'brand' | 'category' | 'warehouse' | 'location') {
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

    compareWarehouses(w1: Warehouse, w2: Warehouse): boolean {
        return w1 && w2 ? w1.warehouseId === w2.warehouseId : w1 === w2;
    }

    compareLocations(l1: StorageLocation, l2: StorageLocation): boolean {
        return l1 && l2 ? l1.storageLocationId === l2.storageLocationId : l1 === l2;
    }

    compareUnits(u1: MeasurementUnit, u2: MeasurementUnit): boolean {
        return u1 && u2 ? u1.unitCode === u2.unitCode : u1 === u2;
    }

    compareBrands(b1: ProductBrand, b2: ProductBrand): boolean {
        return b1 && b2 ? b1.brandId === b2.brandId : b1 === b2;
    }

    compareCategories(c1: ProductCategory, c2: ProductCategory): boolean {
        return c1 && c2 ? c1.categoryId === c2.categoryId : c1 === c2;
    }


    toggleEmbeddedScanner() {
        this.showEmbeddedScanner = !this.showEmbeddedScanner;


        if (this.showEmbeddedScanner) {
            this.showScanner = false;
            this.scanningField = 'search';
            this.scannerEnabled = true;
            this.checkCameraPermission();
        } else {
            this.scannerEnabled = false;
        }
    }

    startScanning(field: 'productCode' | 'search') {
        this.scanningField = field;
        this.showScanner = true;
        this.showEmbeddedScanner = false;
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
                console.warn('Camera permission denied or error:', err);
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
        if (!permission) {
            console.warn('Camera permission denied');
        }
    }

    onDeviceSelectChange(selected: string) {
        const device = this.availableDevices.find(x => x.deviceId === selected);
        if (device) {
            this.currentDevice = device;
        }
    }

    handleScanSuccess(result: string) {
        if (!result) return;


        const now = Date.now();
        if (now - this.lastScanTime < 2000) return;
        this.lastScanTime = now;

        this.playBeep();

        if (this.showEmbeddedScanner || (this.scanningField === 'search' && !this.showScanner)) {
            this.searchTerm = result;
            this.filterProducts();

        } else {

            if (this.scanningField === 'productCode') {
                this.currentProduct.internalCode = result;
            } else if (this.scanningField === 'search') {

                this.searchTerm = result;
                this.filterProducts();
            }
            this.closeScanner();
        }
    }

    closeScanner() {
        this.showScanner = false;
        this.showEmbeddedScanner = false;
        this.scanningField = null;
        this.scannerEnabled = false;
    }

    playBeep() {
        const audio = new Audio();
        audio.src = 'assets/beep.mp3';
        if (!audio.src || audio.src.endsWith('undefined')) {
            this.playOscillatorBeep();
        } else {
            audio.play().catch(e => this.playOscillatorBeep());
        }
    }

    playOscillatorBeep() {
        try {
            const ctx = new (window.AudioContext || (window as any).webkitAudioContext)();
            const osc = ctx.createOscillator();
            osc.type = 'sine';
            osc.frequency.setValueAtTime(880, ctx.currentTime);
            osc.connect(ctx.destination);
            osc.start();
            osc.stop(ctx.currentTime + 0.1);
        } catch (e) {
            console.error('AudioContext error', e);
        }
    }

    saveNewItem() {

        if (!this.newItemName && this.createModalType !== 'location') {
            return;
        }


        if (this.createModalType === 'location' && !this.newItemCode) return;

        if (this.createModalType === 'brand') {
            const newBrand: ProductBrand = { brandName: this.newItemName };
            this.stockService.createBrand(newBrand).subscribe(savedBrand => {
                this.brands.push(savedBrand);
                this.currentProduct.productBrand = savedBrand;
                this.closeModal();
            });
        } else if (this.createModalType === 'category') {
            const newCategory: ProductCategory = { categoryName: this.newItemName };
            this.stockService.createCategory(newCategory).subscribe(savedCategory => {
                this.categories.push(savedCategory);
                this.currentProduct.productCategory = savedCategory;
                this.closeModal();
            });
        } else if (this.createModalType === 'unit') {
            if (!this.newItemCode) {
                alert('El código es requerido para la unidad de medida');
                return;
            }
            const newUnit: MeasurementUnit = { unitCode: this.newItemCode, unitName: this.newItemName };
            this.stockService.createMeasurementUnit(newUnit).subscribe(savedUnit => {
                this.units.push(savedUnit);
                this.currentProduct.measurementUnit = savedUnit;
                this.closeModal();
            });
        } else if (this.createModalType === 'warehouse') {
            if (!this.newItemCode) {
                alert('El código es requerido para el depósito');
                return;
            }
            const newWarehouse: Warehouse = {
                warehouseCode: this.newItemCode,
                warehouseName: this.newItemName,
                address: this.newItemAddress
            };
            this.stockService.createWarehouse(newWarehouse).subscribe(savedWarehouse => {
                this.warehouses.push(savedWarehouse);
                this.selectedGlobalWarehouse = savedWarehouse;
                this.currentProduct.warehouse = savedWarehouse;
                this.filterLocations(savedWarehouse.warehouseId);
                this.filterFormLocations(savedWarehouse.warehouseId);
                this.closeModal();
            });
        } else if (this.createModalType === 'location') {
            if (!this.modalContextWarehouse) {
                alert('Debe seleccionar un depósito antes de crear una ubicación');
                this.closeModal();
                return;
            }
            if (!this.newItemCode) {
                alert('El código es requerido para la ubicación');
                return;
            }
            const newLocation: StorageLocation = {
                warehouse: this.modalContextWarehouse,
                locationCode: this.newItemCode,
                locationDescription: this.newItemDescription,
                locationType: this.newItemType
            };
            this.stockService.createLocation(newLocation).subscribe(savedLocation => {
                this.locations.push(savedLocation);
                this.filterLocations(this.selectedGlobalWarehouse?.warehouseId);
                this.filterFormLocations(this.currentProduct.warehouse?.warehouseId);


                if (this.selectedGlobalWarehouse?.warehouseId === this.modalContextWarehouse?.warehouseId) {
                    this.selectedGlobalLocation = savedLocation;
                    this.filterProducts();
                }


                if (this.currentProduct.warehouse?.warehouseId === this.modalContextWarehouse?.warehouseId) {
                    this.currentProduct.storage = savedLocation;
                }

                this.closeModal();
            });
        }
    }



    openPublishModal(product: Product) {
        this.publishProduct_ = product;
        this.publishError = null;
        this.publishSuccess = null;
        this.publishLoading = false;
        this.publishFields = {};
        this.mlCategories = [];
        this.categorySearchTerm = '';
        this.showCategoryDropdown = false;
        this.publishStep = 1;
        this.mlAttributes = [];
        this.mlAttributesValues = {};


        if (product.tiendaNubeId && !product.mercadoLibreId) {
            this.publishChannel = 'tiendanube';
        } else if (product.mercadoLibreId && !product.tiendaNubeId) {
            this.publishChannel = 'mercadolibre';
        } else {
            this.publishChannel = 'tiendanube';
        }


        if (product.mercadoLibreId) {
            this.publishChannel = 'mercadolibre';
            this.publishLoading = true;
            this.mercadoLibreService.getPublishedProduct(product.productId!).subscribe({
                next: (mlData: any) => {
                    this.publishLoading = false;

                    this.publishFields = {
                        name: { es: product.productName || '' },
                        title: mlData.title || product.productName,
                        description: '',
                        imageUrl: '',
                        pictureUrls: mlData.pictures && mlData.pictures.length > 0
                            ? mlData.pictures.map((p: any) => p.url).join(', ')
                            : '',
                        condition: mlData.condition || 'new',
                        category_id: mlData.category_id || '',
                        listing_type_id: mlData.listing_type_id || 'gold_special',
                        pictures: [],
                        price: mlData.price || product.price,
                        available_quantity: mlData.available_quantity
                    };

                    if (mlData.category_id) {
                        this.categorySearchTerm = mlData.category_id;
                        this.mercadoLibreService.searchCategories(mlData.category_id).subscribe(cats => {
                            if (cats && cats.length > 0) {
                                const cat = cats[0];
                                this.categorySearchTerm = cat.path_from_root || cat.category_name;
                                this.selectMLCategory(cat);
                                if (mlData.attributes) {
                                    mlData.attributes.forEach((attr: any) => {
                                        this.mlAttributesValues[attr.id] = attr.value_name;
                                    });
                                }
                            }
                        });
                    }
                },
                error: (err) => {
                    this.publishLoading = false;
                    console.error('Error fetching ML data:', err);
                    this.setupDefaultPublishFields(product);
                }
            });

        } else if (product.tiendaNubeId) {
            this.publishChannel = 'tiendanube';
            this.setupDefaultPublishFields(product);
        } else {
            this.publishChannel = this.mlConnected ? 'mercadolibre' : (this.tnConnected ? 'tiendanube' : 'mercadolibre');
            this.setupDefaultPublishFields(product);
        }


        this.tiendaNubeService.getConnectionStatus().subscribe(
            (status: any) => this.tnConnected = status?.connected === true,
            () => this.tnConnected = false
        );
        this.mercadoLibreService.getConnectionStatus().subscribe(
            (status: any) => {
                console.log('ML Status:', status);
                this.mlConnected = status?.connected === true;
            },
            (err) => {
                console.error('ML Status Error:', err);
                this.mlConnected = false;
            }
        );

        this.showPublishModal = true;
    }

    setupDefaultPublishFields(product: Product) {
        this.publishFields = {
            name: { es: product.productName || '' },
            title: product.productName || '',
            description: '',
            imageUrl: '',
            pictureUrls: '',
            category_id: '',
            condition: 'new',
            listing_type_id: 'gold_special',
            pictures: [],
            price: product.price,
            available_quantity: product.stockQuantity
        };
    }

    closePublishModal() {
        this.showPublishModal = false;
        this.publishProduct_ = null;
    }

    isEditForCurrentChannel(): boolean {
        if (!this.publishProduct_) return false;
        if (this.publishChannel === 'tiendanube') return !!this.publishProduct_.tiendaNubeId;
        if (this.publishChannel === 'mercadolibre') return !!this.publishProduct_.mercadoLibreId;
        return false;
    }


    searchMLCategories() {
        if (this.categorySearchTimeout) clearTimeout(this.categorySearchTimeout);
        const term = this.categorySearchTerm?.trim();

        if (!term || term.length < 3) {
            this.mlCategories = [];
            this.showCategoryDropdown = false;
            return;
        }

        this.publishLoading = true;
        console.log('Searching categories for:', term);

        this.categorySearchTimeout = setTimeout(() => {
            this.mercadoLibreService.searchCategories(term).subscribe({
                next: (cats) => {
                    console.log('Categories found:', cats);
                    this.mlCategories = cats;
                    this.showCategoryDropdown = cats.length > 0;
                    this.publishLoading = false;
                },
                error: (err) => {
                    console.error('Error searching categories:', err);
                    this.mlCategories = [];
                    this.showCategoryDropdown = false;
                    this.publishLoading = false;
                }
            });
        }, 400);
    }


    showCategoryBrowser = false;
    browseCategories: any[] = [];
    browseBreadcrumbs: any[] = [];
    browseLoading = false;

    openCategoryBrowser() {
        this.showCategoryBrowser = true;
        this.browseBreadcrumbs = [{ id: 'root', name: 'Categorías' }];
        this.loadBrowseCategories('root');
    }

    closeCategoryBrowser() {
        this.showCategoryBrowser = false;
        this.browseCategories = [];
        this.browseBreadcrumbs = [];
    }

    loadBrowseCategories(parentId?: string) {
        this.browseLoading = true;
        const idToLoad = parentId === 'root' ? undefined : parentId;
        this.mercadoLibreService.getCategories(idToLoad).subscribe({
            next: (cats) => {
                this.browseCategories = cats;
                this.browseLoading = false;



            },
            error: (err) => {
                console.error('Error loading categories', err);
                this.browseLoading = false;
            }
        });
    }

    onBrowseCategoryClick(cat: any) {
        this.browseBreadcrumbs.push({ id: cat.id, name: cat.name });
        this.loadBrowseCategories(cat.id);
    }

    onBreadcrumbClick(index: number) {
        if (index === this.browseBreadcrumbs.length - 1) return;
        const item = this.browseBreadcrumbs[index];
        this.browseBreadcrumbs = this.browseBreadcrumbs.slice(0, index + 1);
        this.loadBrowseCategories(item.id);
    }

    selectBrowseCategory(cat: any) {






        this.publishLoading = true;
        this.mercadoLibreService.getCategories(cat.id).subscribe(children => {




            if (children && children.length > 0) {
                this.onBrowseCategoryClick(cat);
                this.publishLoading = false;
            } else {


                this.mercadoLibreService.searchCategories(cat.id).subscribe(fullCats => {
                    this.publishLoading = false;
                    if (fullCats && fullCats.length > 0) {
                        this.selectMLCategory(fullCats[0]);
                        this.closeCategoryBrowser();
                    } else {

                        this.selectMLCategory({
                            category_id: cat.id,
                            category_name: cat.name,
                            domain_name: cat.name
                        });
                        this.closeCategoryBrowser();
                    }
                });
            }
        });
    }

    selectMLCategory(cat: any) {
        this.publishFields.category_id = cat.category_id;
        this.categorySearchTerm = cat.category_name + ' (' + cat.domain_name + ')';
        this.showCategoryDropdown = false;
        this.fetchListingTypes(cat.category_id);


        this.mlAttributes = [];
        this.mlAttributesValues = {};
    }

    fetchListingTypes(categoryId: string) {
        this.mercadoLibreService.getListingTypes(categoryId).subscribe(types => {
            this.mlListingTypes = types;

            if (types.some(t => t.id === 'gold_special')) {
                this.publishFields.listing_type_id = 'gold_special';
            } else if (types.length > 0) {
                this.publishFields.listing_type_id = types[0].id;
            }
        });
    }

    nextStep() {
        if (this.publishStep === 1) {
            const catId = this.publishFields.category_id;
            if (!catId) {
                this.publishError = "Debe seleccionar una categoría";
                return;
            }
            this.publishLoading = true;
            this.mercadoLibreService.getCategoryAttributes(catId).subscribe(attrs => {
                this.mlAttributes = attrs.filter(a => a.tags && (a.tags.required || a.tags.allow_variations));




                this.prefillAttributes();

                this.publishLoading = false;
                this.publishStep = 2;
                this.publishError = null;
            }, err => {
                this.publishLoading = false;
                this.publishError = "Error al obtener atributos de la categoría";
            });
        }
    }

    prevStep() {
        this.publishStep = 1;
        this.publishError = null;
    }

    prefillAttributes() {

        this.mlAttributes.forEach(attr => {
            if (attr.id === 'BRAND' && this.publishProduct_?.productBrand) {
                this.mlAttributesValues['BRAND'] = this.publishProduct_.productBrand.brandName;
            }
            if (attr.id === 'GTIN' || attr.id === 'EAN') {

            }

        });
    }

    validateAttributes(): boolean {
        for (const attr of this.mlAttributes) {
            if (attr.tags?.required) {
                const val = this.mlAttributesValues[attr.id];
                if (!val || (typeof val === 'string' && val.trim() === '')) {
                    this.publishError = `El atributo ${attr.name} es requerido`;
                    return false;
                }
            }
        }
        return true;
    }

    submitPublish() {
        this.publishError = null;
        this.publishSuccess = null;

        if (this.publishChannel === 'mercadolibre') {
            if (this.publishStep === 2) {
                if (!this.validateAttributes()) return;

                this.publishFields.attributes = Object.keys(this.mlAttributesValues).map(key => ({
                    id: key,
                    value_name: this.mlAttributesValues[key]
                }));
            } else if (this.publishStep === 1) {



            }

            this.publishLoading = true;


            const payload = { ...this.publishFields };


            if (payload.pictureUrls && typeof payload.pictureUrls === 'string' && payload.pictureUrls.trim() !== '') {
                payload.pictures = payload.pictureUrls.split(',').map((url: string) => url.trim()).filter((url: string) => url !== '');
            } else {
                payload.pictures = [];
            }

            if (this.isEditForCurrentChannel()) {
                this.mercadoLibreService.updatePublishedProduct(this.publishProduct_!.productId!, payload)
                    .subscribe({
                        next: () => {
                            this.publishLoading = false;
                            this.publishSuccess = 'Publicación actualizada correctamente';

                            if (payload.title) this.publishProduct_!.productName = payload.title;
                            if (payload.price) this.publishProduct_!.price = payload.price;
                            if (payload.available_quantity) this.publishProduct_!.stockQuantity = payload.available_quantity;

                            setTimeout(() => this.closePublishModal(), 1500);
                        },
                        error: (err) => {
                            this.publishLoading = false;
                            this.publishError = err.message || 'Error al actualizar';
                        }
                    });
            } else {
                this.mercadoLibreService.publishProduct(this.publishProduct_!.productId!, payload)
                    .subscribe({
                        next: () => {
                            this.publishLoading = false;
                            this.publishSuccess = 'Producto publicado correctamente';

                            if (payload.title) this.publishProduct_!.productName = payload.title;
                            if (payload.price) this.publishProduct_!.price = payload.price;
                            if (payload.available_quantity) this.publishProduct_!.stockQuantity = payload.available_quantity;

                            setTimeout(() => {
                                this.closePublishModal();
                                this.loadData();
                            }, 1500);
                        },
                        error: (err) => {
                            this.publishLoading = false;
                            this.publishError = err.message || 'Error al publicar';
                        }
                    });
            }
        } else {

            alert('Implementación de TiendaNube pendiente de refactor');
        }
    }
}

