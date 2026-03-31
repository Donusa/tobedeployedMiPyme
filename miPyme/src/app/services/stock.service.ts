import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { MeasurementUnit, ProductBrand, ProductCategory, ProductVariant, StorageLocation, Warehouse, Product } from '../models/stock.models';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class StockService {
  private apiUrl = `${environment.apiUrl}/api/stock`;

  constructor(private http: HttpClient) { }

  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });
  }


  getMeasurementUnits(): Observable<MeasurementUnit[]> {
    return this.http.get<MeasurementUnit[]>(`${this.apiUrl}/units`, { headers: this.getHeaders() });
  }

  createMeasurementUnit(unit: MeasurementUnit): Observable<MeasurementUnit> {
    return this.http.post<MeasurementUnit>(`${this.apiUrl}/units`, unit, { headers: this.getHeaders() });
  }

  updateMeasurementUnit(id: string, unit: MeasurementUnit): Observable<MeasurementUnit> {
    return this.http.put<MeasurementUnit>(`${this.apiUrl}/units/${id}`, unit, { headers: this.getHeaders() });
  }

  deleteMeasurementUnit(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/units/${id}`, { headers: this.getHeaders() });
  }


  getBrands(): Observable<ProductBrand[]> {
    return this.http.get<ProductBrand[]>(`${this.apiUrl}/brands`, { headers: this.getHeaders() });
  }

  createBrand(brand: ProductBrand): Observable<ProductBrand> {
    return this.http.post<ProductBrand>(`${this.apiUrl}/brands`, brand, { headers: this.getHeaders() });
  }

  updateBrand(id: number, brand: ProductBrand): Observable<ProductBrand> {
    return this.http.put<ProductBrand>(`${this.apiUrl}/brands/${id}`, brand, { headers: this.getHeaders() });
  }

  deleteBrand(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/brands/${id}`, { headers: this.getHeaders() });
  }


  getCategories(): Observable<ProductCategory[]> {
    return this.http.get<ProductCategory[]>(`${this.apiUrl}/categories`, { headers: this.getHeaders() });
  }

  createCategory(category: ProductCategory): Observable<ProductCategory> {
    return this.http.post<ProductCategory>(`${this.apiUrl}/categories`, category, { headers: this.getHeaders() });
  }

  updateCategory(id: number, category: ProductCategory): Observable<ProductCategory> {
    return this.http.put<ProductCategory>(`${this.apiUrl}/categories/${id}`, category, { headers: this.getHeaders() });
  }

  deleteCategory(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/categories/${id}`, { headers: this.getHeaders() });
  }


  getWarehouses(): Observable<Warehouse[]> {
    return this.http.get<Warehouse[]>(`${this.apiUrl}/warehouses`, { headers: this.getHeaders() });
  }

  createWarehouse(warehouse: Warehouse): Observable<Warehouse> {
    return this.http.post<Warehouse>(`${this.apiUrl}/warehouses`, warehouse, { headers: this.getHeaders() });
  }

  updateWarehouse(id: number, warehouse: Warehouse): Observable<Warehouse> {
    return this.http.put<Warehouse>(`${this.apiUrl}/warehouses/${id}`, warehouse, { headers: this.getHeaders() });
  }

  deleteWarehouse(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/warehouses/${id}`, { headers: this.getHeaders() });
  }


  getLocations(): Observable<StorageLocation[]> {
    return this.http.get<StorageLocation[]>(`${this.apiUrl}/locations`, { headers: this.getHeaders() });
  }

  createLocation(location: StorageLocation): Observable<StorageLocation> {
    return this.http.post<StorageLocation>(`${this.apiUrl}/locations`, location, { headers: this.getHeaders() });
  }

  updateLocation(id: number, location: StorageLocation): Observable<StorageLocation> {
    return this.http.put<StorageLocation>(`${this.apiUrl}/locations/${id}`, location, { headers: this.getHeaders() });
  }

  deleteLocation(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/locations/${id}`, { headers: this.getHeaders() });
  }


  getProducts(): Observable<Product[]> {
    return this.http.get<Product[]>(`${this.apiUrl}/products`, { headers: this.getHeaders() });
  }

  getProductById(id: number): Observable<Product> {
    return this.http.get<Product>(`${this.apiUrl}/products/${id}`, { headers: this.getHeaders() });
  }

  scanProduct(code: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/products/scan?code=${code}`, { headers: this.getHeaders() });
  }

  getMetricConflicts(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/metrics/conflicts`, { headers: this.getHeaders() });
  }

  createProduct(product: Product): Observable<Product> {
    return this.http.post<Product>(`${this.apiUrl}/products`, product, { headers: this.getHeaders() });
  }

  updateProduct(id: number, product: Product): Observable<Product> {
    return this.http.put<Product>(`${this.apiUrl}/products/${id}`, product, { headers: this.getHeaders() });
  }

  updateProductStock(id: number, quantity: number): Observable<Product> {
    return this.http.patch<Product>(`${this.apiUrl}/products/${id}/stock`, { stockQuantity: quantity }, { headers: this.getHeaders() });
  }

  deleteProduct(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/products/${id}`, { headers: this.getHeaders() });
  }


  getAllVariants(): Observable<ProductVariant[]> {
    return this.http.get<ProductVariant[]>(`${this.apiUrl}/variants`, { headers: this.getHeaders() });
  }

  getVariants(productId: number): Observable<ProductVariant[]> {
    return this.http.get<ProductVariant[]>(`${this.apiUrl}/products/${productId}/variants`, { headers: this.getHeaders() });
  }

  createVariant(productId: number, variant: ProductVariant): Observable<ProductVariant> {
    return this.http.post<ProductVariant>(`${this.apiUrl}/products/${productId}/variants`, variant, { headers: this.getHeaders() });
  }

  updateVariant(id: number, variant: ProductVariant): Observable<ProductVariant> {
    return this.http.put<ProductVariant>(`${this.apiUrl}/variants/${id}`, variant, { headers: this.getHeaders() });
  }

  updateVariantStock(id: number, quantity: number): Observable<ProductVariant> {
    return this.http.patch<ProductVariant>(`${this.apiUrl}/variants/${id}/stock`, { stockQuantity: quantity }, { headers: this.getHeaders() });
  }

  deleteVariant(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/variants/${id}`, { headers: this.getHeaders() });
  }
}
