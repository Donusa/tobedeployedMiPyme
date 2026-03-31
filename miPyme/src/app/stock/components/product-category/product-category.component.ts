import { Component, OnInit } from '@angular/core';
import { StockService } from '../../../services/stock.service';
import { ProductCategory } from '../../../models/stock.models';

@Component({
  selector: 'app-product-category',
  templateUrl: './product-category.component.html',
  styleUrls: ['./product-category.component.css']
})
export class ProductCategoryComponent implements OnInit {
  categories: ProductCategory[] = [];
  currentCategory: ProductCategory = { categoryName: '' };
  isEditing = false;

  constructor(private stockService: StockService) { }

  ngOnInit(): void {
    this.loadCategories();
  }

  loadCategories() {
    this.stockService.getCategories().subscribe(data => this.categories = data);
  }

  save() {

    if (this.currentCategory.parentCategory && this.currentCategory.parentCategory.categoryId === this.currentCategory.categoryId) {
        alert('Una categoría no puede ser padre de sí misma');
        return;
    }

    if (this.isEditing && this.currentCategory.categoryId) {
      this.stockService.updateCategory(this.currentCategory.categoryId, this.currentCategory).subscribe(() => {
        this.loadCategories();
        this.resetForm();
      });
    } else {
      this.stockService.createCategory(this.currentCategory).subscribe(() => {
        this.loadCategories();
        this.resetForm();
      });
    }
  }

  edit(category: ProductCategory) {
    this.currentCategory = { ...category };

    if (this.currentCategory.parentCategory) {
        const parent = this.categories.find(c => c.categoryId === this.currentCategory.parentCategory?.categoryId);
        if (parent) {
            this.currentCategory.parentCategory = parent;
        }
    }
    this.isEditing = true;
  }

  delete(id: number) {
    if(confirm('¿Está seguro de eliminar esta categoría?')) {
        this.stockService.deleteCategory(id).subscribe(() => this.loadCategories());
    }
  }

  resetForm() {
    this.currentCategory = { categoryName: '' };
    this.isEditing = false;
  }
}
