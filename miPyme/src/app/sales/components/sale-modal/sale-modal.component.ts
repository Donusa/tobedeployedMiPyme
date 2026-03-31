import { Component, EventEmitter, Output, OnInit, ViewChild } from '@angular/core';
import { StockService } from '../../../services/stock.service';
import { SalesService } from '../../services/sales.service';
import { Product, ProductVariant } from '../../../models/stock.models';
import { CreateSaleRequest, Sale } from '../../models/sale.model';
import { BarcodeFormat } from '@zxing/library';

@Component({
  selector: 'app-sale-modal',
  templateUrl: './sale-modal.component.html',
  styleUrls: ['./sale-modal.component.css']
})
export class SaleModalComponent implements OnInit {
  @Output() close = new EventEmitter<void>();
  @Output() saleCompleted = new EventEmitter<Sale>();

  products: Product[] = [];
  searchTerm: string = '';
  filteredProducts: Product[] = [];

  cartItems: any[] = [];


  selectedMedioPago: string = 'EFECTIVO';
  readonly mediosPago = [
    { value: 'EFECTIVO', label: 'Efectivo' },
    { value: 'DEBITO', label: 'Débito' },
    { value: 'CREDITO', label: 'Crédito' },
    { value: 'TRANSFERENCIA', label: 'Transferencia' },
    { value: 'QR', label: 'QR' }
  ];


  selectedProduct: Product | null = null;
  variants: ProductVariant[] = [];
  selectedVariant: ProductVariant | null = null;
  quantity: number = 1;

  loadingVariants = false;
  showDropdown = false;


  showScanner = false;
  allowedFormats = [
    BarcodeFormat.QR_CODE,
    BarcodeFormat.EAN_13,
    BarcodeFormat.EAN_8,
    BarcodeFormat.CODE_128,
    BarcodeFormat.CODE_39,
    BarcodeFormat.ITF,
    BarcodeFormat.UPC_A,
    BarcodeFormat.UPC_E
  ];
  availableDevices: MediaDeviceInfo[] = [];
  currentDevice: MediaDeviceInfo | undefined;
  hasPermission: boolean = false;
  scannerEnabled: boolean = false;
  lastScanTime: number = 0;

  constructor(private stockService: StockService, private salesService: SalesService) {}

  ngOnInit() {
    this.stockService.getProducts().subscribe(products => {
      this.products = products;
      this.filteredProducts = products;
    });
  }


  toggleScanner() {
    this.showScanner = !this.showScanner;
    if (this.showScanner) {
      this.scannerEnabled = true;
      this.checkCameraPermission();
    } else {
      this.scannerEnabled = false;
    }
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

  onCamerasFound(devices: MediaDeviceInfo[]) {
    this.availableDevices = devices;
    if (devices.length > 0 && !this.currentDevice) {

        const backCamera = devices.find(d => d.label.toLowerCase().includes('back') || d.label.toLowerCase().includes('trasera'));
        this.currentDevice = backCamera || devices[0];
    }
  }

  onDeviceSelectChange(deviceId: string) {
    const device = this.availableDevices.find(d => d.deviceId === deviceId);
    if (device) {
      this.currentDevice = device;
    }
  }

  onPermissionResponse(hasPermission: boolean) {
    this.hasPermission = hasPermission;
    if (!hasPermission) {
      this.scannerEnabled = false;
    }
  }

  handleScanSuccess(resultStr: string) {
    if (!resultStr) return;

    const now = Date.now();
    if (now - this.lastScanTime < 2000) return;
    this.lastScanTime = now;

    console.log('Scanned:', resultStr);

    this.stockService.scanProduct(resultStr).subscribe({
      next: (response) => {
        this.playBeep();
        if (response.type === 'PRODUCT') {
            if (response.hasVariants) {
                this.selectProduct(response.product);
            } else {
                this.selectedProduct = response.product;
                this.selectedVariant = null;
                this.quantity = 1;
                this.addToCart();
            }
        } else if (response.type === 'VARIANT') {
            this.selectedProduct = response.product;
            this.selectedVariant = response.variant;
            this.quantity = 1;
            this.addToCart();
        }
      },
      error: (err) => {
        console.error('Scan error:', err);
        if (err.status === 409) {
             alert(err.error.message || 'Error: Conflicto de duplicados en métricas. No se puede escanear.');
        } else if (err.status === 404) {
             console.warn('Producto no encontrado');
        } else {

        }
      }
    });
  }

  processScannedProduct(product: Product) {

    this.selectProduct(product);
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

  onSearchFocus() {
    this.showDropdown = true;
    this.filterProducts();
  }

  onSearchBlur() {

    setTimeout(() => {
      this.showDropdown = false;
    }, 200);
  }

  filterProducts() {
    if (!this.searchTerm) {
      this.filteredProducts = this.products;
    } else {
      const term = this.searchTerm.toLowerCase();
      this.filteredProducts = this.products.filter(p =>
        p.productName.toLowerCase().includes(term) ||
        (p.internalCode && p.internalCode.toLowerCase().includes(term))
      );
    }
  }

  selectProduct(product: Product) {
    this.selectedProduct = product;
    this.variants = [];
    this.selectedVariant = null;
    this.loadingVariants = true;

    this.stockService.getVariants(product.productId!).subscribe(variants => {
        this.variants = variants;
        this.loadingVariants = false;
    });
  }

  clearSelection() {
      this.selectedProduct = null;
      this.variants = [];
      this.selectedVariant = null;
      this.quantity = 1;
  }

  addToCart() {
    if (!this.selectedProduct) return;


    if (this.variants.length > 0 && !this.selectedVariant) {
        alert('Seleccione una variante');
        return;
    }

    const stock = this.selectedVariant ? this.selectedVariant.stockQuantity : this.selectedProduct.stockQuantity;
    if (stock === undefined || stock < this.quantity) {
        alert('Stock insuficiente');
        return;
    }

    const price = this.selectedVariant?.price ?? this.selectedProduct.price ?? 0;

    this.cartItems.push({
        product: this.selectedProduct,
        variant: this.selectedVariant,
        quantity: this.quantity,
        price: price,
        subtotal: price * this.quantity
    });


    this.clearSelection();
    this.searchTerm = '';
    this.filterProducts();
  }

  removeFromCart(index: number) {
      this.cartItems.splice(index, 1);
  }

  get total() {
      return this.cartItems.reduce((acc, item) => acc + item.subtotal, 0);
  }

  submitSale() {
      if (this.cartItems.length === 0) return;

      const request: CreateSaleRequest = {
          items: this.cartItems.map(item => ({
              productId: item.variant ? undefined : item.product.productId,
              productVariantId: item.variant ? item.variant.productVariantId : undefined,
              quantity: item.quantity
          })),
          medioPago: this.selectedMedioPago
      };

      this.salesService.createSale(request).subscribe({
          next: (response) => {
              this.saleCompleted.emit(response);
          },
          error: (err) => {
              console.error(err);
              alert('Error al crear la venta. Verifique stock.');
          }
      });
  }
}
