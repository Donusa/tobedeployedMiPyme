import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Offer } from '../../../services/offer.service';
import { StockService } from '../../../services/stock.service';
import { ProductCategory, ProductBrand, Warehouse, StorageLocation, Product } from '../../../models/stock.models';

@Component({
  selector: 'app-offer-form',
  templateUrl: './offer-form.component.html',
  styleUrls: ['./offer-form.component.css']
})
export class OfferFormComponent implements OnInit {
  @Input() offer: Offer | null = null;
  @Output() close = new EventEmitter<void>();
  @Output() save = new EventEmitter<Offer>();

  form: FormGroup;
  targetOptions: any[] = [];

  constructor(
    private fb: FormBuilder,
    private stockService: StockService
  ) {
    this.form = this.fb.group({
      offerId: [null],
      name: ['', Validators.required],
      indefinite: [false],
      startDate: [''],
      endDate: [''],
      targetType: ['CATEGORY', Validators.required],
      targetIds: [[], Validators.required],
      discountType: ['PERCENTAGE', Validators.required],
      discountValue: [0, [Validators.required, Validators.min(0)]],
      buyQuantity: [null],
      payQuantity: [null]
    });
  }

  ngOnInit(): void {
    if (this.offer) {
      this.form.patchValue(this.offer);
    }
    this.loadTargetOptions();
    this.updateValidators(this.form.get('discountType')?.value);

    this.form.get('targetType')?.valueChanges.subscribe(() => {
      this.loadTargetOptions();
      this.form.patchValue({ targetIds: [] });
    });

    this.form.get('discountType')?.valueChanges.subscribe(val => {
      this.updateValidators(val);
    });
  }

  updateValidators(type: string) {
    const valueControl = this.form.get('discountValue');
    const buyControl = this.form.get('buyQuantity');
    const payControl = this.form.get('payQuantity');

    if (type === 'X_FOR_Y') {
        valueControl?.clearValidators();
        buyControl?.setValidators([Validators.required, Validators.min(1)]);
        payControl?.setValidators([Validators.required, Validators.min(1)]);
    } else {
        valueControl?.setValidators([Validators.required, Validators.min(0)]);
        buyControl?.clearValidators();
        payControl?.clearValidators();
    }
    valueControl?.updateValueAndValidity();
    buyControl?.updateValueAndValidity();
    payControl?.updateValueAndValidity();
  }

  loadTargetOptions() {
    const type = this.form.get('targetType')?.value;
    this.targetOptions = [];

    if (type === 'CATEGORY') {
      this.stockService.getCategories().subscribe((data: ProductCategory[]) => this.targetOptions = data.map((c: ProductCategory) => ({ id: c.categoryId, name: c.categoryName })));
    } else if (type === 'BRAND') {
      this.stockService.getBrands().subscribe((data: ProductBrand[]) => this.targetOptions = data.map((b: ProductBrand) => ({ id: b.brandId, name: b.brandName })));
    } else if (type === 'WAREHOUSE') {
       this.stockService.getWarehouses().subscribe((data: Warehouse[]) => this.targetOptions = data.map((w: Warehouse) => ({ id: w.warehouseId, name: w.warehouseName })));
    } else if (type === 'LOCATION') {
       this.stockService.getLocations().subscribe((data: StorageLocation[]) => this.targetOptions = data.map((l: StorageLocation) => ({ id: l.storageLocationId, name: l.locationDescription || l.locationCode })));
    }
    else if (type === 'SPECIFIC') {
        this.stockService.getProducts().subscribe((data: Product[]) => this.targetOptions = data.map((p: Product) => ({ id: p.productId, name: p.productName })));
    }
  }

  onSubmit() {
    if (this.form.valid) {
      this.save.emit(this.form.value);
    }
  }
}
