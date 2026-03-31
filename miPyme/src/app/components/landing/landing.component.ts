import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-landing',
  templateUrl: './landing.component.html',
  styleUrls: ['./landing.component.css']
})
export class LandingComponent implements OnInit, OnDestroy {

  isScrolled = false;
  isMenuOpen = false;

  features = [
    {
      icon: 'bx-cart-alt',
      title: 'Gestión de Ventas',
      desc: 'Registrá y optimizá cada venta con seguimiento en tiempo real, historial completo y reportes inteligentes.',
      color: 'blue'
    },
    {
      icon: 'bx-package',
      title: 'Control de Stock',
      desc: 'Mantenés tu inventario siempre actualizado, con alertas automáticas y movimientos trazados.',
      color: 'green'
    },
    {
      icon: 'bx-file',
      title: 'Facturación ARCA/AFIP',
      desc: 'Emití facturas electrónicas A, B y C conectando directamente con los servicios de AFIP.',
      color: 'purple'
    },
    {
      icon: 'bxl-whatsapp',
      title: 'Mensajería WhatsApp',
      desc: 'Atendé a tus clientes desde la plataforma vía WhatsApp Cloud API sin salir de tu flujo de trabajo.',
      color: 'emerald'
    },
    {
      icon: 'bx-store',
      title: 'E-commerce Integrado',
      desc: 'Sincronizá stock y pedidos con Mercado Libre y Tienda Nube de forma automática y sin fricciones.',
      color: 'orange'
    },
    {
      icon: 'bx-bar-chart-alt-2',
      title: 'Métricas y Analytics',
      desc: 'Tomá decisiones basadas en datos reales: tendencias, márgenes, alertas operativas y más.',
      color: 'indigo'
    }
  ];

  steps = [
    {
      number: '01',
      title: 'Registrate',
      desc: 'Creá tu cuenta en minutos, sin tarjeta de crédito. Empezás con 28 días de prueba Pro completamente gratis.',
      icon: 'bx-user-plus',
      color: 'blue'
    },
    {
      number: '02',
      title: 'Configurá',
      desc: 'Cargá tu negocio, agregá productos a tu stock y conectá tus canales de venta. Un wizard paso a paso te guía.',
      icon: 'bx-cog',
      color: 'green'
    },
    {
      number: '03',
      title: 'Empezá a vender',
      desc: 'Registrá ventas, emití facturas, gestioná pedidos y atendé clientes por WhatsApp desde un solo lugar.',
      icon: 'bx-trending-up',
      color: 'purple'
    }
  ];

  stats = [
    { value: '99.9%', label: 'Uptime garantizado' },
    { value: '100%', label: 'Cumplimiento AFIP' },
    { value: '+50', label: 'PyMEs activas' },
    { value: '24/7', label: 'Soporte dedicado' }
  ];

  plans = [
    {
      id: 'base',
      name: 'Base',
      price: 'US$ 9',
      period: '/mes',
      desc: 'Para comenzar a ordenar tu negocio.',
      features: [
        'Dashboard principal',
        'Gestión de stock',
        'Registro de ventas',
        'Administración de empleados'
      ],
      disabled: ['Facturación ARCA/AFIP'],
      featured: false,
      trialBadge: null as string | null
    },
    {
      id: 'pro',
      name: 'Pro',
      price: 'US$ 19',
      period: '/mes',
      desc: 'Conectá tus tiendas y vendé en todos los canales.',
      features: [
        'Todo lo del plan Base',
        'Ofertas y promociones',
        'Gestión de pedidos',
        'Sincronización Mercado Libre',
        'Sincronización Tienda Nube',
        'Mensajería WhatsApp integrada'
      ],
      disabled: ['Facturación ARCA/AFIP'],
      featured: true,
      trialBadge: '28 días gratis' as string | null
    },
    {
      id: 'enterprise',
      name: 'Enterprise',
      price: 'US$ 29',
      period: '/mes',
      desc: 'Facturación electrónica y cumplimiento fiscal completo.',
      features: [
        'Todo lo del plan Pro',
        'Facturación electrónica ARCA/AFIP',
        'Emisión de facturas y tickets',
        'Métricas y analytics avanzados',
        'Gestión de certificados fiscales',
        'Soporte prioritario'
      ],
      disabled: [],
      featured: false,
      trialBadge: null as string | null
    }
  ];

  testimonials = [
    {
      quote: 'Desde que usamos MiPyme, la facturación con AFIP dejó de ser un calvario. Todo en un solo lugar.',
      author: 'Alejandra M.',
      role: 'Dueña de ferretería, Rosario'
    },
    {
      quote: 'El control de stock y la integración con Mercado Libre nos ahorra horas de trabajo cada semana.',
      author: 'Nicolás T.',
      role: 'Responsable de e-commerce, Buenos Aires'
    },
    {
      quote: 'La atención por WhatsApp directamente desde la plataforma cambió por completo cómo manejamos clientes.',
      author: 'Débora R.',
      role: 'Gerente comercial, Córdoba'
    }
  ];

  faqItems = [
    {
      q: '¿Necesito un contador para usar MiPyme?',
      a: 'No. MiPyme está diseñado para que cualquier dueño de negocio pueda operarlo solo. La facturación AFIP tiene un wizard paso a paso que te guía desde la configuración hasta emitir tu primer comprobante.'
    },
    {
      q: '¿Funciona con monotributo?',
      a: 'Sí. Podés emitir facturas Tipo C para monotributistas desde el plan Enterprise. El sistema también soporta Responsables Inscriptos (facturas A y B). Homologación o Producción, vos elegís.'
    },
    {
      q: '¿Cómo migro mis datos desde otra herramienta?',
      a: 'Ofrecemos importación masiva de productos vía Excel/CSV. Para migraciones más complejas, nuestro equipo de soporte te asiste sin costo adicional durante los primeros 30 días.'
    },
    {
      q: '¿El plan Base incluye facturación AFIP?',
      a: 'No. La facturación electrónica ARCA/AFIP está disponible exclusivamente en el plan Enterprise. Base y Pro están orientados a gestión de ventas, stock e integraciones con canales de venta.'
    },
    {
      q: '¿En qué consiste el período de prueba de 28 días?',
      a: 'Al registrarte accedés automáticamente al plan Pro completo durante 28 días, sin tarjeta de crédito. Al finalizar podés elegir cualquier plan de pago. Si no elegís, tu cuenta pasa al plan Base sin costo.'
    },
    {
      q: '¿Puedo cancelar en cualquier momento?',
      a: 'Sí, sin penalidades ni formularios complicados. Podés cancelar tu suscripción cuando quieras desde tu perfil. Mantenés acceso hasta el fin del período ya abonado.'
    }
  ];

  openFaqIndex: number | null = null;

  activeTestimonial = 0;
  private testimonialInterval?: ReturnType<typeof setInterval>;

  constructor(private router: Router) {}

  ngOnInit(): void {

    document.body.style.overflow = 'auto';
    document.body.style.height = 'auto';

    this.testimonialInterval = setInterval(() => {
      this.activeTestimonial = (this.activeTestimonial + 1) % this.testimonials.length;
    }, 4500);


    const observer = new IntersectionObserver((entries) => {
      entries.forEach(e => {
        if (e.isIntersecting) {
          e.target.classList.add('revealed');
          observer.unobserve(e.target);
        }
      });
    }, { threshold: 0.1, rootMargin: '0px 0px -40px 0px' });

    setTimeout(() => {
      document.querySelectorAll('.reveal').forEach(el => observer.observe(el));
    }, 80);
    (this as any)._revealObserver = observer;
  }

  ngOnDestroy(): void {

    document.body.style.overflow = 'hidden';
    document.body.style.height = '100vh';

    if (this.testimonialInterval) {
      clearInterval(this.testimonialInterval);
    }
    if ((this as any)._revealObserver) {
      (this as any)._revealObserver.disconnect();
    }
  }

  @HostListener('window:scroll')
  onScroll(): void {
    this.isScrolled = window.scrollY > 40;
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }

  scrollTo(sectionId: string): void {
    this.isMenuOpen = false;
    const el = document.getElementById(sectionId);
    if (el) {
      const navbarHeight = 70;
      const top = el.getBoundingClientRect().top + window.scrollY - navbarHeight;
      window.scrollTo({ top, behavior: 'smooth' });
    }
  }

  setTestimonial(index: number): void {
    this.activeTestimonial = index;
  }

  toggleFaq(i: number): void {
    this.openFaqIndex = this.openFaqIndex === i ? null : i;
  }
}
