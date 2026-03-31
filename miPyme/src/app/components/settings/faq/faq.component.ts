import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

interface FaqItem {
  category: string;
  question: string;
  answer: string;
  isOpen?: boolean;
}

@Component({
  selector: 'app-faq',
  templateUrl: './faq.component.html',
  styleUrls: ['./faq.component.css']
})
export class FaqComponent implements OnInit {

  searchTerm: string = '';


  allFaqs: FaqItem[] = [
    {
      category: 'Stock e Inventario',
      question: '¿Cómo crear un nuevo producto?',
      answer: 'Para crear un producto, ve a la sección "Stock" en el menú principal y haz clic en el botón "+ Nuevo Producto". Completa los campos obligatorios como nombre, SKU y precio de costo.'
    },
    {
      category: 'Stock e Inventario',
      question: '¿Cómo funcionan las variantes de producto?',
      answer: 'Las variantes te permiten gestionar diferentes versiones de un mismo producto (ej. tallas o colores). Al crear o editar un producto, busca la sección "Variantes" y añade atributos como Talle: L, Color: Rojo. Cada variante tendrá su propio stock y SKU.'
    },
    {
      category: 'Stock e Inventario',
      question: '¿Cómo ajusto el stock manualmente?',
      answer: 'Puedes ajustar el stock desde la lista de productos. Busca el producto, haz clic en el menú de acciones (tres puntos) y selecciona "Ajustar Stock". También puedes hacerlo editando el producto directamente.'
    },
    {
      category: 'Ventas',
      question: '¿Cómo registro una venta?',
      answer: 'Ve a la sección "Ventas" y pulsa "Nueva Venta". Selecciona los productos del inventario o escanea sus códigos de barra. Confirma el método de pago y finaliza la operación.'
    },
    {
      category: 'Integraciones',
      question: '¿Cómo vinculo mi cuenta de MercadoLibre?',
      answer: 'Dirígete a Configuración > Integraciones > Mercado Libre. Sigue el asistente de vinculación para autorizar a MiPyme a acceder a tu cuenta. Una vez vinculado, podrás importar tus publicaciones.'
    },
    {
      category: 'Integraciones',
      question: '¿Qué hago si mis productos de TiendaNube no se sincronizan?',
      answer: 'Verifica que la integración esté activa en Configuración > Integraciones > Tienda Nube. Si el problema persiste, revisa el "Estado del Sistema" en la sección de Ayuda o intenta forzar una sincronización manual desde el asistente.'
    },
    {
      category: 'Cuenta y Seguridad',
      question: '¿Cómo cambio mi contraseña?',
      answer: 'Ve a Perfil > Contraseña. Deberás ingresar tu contraseña actual y la nueva contraseña dos veces para confirmar.'
    },
    {
      category: 'Cuenta y Seguridad',
      question: '¿Cómo activo la autenticación de dos factores (2FA)?',
      answer: 'En Perfil > Seguridad > 2FA/MFA, puedes activar la autenticación de dos factores. Esto requerirá un código enviado a tu email cada vez que inicies sesión en un nuevo dispositivo.'
    }
  ];

  filteredFaqs: FaqItem[] = [];

  constructor(private router: Router) { }

  ngOnInit(): void {
    this.filteredFaqs = [...this.allFaqs];
  }

  onSearch(): void {
    if (!this.searchTerm.trim()) {
      this.filteredFaqs = [...this.allFaqs];
      return;
    }

    const term = this.searchTerm.toLowerCase();
    this.filteredFaqs = this.allFaqs.filter(faq =>
      faq.question.toLowerCase().includes(term) ||
      faq.answer.toLowerCase().includes(term) ||
      faq.category.toLowerCase().includes(term)
    );
  }


  getCategoryClass(category: string): string {
    const map: { [key: string]: string } = {
      'Stock e Inventario': 'cat-stock',
      'Ventas': 'cat-sales',
      'Integraciones': 'cat-integrations',
      'Cuenta y Seguridad': 'cat-security'
    };
    return map[category] || 'cat-default';
  }

  toggleFaq(faq: FaqItem): void {
    faq.isOpen = !faq.isOpen;
  }

  goToSupport(): void {
    this.router.navigate(['/configuracion/ayuda/soporte']);
  }
}
