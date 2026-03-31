import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { SalesService } from '../../../sales/services/sales.service';
import { Sale } from '../../../sales/models/sale.model';
import { ArcaService, InvoiceResponse } from '../../../services/arca.service';
import { ArcaService as ArcaConfigService, ArcaConfig } from '../../../services/arca/arca.service';
import Swal from 'sweetalert2';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-factura-detalle',
  templateUrl: './factura-detalle.component.html',
  styleUrls: ['./factura-detalle.component.css']
})
export class FacturaDetalleComponent implements OnInit {
  invoiceForm: FormGroup;
  isLoading = false;
  saleId: number | null = null;
  sale: Sale | null = null;


  ivaIncluded = false;


  modalStep: 'closed' | 'preview' | 'authorized' = 'closed';
  previewData: any = null;
  isEmitting = false;
  lastInvoiceResponse: InvoiceResponse | null = null;
  nextCbteNro = 0;


  authorizedData: InvoiceResponse | null = null;
  qrCodeUrl = '';


  companyConfig: ArcaConfig = {};


  ptosVenta: any[] = [];
  cbteTipos: any[] = [];
  docTipos: any[] = [];
  condicionIva: any[] = [];
  monedas: any[] = [];
  ivas: any[] = [];
  tributosList: any[] = [];
  conceptTypes = [
    { id: 1, description: 'Productos' },
    { id: 2, description: 'Servicios' },
    { id: 3, description: 'Productos y Servicios' }
  ];

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private salesService: SalesService,
    private arcaService: ArcaService,
    private arcaConfigService: ArcaConfigService
  ) {
    const today = new Date().toISOString().substring(0, 10);

    this.invoiceForm = this.fb.group({
      fecha: [today, Validators.required],
      puntoVenta: [1, Validators.required],
      tipoComprobante: [6, Validators.required],
      concepto: [1, Validators.required],


      fchServDesde: [''],
      fchServHasta: [''],
      fchVtoPago: [''],


      docTipo: [99, Validators.required],
      docNro: [''],
      razonSocial: ['', Validators.required],
      email: ['', [Validators.email]],
      domicilio: [''],
      condicionIvaReceptor: [5],


      monId: ['PES', Validators.required],
      monCotiz: [1, Validators.required],

      items: this.fb.array([]),
      tributos: this.fb.array([])
    });
  }

  ngOnInit(): void {
    this.loadCatalogs();
    this.loadCompanyConfig();
    this.setupReactiveRules();

    this.route.queryParams.subscribe(params => {
      if (params['saleId']) {
        this.saleId = +params['saleId'];
        this.loadSaleData(this.saleId);
      } else {
        this.addItem();
      }
    });
  }

  loadCatalogs() {
    this.isLoading = true;
    forkJoin({
      ptosVenta: this.arcaService.getParam('ptos-venta'),
      cbteTipos: this.arcaService.getParam('cbte-tipos'),
      docTipos: this.arcaService.getParam('doc-tipos'),
      ivas: this.arcaService.getParam('iva'),
      monedas: this.arcaService.getParam('monedas'),
      tributos: this.arcaService.getParam('tributos')

    }).subscribe({
      next: (res) => {
        this.ptosVenta = res.ptosVenta;
        if (this.ptosVenta.length === 0) {
            this.ptosVenta = [{ id: 1, description: 'Punto de Venta 1 (Simulado)' }];
        }

        this.cbteTipos = res.cbteTipos;
        this.docTipos = res.docTipos;
        this.ivas = res.ivas;
        this.monedas = res.monedas;
        this.tributosList = res.tributos;


        this.isLoading = false;


        this.checkLastVoucher();
      },
      error: (err) => {
        console.error('Error loading catalogs', err);
        this.isLoading = false;
        Swal.fire('Error', 'Error cargando catálogos de AFIP. Se usarán valores por defecto.', 'warning');

        this.ptosVenta = [{ id: 1, description: '1' }];
        this.cbteTipos = [{ id: 6, description: 'Factura B' }];
        this.docTipos = [{ id: 99, description: 'Consumidor Final' }];
        this.monedas = [{ id: 'PES', description: 'Pesos Argentinos' }];
        this.ivas = [{ id: 5, description: '21%' }];
      }
    });
  }

  loadCompanyConfig() {
    this.arcaConfigService.getArcaConfig().subscribe({
      next: (config) => { this.companyConfig = config; },
      error: () => { console.warn('No se pudo cargar config ARCA'); }
    });
  }

  setupReactiveRules() {

    this.invoiceForm.get('concepto')?.valueChanges.subscribe(val => {
      const isService = val == 2 || val == 3;
      const fchDesde = this.invoiceForm.get('fchServDesde');
      const fchHasta = this.invoiceForm.get('fchServHasta');
      const fchVto = this.invoiceForm.get('fchVtoPago');

      if (isService) {
        fchDesde?.setValidators(Validators.required);
        fchHasta?.setValidators(Validators.required);
        fchVto?.setValidators(Validators.required);
      } else {
        fchDesde?.clearValidators();
        fchHasta?.clearValidators();
        fchVto?.clearValidators();
        fchDesde?.setValue('');
        fchHasta?.setValue('');
        fchVto?.setValue('');
      }
      fchDesde?.updateValueAndValidity();
      fchHasta?.updateValueAndValidity();
      fchVto?.updateValueAndValidity();
    });


    this.invoiceForm.get('monId')?.valueChanges.subscribe(val => {
      if (val && val !== 'PES') {
        this.arcaService.getCotizacion(val).subscribe(cotiz => {
          this.invoiceForm.get('monCotiz')?.setValue(cotiz);
        });
      } else {
        this.invoiceForm.get('monCotiz')?.setValue(1);
      }
    });


    this.invoiceForm.get('puntoVenta')?.valueChanges.subscribe(() => this.checkLastVoucher());
    this.invoiceForm.get('tipoComprobante')?.valueChanges.subscribe(() => this.checkLastVoucher());
  }

  checkLastVoucher() {
    const pto = this.invoiceForm.get('puntoVenta')?.value;
    const tipo = this.invoiceForm.get('tipoComprobante')?.value;

    if (pto && tipo) {
      this.arcaService.getLastVoucher(pto, tipo).subscribe({
        next: (nro) => {
          this.nextCbteNro = nro + 1;
          console.log(`Último comprobante autorizado: ${nro} → Próximo: ${this.nextCbteNro}`);
        },
        error: () => {
          this.nextCbteNro = 1;
          console.warn('No se pudo obtener el último comprobante');
        }
      });
    }
  }


  get items(): FormArray {
    return this.invoiceForm.get('items') as FormArray;
  }

  get tributos(): FormArray {
    return this.invoiceForm.get('tributos') as FormArray;
  }


  addItem(itemData?: any) {
    const itemGroup = this.fb.group({
      codigo: [itemData?.codigo || ''],
      descripcion: [itemData?.descripcion || '', Validators.required],
      cantidad: [itemData?.cantidad || 1, [Validators.required, Validators.min(0.01)]],
      precioUnitario: [itemData?.precioUnitario || 0, [Validators.required, Validators.min(0)]],
      ivaId: [itemData?.ivaId || 5, Validators.required],
      subtotal: [{ value: itemData?.subtotal || 0, disabled: true }]
    });

    itemGroup.valueChanges.subscribe(val => {
      const sub = (val.cantidad || 0) * (val.precioUnitario || 0);
      itemGroup.get('subtotal')?.setValue(sub, { emitEvent: false });
    });

    this.items.push(itemGroup);
  }

  removeItem(index: number) {
    this.items.removeAt(index);
  }


  addTributo() {
    const group = this.fb.group({
      id: ['', Validators.required],
      desc: [''],
      baseImp: [0, Validators.required],
      alic: [0, Validators.required],
      importe: [0, Validators.required]
    });



    group.valueChanges.subscribe(val => {
        if (val.baseImp && val.alic) {
            const imp = (val.baseImp * val.alic) / 100;



        }
    });

    this.tributos.push(group);
  }

  removeTributo(index: number) {
    this.tributos.removeAt(index);
  }


  loadSaleData(id: number) {
    this.isLoading = true;
    this.salesService.getAll().subscribe({
      next: (sales) => {
        const sale = sales.find(s => s.saleId === id);
        if (sale) {
          this.sale = sale;
          this.populateFormFromSale(sale);
        }
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
      }
    });
  }

  populateFormFromSale(sale: Sale) {
    this.invoiceForm.patchValue({
      fecha: new Date().toISOString().substring(0, 10),
      concepto: 1
    });

    this.items.clear();
    if (sale.items && sale.items.length > 0) {
      sale.items.forEach(item => {
        this.addItem({
          codigo: item.variantSku || item.internalCode || '',
          descripcion: item.variantSku ?
            `${item.productName || 'Producto'} - ${item.variantSku}` :
            item.productName || 'Ítem de venta',
          cantidad: item.quantity,
          precioUnitario: item.unitPrice,
          subtotal: item.subtotal,
          ivaId: 5
        });
      });
    }
  }


  get subtotalNeto(): number {
    return this.items.controls.reduce((acc, control) => {
      const val = control.value;
      let unitPrice = val.precioUnitario;


      if (this.ivaIncluded) {
          const id = Number(val.ivaId);
          let rate = this.getIvaRate(id);
          unitPrice = unitPrice / (1 + rate);
      }

      return acc + (val.cantidad * unitPrice);
    }, 0);
  }

  get totalIva(): number {
    return this.items.controls.reduce((acc, control) => {
      const val = control.value;
      const id = Number(val.ivaId);
      let rate = this.getIvaRate(id);

      let baseImp = val.cantidad * val.precioUnitario;
      if (this.ivaIncluded) {
          baseImp = baseImp / (1 + rate);
      }

      return acc + (baseImp * rate);
    }, 0);
  }

  getIvaRate(id: number): number {
      switch(id) {
          case 5: return 0.21;
          case 4: return 0.105;
          case 6: return 0.27;
          case 8: return 0.05;
          case 9: return 0.025;
          default: return 0;
      }
  }

  get totalTributos(): number {
      return this.tributos.controls.reduce((acc, control) => {
          return acc + (Number(control.value.importe) || 0);
      }, 0);
  }

  get totalGeneral(): number {

      if (this.ivaIncluded) {
        return this.items.controls.reduce((acc, control) => {
            const val = control.value;
            return acc + (val.cantidad * val.precioUnitario);
        }, 0) + this.totalTributos;
      }

      return this.subtotalNeto + this.totalIva + this.totalTributos;
  }

  toggleIvaIncluded() {
      this.ivaIncluded = !this.ivaIncluded;

  }

  setConsumidorFinal() {
      this.invoiceForm.patchValue({
          tipoComprobante: 6,
          docTipo: 99,
          docNro: '0',
          razonSocial: 'Consumidor Final',
          condicionIvaReceptor: 5
      });
  }


  savePreset() {
      Swal.fire({
          title: 'Guardar Configuración',
          input: 'text',
          inputLabel: 'Nombre de la configuración',
          showCancelButton: true
      }).then((result) => {
          if (result.isConfirmed && result.value) {
              const preset = {
                  name: result.value,
                  data: this.invoiceForm.getRawValue(),
                  ivaIncluded: this.ivaIncluded
              };

              const presets = JSON.parse(localStorage.getItem('invoice_presets') || '[]');
              presets.push(preset);
              localStorage.setItem('invoice_presets', JSON.stringify(presets));
              Swal.fire('Guardado', '', 'success');
          }
      });
  }

  loadPresets() {
      const presets = JSON.parse(localStorage.getItem('invoice_presets') || '[]');
      if (presets.length === 0) {
          Swal.fire('Sin configuraciones', 'No hay configuraciones guardadas', 'info');
          return;
      }

      const options: {[key: string]: string} = {};
      presets.forEach((p: any, index: number) => options[index] = p.name);

      Swal.fire({
          title: 'Cargar Configuración',
          input: 'select',
          inputOptions: options,
          showCancelButton: true
      }).then((result) => {
          if (result.isConfirmed) {
              const selected = presets[result.value];
              this.invoiceForm.patchValue(selected.data);
              this.ivaIncluded = selected.ivaIncluded || false;


          }
      });
  }


  getIvaDesc(id: any): string {
      const found = this.ivas.find(i => i.id == id);
      return found ? found.description : id;
  }

  getDocTipoDesc(id: any): string {
      const found = this.docTipos.find(d => d.id == id);
      return found ? found.description : id;
  }

  getCbteTipoDesc(id: any): string {
      const found = this.cbteTipos.find(c => c.id == id);
      return found ? found.description : id;
  }

  getAppliedIvaRates(items: any[]): any[] {
      const rates: any = {};
      if (items) {
          items.forEach(item => {
              const ivaId = Number(item.ivaId);
              const rate = this.getIvaRate(ivaId);

              if (rate > 0) {
                  if (!rates[ivaId]) {
                      rates[ivaId] = { desc: this.getIvaDesc(ivaId), amount: 0 };
                  }
                  let base = (item.cantidad * item.precioUnitario);
                  if (this.ivaIncluded) {
                      base = base / (1 + rate);
                  }
                  rates[ivaId].amount += base * rate;
              }
          });
      }
      return Object.values(rates);
  }

  onSubmit() {
    if (this.invoiceForm.invalid) {
      this.invoiceForm.markAllAsTouched();
      Swal.fire('Error', 'Por favor complete los campos requeridos', 'warning');
      return;
    }

    if (this.items.length === 0) {
      Swal.fire('Error', 'La factura debe tener al menos un ítem', 'warning');
      return;
    }


    this.previewData = {
        ...this.invoiceForm.getRawValue(),
        subtotalNeto: this.subtotalNeto,
        totalIva: this.totalIva,
        totalTributos: this.totalTributos,
        totalGeneral: this.totalGeneral
    };

    this.modalStep = 'preview';
  }

  confirmEmission() {
    if (this.isEmitting) return;
    this.isEmitting = true;

    const formRaw = this.invoiceForm.getRawValue();


    const mappedItems = formRaw.items.map((item: any) => {
      const ivaId = Number(item.ivaId);
      const rate = this.getIvaRate(ivaId);
      let baseImp = item.cantidad * item.precioUnitario;
      if (this.ivaIncluded) {
        baseImp = baseImp / (1 + rate);
      }
      const ivaImporte = baseImp * rate;
      return {
        codigo: item.codigo,
        descripcion: item.descripcion,
        cantidad: item.cantidad,
        precioUnitario: item.precioUnitario,
        ivaId: ivaId,
        baseImp: Math.round(baseImp * 100) / 100,
        ivaImporte: Math.round(ivaImporte * 100) / 100
      };
    });

    const payload = {
      puntoVenta: Number(formRaw.puntoVenta),
      tipoComprobante: Number(formRaw.tipoComprobante),
      concepto: Number(formRaw.concepto),
      docTipo: Number(formRaw.docTipo),
      docNro: formRaw.docNro || '0',
      condicionIvaReceptor: Number(formRaw.condicionIvaReceptor) || 5,
      fecha: formRaw.fecha,
      fchServDesde: formRaw.fchServDesde || null,
      fchServHasta: formRaw.fchServHasta || null,
      fchVtoPago: formRaw.fchVtoPago || null,
      monId: formRaw.monId,
      monCotiz: Number(formRaw.monCotiz),
      impNeto: Math.round(this.subtotalNeto * 100) / 100,
      impIVA: Math.round(this.totalIva * 100) / 100,
      impTrib: Math.round(this.totalTributos * 100) / 100,
      impTotal: Math.round(this.totalGeneral * 100) / 100,
      impTotConc: 0,
      impOpEx: 0,
      items: mappedItems,
      tributos: formRaw.tributos || [],
      saleId: this.saleId
    };

    this.arcaService.emitInvoice(payload).subscribe({
      next: (resp: InvoiceResponse) => {
        this.isEmitting = false;
        this.lastInvoiceResponse = resp;

        if (resp.success) {

          if (this.saleId) {
            this.salesService.markAsFacturado(this.saleId).subscribe({
              error: (err) => console.warn('No se pudo marcar la venta como facturada:', err)
            });
          }


          this.authorizedData = resp;
          this.generateQrCode(resp);
          this.modalStep = 'authorized';

        } else {

          const erroresHtml = resp.errores && resp.errores.length > 0
            ? '<ul>' + resp.errores.map(e => `<li>${e}</li>`).join('') + '</ul>'
            : '';
          const obsHtml = resp.observaciones && resp.observaciones.length > 0
            ? '<p><strong>Observaciones:</strong></p><ul>' +
              resp.observaciones.map(o => `<li>${o}</li>`).join('') + '</ul>'
            : '';

          Swal.fire({
            icon: 'error',
            title: 'Factura Rechazada por AFIP',
            html: `
              <div style="text-align: left; font-size: 14px;">
                <p>La factura fue rechazada (Resultado: ${resp.resultado})</p>
                ${erroresHtml}
                ${obsHtml}
              </div>
            `
          });
        }
      },
      error: (err) => {
        this.isEmitting = false;
        console.error('Error emitting invoice:', err);

        const errorMsg = err.error?.message || err.message || 'Error desconocido al comunicarse con AFIP';
        Swal.fire({
          icon: 'error',
          title: 'Error de Emisión',
          text: errorMsg
        });
      }
    });
  }

  generateQrCode(resp: InvoiceResponse) {
    try {
      const formRaw = this.invoiceForm.getRawValue();
      const qrData = {
        ver: 1,
        fecha: formRaw.fecha,
        cuit: this.companyConfig.cuit ? Number(this.companyConfig.cuit.replace(/\D/g, '')) : 0,
        ptoVta: resp.puntoVenta,
        tipoCmp: resp.tipoComprobante,
        nroCmp: resp.cbteDesde,
        importe: Math.round(this.totalGeneral * 100) / 100,
        moneda: formRaw.monId || 'PES',
        ctz: Number(formRaw.monCotiz) || 1,
        tipoDocRec: Number(formRaw.docTipo),
        nroDocRec: Number(formRaw.docNro) || 0,
        tipoCodAut: 'E',
        codAut: Number(resp.cae)
      };
      const jsonStr = JSON.stringify(qrData);

      const base64 = btoa(unescape(encodeURIComponent(jsonStr)));
      this.qrCodeUrl = 'https://www.afip.gob.ar/fe/qr/?p=' + base64;
    } catch (e) {
      console.warn('Error generando QR:', e);
      this.qrCodeUrl = 'https://www.afip.gob.ar/fe/qr/';
    }
  }

  getInvoiceLetter(tipoComprobante: number): string {
    if ([1, 2, 3, 4, 5].includes(tipoComprobante)) return 'A';
    if ([6, 7, 8, 9, 10].includes(tipoComprobante)) return 'B';
    if ([11, 12, 13, 15].includes(tipoComprobante)) return 'C';
    return '';
  }

  getInvoiceCode(tipoComprobante: number): string {
    return String(tipoComprobante).padStart(3, '0');
  }

  getCondicionIvaDesc(id: number): string {
    const map: { [key: number]: string } = {
      1: 'IVA Responsable Inscripto',
      4: 'IVA Sujeto Exento',
      5: 'Consumidor Final',
      6: 'Responsable Monotributo',
      8: 'Proveedor del Exterior',
      9: 'Cliente del Exterior',
      10: 'IVA Liberado – Ley Nº 19.640',
      11: 'IVA Responsable Inscripto – Agente de Percepción',
      13: 'Monotributista Social',
      15: 'IVA No Alcanzado'
    };
    return map[id] || 'Consumidor Final';
  }

  formatCuit(cuit: string | undefined): string {
    if (!cuit) return '—';
    const clean = cuit.replace(/\D/g, '');
    if (clean.length === 11) {
      return `${clean.substring(0, 2)}-${clean.substring(2, 10)}-${clean.substring(10)}`;
    }
    return cuit;
  }

  formatCaeFchVto(fch: string): string {
    if (!fch || fch.length !== 8) return '—';
    return `${fch.substring(6, 8)}/${fch.substring(4, 6)}/${fch.substring(0, 4)}`;
  }

  formatCbteNro(ptoVta: number, cbteNro: number): string {
    return `${String(ptoVta).padStart(4, '0')}-${String(cbteNro).padStart(8, '0')}`;
  }

  printInvoice() {
    const printArea = document.getElementById('invoice-print-area');
    if (!printArea) return;


    const clone = printArea.cloneNode(true) as HTMLElement;
    const canvases = printArea.querySelectorAll('canvas');
    const clonedCanvases = clone.querySelectorAll('canvas');
    canvases.forEach((c, i) => {
      const img = document.createElement('img');
      img.src = c.toDataURL();
      img.style.width = c.style.width || c.width + 'px';
      img.style.height = c.style.height || c.height + 'px';
      clonedCanvases[i]?.parentNode?.replaceChild(img, clonedCanvases[i]);
    });


    clone.querySelectorAll('img').forEach(img => {
      if (img.src) img.setAttribute('src', img.src);
    });

    const printWindow = window.open('', '_blank');
    if (!printWindow) return;

    printWindow.document.write(`<!DOCTYPE html>
<html>
<head>
<title>Factura</title>
<style>
  @page { size: A4; margin: 10mm; }
  * { box-sizing: border-box; }
  body {
    margin: 0; padding: 15mm 20mm;
    background: #fff; color: #000;
    font-family: Arial, Helvetica, sans-serif;
    font-size: 11px; line-height: 1.4;
  }
  .invoice-top {
    display: flex; border: 2px solid #000; position: relative;
    margin-bottom: 8px; min-height: 140px; overflow: hidden;
  }
  .invoice-top::after {
    content: ''; position: absolute; top: 0; bottom: 0;
    left: 50%; transform: translateX(-50%);
    width: 2px; background: #000; z-index: 0;
  }
  .invoice-letter-box {
    position: absolute; top: -2px; left: 50%;
    transform: translateX(-50%);
    width: 64px; height: 64px; background: #fff;
    border: 2px solid #000; box-sizing: border-box;
    display: flex; flex-direction: column;
    align-items: center; justify-content: center; z-index: 2;
  }
  .letter { font-size: 32px; font-weight: bold; line-height: 1; }
  .code { font-size: 9px; color: #333; margin-top: 2px; }
  .company-col { flex: 1; padding: 16px 20px; box-sizing: border-box; }
  .company-name { font-size: 18px; font-weight: bold; margin: 0 0 8px; text-transform: uppercase; }
  .company-detail { margin: 2px 0; font-size: 11px; color: #000; }
  .invoice-data-col { flex: 1; padding: 16px 20px 16px 50px; box-sizing: border-box; }
  .invoice-type-title { font-size: 16px; font-weight: bold; margin: 0 0 8px; }
  .invoice-detail { margin: 3px 0; font-size: 11px; color: #000; }
  .period-row { border: 1px solid #000; padding: 8px 12px; margin-bottom: 8px; font-size: 11px; }
  .receiver-row { border: 1px solid #000; padding: 10px 14px; margin-bottom: 8px; display: flex; gap: 20px; font-size: 11px; }
  .receiver-row .col { flex: 1; }
  .receiver-row p { margin: 3px 0; }
  .preview-table { width: 100%; border: 1px solid #000; border-collapse: collapse; margin-bottom: 8px; font-size: 10px; }
  .preview-table th { background: #f0f0f0; font-weight: bold; padding: 6px 8px; border: 1px solid #000; font-size: 10px; text-align: center; text-transform: uppercase; }
  .preview-table td { padding: 5px 8px; border: 1px solid #000; text-align: center; }
  .preview-totals-section { border: 1px solid #000; padding: 10px 14px; margin-bottom: 8px; display: flex; justify-content: flex-end; }
  .totals-box { text-align: right; font-size: 11px; min-width: 300px; }
  .totals-box .row { margin-bottom: 3px; }
  .totals-box .total { font-weight: bold; font-size: 14px; margin-top: 6px; padding-top: 6px; border-top: 1px solid #000; }
  .totals-box .imp-no-gravado, .totals-box .imp-exento { color: #555; }
  .preview-bottom-footer { border: 1px solid #000; padding: 12px; display: flex; justify-content: space-between; align-items: center; }
  .footer-left-group { display: flex; gap: 14px; align-items: center; flex: 2; }
  .qr-box { display: flex; align-items: center; justify-content: center; width: 120px; height: 120px; flex-shrink: 0; }
  .qr-box img { display: block; width: 120px !important; height: 120px !important; image-rendering: pixelated; }
  .afip-logo-box { display: flex; flex-direction: column; justify-content: center; }
  .arca-logo-img { max-height: 36px; margin-bottom: 4px; align-self: flex-start; }
  .auth-text { font-weight: bold; font-style: italic; font-size: 12px; margin: 0; }
  .disclaimer-text { font-size: 7.5px; font-style: italic; margin-top: 2px; max-width: 240px; color: #555; }
  .footer-center { flex: 0.5; text-align: center; font-weight: bold; font-size: 11px; }
  .footer-right { flex: 1.5; text-align: right; }
  .cae-info p { margin: 3px 0; font-size: 12px; }
  .observations-box { margin-top: 10px; padding: 8px 12px; border: 1px dashed #999; font-size: 10px; color: #555; }
  .observations-box p { margin: 0 0 4px; }
  .observations-box ul { margin: 0; padding-left: 18px; }
  .observations-box li { margin-bottom: 2px; }
</style>
</head>
<body>${clone.innerHTML}</body>
</html>`);
    printWindow.document.close();


    const images = printWindow.document.querySelectorAll('img');
    let loaded = 0;
    const total = images.length;

    const triggerPrint = () => {
      printWindow.focus();
      printWindow.print();
      printWindow.close();
    };

    if (total === 0) {
      triggerPrint();
    } else {
      images.forEach(img => {
        if (img.complete) {
          loaded++;
          if (loaded === total) triggerPrint();
        } else {
          img.onload = img.onerror = () => {
            loaded++;
            if (loaded === total) triggerPrint();
          };
        }
      });
    }
  }

  closeModal() {
    this.modalStep = 'closed';
  }

  closeAuthorizedInvoice() {
    this.router.navigate(['/facturacion']);
  }

  cancel() {
    this.router.navigate(['/facturacion']);
  }
}
