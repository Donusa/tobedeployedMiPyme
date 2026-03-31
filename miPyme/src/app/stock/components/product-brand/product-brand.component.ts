import { Component, OnInit } from '@angular/core';
import { StockService } from '../../../services/stock.service';
import { ProductBrand } from '../../../models/stock.models';

@Component({
  selector: 'app-product-brand',
  templateUrl: './product-brand.component.html',
  styleUrls: ['./product-brand.component.css']
})
export class ProductBrandComponent implements OnInit {
  brands: ProductBrand[] = [];
  currentBrand: ProductBrand = { brandName: '' };
  isEditing = false;

  constructor(private stockService: StockService) { }

  ngOnInit(): void {
    this.loadBrands();
  }

  loadBrands() {
    this.stockService.getBrands().subscribe(data => this.brands = data);
  }

  save() {
    if (this.isEditing && this.currentBrand.brandId) {
      this.stockService.updateBrand(this.currentBrand.brandId, this.currentBrand).subscribe(() => {
        this.loadBrands();
        this.resetForm();
      });
    } else {
      this.stockService.createBrand(this.currentBrand).subscribe(() => {
        this.loadBrands();
        this.resetForm();
      });
    }
  }

  edit(brand: ProductBrand) {
    this.currentBrand = { ...brand };
    this.isEditing = true;
  }

  delete(id: number) {
    if(confirm('¿Está seguro de eliminar esta marca?')) {
        this.stockService.deleteBrand(id).subscribe(() => this.loadBrands());
    }
  }

  resetForm() {
    this.currentBrand = { brandName: '' };
    this.isEditing = false;
  }
}
