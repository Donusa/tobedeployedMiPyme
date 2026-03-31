import { Component, OnInit } from '@angular/core';

interface ChangeLogItem {
  version: string;
  date: string;
  title: string;
  description: string;
  changes: string[];
  type: 'major' | 'minor' | 'patch';
}

@Component({
  selector: 'app-changelog',
  templateUrl: './changelog.component.html',
  styleUrls: ['./changelog.component.css']
})
export class ChangelogComponent implements OnInit {
  changelog: ChangeLogItem[] = [
    {
      version: '1.2.0',
      date: '2025-05-15',
      title: 'Integración con MercadoLibre y Mejoras de UI',
      description: 'Lanzamos nuevas herramientas de sincronización y unificamos la experiencia de usuario.',
      changes: [
        'Nuevo Wizard de sincronización para MercadoLibre y TiendaNube.',
        'Unificación de estilos en tarjetas y métricas (Desktop/Mobile).',
        'Soporte para 2FA vía Email.',
        'Corrección de errores en la visualización de stock.'
      ],
      type: 'minor'
    },
    {
      version: '1.1.5',
      date: '2025-04-20',
      title: 'Optimización de Stock',
      description: 'Mejoras en el rendimiento del módulo de stock y nuevas alertas.',
      changes: [
        'Nuevas métricas operativas para control de inventario.',
        'Alertas de stock mínimo y crítico.',
        'Optimización en la carga de productos masivos.'
      ],
      type: 'patch'
    },
    {
      version: '1.1.0',
      date: '2025-03-10',
      title: 'Módulo de Ventas',
      description: 'Gestión completa de ventas y facturación.',
      changes: [
        'Creación y gestión de órdenes de venta.',
        'Historial de facturación.',
        'Reportes de ingresos mensuales.'
      ],
      type: 'minor'
    }
  ];

  constructor() { }

  ngOnInit(): void {
  }
}
