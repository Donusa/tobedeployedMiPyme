import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { PlanService } from '../../../services/plan.service';

interface BottomNavItem {
  label: string;
  icon: string;
  route?: string;
  action?: 'menu' | 'settings';
  permission?: string;
}

@Component({
  selector: 'app-bottom-nav',
  templateUrl: './bottom-nav.component.html',
  styleUrls: ['./bottom-nav.component.css']
})
export class BottomNavComponent implements OnInit {

  @Output() menuToggle = new EventEmitter<void>();
  @Output() settingsToggle = new EventEmitter<void>();

  navItems: BottomNavItem[] = [
    { label: 'Inicio',    icon: 'bx bx-home',                 route: '/home',       permission: 'ventas' },
    { label: 'Stock',     icon: 'bx bx-package',              route: '/stock',       permission: 'stock' },
    { label: 'Ventas',    icon: 'bx bx-cart',                 route: '/ventas',      permission: 'ventas' },
    { label: 'Mensajes',  icon: 'bx bx-message-square-dots',  route: '/mensajeria',  permission: 'mensajeria' },
    { label: 'Config',    icon: 'bx bx-cog',                  action: 'settings' },
    { label: 'Menú',      icon: 'bx bx-menu',                  action: 'menu' }
  ];

  visibleItems: BottomNavItem[] = [];

  constructor(
    public router: Router,
    private authService: AuthService,
    private planService: PlanService
  ) {}

  ngOnInit(): void {

    this.visibleItems = this.navItems.filter(item =>
      !item.permission || this.authService.hasPermission(item.permission)
    );

    const menuItem = this.visibleItems.find(i => i.action === 'menu');
    if (menuItem) {
      this.visibleItems = [
        ...this.visibleItems.filter(i => i.action !== 'menu'),
        menuItem
      ];
    }
  }

  isActive(route?: string): boolean {
    if (!route) return false;
    return this.router.url === route || this.router.url.startsWith(route + '/');
  }

  onItemClick(item: BottomNavItem): void {
    if (item.action === 'menu') {
      this.menuToggle.emit();
    } else if (item.action === 'settings') {
      this.settingsToggle.emit();
    }

  }
}
